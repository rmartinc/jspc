package com.javatpoint.sonoo;  
import java.util.Calendar;  
import jakarta.servlet.jsp.JspException;  
import jakarta.servlet.jsp.JspWriter;  
import jakarta.servlet.jsp.tagext.TagSupport;  
public class MyTagHandler extends TagSupport{  
  
public int doStartTag() throws JspException {  
    JspWriter out=pageContext.getOut();//returns the instance of JspWriter  
    try{  
     out.print(Calendar.getInstance().getTime());//printing date and time using JspWriter  
    }catch(Exception e){System.out.println(e);}  
    return SKIP_BODY;//will not evaluate the body content of the tag  
}  
}
