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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import org.apache.log4j.Level;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author rmartinc
 */
public class CommanLineArgumentsTest {

    private static void deleteTemporaryDir(String path) {
        try {
            Path pathToDelete = Paths.get(path);
            if (Files.exists(pathToDelete) && Files.isDirectory(pathToDelete)) {
                Files.walk(pathToDelete)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } catch (IOException e) {
        }
    }

    private static void deleteTemporaryFile(String path) {
        try {
            Files.deleteIfExists(Paths.get(path));
        } catch (IOException e) {
        }
    }

    @Test
    public void testInvalidWebappDirectory() throws Exception {
        IllegalArgumentException e = Assert.assertThrows(IllegalArgumentException.class,
                () -> new JspCCommandLineBuilder()
                        .set(JspCCommandLineBuilder.JspCArgument.WEBAPP, "invalid-directory")
                        .build());
        MatcherAssert.assertThat(e.getMessage(), CoreMatchers.containsString("ERROR: Invalid directory"));
    }

    @Test
    public void testInvalidOuputDirectory() throws Exception {
        IllegalArgumentException e = Assert.assertThrows(IllegalArgumentException.class,
                () -> new JspCCommandLineBuilder()
                        .set(JspCCommandLineBuilder.JspCArgument.OUTPUT_DIR, "invalid-directory")
                        .build());
        MatcherAssert.assertThat(e.getMessage(), CoreMatchers.containsString("ERROR: Invalid directory"));
    }

    @Test
    public void testInvalidErrorCodeNoNumber() throws Exception {
        IllegalArgumentException e = Assert.assertThrows(IllegalArgumentException.class,
                () -> new JspCCommandLineBuilder()
                        .set(JspCCommandLineBuilder.JspCArgument.DIE, "no-number")
                        .build());
        MatcherAssert.assertThat(e.getMessage(), CoreMatchers.containsString("ERROR: Invalid number"));
    }

    @Test
    public void testInvalidErrorCode() throws Exception {
        IllegalArgumentException e = Assert.assertThrows(IllegalArgumentException.class,
                () -> new JspCCommandLineBuilder()
                        .set(JspCCommandLineBuilder.JspCArgument.DIE, "-1")
                        .build());
        MatcherAssert.assertThat(e.getMessage(), CoreMatchers.containsString("ERROR: Invalid number"));
    }

    @Test
    public void testInvalidWebfrgFile() throws Exception {
        IllegalArgumentException e = Assert.assertThrows(IllegalArgumentException.class,
                () -> new JspCCommandLineBuilder()
                        .set(JspCCommandLineBuilder.JspCArgument.WEB_FRG, "invalid-dir/invalid-file")
                        .build());
        MatcherAssert.assertThat(e.getMessage(), CoreMatchers.containsString("ERROR: Invalid writable file"));
    }

    @Test
    public void testInvalidWebincFile() throws Exception {
        IllegalArgumentException e = Assert.assertThrows(IllegalArgumentException.class,
                () -> new JspCCommandLineBuilder()
                        .set(JspCCommandLineBuilder.JspCArgument.WEB_INC, "invalid-dir/invalid-file")
                        .build());
        MatcherAssert.assertThat(e.getMessage(), CoreMatchers.containsString("ERROR: Invalid writable file"));
    }

    @Test
    public void testInvalidMergeXmlMappings() throws Exception {
        IllegalArgumentException e = Assert.assertThrows(IllegalArgumentException.class,
                () -> new JspCCommandLineBuilder()
                        .set(JspCCommandLineBuilder.JspCArgument.MERGE_XML, "invalid-dir/invalid-file")
                        .build());
        MatcherAssert.assertThat(e.getMessage(), CoreMatchers.containsString("ERROR: Invalid writable file"));
    }

    @Test
    public void testInvalidWebxmlFile() throws Exception {
        IllegalArgumentException e = Assert.assertThrows(IllegalArgumentException.class,
                () -> new JspCCommandLineBuilder()
                        .set(JspCCommandLineBuilder.JspCArgument.WEB_XML, "invalid-dir/invalid-file")
                        .build());
        MatcherAssert.assertThat(e.getMessage(), CoreMatchers.containsString("ERROR: Invalid writable file"));
    }

    @Test
    public void testInvalidTwoWebOptions() throws Exception {
        String webinc = Files.createTempFile("webinc", ".xml").toFile().getCanonicalFile().getAbsolutePath();
        String webfrg = Files.createTempFile("webfrg", ".xml").toFile().getCanonicalFile().getAbsolutePath();
        try {
            IllegalArgumentException e = Assert.assertThrows(IllegalArgumentException.class,
                    () -> new JspCCommandLineBuilder()
                            .set(JspCCommandLineBuilder.JspCArgument.WEB_INC, webinc)
                            .set(JspCCommandLineBuilder.JspCArgument.WEB_FRG, webfrg)
                            .build());
            MatcherAssert.assertThat(e.getMessage(), CoreMatchers.containsString("because the output was previously set to"));
        } finally {
            deleteTemporaryFile(webfrg);
            deleteTemporaryFile(webinc);
        }
    }

    @Test
    public void testInvalidThreadCount() throws Exception {
        IllegalArgumentException e = Assert.assertThrows(IllegalArgumentException.class,
                () -> new JspCCommandLineBuilder()
                        .set(JspCCommandLineBuilder.JspCArgument.THREAD_COUNT, "no-number")
                        .build());
        MatcherAssert.assertThat(e.getMessage(), CoreMatchers.containsString("ERROR: Invalid number"));
    }

    @Test
    public void testInvalidThreadCountNoPositiveNumber() throws Exception {
        IllegalArgumentException e = Assert.assertThrows(IllegalArgumentException.class,
          () -> new JspCCommandLineBuilder()
                .set(JspCCommandLineBuilder.JspCArgument.THREAD_COUNT, "0")
                .build());
        MatcherAssert.assertThat(e.getMessage(), CoreMatchers.containsString("ERROR: Invalid number"));
    }

    @Test
    public void testNoJSP() throws Exception {
        IllegalArgumentException e = Assert.assertThrows(IllegalArgumentException.class,
                () -> new JspCCommandLineBuilder()
                        .build());
        MatcherAssert.assertThat(e.getMessage(), CoreMatchers.containsString("ERROR: No -webapp or JSP files passed"));
    }

    @Test
    public void testNoJSPInWebapp() throws Exception {
        IllegalArgumentException e = Assert.assertThrows(IllegalArgumentException.class,
                () -> new JspCCommandLineBuilder()
                        .set(JspCCommandLineBuilder.JspCArgument.WEBAPP, "src")
                        .build());
        MatcherAssert.assertThat(e.getMessage(), CoreMatchers.containsString("ERROR: No JSP pages in webapp"));
    }

    @Test
    public void testDefaultValues() throws Exception {
        JspC jspc = new JspCCommandLineBuilder()
                .addFile("samples/simple.jsp")
                .build();
        Assert.assertEquals("Argument webapp assigned", new File("samples").getCanonicalFile(), new File(jspc.getUriRoot()).getCanonicalFile());
        Assert.assertEquals("Output dir is tmp", new File(System.getProperty("java.io.tmpdir")).getAbsoluteFile(), jspc.getOptions().getScratchDir());
        Assert.assertNull("Package is null", jspc.getTargetPackage());
        Assert.assertNull("Class name is null", jspc.getTargetClassName());
        Assert.assertEquals("Debug level", Level.WARN, jspc.getDebugLevel());
        Assert.assertEquals("Mapped option", false, jspc.getOptions().getMappedFile());
        Assert.assertEquals("Uribase option", "/", jspc.getUriBase());
        Assert.assertEquals("fail on error option", true, jspc.isFailOnError());
        Assert.assertEquals("fail on error option", false, jspc.isFailFast());
        Assert.assertNull("WEB output options", jspc.getWebxmlLevel());
        Assert.assertNull("WEB output options", jspc.getWebxmlFile());
        Assert.assertEquals("webxmlEncoding option", StandardCharsets.UTF_8, jspc.getWebxmlEncoding());
        Assert.assertEquals("ieplugin option", "clsid:8AD9C840-044E-11D1-B3E9-00805F499D93", jspc.getOptions().getIeClassId());
        Assert.assertNull("classpath option", jspc.getOptions().getClassPath());
        Assert.assertEquals("xpoweredby option", false, jspc.getOptions().isXpoweredBy());
        Assert.assertEquals("trimSpaces option", false, jspc.getOptions().getTrimSpaces());
        Assert.assertEquals("javaEncoding option", StandardCharsets.UTF_8.displayName(), jspc.getOptions().getJavaEncoding());
        Assert.assertEquals("target option", "1.8", jspc.getOptions().getCompilerSourceVM());
        Assert.assertEquals("target option", "1.8", jspc.getOptions().getCompilerTargetVM());
        Assert.assertEquals("threadCount option", (Runtime.getRuntime().availableProcessors() / 2) + 1, jspc.getThreadCount());
        Assert.assertEquals("deletesources option", false, jspc.getDeleteSources());
    }

    @Test
    public void testDifferentValues() throws Exception {
        String outputDir = Files.createTempDirectory("output").toFile().getCanonicalFile().toString();
        String webincFile = Files.createTempFile("webinc", ".xml").toFile().getCanonicalFile().toString();
        try {
            JspC jspc = new JspCCommandLineBuilder()
                    .set(JspCCommandLineBuilder.JspCArgument.WEBAPP, "./samples/")
                    .set(JspCCommandLineBuilder.JspCArgument.OUTPUT_DIR, outputDir)
                    .set(JspCCommandLineBuilder.JspCArgument.PACKAGE, "com.samples.precompiled")
                    .set(JspCCommandLineBuilder.JspCArgument.CLASSNAME, "FirstJSP")
                    .set(JspCCommandLineBuilder.JspCArgument.VERBOSE)
                    .set(JspCCommandLineBuilder.JspCArgument.MAPPED)
                    .set(JspCCommandLineBuilder.JspCArgument.URIBASE, "/test")
                    .set(JspCCommandLineBuilder.JspCArgument.NO_FAIL_ON_ERROR)
                    .set(JspCCommandLineBuilder.JspCArgument.FAIL_FAST)
                    .set(JspCCommandLineBuilder.JspCArgument.WEB_INC, webincFile)
                    .set(JspCCommandLineBuilder.JspCArgument.WEB_XML_ENCODING, StandardCharsets.ISO_8859_1.displayName())
                    .set(JspCCommandLineBuilder.JspCArgument.IE_PUGLIN, "another-plugin-id")
                    .set(JspCCommandLineBuilder.JspCArgument.CLASSPATH, "lala.jar")
                    .set(JspCCommandLineBuilder.JspCArgument.X_POWERED_BY)
                    .set(JspCCommandLineBuilder.JspCArgument.TRIM_SPACES)
                    .set(JspCCommandLineBuilder.JspCArgument.JAVA_ENCODING, StandardCharsets.ISO_8859_1.displayName())
                    .set(JspCCommandLineBuilder.JspCArgument.SOURCE, "1.7")
                    .set(JspCCommandLineBuilder.JspCArgument.TARGET, "1.7")
                    .set(JspCCommandLineBuilder.JspCArgument.THREAD_COUNT, "1")
                    .set(JspCCommandLineBuilder.JspCArgument.DELETE_SOURCES)
                    .addFile("samples/simple.jsp")
                    .build();
            Assert.assertEquals("Argument webapp assigned", new File("samples").getCanonicalFile(), new File(jspc.getUriRoot()).getCanonicalFile());
            Assert.assertEquals("output option", outputDir, jspc.getOptions().getScratchDir().getCanonicalFile().getAbsolutePath());
            Assert.assertEquals("Package option", "com.samples.precompiled", jspc.getTargetPackage());
            Assert.assertEquals("Classanem option", "FirstJSP", jspc.getTargetClassName());
            Assert.assertEquals("Debug level", Level.DEBUG, jspc.getDebugLevel());
            Assert.assertEquals("Mapped option", true, jspc.getOptions().getMappedFile());
            Assert.assertEquals("Uribase option", "/test", jspc.getUriBase());
            Assert.assertEquals("fail on error option", false, jspc.isFailOnError());
            Assert.assertEquals("fail on error option", true, jspc.isFailFast());
            Assert.assertEquals("WEB output options", JspC.WEBXML_LEVEL.INC_WEBXML, jspc.getWebxmlLevel());
            Assert.assertEquals("WEB output options", webincFile, jspc.getWebxmlFile());
            Assert.assertEquals("webxmlEncoding option", StandardCharsets.ISO_8859_1, jspc.getWebxmlEncoding());
            Assert.assertEquals("ieplugin option", "another-plugin-id", jspc.getOptions().getIeClassId());
            Assert.assertEquals("classpath option", "lala.jar", jspc.getOptions().getClassPath());
            Assert.assertEquals("xpoweredby option", true, jspc.getOptions().isXpoweredBy());
            Assert.assertEquals("trimSpaces option", true, jspc.getOptions().getTrimSpaces());
            Assert.assertEquals("javaEncoding option", StandardCharsets.ISO_8859_1.displayName(), jspc.getOptions().getJavaEncoding());
            Assert.assertEquals("target option", "1.7", jspc.getOptions().getCompilerSourceVM());
            Assert.assertEquals("target option", "1.7", jspc.getOptions().getCompilerTargetVM());
            Assert.assertEquals("threadCount option", 1, jspc.getThreadCount());
            Assert.assertEquals("deletesources option", true, jspc.getDeleteSources());
        } finally {
            deleteTemporaryDir(outputDir);
            deleteTemporaryFile(webincFile);
        }
    }

    @Test
    public void testThreadCountPerProcessor() throws Exception {
        JspC jspc = new JspCCommandLineBuilder()
                .set(JspCCommandLineBuilder.JspCArgument.THREAD_COUNT, "2C")
                .addFile("samples/simple.jsp")
                .build();
        Assert.assertEquals("threadCount option", Runtime.getRuntime().availableProcessors() * 2, jspc.getThreadCount());
    }
}
