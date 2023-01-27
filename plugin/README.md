# JSPC Mave Plugin

The maven plugin is just a modification of the [Jetty JSPC Maven PLugin](https://wiki.eclipse.org/Jetty/Feature/Jetty_Jspc_Maven_Plugin) to use the Jastow counterpart. All the tool options haven added as plugin properties to cover the same functionality.

* **useProvidedScope**. Default value: `false`. If true, jars of dependencies marked with *provided* will be placed on the compilation classpath.
* **webXmlType**. Default value: `MERGE_WEBXML`. What file output will be used, the options are the same than in the tool: `INC_WEBXML` (include file, just generate the servlet and mapping elements), `FRG_WEBXML` (generate a complete fragment file), `ALL_WEBXML` (generate a full and complete *web.xml* file), `MERGE_WEBXML` (merge the servlet and mapping elements inside the existing *web.xml* file in the app).
* **webXml**. Default value: `${basedir}/target/web.xml`. File to generate.
* **generatedClasses**. Default value: `${project.build.outputDirectory}`. Folder in which the classes are generated.
* **keepSources**. Default value: `false`. If false the generated java files are deleted after compilation.
* **webAppSourceDirectory**. Default value: `${basedir}/src/main/webapp`. Root directory containing the jsp files to compile.
* **includes**. Default value: `**\/*.jsp, **\/*.jspx`. The comma separated list of patterns for file extensions to be processed.
* **excludes**. Default value: `**\/.svn\/**`. The comma separated list of file name patters to exclude from compilation.
* **classesDirectory**. Default value: `${project.build.outputDirectory}`. The location of the compiled classes for the webapp.
* **sourceVersion**. Target version, if not set defaults to jspc default.
* **targetVersion**. Target version, if not set defaults to jspc default.
* **targetPackage**. Package name used for the compiled servlets, if not set defaults to jspc default.
* **debugLevel**. Debug level for the JspC output. Values: `OFF`, `FATAL`, `ERROR`, `WARN`, `INFO`, `DEBUG`, `TRACE`, `ALL`. If not set defaults to jspc default.
* **xpoweredBy**. Default value: `false`. Add `X-Powered-By` response header.
* **trimSpaces**. Default value: `false`. Remove template text that consists entirely of whitespace.
* **javaEncoding**. Encoding charset for Java classes. If not set defaults to jspc default.
* **webxmlEncoding**. Encoding to read and write the *web.xml* and the other generated files. If not set defaults to jspc default.
* **threadCount**. Number of threads to use to perform the compilation. By default the JspC default value is used (number of available threads in the target host divided by 2 plus 1).
* **failOnError**. Default value: `true`. If any JSP gives an error the plugin throws an exception.
* **failFast**. Default value: `false`. Stop on first compile error. It needs `failOnError` to be true (the option does nothing if `failOnError` is false).

The plugin uses the dependencies defined at project (check option *useProvidedScope*) and plugin level to execute the JSPC tool.

## Example

```xml
<plugin>
    <groupId>org.wildfly.jastow.jspc</groupId>
    <artifactId>jastow-jspc-maven-plugin</artifactId>
    <version>${project.version}</version>
    <executions>
        <execution>
            <id>jspc</id>
            <goals>
                <goal>jspc</goal>
            </goals>
            <configuration>
                <useProvidedScope>true</useProvidedScope>
            </configuration>
        </execution>
    </executions>
    <dependencies>
        <dependency>
            ...
        </dependency>
    </dependencies>
</plugin>
```
The folder *testapp* contains a test *WAR* application that is a working example for the plugin.