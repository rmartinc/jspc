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

import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.SessionTrackingMode;
import jakarta.servlet.descriptor.JspConfigDescriptor;
import jakarta.servlet.descriptor.JspPropertyGroupDescriptor;
import jakarta.servlet.descriptor.TaglibDescriptor;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.apache.jasper.Constants;
import org.apache.jasper.JasperException;
import org.apache.jasper.deploy.JspPropertyGroup;
import org.apache.jasper.deploy.TagLibraryInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.metadata.parser.servlet.WebMetaDataParser;
import org.jboss.metadata.parser.util.MetaDataElementParser;
import org.jboss.metadata.property.CompositePropertyResolver;
import org.jboss.metadata.property.PropertyReplacers;
import org.jboss.metadata.property.SimpleExpressionResolver;
import org.jboss.metadata.property.SystemPropertyResolver;
import org.jboss.metadata.web.spec.TaglibMetaData;
import org.jboss.metadata.web.spec.WebMetaData;

/**
 *
 * @author rmartinc
 */
public class JspCServletContext implements ServletContext {

    private final Logger log = LogManager.getLogger(JspC.class.getPackageName());

    private final Map<String,String> initParams = new ConcurrentHashMap<>();
    private final Map<String,Object> attrs = new HashMap<>();
    private File uriRoot;
    private JspConfigDescriptor jspConfigDescriptor;
    private ClassLoader loader;

    public JspCServletContext() {
        HashMap<String, TagLibraryInfo> jspTagLibraries = new HashMap<>();
        attrs.put(Constants.JSP_TAG_LIBRARIES, jspTagLibraries);
    }

    public void setUriRoot(String uriRoot) {
        this.uriRoot = new File(uriRoot);
    }

    public void setClassLoader(ClassLoader loader) {
        this.loader = loader;
    }

    public HashMap<String, TagLibraryInfo> getJspTagLibraries() {
        return (HashMap<String, TagLibraryInfo>) attrs.get(Constants.JSP_TAG_LIBRARIES);
    }

    @Override
    public String getContextPath() {
        return null;
    }

    @Override
    public ServletContext getContext(String uripath) {
        return null;
    }

    @Override
    public int getMajorVersion() {
        return 3;
    }

    @Override
    public int getMinorVersion() {
        return 1;
    }

    @Override
    public int getEffectiveMajorVersion() {
        return 3;
    }

    @Override
    public int getEffectiveMinorVersion() {
        return 1;
    }

    @Override
    public String getMimeType(String file) {
        return null;
    }

    private void appendPathsInDirectory(String path, String basePath, Set<String> paths) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(basePath))) {
            for (Path child : stream) {
                if (!Files.isDirectory(child)) {
                    paths.add(path + child.getFileName().toString());
                } else {
                    paths.add(path + child.getFileName().toString() + "/");
                }
            }
        } catch (IOException e) {
            log.debug("Error adding paths from directory", e);
        }
    }

    private void appendPathsInJar(String path, URL url, Set<String> paths) {
        try {
            JarURLConnection conn = (JarURLConnection) url.openConnection();
            try (JarFile jarFile = conn.getJarFile()) {
                Enumeration<JarEntry> e = jarFile.entries();
                while (e.hasMoreElements()) {
                    JarEntry entry = e.nextElement();
                    if (entry.getName().startsWith("META-INF/resources" + path)) {
                        String realPath = entry.getName().substring(("META-INF/resources" + path).length() - 1);
                        if (realPath.length() > 1) {
                            int sep = realPath.indexOf("/", 1);
                            if (sep < 0) {
                                // this is a file
                                paths.add(realPath);
                            } else {
                                // it's a directory, include the "/"
                                realPath = realPath.substring(0, sep + 1);
                                if (!paths.contains(realPath)) {
                                    paths.add(realPath);
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                log.debug("Error adding paths from jar file: " + url, e);
            }
        } catch (IOException e) {
            log.debug("Error adding paths from jar file: " + url, e);
        }
    }

    @Override
    public Set<String> getResourcePaths(String path) {
        log.trace("path=" + path);
        Set<String> paths = new HashSet<>();
        if (!path.endsWith("/")) {
            path += "/";
        }
        String basePath = getRealPath(path);
        log.trace("basePath=" + basePath);
        if (basePath != null) {
            appendPathsInDirectory(path, basePath, paths);
        }
        // add META-INF/resources from jars/classpath
        if (loader != null) {
            try {
                String resourcesPath = "META-INF/resources" + path;
                Enumeration<URL> e = loader.getResources(resourcesPath);
                while (e.hasMoreElements()) {
                    URL url = e.nextElement();
                    log.trace("loading loader url=" + url);
                    if (url.getProtocol().equals("file")) {
                        appendPathsInDirectory(path, url.getPath(), paths);
                    } else if (url.getProtocol().equals("jar")) {
                        appendPathsInJar(path, url, paths);
                    } else {
                        log.warn("Unmanaged protocol calculating resource paths from class loader: " + url);
                    }
                }
            } catch (IOException e) {
                log.debug("Error loading resources from class loader", e);
            }
        }
        log.trace("result=" + paths);
        return paths;
    }

    @Override
    public URL getResource(String path) throws MalformedURLException {
        log.trace("path=" + path);
        // local files inside uriRoot
        if (!path.startsWith("/")) {
            throw new MalformedURLException(String.format("File should start with /. Invalid file: %s", path));
        }
        URL url = new URL(uriRoot.getAbsoluteFile().toURI().toURL(), path.substring(1));
        try (InputStream is = url.openStream()) {
            return url;
        } catch (IOException e) {
            log.trace("File not found in local path: " + path, e);
            url = null;
        }
        // search on JARS under the /META-INF/resources
        if (loader != null) {
            url = loader.getResource("META-INF/resources" + path);
        }
        log.trace("result=" + url);
        return url;
    }

    @Override
    public InputStream getResourceAsStream(String path) {
        InputStream is = null;
        try {
            URL url = this.getResource(path);
            if (url != null) {
                is = url.openStream();
            }
            return is;
        } catch (IOException e) {
            log.debug("Error loading resource: " + path, e);
            return null;
        }
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        return null;
    }

    @Override
    public RequestDispatcher getNamedDispatcher(String name) {
        return null;
    }

    @Override
    public void log(String msg) {
        log.warn(msg);
    }

    @Override
    public void log(String message, Throwable throwable) {
        log.warn(message, throwable);
    }

    @Override
    public String getRealPath(String path) {
        if (!path.startsWith("/")) {
            return null;
        }
        try {
            URL url = getResource(path);
            if (url != null && "file".equals(url.getProtocol())) {
                return new File(url.toURI()).getAbsolutePath();
            }
        } catch (MalformedURLException | URISyntaxException e) {
            log.debug("Error translating path: " + path, e);
        }
        return null;
    }

    @Override
    public String getServerInfo() {
        return "JspC/Jastow";
    }

    @Override
    public String getInitParameter(String name) {
        return initParams.get(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration(initParams.keySet());
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        return initParams.putIfAbsent(name, value) == null;
    }

    @Override
    public Object getAttribute(String name) {
        return attrs.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attrs.keySet());
    }

    @Override
    public void setAttribute(String name, Object value) {
        attrs.put(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        attrs.remove(name);
    }

    @Override
    public String getServletContextName() {
        return this.getServerInfo();
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, String className) {
        return null;
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet) {
        return null;
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
        return null;
    }

    @Override
    public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
        return null;
    }

    @Override
    public ServletRegistration getServletRegistration(String servletName) {
        return null;
    }

    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        return null;
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, String className) {
        return null;
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
        return null;
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
        return null;
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
        return null;
    }

    @Override
    public FilterRegistration getFilterRegistration(String filterName) {
        return null;
    }

    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        return null;
    }

    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        return null;
    }

    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
        // nothing
    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        return EnumSet.noneOf(SessionTrackingMode.class);
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        return EnumSet.noneOf(SessionTrackingMode.class);
    }

    @Override
    public void addListener(String className) {
        // nothing
    }

    @Override
    public <T extends EventListener> void addListener(T t) {
        // nothing
    }

    @Override
    public void addListener(Class<? extends EventListener> listenerClass) {
        // nothing
    }

    @Override
    public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
        return null;
    }

    public void calculateJspConfigDescriptor() throws JasperException {
        // TODO: web-fragment.xml should also be managed here (order and all the stuff)
        // TODO: and maybe jboss-web.xml jsp-config section too
        try {
            URL url = this.getResource("/WEB-INF/web.xml");
            if (url != null) {
                XMLInputFactory inputFactory = XMLInputFactory.newInstance();
                XMLStreamReader xmlReader = inputFactory.createXMLStreamReader(url.openStream());
                MetaDataElementParser.DTDInfo dtdInfo = new MetaDataElementParser.DTDInfo();
                inputFactory.setXMLResolver(dtdInfo);
                WebMetaData webMetaData = WebMetaDataParser.parse(xmlReader, dtdInfo,
                        PropertyReplacers.resolvingExpressionReplacer(new CompositePropertyResolver((SimpleExpressionResolver) SystemPropertyResolver.INSTANCE)));
                if (webMetaData.getJspConfig() != null) {
                    Map<String, JspPropertyGroup> propsMap = Utils.createJspConfigPropertyGroups(webMetaData);
                    List<TaglibDescriptor> libs = new ArrayList<>();
                    if (webMetaData.getJspConfig().getTaglibs() != null) {
                        for (TaglibMetaData tldMetaData : webMetaData.getJspConfig().getTaglibs()) {
                            libs.add(new TaglibDescriptorImpl(tldMetaData.getTaglibUri(), tldMetaData.getTaglibLocation()));
                        }
                    }
                    List<JspPropertyGroupDescriptor> props = propsMap.values().stream().map(p -> (JspPropertyGroupDescriptor) new JspPropertyGroupDescriptorImpl(p)).collect(Collectors.toList());
                    jspConfigDescriptor = new JspConfigDescriptorImpl(libs, props);
                }
            }
        } catch (FactoryConfigurationError | IOException | XMLStreamException e) {
            throw new JasperException(e);
        }
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        return jspConfigDescriptor;
    }

    @Override
    public ClassLoader getClassLoader() {
        return loader;
    }

    @Override
    public void declareRoles(String... roleNames) {
        // nothing
    }

    @Override
    public String getVirtualServerName() {
        return null;
    }

    public ServletRegistration.Dynamic addJspFile(String string, String string1) {
        return null;
    }

    public int getSessionTimeout() {
        return -1;
    }

    public void setSessionTimeout(int i) {
        // nothing
    }

    public String getRequestCharacterEncoding() {
        return null;
    }

    public void setRequestCharacterEncoding(String string) {
        // nothing
    }

    public String getResponseCharacterEncoding() {
        return null;
    }

    public void setResponseCharacterEncoding(String string) {
        // nothing
    }
}
