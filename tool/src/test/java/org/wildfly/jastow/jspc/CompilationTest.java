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
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.apache.logging.log4j.Level;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author rmartinc
 */
public class CompilationTest {

    private String tempDir;

    @Before
    public void setUpClass() throws IOException {
        tempDir = Files.createTempDirectory("output").toFile().getCanonicalFile().toString();
    }

    @After
    public void tearDownClass() throws IOException {
        deleteTemporaryDir(tempDir);
    }

    private static void deleteTemporaryDir(String path) throws IOException {
        Path pathToDelete = Paths.get(path);
        if (Files.exists(pathToDelete) && Files.isDirectory(pathToDelete)) {
            Files.walk(pathToDelete)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    @Test
    public void testCompilationSimple() throws Exception {
        JspCResults results = new JspC()
                .setDebugLevel(Level.OFF)
                .setTargetClassName("SimpleJSP")
                .setTargetPackage("com.sample.precompiled")
                .setOutputDir(tempDir)
                .setWebxmlLevel(JspC.WEBXML_LEVEL.INC_WEBXML)
                .setWebxmlFile(tempDir + "/web-inc.xml")
                .setThreadCount(1)
                .addPage("samples/simple.jsp")
                .execute();
        Assert.assertFalse("Error result", results.isError());
        Assert.assertEquals("No error", 0, results.errors());
        Assert.assertEquals("No error", 1, results.total());
        Assert.assertEquals("No error", 1, results.results());
        Assert.assertEquals("Correct URI", "/simple.jsp", results.getResults().get(0).getJspUri());
        Assert.assertEquals("Correct URI", "com.sample.precompiled.SimpleJSP", results.getResults().get(0).getServletName());
        Assert.assertNull("No error", results.getResults().get(0).getError());
        String pathName = results.getResults().get(0).getServletName().replace(".", File.separator);
        Assert.assertTrue("Class file exists", Files.exists(Paths.get(tempDir).resolve(pathName + ".class")));
        Assert.assertTrue("Java file exists", Files.exists(Paths.get(tempDir).resolve(pathName + ".java")));
        Assert.assertTrue("web-inc.xml file exists", Files.exists(Paths.get(tempDir + "/web-inc.xml")));
        Assert.assertTrue("web-inc.xml is not empty", Files.size(Paths.get(tempDir + "/web-inc.xml")) > 0);
    }

    @Test
    public void testNormalFailOptions() throws Exception {
        JspCResults results = new JspC()
                .setDebugLevel(Level.OFF)
                .setOutputDir(tempDir)
                .setWebxmlLevel(JspC.WEBXML_LEVEL.FRG_WEBXML)
                .setWebxmlFile(tempDir + "/web-fragment.xml")
                .setThreadCount(1)
                .addPage("samples/error.jsp.err")
                .addPage("samples/jstl-bean.jsp")
                .addPage("samples/simple.jsp")
                .addPage("samples/another-simple.jsp")
                .addPage("samples/jstl-simple.jsp")
                .execute();
        Assert.assertTrue("Error result", results.isError());
        Assert.assertEquals("error = 1", 1, results.errors());
        Assert.assertEquals("results = 4", 4, results.results());
        Assert.assertEquals("total = 5", 5, results.total());
        Assert.assertTrue("web-inc.xml file doesn't exist", Files.notExists(Paths.get(tempDir + "/web-fragment.xml")));
    }

    @Test
    public void testFailFastOption() throws Exception {
        JspCResults results = new JspC()
                .setDebugLevel(Level.OFF)
                .setOutputDir(tempDir)
                .setWebxmlLevel(JspC.WEBXML_LEVEL.FRG_WEBXML)
                .setWebxmlFile(tempDir + "/web-fragment.xml")
                .setFailFast(true)
                .setThreadCount(1)
                .addPage("samples/error.jsp.err")
                .addPage("samples/jstl-bean.jsp")
                .addPage("samples/simple.jsp")
                .addPage("samples/another-simple.jsp")
                .addPage("samples/jstl-simple.jsp")
                .execute();
        Assert.assertTrue("Error result", results.isError());
        Assert.assertEquals("error = 1", 1, results.errors());
        Assert.assertTrue("results < 4", results.results() < 4);
        Assert.assertTrue("total < 5", results.total() < 5);
        Assert.assertTrue("web-fragment.xml file doesn't exist", Files.notExists(Paths.get(tempDir + "/web-fragment.xml")));
    }

    @Test
    public void testNoFailOnErrorOption() throws Exception {
        JspCResults results = new JspC()
                .setDebugLevel(Level.OFF)
                .setOutputDir(tempDir)
                .setWebxmlLevel(JspC.WEBXML_LEVEL.ALL_WEBXML)
                .setWebxmlFile(tempDir + "/web.xml")
                .setFailOnError(false)
                .setThreadCount(1)
                .addPage("samples/error.jsp.err")
                .addPage("samples/jstl-bean.jsp")
                .addPage("samples/simple.jsp")
                .addPage("samples/another-simple.jsp")
                .addPage("samples/jstl-simple.jsp")
                .execute();
        Assert.assertTrue("Error result", results.isError());
        Assert.assertEquals("error = 1", 1, results.errors());
        Assert.assertEquals("results = 4", 4, results.results());
        Assert.assertEquals("total = 5", 5, results.total());
        Assert.assertTrue("web.xml file exists", Files.exists(Paths.get(tempDir + "/web.xml")));
        Assert.assertTrue("web.xml is not empty", Files.size(Paths.get(tempDir + "/web.xml")) > 0);
    }

    @Test
    public void testExternalLibrary() throws Exception {
        // it should fail because TLD and classes are not found
        JspCResults results = new JspC()
                .setDebugLevel(Level.OFF)
                .setOutputDir(tempDir)
                .setWebxmlLevel(JspC.WEBXML_LEVEL.INC_WEBXML)
                .setWebxmlFile(tempDir + "/web-inc.xml")
                .addPage("samples/beginnersbook-details-extlib.jsp.no")
                .execute();
        Assert.assertTrue("Error result", results.isError());
        Assert.assertEquals("Errors = 1", 1, results.errors());
        Assert.assertFalse("web-inc.xml doesn't exist", Files.exists(Paths.get(tempDir + "/web-inc.xml")));
        // append the classpath (twice to check the separator)
        String extLib = Paths.get("samples/beginnersbook-details-extlib.jar").toAbsolutePath().toString();
        results = new JspC()
                .setDebugLevel(Level.OFF)
                .setOutputDir(tempDir)
                .setWebxmlLevel(JspC.WEBXML_LEVEL.INC_WEBXML)
                .setWebxmlFile(tempDir + "/web-inc.xml")
                .setClassPath(extLib + File.pathSeparator + extLib)
                .addPage("samples/beginnersbook-details-extlib.jsp.no")
                .execute();
        Assert.assertFalse("No error", results.isError());
        Assert.assertEquals("Errors = 0", 0, results.errors());
        Assert.assertEquals("Results = 1", 1, results.results());
        Assert.assertEquals("Total = 1", 1, results.total());
        Assert.assertTrue("web-inc.xml file exists", Files.exists(Paths.get(tempDir + "/web-inc.xml")));
        Assert.assertTrue("web-inc.xml is not empty", Files.size(Paths.get(tempDir + "/web-inc.xml")) > 0);
    }

    @Test
    public void testDeleteJava() throws Exception {
        JspCResults results = new JspC()
                .setDebugLevel(Level.OFF)
                .setTargetPackage("com.sample.precompiled")
                .setOutputDir(tempDir)
                .setWebxmlLevel(JspC.WEBXML_LEVEL.FRG_WEBXML)
                .setWebxmlFile(tempDir + "/web-fragment.xml")
                .setThreadCount(1)
                .setDeleteSources(true)
                .addPage("samples/simple.jsp")
                .addPage("samples/another-simple.jsp")
                .execute();
        Assert.assertFalse("Error result", results.isError());
        Assert.assertEquals("No error", 0, results.errors());
        Assert.assertEquals("No error", 2, results.total());
        Assert.assertEquals("No error", 2, results.results());
        for (JspCResults.ResultEntry result : results.getResults()) {
            String pathName = result.getServletName().replace(".", File.separator);
            Assert.assertTrue("Class file exists", Files.exists(Paths.get(tempDir).resolve(pathName + ".class")));
            Assert.assertTrue("Java file does not exist", !Files.exists(Paths.get(tempDir).resolve(pathName + ".java")));
        }
        Assert.assertTrue("web-fragment.xml file exists", Files.exists(Paths.get(tempDir + "/web-fragment.xml")));
        Assert.assertTrue("web-fragment.xml is not empty", Files.size(Paths.get(tempDir + "/web-fragment.xml")) > 0);
    }

    @Test
    public void testCompilationMergeXml() throws Exception {
        JspCResults results = new JspC()
                .setDebugLevel(Level.OFF)
                .setOutputDir(tempDir)
                .setUriRoot("samples")
                .setWebxmlLevel(JspC.WEBXML_LEVEL.MERGE_WEBXML)
                .setWebxmlFile(tempDir + "/web.xml")
                .setWebxmlEncoding(StandardCharsets.UTF_8)
                .execute();
        Assert.assertFalse("Error result", results.isError());
        Assert.assertEquals("No error", 0, results.errors());
        Assert.assertEquals("No error", 16, results.total());
        Assert.assertEquals("No error", 16, results.results());
        Assert.assertTrue("web.xml file exists", Files.exists(Paths.get(tempDir + "/web.xml")));
        Assert.assertTrue("web.xml is not empty", Files.size(Paths.get(tempDir + "/web.xml")) > 0);
        MatcherAssert.assertThat(new String(Files.readAllBytes(Paths.get(tempDir + "/web.xml")), StandardCharsets.UTF_8),
                CoreMatchers.containsString("http://tomcat.apache.org/example-taglib"));
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFactory.newSchema(new File(getClass().getClassLoader().getResource("web-app_5_0.xsd").getFile()));
        Validator validator = schema.newValidator();
        validator.validate(new StreamSource(Paths.get(tempDir + "/web.xml").toFile()));
    }

    @Test
    public void testCompilationAll() throws Exception {
        JspCResults results = new JspC()
                .setDebugLevel(Level.OFF)
                .setOutputDir(tempDir)
                .setUriRoot("samples")
                .setWebxmlLevel(JspC.WEBXML_LEVEL.FRG_WEBXML)
                .setWebxmlFile(tempDir + "/web-fragment.xml")
                .execute();
        Assert.assertFalse("Error result", results.isError());
        Assert.assertEquals("No error", 0, results.errors());
        Assert.assertEquals("No error", 16, results.total());
        Assert.assertEquals("No error", 16, results.results());
        Assert.assertTrue("web-fragment.xml file exists", Files.exists(Paths.get(tempDir + "/web-fragment.xml")));
        Assert.assertTrue("web-fragment.xml is not empty", Files.size(Paths.get(tempDir + "/web-fragment.xml")) > 0);
    }
}
