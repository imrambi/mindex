package com.mindex.challenge.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mindex.challenge.dao.EmployeeRepository;
import com.mindex.challenge.data.Compensation;
import com.mindex.challenge.data.Employee;
import com.mindex.challenge.data.ReportingStructure;
import com.mindex.challenge.exception.CompensationCanNotBeInPastException;
import com.mindex.challenge.exception.EmployeeDoesNotExistsException;
import com.mindex.challenge.exception.EmployeeIdAlreadyExistsException;
import com.mindex.challenge.exception.EmployeeIdNotSpecifiedException;
import com.mindex.challenge.service.EmployeeService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmployeeServiceImpl implements EmployeeService
{

	@Autowired
	private EmployeeRepository employeeRepository;

	@Override
	public Employee create(@Valid Employee employee)
			throws EmployeeIdAlreadyExistsException, EmployeeDoesNotExistsException
	{
		log.debug("Creating employee [{}]", employee.toString());

		// If the employee ID exists, we can not create the user. Throw an error.
		if (StringUtils.isNotBlank(employee.getEmployeeId()) && read(employee.getEmployeeId()) != null)
		{
			String errorMsg = "User \"%employeeId%\" already exists. Employee ID must be unique";
			errorMsg = errorMsg.replaceAll("%employeeId%", employee.getEmployeeId());
			throw new EmployeeIdAlreadyExistsException(errorMsg);
		}

		// Validate direct reports exists
		List<Employee> directReportEmployees = employee.getDirectReports();
		if (CollectionUtils.isNotEmpty(directReportEmployees))
		{
			List<String> directReportIds = directReportEmployees.stream().map(Employee::getEmployeeId).distinct()
					.collect(Collectors.toList());
			List<Employee> foundDirectReports = employeeRepository.findAllById(directReportIds);

			if (foundDirectReports.size() != directReportIds.size())
			{
				List<String> existingDirectReports = foundDirectReports.stream().map(Employee::getEmployeeId).distinct()
						.collect(Collectors.toList());
				directReportIds.removeAll(existingDirectReports);
				if (CollectionUtils.isNotEmpty(directReportIds))
				{
					String msg = "Employee IDs %ids% were not found while creating %employee%";
					msg = msg.replaceAll("%ids%", directReportIds.toString());
					msg = msg.replaceAll("%employee%", employee.toString());
					throw new EmployeeDoesNotExistsException(msg);
				}
			}

		}

		employee.setEmployeeId(UUID.randomUUID().toString());
		employeeRepository.insert(employee);

		return employee;
	}

	@Override
	public Employee read(String id)
	{
		log.debug("Creating employee with id [{}]", id);

		// If the employee does not exists, this is ok, we want a null object
		return employeeRepository.findByEmployeeId(id);
	}

	@Override
	public Employee update(String id, Employee employee)
			throws EmployeeIdAlreadyExistsException, EmployeeDoesNotExistsException, EmployeeIdNotSpecifiedException
	{
		log.debug("Updating employee [{}]", employee.toString());

		validateEmployeeId(employee.getEmployeeId());

		// The new employeeID must not exists as employeeIDs must be unique. If there is
		// an employee already
		// with that employeeID we can not change to that employeeID.
		try
		{
			Employee existingEmployee = validateEmployeeId(id);
			if (!StringUtils.equalsAnyIgnoreCase(id, employee.getEmployeeId()) && existingEmployee != null)
			{
				String errorMsg = "Unable to change employee ID from %currentID% to %newID%. New ID already exists.";
				errorMsg = errorMsg.replaceAll("%currentID%", employee.getEmployeeId());
				errorMsg = errorMsg.replaceAll("%newID%", id);
				log.info(errorMsg);
				throw new EmployeeIdAlreadyExistsException(errorMsg);
			}
		}
		catch (EmployeeDoesNotExistsException dne)
		{
			// Safely ignore as we don't want the employee to exists
		}

		employee.setEmployeeId(id);
		return employeeRepository.save(employee);
	}

	@Override
	public ReportingStructure findReportingStructure(String id)
	{
		ReportingStructure reportingStructure = new ReportingStructure();

		Employee manager;
		// Employee ID does not exists, can return now.
		try
		{
			manager = validateEmployeeId(id);
		}
		catch (EmployeeDoesNotExistsException | EmployeeIdNotSpecifiedException badIdException)
		{
			return reportingStructure;
		}

		reportingStructure.setEmployee(manager);

		// We have an employee, but is not a manager, we can return now
		if (CollectionUtils.isEmpty(manager.getDirectReports()))
		{
			reportingStructure.setNumberOfReports(0);
			return reportingStructure;
		}

		// Not the most efficient, as this is breadth first search down the tree
		// and fetches one employee at a time. If they have direct reports,
		// the ids are added to the list to see if those have direct reports.
		List<String> directReportIds = new CopyOnWriteArrayList<String>();

		directReportIds.addAll(manager.getDirectReports().stream().map(Employee::getEmployeeId).distinct()
				.collect(Collectors.toList()));

		for (String currentEmployeeId : directReportIds)
		{
			Employee employee = read(currentEmployeeId);

			if (employee == null || CollectionUtils.isEmpty(employee.getDirectReports()))
			{
				continue;
			}

			directReportIds.addAll(employee.getDirectReports().stream().map(Employee::getEmployeeId).distinct()
					.collect(Collectors.toList()));

		}

		reportingStructure.setNumberOfReports(directReportIds.size());
		return reportingStructure;

	}

	@Override
	@Transactional
	public Employee addCompensation(String id, Compensation compensation) throws EmployeeIdNotSpecifiedException,
			CompensationCanNotBeInPastException, EmployeeDoesNotExistsException, EmployeeIdAlreadyExistsException
	{

		Employee employee = validateEmployeeId(id);
		
		// Sort the compensation by effective date ascending
		List<Compensation> compensationHistory = employee.getCompensation();
		if (CollectionUtils.isNotEmpty(compensationHistory))
		{
			List<Compensation> orderedCompensation = compensationHistory.stream()
					.sorted((c1, c2) -> c1.getEffectiveDate().compareTo(c2.getEffectiveDate()))
					.collect(Collectors.toList());

			Compensation latestComp = orderedCompensation.get(orderedCompensation.size() - 1);

			if (compensation.getEffectiveDate().compareTo(latestComp.getEffectiveDate()) <= 0)
			{
				String msg = "Failed to update employee %employeeID%. Latest compensation %latestComp% is after requested comp %requestedComp%";
				msg = msg.replaceAll("%employeeID%", id);
				msg = msg.replaceAll("%latestComp%", latestComp.toString());
				msg = msg.replaceAll("%requestedComp%", compensation.toString());
				log.debug(msg);
				throw new CompensationCanNotBeInPastException(msg);
			}
		}

		if (CollectionUtils.isEmpty(compensationHistory))
		{
			compensationHistory = new ArrayList<>();
		}
		else
		{
			//Make a list, not an array so we can add the new compensation
			compensationHistory = new ArrayList<>(compensationHistory);
		}
		
		compensationHistory.add(compensation);
		employee.setCompensation(compensationHistory);

		return employeeRepository.save(employee);

	}

	@Override
	public List<Compensation> findCompensation(Employee employee)
			throws EmployeeIdNotSpecifiedException, EmployeeDoesNotExistsException
	{
		Employee foundEmployee = validateEmployeeId(employee.getEmployeeId());

		if (CollectionUtils.isEmpty(foundEmployee.getCompensation()))
		{
			return foundEmployee.getCompensation();
		}

		return foundEmployee.getCompensation().stream()
				.sorted((c1, c2) -> c1.getEffectiveDate().compareTo(c2.getEffectiveDate()))
				.collect(Collectors.toList());
	}

	/**
	 * Checks to see if the employeeID is valid and returns the employee.
	 * 
	 * @param employeeID - The employeeID for the employee to be found
	 * @return The employee that is found
	 * @throws EmployeeIdNotSpecifiedException If the employeeID is blank
	 * @throws EmployeeDoesNotExistsException  If the employeeID is not found
	 */
	protected Employee validateEmployeeId(String employeeId)
			throws EmployeeIdNotSpecifiedException, EmployeeDoesNotExistsException
	{
		// Validate employee ID
		if (StringUtils.isBlank(employeeId))
		{
			String msg = "Employee ID must be specified.";
			log.debug(msg);
			throw new EmployeeIdNotSpecifiedException(msg);
		}

		Employee employee = null;

		if ((employee = employeeRepository.findByEmployeeId(employeeId)) == null)
		{
			String msg = "Employee for employee ID %employeeID% does not exists.";
			msg = msg.replaceAll("%employeeID%", employeeId);
			log.debug(msg);
			throw new EmployeeDoesNotExistsException(msg);
		}

		return employee;

	}
}
