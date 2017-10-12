//bin/true; /usr/bin/mvn compile -f "`dirname \"$0\"`/pom.xml" -q && CLASSPATH=`/usr/bin/mvn -f "\`dirname \"$0\"\`/pom.xml" exec:exec -q` java com.dynamodan.framework.CHTest $@
//bin/true; exec echo "Done"
/**
 * Chronessan Framework test stub
 * 
 * @author Dan Hartman <info@dynamodan.com>
 * @version 0.34
 * @since 2016-01-18
 */

package com.dynamodan.framework;
import com.dynamodan.framework.Chronessan;


public class CHTest extends Chronessan {
	
	public String version = "0.34";
	public static CHTest self;

	// dud constructor, must use self.instantiateMy(self, args) to force singleton behavior
	// otherwise, will throw an error like this: Constructor error: Can't find default constructor for: class prism_api
	protected CHTest() {	}
	
	public String getVersion() {
		return self.getClass().getSimpleName()+" v"+version;
	}

	public static void main( String[] args ) {
		// only need to do this once, here, and then we have a singleton self everywhere else
		self = new CHTest();
		self.instantiateMy(self, args);
		
	}

}
