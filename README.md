# JSPC (JSP Compiler for Wildfly)

This is a clone of the tomcat JspC tool for pre-compiling JSPs (almost all the options have been replicated to have the same command syntax).

## How to build it

It is a maven project so just install or package it. Maven and JDK should be present in the system.

```
mvn clean package
```

## How to use it

For the moment the tool should be used using the [exec-maven-plugin](https://www.mojohaus.org/exec-maven-plugin/). So the options should be passed using the `exec.args` system property of the exec plugin. For example, assuming the WAR exploded application is inside the directory `/path/to/webapp`, the following commands creates a `precompiled-jsp.jar` library with all the compiled classes in it:

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

As comented before the implementation tries to follow the same syntax than the tomcat counterpart.

## What wildfly versions are supposed to work

The utility has been done and tested with wildfly 18. Under the hood the maven project is configured to use the needed library versions that are present in that version (jastow, metadata, servlet spec, jstl and jsf modules). The idea is the `pom.xml` can have different wildfly (and EAP) profiles to work with different versions. Currently only two profiles have been added: `wildfly18` and `eap72` (`wildfly18` is the default one). In order to add a new version of wildfly a new profile should be added with the versions used in that specific release.

For example the `wildfly18` profile is defined like this:

```xml
<profile>
    <id>wildfly18</id>
    <properties>
        <version.io.undertow.jastow.jastow>2.0.7.Final</version.io.undertow.jastow.jastow>
        <version.org.jboss.metadata.jboss-metadata-web>13.0.0.Final</version.org.jboss.metadata.jboss-metadata-web>
        <version.org.jboss.spec.javax.servlet.jsp.jboss-jsp-api>2.0.0.Final</version.org.jboss.spec.javax.servlet.jsp.jboss-jsp-api>
        <version.org.jboss.logmanager.log4j-jboss-logmanager>1.2.0.Final</version.org.jboss.logmanager.log4j-jboss-logmanager>
        <version.org.apache.taglibs.taglibs-standard-spec>1.2.6-RC1</version.org.apache.taglibs.taglibs-standard-spec>
        <version.org.jboss.spec.javax.faces.jboss-jsf-api>3.0.0.Final</version.org.jboss.spec.javax.faces.jboss-jsf-api>
        <version.com.sun.faces.jsf-impl>2.3.9.SP03</version.com.sun.faces.jsf-impl>
    </properties>
</profile>
```

Adding a new (or older) version would need to find the exact version libraries used for that version of the wildfly server and create a new profile. For example to build and use the `eap72` profile the profile should be passed to the commands:

```
mvn clean package -P eap72
mvn exec:java -P eap72 -Dexec.args="..."
```

