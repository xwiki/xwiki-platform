/*
 * Copyright 2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xpn.xwiki.test;

import junit.extensions.TestSetup;
import junit.framework.Test;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.taskdefs.ExecTask;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * JUnit TestSetup extension that starts/stops XWiki using a script passed using System Properties.
 * These properties are meant to be passed by the underlying build system. This class is meant
 * to wrap a JUnit TestSuite. For example:
 * <pre><code>
 * public static Test suite()
 * {
 *     // Create some TestSuite object here
 *     return new XWikiTestSetup(suite);
 * }
 * </code></pre>
 *
 * <p>Note: We could start XWiki using Java directly but we're using a script so that we can test
 * the exact same script used by XWiki users who download the standalone distribution.</p>
 *
 * @version $Id: $
 */
public class XWikiTestSetup extends TestSetup
{
    private static final String EXECUTION_DIRECTORY = System.getProperty("xwikiExecutionDirectory");
    private static final String START_COMMAND = System.getProperty("xwikiExecutionStartCommand");
    private static final String STOP_COMMAND = System.getProperty("xwikiExecutionStopCommand");
    private static final String PORT = System.getProperty("xwikiPort", "8080");
    private static final boolean DEBUG = System.getProperty("debug", "false").equalsIgnoreCase("true");
    private static final int TIMEOUT_SECONDS = 60;

    private Project project;

    public XWikiTestSetup(Test test)
    {
        super(test);

        this.project = new Project();
	    this.project.init();
        this.project.addBuildListener(new AntBuildListener(DEBUG));
    }

    protected void setUp() throws Exception
    {
        startXWikiInSeparateThread();
        waitForXWikiToLoad();
    }

    private void startXWikiInSeparateThread()
    {
        Thread startThread = new Thread(new Runnable() {
            public void run() {
                try {
                    startXWiki();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        startThread.start();
    }

    private void startXWiki() throws Exception
    {
        ExecTask execTask = (ExecTask) this.project.createTask("exec");
        execTask.setDir(new File(EXECUTION_DIRECTORY));
        Commandline commandLine = new Commandline(START_COMMAND);
        execTask.setCommand(commandLine);
        execTask.execute();
    }

    private Task createStopTask()
    {
        ExecTask execTask = (ExecTask) this.project.createTask("exec");
        execTask.setDir(new File(EXECUTION_DIRECTORY));
        Commandline commandLine = new Commandline(STOP_COMMAND);
        execTask.setCommand(commandLine);
        return execTask;
    }

    private void waitForXWikiToLoad() throws Exception
    {
        // Wait till the main page becomes available which means the server is started fine
        System.out.println("Checking that XWiki is up and running...");
        URL url = new URL("http://localhost:" + PORT + "/xwiki/bin/view/Main/");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        boolean connected = false;
        boolean timedOut = false;
        long startTime = System.currentTimeMillis();
        while (!connected && !timedOut) {
            try {
                connection.connect();
                int responseCode = connection.getResponseCode();
                if (DEBUG) {
                    System.out.println("Result of pinging [" + url + "] = ["
                        + responseCode + "], Message = ["
                        + connection.getResponseMessage() + "]");
                }
                connected = (responseCode == 200);
            } catch (IOException e) {
                // Do nothing as it simply means the server is not ready yet...
            }
            Thread.sleep(100L);
            timedOut = (System.currentTimeMillis() - startTime > TIMEOUT_SECONDS * 1000L);
        }
        if (timedOut) {
            String message = "Failed to start XWiki in [" + TIMEOUT_SECONDS + "] seconds";
            System.out.println(message);
            tearDown();
            throw new RuntimeException(message);
        }
    }

    protected void tearDown() throws Exception
    {
        // Stop XWiki
        createStopTask().execute();
    }
}
