package com.mindex.challenge.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.mindex.challenge.data.Compensation;
import com.mindex.challenge.data.Employee;
import com.mindex.challenge.data.ReportingStructure;
import com.mindex.challenge.exception.CompensationCanNotBeInPastException;
import com.mindex.challenge.exception.EmployeeDoesNotExistsException;
import com.mindex.challenge.exception.EmployeeIdAlreadyExistsException;
import com.mindex.challenge.exception.EmployeeIdNotSpecifiedException;
import com.mindex.challenge.service.EmployeeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;
    
    @Value("${contact.support.message}")
    private String contactSupport;

    @Operation(summary = "Create a new Employee based on the employee passed in")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Created the employee", 
    							content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
    							schema = @Schema(implementation = Employee.class))),
    		@ApiResponse(responseCode = "400", description = "Failed to create Employee. Message will contain reason. Could be"
    				+ " id already exists, direct reports do not exists, etc.",
    				content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE)),
    		@ApiResponse(responseCode = "500", description = "Internal server error. Contact support if this happens",
    				content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE))
    })
    @PostMapping(value = "/employee", produces = MediaType.APPLICATION_JSON_VALUE)
    @SuppressWarnings("rawtypes")
    public ResponseEntity create(@RequestBody Employee employee) {
        log.debug("Received employee create request for [{}]", employee);
        
        try
        {
        	return ResponseEntity.ok().body(employeeService.create(employee));
        }
        catch (EmployeeIdAlreadyExistsException eiaee)
        {
        	log.info("Employee ID " + employee.getEmployeeId() + " already exists. Unable to create employee: " + employee.toString());
        	return ResponseEntity.badRequest().body("Employee ID already exists. Employee ID must be unique.");
        }
        catch (EmployeeDoesNotExistsException ednee)
        {
        	log.info("Invalid employee ID,", ednee);
        	return ResponseEntity.badRequest().body(ednee.getLocalizedMessage());
        }
        catch (Exception e)
        {
        	log.info("Caught error while creating Employee: " + employee.toString());
        	return ResponseEntity.internalServerError().body("Error while creating employee " + employee.toString() + ". " + contactSupport);
        }
    }

    @Operation(summary = "Retrieves an employee record based on the employee ID")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Found the employee, or no object if not found", 
    							content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
    							schema = @Schema(implementation = Employee.class))),
    		@ApiResponse(responseCode = "500", description = "Internal server error. Contact support if this happens",
    				content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE))
    })
    @SuppressWarnings("rawtypes")
	@GetMapping(value = "/employee/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity read(@PathVariable("id") String id) {
        log.debug("Received employee create request for id [{}]", id);

        try
        {
        	return ResponseEntity.ok(employeeService.read(id));
        }
        catch (Exception e)
        {
        	log.info("Caught error while fetching for employee {}", id, e);
        	return ResponseEntity.internalServerError().body("Unable to fetch employee for employee ID: " + id + ". " + contactSupport);
        }
    }

    @Operation(summary = "Modifies the employee record that is passed in to the id that is used in the path.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Modified the employee", 
    							content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
    							schema = @Schema(implementation = Employee.class))),
    		@ApiResponse(responseCode = "400", description = "Failed to update the employee. Data is incorrect. This could be "
    				+ "the new employee id is already in use, or the existing employee id does not exists. Error message will "
    				+ "contain information on the cause.",
    				content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE)),
    		@ApiResponse(responseCode = "500", description = "Internal server error. Contact support if this happens",
    				content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE))
    })
    @SuppressWarnings("rawtypes")
	@PutMapping(value = "/employee/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity update(@PathVariable("id") String id, @RequestBody Employee employee) {
        log.debug("Received employee create request for id [{}] and employee [{}]", id, employee);
        
        try
        {
        	return ResponseEntity.ok(employeeService.update(id, employee));
        }
        catch (EmployeeIdAlreadyExistsException | EmployeeDoesNotExistsException eiaee)
        {
        	log.info("Error with data passed in", eiaee);
        	return ResponseEntity.badRequest().body("Unable to update Employee ID to " + id + ". " + eiaee.getLocalizedMessage());
        }
        catch (Exception e)
        {
        	log.info("Caught error while changing employee ID from {} to {}: {}", employee.getEmployeeId(), id, employee.toString(), e);
        	return ResponseEntity.internalServerError().body("Unable to update employee ID " + employee.getEmployeeId() + ". " + contactSupport);
        }
    }
    

    @Operation(summary = "Retreives the employee and how many total direct reports for this employee")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "If employee is found, then the response will have the "
    		+ "employee and how many director reports. If the employee is not found, values will be null", 
    							content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
    							schema = @Schema(implementation = ReportingStructure.class))),
    		@ApiResponse(responseCode = "500", description = "Internal server error. Contact support if this happens",
    				content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE))
    })
    @SuppressWarnings("rawtypes")
	@GetMapping(value = "/reportingStructure/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity findReportingStructure(@PathVariable("id") String id)
    {
    	try
    	{
    		return ResponseEntity.ok(employeeService.findReportingStructure(id));
    	}
    	catch (Exception e)
    	{
    		log.info("Caught error fetching reporting structure.", e);
    		return ResponseEntity.internalServerError().body("Unable to find reporting structure. " + contactSupport);
    	}
    	
    }
    
    @Operation(summary = "Adds compensation record to the employee")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "If employee is found, it will add the compensation and return the employee", 
    							content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
    							schema = @Schema(implementation = Employee.class))),
    		@ApiResponse(responseCode = "400", description = "Invalid employee ID. See message for details.",
    					content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE)),
    		@ApiResponse(responseCode = "500", description = "Internal server error. Contact support if this happens",
    				content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE))
    })
    @SuppressWarnings("rawtypes")
	@PostMapping(value = "/compensation/add/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity addCompensation(@PathVariable("id") String id, @RequestBody @Valid Compensation compensation)
    {
    	try
    	{
    		return ResponseEntity.ok(employeeService.addCompensation(id, compensation));
    	}
    	catch (EmployeeDoesNotExistsException | EmployeeIdNotSpecifiedException badIdException)
    	{
    		log.info("Invalid employeeID.", badIdException);
    		return ResponseEntity.badRequest().body(badIdException.getLocalizedMessage());
    	}
    	catch (CompensationCanNotBeInPastException inPastException)
    	{
    		log.info("Compensation in the past.", inPastException);
    		return ResponseEntity.badRequest().body(inPastException.getLocalizedMessage());
    	}
    	catch (Exception e)
    	{
    		log.info("Caught error adding compensation.", e);
    		return ResponseEntity.internalServerError().body("Unable to add compensation record. " + contactSupport);
    	}
    	
    }
    
    @Operation(summary = "Retrieves all compensation records for the employee specified")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Return a list ordered by date ascending of compensation.", 
    							content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
    							schema = @Schema(implementation = List.class))),
    		@ApiResponse(responseCode = "400", description = "Invalid employee ID. See message for details.",
    					content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE)),
    		@ApiResponse(responseCode = "500", description = "Internal server error. Contact support if this happens",
    				content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE))
    })
    @SuppressWarnings("rawtypes")
	@PostMapping(value = "/compensation/", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity findCompensation(@RequestBody Employee employee)
    {
    	try
    	{
    		return ResponseEntity.ok(employeeService.findCompensation(employee));
    	}
    	catch (EmployeeDoesNotExistsException | EmployeeIdNotSpecifiedException badIdException)
    	{
    		log.info("Invalid employeeID.", badIdException);
    		return ResponseEntity.badRequest().body(badIdException.getLocalizedMessage());
    	}
    	catch (Exception e)
    	{
    		log.info("Caught error finding compensation.", e);
    		return ResponseEntity.internalServerError().body("Unable to add compensation record. " + contactSupport);
    	}
    	
    }
    
    
}
