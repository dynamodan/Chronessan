/**
 * Chronession Framework
 * <p>
 * A minimal java framework to wrap a beanshell, config, logging,
 * and other commonly used stuff.  Inspired by a favorite perl construct
 * from days of yore
 * 
 * @author Dan Hartman <info@dynamodan.com>
 * @version 0.01
 * @since 2016-01-18
 */

package com.dynamodan.framework;

// java primitives:
import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Date;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Properties;
import java.net.InetAddress;

// command line and repl stuff:
import org.apache.commons.cli.*;
import bsh.Interpreter;
import bsh.EvalError;
import jline.console.ConsoleReader;
import jline.TerminalFactory;
import jline.UnixTerminal;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JsonHierarchicalStreamDriver;

// mssql libs:
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;

// logging stuff:
import com.dynamodan.loghelper.Wrapper;
import static com.dynamodan.loghelper.Wrapper.log;
import static com.dynamodan.loghelper.Wrapper.warning;
import static com.dynamodan.loghelper.Wrapper.debug;
import org.apache.log4j.*;

// yaml dumper:
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.DumperOptions;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;

import com.dynamodan.framework.ConfigSingleton; // the config is a singleton so that SoapCredentialLoader can get it
import com.dynamodan.framework.ConfigSingletonException;

// smtp so we can send out warnings:
import javax.mail.*;
import javax.activation.*;

public class Chronessan {
	public static Chronessan self = null;
	protected String version = "0.01";
	public Options options = new Options();
	public StringWriter warnings = new StringWriter();
	public CommandLine cmd = null;
	public Object config = null;
	public Connection mssql_connection = null;
	public Connection mysql_connection = null;
	public Connection MariaDBConnection = null;
	public LinkedHashMap<String, LinkedHashMap<String, Object>> sqlMetaData = new LinkedHashMap();
	
	
	/**
	 * Beginning of methods that are intended to be overridden:
	 */
	public String setLogPropName() {
		return self.getProgramClass()+".log4j.properties";
	}
	
	public String getCwd() {
		String cwd = self.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
		// determine if we're running from jar or class directory:
		File jarFile = new File(cwd);
		if(!jarFile.isDirectory()) {
			cwd = jarFile.getParent();
		}
		
		boolean IS_WINDOWS = System.getProperty( "os.name" ).contains( "indow" );
		String osAppropriatePath = cwd;
		// lop off the leading slash in some cases:
		if(IS_WINDOWS && (cwd.indexOf("\\") == 0 || cwd.indexOf("/") == 0)) { osAppropriatePath = cwd.substring(1); }
		
		return osAppropriatePath;
	}

	public void setCliOptions() {
		
	}
	
	public String setConfigFileName() {
		return self.getProgramClass()+".yaml.conf";
	}
	
	public String getVersion() {
		return self.getProgramClass()+" v"+self.version;
	}
	
	public String getProgramName() {
		return self.getProgramClass()+".java";
	}
	
	public String getProgramClass() {
		return self.getClass().getSimpleName();
	}
	
	public void configureBeanShell(Interpreter i) {
		// for example,
		try {
			i.set("somevar", "fubar-baz");
		} catch (EvalError e) {
			log(e.getMessage());
		}
	}
	
	// secure and release pid file for concurrency checking.  Individual scripts implement this
	public void securePID() {
		return;
	}
	
	public void releasePID() {
		return;
	}
	
	public void run() {
		// nothing to do here, you should override this in your derived class
	}
	
	// send out an email:
	public void report() {
		LinkedHashMap config = null;
		
		if(this.config instanceof LinkedHashMap) {
			config = (LinkedHashMap) self.config;
		} else {
			log("config should be a LinkedHashMap.  It was a "+this.config.getClass());
			log("Can't report anything.");
			return;
		}
		
		// Recipient's email ID needs to be mentioned.
    	String to = (String) config.get("email_recipient");
    	
    	// Sender's email ID needs to be mentioned
    	String from = (String) config.get("email_sender");
    	
    	// SMTP host:
    	String host = (String) config.get("smtp_host");
    	final String pass = (String) config.get("smtp_pass");
    	final String user = (String) config.get("smtp_user");
    	
    	// set up the properties
    	Properties properties = new Properties();
    	properties.put("mail.smtp.host", host);
    	
		// set up auth:
		javax.mail.Authenticator auth = new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(user, pass);
			}
		};
		if(pass != null) {
    		properties.put("mail.smtp.auth", true);
    	}
		
		// Get a Session object.
		Session session = null;
		
		if(pass != null) {
			debug("getting smtp session with auth");
			session = Session.getInstance(properties, auth);
		} else {
			debug("getting smtp session without auth");
			session = Session.getInstance(properties);
		}
		if(self.cmd.hasOption("debug")) { session.setDebug(true); }

		try {
			// Create a default MimeMessage object.
			javax.mail.internet.MimeMessage message = new javax.mail.internet.MimeMessage(session);
			
			// Set From: header field of the header.
			message.setFrom(new javax.mail.internet.InternetAddress(from));
			
			// Set To: header field of the header.
			message.addRecipients(Message.RecipientType.TO, to);
			
			String hostip = "";
			try {
				hostip = " on "+InetAddress.getLocalHost().toString();
			} catch (java.net.UnknownHostException e) {
				// do nothing
			}
			
			// set up the subject and body:
			if(self.warnings.getBuffer().length() > 0) {
				message.setSubject("ALERT: "+self.getProgramName()+hostip+" ran with warnings");
				String body = "";
				for(String w : self.warnings.toString().split("\\|{3}")) {
					body = body + w;
				}
				message.setText("The java program ran with the following warnings:\n\n" + body);
			} else {
				message.setSubject("Report from "+self.getProgramName()+hostip);
				message.setText("The java program ran ok without warnings. (This message may be expanded to include more information in the future)");
			}
			
			// Now set the actual message
			
			// Send message
			Transport.send(message);
			System.out.println("Sent message successfully....");
		}
      
		catch(AuthenticationFailedException authError) {
			warning("Got an SMTP authentication error: "+authError.getMessage());
		}
      
		catch (MessagingException mex) {
		   
			warning("Got an SMTP error: "+mex.getMessage());
		}
		
		catch (NullPointerException e) {
			warning("Got a NullPointerException trying to send report: "+e.getMessage());
			warning("Perhaps there are problems with your SMTP config. Stack trace:");
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			warning(sw.toString());
		}
	}
	
	/**
	 * end of methods that are intended to be overridden
	 */
	 
	 

	public void instantiateMy(Chronessan instance, String[] args) {
		
		this.self = instance;
		this.self.version = instance.version;
        
		// set up log wrapper
		Wrapper lw = Wrapper.getInstance(instance.getCwd()+"../../"+instance.setLogPropName(), instance.getCwd()+"../../");
		lw.setCallerWriter(instance.warnings); // this enables us to capture warnings so they can be sent
        
		// set up to read the common cli switches:
		instance.options.addOption(new Option("repl", "repl", false, "Enter a Beanshell Read-Eval-Print-Loop"));
		instance.options.addOption(new Option("debug", "debug", false, "Print debugging messages to screen and log"));
		instance.options.addOption(new Option("d", "dryrun", false, "Do a dry run, i.e. read-only, no changes"));
		instance.options.addOption(new Option("h", "help", false, "display this help message"));
		
		// set up to read the custom cli switches:
		instance.setCliOptions();
		
		CommandLineParser parser = new BasicParser();
		try {                                                                          
			instance.cmd = parser.parse(instance.options, args);
			lw.setCmd(instance.cmd);
		} catch (ParseException e) {
			warning("Couldn't parse command line:");
			warning(e.getMessage());
			System.exit(1);
		}
		
		if(instance.cmd.hasOption("help")) {
			System.out.println("");
			HelpFormatter hf = new HelpFormatter();
			hf.printHelp(80, "./"+instance.getProgramName()+" [options]", "==================================", options, "==================================");
			System.out.println("");
			System.exit(0);
		}
		
		log("Starting up...");
		
		// load the configuration yaml:
		instance.loadConfig();

		// get the root logger:
		Logger rl = LogManager.getRootLogger();
		
		// write a pid file, if so equipped:
		instance.securePID();
		
		// start a JLine-wrapped beanshell:
		if(instance.cmd.hasOption("repl")) {
			
			// turn off console logging, it doesn't need to echo into the
			// repl.  It already echos via beanshell and ConsoleReader
			ConsoleAppender appender = (ConsoleAppender) rl.getAppender("CA");
			// appender.setThreshold(Level.OFF);
			
			// set a new layout that only echoes the message, and maybe the pid:
			PatternLayout appenderLayout = (PatternLayout) appender.getLayout();
			String origPattern = appenderLayout.getConversionPattern();
			appenderLayout.setConversionPattern("[%X{PID}] %m%n");
			// appender.setLayout(newLayout);
			
			
			// set up the console reader:
			ConsoleReader console = null;
			
			try {
				console = new ConsoleReader();
			} catch (IOException e) {
				warning("Got IOException: ");
				warning(e.getMessage());
			}
			
			// set up a Beanshell interpreter:
			Interpreter i = new bsh.Interpreter();
			try {
				i.set("console", console);
				i.set("cmd", instance.cmd);
				i.set("version", instance.version);
				i.set("self", instance);
				i.set("lw", lw);
				
				instance.configureBeanShell(i);
				
				i.eval("void exit() { print(\"Use CTRL-d to exit.\"); }");
			} catch (EvalError e) {
				log(e.getMessage());
			}
			
			System.out.println("Press CTRL-c or CTRL-d (or type quit) to exit.");
			
			console.setHandleUserInterrupt(true);
			console.setPrompt("beanshell> ");
            
			String line = null;
			Object evalResult = null;
			int maxChar = 300;

			try {
				while ((line = console.readLine()) != null) {
					try {
						
						appender.setThreshold(Level.OFF);
						log("beanshell> "+line);
						if("quit".equals(line)) {
							log("Bye");
							break;
						}
						appender.setThreshold(Level.ALL);
						
						
						evalResult = i.eval(line);
						if(evalResult != null) {  // check first, it could be null!!
							// console.println(evalResult.toString());
							String logString = evalResult.toString();
							int maxLength = (logString.length() < maxChar)?logString.length():maxChar;
							log("beanshell> "+logString.substring(0,maxLength));
						}
					}	catch (EvalError e) {
						String errstr = e.getMessage();
						warning("beanshell> "+errstr);
						// console.println(errstr);
					}	catch (Exception e) {
						String errstr = e.getMessage();
						warning("beanshell> "+errstr);
					}
					
				}
				
			} catch(IOException e) {
				warning("Got IOException: ");
				warning(e.getMessage());
			} catch (jline.console.UserInterruptException e) {
				appender.setThreshold(Level.ALL);
				log("Caught CTRL-c");
			} finally {
				try {
					TerminalFactory.get().restore();
				} catch(Exception e) {
					e.printStackTrace();
				}
			}

			appenderLayout.setConversionPattern(origPattern);
			appender.setThreshold(Level.ALL);
			log("exiting beanshell");
			
		}
		
		else {
			// no repl or anything that would stop us, so, just go right into the run function:
			instance.run();
		}
		
		int warningSize = instance.warnings.toString().split("\\|{3}").length;
		int warningsLength = instance.warnings.toString().length();
		if(warningsLength > 0) {
			log("Ran successfully, but with "+warningSize+" warnings.");
			
			// setup an SMTP connection, and mail out the warning:
			instance.report();
		} else {
			log("Run completed without warnings.");
		}
		
		instance.releasePID();
		
	}
	
		// try to load the config.  Return an empty object if not.  (TODO, make this throw an exception in true java style!)
	// this should actually return a java.util.LinkedHashMap
	public void loadConfig() {
		String configPath = self.getCwd()+"../../"+self.setConfigFileName();
		ConfigSingleton cnf = ConfigSingleton.getInstance();
		cnf.setConfigPath(configPath);
		
		try {
			self.config = cnf.loadConfig();
		}
		
		catch (ConfigSingletonException e) {
			System.out.println(e.getMessage());
			warning(e.getMessage());
		}
		
	}
	
	// get a mssql connection.  This assumes that loadConfig() has already been called.
	public Connection getMssqlConnection() {
		LinkedHashMap config = null;
		if(this.mssql_connection != null) {
			return this.mssql_connection;
		}
		
		if(this.config == null) {
			log("Configuration not loaded yet for getMssqlConnection.  Run loadConfig() first.");
			return this.mssql_connection;
		}
		
		if(this.config instanceof LinkedHashMap) {
			config = (LinkedHashMap) this.config;
		} else {
			log("config should be a LinkedHashMap.  It was a "+this.config.getClass());
			return this.mssql_connection;
		}
		
		if(config.get("mssql_host") == null) {
			log("Configuration doesn't contain mssql_host setting.");
		} else {
			try {
				String port;
				if(config.get("mssql_port") == null) { port = ""; } else { port = ":" + config.get("mssql_port").toString(); }
				Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
				this.mssql_connection = DriverManager.getConnection(
					"jdbc:sqlserver://"+config.get("mssql_host") + port
					+";user="+config.get("mssql_user")
					+";password="+config.get("mssql_pass")
					+";database="+config.get("mssql_schema")
				);
			} catch (ClassNotFoundException e) {
				warning("ClassNotFound: "+e.getMessage());
				
			} catch (SQLException e) {
				warning(e.getMessage());
			}
		}
		return this.mssql_connection;
	}
	
	// get a mysql connection to a MariaDB db.  This assumes that loadConfig() has already been called.
	public Connection getMariaDBConnection() {
		LinkedHashMap config = null;
		if(this.MariaDBConnection != null) {
			return this.MariaDBConnection;
		}
		
		if(this.config == null) {
			log("Configuration not loaded yet for getMariaDBConnection.  Run loadConfig() first.");
			return this.MariaDBConnection;
		}
		
		if(this.config instanceof LinkedHashMap) {
			config = (LinkedHashMap) this.config;
		} else {
			log("config should be a LinkedHashMap.  It was a "+this.config.getClass());
			return this.MariaDBConnection;
		}
		
		if(config.get("mysql_host") == null) {
			log("Configuration doesn't contain mysql_host setting.");
		} else {
			try {
				String port;
				if(config.get("mysql_port") == null) { port = ""; } else { port = ":" + config.get("mysql_port").toString(); }
				//Class.forName("org.mariadb.jdbc.Driver");
				String url = "jdbc:mariadb://"+config.get("mysql_host") + port + "/" + config.get("mysql_schema")+"?zeroDateTimeBehavior=convertToNull;alwaysAutoGeneratedKeys=TRUE";
				this.MariaDBConnection = DriverManager.getConnection(url, (String) config.get("mysql_user"), (String) config.get("mysql_pass"));
			} catch (SQLException e) {
				warning(e.getMessage());
			} catch (Exception e) {
				warning(e.getMessage());
			}
		}
		return this.MariaDBConnection;
	}
	
	// get a mysql connection.  This assumes that loadConfig() has already been called.
	public Connection getMysqlConnection() {
		LinkedHashMap config = null;
		if(this.mysql_connection != null) {
			return this.mysql_connection;
		}
		
		if(this.config == null) {
			log("Configuration not loaded yet for getMysqlConnection.  Run loadConfig() first.");
			return this.mysql_connection;
		}
		
		if(this.config instanceof LinkedHashMap) {
			config = (LinkedHashMap) this.config;
		} else {
			log("config should be a LinkedHashMap.  It was a "+this.config.getClass());
			return this.mysql_connection;
		}
		
		if(config.get("mysql_host") == null) {
			log("Configuration doesn't contain mysql_host setting.");
		} else {
			try {
				String port;
				if(config.get("mysql_port") == null) { port = ""; } else { port = ":" + config.get("mysql_port").toString(); }
				Class.forName("com.mysql.jdbc.Driver");
				String url = "jdbc:mysql://"+config.get("mysql_host") + port + "/" + config.get("mysql_schema");
				this.mysql_connection = DriverManager.getConnection(url, (String) config.get("mysql_user"), (String) config.get("mysql_pass"));
			} catch (ClassNotFoundException e) {
				warning("ClassNotFound: "+e.getMessage());
				
			} catch (SQLException e) {
				warning(e.getMessage());
			}
		}
		return this.mysql_connection;
	}
	
	/**
		when given a ResultSet (after the .next() function was called), reads a row and fetches each field into a string
	*/
	public LinkedHashMap<String, String> getRowHash(ResultSet rs) {
		// cache some metadata because it's probably expensive and may run calls on the sql connection:
		String ihc = Integer.toHexString(System.identityHashCode(rs));
		LinkedHashMap<String, Object> hm = new LinkedHashMap();
		LinkedHashMap<String, String> output = new LinkedHashMap();
		try {
			if(self.sqlMetaData.get(ihc) == null) {
				ResultSetMetaData md = rs.getMetaData();
				int colCount = md.getColumnCount();
				hm = new LinkedHashMap();
				hm.put("metaData", md);
				hm.put("colCount", new Integer(colCount));
				self.sqlMetaData.put(ihc, hm);
			} else {
				hm = self.sqlMetaData.get(ihc);
			}
			
			Integer colCount = (Integer) hm.get("colCount");
			for(int i = 1; i <= colCount; i++) {
				String fieldValue = rs.getString(i);
				String fieldName = ((ResultSetMetaData) hm.get("metaData")).getColumnLabel(i);
				if(fieldValue == null) {
					output.put(fieldName, "");
				} else {
					output.put(fieldName, fieldValue);
				}
			}
		} catch (SQLException e) {
			warning(e.getMessage());
			return output;
		}
		return output;
		
	}

	// utility functions to try dumping out java objects, useful in beanshell
	// an inspector like Data::Dumper
	
	// Pretty-Print XML:
	public String pp (Object o) {
		XStream dumper = new XStream();
		return dumper.toXML(o);
	}
	
	// Pretty-Print Yaml: 
	public String ppy (Object o) {
		DumperOptions options = new DumperOptions();
		options.setCanonical(false);
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		options.setExplicitStart(true);
		options.setExplicitEnd(false);
		// options.setWidth(YAML_LINE_WIDTH);

		Yaml yaml = new Yaml(options);
		return yaml.dump(o);
	}
	
	// Pretty-Print JSON:
	public String ppj (Object o) {
		XStream dumper = new XStream(new JsonHierarchicalStreamDriver());
		return dumper.toXML(o);
	}
}

