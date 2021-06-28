# JSPC Mave Plugin

The maven plugin is just a modification of the [Jetty JSPC Maven PLugin](https://wiki.eclipse.org/Jetty/Feature/Jetty_Jspc_Maven_Plugin) to use the Jastow counterpart. For the moment is just WIP and probably some more options will be added.

The plugin uses the dependencies defined at project and plugin level to execute the JSPC tool.

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
