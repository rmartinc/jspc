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
package org.wildfly.jastow.jspc;

import jakarta.servlet.descriptor.JspConfigDescriptor;
import jakarta.servlet.descriptor.TaglibDescriptor;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.jasper.JasperException;
import org.apache.jasper.compiler.JspRuntimeContext;
import org.apache.jasper.deploy.TagLibraryInfo;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author rmartinc
 */
public class JspC {

    private Logger log = LogManager.getLogger(JspC.class.getPackageName());

    public enum WEBXML_LEVEL {INC_WEBXML, FRG_WEBXML, ALL_WEBXML, MERGE_WEBXML};

    private final JspCServletContext ctx;
    private final JspCOptions options;
    private String uriRoot;
    private String uriBase;
    private String targetPackage = null;
    private String targetClassName = null;
    private String webxmlFile;
    private Charset webxmlEncoding = StandardCharsets.UTF_8;
    private ClassLoader loader;
    private JspRuntimeContext rctxt;
    private JspCServletConfig config;
    private HashMap<String, TagLibraryInfo> jspTagLibraries;
    private List<String> pages = new ArrayList<>();
    private Iterator<String> pagesIterator;
    private JspCResults results;
    private WEBXML_LEVEL webxmlLevel;
    private boolean failOnError = true;
    private boolean failFast = false;
    private int threadCount = (Runtime.getRuntime().availableProcessors() / 2) + 1;
    private boolean deleteSources = false;

    // getters

    public String getUriRoot() {
        return uriRoot;
    }

    public String getUriBase() {
        return uriBase;
    }

    public String getTargetPackage() {
        return targetPackage;
    }

    public String getTargetClassName() {
        return targetClassName;
    }

    public String getWebxmlFile() {
        return webxmlFile;
    }

    public Charset getWebxmlEncoding() {
        return webxmlEncoding;
    }

    public WEBXML_LEVEL getWebxmlLevel() {
        return webxmlLevel;
    }

    public boolean isFailOnError() {
        return failOnError;
    }

    public boolean isFailFast() {
        return failFast;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public JspCOptions getOptions()  {
        return this.options;
    }

    public Level getDebugLevel() {
        return log.getLevel();
    }

    public boolean getDeleteSources() {
        return deleteSources;
    }

    // setters

    public JspC setDieLevel(int dieLevel) {
        if (results == null) {
            results = new JspCResults(dieLevel);
        } else {
            results.setErrorCode(dieLevel);
        }
        return this;
    }

    public JspC setUriRoot(String uriRoot) throws IOException {
        if (uriRoot == null) {
            this.uriRoot = null;
        } else {
            File dir = new File(uriRoot);
            this.uriRoot = dir.getCanonicalPath();
        }
        ctx.setUriRoot(this.uriRoot);
        return this;
    }

    public JspC setOutputDir(String outputDir) {
        options.setScratchDir(new File(outputDir));
        return this;
    }

    public JspC setDebugLevel(Level level) {
        this.log = LogManager.getLogger(this.getClass().getPackage().getName());
        Configurator.setLevel(this.log.getName(), level);
        return this;
    }

    public JspC setTargetPackage(String targetPackage) {
        this.targetPackage = targetPackage;
        return this;
    }

    public JspC setTargetClassName(String targetClassName) {
        this.targetClassName = targetClassName;
        return this;
    }

    public JspC setMappedFile(boolean mappedFile) {
        this.options.setMappedFile(mappedFile);
        return this;
    }

    public JspC setUriBase(String uriBase) {
        this.uriBase = uriBase;
        return this;
    }

    public JspC setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
        return this;
    }

    public JspC setFailFast(boolean failFast) {
        this.failFast = failFast;
        return this;
    }

    public JspC setWebxmlLevel(WEBXML_LEVEL webxmlLevel) {
        this.webxmlLevel = webxmlLevel;
        return this;
    }

    public JspC setWebxmlFile(String webxmlFile) {
        this.webxmlFile = webxmlFile;
        return this;
    }

    public JspC setWebxmlEncoding(Charset webxmlEncoding) {
        this.webxmlEncoding = webxmlEncoding;
        return this;
    }

    public JspC setClassPath(String classpath) {
        this.options.setClassPath(classpath);
        return this;
    }

    public JspC setXpoweredBy(boolean xpoweredBy) {
        this.options.setXpoweredBy(xpoweredBy);
        return this;
    }

    public JspC setTrimSpaces(boolean trimSpaces) {
        this.options.setTrimSpaces(trimSpaces);
        return this;
    }

    public JspC setJavaEncoding(String javaEncoding) {
        this.options.setJavaEncoding(javaEncoding);
        return this;
    }

    public JspC setCompilerSourceVM(String compilerSourceVM) {
        this.options.setCompilerSourceVM(compilerSourceVM);
        return this;
    }

    public JspC setCompilerTargetVM(String compilerTargetVM) {
        this.options.setCompilerTargetVM(compilerTargetVM);
        return this;
    }

    public JspC setThreadCount(int threadCount) {
        this.threadCount = threadCount;
        return this;
    }

    public JspC setPages(List<String> pages) {
        this.pages = pages;
        return this;
    }

    public JspC addPage(String page) {
        this.pages.add(page);
        return this;
    }

    public JspC setDeleteSources(boolean deleteSources) {
        this.deleteSources = deleteSources;
        return this;
    }

    // usage

    private void usage(String error) {
        String nl = System.getProperty("line.separator");
        StringBuilder sb = new StringBuilder();
        if (error != null && !error.isEmpty()) {
            sb.append(nl).append("ERROR: ").append(error);
        }
        sb.append(nl)
                .append("Usage: jspc <options> [--] <jsp files>").append(nl)
                .append("where jsp files is").append(nl)
                .append("    -webapp <dir>         A directory containing a web-app, whose JSP pages").append(nl)
                .append("                          will be processed recursively").append(nl)
                .append("or any number of").append(nl)
                .append("    <file>                A file to be parsed as a JSP page").append(nl)
                .append("where options include:").append(nl)
                .append("    -help                 Print this help message").append(nl)
                .append("    -v[v]                 Verbose mode").append(nl)
                .append("    -d <dir>              Output Directory (default -Djava.io.tmpdir)").append(nl)
                .append("    -l                    Outputs the name of the JSP page upon failure").append(nl)
                .append("    -s                    Outputs the name of the JSP page upon success").append(nl)
                .append("    -p <name>             Name of target package (default org.apache.jsp)").append(nl)
                .append("    -c <name>             Name of target class name (only applies to first JSP page)").append(nl)
                .append("    -mapped               Generates separate write() calls for each HTML line in the JSP").append(nl)
                .append("    -die <#>              Generates an error return code (#) on fatal errors (default 1)").append(nl)
                .append("    -uribase <dir>        The uri directory compilations should be relative to").append(nl)
                .append("                          (default \"/\")").append(nl)
                .append("    -uriroot <dir>        Same as -webapp").append(nl)
                //.append("    -compile              Compiles generated servlets").append(nl)
                .append("    -noFailOnError        Do not fail on error and generate XML outputs if required").append(nl)
                .append("    -failFast             Stop on first compile error").append(nl)
                .append("    -webinc <file>        Creates a partial servlet mappings in the file").append(nl)
                .append("    -webfrg <file>        Creates a complete web-fragment.xml file").append(nl)
                .append("    -webxml <file>        Creates a complete web.xml in the file").append(nl)
                .append("    -mergexml <file>      Merges existing web.xml in the app to a different web.xml position").append(nl)
                .append("    -webxmlencoding <enc> Set the encoding charset used to read and write the web.xml").append(nl)
                .append("                          file (default is UTF-8)").append(nl)
                .append("    -addwebxmlmappings    Merge generated web.xml fragment into the web.xml file of the web-app,").append(nl)
                .append("                          whose JSP pages we are processing. A backup file is created.").append(nl)
                .append("    -classpath <path>     Overrides java.class.path system property").append(nl)
                .append("    -xpoweredBy           Add X-Powered-By response header").append(nl)
                .append("    -trimSpaces           Remove template text that consists entirely of whitespace").append(nl)
                .append("    -javaEncoding <enc>   Set the encoding charset for Java classes (default UTF-8)").append(nl)
                .append("    -source <version>     Set the -source argument to the compiler (default 1.8)").append(nl)
                .append("    -target <version>     Set the -target argument to the compiler (default 1.8)").append(nl)
                .append("    -threadCount <count>  Number of threads to use for compilation.").append(nl)
                .append("                          (\"2.0C\" means two threads per core)").append(nl)
                .append("    -deletesources        Delete generated Java source files.").append(nl);
        throw new IllegalArgumentException(sb.toString());
    }

    // parse methods

    private String getArgumentIndex(String option, int idx, String[] args) {
        if (idx < args.length) {
            return args[idx];
        } else {
            usage(String.format("Option \"%s\" needs an argument", option));
            return null;
        }
    }

    private String parseDirectory(String option, int idx, String[] args) throws IOException {
        String file = getArgumentIndex(option, idx, args);
        File dir = new File(file);
        if (!dir.isDirectory()) {
            usage(String.format("Invalid directory \"%s\" for option \"%s\"", file, option));
        }
        return dir.getCanonicalPath();
    }

    private String parseWritableFile(String option, int idx, String[] args) throws IOException {
        String file = getArgumentIndex(option, idx, args);
        File f = new File(file);
        if (f.isDirectory() || (f.exists() && !f.canWrite())) {
            usage(String.format("Invalid writable file \"%s\" for option \"%s\"", file, option));
        } else {
            try {
                f.createNewFile();
            } catch (IOException e) {
                usage(String.format("Invalid writable file \"%s\" for option \"%s\"", file, option));
            }
        }
        return f.getCanonicalPath();
    }

    private Charset parseCharset(String option, int idx, String[] args) {
        String charset = getArgumentIndex(option, idx, args);
        try {
            return Charset.forName(charset);
        } catch (Exception e) {
            usage(String.format("Invalid chasert file \"%s\" for option \"%s\"", charset, option));
            return null;
        }
    }

    private int parseInteger(String option, int idx, String[] args) {
        String number = getArgumentIndex(option, idx, args);
        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException e) {
            usage(String.format("Invalid number \"%s\" for option \"%s\"", number, option));
            return -1;
        }
    }

    private double parseDouble(String option, String number) {
        try {
            return Double.parseDouble(number);
        } catch (NumberFormatException e) {
            usage(String.format("Invalid number \"%s\" for option \"%s\"", number, option));
            return -1;
        }
    }

    private void setDebugLevelIfNecessary(Level l) {
        if (this.log.getLevel().compareTo(l) < 0) {
            setDebugLevel(l);
        }
    }

    private void parseArgs(String... args) throws IOException {
        boolean finished = false;
        int i;
        for (i = 0; i < args.length && !finished; i++) {
            switch(args[i]) {
                case "-webapp":
                case "-uriroot":
                    this.setUriRoot(parseDirectory(args[i], ++i, args));
                    break;
                case "-help":
                    usage(null);
                    break;
                case "-d":
                    String outputDir = parseDirectory(args[i], ++i, args);
                    this.setOutputDir(outputDir);
                    break;
                case "-vv":
                    setDebugLevelIfNecessary(Level.TRACE);
                    break;
                case "-v":
                    setDebugLevelIfNecessary(Level.DEBUG);
                    break;
                case "-s":
                    setDebugLevelIfNecessary(Level.INFO);
                    break;
                case "-l":
                    setDebugLevelIfNecessary(Level.WARN);
                    break;
                case "-p":
                    setTargetPackage(getArgumentIndex(args[i], ++i, args));
                    break;
                case "-c":
                    setTargetClassName(getArgumentIndex(args[i], ++i, args));
                    break;
                case "-mapped":
                    setMappedFile(true);
                    break;
                case "-die":
                    int errorCode = parseInteger(args[i], ++i, args);
                    if (errorCode <= 0) {
                        usage(String.format("Invalid number \"%d\" code for option \"die\"", errorCode));
                    }
                    setDieLevel(errorCode);
                    break;
                case "-uribase":
                    setUriBase(getArgumentIndex(args[i], ++i, args));
                    break;
                // "compile" flag has no sense because we always do this
                // the JSP is compiled and if exists it is checked for changes
                // case "-compile":
                //    usage(String.format("Not implemented option \"%s\"", args[i]));
                //    break;
                case "-noFailOnError":
                    setFailOnError(false);
                    break;
                case "-failFast":
                    setFailFast(true);
                    break;
                case "-webinc":
                    if (webxmlLevel != null) {
                        usage(String.format("Invalid -webinc option because the output was previously set to \"%s\"", webxmlLevel.name()));
                    }
                    setWebxmlFile(parseWritableFile(args[i], ++i, args));
                    setWebxmlLevel(WEBXML_LEVEL.INC_WEBXML);
                    break;
                case "-webfrg":
                    if (webxmlLevel != null) {
                        usage(String.format("Invalid -webfrg option because the output was previously set to \"%s\"", webxmlLevel.name()));
                    }
                    setWebxmlFile(parseWritableFile(args[i], ++i, args));
                    setWebxmlLevel(WEBXML_LEVEL.FRG_WEBXML);
                    break;
                case "-webxml":
                    if (webxmlLevel != null) {
                        usage(String.format("Invalid -webxml option because the output was previously set to \"%s\"", webxmlLevel.name()));
                    }
                    setWebxmlFile(parseWritableFile(args[i], ++i, args));
                    setWebxmlLevel(WEBXML_LEVEL.ALL_WEBXML);
                    break;
                case "-addwebxmlmappings":
                    if (webxmlLevel != null) {
                        usage(String.format("Invalid -addwebxmlmappings option because the output was previously set to \"%s\"", webxmlLevel.name()));
                    }
                    setWebxmlLevel(WEBXML_LEVEL.MERGE_WEBXML);
                    break;
                case "-mergexml":
                    if (webxmlLevel != null) {
                        usage(String.format("Invalid -mergexml option because the output was previously set to \"%s\"", webxmlLevel.name()));
                    }
                    setWebxmlFile(parseWritableFile(args[i], ++i, args));
                    setWebxmlLevel(WEBXML_LEVEL.MERGE_WEBXML);
                    break;
                case "-webxmlencoding":
                    setWebxmlEncoding(parseCharset(args[i], ++i, args));
                    break;
                case "-classpath":
                    setClassPath(getArgumentIndex(args[i], ++i, args));
                    break;
                case "-xpoweredBy":
                    setXpoweredBy(true);
                    break;
                case "-trimSpaces":
                    setTrimSpaces(true);
                    break;
                case "-javaEncoding":
                    setJavaEncoding(getArgumentIndex(args[i], ++i, args));
                    break;
                case "-source":
                    setCompilerSourceVM(getArgumentIndex(args[i], ++i, args));
                    break;
                case "-target":
                    setCompilerTargetVM(getArgumentIndex(args[i], ++i, args));
                    break;
                case "-threadCount":
                    String option = getArgumentIndex(args[i], i+1, args);
                    if (option.endsWith("C")) {
                        setThreadCount((int) Math.ceil(parseDouble("-threadCount", option.substring(0, option.length() - 1)) * Runtime.getRuntime().availableProcessors()));
                        i++;
                    } else {
                        setThreadCount(parseInteger(args[i], ++i, args));
                    }
                    if (threadCount <= 0) {
                        usage(String.format("Invalid number of threads \"%s\"", args[i]));
                    }
                    break;
                case "-deletesources":
                    setDeleteSources(true);
                    break;
                default:
                    if (args[i].equals("--")) {
                        finished = true;
                    } else if (args[i].startsWith("-")) {
                        usage("Invalid option: " + args[i]);
                    } else {
                        finished = true;
                        i--;
                    }
            }
        }
        // all the rest are jsp files to execute
        if (i < args.length) {
            for (; i < args.length; i++) {
                File f = new File(args[i]);
                if (!f.exists() || !f.canRead()) {
                    usage(String.format("Invalid JSP file \"%s\"", args[i]));
                } else {
                    pages.add(args[i]);
                }
            }
        }
        if (uriRoot == null && pages.isEmpty()) {
            usage("No -webapp or JSP files passed");
        }
        // locate the uriRoot using the first jsp if webroot not passed
        if (uriRoot == null) {
            try {
                locateUriRootFromFirstPage(pages.get(0));
            } catch (JasperException e) {
                usage(String.format("The webapp path cannot be located using the first JSP file \"%s\"", pages.get(0)));
            }
        }
        // check if we have jsp pages or load all the jsp files in the app
        if (pages.isEmpty()) {
            scanJspFilesinWebApp("/");
            if (pages.isEmpty()) {
                usage(String.format("No JSP pages in webapp \"%s\"", uriRoot));
            }
        }
    }

    // methods to locate JSP files and the web root if not passed

    private void scanJspFilesinWebApp(String input) {
        Set<String> paths = ctx.getResourcePaths(input);
        for (String path : paths) {
            if (path.endsWith("/")) {
                scanJspFilesinWebApp(path);
            } else if (path.endsWith(".jsp") || path.endsWith(".jspx")) {
                pages.add(path);
            }
        }
    }

    public void locateUriRootFromFirstPage(String jsp) throws JasperException {
        if (uriBase == null) {
            uriBase = "/";
        }
        File f = new File(jsp);
        try {
            if (f.exists()) {
                f = new File(f.getAbsolutePath());
                while (f != null) {
                    File g = new File(f, "WEB-INF");
                    if (g.exists() && g.isDirectory()) {
                        uriRoot = f.getCanonicalPath();
                        break;
                    }
                    if (f.exists() && f.isDirectory()) {
                        uriBase = "/" + f.getName() + "/" + uriBase;
                    }
                    f = f.getParentFile();
                }

                if (uriRoot != null) {
                    File froot = new File(uriRoot);
                    setUriRoot(froot.getCanonicalPath());
                }
            }
        } catch (IOException e) {
            throw new JasperException("Error locating uriroot from the first JSP ", e);
        }
        if (uriRoot == null) {
            throw new JasperException("The webapp path cannot be located using the first JSP file");
        }
    }

    // constructors

    public JspC() throws IOException {
        ctx = new JspCServletContext();
        options = new JspCOptions(ctx);
        this.setDebugLevel(Level.WARN);
    }

    public JspC(String... args) throws IOException {
        this();
        // parse the arguments
        parseArgs(args);
    }

    // methods to locate TLD inside jars and app

    private void scanJar(URL url, Pattern pattern, HashMap<String, TagLibraryInfo> jspTagLibraries) throws IOException {
        JarURLConnection conn = (JarURLConnection) url.openConnection();
        JarFile jarFile = conn.getJarFile();
        Enumeration<JarEntry> e = jarFile.entries();
        while (e.hasMoreElements()) {
            JarEntry entry = e.nextElement();
            if (pattern.matcher(entry.getName()).matches()) {
                try {
                    Utils.parseTldFile(jarFile.getName(), jarFile.getInputStream(entry), jspTagLibraries);
                } catch (IOException|XMLStreamException ex) {
                    log.error("Error parsing TLD file from jsp file: " + jarFile.getName() + " " + entry.getName(), ex);
                }
            }
        }
    }

    private void scanFilePath(URL url, HashMap<String, TagLibraryInfo> jspTagLibraries) throws IOException, URISyntaxException {
        try (Stream<Path> paths = Files.walk(Paths.get(url.toURI()), 2)) {
            paths.filter(path -> path.endsWith(".tld"))
                    .forEach(path -> {
                        try {
                            Utils.parseTldFile(path.toString(), Files.newInputStream(path), jspTagLibraries);
                        } catch (IOException | XMLStreamException ex) {
                            log.error("Error parsing TLD file from file : " + path, ex);
                        }
                    });
        }
    }

    private static final Pattern TLD_PATTERN_IN_JAR = Pattern.compile("META-INF/.*\\.tld");

    private void scanJarsForTlds() throws IOException, URISyntaxException, XMLStreamException {
        // locate all /META-INF directories in the classpath
        Enumeration<URL> e = loader.getResources("META-INF");
        while (e.hasMoreElements()) {
            URL metaInf = e.nextElement();
            if (metaInf.getProtocol().equals("file")) {
                scanFilePath(metaInf, jspTagLibraries);
            } else if (metaInf.getProtocol().equals("jar")) {
                scanJar(metaInf, TLD_PATTERN_IN_JAR, jspTagLibraries);
            } else {
                log.warn("Unmanaged protocol locating taglibs from url: " + metaInf);
            }
        }
    }

    private void scanJspConfigForTlds() {
        JspConfigDescriptor desc = ctx.getJspConfigDescriptor();
        if (desc != null) {
            for (TaglibDescriptor taglib: desc.getTaglibs()) {
                String resourcePath = taglib.getTaglibLocation();
                try {
                    URL url = ctx.getResource(resourcePath);
                    Utils.parseTldFile(resourcePath, url.openStream(), jspTagLibraries);
                } catch (IOException | XMLStreamException e) {
                    log.warn("Error parsing TLD file from jsp-config from web.xml: " + resourcePath, e);
                }
            }
        }
    }

    private void scanWebInfPathForTlds(String path) {
        Set<String> files = ctx.getResourcePaths(path);
        log.trace("files=" + files);
        for (String file : files) {
            if (file.endsWith("/")) {
                scanWebInfPathForTlds(file);
            } else if (file.endsWith(".tld")) {
                try {
                    Utils.parseTldFile(file, ctx.getResourceAsStream(file), jspTagLibraries);
                } catch (XMLStreamException e) {
                    log.warn("Error parsing TLD file from WEB-INF directory: " + file, e);
                }
            }
        }
    }

    // the class loader from the app is added to normal class loader

    private ClassLoader setupClassLoader() throws IOException {
        String optionsClasspath = options.getClassPath();
        StringBuilder classpath = new StringBuilder();
        List<URL> clUrls = new ArrayList<>();
        if (optionsClasspath != null) {
            String[] optionsClasspathArray = options.getClassPath().split(File.pathSeparator);
            Arrays.stream(optionsClasspathArray).filter(c -> c != null && !c.isEmpty())
                    .forEach(c -> {
                        try {
                            classpath.append(c).append(File.pathSeparator);
                            File libFile = new File(c);
                            if (libFile.exists()) {
                                clUrls.add(libFile.getCanonicalFile().toURI().toURL());
                            } else {
                                log.warn("Invalid classpath entry: " + c);
                            }
                        } catch (IOException e) {
                            log.warn(String.format("Error adding URL \"%s\" to the classpath", c), e);
                        }
                    });
        }
        // add application jars and classes
        File webappBase = new File(uriRoot);
        if (webappBase.exists()) {
            // add the WEB-INF/classes directory of the app
            File classes = new File(webappBase, "/WEB-INF/classes");
            if (classes.exists()) {
                classpath.append(classes.getCanonicalPath()).append(File.pathSeparator);
                clUrls.add(classes.getCanonicalFile().toURI().toURL());
            }
            // add all the jars inside the WEB-INF/lib if they exists
            File lib = new File(webappBase, "/WEB-INF/lib");
            if (lib.exists() && lib.isDirectory()) {
                String[] libs = lib.list();
                if (libs != null) {
                    for (String lib1 : libs) {
                        if (lib1.endsWith(".jar")) {
                            File libFile = new File(lib, lib1);
                            classpath.append(libFile.getCanonicalPath()).append(File.pathSeparator);
                            clUrls.add(libFile.getCanonicalFile().toURI().toURL());
                        }
                    }
                }
            }
        }
        // construct the classloader
        options.setClassPath(classpath.toString());
        log.trace("URLs configured in class loader: " + clUrls);
        return new URLClassLoader(clUrls.toArray(new URL[0]), Thread.currentThread().getContextClassLoader());
    }

    // web.xml output methods

    private void writeWebXml() throws IOException, ParserConfigurationException, SAXException, TransformerException {
        if (webxmlLevel != null) {
            switch(webxmlLevel) {
                case ALL_WEBXML:
                    writeAllWebXml();
                    break;
                case FRG_WEBXML:
                    writeWebFragment();
                    break;
                case INC_WEBXML:
                    writeWebInclude();
                    break;
                case MERGE_WEBXML:
                    mergeIntoWebXml();
                    break;
            }
        }
    }

    private void mergeIntoWebXml() throws IOException, ParserConfigurationException, SAXException, TransformerException {
        // TODO: maybe we can do this better, but for the moment I have only this
        // TODO: maybe split the method in several parts
        File webXml = new File(this.uriRoot, "/WEB-INF/web.xml");
        Set<String> stopElements = new HashSet(Arrays.asList(new String[]{"servlet-mapping", "session-config",
            "mime-mapping", "welcome-file-list", "error-page", "jsp-config", "security-constraint",
            "login-config", "security-role", "env-entry", "ejb-ref", "ejb-local-ref"}));
        if (!webXml.exists()) {
            // just write the file from scratch
            if (this.webxmlFile == null) {
                // if it is null we are using the same web.xml file in the app
                this.webxmlFile = webXml.getAbsolutePath();
            }
            writeAllWebXml();
        } else {
            Document doc = Utils.readXmlIntoDocument(webXml);
            if (!doc.getDocumentElement().getNodeName().equals("web-app")) {
                throw new IllegalStateException("Invalid web.xml to add mappings");
            }
            // locate any element after the zone to insert
            NodeList list = doc.getDocumentElement().getChildNodes();
            Node selectedNode = null;
            for (int i = 0; i < list.getLength(); i++) {
                Node node = list.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE && stopElements.contains(node.getNodeName())) {
                    selectedNode = node;
                    break;
                }
            }
            // append all the generated data before the selected node
            if (selectedNode != null) {
                doc.getDocumentElement().insertBefore(doc.createTextNode("\n\n    "), selectedNode);
                doc.getDocumentElement().insertBefore(doc.createComment("Automatically generated web include"), selectedNode);
                doc.getDocumentElement().insertBefore(doc.createTextNode("\n    "), selectedNode);
            } else {
                doc.getDocumentElement().appendChild(doc.createTextNode("\n\n    "));
                doc.getDocumentElement().appendChild(doc.createComment("Automatically generated web include"));
                doc.getDocumentElement().appendChild(doc.createTextNode("\n    "));
            }
            for (JspCResults.ResultEntry entry : results.getResults()) {
                Element servlet = doc.createElement("servlet");
                Element servletName = doc.createElement("servlet-name");
                servletName.setTextContent(entry.getServletName());
                Element servletClass = doc.createElement("servlet-class");
                servletClass.setTextContent(entry.getServletName());
                servlet.appendChild(doc.createTextNode("\n        "));
                servlet.appendChild(servletName);
                servlet.appendChild(doc.createTextNode("\n        "));
                servlet.appendChild(servletClass);
                servlet.appendChild(doc.createTextNode("\n    "));
                if (selectedNode != null) {
                    doc.getDocumentElement().insertBefore(servlet, selectedNode);
                    doc.getDocumentElement().insertBefore(doc.createTextNode("\n    "), selectedNode);
                } else {
                    doc.getDocumentElement().appendChild(servlet);
                    doc.getDocumentElement().appendChild(doc.createTextNode("\n    "));
                }
            }
            for (JspCResults.ResultEntry entry : results.getResults()) {
                Element servletMapping = doc.createElement("servlet-mapping");
                Element servletName = doc.createElement("servlet-name");
                servletName.setTextContent(entry.getServletName());
                Element urlPattern = doc.createElement("url-pattern");
                urlPattern.setTextContent(entry.getJspUri());
                servletMapping.appendChild(doc.createTextNode("\n        "));
                servletMapping.appendChild(servletName);
                servletMapping.appendChild(doc.createTextNode("\n        "));
                servletMapping.appendChild(urlPattern);
                servletMapping.appendChild(doc.createTextNode("\n    "));
                if (selectedNode != null) {
                    doc.getDocumentElement().insertBefore(servletMapping, selectedNode);
                    doc.getDocumentElement().insertBefore(doc.createTextNode("\n    "), selectedNode);
                } else {
                    doc.getDocumentElement().appendChild(servletMapping);
                    doc.getDocumentElement().appendChild(doc.createTextNode("\n    "));
                }
            }
            if (selectedNode != null) {
                doc.getDocumentElement().insertBefore(doc.createComment("End of web include"), selectedNode);
                doc.getDocumentElement().insertBefore(doc.createTextNode("\n\n    "), selectedNode);
            } else {
                doc.getDocumentElement().appendChild(doc.createComment("End of web include"));
                doc.getDocumentElement().appendChild(doc.createTextNode("\n\n    "));
            }
            if (webxmlFile == null || new File(webxmlFile).getCanonicalPath().equals(webXml.getCanonicalPath())) {
                // overwriting the same web.xml in the app adding the bindings
                // do a backup just in case and overwrite the web.xml in the app
                webxmlFile = webXml.getAbsolutePath();
                File backup = new File(webxmlFile + ".jspc-" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
                webXml.renameTo(backup);
            }
            // write the contents to web.xml
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, webxmlEncoding.name());
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(webxmlFile);
            transformer.transform(source, result);
        }
    }

    private void writeWebFragment() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(webxmlFile), webxmlEncoding))) {
            writer.write(String.format("<?xml version=\"1.0\" encoding=\"%s\"?>", webxmlEncoding.name()));
            writer.newLine();
            writer.write("<web-fragment xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
            writer.newLine();
            writer.write("              xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-fragment_4_0.xsd\"");
            writer.newLine();
            writer.write("              version=\"4.0\" metadata-complete=\"true\">");
            writer.newLine();
            writer.write("    <name>org.wildfly.jastow.jspc</name>");
            writer.newLine();
            writer.write("    <!-- Automatically generated web-fragment.xml -->");
            writer.newLine();
            writer.newLine();
            // the servlet and mappings
            writeEntries(writer);
            // end web fragment
            writer.newLine();
            writer.write("</web-fragment>");
            writer.newLine();
        }
    }

    private void writeWebInclude() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(webxmlFile), webxmlEncoding))) {
            writer.write("<!-- Automatically generated web include -->");
            writer.newLine();
            writer.newLine();
            // the servlet and mappings
            writeEntries(writer);
            // end web include
            writer.newLine();
            writer.write("<!-- End of web include -->");
            writer.newLine();
        }
    }

    private void writeAllWebXml() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(webxmlFile), webxmlEncoding))) {
            writer.write(String.format("<?xml version=\"1.0\" encoding=\"%s\"?>", webxmlEncoding.name()));
            writer.newLine();
            writer.write("<web-app xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
            writer.newLine();
            writer.write("              xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee  http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd\"");
            writer.newLine();
            writer.write("              version=\"4.0\">");
            writer.newLine();
            writer.write("    <!-- Automatically generated web.xml -->");
            writer.newLine();
            writer.newLine();
            // the servlet and mappings
            writeEntries(writer);
            // end web include
            writer.newLine();
            writer.write("</web-app>");
            writer.newLine();
        }
    }

    private void writeEntries(BufferedWriter writer) throws IOException {
        for (JspCResults.ResultEntry entry : results.getResults()) {
            writeServletEntry(writer, entry.getServletName());
        }
        writer.newLine();
        for (JspCResults.ResultEntry entry : results.getResults()) {
            writeMappingEntry(writer, entry.getServletName(), entry.getJspUri());
        }
    }

    private void writeServletEntry(BufferedWriter writer, String servletName) throws IOException {
        writer.write("    <servlet>");
        writer.newLine();
        writer.write("        <servlet-name>");
        writer.write(servletName);
        writer.write("</servlet-name>");
        writer.newLine();
        writer.write("        <servlet-class>");
        writer.write(servletName);
        writer.write("</servlet-class>");
        writer.newLine();
        writer.write("    </servlet>");
        writer.newLine();
    }

    private void writeMappingEntry(BufferedWriter writer, String servletName, String jspUri) throws IOException {
        writer.write("    <servlet-mapping>");
        writer.newLine();
        writer.write("        <servlet-name>");
        writer.write(servletName);
        writer.write("</servlet-name>");
        writer.newLine();
        writer.write("        <url-pattern>");
        writer.write(jspUri.replace('\\', '/'));
        writer.write("</url-pattern>");
        writer.newLine();
        writer.write("    </servlet-mapping>");
        writer.newLine();
    }

    // real execute methods

    public synchronized String nextJsp() {
        if (pagesIterator == null) {
            pagesIterator = pages.iterator();
        }
        if (pagesIterator.hasNext() && !(failFast && failOnError && results.isError())) {
            return pagesIterator.next();
        } else {
            return null;
        }
    }

    private void prepareEnvironmentToCompile() throws JasperException, IOException, URISyntaxException, XMLStreamException {
        // create the results if no error code passed
        if (this.results == null) {
            this.results = new JspCResults();
        } else if (results.total() > 0) {
            throw new JasperException("Already executed JspC instance");
        }
        // load into the classpath application libs and classes
        loader = setupClassLoader();
        // setup context with missing things
        ctx.calculateJspConfigDescriptor();
        ctx.setClassLoader(loader);
        // scan all possible TLD locations for taglibs and set them in the ctx for jastow
        jspTagLibraries = ctx.getJspTagLibraries();
        scanJspConfigForTlds();
        scanWebInfPathForTlds("/WEB-INF/");
        scanJarsForTlds();
        log.trace("JSP taglibs that have been found: " + jspTagLibraries);
        // finally setup the runtime and config
        rctxt = new JspRuntimeContext(ctx, options);
        config = new JspCServletConfig(ctx);
    }

    public JspCResults execute() throws JasperException, IOException, ParserConfigurationException, SAXException, TransformerException, URISyntaxException, XMLStreamException {
        // check everything is OK to start
        if (uriRoot == null && pages.size() > 0) {
            // locate uriRoot if not set/located yet
            locateUriRootFromFirstPage(pages.get(0));
        } else if (pages.isEmpty() && uriRoot != null) {
            // check if we have jsp pages or load all the jsp files in the app
            scanJspFilesinWebApp("/");
        }
        if (pages.isEmpty()) {
            throw new JasperException("No JSP files passed or discovered for compilation");
        }
        // OK prepare and start
        log.debug("JSP to compile: " + pages);
        prepareEnvironmentToCompile();
        // execute the first JSP without threads because of the class name issue
        if (this.targetClassName != null) {
            compileJsp(this.nextJsp());
        }
        // create the threads and execute them
        log.debug(String.format("Compilation will be executed with %d threads", threadCount));
        CompilerThread[] compilers = new CompilerThread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            compilers[i] = new CompilerThread(this);
            compilers[i].start();
        }
        // wait for them
        for (int i = 0; i < threadCount; i++) {
            try {
                compilers[i].join();
            } catch (InterruptedException e) {
                log.warn("Interruped waiting for thread " + i, e);
            }
        }
        // write the XML if not error or forced
        if (!results.isError() || !failOnError) {
            writeWebXml();
        }
        return results;
    }

    public void compileJsp(String jsp) {
        log.trace("jsp=" + jsp);
        String jspUri = jsp;
        ClassLoader originalClassLoader = null;
        try {
            // the jsp should be under uriRoot
            File fjsp = new File(jsp);
            String absPath = fjsp.getCanonicalPath();
            if (absPath.startsWith(uriRoot)) {
                // given JSP files directly the uriRoot should be removed
                jspUri = absPath.substring(uriRoot.length());
            }
            jspUri = jspUri.replace('\\', '/');
            log.trace("final JSP to compile: " + jspUri);
            // generate the servlet compiler for jastow
            JspCServletWrapper jsw = new JspCServletWrapper(config, options, jspUri, rctxt);
            // assign the package name and class
            if ((targetClassName != null) && (targetClassName.length() > 0)) {
                jsw.setServletClassName(targetClassName);
                targetClassName = null; // only the first class
            }
            if (targetPackage != null) {
                jsw.setServletPackageName(targetPackage);
            }
            // assign the class loaders
            originalClassLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(loader);
            // compile
            jsw.setClassLoader(loader);
            jsw.compile();
            String servletName = ("".equals(jsw.getServletPackageName()))?
                    jsw.getServletClassName() : jsw.getServletPackageName() + '.' + jsw.getServletClassName();
            // delete java file if necessary
            if (deleteSources) {
                final Path javaFile = this.options.getScratchDir().toPath().resolve(servletName.replace('.', File.separatorChar) + ".java");
                Files.deleteIfExists(javaFile);
            }
            // add the results to the list
            this.results.addSuccess(jspUri, servletName);

            log.info("Built file: " + jsp);
        } catch (Throwable e) {
            log.warn("Error in file: " + jsp, e);
            this.results.addError(jspUri, e);
        } finally {
            if (originalClassLoader != null) {
                Thread.currentThread().setContextClassLoader(originalClassLoader);
            }
        }
    }

    // main

    public static void main(String... args) throws Exception {
        JspC jspc = new JspC(args);
        JspCResults results = jspc.execute();
        System.out.println(String.format("Generation completed for [%d] files with [%d] errors in [%d] milliseconds",
                results.total(), results.errors(), results.getTime()));
        System.exit(results.getErrorCode());
    }

}
