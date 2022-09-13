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
package org.wildfly.jastow.jspc.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import org.apache.logging.log4j.Level;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.wildfly.jastow.jspc.JspC;
import org.wildfly.jastow.jspc.JspCResults;

/**
 * <p>Simple test for the JspcMojo that uses the testapp JSPs to perform the
 * compilation. As the testapp library is not included there are two files that
 * cannot be compiled. Those two JSPs were used to trigger errors.</p>
 *
 * @author rmartinc
 */
public class JspCMojoTest extends AbstractMojoTestCase {

    private static final File pomFile = new File(getBasedir(), "target/test-classes/plugin-config.xml");
    private String tempDir;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        tempDir = Files.createTempDirectory("output").toFile().getCanonicalPath();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
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

    private JspcMojo createDefaultMojo() throws Exception {
        JspcMojo mojo = (JspcMojo) lookupMojo("jspc", pomFile);
        setVariableValueToObject(mojo, "generatedClasses", tempDir);
        // classes needed for the compilation are got from the tool folder
        setVariableValueToObject(mojo, "classesDirectory", new File(getBasedir(), "../tool/samples/WEB-INF/classes"));
        // the JSP files to compile from the testapp folder
        setVariableValueToObject(mojo, "webAppSourceDirectory", getBasedir() + "/../testapp/src/main/webapp");
        setVariableValueToObject(mojo, "pluginArtifacts", new ArrayList<Artifact>());
        setVariableValueToObject(mojo, "webXmlType", "MERGE_WEBXML");
        setVariableValueToObject(mojo, "webXml", tempDir + "/web.xml");
        setVariableValueToObject(mojo, "debugLevel", "OFF");
        setVariableValueToObject(mojo, "includes", "**\\/*.jsp");
        setVariableValueToObject(mojo, "excludes", "tld-in-jar-resources.jsp,jsp-include-main-jar.jsp");
        setVariableValueToObject(mojo, "failOnError", true);
        setVariableValueToObject(mojo, "failFast", true);
        setVariableValueToObject(mojo, "threadCount", 2);
        setVariableValueToObject(mojo, "xpoweredBy", true);
        setVariableValueToObject(mojo, "trimSpaces", true);
        setVariableValueToObject(mojo, "javaEncoding", "UTF-8");
        setVariableValueToObject(mojo, "webxmlEncoding", "UTF-8");
        setVariableValueToObject(mojo, "targetPackage", "test.mojo.jsps");
        setVariableValueToObject(mojo, "sourceVersion", "11");
        setVariableValueToObject(mojo, "targetVersion", "11");
        setVariableValueToObject(mojo, "keepSources", false);

        MavenProject project = Mockito.mock(MavenProject.class);
        setVariableValueToObject(mojo, "project", project);

        return mojo;
    }

    @Test
    public void testSuccess() throws Exception {
        JspcMojo mojo = createDefaultMojo();

        mojo.execute();

        // assert initialization of jspc contains the maven parameters
        Assert.assertEquals("generatedClasses is OK", Paths.get(tempDir).toFile().getCanonicalPath(), mojo.getJspC().getOptions().getScratchDir().getCanonicalPath());
        Assert.assertEquals("webAppSourceDirectory is OK", Paths.get(getBasedir(), "../testapp/src/main/webapp").toFile().getCanonicalPath(), Paths.get(mojo.getJspC().getUriRoot()).toFile().getCanonicalPath());
        Assert.assertEquals("webXmlType is OK", JspC.WEBXML_LEVEL.MERGE_WEBXML, mojo.getJspC().getWebxmlLevel());
        Assert.assertEquals("debugLevel is OK", Level.OFF, mojo.getJspC().getDebugLevel());
        Assert.assertEquals("webXml", Paths.get(tempDir + "/web.xml").toFile().getCanonicalPath(), Paths.get(mojo.getJspC().getWebxmlFile()).toFile().getCanonicalPath());
        Assert.assertEquals("failOnError is OK", true, mojo.getJspC().isFailOnError());
        Assert.assertEquals("failFast is OK", true, mojo.getJspC().isFailFast());
        Assert.assertEquals("threadCount is OK", 2, mojo.getJspC().getThreadCount());
        Assert.assertEquals("xpoweredBy is OK", true, mojo.getJspC().getOptions().isXpoweredBy());
        Assert.assertEquals("trimSpaces is OK", true, mojo.getJspC().getOptions().getTrimSpaces());
        Assert.assertEquals("javaEncoding is OK", "UTF-8", mojo.getJspC().getOptions().getJavaEncoding());
        Assert.assertEquals("webxmlEncoding is OK", StandardCharsets.UTF_8, mojo.getJspC().getWebxmlEncoding());
        Assert.assertEquals("targetPackage is OK", "test.mojo.jsps", mojo.getJspC().getTargetPackage());
        Assert.assertEquals("sourceVersion is OK", "11", mojo.getJspC().getOptions().getCompilerSourceVM());
        Assert.assertEquals("targetVersion is OK", "11", mojo.getJspC().getOptions().getCompilerTargetVM());
        Assert.assertEquals("keepSources is OK", true, mojo.getJspC().getDeleteSources());

        // assert results
        Assert.assertFalse("Error executing", mojo.getResults().isError());
        Assert.assertEquals("Executed over 15 files", 15, mojo.getResults().results());
        MatcherAssert.assertThat(mojo.getResults().getResults().get(0).getServletName(), CoreMatchers.startsWith("test.mojo.jsps."));
        for (JspCResults.ResultEntry r : mojo.getResults().getResults()) {
            String pathName = r.getServletName().replace(".", File.separator);
            Assert.assertTrue("Class file exists", Files.exists(Paths.get(tempDir).resolve(pathName + ".class")));
            Assert.assertTrue("Java file does not exist", !Files.exists(Paths.get(tempDir).resolve(pathName + ".java")));
        }
        Assert.assertTrue("web.xml file exists", Files.exists(Paths.get(tempDir, "web.xml")));
        Assert.assertTrue("web.xml is not empty", Files.size(Paths.get(tempDir, "web.xml")) > 0);
    }

    @Test
    public void testError() throws Exception {
        JspcMojo mojo = createDefaultMojo();
        setVariableValueToObject(mojo, "excludes", null);
        setVariableValueToObject(mojo, "failFast", false);

        try {
            mojo.execute();
            Assert.fail("Execution should have thrown an exception");
        } catch (MojoExecutionException e) {
            // expected
        }

        Assert.assertTrue("Error executing", mojo.getResults().isError());
        Assert.assertEquals("2 Errors", 2, mojo.getResults().errors());
        Assert.assertEquals("Executed over 17 files", 17, mojo.getResults().total());
        Assert.assertTrue("web.xml file does not exist", !Files.exists(Paths.get(tempDir, "web.xml")));
    }

    @Test
    public void testNoFailOnError() throws Exception {
        JspcMojo mojo = createDefaultMojo();
        setVariableValueToObject(mojo, "excludes", null);
        setVariableValueToObject(mojo, "failOnError", false);

        mojo.execute();

        Assert.assertTrue("Error executing", mojo.getResults().isError());
        Assert.assertEquals("2 Errors", 2, mojo.getResults().errors());
        Assert.assertEquals("Executed over 17 files", 17, mojo.getResults().total());
        for (JspCResults.ResultEntry r : mojo.getResults().getResults()) {
            String pathName = r.getServletName().replace(".", File.separator);
            Assert.assertTrue("Class file exists", Files.exists(Paths.get(tempDir).resolve(pathName + ".class")));
            Assert.assertTrue("Java file does not exist", !Files.exists(Paths.get(tempDir).resolve(pathName + ".java")));
        }
        Assert.assertTrue("web.xml file exists", Files.exists(Paths.get(tempDir, "web.xml")));
        Assert.assertTrue("web.xml is not empty", Files.size(Paths.get(tempDir, "web.xml")) > 0);
    }
}
