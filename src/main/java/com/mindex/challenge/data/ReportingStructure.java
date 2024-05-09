package com.mindex.challenge.data;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class ReportingStructure {

	private Employee employee;
	private Integer numberOfReports;
}
