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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.apache.commons.exec.environment.EnvironmentUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Start and stop an XWiki instance.
 *
 * @version $Id$
 * @since 2.0RC1
 */
public class XWikiExecutor
{
    /**
     * Only start XWiki if the System property {@code xwiki.test.startXWiki} is undefined or has a value of true. This
     * allows the build to start XWiki (this is the case for example when running functional tests with Docker).
     */
    private static final boolean SHOULD_START_XWIKI =
        Boolean.valueOf(System.getProperty("xwiki.test.startXWiki", "true"));

    protected static final Logger LOGGER = LoggerFactory.getLogger(XWikiExecutor.class);

    /**
     * If defined then we check for an existing running XWiki instance before trying to start XWiki.
     */
    public static final String VERIFY_RUNNING_XWIKI_AT_START =
        System.getProperty("xwiki.test.verifyRunningXWikiAtStart", "true");

    public static final String BASEDIR = System.getProperty("basedir");

    public static final String URL = System.getProperty("xwiki.test.baseURL", "http://localhost");

    /**
     * By default assume that XWiki is deployed under the "xwiki" Servlet context. Pass an empty string to singify the
     * root context. Note the "/" suffix which is required.
     */
    public static final String DEFAULT_CONTEXT = System.getProperty("xwiki.test.context", "/xwiki");

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

    private static final long PROCESS_FINISH_TIMEOUT = 5 * 60L * 1000L;

    private static final int VERIFY_RUNNING_XWIKI_AT_START_TIMEOUT =
        Integer.valueOf(System.getProperty("xwiki.test.verifyRunningXWikiAtStartTimeout", "15"));

    private static final int DEBUG_PORT = 5005;

    private int port;

    private int stopPort;

    private int rmiPort;

    private String executionDirectory;

    private Map<String, String> environment = new HashMap<>();

    private DefaultExecuteResultHandler startedProcessHandler;

    private FileBasedConfigurationBuilder<PropertiesConfiguration> xwikipropertiesBuilder;

    /**
     * Was XWiki server already started. We don't try to stop it if it was already started.
     */
    private boolean wasStarted;

    /**
     * @see #isManaged()
     */
    private boolean managed;

    private XWikiWatchdog watchdog = new XWikiWatchdog();

    private long startTimeout = Long.valueOf(System.getProperty("xwikiExecutionStartTimeout", "120"));

    private int debugPort ;

    public XWikiExecutor(int index)
    {
        // resolve ports
        String portString = System.getProperty("xwikiPort" + index);
        this.port = portString != null ? Integer.valueOf(portString) : (Integer.valueOf(DEFAULT_PORT) + index);
        String stopPortString = System.getProperty("xwikiStopPort" + index);
        this.stopPort =
            stopPortString != null ? Integer.valueOf(stopPortString) : (Integer.valueOf(DEFAULT_STOPPORT) - index);
        String rmiPortString = System.getProperty("rmiPort" + index);
        this.rmiPort =
            rmiPortString != null ? Integer.valueOf(rmiPortString) : (Integer.valueOf(DEFAULT_RMIPORT) + index);
        this.debugPort = DEBUG_PORT + index;

        // Resolve the execution directory, which should point to a location where an XWiki distribution is located
        // and can be started (directory where the start_xwiki.sh|bat files are located).
        this.executionDirectory = System.getProperty("xwikiExecutionDirectory" + index);
        if (this.executionDirectory == null) {
            this.executionDirectory = DEFAULT_EXECUTION_DIRECTORY;
            if (this.executionDirectory == null) {
                if (BASEDIR != null) {
                    this.executionDirectory = BASEDIR + '/';
                } else {
                    this.executionDirectory = "";
                }
                this.executionDirectory += "target/xwiki";
            }
            if (index > 0) {
                this.executionDirectory += "-" + index;
            }
        }
        // Make sure the execution directory exists
        try {
            FileUtils.forceMkdir(new File(this.executionDirectory));
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to create directory [%s]", this.executionDirectory), e);
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

    public int getDebugPort()
    {
        return this.debugPort;
    }

    public String getExecutionDirectory()
    {
        if (this.executionDirectory == null) {
            throw new RuntimeException("Invalid configuration for the execution directory. The "
                + "[xwikiExecutionDirectory] system property must be specified.");
        }
        return this.executionDirectory;
    }

    public void setXWikiOpts(String opts)
    {
        addEnvironmentVariable("XWIKI_OPTS", opts);
    }

    /**
     * Change the timeout checked when starting and initializing XWiki.
     * 
     * @since 10.11RC1
     */
    public void setTimeoutSeconds(long timeoutSeconds)
    {
        this.startTimeout = timeoutSeconds;
    }

    public void addEnvironmentVariable(String key, String value)
    {
        this.environment.put(key, value);
    }

    /**
     * @return true the XWiki instance was (successfully) started by the executor itself (other possibilities being
     *         failed startup or already running instance).
     * @since 9.7RC1
     */
    public boolean isManaged()
    {
        return this.managed;
    }

    /**
     * Start XWiki using the following strategy:
     * <ul>
     * <li>If the {@code xwiki.test.startXWiki} system property is set to "false" then don't start/stop XWiki</li>
     * <li>If the {@link #VERIFY_RUNNING_XWIKI_AT_START} property is set then checks if an XWiki instance is already
     * running before trying to start XWiki and if so, reuse it and don't start XWiki</li>
     * <li>If the {@link #VERIFY_RUNNING_XWIKI_AT_START} property is set to false then verify if some XWiki instance is
     * already running by verifying if the port is free and fail if so. Otherwise start XWiki.</li>
     * </ul>
     * 
     * @throws Exception when failing to start XWiki
     */
    public void start() throws Exception
    {
        if (!SHOULD_START_XWIKI) {
            return;
        }

        this.wasStarted = false;
        if (VERIFY_RUNNING_XWIKI_AT_START.equals("true")) {
            LOGGER.info("Checking if an XWiki server is already started at [{}]", getURL());
            // First, verify if XWiki is started. If it is then don't start it again.
            this.wasStarted = !this.watchdog.isXWikiStarted(getURL(), VERIFY_RUNNING_XWIKI_AT_START_TIMEOUT).timedOut;
        }

        if (!this.wasStarted) {
            LOGGER.info("Stopping any potentially running XWiki server at [{}]", getURL());
            stopInternal();
            LOGGER.info("Starting XWiki server at [{}], using stop port [{}] and RMI port [{}]", getURL(),
                getStopPort(), getRMIPort());
            startXWiki();
            waitForXWikiToLoad();
            this.managed = true;
        } else {
            LOGGER.info("XWiki server is already started at [{}]", getURL());
        }
    }

    private DefaultExecuteResultHandler executeCommand(String commandLine) throws Exception
    {
        // The command line to execute
        CommandLine command = CommandLine.parse(commandLine);

        // Execute the process asynchronously so that we don't block.
        DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();

        // Send Process output and error streams to our logger.
        PumpStreamHandler streamHandler = new PumpStreamHandler(new XWikiLogOutputStream());

        // Make sure we end the process when the JVM exits
        ShutdownHookProcessDestroyer processDestroyer = new ShutdownHookProcessDestroyer();

        // Prevent the process from running indefinitely and kill it after 1 hour...
        ExecuteWatchdog watchDog = new ExecuteWatchdog(60L * 60L * 1000L);

        // The executor to execute the command
        DefaultExecutor executor = new DefaultExecutor();
        executor.setStreamHandler(streamHandler);
        executor.setWorkingDirectory(new File(getExecutionDirectory()));
        executor.setProcessDestroyer(processDestroyer);
        executor.setWatchdog(watchDog);

        // Inherit the current process's environment variables and add the user-defined ones
        @SuppressWarnings("unchecked")
        Map<String, String> newEnvironment = EnvironmentUtils.getProcEnvironment();
        newEnvironment.putAll(this.environment);

        try {
            executor.execute(command, newEnvironment, resultHandler);
        } catch (Exception e) {
            throw new Exception(String.format("Failed to execute command [%s]", commandLine), e);
        }

        return resultHandler;
    }

    /**
     * Starts XWiki and returns immediately.
     */
    private void startXWiki() throws Exception
    {
        File dir = new File(getExecutionDirectory());
        if (dir.exists()) {
            String startCommand = getDefaultStartCommand(getPort(), getStopPort(), getRMIPort(), getDebugPort());
            LOGGER.debug("Executing command: [{}]", startCommand);
            this.startedProcessHandler = executeCommand(startCommand);
        } else {
            throw new Exception(String.format("Invalid directory from where to start XWiki [%s]. If you're starting "
                + "a functional test from your IDE, make sure to either have started an XWiki instance beforehand or "
                + "configure your IDE so that either the [basedir] or [xwikiExecutionDirectory] properties have been "
                + "defined so that the test framework can start XWiki for you. If you set [basedir] make it point to "
                + "the directory containing the [target/] directory of your project. The test framework will then try "
                + "to locate an XWiki instance in [<basedir>/target/xwiki]. If the XWiki instance you wish to start is "
                + "elsewhere then define the [xwikiExecutionDirectory] System property to point to it.",
                this.executionDirectory));
        }
    }

    private void waitForXWikiToLoad() throws Exception
    {
        // Wait till the main page becomes available which means the server is started fine
        LOGGER.info("Checking that XWiki is up and running...");

        // If we're in debug mode then don't use a timeout (or rather use a very long one - we use 1 day) since we'll
        // start XWiki in debug mode and wait to connect to it (suspend = true).
        long timeout = DEBUG ? 60*60*24 : this.startTimeout;
        WatchdogResponse response = this.watchdog.isXWikiStarted(getURL(), timeout);
        if (response.timedOut) {
            String message = String.format("Failed to start XWiki in [%s] seconds, last error code [%s], message [%s]",
                timeout, response.responseCode, new String(response.responseBody));
            LOGGER.info(message);
            stop();
            throw new RuntimeException(message);
        } else {
            LOGGER.info("Server is answering to [{}]... cool", getURL());
        }
    }

    public void stop() throws Exception
    {
        if (!SHOULD_START_XWIKI) {
            return;
        }

        LOGGER.debug("Checking if we need to stop the XWiki server running at [{}]...", getURL());

        // Do not try to stop XWiki if we've not been successful in starting it!
        if (!this.managed) {
            return;
        }

        // Stop XWiki if it was started by start()
        if (!this.wasStarted) {
            stopInternal();

            // Now wait for the start process to be stopped, waiting a max of 5 minutes!
            if (this.startedProcessHandler != null) {
                waitForProcessToFinish(this.startedProcessHandler, PROCESS_FINISH_TIMEOUT);
            }

            LOGGER.info("XWiki server running at [{}] has been stopped", getURL());
        } else {
            LOGGER.info("XWiki server not stopped since we didn't start it (it was already started)");
        }
    }

    private void stopInternal() throws Exception
    {
        String stopCommand = getDefaultStopCommand(getPort(), getStopPort());
        LOGGER.debug("Executing command: [{}]", stopCommand);
        DefaultExecuteResultHandler stopProcessHandler = executeCommand(stopCommand);

        // First wait for the stop process to have stopped, waiting a max of 5 minutes!
        // It's going to stop the start process...
        waitForProcessToFinish(stopProcessHandler, PROCESS_FINISH_TIMEOUT);
    }

    private void waitForProcessToFinish(DefaultExecuteResultHandler handler, long timeout) throws Exception
    {
        // Wait for the process to finish.
        handler.waitFor(timeout);

        // Check the exit value and fail the test if the process has not properly finished.
        if (handler.getExitValue() != 0 || !handler.hasResult()) {
            if (handler.getExitValue() == 143) {
                LOGGER.warn("XWiki instance was killed with SIGTERM (usually mean it took to long to stop by itself)");
            } else {
                String message =
                    String.format("Process failed to close properly after [%d] seconds, process ended [%b].", timeout,
                        handler.hasResult());
                if (handler.hasResult()) {
                    message = String.format("%s Exit code [%d], message [%s].", message, handler.getExitValue(),
                        handler.getException().getMessage());
                }
                message = String.format("%s Failing test.", message);
                throw new RuntimeException(message);
            }
        }
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

    public PropertiesConfiguration loadXWikiPropertiesConfiguration() throws Exception
    {
        return getPropertiesConfiguration(getXWikiPropertiesPath());
    }

    /**
     * @deprecated since 4.2M1 use {@link #getPropertiesConfiguration(String)} instead
     */
    @Deprecated
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
            LOGGER.debug("Failed to load properties [{}]", path, e);
        }

        return properties;
    }

    /**
     * @since 4.2M1
     */
    private PropertiesConfiguration getPropertiesConfiguration(String path) throws ConfigurationException
    {
        this.xwikipropertiesBuilder =
            new FileBasedConfigurationBuilder<PropertiesConfiguration>(PropertiesConfiguration.class)
                .configure(new Parameters().properties().setListDelimiterHandler(new DefaultListDelimiterHandler(','))
                    .setFileName(path));

        return xwikipropertiesBuilder.getConfiguration();
    }

    public void saveXWikiCfg(Properties properties) throws Exception
    {
        saveProperties(getXWikiCfgPath(), properties);
    }

    /**
     * @deprecated since 4.2M1 use {@link #saveXWikiProperties()} instead
     */
    @Deprecated
    public void saveXWikiProperties(Properties properties) throws Exception
    {
        saveProperties(getXWikiPropertiesPath(), properties);
    }

    /**
     * @since 4.2M1
     */
    public void saveXWikiProperties() throws Exception
    {
        this.xwikipropertiesBuilder.save();
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

    public String getURL()
    {
        // We use "get" action for 2 reasons:
        // 1) the page loads faster since it doesn't need to display the skin
        // 2) if the page doesn't exist it won't return a 404 HTTP Response code
        return URL + ":" + getPort() + DEFAULT_CONTEXT + "/bin/get/Main/";
    }

    private String getDefaultStartCommand(int port, int stopPort, int rmiPort, int debugPort)
    {
        String startCommand = START_COMMAND;
        if (startCommand == null) {
            String scriptNamePrefix = DEBUG ? "start_xwiki_debug" : "start_xwiki";
            if (SystemUtils.IS_OS_WINDOWS) {
                startCommand = String.format("cmd /c %s.bat %s %s", scriptNamePrefix, port, stopPort);
            } else {
                String debugParams = DEBUG ? String.format("-dp %d --suspend", debugPort) : "";
                startCommand =
                    String.format("bash %s.sh -p %s -sp %s %s", scriptNamePrefix, port, stopPort, debugParams);
            }
        } else {
            startCommand = startCommand.replaceFirst(DEFAULT_PORT, String.valueOf(port));
            startCommand = startCommand.replaceFirst(DEFAULT_STOPPORT, String.valueOf(stopPort));
            startCommand = startCommand.replaceFirst(DEFAULT_RMIPORT, String.valueOf(rmiPort));
        }

        return startCommand;
    }

    private String getDefaultStopCommand(int port, int stopPort)
    {
        String stopCommand = STOP_COMMAND;
        if (stopCommand == null) {
            if (SystemUtils.IS_OS_WINDOWS) {
                stopCommand = String.format("cmd /c stop_xwiki.bat %s", stopPort);
            } else {
                stopCommand = String.format("bash stop_xwiki.sh -p %s -sp %s", port, stopPort);
            }
        } else {
            stopCommand = stopCommand.replaceFirst(DEFAULT_PORT, String.valueOf(port));
            stopCommand = stopCommand.replaceFirst(DEFAULT_STOPPORT, String.valueOf(stopPort));
        }

        return stopCommand;
    }
}
