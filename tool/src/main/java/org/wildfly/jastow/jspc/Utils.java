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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.apache.jasper.deploy.FunctionInfo;
import org.apache.jasper.deploy.JspPropertyGroup;
import org.apache.jasper.deploy.TagAttributeInfo;
import org.apache.jasper.deploy.TagFileInfo;
import org.apache.jasper.deploy.TagInfo;
import org.apache.jasper.deploy.TagLibraryInfo;
import org.apache.jasper.deploy.TagLibraryValidatorInfo;
import org.apache.jasper.deploy.TagVariableInfo;
import org.jboss.annotation.javaee.Icon;
import org.jboss.metadata.javaee.spec.DescriptionGroupMetaData;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.parser.jsp.TldMetaDataParser;
import org.jboss.metadata.web.spec.AttributeMetaData;
import org.jboss.metadata.web.spec.FunctionMetaData;
import org.jboss.metadata.web.spec.JspConfigMetaData;
import org.jboss.metadata.web.spec.JspPropertyGroupMetaData;
import org.jboss.metadata.web.spec.ListenerMetaData;
import org.jboss.metadata.web.spec.TagFileMetaData;
import org.jboss.metadata.web.spec.TagMetaData;
import org.jboss.metadata.web.spec.TldMetaData;
import org.jboss.metadata.web.spec.VariableMetaData;
import org.jboss.metadata.web.spec.WebMetaData;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 *
 * @author rmartinc
 */
public class Utils {

    public static Document readXmlIntoDocument(File f) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        docFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        docFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        docFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        docFactory.setXIncludeAware(false);
        docFactory.setExpandEntityReferences(false);
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        return docBuilder.parse(f);
    }

    public static void parseTldFile(String location, InputStream is, HashMap<String, TagLibraryInfo> jspTagLibraries) throws XMLStreamException {
        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
            XMLStreamReader xmlReader = factory.createXMLStreamReader(is);
            TldMetaData tldMetaData = TldMetaDataParser.parse(xmlReader);
            Utils.createTldInfo(location, tldMetaData, jspTagLibraries);
        } finally {
            try {
                is.close();
            } catch(IOException e) {};
        }
    }

    // https://github.com/wildfly/wildfly/blob/fd9ac6510eacbc30e8f4365703ca624a411fdd00/undertow/src/main/java/org/wildfly/extension/undertow/deployment/UndertowDeploymentProcessor.java#L607
    public static TagLibraryInfo createTldInfo(final String location, final TldMetaData tldMetaData, final HashMap<String, TagLibraryInfo> ret) {
        String relativeLocation = location;
        String jarPath = null;
        if (relativeLocation != null && relativeLocation.startsWith("/WEB-INF/lib/")) {
            int pos = relativeLocation.indexOf('/', "/WEB-INF/lib/".length());
            if (pos > 0) {
                jarPath = relativeLocation.substring(pos);
                if (jarPath.startsWith("/")) {
                    jarPath = jarPath.substring(1);
                }
                relativeLocation = relativeLocation.substring(0, pos);
            }
        }

        TagLibraryInfo tagLibraryInfo = new TagLibraryInfo();
        if(tldMetaData.getListeners() != null) {
            for (ListenerMetaData l : tldMetaData.getListeners()) {
                tagLibraryInfo.addListener(l.getListenerClass());
            }
        }
        tagLibraryInfo.setTlibversion(tldMetaData.getTlibVersion());
        if (tldMetaData.getJspVersion() == null) {
            tagLibraryInfo.setJspversion(tldMetaData.getVersion());
        } else {
            tagLibraryInfo.setJspversion(tldMetaData.getJspVersion());
        }
        tagLibraryInfo.setShortname(tldMetaData.getShortName());
        tagLibraryInfo.setUri(tldMetaData.getUri());
        if (tldMetaData.getDescriptionGroup() != null) {
            tagLibraryInfo.setInfo(tldMetaData.getDescriptionGroup().getDescription());
        }
        // Validator
        if (tldMetaData.getValidator() != null) {
            TagLibraryValidatorInfo tagLibraryValidatorInfo = new TagLibraryValidatorInfo();
            tagLibraryValidatorInfo.setValidatorClass(tldMetaData.getValidator().getValidatorClass());
            if (tldMetaData.getValidator().getInitParams() != null) {
                for (ParamValueMetaData paramValueMetaData : tldMetaData.getValidator().getInitParams()) {
                    tagLibraryValidatorInfo.addInitParam(paramValueMetaData.getParamName(), paramValueMetaData.getParamValue());
                }
            }
            tagLibraryInfo.setValidator(tagLibraryValidatorInfo);
        }
        // Tag
        if (tldMetaData.getTags() != null) {
            for (TagMetaData tagMetaData : tldMetaData.getTags()) {
                TagInfo tagInfo = new TagInfo();
                tagInfo.setTagName(tagMetaData.getName());
                tagInfo.setTagClassName(tagMetaData.getTagClass());
                tagInfo.setTagExtraInfo(tagMetaData.getTeiClass());
                if (tagMetaData.getBodyContent() != null) {
                    tagInfo.setBodyContent(tagMetaData.getBodyContent().toString());
                }
                tagInfo.setDynamicAttributes(tagMetaData.getDynamicAttributes());
                // Description group
                if (tagMetaData.getDescriptionGroup() != null) {
                    DescriptionGroupMetaData descriptionGroup = tagMetaData.getDescriptionGroup();
                    if (descriptionGroup.getIcons() != null && descriptionGroup.getIcons().value() != null
                            && (descriptionGroup.getIcons().value().length > 0)) {
                        Icon icon = descriptionGroup.getIcons().value()[0];
                        tagInfo.setLargeIcon(icon.largeIcon());
                        tagInfo.setSmallIcon(icon.smallIcon());
                    }
                    tagInfo.setInfoString(descriptionGroup.getDescription());
                    tagInfo.setDisplayName(descriptionGroup.getDisplayName());
                }
                // Variable
                if (tagMetaData.getVariables() != null) {
                    for (VariableMetaData variableMetaData : tagMetaData.getVariables()) {
                        TagVariableInfo tagVariableInfo = new TagVariableInfo();
                        tagVariableInfo.setNameGiven(variableMetaData.getNameGiven());
                        tagVariableInfo.setNameFromAttribute(variableMetaData.getNameFromAttribute());
                        tagVariableInfo.setClassName(variableMetaData.getVariableClass());
                        tagVariableInfo.setDeclare(variableMetaData.getDeclare());
                        if (variableMetaData.getScope() != null) {
                            tagVariableInfo.setScope(variableMetaData.getScope().toString());
                        }
                        tagInfo.addTagVariableInfo(tagVariableInfo);
                    }
                }
                // Attribute
                if (tagMetaData.getAttributes() != null) {
                    for (AttributeMetaData attributeMetaData : tagMetaData.getAttributes()) {
                        TagAttributeInfo tagAttributeInfo = new TagAttributeInfo();
                        tagAttributeInfo.setName(attributeMetaData.getName());
                        tagAttributeInfo.setType(attributeMetaData.getType());
                        tagAttributeInfo.setReqTime(attributeMetaData.getRtexprvalue());
                        tagAttributeInfo.setRequired(attributeMetaData.getRequired());
                        tagAttributeInfo.setFragment(attributeMetaData.getFragment());
                        if (attributeMetaData.getDeferredValue() != null) {
                            tagAttributeInfo.setDeferredValue("true");
                            tagAttributeInfo.setExpectedTypeName(attributeMetaData.getDeferredValue().getType());
                        } else {
                            tagAttributeInfo.setDeferredValue("false");
                        }
                        if (attributeMetaData.getDeferredMethod() != null) {
                            tagAttributeInfo.setDeferredMethod("true");
                            tagAttributeInfo.setMethodSignature(attributeMetaData.getDeferredMethod().getMethodSignature());
                        } else {
                            tagAttributeInfo.setDeferredMethod("false");
                        }
                        tagInfo.addTagAttributeInfo(tagAttributeInfo);
                    }
                }
                tagLibraryInfo.addTagInfo(tagInfo);
            }
        }
        // Tag files
        if (tldMetaData.getTagFiles() != null) {
            for (TagFileMetaData tagFileMetaData : tldMetaData.getTagFiles()) {
                TagFileInfo tagFileInfo = new TagFileInfo();
                tagFileInfo.setName(tagFileMetaData.getName());
                tagFileInfo.setPath(tagFileMetaData.getPath());
                tagLibraryInfo.addTagFileInfo(tagFileInfo);
            }
        }
        // Function
        if (tldMetaData.getFunctions() != null) {
            for (FunctionMetaData functionMetaData : tldMetaData.getFunctions()) {
                FunctionInfo functionInfo = new FunctionInfo();
                functionInfo.setName(functionMetaData.getName());
                functionInfo.setFunctionClass(functionMetaData.getFunctionClass());
                functionInfo.setFunctionSignature(functionMetaData.getFunctionSignature());
                tagLibraryInfo.addFunctionInfo(functionInfo);
            }
        }

        if (jarPath == null && relativeLocation == null) {
            if (!ret.containsKey(tagLibraryInfo.getUri())) {
                ret.put(tagLibraryInfo.getUri(), tagLibraryInfo);
            }
        } else if (jarPath == null) {
            tagLibraryInfo.setLocation("");
            tagLibraryInfo.setPath(relativeLocation);
            if (!ret.containsKey(tagLibraryInfo.getUri())) {
                ret.put(tagLibraryInfo.getUri(), tagLibraryInfo);
            }
            ret.put(relativeLocation, tagLibraryInfo);
        } else {
            tagLibraryInfo.setLocation(relativeLocation);
            tagLibraryInfo.setPath(jarPath);
            if (!ret.containsKey(tagLibraryInfo.getUri())) {
                ret.put(tagLibraryInfo.getUri(), tagLibraryInfo);
            }
            if (jarPath.equals("META-INF/taglib.tld")) {
                ret.put(relativeLocation, tagLibraryInfo);
            }
        }
        return tagLibraryInfo;
    }

    // https://github.com/wildfly/wildfly/blob/1bf993be72b570a0c780a58ba05cabafe1ce7935/undertow/src/main/java/org/wildfly/extension/undertow/deployment/UndertowDeploymentInfoService.java#L1154
    public static Map<String, JspPropertyGroup> createJspConfigPropertyGroups(WebMetaData metaData) {
        final HashMap<String, JspPropertyGroup> result = new HashMap<>();
        // JSP Config
        JspConfigMetaData config = metaData.getJspConfig();
        if (config != null) {
            // JSP Property groups
            List<JspPropertyGroupMetaData> groups = config.getPropertyGroups();
            if (groups != null) {
                for (JspPropertyGroupMetaData group : groups) {
                    org.apache.jasper.deploy.JspPropertyGroup jspPropertyGroup = new org.apache.jasper.deploy.JspPropertyGroup();
                    for (String pattern : group.getUrlPatterns()) {
                        jspPropertyGroup.addUrlPattern(pattern);
                    }
                    jspPropertyGroup.setElIgnored(group.getElIgnored());
                    jspPropertyGroup.setPageEncoding(group.getPageEncoding());
                    jspPropertyGroup.setScriptingInvalid(group.getScriptingInvalid());
                    jspPropertyGroup.setIsXml(group.getIsXml());
                    if (group.getIncludePreludes() != null) {
                        for (String includePrelude : group.getIncludePreludes()) {
                            jspPropertyGroup.addIncludePrelude(includePrelude);
                        }
                    }
                    if (group.getIncludeCodas() != null) {
                        for (String includeCoda : group.getIncludeCodas()) {
                            jspPropertyGroup.addIncludeCoda(includeCoda);
                        }
                    }
                    jspPropertyGroup.setDeferredSyntaxAllowedAsLiteral(group.getDeferredSyntaxAllowedAsLiteral());
                    jspPropertyGroup.setTrimDirectiveWhitespaces(group.getTrimDirectiveWhitespaces());
                    jspPropertyGroup.setDefaultContentType(group.getDefaultContentType());
                    jspPropertyGroup.setBuffer(group.getBuffer());
                    jspPropertyGroup.setErrorOnUndeclaredNamespace(group.getErrorOnUndeclaredNamespace());
                    for (String pattern : jspPropertyGroup.getUrlPatterns()) {
                        // Split off the groups to individual mappings
                        result.put(pattern, jspPropertyGroup);
                    }
                }
            }
        }

        //it looks like jasper needs these in order of least specified to most specific
        final LinkedHashMap<String, JspPropertyGroup> ret = new LinkedHashMap<>();
        final ArrayList<String> paths = new ArrayList<>(result.keySet());
        Collections.sort(paths, new Comparator<String>() {
            @Override
            public int compare(final String o1, final String o2) {
                return o1.length() - o2.length();
            }
        });
        for (String path : paths) {
            ret.put(path, result.get(path));
        }
        return ret;
    }
}
