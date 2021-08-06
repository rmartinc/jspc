/*
 * Copyright 2021 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.jastow.jspc.plugin;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.log4j.Level;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.wildfly.jastow.jspc.JspC;
import org.wildfly.jastow.jspc.JspCResults;

/**
 * <p>Class initially based on the
 * <a href="https://github.com/eclipse/jetty.project/blob/jetty-11.0.x/jetty-jspc-maven-plugin/src/main/java/org/eclipse/jetty/jspc/plugin/JspcMojo.java">jetty maven plugin</a>.
 * The idea is using a maven plugin in order to execute the jastow JspC tool.
 * This goal will compile jsps for a webapp so that they can be included in a
 * war or jar bundle.</p>
 *
 * <p>Runs jastow jspc compiler to produce .java and .class files.</p>
 */
@Mojo(name = "jspc", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class JspcMojo extends AbstractMojo {

    /**
     * Whether or not to include dependencies on the plugin's classpath with
     * &lt;scope&gt;provided&lt;/scope&gt; Use WITH CAUTION as you may wind up
     * with duplicate jars/classes.
     */
    @Parameter(defaultValue = "false")
    private boolean useProvidedScope;

    /**
     * The artifacts for the project.
     */
    @Parameter(defaultValue = "${project.artifacts}", readonly = true)
    private Set<Artifact> projectArtifacts;

    /**
     * The maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * The artifacts for the plugin itself.
     */
    @Parameter(defaultValue = "${plugin.artifacts}", readonly = true)
    private List<Artifact> pluginArtifacts;

    /**
     * Type of file to generate with the servlets and filters (JspC.WEBXML_LEVEL).
     * It should be: INC_WEBXML, FRG_WEBXML, ALL_WEBXML, MERGE_WEBXML. Default
     * values is MERGE_WEBXML to add the info into the existing app web.xml.
     */
    @Parameter(defaultValue = "MERGE_WEBXML")
    private String webXmlType;

    /**
     * File into which to generate the &lt;servlet&gt; and
     * &lt;servlet-mapping&gt; tags for the compiled jsps
     */
    @Parameter(defaultValue = "${basedir}/target/web.xml")
    private String webXml;

    /**
     * The destination directory into which to put the compiled jsps.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}")
    private String generatedClasses;

    /**
     * Controls whether or not .java files generated during compilation will be
     * preserved.
     */
    @Parameter(defaultValue = "false")
    private boolean keepSources;

    /**
     * Root directory for all html/jsp etc files
     */
    @Parameter(defaultValue = "${basedir}/src/main/webapp")
    private String webAppSourceDirectory;

    /**
     * The comma separated list of patterns for file extensions to be processed.
     * By default will include all .jsp and .jspx files.
     */
    @Parameter(defaultValue = "**\\/*.jsp, **\\/*.jspx")
    private String includes;

    /**
     * The comma separated list of file name patters to exclude from
     * compilation.
     */
    @Parameter(defaultValue = "**\\/.svn\\/**")
    private String excludes;

    /**
     * The location of the compiled classes for the webapp
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}")
    private File classesDirectory;

    /**
     * Source version - if not set defaults to jsp default.
     */
    @Parameter
    private String sourceVersion;

    /**
     * Target version - if not set defaults to jsp default.
     */
    @Parameter
    private String targetVersion;

    /**
     * Package name used for the compiled servlets.
     */
    @Parameter
    private String targetPackage;

    /**
     * Debug level for the JspCoutput. Values: OFF, FATAL, ERROR, WARN, INFO, DEBUG, TRACE, ALL.
     */
    @Parameter
    private String debugLevel;

    /**
     * Add X-Powered-By response header.
     */
    @Parameter(defaultValue = "false")
    private boolean xpoweredBy;

    /**
     * Remove template text that consists entirely of whitespace.
     */
    @Parameter(defaultValue = "false")
    private boolean trimSpaces;

    /**
     * Encoding charset for Java classes.
     */
    @Parameter
    private String javaEncoding;

    /**
     * Encoding to read and write the web.xml and the other generated files.
     */
    @Parameter
    private String webxmlEncoding;

    /**
     * Number of threads to use to perform the compilation. By default the JspC
     * default value is used (number of available threads in the target host
     * divided by 2 plus 1).
     */
    @Parameter
    private Integer threadCount;

    /**
     * If any JSP gives an error the plugin throws an exception. The same
     * value is passed to the JspC tool.
     */
    @Parameter(defaultValue = "true")
    private boolean failOnError;

    /**
     * Stop on first compile error. It needs failOnError to be true (the option
     * does nothing if failOnError is false).
     */
    @Parameter(defaultValue = "false")
    private boolean failFast;

    /**
     * The JspC instance being used to compile the jsps.
     */
    @Parameter
    private JspC jspc;

    private JspCResults results;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (getLog().isDebugEnabled()) {

            getLog().info("webAppSourceDirectory=" + webAppSourceDirectory);
            getLog().info("generatedClasses=" + generatedClasses);
            getLog().info("webXmlType=" + webXmlType);
            getLog().info("webXml=" + webXml);
            getLog().info("keepSources=" + keepSources);
            getLog().info("targetPackage=" + targetPackage);
            if (sourceVersion != null) {
                getLog().info("sourceVersion=" + sourceVersion);
            }
            if (targetVersion != null) {
                getLog().info("targetVersion=" + targetVersion);
            }
        }
        try {
            prepare();
            compile();
        } catch (Exception e) {
            throw new MojoExecutionException("Failure processing jsps", e);
        }
    }

    public void compile() throws Exception {
        ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();

        //set up the classpath of the webapp
        List<URL> webAppUrls = setUpWebAppClassPath();

        //set up the classpath of the container (ie jetty and jsp jars)
        Set<URL> pluginJars = getPluginJars();
        Set<URL> providedJars = getProvidedScopeJars(pluginJars);

        //Make a classloader so provided jars will be on the classpath
        List<URL> sysUrls = new ArrayList<>();
        sysUrls.addAll(providedJars);
        URLClassLoader sysClassLoader = new URLClassLoader(sysUrls.toArray(new URL[0]), currentClassLoader);

        //make a classloader with the webapp classpath
        URLClassLoader webAppClassLoader = new URLClassLoader(webAppUrls.toArray(new URL[0]), sysClassLoader);
        StringBuilder webAppClassPath = new StringBuilder();

        for (int i = 0; i < webAppUrls.size(); i++) {
            if (getLog().isDebugEnabled()) {
                getLog().debug("webappclassloader contains: " + webAppUrls.get(i));
            }
            webAppClassPath.append(new File(webAppUrls.get(i).toURI()).getCanonicalPath());
            if (getLog().isDebugEnabled()) {
                getLog().debug("added to classpath: " + (webAppUrls.get(i)).getFile());
            }
            if (i + 1 < webAppUrls.size()) {
                webAppClassPath.append(System.getProperty("path.separator"));
            }
        }

        //Interpose a fake classloader as the webapp class loader. This is because the Apache JspC class
        //uses a TldScanner which ignores jars outside of the WEB-INF/lib path on the webapp classloader.
        //It will, however, look at all jars on the parents of the webapp classloader.
        URLClassLoader fakeWebAppClassLoader = new URLClassLoader(new URL[0], webAppClassLoader);
        Thread.currentThread().setContextClassLoader(fakeWebAppClassLoader);

        Thread.currentThread().setContextClassLoader(fakeWebAppClassLoader);

        try {
            if (jspc == null) {
                jspc = new JspC();
            }

            jspc.setUriRoot(webAppSourceDirectory)
                    .setOutputDir(generatedClasses)
                    .setDeleteSources(!keepSources)
                    .setWebxmlLevel(JspC.WEBXML_LEVEL.valueOf(webXmlType))
                    .setWebxmlFile(webXml)
                    .setXpoweredBy(xpoweredBy)
                    .setTrimSpaces(trimSpaces)
                    .setFailFast(failFast)
                    .setFailOnError(failOnError);
            if (targetPackage != null) {
                jspc.setTargetPackage(targetPackage);
            }
            if (sourceVersion != null) {
                jspc.setCompilerSourceVM(sourceVersion);
            }
            if (targetVersion != null) {
                jspc.setCompilerTargetVM(targetVersion);
            }
            if (debugLevel != null) {
                jspc.setDebugLevel(Level.toLevel(debugLevel));
            }
            if (javaEncoding != null) {
                jspc.setJavaEncoding(javaEncoding);
            }
            if (webxmlEncoding != null) {
                jspc.setWebxmlEncoding(Charset.forName(webxmlEncoding));
            }
            if (threadCount != null) {
                jspc.setThreadCount(threadCount);
            }

            // JspC#setExtensions() does not exist, so
            // always set concrete list of files that will be processed.
            List<String> jspFiles = getJspFiles(webAppSourceDirectory);

            if (jspFiles == null || jspFiles.isEmpty()) {
                getLog().info("No files selected to precompile");
            } else {
                getLog().info("Compiling " + jspFiles + " from includes=" + includes + " excludes=" + excludes);
                jspc.setPages(jspFiles);
                results = jspc.execute();
                if (results.isError()) {
                    getLog().error(String.format("Generation completed for [%d] files with [%d] errors in [%d] milliseconds",
                            results.total(), results.errors(), results.getTime()));
                    if (failOnError) {
                        throw new IllegalStateException(String.format("Compilation failed for %d JSP files.", results.errors()));
                    }
                } else {
                    getLog().info(String.format("Generation completed for [%d] files with [%d] errors in [%d] milliseconds",
                            results.total(), results.errors(), results.getTime()));
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(currentClassLoader);
        }
    }

    public JspC getJspC() {
        return jspc;
    }

    public JspCResults getResults() {
        return results;
    }

    private List<String> getJspFiles(String webAppSourceDirectory)
            throws Exception {
        return FileUtils.getFileNames(new File(webAppSourceDirectory), includes, excludes, true);
    }

    private void prepare() throws Exception {
        // For some reason JspC doesn't like it if the dir doesn't
        // already exist and refuses to create the web.xml fragment
        File generatedSourceDirectoryFile = new File(generatedClasses);
        if (!generatedSourceDirectoryFile.exists()) {
            generatedSourceDirectoryFile.mkdirs();
        }
    }

    /**
     * Set up the execution classpath for Jasper.
     *
     * Put everything in the classesDirectory and all of the dependencies on the
     * classpath.
     *
     * @returns a list of the urls of the dependencies
     */
    private List<URL> setUpWebAppClassPath() throws Exception {
        //add any classes from the webapp
        List<URL> urls = new ArrayList<>();
        String classesDir = classesDirectory.getCanonicalPath();
        classesDir = classesDir + (classesDir.endsWith(File.pathSeparator) ? "" : File.separator);
        urls.add(new File(classesDir).toURI().toURL());

        if (getLog().isDebugEnabled()) {
            getLog().debug("Adding to classpath classes dir: " + classesDir);
        }

        //add the dependencies of the webapp (which will form WEB-INF/lib)
        for (Artifact artifact : project.getArtifacts()) {
            // Include runtime and compile time libraries
            if (!Artifact.SCOPE_TEST.equals(artifact.getScope()) && !Artifact.SCOPE_PROVIDED.equals(artifact.getScope())) {
                String filePath = artifact.getFile().getCanonicalPath();
                if (getLog().isDebugEnabled()) {
                    getLog().debug("Adding to classpath dependency file: " + filePath);
                }

                urls.add(artifact.getFile().toURI().toURL());
            }
        }
        return urls;
    }

    /**
     *
     */
    private Set<URL> getPluginJars() throws MalformedURLException {
        HashSet<URL> pluginJars = new HashSet<>();
        for (Artifact pluginArtifact : pluginArtifacts) {
            if ("jar".equalsIgnoreCase(pluginArtifact.getType())) {
                if (getLog().isDebugEnabled()) {
                    getLog().debug("Adding plugin artifact " + pluginArtifact);
                }
                pluginJars.add(pluginArtifact.getFile().toURI().toURL());
            }
        }

        return pluginJars;
    }

    /**
     *
     */
    private Set<URL> getProvidedScopeJars(Set<URL> pluginJars) throws MalformedURLException {
        if (!useProvidedScope) {
            return Collections.emptySet();
        }

        HashSet<URL> providedJars = new HashSet<>();

        for (Artifact artifact : projectArtifacts) {
            if (Artifact.SCOPE_PROVIDED.equals(artifact.getScope())) {
                //test to see if the provided artifact was amongst the plugin artifacts
                URL jar = artifact.getFile().toURI().toURL();
                if (!pluginJars.contains(jar)) {
                    providedJars.add(jar);
                    if (getLog().isDebugEnabled()) {
                        getLog().debug("Adding provided artifact: " + artifact);
                    }
                } else {
                    if (getLog().isDebugEnabled()) {
                        getLog().debug("Skipping provided artifact: " + artifact);
                    }
                }
            }
        }
        return providedJars;
    }

}
