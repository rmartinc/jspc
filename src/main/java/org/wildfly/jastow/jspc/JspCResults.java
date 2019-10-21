/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.wildfly.jastow.jspc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author rmartinc
 */
public class JspCResults {

    public static class ResultEntry {

        private final String jspUri;
        private final String servletName;
        private final Throwable error;

        public ResultEntry(String jspUri, String servletName) {
            this.jspUri = jspUri;
            this.servletName = servletName;
            this.error = null;
        }

        public ResultEntry(String jspUri, Throwable error) {
            this.jspUri = jspUri;
            this.error = error;
            this.servletName = null;
        }

        public String getJspUri() {
            return jspUri;
        }

        public String getServletName() {
            return servletName;
        }

        public Throwable getError() {
            return error;
        }

        public boolean isError() {
            return error != null;
        }
    }

    private final List<ResultEntry> results;
    private final List<ResultEntry> errors;
    private final int errorCode;
    private final long startTime;

    public JspCResults() {
        this(1);
    }

    public JspCResults(int errorCode) {
        this.results = Collections.synchronizedList(new ArrayList<>());
        this.errors = Collections.synchronizedList(new ArrayList<>());
        this.errorCode = errorCode;
        this.startTime = System.currentTimeMillis();
    }

    public void addSuccess(String jspUri, String servletName) {
        this.results.add(new ResultEntry(jspUri, servletName));
    }

    public void addError(String jspUri, Throwable e) {
        this.errors.add(new ResultEntry(jspUri, e));
    }

    public List<ResultEntry> getResults() {
        return Collections.unmodifiableList(this.results);
    }

    public List<ResultEntry> getErrors() {
        return Collections.unmodifiableList(this.errors);
    }

    public int results() {
        return this.results.size();
    }

    public int errors() {
        return this.errors.size();
    }

    public int total() {
        return this.results() + this.errors();
    }
    
    public boolean isError() {
        return this.errors() > 0;
    }

    public int getErrorCode() {
        return this.isError()? errorCode : 0;
    }

    public long getTime() {
        return System.currentTimeMillis() - startTime;
    }
}
