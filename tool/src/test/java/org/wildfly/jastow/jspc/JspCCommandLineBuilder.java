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
        CLASSPATH("-classpath", true),
        X_POWERED_BY("-xpoweredBy"),
        TRIM_SPACES("-trimSpaces"),
        JAVA_ENCODING("-javaEncoding", true),
        SOURCE("-source", true),
        TARGET("-target", true),
        THREAD_COUNT("-threadCount", true),
        DELETE_SOURCES("-deletesources"),
        MERGE_XML("-mergexml", true);

        private final String argument;
        private final boolean valueNeeded;

        private JspCArgument(String argument) {
            this(argument, false);
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

        public boolean isValueNone() {
            return !valueNeeded;
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
        } else if (arg.isValueNone() && value != null) {
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
