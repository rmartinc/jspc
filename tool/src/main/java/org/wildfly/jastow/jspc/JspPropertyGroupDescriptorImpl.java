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

import jakarta.servlet.descriptor.JspPropertyGroupDescriptor;
import java.util.Collection;
import org.apache.jasper.deploy.JspPropertyGroup;

/**
 *
 * @author rmartinc
 */
public class JspPropertyGroupDescriptorImpl implements JspPropertyGroupDescriptor {

    private final JspPropertyGroup propertyGroup;

    public JspPropertyGroupDescriptorImpl(JspPropertyGroup propertyGroup) {
        this.propertyGroup = propertyGroup;
    }

    @Override
    public Collection<String> getUrlPatterns() {
        return propertyGroup.getUrlPatterns();
    }

    @Override
    public String getElIgnored() {
        return propertyGroup.getElIgnored();
    }

    @Override
    public String getPageEncoding() {
        return propertyGroup.getPageEncoding();
    }

    @Override
    public String getScriptingInvalid() {
        return propertyGroup.getScriptingInvalid();
    }

    @Override
    public String getIsXml() {
        return propertyGroup.getIsXml();
    }

    @Override
    public Collection<String> getIncludePreludes() {
        return propertyGroup.getIncludePreludes();
    }

    @Override
    public Collection<String> getIncludeCodas() {
        return propertyGroup.getIncludeCodas();
    }

    @Override
    public String getDeferredSyntaxAllowedAsLiteral() {
        return propertyGroup.getDeferredSyntaxAllowedAsLiteral();
    }

    @Override
    public String getTrimDirectiveWhitespaces() {
        return propertyGroup.getTrimDirectiveWhitespaces();
    }

    @Override
    public String getDefaultContentType() {
        return propertyGroup.getDefaultContentType();
    }

    @Override
    public String getBuffer() {
        return propertyGroup.getBuffer();
    }

    @Override
    public String getErrorOnUndeclaredNamespace() {
        return propertyGroup.getErrorOnUndeclaredNamespace();
    }

    @Override
    public String getErrorOnELNotFound() {
        return propertyGroup.getErrorOnELNotFound();
    }
}
