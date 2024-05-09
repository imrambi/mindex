package com.mindex.challenge.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.context.TestPropertySource;

import com.mindex.challenge.dao.EmployeeRepository;
import com.mindex.challenge.data.Compensation;
import com.mindex.challenge.data.Employee;
import com.mindex.challenge.data.ReportingStructure;
import com.mindex.challenge.exception.CompensationCanNotBeInPastException;
import com.mindex.challenge.exception.EmployeeDoesNotExistsException;
import com.mindex.challenge.exception.EmployeeIdAlreadyExistsException;
import com.mindex.challenge.exception.EmployeeIdNotSpecifiedException;

@RunWith(MockitoJUnitRunner.class)
@TestPropertySource(locations = "classpath:application.properties")
public class EmployeeServiceImplMockTest {
    
    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    @Test
    public void testCreateReadUpdate() throws EmployeeIdAlreadyExistsException, EmployeeDoesNotExistsException, EmployeeIdNotSpecifiedException {
        Employee testEmployee = new Employee();
        testEmployee.setFirstName("John");
        testEmployee.setLastName("Doe");
        testEmployee.setDepartment("Engineering");
        testEmployee.setPosition("Developer");

        Employee createdEmployee = employeeService.create(testEmployee);
        

        assertNotNull(createdEmployee.getEmployeeId());
        assertEmployeeEquivalence(testEmployee, createdEmployee);
        verify(employeeRepository, times(0)).findByEmployeeId(anyString());

        // Read checks
        when(employeeRepository.findByEmployeeId(anyString())).thenReturn(createdEmployee);
        Employee readEmployee = employeeService.read(createdEmployee.getEmployeeId());
        assertEquals(createdEmployee.getEmployeeId(), readEmployee.getEmployeeId());
        assertEmployeeEquivalence(createdEmployee, readEmployee);
        verify(employeeRepository, times(1)).findByEmployeeId(anyString());


        // Update checks
        readEmployee.setPosition("Development Manager");

        when(employeeRepository.save(any())).thenReturn(readEmployee);
        Employee updatedEmployee = employeeService.update(readEmployee.getEmployeeId(), readEmployee);
        verify(employeeRepository, times(1)).save(any(Employee.class));
        
        assertEmployeeEquivalence(readEmployee, updatedEmployee);
    }
    
    @Test
    public void testReportingStructure_0_Reports()
    {
    	Employee employee = new Employee();
    	employee.setDepartment("MOCK department");
    	employee.setEmployeeId("1");
    	employee.setFirstName("Bob");
    	employee.setLastName("Newhart");
    	employee.setDepartment("MOCK Department");
    	
    	when(employeeRepository.findByEmployeeId(employee.getEmployeeId())).thenReturn(employee);
    	
    	ReportingStructure reportingStructure = employeeService.findReportingStructure(employee.getEmployeeId());
    	
    	Employee reportingEmployee = reportingStructure.getEmployee();
    	assertNotNull(reportingEmployee);
    	assertEmployeeEquivalence(employee, reportingEmployee);
    	
    	assertEquals((Integer) 0, reportingStructure.getNumberOfReports());
    	
    	verify(employeeRepository, times(1)).findByEmployeeId(employee.getEmployeeId());
    }
    
    @Test
    public void testReportingStructure_SingleTier_Reports()
    {
    	Employee tier1Emp1 = new Employee();
    	tier1Emp1.setDepartment("Tier 1");
    	tier1Emp1.setEmployeeId("1");
    	tier1Emp1.setFirstName("Macho");
    	tier1Emp1.setLastName("Man");
    	
    	Employee tier2Emp1 = new Employee();
    	tier2Emp1.setDepartment("Tier 2-1");
    	tier2Emp1.setEmployeeId("2-1");
    	tier2Emp1.setFirstName("Wimpy");
    	tier2Emp1.setLastName("Wimpy");
    	
    	Employee tier2Emp2 = new Employee();
    	tier2Emp2.setDepartment("Tier 2-2");
    	tier2Emp2.setEmployeeId("2-2");
    	tier2Emp2.setFirstName("Here's");
    	tier2Emp2.setLastName("Waldo");
    	
    	tier1Emp1.setDirectReports(Arrays.asList(tier2Emp1, tier2Emp2));
    	
    	when(employeeRepository.findByEmployeeId(tier1Emp1.getEmployeeId())).thenReturn(tier1Emp1);
    	when(employeeRepository.findByEmployeeId(tier2Emp1.getEmployeeId())).thenReturn(tier2Emp1);
    	when(employeeRepository.findByEmployeeId(tier2Emp2.getEmployeeId())).thenReturn(tier2Emp2);
    	
    	ReportingStructure reportingStructure = employeeService.findReportingStructure(tier1Emp1.getEmployeeId());
    	assertNotNull(reportingStructure.getEmployee());
    	Employee employee = reportingStructure.getEmployee();
    	
    	assertEmployeeEquivalence(tier1Emp1, employee);
    	
    	assertTrue(CollectionUtils.isNotEmpty(employee.getDirectReports()));
    	assertEquals((Integer) 2, reportingStructure.getNumberOfReports());
    	
    	verify(employeeRepository, times(3)).findByEmployeeId(any());
    }
    
    @Test
    public void testReportingStructure_MultipleTier_Reports()
    {
    	Employee tier1Emp1 = new Employee();
    	tier1Emp1.setDepartment("Tier 1");
    	tier1Emp1.setEmployeeId("1");
    	tier1Emp1.setFirstName("Macho");
    	tier1Emp1.setLastName("Man");
    	
    	Employee tier2Emp1 = new Employee();
    	tier2Emp1.setDepartment("Tier 2-1");
    	tier2Emp1.setEmployeeId("2-1");
    	tier2Emp1.setFirstName("Wimpy");
    	tier2Emp1.setLastName("Wimpy");
    	
    	Employee tier2Emp2 = new Employee();
    	tier2Emp2.setDepartment("Tier 2-2");
    	tier2Emp2.setEmployeeId("2-2");
    	tier2Emp2.setFirstName("Here's");
    	tier2Emp2.setLastName("Waldo");
    	
    	Employee tier3Emp1 = new Employee();
    	tier3Emp1.setDepartment("Tier 3-1");
    	tier3Emp1.setEmployeeId("3-1");
    	tier3Emp1.setFirstName("Amilla");
    	tier3Emp1.setLastName("Badilla");
    	
    	tier2Emp2.setDirectReports(Arrays.asList(tier3Emp1));
    	
    	tier1Emp1.setDirectReports(Arrays.asList(tier2Emp1, tier2Emp2));
    	
    	when(employeeRepository.findByEmployeeId(tier1Emp1.getEmployeeId())).thenReturn(tier1Emp1);
    	when(employeeRepository.findByEmployeeId(tier2Emp1.getEmployeeId())).thenReturn(tier2Emp1);
    	when(employeeRepository.findByEmployeeId(tier2Emp2.getEmployeeId())).thenReturn(tier2Emp2);
    	
    	ReportingStructure reportingStructure = employeeService.findReportingStructure(tier1Emp1.getEmployeeId());
    	assertNotNull(reportingStructure.getEmployee());
    	Employee employee = reportingStructure.getEmployee();
    	
    	assertEmployeeEquivalence(tier1Emp1, employee);
    	
    	assertTrue(CollectionUtils.isNotEmpty(employee.getDirectReports()));
    	assertEquals((Integer) 3, reportingStructure.getNumberOfReports());
    	
    	verify(employeeRepository, times(3)).findByEmployeeId(any());
    }
    
    @Test
    public void testAddCompensation_FirstCompensation() throws EmployeeIdNotSpecifiedException, CompensationCanNotBeInPastException, EmployeeDoesNotExistsException, EmployeeIdAlreadyExistsException
    {
    	Employee emp = new Employee();
    	emp.setDepartment("Tier 1");
    	emp.setEmployeeId("1");
    	emp.setFirstName("Macho");
    	emp.setLastName("Man");
    	
    	Employee readEmp = new Employee();
    	readEmp.setDepartment("Tier 1");
    	readEmp.setEmployeeId("1");
    	readEmp.setFirstName("Macho");
    	readEmp.setLastName("Man");
    	
    	Compensation comp = new Compensation();
    	comp.setEffectiveDate(LocalDate.now());
    	comp.setSalary(BigDecimal.TEN);
    	
    	readEmp.setCompensation(Arrays.asList(comp));
    	
    	when(employeeRepository.findByEmployeeId(anyString())).thenReturn(emp);
    	when(employeeRepository.save(any())).thenReturn(readEmp);
    	Employee compEmployee = employeeService.addCompensation(emp.getEmployeeId(), comp);
    	verify(employeeRepository, times(1)).findByEmployeeId(anyString());
    	
    	assertNotNull(compEmployee);
    	List<Compensation> comps = compEmployee.getCompensation();
    	assertTrue(CollectionUtils.isNotEmpty(comps));
    	assertEquals((Integer) 1,(Integer) comps.size());
    	
    	assertComps(comp, comps.get(0));
    	
    }
    
    @Test
    public void testAddCompensation_MultipleCompensation() throws EmployeeIdNotSpecifiedException, CompensationCanNotBeInPastException, EmployeeDoesNotExistsException, EmployeeIdAlreadyExistsException
    {
    	Employee emp = new Employee();
    	emp.setDepartment("Tier 1");
    	emp.setEmployeeId("1");
    	emp.setFirstName("Macho");
    	emp.setLastName("Man");
    	
    	Employee returnEmp = new Employee();
    	returnEmp.setDepartment("Tier 1");
    	returnEmp.setEmployeeId("1");
    	returnEmp.setFirstName("Macho");
    	returnEmp.setLastName("Man");
    	
    	Compensation comp1 = new Compensation();
    	comp1.setEffectiveDate(LocalDate.of(2020,1,1));
    	comp1.setSalary(BigDecimal.ONE);
    	
    	Compensation comp2 = new Compensation();
    	comp2.setEffectiveDate(LocalDate.of(2021, 1, 1));
    	comp2.setSalary(new BigDecimal(5));
    	
    	emp.setCompensation(Arrays.asList(comp1, comp2));
    	
    	Compensation comp3 = new Compensation();
    	comp3.setEffectiveDate(LocalDate.now());
    	comp3.setSalary(BigDecimal.TEN);
    	
    	when(employeeRepository.findByEmployeeId(anyString())).thenReturn(emp);
    	
    	returnEmp.setCompensation(Arrays.asList(comp1, comp2, comp3));
    	
    	when(employeeRepository.save(any())).thenReturn(returnEmp);
    	Employee compEmployee = employeeService.addCompensation(returnEmp.getEmployeeId(), comp3);
    	verify(employeeRepository, times(1)).findByEmployeeId(anyString());
    	verify(employeeRepository, times(1)).save(any());
    	
    	assertNotNull(compEmployee);
    	List<Compensation> comps = compEmployee.getCompensation();
    	assertTrue(CollectionUtils.isNotEmpty(comps));
    	assertEquals((Integer) 3,(Integer) comps.size());
    	
    	assertComps(comp3, comps.get(2));
    	
    }
    
    @Test
    public void testAddCompensation_PastCompensation() throws EmployeeIdNotSpecifiedException, CompensationCanNotBeInPastException, EmployeeDoesNotExistsException, EmployeeIdAlreadyExistsException
    {
    	Employee emp = new Employee();
    	emp.setDepartment("Tier 1");
    	emp.setEmployeeId("1");
    	emp.setFirstName("Macho");
    	emp.setLastName("Man");
    	
    	Employee returnEmp = new Employee();
    	returnEmp.setDepartment("Tier 1");
    	returnEmp.setEmployeeId("1");
    	returnEmp.setFirstName("Macho");
    	returnEmp.setLastName("Man");
    	
    	Compensation comp1 = new Compensation();
    	comp1.setEffectiveDate(LocalDate.of(2020,1,1));
    	comp1.setSalary(BigDecimal.ONE);
    	
    	Compensation comp2 = new Compensation();
    	comp2.setEffectiveDate(LocalDate.of(2021, 1, 1));
    	comp2.setSalary(new BigDecimal(5));
    	
    	emp.setCompensation(Arrays.asList(comp1, comp2));
    	
    	Compensation comp3 = new Compensation();
    	comp3.setEffectiveDate(LocalDate.of(2019,1,1));
    	comp3.setSalary(BigDecimal.TEN);
    	
    	when(employeeRepository.findByEmployeeId(anyString())).thenReturn(emp);
    	
    	returnEmp.setCompensation(Arrays.asList(comp1, comp2, comp3));
    	
    	try
    	{
    		Employee compEmployee = employeeService.addCompensation(returnEmp.getEmployeeId(), comp3);
    		fail("Should have failed as compensation is in the past.");
    	}
    	catch (CompensationCanNotBeInPastException inPastException)
    	{
    		assertEquals("Failed to update employee 1. Latest compensation Compensation(salary=5, effectiveDate=2021-01-01) is after requested comp Compensation(salary=10, effectiveDate=2019-01-01)", inPastException.getMessage());
    		verify(employeeRepository, times(1)).findByEmployeeId(anyString());
    		verify(employeeRepository, times(0)).save(any());
    	}
    	catch (Exception e)
    	{
    		fail("Should have caught expcetion.", e);
    	}
    	
    	
    }
    
    @Test
    public void testFindCompensation() throws EmployeeIdNotSpecifiedException, CompensationCanNotBeInPastException, EmployeeDoesNotExistsException, EmployeeIdAlreadyExistsException
    {
    	Employee emp = new Employee();
    	emp.setDepartment("Tier 1");
    	emp.setEmployeeId("1");
    	emp.setFirstName("Macho");
    	emp.setLastName("Man");
    	
    	Compensation comp1 = new Compensation();
    	comp1.setEffectiveDate(LocalDate.of(2020,1,1));
    	comp1.setSalary(BigDecimal.ONE);
    	
    	Compensation comp2 = new Compensation();
    	comp2.setEffectiveDate(LocalDate.of(2021, 1, 1));
    	comp2.setSalary(new BigDecimal(5));
    	
    	emp.setCompensation(Arrays.asList(comp1, comp2));
    	
    	
    	
    	when(employeeRepository.findByEmployeeId(anyString())).thenReturn(emp);
    	
    	List<Compensation> returnComps = employeeService.findCompensation(emp);
    	
    	assertTrue(CollectionUtils.isNotEmpty(returnComps));
    	assertEquals((Integer) 2, (Integer) returnComps.size());
    	assertComps(comp1, returnComps.get(0));
    	assertComps(comp2, returnComps.get(1));
    	
    	
    	
    }
    
    @Test
    public void testAddAlreadyExists()
    {
    	Employee emp1 = new Employee();
    	emp1.setFirstName("Already");
    	emp1.setLastName("Exists");
    	emp1.setEmployeeId("1");
    	
    	Employee emp2 = new Employee();
    	emp2.setFirstName("New");
    	emp2.setLastName("Employee");
    	emp2.setEmployeeId("1");
    	
    	try
    	{
    		when(employeeRepository.findByEmployeeId(anyString())).thenReturn(emp1);
    		employeeService.create(emp2);
    		fail("Should have failed as employee ID already exists.");
    	}
    	catch (EmployeeIdAlreadyExistsException eiaee)
    	{
    		verify(employeeRepository, times(0)).save(any());
    		assertEquals("User \"1\" already exists. Employee ID must be unique", eiaee.getMessage());
    	}
    	catch (Exception e)
    	{
    		fail("Should have failed with EmployeeIdAlreadyExistsException.", e);
    	}
    	
    }
    
    @Test
    public void testUpdateIdAlreadyExists()
    {
    	Employee emp1 = new Employee();
    	emp1.setFirstName("Already");
    	emp1.setLastName("Exists");
    	emp1.setEmployeeId("1");
    	
    	Employee emp2 = new Employee();
    	emp2.setFirstName("New");
    	emp2.setLastName("Employee");
    	emp2.setEmployeeId("2");
    	
    	try
    	{
    		when(employeeRepository.findByEmployeeId(anyString())).thenReturn(emp1, emp2);
    		employeeService.update("2", emp1);
    		fail("Should have failed as employee ID already exists.");
    	}
    	catch (EmployeeIdAlreadyExistsException eiaee)
    	{
    		verify(employeeRepository, times(0)).save(any());
    		assertEquals("Unable to change employee ID from 1 to 2. New ID already exists.", eiaee.getMessage());
    	}
    	catch (Exception e)
    	{
    		fail("Should have failed with EmployeeIdAlreadyExistsException.", e);
    	}
    	
    }
    
    @Test
    public void testAddMissingDirectReports()
    {
    	Employee emp1 = new Employee();
    	emp1.setFirstName("Already");
    	emp1.setLastName("Exists");
    	emp1.setEmployeeId("1");
    	
    	Employee emp2 = new Employee();
    	emp2.setEmployeeId("2");
    	
    	emp1.setDirectReports(Arrays.asList(emp2));
    	
    	try
    	{
    		when(employeeRepository.findByEmployeeId(anyString())).thenReturn(null);
    		when(employeeRepository.findAllById(any())).thenReturn(new ArrayList<>());
    		employeeService.create(emp1);
    		fail("Should have as direct reports don't exists");
    	}
    	catch (EmployeeDoesNotExistsException ednee)
    	{
    		verify(employeeRepository, times(0)).save(any());
    		verify(employeeRepository, times(1)).findByEmployeeId(anyString());
    		//verify(employeeRepository, times(1)).findAllById(any());
    		assertEquals("Employee IDs [2] were not found while creating Employee(employeeId=1, firstName=Already, lastName=Exists, position=null, department=null, directReports=[Employee(employeeId=2, firstName=null, lastName=null, position=null, department=null, directReports=null, compensation=null)], compensation=null)", ednee.getMessage());
    	}
    	catch (Exception e)
    	{
    		fail("Should have failed with EmployeeDoesNotExistsException.", e);
    	}
    	
    }
    

    private static void assertEmployeeEquivalence(Employee expected, Employee actual) {
        assertEquals(expected.getFirstName(), actual.getFirstName());
        assertEquals(expected.getLastName(), actual.getLastName());
        assertEquals(expected.getDepartment(), actual.getDepartment());
        assertEquals(expected.getPosition(), actual.getPosition());
    }
    
    private static void assertComps(Compensation expected, Compensation actual)
    {
    	assertTrue(expected.getEffectiveDate().compareTo(actual.getEffectiveDate())==0, "Dates do not match");
    	assertTrue(expected.getSalary().compareTo(actual.getSalary())==0, "Salaries do not match.");
    	
    }
}
