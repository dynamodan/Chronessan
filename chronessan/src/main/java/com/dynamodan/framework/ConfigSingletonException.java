package com.dynamodan.framework;

// just a wrapper for Exception. ;)
public class ConfigSingletonException extends Exception {
	public ConfigSingletonException() {
		super();
	}
	public ConfigSingletonException(String msg) {
		super(msg);
	}
}
