/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.wildfly.jastow.jspc;

import javax.servlet.descriptor.TaglibDescriptor;

/**
 *
 * @author rmartinc
 */
public class TaglibDescriptorImpl implements TaglibDescriptor {

    private final String tagLibURI;
    private final String taglibLocation;

    public TaglibDescriptorImpl(String tagLibURI, String taglibLocation) {
        this.tagLibURI = tagLibURI;
        this.taglibLocation = taglibLocation;
    }

    @Override
    public String getTaglibURI() {
        return tagLibURI;
    }

    @Override
    public String getTaglibLocation() {
        return taglibLocation;
    }
}
