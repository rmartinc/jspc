package com.journaldev.jsp.customtags;

import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.SkipPageException;
import jakarta.servlet.jsp.tagext.SimpleTagSupport;
import java.io.IOException;
import java.text.DecimalFormat;

public class NumberFormatterTag extends SimpleTagSupport {

	private String format;
	private String number;

	public NumberFormatterTag() {
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	@Override
	public void doTag() throws JspException, IOException {
		System.out.println("Number is:" + number);
		System.out.println("Format is:" + format);
		try {
			double amount = Double.parseDouble(number);
			DecimalFormat formatter = new DecimalFormat(format);
			String formattedNumber = formatter.format(amount);
			getJspContext().getOut().write(formattedNumber);
		} catch (Exception e) {
			e.printStackTrace();
			// stop page from loading further by throwing SkipPageException
			throw new SkipPageException("Exception in formatting " + number
					+ " with format " + format);
		}
	}

}
