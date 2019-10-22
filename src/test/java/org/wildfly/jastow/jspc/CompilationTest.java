/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.wildfly.jastow.jspc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import org.apache.log4j.Level;
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
        Assert.assertTrue("Class file exists", new File(tempDir + "/" + pathName + ".class").exists());
        Assert.assertTrue("Java file exists", new File(tempDir + "/" + pathName + ".java").exists());
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
                .setThreadCount(2)
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
                .setThreadCount(2)
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
                .setThreadCount(2)
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
        Assert.assertEquals("No error", 17, results.total());
        Assert.assertEquals("No error", 17, results.results());
        Assert.assertTrue("web-fragment.xml files exists", Files.exists(Paths.get(tempDir + "/web-fragment.xml")));
        Assert.assertTrue("web-fragment.xml is not empty", Files.size(Paths.get(tempDir + "/web-fragment.xml")) > 0);
    }

}
