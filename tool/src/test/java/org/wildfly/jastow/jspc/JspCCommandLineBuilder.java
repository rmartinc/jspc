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

    protected enum ValueType {NEEDED, OPTIONAL, NONE};

    protected enum JspCArgument {
        WEBAPP("-webapp", ValueType.NEEDED),
        HELP("-help"),
        VERBOSE("-v"),
        VERY_VERBOSE("-vv"),
        OUTPUT_DIR("-d", ValueType.NEEDED),
        LIST_ERRORS("-l"),
        SHOW_SUCCESS("-s"),
        PACKAGE("-p", ValueType.NEEDED),
        CLASSNAME("-c", ValueType.NEEDED),
        MAPPED("-mapped"),
        DIE("-die", ValueType.NEEDED),
        URIBASE("-uribase", ValueType.NEEDED),
        URIROOT("-uriroot", ValueType.NEEDED),
        NO_FAIL_ON_ERROR("-noFailOnError"),
        FAIL_FAST("-failFast"),
        WEB_INC("-webinc", ValueType.NEEDED),
        WEB_FRG("-webfrg", ValueType.NEEDED),
        WEB_XML("-webxml", ValueType.NEEDED),
        WEB_XML_ENCODING("-webxmlencoding", ValueType.NEEDED),
        ADD_WEB_XML_MAPPINGS("-addwebxmlmappings", ValueType.OPTIONAL),
        IE_PUGLIN("-ieplugin", ValueType.NEEDED),
        CLASSPATH("-classpath", ValueType.NEEDED),
        X_POWERED_BY("-xpoweredBy"),
        TRIM_SPACES("-trimSpaces"),
        JAVA_ENCODING("-javaEncoding", ValueType.NEEDED),
        SOURCE("-source", ValueType.NEEDED),
        TARGET("-target", ValueType.NEEDED),
        THREAD_COUNT("-threadCount", ValueType.NEEDED),
        DELETE_SOURCES("-deletesources");

        private final String argument;
        private final ValueType valueType;

        private JspCArgument(String argument) {
            this.argument = argument;
            this.valueType = ValueType.NONE;
        }

        private JspCArgument(String argument, ValueType valueType) {
            this.argument = argument;
            this.valueType = valueType;
        }

        public String getArgument() {
            return argument;
        }

        public boolean isValueNeeded() {
            return valueType == ValueType.NEEDED;
        }

        public boolean isValueNone() {
            return valueType == ValueType.NONE;
        }

        public boolean isValueOptional() {
            return valueType == ValueType.OPTIONAL;
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
