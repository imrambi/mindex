package com.mindex.challenge.service;

import java.util.List;

import com.mindex.challenge.data.Compensation;
import com.mindex.challenge.data.Employee;
import com.mindex.challenge.data.ReportingStructure;
import com.mindex.challenge.exception.CompensationCanNotBeInPastException;
import com.mindex.challenge.exception.EmployeeDoesNotExistsException;
import com.mindex.challenge.exception.EmployeeIdAlreadyExistsException;
import com.mindex.challenge.exception.EmployeeIdNotSpecifiedException;

import jakarta.validation.constraints.NotEmpty;

public interface EmployeeService {
    Employee create(Employee employee) throws EmployeeIdAlreadyExistsException, EmployeeDoesNotExistsException;
    Employee read(String id);
    Employee update(String id, Employee employee) throws EmployeeIdAlreadyExistsException, EmployeeDoesNotExistsException, EmployeeIdNotSpecifiedException;
	ReportingStructure findReportingStructure(@NotEmpty String id);
	Employee addCompensation(String id, Compensation compensation) throws EmployeeIdNotSpecifiedException, CompensationCanNotBeInPastException, EmployeeDoesNotExistsException, EmployeeIdAlreadyExistsException;
	List<Compensation> findCompensation(Employee employee) throws EmployeeIdNotSpecifiedException, EmployeeDoesNotExistsException;
}
