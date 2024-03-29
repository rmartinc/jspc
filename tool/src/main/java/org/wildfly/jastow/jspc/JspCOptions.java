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

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.apache.jasper.Options;
import org.apache.jasper.compiler.JspConfig;
import org.apache.jasper.compiler.TagPluginManager;
import org.apache.jasper.deploy.TagLibraryInfo;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;

/**
 *
 * @author rmartinc
 */
public class JspCOptions implements Options {

    private boolean errorOnUseBeanInvalidClassAttribute =true;
    private boolean poolingEnabled = true;
    private boolean mappedFile = false;
    private boolean classDebugInfo = true;
    private boolean smapSuppressed = true;
    private boolean smapDumped = false;
    private boolean trimSpaces = false;
    private File scratchDir = new File(System.getProperty("java.io.tmpdir")).getAbsoluteFile();
    private String classPath = null;
    private String compiler = null;
    private String compilerTargetVM = CompilerOptions.VERSION_11;
    private String compilerSourceVM = CompilerOptions.VERSION_11;
    private String javaEncoding = "UTF-8";
    private boolean fork = false;
    private JspConfig jspConfig = null;
    private boolean xpoweredBy = false;
    private TagPluginManager tagPluginManager = null;
    private boolean genStringAsCharArray = false;
    private boolean caching = true;
    protected final Map<String, TagLibraryInfo> cache = new HashMap<>();
    private boolean optimizeJSPScriptlets = false;
    private boolean keepGenerated = true;
    
    public JspCOptions(JspCServletContext ctx) {
        jspConfig = new JspConfig(ctx);
        tagPluginManager = new TagPluginManager(ctx);
    }

    @Override
    public boolean getErrorOnUseBeanInvalidClassAttribute() {
        return errorOnUseBeanInvalidClassAttribute;
    }

    public JspCOptions getErrorOnUseBeanInvalidClassAttribute(boolean errorOnUseBeanInvalidClassAttribute) {
        this.errorOnUseBeanInvalidClassAttribute = errorOnUseBeanInvalidClassAttribute;
        return this;
    }

    @Override
    public boolean getKeepGenerated() {
        return keepGenerated;
    }

    public JspCOptions setKeepGenerated(boolean keepGenerated) {
        this.keepGenerated = keepGenerated;
        return this;
    }

    @Override
    public boolean isPoolingEnabled() {
        return poolingEnabled;
    }

    public JspCOptions setPoolingEnabled(boolean poolingEnabled) {
        this.poolingEnabled = poolingEnabled;
        return this;
    }

    @Override
    public boolean getMappedFile() {
        return mappedFile;
    }

    public JspCOptions setMappedFile(boolean mappedFile) {
        this.mappedFile = mappedFile;
        return this;
    }

    @Override
    public boolean getClassDebugInfo() {
        return classDebugInfo;
    }

    public JspCOptions setClassDebugInfo(boolean classDebugInfo) {
        this.classDebugInfo = classDebugInfo;
        return this;
    }

    @Override
    public int getCheckInterval() {
        // no sense to return other thing
        return -1;
    }

    @Override
    public boolean getDevelopment() {
        return false;
    }

    @Override
    public boolean getDisplaySourceFragment() {
        return true;
    }

    @Override
    public boolean isSmapSuppressed() {
        return smapSuppressed;
    }

    public JspCOptions setSmapSuppressed(boolean smapSuppressed) {
        this.smapSuppressed = smapSuppressed;
        return this;
    }

    @Override
    public boolean isSmapDumped() {
        return smapDumped;
    }

    public JspCOptions setSmapDumped(boolean smapDumped) {
        this.smapDumped = smapDumped;
        return this;
    }

    @Override
    public boolean getTrimSpaces() {
        return trimSpaces;
    }

    public JspCOptions setTrimSpaces(boolean trimSpaces) {
        this.trimSpaces = trimSpaces;
        return this;
    }

    @Override
    public File getScratchDir() {
        return scratchDir;
    }

    public JspCOptions setScratchDir(File scratchDir) {
        this.scratchDir = scratchDir;
        return this;
    }

    @Override
    public String getClassPath() {
        return this.classPath;
    }

    public JspCOptions setClassPath(String classPath) {
        this.classPath = classPath;
        return this;
    }

    @Override
    public String getCompiler() {
        return compiler;
    }

    public JspCOptions setCompiler(String compiler) {
        this.compiler = compiler;
        return this;
    }

    @Override
    public String getCompilerTargetVM() {
        return compilerTargetVM;
    }

    public JspCOptions setCompilerTargetVM(String compilerTargetVM) {
        this.compilerTargetVM = compilerTargetVM;
        return this;
    }

    @Override
    public String getCompilerSourceVM() {
        return compilerSourceVM;
    }

    public JspCOptions setCompilerSourceVM(String compilerSourceVM) {
        this.compilerSourceVM = compilerSourceVM;
        return this;
    }

    @Override
    public String getCompilerClassName() {
        return null;
    }

    @Override
    public String getJavaEncoding() {
        return javaEncoding;
    }

    public JspCOptions setJavaEncoding(String javaEncoding) {
        this.javaEncoding = javaEncoding;
        return this;
    }

    @Override
    public boolean getFork() {
        return fork;
    }

    public JspCOptions setFork(boolean fork) {
        this.fork = fork;
        return this;
    }

    @Override
    public JspConfig getJspConfig() {
        return jspConfig;
    }

    public JspCOptions setJspConfig(JspConfig jspConfig) {
        this.jspConfig = jspConfig;
        return this;
    }

    @Override
    public boolean isXpoweredBy() {
        return xpoweredBy;
    }

    public JspCOptions setXpoweredBy(boolean xpoweredBy) {
        this.xpoweredBy = xpoweredBy;
        return this;
    }

    @Override
    public TagPluginManager getTagPluginManager() {
        return tagPluginManager;
    }

    public JspCOptions setTagPluginManager(TagPluginManager tagPluginManager) {
        this.tagPluginManager = tagPluginManager;
        return this;
    }

    @Override
    public boolean genStringAsCharArray() {
        return genStringAsCharArray;
    }

    public JspCOptions setGenStringAsCharArray(boolean genStringAsCharArray) {
        this.genStringAsCharArray = genStringAsCharArray;
        return this;
    }

    @Override
    public int getModificationTestInterval() {
        return 0;
    }

    @Override
    public boolean getRecompileOnFail() {
        return false;
    }

    @Override
    public boolean isCaching() {
        return caching;
    }

    public JspCOptions setCaching(boolean caching) {
        this.caching = caching;
        return this;
    }

    @Override
    public Map getCache() {
        return cache;
    }

    @Override
    public int getMaxLoadedJsps() {
        return -1;
    }

    @Override
    public int getJspIdleTimeout() {
        return -1;
    }

    @Override
    public boolean isOptimizeJSPScriptlets() {
        return optimizeJSPScriptlets;
    }

    public JspCOptions setOptimizeJSPScriptlets(boolean optimizeJSPScriptlets) {
        this.optimizeJSPScriptlets = optimizeJSPScriptlets;
        return this;
    }
}
