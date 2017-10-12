// a wrapper around the excellent apache log4j system, as per the recommendation
// from the javadoc page here:
// https://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/Category.html#log(org.apache.log4j.Priority, java.lang.Object)
// (look at the log function's third overload form, that has the most parameters,
// where the description says "It is intended to be invoked by wrapper classes")
package com.dynamodan.loghelper;

import org.apache.log4j.*;
import org.apache.commons.cli.*;
import java.lang.management.*;
import java.io.*;

public class Wrapper {
	private static Wrapper instance = null;
	private static Logger LOG = Logger.getLogger(Wrapper.class);
	private static StringWriter warnings = null;
	private static CommandLine cmd = null;
	private static WriterAppender wa = null;
	private static String lastLogString = "";
	private static Integer repeatCount = 0;
	
	
	public static void logSetup() {
		String logPropertyName = "generic.log4j.properties";
		logSetup(logPropertyName);
	}
	
	public static void logSetup(String logPropertyName) {
		logSetup(logPropertyName, null);
	}
	
	public static void logSetup(String logPropertyName, String logFolder) {
		String path = Wrapper.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		
		File propFile = new File(path + logPropertyName);

		// check if the properties file is an absolute path or relative:
		if((new File(logPropertyName)).isAbsolute()) {
			propFile = new File(logPropertyName);
		}
		
		String logPath = logFolder;
		System.out.println("Will use log folder "+logFolder);
		
		// if no logPath, get the path to the property file, and
		// use that for the log.folder system property:
		if(logFolder == null) { logPath = propFile.getParent(); }
		if(!logPath.matches("\\/$")) { logPath = logPath+"/"; }
		
		System.setProperty("log.folder", logPath);
		
		System.out.println("Setting up log from "+propFile.getAbsolutePath());
		if(!propFile.exists()) {
			System.out.println("Can't locate log properties path "+propFile.getAbsolutePath());
			return;
		}
		
		PropertyConfigurator.configure(propFile.getAbsolutePath());
		MDC.put("PID", getPID());
	}
	
	protected Wrapper() { }
	
	public static Wrapper getInstance() {
		if(instance == null) {
			Wrapper instance = new Wrapper();
			instance.logSetup();
		}
		return instance;
	}
	
	public static Wrapper getInstance(String logPropertyName) {
		return getInstance(logPropertyName, null);
	}
	
	public static Wrapper getInstance(String logPropertyName, String logFolder) {
		if(instance == null) {
			instance = new Wrapper();
		}
		System.out.println("Setting up logging helper using "+logPropertyName);
		instance.logSetup(logPropertyName, logFolder);
		return instance;
	}
	
	public void setCallerWriter(StringWriter callerWriter) {
		this.warnings = callerWriter;
	}
	
	public void setCmd(CommandLine callerCmd) {
		this.cmd = callerCmd;
	}
	
	
	public static void log(String message) {
		//Wrapper inst = getInstance();
		if(lastLogString.equals(message)) {
			repeatCount++;
		} else {
			if(repeatCount > 0) {
				LOG.log(Level.INFO, lastLogString+" [repeated "+repeatCount+" times]", null);
				repeatCount = 0;
			}
			LOG.log(Wrapper.class.getCanonicalName(), Level.INFO, message, null);
			lastLogString = message;
		}
	}
	
	public static void log(Object obj) {
		log(obj.toString());
	}
	
	public static void debug (String msg) {
		if(getInstance().cmd == null) { return; }
		if(cmd.hasOption("debug")) {
			LOG.log(Wrapper.class.getCanonicalName(), Level.INFO, msg, null);
		}
	}
	
	public static void debug (Object obj) {
		debug(obj.toString());
	}
	
	public static void warning(Object obj) {
		warning(obj.toString());
	}

	public static void warning (String msg) {
		Wrapper inst = getInstance();
		if(inst.warnings == null) { return; }
		if(inst.wa == null) {
			Logger rl = LogManager.getRootLogger();
			Appender fa = rl.getAppender("FA");
			PatternLayout appenderLayout = (PatternLayout) fa.getLayout();
			String warnPattern = appenderLayout.getConversionPattern() + "|||";
			PatternLayout warnLayout = new PatternLayout();
			warnLayout.setConversionPattern(warnPattern);
			wa = new WriterAppender(warnLayout, warnings);
			rl.addAppender(wa);
		}
		
		inst.wa.setThreshold(Level.ALL);
		LOG.log(Wrapper.class.getCanonicalName(), Level.INFO, msg, null);
		inst.wa.setThreshold(Level.OFF);
		
		// self.warnings.add(msg);
	}

	
	private static String getPID() {
	  // RuntimeMXBean rt = ManagementFactory.getRuntimeMXBean();
	  // return rt.getName();
	  String fn = "";
	  try {
	  		fn = new File("/proc/self").getCanonicalFile().getName();
	  }
	  
	  catch (IOException ioe) {
	  	  System.out.println(ioe.getMessage());
	  }
	  
	  return fn;
	  
	}
	
}

