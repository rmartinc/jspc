/*
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
