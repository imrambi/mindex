package com.mindex.challenge.exception;

public class EmployeeDoesNotExistsException extends MindexException
{

	private static final long serialVersionUID = 1L;

	public EmployeeDoesNotExistsException(String message) {
		super(message);
	}

}
