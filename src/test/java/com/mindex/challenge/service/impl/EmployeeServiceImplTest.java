package com.mindex.challenge.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.mindex.challenge.data.Compensation;
import com.mindex.challenge.data.Employee;
import com.mindex.challenge.data.ReportingStructure;
import com.mindex.challenge.service.EmployeeService;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:application.properties")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class EmployeeServiceImplTest {

    private String employeeUrl;
    private String employeeIdUrl;
    private String reportinStructureUrl;
    private String addCompensationUrl;
    private String findCompensationUrl;
    
    @Autowired
    private EmployeeService employeeService;

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Before
    public void setup() {
        employeeUrl = "http://localhost:" + port + "/employee";
        employeeIdUrl = "http://localhost:" + port + "/employee/{id}";
        reportinStructureUrl = "http://localhost:" + port + "/reportingStructure/{id}";
        addCompensationUrl = "http://localhost:" + port + "/compensation/add/{id}";
        findCompensationUrl = "http://localhost:" + port + "/compensation/";
        
        
    }

    @Test
    @Rollback
    public void testCreateReadUpdate() {
        Employee testEmployee = new Employee();
        testEmployee.setFirstName("John");
        testEmployee.setLastName("Doe");
        testEmployee.setDepartment("Engineering");
        testEmployee.setPosition("Developer");

        // Create checks
        Employee createdEmployee = restTemplate.postForEntity(employeeUrl, testEmployee, Employee.class).getBody();

        assertNotNull(createdEmployee.getEmployeeId());
        assertEmployeeEquivalence(testEmployee, createdEmployee);


        // Read checks
        Employee readEmployee = restTemplate.getForEntity(employeeIdUrl, Employee.class, createdEmployee.getEmployeeId()).getBody();
        assertEquals(createdEmployee.getEmployeeId(), readEmployee.getEmployeeId());
        assertEmployeeEquivalence(createdEmployee, readEmployee);


        // Update checks
        readEmployee.setPosition("Development Manager");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Employee updatedEmployee =
                restTemplate.exchange(employeeIdUrl,
                        HttpMethod.PUT,
                        new HttpEntity<Employee>(readEmployee, headers),
                        Employee.class,
                        readEmployee.getEmployeeId()).getBody();

        assertEmployeeEquivalence(readEmployee, updatedEmployee);
    }
    
    @Test
    @Rollback
    public void testUpdateEmployeeId()
    {
    	Employee testEmployee = new Employee();
        testEmployee.setFirstName("John");
        testEmployee.setLastName("Doe");
        testEmployee.setDepartment("Engineering");
        testEmployee.setPosition("Developer");

        // Create checks
        Employee createdEmployee = restTemplate.postForEntity(employeeUrl, testEmployee, Employee.class).getBody();

        assertNotNull(createdEmployee.getEmployeeId());
        assertEmployeeEquivalence(testEmployee, createdEmployee);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        Employee updatedEmployee =
                restTemplate.exchange(employeeIdUrl,
                        HttpMethod.PUT,
                        new HttpEntity<Employee>(createdEmployee, headers),
                        Employee.class,
                        5).getBody();
        
    }

    
    @Test
    @Rollback
    public void testReportingStructure_0_Reports()
    {
    	String employeeId = "62c1084e-6e34-4630-93fd-9153afb65309";
    	ReportingStructure reportingStructure = restTemplate.getForEntity(reportinStructureUrl, ReportingStructure.class, employeeId).getBody();
    	assertNotNull(reportingStructure.getEmployee());
    	Employee employee = reportingStructure.getEmployee();
    	assertEquals(employeeId, employee.getEmployeeId());
    	assertEquals("Pete", employee.getFirstName());
    	assertEquals("Best", employee.getLastName());
    	assertEquals("Developer II", employee.getPosition());
    	assertEquals("Engineering", employee.getDepartment());
    	assertTrue(CollectionUtils.isEmpty(employee.getDirectReports()));
    	assertEquals((Integer) 0, reportingStructure.getNumberOfReports());
    }
    
    @Test
    @Rollback
    public void testReportingStructure_MultipleTier_Reports()
    {
    	String employeeId = "16a596ae-edd3-4847-99fe-c4518e82c86f";
    	ReportingStructure reportingStructure = restTemplate.getForEntity(reportinStructureUrl, ReportingStructure.class, employeeId).getBody();
    	assertNotNull(reportingStructure.getEmployee());
    	Employee employee = reportingStructure.getEmployee();
    	assertEquals(employeeId, employee.getEmployeeId());
    	assertEquals("John", employee.getFirstName());
    	assertEquals("Lennon", employee.getLastName());
    	assertEquals("Development Manager", employee.getPosition());
    	assertEquals("Engineering", employee.getDepartment());
    	assertTrue(CollectionUtils.isNotEmpty(employee.getDirectReports()));
    	assertEquals((Integer) 4, reportingStructure.getNumberOfReports());
    }
    
    @Test
    @Rollback
    public void testReportingStructure_SingleTier_Reports()
    {
    	String employeeId = "03aa1462-ffa9-4978-901b-7c001562cf6f";
    	ReportingStructure reportingStructure = restTemplate.getForEntity(reportinStructureUrl, ReportingStructure.class, employeeId).getBody();
    	assertNotNull(reportingStructure.getEmployee());
    	Employee employee = reportingStructure.getEmployee();
    	assertEquals(employeeId, employee.getEmployeeId());
    	assertEquals("Ringo", employee.getFirstName());
    	assertEquals("Starr", employee.getLastName());
    	assertEquals("Developer V", employee.getPosition());
    	assertEquals("Engineering", employee.getDepartment());
    	assertTrue(CollectionUtils.isNotEmpty(employee.getDirectReports()));
    	assertEquals((Integer) 2, reportingStructure.getNumberOfReports());
    }
    
    @Test
    @Rollback
    public void testAddCompensation()
    {
    	Compensation comp = new Compensation();
    	comp.setEffectiveDate(LocalDate.now());
    	comp.setSalary(BigDecimal.ONE);
    	
    	String employeeId = "03aa1462-ffa9-4978-901b-7c001562cf6f";
    	
    	Employee employee = restTemplate.postForEntity(addCompensationUrl, comp, Employee.class, employeeId).getBody();
    	
    	assertTrue(CollectionUtils.isNotEmpty(employee.getCompensation()));
    	
    	Employee readEmployee = restTemplate.getForEntity(employeeIdUrl, Employee.class, employeeId).getBody();
        
    	assertEmployeeEquivalence(employee, readEmployee);
    	assertTrue(CollectionUtils.isNotEmpty(readEmployee.getCompensation()));
    	
    	
    }
    
    @Test
    @Rollback
    public void testAddCompensation_WithNewerCompensation()
    {
    	Compensation comp = new Compensation();
    	comp.setEffectiveDate(LocalDate.now());
    	comp.setSalary(BigDecimal.ONE);
    	
    	String employeeId = "16a596ae-edd3-4847-99fe-c4518e82c86f";
    	
    	Employee employee = restTemplate.postForEntity(addCompensationUrl, comp, Employee.class, employeeId).getBody();
    	
    	assertTrue(CollectionUtils.isNotEmpty(employee.getCompensation()));
    	
    	Compensation comp2 = new Compensation();
    	comp2.setSalary(BigDecimal.TEN);
    	comp2.setEffectiveDate(LocalDate.of(2020, 5, 20));
    	
    	ResponseEntity<String> invalidCompResponse = restTemplate.postForEntity(addCompensationUrl, comp2, String.class, employeeId);
    	
    	assertEquals(HttpStatus.BAD_REQUEST, invalidCompResponse.getStatusCode());
    	String body = invalidCompResponse.getBody();
    	assertTrue(body.startsWith("Failed to update employee"));
    	
    	Employee emp = new Employee();
    	emp.setEmployeeId(employeeId);
    	List<Compensation> comps = restTemplate.postForEntity(findCompensationUrl, emp, List.class).getBody();
    	
    	assertTrue(CollectionUtils.isNotEmpty(comps));
    	assertEquals(1, comps.size());
        
    	
    	
    }
    
    @Test
    @Rollback
    public void testCompBadId()
    {
    	Compensation comp2 = new Compensation();
    	comp2.setSalary(BigDecimal.TEN);
    	comp2.setEffectiveDate(LocalDate.of(2020, 5, 20));
    	
    	ResponseEntity<String> invalidCompResponse = restTemplate.postForEntity(addCompensationUrl, comp2, String.class, " ");
    	
    	assertEquals(HttpStatus.BAD_REQUEST, invalidCompResponse.getStatusCode());
    	
    	assertEquals("Employee ID must be specified.", invalidCompResponse.getBody());
    }
    
    @Test
    @Rollback
    public void testAddEmployee_With_InvalidDirectReports()
    {
    	Employee emp = new Employee();
    	emp.setFirstName("John");
    	emp.setLastName("Smith");
    	
    	Employee report1 = new Employee();
    	report1.setFirstName("Invalid");
    	report1.setLastName("Person");
    	report1.setEmployeeId("-999");
    	
    	emp.setDirectReports(Arrays.asList(report1));
    	
    	ResponseEntity<String> response = restTemplate.postForEntity(employeeUrl, emp, String.class);
    	
    	String body = response.getBody().toString();
    	
    	assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    	assertTrue(body.startsWith("Employee IDs [-999] were not found"));
    }
    
    @Test
    @Rollback
    public void testAddEmployee_AlreadyExists()
    {
    	Employee emp = new Employee();
    	emp.setEmployeeId("03aa1462-ffa9-4978-901b-7c001562cf6f");
    	emp.setFirstName("John");
    	emp.setLastName("Smith");
    	
    	ResponseEntity<String> response = restTemplate.postForEntity(employeeUrl, emp, String.class);
    	
    	String body = response.getBody().toString();
    	
    	assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    	assertEquals("Employee ID already exists. Employee ID must be unique.", body);
    }
    
    @Test
    public void testFindReportingStructure_NoId()
    {
    	String employeeId = " ";
    	ResponseEntity<ReportingStructure> response = restTemplate.getForEntity(reportinStructureUrl, ReportingStructure.class, employeeId);
    
    	assertEquals(HttpStatus.OK, response.getStatusCode());
    	assertNull(response.getBody().getEmployee());
    	assertNull(response.getBody().getNumberOfReports());
    	
    }

    private static void assertEmployeeEquivalence(Employee expected, Employee actual) {
        assertEquals(expected.getFirstName(), actual.getFirstName());
        assertEquals(expected.getLastName(), actual.getLastName());
        assertEquals(expected.getDepartment(), actual.getDepartment());
        assertEquals(expected.getPosition(), actual.getPosition());
    }
}
