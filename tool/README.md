# JSPC (JSP Compiler for Wildfly)

This is a clone of the tomcat JspC tool for pre-compiling JSPs (almost all the options have been replicated to have the same command syntax).

## How to build it

It is a maven project so just install or package it. Maven and JDK should be present in the system.

```
mvn clean package
```

## How to use it

For the moment the tool should be executed using the [exec-maven-plugin](https://www.mojohaus.org/exec-maven-plugin/). So the options should be passed using the `exec.args` system property of the exec plugin. For example, assuming the WAR exploded application is inside the directory `/path/to/webapp`, the following commands creates a `precompiled-jsp.jar` library with all the compiled classes in it:

```
mkdir -p /precompiled/classes/META-INF
mvn exec:java -Dexec.args="-v -p pre.compiled.jsps -d /precompiled/classes -webapp /path/to/webapp -webfrg /precompiled/classes/META-INF/web-fragment.xml"
cd /precompiled/classes/
jar cvf precompiled-jsp.jar *
```

The previous example searches all the JSP files inside the webapp application directory and compiles them inside the specified directory (specific JSP files can also passed to the tool). The option `-webfrg` creates a web fragment standard in the `META-INF` directory that is automatically parsed by wildfly (by spec). Just adding the JAR file inside the `WEB-INF/lib` of the app will avoid pre-compilation (the pre-compiled class will be used instead). The complete usage for the utiltity can be shown just using the `-help` option:

```
mvn exec:java -Dexec.args="-help"
```

If the application uses global libraries (wildfly modules, JARs inside an EAR file,...) that are not present in the WAR itself they can be provided to the tool using the `-classpath` option.

As commented before the implementation tries to follow the same syntax than the tomcat counterpart.

The packaging also generates a JAR file with the suffix `-with-dependencies` which contains the JspC and all the wildfly dependencies needed to execute the tool. That library can also be used with the same arguments.

```
java -jar jspc-1.0.0-SNAPSHOT-jar-with-dependencies.jar -help
```

## What wildfly versions are supposed to work

The utility has been done and tested with wildfly. Under the hood the maven project is configured to use the needed library versions that are present in the exact version used (jastow, metadata, servlet spec, jstl,...). The idea is the `pom.xml` can have different wildfly (and EAP) profiles to work with different versions. Currently several profiles have been added for wildfly and eap (the default one will point to the last wildfly version tested).

For example the `wildfly27` profile is defined like this:

```xml
        <profile>
            <id>wildfly27</id>
            <properties>
                <version.io.undertow.jastow.jastow>2.2.4.Final</version.io.undertow.jastow.jastow>
                <version.jakarta.servlet.servlet-api>6.0.0</version.jakarta.servlet.servlet-api>
                <version.org.eclipse.jdt.ecj>3.31.0</version.org.eclipse.jdt.ecj>
                <version.org.glassfish.expressly>5.0.0</version.org.glassfish.expressly>
                <version.org.jboss.metadata.jboss-metadata-web>15.2.0.Final</version.org.jboss.metadata.jboss-metadata-web>
                <version.jakarta.servlet.jsp.jsp-api>3.1.0</version.jakarta.servlet.jsp.jsp-api>
                <version.org.apache.logging.log4j>2.19.0</version.org.apache.logging.log4j>
                <version.jakarta.servlet.jsp.jstl.jstl-api>3.0.0</version.jakarta.servlet.jsp.jstl.jstl-api>
                <version.org.glassfish.web.jstl>3.0.0</version.org.glassfish.web.jstl>
                <version.org.wildfly.wildfly.dist>27.0.0.Final</version.org.wildfly.wildfly.dist>
            </properties>
        </profile>
```

Adding a newer version would need to find the exact version libraries used for that version of the wildfly server and create a new profile. For example to build and use the `eap80beta` that profile should be passed to the command line:

```
mvn clean package -P eap80beta
mvn exec:java -P eap80beta -Dexec.args="..."
```

The default `jakarta` branch is prepared to work with the new jakarta ee 10 compatible version of wildfly (since wildfly-27). The `javax` branch is for the old jakarta 9 versions although it is not updated anymore.

