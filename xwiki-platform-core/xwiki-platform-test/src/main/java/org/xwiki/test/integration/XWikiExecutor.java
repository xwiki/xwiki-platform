/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
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
package org.xwiki.test.integration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.lang3.SystemUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Start and stop a xwiki instance.
 * 
 * @version $Id$
 * @since 2.0RC1
 */
public class XWikiExecutor
{
    protected static final Logger LOGGER = LoggerFactory.getLogger(XWikiExecutor.class);

    public static final String BASEDIR = System.getProperty("basedir");

    public static final String DEFAULT_PORT = System.getProperty("xwikiPort", "8080");

    public static final String DEFAULT_STOPPORT = System.getProperty("xwikiStopPort", "8079");

    public static final String DEFAULT_RMIPORT = System.getProperty("rmiPort", "9010");

    private static final String DEFAULT_EXECUTION_DIRECTORY = System.getProperty("xwikiExecutionDirectory");

    private static final String START_COMMAND = System.getProperty("xwikiExecutionStartCommand");

    private static final String STOP_COMMAND = System.getProperty("xwikiExecutionStopCommand");

    private static final boolean DEBUG = System.getProperty("debug", "false").equalsIgnoreCase("true");

    private static final String WEBINF_PATH = "/webapps/xwiki/WEB-INF";

    private static final String XWIKICFG_PATH = WEBINF_PATH + "/xwiki.cfg";

    private static final String XWIKIPROPERTIES_PATH = WEBINF_PATH + "/xwiki.properties";

    private static final int TIMEOUT_SECONDS = 120;

    private Project project;

    private int port;

    private int stopPort;

    private int rmiPort;

    private String executionDirectory;

    private List<Environment.Variable> env = new ArrayList<Environment.Variable>();

    private String opts;

    /**
     * Was XWiki server already started. We don't try to stop it if it was already started.
     */
    private boolean wasStarted;

    private class Response
    {
        public boolean timedOut;

        public byte[] responseBody;

        public int responseCode;
    }

    public XWikiExecutor(int index)
    {
        this.project = new Project();
        this.project.init();
        this.project.addBuildListener(new AntBuildListener(DEBUG));

        // resolve ports
        String portString = System.getProperty("xwikiPort" + index);
        this.port = portString != null ? Integer.valueOf(portString) : (Integer.valueOf(DEFAULT_PORT) + index);
        String stopPortString = System.getProperty("xwikiStopPort" + index);
        this.stopPort =
            stopPortString != null ? Integer.valueOf(stopPortString) : (Integer.valueOf(DEFAULT_STOPPORT) - index);
        String rmiPortString = System.getProperty("rmiPort" + index);
        this.rmiPort =
            rmiPortString != null ? Integer.valueOf(rmiPortString) : (Integer.valueOf(DEFAULT_RMIPORT) + index);

        // resolve execution directory
        this.executionDirectory = System.getProperty("xwikiExecutionDirectory" + index);
        if (this.executionDirectory == null) {
            this.executionDirectory = DEFAULT_EXECUTION_DIRECTORY;
            if (this.executionDirectory == null) {
                this.executionDirectory = BASEDIR + "/target/xwiki";
            }
            if (index > 0) {
                this.executionDirectory += "-" + index;
            }
        }
    }

    public int getPort()
    {
        return this.port;
    }

    public int getStopPort()
    {
        return this.stopPort;
    }

    public int getRMIPort()
    {
        return this.rmiPort;
    }

    public String getExecutionDirectory()
    {
        if (this.executionDirectory == null) {
            throw new RuntimeException("Invalid configuration for the execution directory. The "
                + "[xwikiExecutionDirectory] system property must be specified.");
        }
        return this.executionDirectory;
    }

    public void addEnvironmentVariable(String key, String value)
    {
        Environment.Variable variable = new Environment.Variable();

        variable.setKey(key);
        variable.setValue(value);

        this.env.add(variable);
    }

    public void setOpts(String opts)
    {
        this.opts = opts;
    }

    public void start() throws Exception
    {
        System.out.println("Starting XWiki server start");

        // First, verify if XWiki is started. If it is then don't start it again.
        this.wasStarted = !isXWikiStarted(getURL(), 15).timedOut;
        if (!this.wasStarted) {
            startXWikiInSeparateThread();
            waitForXWikiToLoad();
        } else {
            System.out.println("XWiki server is already started!");
        }
    }

    private void startXWikiInSeparateThread()
    {
        Thread startThread = new Thread(new Runnable()
        {
            public void run()
            {
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
        File dir = new File(getExecutionDirectory());
        if (dir.exists()) {
            ExecTask execTask = (ExecTask) this.project.createTask("exec");
            execTask.setDir(new File(getExecutionDirectory()));
            for (Environment.Variable variable : this.env) {
                execTask.addEnv(variable);
            }

            if (this.opts != null) {
                Environment.Variable optsVariable = new Environment.Variable();
                optsVariable.setKey("XWIKI_OPTS");
                optsVariable.setValue(this.opts);
                execTask.addEnv(optsVariable);
            }

            String startCommand = getDefaultStartCommand(getPort(), getStopPort(), getRMIPort());
            Commandline commandLine = new Commandline(startCommand);
            execTask.setCommand(commandLine);

            execTask.execute();
        } else {
            throw new Exception("Invalid directory from where to start XWiki [" + this.executionDirectory + "]");
        }
    }

    private Task createStopTask() throws Exception
    {
        ExecTask execTask;
        File dir = new File(getExecutionDirectory());
        if (dir.exists()) {
            execTask = (ExecTask) this.project.createTask("exec");
            execTask.setDir(new File(getExecutionDirectory()));

            String stopCommand = getDefaultStopCommand(getStopPort());
            Commandline commandLine = new Commandline(stopCommand);
            execTask.setCommand(commandLine);
        } else {
            throw new Exception("Invalid directory from where to stop XWiki [" + this.executionDirectory + "]");
        }

        return execTask;
    }

    private void waitForXWikiToLoad() throws Exception
    {
        // Wait till the main page becomes available which means the server is started fine
        System.out.println("Checking that XWiki is up and running...");

        Response response = isXWikiStarted(getURL(), TIMEOUT_SECONDS);
        if (response.timedOut) {
            String message =
                "Failed to start XWiki in [" + TIMEOUT_SECONDS + "] seconds, last error code [" + response.responseCode
                    + ", message [" + new String(response.responseBody) + "]";
            System.out.println(message);
            stop();
            throw new RuntimeException(message);
        } else {
            System.out.println("Server is answering to [" + getURL() + "]... cool");
        }
    }

    public Response isXWikiStarted(String url, int timeout) throws Exception
    {
        HttpClient client = new HttpClient();

        boolean connected = false;
        long startTime = System.currentTimeMillis();
        Response response = new Response();
        response.timedOut = false;
        response.responseCode = -1;
        response.responseBody = new byte[0];
        while (!connected && !response.timedOut) {
            GetMethod method = new GetMethod(url);

            // Don't retry automatically since we're doing that in the algorithm below
            method.getParams()
                .setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(0, false));
            // Set a socket timeout to ensure the server has no chance of not answering to our request...
            method.getParams().setParameter(HttpMethodParams.SO_TIMEOUT, new Integer(10000));

            try {
                // Execute the method.
                response.responseCode = client.executeMethod(method);

                // We must always read the response body.
                response.responseBody = method.getResponseBody();

                if (DEBUG) {
                    System.out.println("Result of pinging [" + url + "] = [" + response.responseCode + "], Message = ["
                        + new String(response.responseBody) + "]");
                }

                // check the http response code is either not an error, either "unauthorized"
                // (which is the case for products that deny view for guest, for example).
                connected = (response.responseCode < 400 || response.responseCode == 401);
            } catch (Exception e) {
                // Do nothing as it simply means the server is not ready yet...
            } finally {
                // Release the connection.
                method.releaseConnection();
            }
            Thread.sleep(500L);
            response.timedOut = (System.currentTimeMillis() - startTime > timeout * 1000L);
        }
        return response;
    }

    public void stop() throws Exception
    {
        // Stop XWiki if it was started by start()
        if (!this.wasStarted) {
            createStopTask().execute();
        }
        System.out.println("XWiki server stopped");
    }

    public String getWebInfDirectory()
    {
        return getExecutionDirectory() + WEBINF_PATH;
    }

    public String getXWikiCfgPath()
    {
        return getExecutionDirectory() + XWIKICFG_PATH;
    }

    public String getXWikiPropertiesPath()
    {
        return getExecutionDirectory() + XWIKIPROPERTIES_PATH;
    }

    public Properties loadXWikiCfg() throws Exception
    {
        return getProperties(getXWikiCfgPath());
    }

    public Properties loadXWikiProperties() throws Exception
    {
        return getProperties(getXWikiPropertiesPath());
    }

    private Properties getProperties(String path) throws Exception
    {
        Properties properties = new Properties();

        FileInputStream fis;
        try {
            fis = new FileInputStream(path);

            try {
                properties.load(fis);
            } finally {
                fis.close();
            }
        } catch (FileNotFoundException e) {
            LOGGER.debug("Failed to load properties [" + path + "]", e);
        }

        return properties;
    }

    public void saveXWikiCfg(Properties properties) throws Exception
    {
        saveProperties(getXWikiCfgPath(), properties);
    }

    public void saveXWikiProperties(Properties properties) throws Exception
    {
        saveProperties(getXWikiPropertiesPath(), properties);
    }

    private void saveProperties(String path, Properties properties) throws Exception
    {
        FileOutputStream fos = new FileOutputStream(path);
        try {
            properties.store(fos, null);
        } finally {
            fos.close();
        }
    }

    private String getURL()
    {
        // We use "xpage=plain" for 2 reasons:
        // 1) the page loads faster since it doesn't need to display the skin
        // 2) if the page doesn't exist it won't return a 404 HTTP Response code
        return "http://localhost:" + getPort() + "/xwiki/bin/view/Main?xpage=plain";
    }

    private String getDefaultStartCommand(int port, int stopPort, int rmiPort)
    {
        String startCommand = START_COMMAND;
        if (startCommand == null) {
            if (SystemUtils.IS_OS_WINDOWS) {
                startCommand = String.format("cmd /c start_xwiki.bat %s %s", port, stopPort);
            } else {
                startCommand = String.format("sh -f start_xwiki.sh %s %s", port, stopPort);
            }
        } else {
            startCommand = startCommand.replaceFirst(DEFAULT_PORT, String.valueOf(port));
            startCommand = startCommand.replaceFirst(DEFAULT_STOPPORT, String.valueOf(stopPort));
            startCommand = startCommand.replaceFirst(DEFAULT_RMIPORT, String.valueOf(rmiPort));
        }

        return startCommand;
    }

    private String getDefaultStopCommand(int stopPort)
    {
        String stopCommand = STOP_COMMAND;
        if (stopCommand == null) {
            if (SystemUtils.IS_OS_WINDOWS) {
                stopCommand = String.format("cmd /c stop_xwiki.bat %s", stopPort);
            } else {
                stopCommand = String.format("sh -f stop_xwiki.sh %s", stopPort);
            }
        } else {
            stopCommand = stopCommand.replaceFirst(DEFAULT_STOPPORT, String.valueOf(stopPort));
        }

        return stopCommand;
    }
}
