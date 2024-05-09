package com.mindex.challenge.data;

import java.util.List;

import org.springframework.data.annotation.Id;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString
@EqualsAndHashCode(exclude = { "compensation", "directReports" })
@JsonIgnoreProperties(ignoreUnknown = true)
public class Employee
{
	@Id
	private String employeeId;
	private String firstName;
	private String lastName;
	private String position;
	private String department;
	private List<Employee> directReports;
	private List<Compensation> compensation;

}
