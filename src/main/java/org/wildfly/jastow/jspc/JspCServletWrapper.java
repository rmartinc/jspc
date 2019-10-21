/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.wildfly.jastow.jspc;

import java.io.FileNotFoundException;
import javax.servlet.ServletConfig;
import org.apache.jasper.Options;
import org.apache.jasper.compiler.JspRuntimeContext;
import org.apache.jasper.servlet.JspServletWrapper;
import org.apache.jasper.JasperException;

/**
 *
 * @author rmartinc
 */
public class JspCServletWrapper extends JspServletWrapper {

    public JspCServletWrapper(ServletConfig config, Options options, String jspUri, JspRuntimeContext rctxt) {
        super(config, options, jspUri, rctxt);
    }

    public void setClassLoader(ClassLoader loader) {
        this.getJspEngineContext().setClassLoader(loader);
    }

    public void setServletPackageName(String servletPackageName) {
        this.getJspEngineContext().setServletPackageName(servletPackageName);
    }

    public void setServletClassName(String className) {
        this.getJspEngineContext().setServletClassName(className);
    }

    public void compile() throws JasperException, FileNotFoundException {
        this.getJspEngineContext().compile();
    }

    public String getServletClassName() {
        return this.getJspEngineContext().getServletClassName();
    }

    public String getServletPackageName() {
        return this.getJspEngineContext().getServletPackageName();
    }
}
