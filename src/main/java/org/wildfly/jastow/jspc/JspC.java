/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.wildfly.jastow.jspc;

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
import java.util.ArrayList;
import java.util.Arrays;
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
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.descriptor.TaglibDescriptor;
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
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
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

    private static final Logger log = Logger.getLogger(JspC.class);

    private enum WEBXML_LEVEL {INC_WEBXML, FRG_WEBXML, ALL_WEBXML, MERGE_WEBXML};

    private final JspCServletContext ctx;
    private final JspCOptions options;
    private String uriRoot;
    private String uriBase;
    private String targetPackage = null;
    private String targetClassName = null;
    private String webxmlFile;
    private Charset webxmlEncoding = StandardCharsets.UTF_8;
    private final ClassLoader loader;
    private final JspRuntimeContext rctxt;
    private final JspCServletConfig config;
    private final HashMap<String, TagLibraryInfo> jspTagLibraries;
    private final List<String> pages = new ArrayList<>();
    private Iterator<String> pagesIterator;
    private JspCResults results;
    private WEBXML_LEVEL webxmlLevel;
    private boolean failOnError = true;
    private boolean failFast = false;
    private int threadCount = (Runtime.getRuntime().availableProcessors() / 2) + 1;

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
                .append("    -webxmlencoding <enc> Set the encoding charset used to read and write the web.xml").append(nl)
                .append("                          file (default is UTF-8)").append(nl)
                .append("    -addwebxmlmappings    Merge generated web.xml fragment into the web.xml file of the").append(nl)
                .append("                          web-app, whose JSP pages we are processing").append(nl)
                .append("    -ieplugin <clsid>     Java Plugin classid for Internet Explorer").append(nl)
                .append("    -classpath <path>     Overrides java.class.path system property").append(nl)
                .append("    -xpoweredBy           Add X-Powered-By response header").append(nl)
                .append("    -trimSpaces           Remove template text that consists entirely of whitespace").append(nl)
                .append("    -javaEncoding <enc>   Set the encoding charset for Java classes (default UTF-8)").append(nl)
                .append("    -source <version>     Set the -source argument to the compiler (default 1.8)").append(nl)
                .append("    -target <version>     Set the -target argument to the compiler (default 1.8)").append(nl)
                .append("    -threadCount <count>  Number of threads to use for compilation.").append(nl)
                .append("                          (\"2.0C\" means two threads per core)").append(nl);
        throw new IllegalArgumentException(sb.toString());
    }

    // parse methods

    private String parseDirectory(String option, String file) {
        File dir = new File(file);
        if (!dir.isDirectory()) {
            usage(String.format("Invalid directory \"%s\" for option \"%s\"", file, option));
        }
        return file;
    }

    private String parseWritableFile(String option, String file) {
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
        return file;
    }

    private Charset parseCharset(String option, String charset) {
        try {
            return Charset.forName(charset);
        } catch (Exception e) {
            usage(String.format("Invalid chasert file \"%s\" for option \"%s\"", charset, option));
            return null;
        }
    }

    private int parseInteger(String option, String number) {
        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException e) {
            usage(String.format("Invalid number \"%s\" for option \"%s\"", number, option));
            return -1;
        }
    }

    private void setDebugLevel(Level l) {
        Logger jspcLogger = Logger.getLogger(this.getClass().getPackage().getName());
        if (jspcLogger.getLevel() == null || jspcLogger.getLevel().toInt() > l.toInt()) {
            jspcLogger.setLevel(l);
        }
    }

    private void parseArgs(String... args) {
        boolean finished = false;
        int i;
        for (i = 0; i < args.length && !finished; i++) {
            switch(args[i]) {
                case "-webapp":
                case "-uriroot":
                    uriRoot = parseDirectory(args[i], args[++i]);
                    break;
                case "-help":
                    usage(null);
                    break;
                case "-d":
                    String outputDir = parseDirectory(args[i], args[++i]);
                    options.setScratchDir(new File(outputDir));
                    break;
                case "-vv":
                    setDebugLevel(Level.TRACE);
                    break;
                case "-v":
                    setDebugLevel(Level.DEBUG);
                    break;
                case "-s":
                    setDebugLevel(Level.INFO);
                    break;
                case "-l":
                    setDebugLevel(Level.WARN);
                    break;
                case "-p":
                    targetPackage = args[++i];
                    break;
                case "-c":
                    targetClassName = args[++i];
                    break;
                case "-mapped":
                    options.setMappedFile(true);
                    break;
                case "die":
                    int errorCode = parseInteger(args[i], args[++i]);
                    this.results = new JspCResults(errorCode);
                    break;
                case "-uribase":
                    uriBase = args[++i];
                    break;
                // "compile" flag has no sense because we always do this
                // the JSP is compiled and if exists it is checked for changes
                // case "-compile":
                //    usage(String.format("Not implemented option \"%s\"", args[i]));
                //    break;
                case "-noFailOnError":
                    this.failOnError = false;
                    break;
                case "-failFast":
                    this.failFast = true;
                    break;
                case "-webinc":
                    webxmlFile = parseWritableFile(args[i], args[++i]);
                    if (webxmlLevel != null) {
                        usage(String.format("Invalid -webinc option because the ouput was previously set to \"%s\"", webxmlLevel.name()));
                    }
                    webxmlLevel = WEBXML_LEVEL.INC_WEBXML;
                    break;
                case "-webfrg":
                    webxmlFile = parseWritableFile(args[i], args[++i]);
                    if (webxmlLevel != null) {
                        usage(String.format("Invalid -webfrg option because the ouput was previously set to \"%s\"", webxmlLevel.name()));
                    }
                    webxmlLevel = WEBXML_LEVEL.FRG_WEBXML;
                    break;
                case "-webxml":
                    webxmlFile = parseWritableFile(args[i], args[++i]);
                    if (webxmlLevel != null) {
                        usage(String.format("Invalid -webxml option because the ouput was previously set to \"%s\"", webxmlLevel.name()));
                    }
                    webxmlLevel = WEBXML_LEVEL.ALL_WEBXML;
                    break;
                case "-addwebxmlmappings":
                    if (webxmlLevel != null) {
                        usage(String.format("Invalid -addwebxmlmappings option because the ouput was previously set to \"%s\"", webxmlLevel.name()));
                    }
                    webxmlLevel = WEBXML_LEVEL.MERGE_WEBXML;
                    break;
                case "-webxmlencoding":
                    webxmlEncoding = parseCharset(args[i], args[++i]);
                    break;
                case "-ieplugin":
                    options.setIeClassId(args[++i]);
                    break;
                case "-classpath":
                    options.setClassPath(args[++i]);
                    break;
                case "-xpoweredBy":
                    options.setXpoweredBy(true);
                    break;
                case "-trimSpaces":
                    options.setTrimSpaces(true);
                    break;
                case "-javaEncoding":
                    options.setJavaEncoding(args[++i]);
                    break;
                case "-source":
                    options.setCompilerSourceVM(args[++i]);
                    break;
                case "-target":
                    options.setCompilerTargetVM(args[++i]);
                    break;
                case "-threadCount":
                    threadCount = parseInteger(args[i], args[++i]);
                    if (threadCount <= 0) {
                        usage(String.format("Invalid number of threads \"%s\"", args[i]));
                    }
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
            locateUriRootFromFirstPage(pages.get(0));
            if (uriRoot == null) {
                usage("The webapp path cannot be located using the first JSP file");
            }
        }
        // create the results if no error code passed
        if (this.results == null) {
            this.results = new JspCResults();
        }
    }

    // methods to locate JSP files and the web root if not passed

    private void scanJspFilesinWebApp(String input) {
        Set<String> paths = ctx.getResourcePaths(input);
        for (String path : paths) {
            if (path.endsWith("/")) {
                scanJspFilesinWebApp(path);
            } else if (path.endsWith(".jsp") || path.endsWith("jspx")) {
                pages.add(path);
            }
        }
    }

    public void locateUriRootFromFirstPage(String jsp) {
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
                    uriRoot = froot.getCanonicalPath();
                }
            }
        } catch (IOException e) {
            // Missing uriRoot will be handled in the caller.
        }
    }

    // constructor

    public JspC(String... args) throws IOException, URISyntaxException, XMLStreamException, JasperException {
        ctx = new JspCServletContext();
        options = new JspCOptions(ctx);
        // parse the arguments
        parseArgs(args);

        // load into the classpath application libs and classes
        loader = setupClassLoader();
        // setup context with missing things
        ctx.setUriRoot(uriRoot);
        ctx.calculateJspConfigDescriptor();
        ctx.setClassLoader(loader);
        // check if we have jsp pages or load all the jsp files in the app
        if (pages.isEmpty()) {
            scanJspFilesinWebApp("/");
            if (pages.isEmpty()) {
                usage(String.format("No JSP pages in webapp \"%s\"", uriRoot));
            }
        }
        log.debug("JSP to compile: " + pages);
        // scan all possible TLD locations for taglibs and set them in the ctx for jastow
        jspTagLibraries = ctx.getJspTagLibraries();
        scanJspConfigForTlds();
        scanWebInfPathForTlds("/WEB-INF/");
        scanJarsForTlds();
        log.trace("JSP taglibs that have been found: " + jspTagLibraries);
        // finally setup the run
        rctxt = new JspRuntimeContext(ctx, options);
        config = new JspCServletConfig(ctx);
    }

    // methods to locate TLD inside jars and app

    private static void scanJar(URL url, Pattern pattern, HashMap<String, TagLibraryInfo> jspTagLibraries) throws IOException {
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

    private void scanFilePath(URL url, HashMap<String, TagLibraryInfo> jspTagLibraries) throws IOException {
        try (Stream<Path> paths = Files.walk(Paths.get(url.getPath()), 2)) {
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
        StringBuilder classpath = new StringBuilder(options.getClassPath()).append(File.pathSeparator);
        List<URL> clUrls = new ArrayList<>();
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
                            clUrls.add(libFile.getAbsoluteFile().toURI().toURL());
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
        // TODO: at least split the method in several parts and create a backup of the web.xml
        String nl = System.getProperty("line.separator");
        File webXml = new File(this.uriRoot, "/WEB-INF/web.xml");
        Set<String> stopElements = new HashSet(Arrays.asList(new String[]{"servlet-mapping", "session-config>",
            "mime-mapping", "welcome-file-list", "error-page", "taglib", "resource-env-ref",
            "resource-ref", "security-constraint", "login-config", "security-role", "env-entry",
            "ejb-ref", "ejb-local-ref"}));
        if (!webXml.exists()) {
            // just write the file from scratch
            this.webxmlFile = webXml.getAbsolutePath();
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
                doc.getDocumentElement().insertBefore(doc.createTextNode(nl + nl + "    "), selectedNode);
                doc.getDocumentElement().insertBefore(doc.createComment("Automatically generated web include"), selectedNode);
                doc.getDocumentElement().insertBefore(doc.createTextNode(nl + "    "), selectedNode);
            } else {
                doc.getDocumentElement().appendChild(doc.createTextNode(nl + nl + "    "));
                doc.getDocumentElement().appendChild(doc.createComment("Automatically generated web include"));
                doc.getDocumentElement().appendChild(doc.createTextNode(nl + "    "));
            }
            for (JspCResults.ResultEntry entry : results.getResults()) {
                Element servlet = doc.createElement("servlet");
                Element servletName = doc.createElement("servlet-name");
                servletName.setTextContent(entry.getServletName());
                Element servletClass = doc.createElement("servlet-class");
                servletClass.setTextContent(entry.getServletName());
                servlet.appendChild(doc.createTextNode(nl + "        "));
                servlet.appendChild(servletName);
                servlet.appendChild(doc.createTextNode(nl + "        "));
                servlet.appendChild(servletClass);
                servlet.appendChild(doc.createTextNode(nl + "    "));
                if (selectedNode != null) {
                    doc.getDocumentElement().insertBefore(servlet, selectedNode);
                    doc.getDocumentElement().insertBefore(doc.createTextNode(nl + "    "), selectedNode);
                } else {
                    doc.getDocumentElement().appendChild(servlet);
                    doc.getDocumentElement().appendChild(doc.createTextNode(nl + "    "));
                }
            }
            for (JspCResults.ResultEntry entry : results.getResults()) {
                Element servletMapping = doc.createElement("servlet-mapping");
                Element servletName = doc.createElement("servlet-name");
                servletName.setTextContent(entry.getServletName());
                Element urlPattern = doc.createElement("url-pattern");
                urlPattern.setTextContent(entry.getJspUri());
                servletMapping.appendChild(doc.createTextNode(nl + "        "));
                servletMapping.appendChild(servletName);
                servletMapping.appendChild(doc.createTextNode(nl + "        "));
                servletMapping.appendChild(urlPattern);
                servletMapping.appendChild(doc.createTextNode(nl + "    "));
                if (selectedNode != null) {
                    doc.getDocumentElement().insertBefore(servletMapping, selectedNode);
                    doc.getDocumentElement().insertBefore(doc.createTextNode(nl + "    "), selectedNode);
                } else {
                    doc.getDocumentElement().appendChild(servletMapping);
                    doc.getDocumentElement().appendChild(doc.createTextNode(nl + "    "));
                }
            }
            if (selectedNode != null) {
                doc.getDocumentElement().insertBefore(doc.createComment("End of web include"), selectedNode);
                doc.getDocumentElement().insertBefore(doc.createTextNode(nl + nl + "    "), selectedNode);
            } else {
                doc.getDocumentElement().appendChild(doc.createComment("End of web include"));
                doc.getDocumentElement().appendChild(doc.createTextNode(nl + nl + "    "));
            }
            // write the contents
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, webxmlEncoding.name());
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(webXml);
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
            writer.write("    <name>org_apache_jasper.jspc</name>");
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
        if (pagesIterator.hasNext() && !(failFast && results.isError())) {
            return pagesIterator.next();
        } else {
            return null;
        }
    }

    public JspCResults execute() throws JasperException, IOException,ParserConfigurationException, SAXException, TransformerException {
        // execute the first JSP without threads because of the class name issue
        if (this.targetClassName != null) {
            compileJsp(this.nextJsp());
        }
        // create the threads and execute them
        log.debug(String.format("Compilation will be executd with %d threads", threadCount));
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
        // the jsp should be under uriRoot
        File fjsp = new File(jsp);
        String absPath = fjsp.getAbsolutePath();
        String jspUri = jsp;
        if (absPath.startsWith(uriRoot)) {
            // given JSP files directly the uriRoot should be removed
            jspUri = absPath.substring(uriRoot.length());
        }
        ClassLoader originalClassLoader = null;
        try {
            jspUri = jspUri.replace('\\', '/');
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
        // TODO: using log4j because logmanager needs to be initialized at
        //       startup using the "-Djava.util.logging.manager=org.jboss.logmanager.LogManager"
        //       and that doesn't work inside the "exec-maven-plugin" easily
        PropertyConfigurator.configure(Thread.currentThread().getContextClassLoader().getResource("log4j.properties"));
        JspC jspc = new JspC(args);
        JspCResults results = jspc.execute();
        System.out.println(String.format("Generation completed for [%d] files with [%d] errors in [%d] milliseconds",
                results.total(), results.errors(), results.getTime()));
        System.exit(results.getErrorCode());
    }

}
