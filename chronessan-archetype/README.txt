# you can use this maven archetype like this:

mvn archetype:generate -DgroupId=com.dynamodan -DartifactId=Snapper -DarchetypeGroupId=com.dynamodan -DarchetypeVersion=1.0-SNAPSHOT -DarchetypeArtifactId=chronessan-archetype -DinteractiveMode=false
cd Snapper

# and then you'll have a Snapper.java file that you can symbolic link to:

ln -s src/main/java/com/dynamodan/Snapper.java Snapper.java
chmod 755 Snapper.java

# and then to run it in repl mode:
./Snapper.java --repl
