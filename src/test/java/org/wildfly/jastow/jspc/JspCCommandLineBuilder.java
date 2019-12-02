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

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import org.apache.jasper.JasperException;

/**
 *
 * @author rmartinc
 */
public class JspCCommandLineBuilder {

    protected enum JspCArgument {
        WEBAPP("-webapp", true),
        HELP("-help"),
        VERBOSE("-v"),
        VERY_VERBOSE("-vv"),
        OUTPUT_DIR("-d", true),
        LIST_ERRORS("-l"),
        SHOW_SUCCESS("-s"),
        PACKAGE("-p", true),
        CLASSNAME("-c", true),
        MAPPED("-mapped"),
        DIE("-die", true),
        URIBASE("-uribase", true),
        URIROOT("-uriroot", true),
        NO_FAIL_ON_ERROR("-noFailOnError"),
        FAIL_FAST("-failFast"),
        WEB_INC("-webinc", true),
        WEB_FRG("-webfrg", true),
        WEB_XML("-webxml", true),
        WEB_XML_ENCODING("-webxmlencoding", true),
        ADD_WEB_XML_MAPPINGS("-addwebxmlmappings"),
        IE_PUGLIN("-ieplugin", true),
        CLASSPATH("-classpath", true),
        X_POWERED_BY("-xpoweredBy"),
        TRIM_SPACES("-trimSpaces"),
        JAVA_ENCODING("-javaEncoding", true),
        SOURCE("-source", true),
        TARGET("-target", true),
        THREAD_COUNT("-threadCount", true);

        private final String argument;
        private final boolean valueNeeded;

        private JspCArgument(String argument) {
            this.argument = argument;
            this.valueNeeded = false;
        }

        private JspCArgument(String argument, boolean valueNeeded) {
            this.argument = argument;
            this.valueNeeded = valueNeeded;
        }

        public String getArgument() {
            return argument;
        }

        public boolean isValueNeeded() {
            return valueNeeded;
        }
    };

    Map<String, String> arguments;
    List<String> files;

    public JspCCommandLineBuilder() {
        arguments = new HashMap<>();
        files = new ArrayList<>();
    }

    public JspCCommandLineBuilder set(String arg, String value) {
        arguments.put(arg, value);
        return this;
    }

    public JspCCommandLineBuilder set(JspCArgument arg, String value) {
        if (arg.isValueNeeded() && value == null) {
            throw new IllegalArgumentException(String.format("Argument \"%s\" needs a value", arg.getArgument()));
        } else if (!arg.isValueNeeded() && value != null) {
            throw new IllegalArgumentException(String.format("Argument \"%s\" doesn't need a value", arg.getArgument()));
        }
        arguments.put(arg.getArgument(), value);
        return this;
    }

    public JspCCommandLineBuilder set(JspCArgument arg) {
        if (arg.isValueNeeded()) {
            throw new IllegalArgumentException(String.format("Argument \"%s\" needs a value", arg.getArgument()));
        }
        arguments.put(arg.getArgument(), null);
        return this;
    }
    
    public JspCCommandLineBuilder addFile(String... files) {
        this.files.addAll(Arrays.asList(files));
        return this;
    }

    public JspC build() throws IOException, URISyntaxException, XMLStreamException, JasperException {
        List<String> args = new ArrayList<>(this.arguments.size() + this.files.size());
        for (Map.Entry<String, String> e: this.arguments.entrySet()) {
            args.add(e.getKey());
            if (e.getValue() != null && !e.getValue().isEmpty()) {
                args.add(e.getValue());
            }
        }
        for (String file: this.files) {
            args.add(file);
        }
        return new JspC(args.toArray(new String[0]));
    }

}
