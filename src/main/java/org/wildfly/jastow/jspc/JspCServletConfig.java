/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.wildfly.jastow.jspc;

import java.util.Collections;
import java.util.Enumeration;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

/**
 *
 * @author rmartinc
 */
public class JspCServletConfig implements ServletConfig {

    private final JspCServletContext ctx;

    public JspCServletConfig(JspCServletContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String getServletName() {
        return null;
    }

    @Override
    public ServletContext getServletContext() {
        return ctx;
    }

    @Override
    public String getInitParameter(String name) {
        return null;
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.emptyEnumeration();
    }

}
