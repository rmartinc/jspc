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
package org.wildfly.jastow.jspc;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author rmartinc
 */
class CompilerThread extends Thread {

    private final Logger log = LogManager.getLogger(JspC.class.getPackageName());

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
