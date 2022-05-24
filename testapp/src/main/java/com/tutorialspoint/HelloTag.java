package com.tutorialspoint;

import jakarta.servlet.jsp.tagext.*;
import jakarta.servlet.jsp.*;
import java.io.*;

public class HelloTag extends SimpleTagSupport {
   public void doTag() throws JspException, IOException {
      JspWriter out = getJspContext().getOut();
      out.println("Hello Custom Tag!");
   }
}
