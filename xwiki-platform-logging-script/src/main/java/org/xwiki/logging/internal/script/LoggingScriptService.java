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
package org.xwiki.logging.internal.script;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.apache.commons.lang3.ArrayUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.logging.LogQueue;
import org.xwiki.logging.event.LogQueueListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.script.service.ScriptService;

/**
 * Provide logging related script oriented APIs.
 * 
 * @version $Id$
 */
@Component
@Named("logging")
@Singleton
public class LoggingScriptService implements ScriptService, Initializable
{
    /**
     * Used to listen to logs.
     */
    @Inject
    private ObservationManager observation;

    /**
     * All the produced log since last call to {@link #startLog()}.
     */
    private LogQueue logQueue = new LogQueue();

    /**
     * The actual log listener.
     */
    private LogQueueListener logQueueListener = new LogQueueListener("logging.script", this.logQueue);

    private MBeanServer jmxServer;

    private ObjectName jmxName;

    // TODO: put needed generic methods in LogManager instead of using JMX directly
    @Override
    public void initialize() throws InitializationException
    {
        this.jmxServer = ManagementFactory.getPlatformMBeanServer();
        try {
            this.jmxName = new ObjectName("logback:type=xwiki");
        } catch (Exception e) {
            // That should never happen
        }
    }

    // JMX

    private List<String> jmxLoggerList() throws ReflectionException, MBeanException, InstanceNotFoundException,
        AttributeNotFoundException
    {
        return (List<String>) this.jmxServer.getAttribute(this.jmxName, "LoggerList");
    }

    private String jmxgetLoggerLevel(String level) throws InstanceNotFoundException, ReflectionException,
        MBeanException
    {
        return (String) this.jmxServer.invoke(this.jmxName, "getLoggerLevel", new Object[] {level},
            new String[] {"java.lang.String"});
    }

    private String jmxsetLevel(String logger, String level) throws InstanceNotFoundException, ReflectionException,
        MBeanException
    {
        return (String) this.jmxServer.invoke(this.jmxName, "setLoggerLevel", new Object[] {logger, level}, new String[] {
        "java.lang.String", "java.lang.String"});
    }

    private String jmxreloadDefaultConfiguration() throws InstanceNotFoundException, ReflectionException,
        MBeanException
    {
        return (String) this.jmxServer.invoke(this.jmxName, "reloadDefaultConfiguration",
            ArrayUtils.EMPTY_OBJECT_ARRAY, ArrayUtils.EMPTY_STRING_ARRAY);
    }

    // Configuration

    public void reloadDefaultConfiguration() throws InstanceNotFoundException, ReflectionException, MBeanException
    {
        jmxreloadDefaultConfiguration();
    }

    // Get/Set log levels

    /**
     * @return all the loggers (usually packages) with corresponding levels.
     */
    public Map<String, String> getLevels() throws InstanceNotFoundException, ReflectionException, MBeanException,
        AttributeNotFoundException
    {
        List<String> loggers = jmxLoggerList();

        Map<String, String> levels = new HashMap<String, String>(loggers.size());

        for (String logger : loggers) {
            levels.put(logger, getLevel(logger));
        }

        return levels;
    }

    /**
     * @param logger the logger name (usually packages)
     * @return the level associated to the logger
     */
    public String getLevel(String logger) throws InstanceNotFoundException, ReflectionException, MBeanException
    {
        return jmxgetLoggerLevel(logger);
    }

    /**
     * @param logger the logger name (usually package)
     * @param level the level associated to the logger
     */
    public void setLevel(String logger, String level) throws InstanceNotFoundException, ReflectionException,
        MBeanException
    {
        jmxsetLevel(logger, level);
    }

    // Log queue

    /**
     * Start listening to produced logs and fill the log queue.
     * <p>
     * The previous log is removed first.
     * 
     * @see #getLogQueue()
     */
    public void startLog()
    {
        this.logQueue.clear();
        if (this.observation.getListener(this.logQueueListener.getName()) != this.logQueueListener) {
            this.observation.addListener(this.logQueueListener);
        }
    }

    /**
     * Stop listening to logs.
     */
    public void endLog()
    {
        this.observation.removeListener(this.logQueueListener.getName());
    }

    /**
     * @return all the log produced since {@link #startLog()} has been called
     */
    public LogQueue getLogQueue()
    {
        return this.logQueue;
    }
}
