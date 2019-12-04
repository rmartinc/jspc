/*
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
    public void testExternalLibrary() throws Exception {
        Assert.assertTrue("Library moved", new File("samples/WEB-INF/lib/inner-lib.jar")
                .renameTo(new File("samples/WEB-INF/lib/inner-lib.jar.NO")));
        try {
            // it should fail because TLD and classes are not found
            JspCResults results = new JspC()
                    .setDebugLevel(Level.OFF)
                    .setOutputDir(tempDir)
                    .setWebxmlLevel(JspC.WEBXML_LEVEL.INC_WEBXML)
                    .setWebxmlFile(tempDir + "/web-inc.xml")
                    .addPage("samples/tld-in-jar-resources.jsp")
                    .execute();
            Assert.assertTrue("Error result", results.isError());
            Assert.assertEquals("Errors = 1", 1, results.errors());
            Assert.assertFalse("web-inc.xml doesn't exist", Files.exists(Paths.get(tempDir + "/web-inc.xml")));
            // append the classpath (twice to check the separator)
            results = new JspC()
                    .setDebugLevel(Level.OFF)
                    .setOutputDir(tempDir)
                    .setWebxmlLevel(JspC.WEBXML_LEVEL.INC_WEBXML)
                    .setWebxmlFile(tempDir + "/web-inc.xml")
                    .setClassPath("samples/WEB-INF/lib/inner-lib.jar.NO" + File.pathSeparator + "samples/WEB-INF/lib/inner-lib.jar.NO")
                    .addPage("samples/tld-in-jar-resources.jsp")
                    .execute();
            Assert.assertFalse("No error", results.isError());
            Assert.assertEquals("Errors = 0", 0, results.errors());
            Assert.assertEquals("Results = 1", 1, results.results());
            Assert.assertEquals("Total = 1", 1, results.total());
            Assert.assertTrue("web-inc.xml file exists", Files.exists(Paths.get(tempDir + "/web-inc.xml")));
            Assert.assertTrue("web-inc.xml is not empty", Files.size(Paths.get(tempDir + "/web-inc.xml")) > 0);
        } finally {
            new File("samples/WEB-INF/lib/inner-lib.jar.NO")
                    .renameTo(new File("samples/WEB-INF/lib/inner-lib.jar"));
        }
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
        Assert.assertTrue("web-fragment.xml file exists", Files.exists(Paths.get(tempDir + "/web-fragment.xml")));
        Assert.assertTrue("web-fragment.xml is not empty", Files.size(Paths.get(tempDir + "/web-fragment.xml")) > 0);
    }

}
