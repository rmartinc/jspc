package beginnersbook.com;
import javax.servlet.jsp.tagext.*;
import javax.servlet.jsp.*;
import java.io.*;
public class Details extends SimpleTagSupport {
   public void doTag() throws JspException, IOException {
      /*This is just to display a message, when
       * we will use our custom tag. This message
       * would be displayed
       */
      JspWriter out = getJspContext().getOut();
      out.println("This is my own custom tag");
   }
}
