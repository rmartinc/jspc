
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.hamcrest.CoreMatchers;
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

    // test precompilation is done and the context param is inserted

    @Test
    public void testPrecompile() throws IOException {
        Assert.assertThat(doGet("precompile.jsp"), CoreMatchers.containsString("org.wildfly.jastow.jspc.precompiled=true"));
    }

    // simple JSPs

    @Test
    public void testSimple() throws IOException {
        Assert.assertThat(doGet("simple.jsp"), CoreMatchers.containsString("Welcome to javaTpoint"));
    }

    @Test
    public void testAnotherSimple() throws IOException {
        Assert.assertThat(doGet("another-simple.jsp"), CoreMatchers.containsString("<title>First JSP</title>"));
    }

    @Test
    public void testJspInDir() throws IOException {
        Assert.assertThat(doGet("dir1/jsp-in-dir.jsp"), CoreMatchers.containsString("Current Time is"));
    }

    // includes

    @Test
    public void testInclude() throws IOException {
        Assert.assertThat(doGet("jsp-include-main.jsp"), CoreMatchers.containsString("This is the content of my file"));
    }

    @Test
    public void testIncludeJar() throws IOException {
        Assert.assertThat(doGet("jsp-include-main-jar.jsp"), CoreMatchers.containsString("This is the content of my file in a resources jar"));
    }

    // TLDs

    @Test
    public void testTldDirect() throws IOException {
        Assert.assertThat(doGet("tld-direct.jsp"), CoreMatchers.containsString("Current Date and Time is: "));
    }

    @Test
    public void testTldWebXml() throws IOException {
        Assert.assertThat(doGet("tld-in-web.jsp"), CoreMatchers.containsString("Current Date and Time is: "));
    }

    @Test
    public void testTldWebInf() throws IOException {
        Assert.assertThat(doGet("tld-in-web-inf.jsp"), CoreMatchers.containsString("Hello Custom Tag!"));
    }

    @Test
    public void testTldJar() throws IOException {
        Assert.assertThat(doGet("tld-in-jar-resources.jsp"), CoreMatchers.containsString("SUBSTR(GOODMORNING, 1, 6) is"));
    }

    // JSTL

    @Test
    public void testJstlSimple() throws IOException {
        Assert.assertThat(doGet("jstl-simple.jsp"), CoreMatchers.containsString("Welcome to javaTpoint"));
    }

    @Test
    public void testJstlBean() throws IOException {
        Assert.assertThat(doGet("jstl-bean.jsp"), CoreMatchers.containsString("Zara"));
    }

    @Test
    public void testJstlImport() throws IOException {
        Assert.assertThat(doGet("jstl-import.jsp"), CoreMatchers.containsString("Padam History"));
    }

    @Test
    public void testJstlSql() throws IOException {
        Assert.assertThat(doGet("jstl-sql.jsp"), CoreMatchers.containsString("<td>surname</td>"));
    }

    // EL

    @Test
    public void testElSimple() throws IOException {
        Assert.assertThat(doGet("el-simple.jsp"), CoreMatchers.containsString("b is TRUE"));
    }

    @Test
    public void testElConstants() throws IOException {
        Assert.assertThat(doGet("el-constants.jsp"), CoreMatchers.containsString("Integer.MAX_VALUE: 2147483647"));
    }

    // JSF

    @Test
    public void testJsfConvertor() throws IOException {
        Assert.assertThat(doGet("faces/Convertor.jsp"), CoreMatchers.containsString("Celsius"));
    }
}
