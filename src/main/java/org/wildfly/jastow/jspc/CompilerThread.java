/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.wildfly.jastow.jspc;

import org.apache.log4j.Logger;

/**
 *
 * @author rmartinc
 */
class CompilerThread extends Thread {

    private static final Logger log = Logger.getLogger(JspC.class);

    private final JspC jspc;

    public CompilerThread(JspC jspc) {
        this.jspc = jspc;
    }

    @Override
    public void run() {
        String jsp = jspc.nextJsp();
        while (jsp != null) {
            log.trace("Starting compilation for: " + jsp);
            jspc.compileJsp(jsp);
            jsp = jspc.nextJsp();
        }
        log.trace("Thread finishing...");
    }

}
