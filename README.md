# Chronessan
Java Utility Library & Maven Archetype

# Requires
linux, java 8, maven, bash

# Synopsis
To build and install the library into your local maven repository, first
pull or download the files, cd to the top directory and then:
```
cd chronessan-archetype
mvn install
```

To generate a stub from the archetype that you can build on:
```
mkdir NewProgram
cd NewProgram
mvn archetype:generate -DgroupId=com.dynamodan -DartifactId=MyUtility -DarchetypeGroupId=com.dynamodan -DarchetypeVersion=1.0-SNAPSHOT -DarchetypeArtifactId=chronessan-archetype -DinteractiveMode=false
cd MyUtility
ln -s src/main/java/com/dynamodan/MyUtility.java MyUtility.java
chmod 755 MyUtility.java
```

Then in bash shell, because of the special java shebang line you can do things like this:
```
./MyUtility.java
```

so then for example, to run it in repl mode:
```
./MyUtility.java --repl
```

The stub generated in this example is then a new java class that extends the `Chronessan` java class, 
which encapsulates many handy features including config file loading, logging, an email warning system, and
many convenience functions such as connecting to mysql databases, dumping objects in json format, providing a 
beanshell read-eval-print loop (repl), and reading command line switches.
