
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * <p>IT test that deploys the generated <em>testapp.war</em> with the
 * pre-compiled JSPs inside the bundle and tests each of them.</p>
 *
 * @author rmartinc
 */
@RunWith(Arquillian.class)
@RunAsClient
public class AppITCase {

    @ArquillianResource
    private URL appUrl;

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(ZipImporter.class, "testapp.war")
                .importFrom(new File(System.getProperty("warFile"))).as(WebArchive.class);
    }

    private String doGet(String jsp) throws IOException {
        URL url = new URL(appUrl.toString() + jsp);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        Assert.assertEquals(HttpURLConnection.HTTP_OK, conn.getResponseCode());
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder content = new StringBuilder();
            String inputLine = in.readLine();
            while (inputLine != null) {
                content.append(inputLine);
                inputLine = in.readLine();
            }
            return content.toString();
        }
    }

    // test precompilation is done and the servlet name contains the specific package

    @Test
    public void testPrecompile() throws IOException {
        MatcherAssert.assertThat(doGet("precompile.jsp"), CoreMatchers.containsString("servletName=test.app.servlet"));
    }

    // simple JSPs

    @Test
    public void testSimple() throws IOException {
        MatcherAssert.assertThat(doGet("simple.jsp"), CoreMatchers.containsString("Welcome to javaTpoint"));
    }

    @Test
    public void testAnotherSimple() throws IOException {
        MatcherAssert.assertThat(doGet("another-simple.jsp"), CoreMatchers.containsString("<title>First JSP</title>"));
    }

    @Test
    public void testJspInDir() throws IOException {
        MatcherAssert.assertThat(doGet("dir1/jsp-in-dir.jsp"), CoreMatchers.containsString("Current Time is"));
    }

    // includes

    @Test
    public void testInclude() throws IOException {
        MatcherAssert.assertThat(doGet("jsp-include-main.jsp"), CoreMatchers.containsString("This is the content of my file"));
    }

    @Test
    public void testIncludeJar() throws IOException {
        MatcherAssert.assertThat(doGet("jsp-include-main-jar.jsp"), CoreMatchers.containsString("This is the content of my file in a resources jar"));
    }

    // TLDs

    @Test
    public void testTldDirect() throws IOException {
        MatcherAssert.assertThat(doGet("tld-direct.jsp"), CoreMatchers.containsString("Current Date and Time is: "));
    }

    @Test
    public void testTldWebXml() throws IOException {
        MatcherAssert.assertThat(doGet("tld-in-web.jsp"), CoreMatchers.containsString("Current Date and Time is: "));
    }

    @Test
    public void testTldWebInf() throws IOException {
        MatcherAssert.assertThat(doGet("tld-in-web-inf.jsp"), CoreMatchers.containsString("Hello Custom Tag!"));
    }

    @Test
    public void testTldJar() throws IOException {
        MatcherAssert.assertThat(doGet("tld-in-jar-resources.jsp"), CoreMatchers.containsString("SUBSTR(GOODMORNING, 1, 6) is"));
    }

    // JSTL

    @Test
    public void testJstlSimple() throws IOException {
        MatcherAssert.assertThat(doGet("jstl-simple.jsp"), CoreMatchers.containsString("Welcome to javaTpoint"));
    }

    @Test
    public void testJstlBean() throws IOException {
        MatcherAssert.assertThat(doGet("jstl-bean.jsp"), CoreMatchers.containsString("Zara"));
    }

    @Test
    public void testJstlImport() throws IOException {
        MatcherAssert.assertThat(doGet("jstl-import.jsp"), CoreMatchers.containsString("Padam History"));
    }

    @Test
    public void testJstlSql() throws IOException {
        MatcherAssert.assertThat(doGet("jstl-sql.jsp"), CoreMatchers.containsString("<td>surname</td>"));
    }

    // EL

    @Test
    public void testElSimple() throws IOException {
        MatcherAssert.assertThat(doGet("el-simple.jsp"), CoreMatchers.containsString("b is TRUE"));
    }

    @Test
    public void testElConstants() throws IOException {
        MatcherAssert.assertThat(doGet("el-constants.jsp"), CoreMatchers.containsString("Integer.MAX_VALUE: 2147483647"));
    }

    // web-fragment from the other sub project

    @Test
    public void testWebFragment() throws IOException {
        MatcherAssert.assertThat(doGet("/web-fragment.jsp"), CoreMatchers.containsString("Web Fragment Example"));
    }
}
