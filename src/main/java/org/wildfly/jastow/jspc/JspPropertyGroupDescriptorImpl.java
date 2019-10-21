/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.wildfly.jastow.jspc;

import java.util.Collection;
import javax.servlet.descriptor.JspPropertyGroupDescriptor;
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
}