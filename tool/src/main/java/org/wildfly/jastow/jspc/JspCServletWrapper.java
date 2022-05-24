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

import jakarta.servlet.ServletConfig;
import java.io.FileNotFoundException;
import org.apache.jasper.JasperException;
import org.apache.jasper.Options;
import org.apache.jasper.compiler.JspRuntimeContext;
import org.apache.jasper.servlet.JspServletWrapper;

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
