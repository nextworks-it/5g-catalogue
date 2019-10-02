Added lines in nfv descriptors POM

<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-jar-plugin</artifactId>
    <version>3.0.2</version>
</plugin>

in order to work with openjdk9 and maven 3.6
need to check POMs in git public repo

Need to define a company strategy and structure to git folders when ansible installation is required:
-build locally or on target machine? 
-just putting the jar-exe file on the target machine or the entire code? 
-How generic has to be the installation (user/key/topics need to be choosen runtime?)
