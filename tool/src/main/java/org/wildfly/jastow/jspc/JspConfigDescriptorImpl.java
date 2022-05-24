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

import jakarta.servlet.descriptor.JspConfigDescriptor;
import jakarta.servlet.descriptor.JspPropertyGroupDescriptor;
import jakarta.servlet.descriptor.TaglibDescriptor;
import java.util.Collection;

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
