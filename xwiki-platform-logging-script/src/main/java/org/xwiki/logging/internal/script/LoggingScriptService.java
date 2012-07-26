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
import java.util.logging.LogManager;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.xwiki.component.annotation.Component;
import org.xwiki.logging.LogQueue;
import org.xwiki.logging.event.LogQueueListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.script.service.ScriptService;

import ch.qos.logback.classic.jmx.JMXConfiguratorMBean;

@Component
@Named("logging")
@Singleton
public class LoggingScriptService implements ScriptService
{
    @Inject
    private LogManager logManager;

    @Inject
    private ObservationManager observation;

    private LogQueue logQueue = new LogQueue();

    private LogQueueListener logQueueListener = new LogQueueListener("logging.script", this.logQueue);

    private JMXConfiguratorMBean mbean;

    // TODO: put needed generic methods in LogManager instead of using JMX directly
    public JMXConfiguratorMBean getJMXConfiguratorMBean() throws InstanceNotFoundException
    {
        if (this.mbean == null) {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            ObjectName name;
            try {
                name = new ObjectName("logback:type=xwiki");
            } catch (Exception e) {
                // That should never happen
                name = null;
            }

            this.mbean = (JMXConfiguratorMBean) server.getObjectInstance(name);
        }

        return this.mbean;
    }

    // Get/Set log levels

    public Map<String, String> getLevels() throws InstanceNotFoundException
    {
        List<String> loggers = getJMXConfiguratorMBean().getLoggerList();

        Map<String, String> levels = new HashMap<String, String>(loggers.size());

        for (String logger : loggers) {
            levels.put(logger, getLevel(logger));
        }

        return levels;
    }

    public String getLevel(String logger) throws InstanceNotFoundException
    {
        return getJMXConfiguratorMBean().getLoggerLevel(logger);
    }

    public void setLevel(String logger, String level) throws InstanceNotFoundException
    {
        getJMXConfiguratorMBean().setLoggerLevel(logger, level);
    }

    // LogGueue

    public void startLog()
    {
        this.logQueue.clear();
        if (this.observation.getListener(this.logQueueListener.getName()) != this.logQueueListener) {
            this.observation.addListener(this.logQueueListener);
        }
    }

    public void endLog()
    {
        this.observation.removeListener(this.logQueueListener.getName());
    }

    public LogQueue getLogQueue()
    {
        return this.logQueue;
    }
}
