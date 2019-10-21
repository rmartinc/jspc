/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.wildfly.jastow.jspc;

import java.util.Collection;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.descriptor.JspPropertyGroupDescriptor;
import javax.servlet.descriptor.TaglibDescriptor;

/**
 *
 * @author rmartinc
 */
public class JspConfigDescriptorImpl implements JspConfigDescriptor {

    private final Collection<TaglibDescriptor> taglibs;
    private final Collection<JspPropertyGroupDescriptor> jspPropertyGroups;

    public JspConfigDescriptorImpl(Collection<TaglibDescriptor> taglibs, Collection<JspPropertyGroupDescriptor> jspPropertyGroups) {
        this.taglibs = taglibs;
        this.jspPropertyGroups = jspPropertyGroups;
    }

    @Override
    public Collection<TaglibDescriptor> getTaglibs() {
        return taglibs;
    }

    @Override
    public Collection<JspPropertyGroupDescriptor> getJspPropertyGroups() {
        return jspPropertyGroups;
    }
}