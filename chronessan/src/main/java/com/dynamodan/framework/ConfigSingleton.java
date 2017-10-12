package com.dynamodan.framework;

import java.io.*;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;
import com.dynamodan.framework.ConfigSingletonException;

public class ConfigSingleton {
	private static ConfigSingleton instance = null;
	private static String configPath = null;
	
	protected ConfigSingleton() {
		
	}
	
	public static ConfigSingleton getInstance() {
		if(instance == null) {
			instance = new ConfigSingleton();
		}
		return instance;
	}
	
	public void setConfigPath(String path) {
		getInstance().configPath = path;
	}
	
	public String getConfigPath() {
		return getInstance().configPath;
	}
	
	// try to load the config.  Return an empty object if not.  (TODO, make this throw an exception in true java style!)
	// this should actually return a java.util.LinkedHashMap
	public static Object loadConfig() throws ConfigSingletonException  {
		if(getInstance().configPath == null) {
			// System.out.println("configPath not set.  Use setConfigPath() to set it.");
			throw new ConfigSingletonException("configPath not set.  Use setConfigPath() to set it.");
		}
		
		Yaml yaml = new Yaml();
		String document = null;
		FileReader fr = null;
		
		try {
			fr = new FileReader(getInstance().configPath);
		}
		
		catch (FileNotFoundException e) {
			System.out.println(e.getMessage());
			return new Object();
		}
		
		try {
			return yaml.load(fr);
		}
		
		catch (YAMLException e) {
			System.out.println(e.getMessage());
			return new Object();
		}
		
	}	
	
}
