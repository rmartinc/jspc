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
