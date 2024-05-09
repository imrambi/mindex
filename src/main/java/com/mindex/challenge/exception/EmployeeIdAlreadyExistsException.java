package com.mindex.challenge.exception;

public class EmployeeIdAlreadyExistsException extends MindexException
{

	private static final long serialVersionUID = 1L;

	public EmployeeIdAlreadyExistsException(String message) {
		super(message);
	}

}
