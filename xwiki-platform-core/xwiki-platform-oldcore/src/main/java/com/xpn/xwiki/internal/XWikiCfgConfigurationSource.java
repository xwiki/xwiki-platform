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
package com.xpn.xwiki.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.commons.collections4.EnumerationUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.environment.Environment;
import org.xwiki.properties.ConverterManager;

/**
 * Looks for configuration data in {@code /WEB-INF/xwiki.cfg}.
 *
 * @version $Id$
 * @since 6.1M2
 */
@Component
@Named(XWikiCfgConfigurationSource.ROLEHINT)
@Singleton
public class XWikiCfgConfigurationSource implements ConfigurationSource, Initializable
{
    /**
     * The name of the JDNI variable.
     */
    public static final String CFG_ENV_NAME = "XWikiConfig";

    /**
     * The component role hint.
     */
    public static final String ROLEHINT = "xwikicfg";

    @Inject
    private Environment environment;

    @Inject
    private ConverterManager converter;

    @Inject
    private Logger logger;

    private Properties properties = new Properties();

    private String configurationLocation;

    /**
     * @return the location where to find the configuration
     */
    public static String getConfigPath()
    {
        String configurationLocation;

        try {
            Context envContext = (Context) new InitialContext().lookup("java:comp/env");
            configurationLocation = (String) envContext.lookup(CFG_ENV_NAME);
        } catch (Exception e) {
            configurationLocation = "/etc/xwiki/xwiki.cfg";
            if (!new File(configurationLocation).exists()) {
                configurationLocation = "/WEB-INF/xwiki.cfg";
            }
        }

        return configurationLocation;
    }

    @Override
    public void initialize() throws InitializationException
    {
        this.configurationLocation = getConfigPath();

        InputStream xwikicfgis = loadConfiguration();

        if (xwikicfgis != null) {
            try {
                this.properties.load(xwikicfgis);
            } catch (Exception e) {
                this.logger.error("Failed to load configuration", e);
            }
        }
    }

    private InputStream loadConfiguration()
    {
        this.configurationLocation = getConfigPath();

        // First try loading from a file.
        File f = new File(this.configurationLocation);
        try {
            if (f.exists()) {
                return new FileInputStream(f);
            }
        } catch (Exception e) {
            // Error loading the file. Most likely, the Security Manager prevented it.
            // We'll try loading it as a resource below.
            this.logger.debug("Failed to load the file [{}] using direct file access."
                + " Trying to load it as a resource using the Servlet Context...", this.configurationLocation, e);
        }

        // Second, try loading it as a resource from the environment
        InputStream xwikicfgis = this.environment.getResourceAsStream(this.configurationLocation);

        // Third, try loading it from the classloader used to load this current class
        if (xwikicfgis == null) {
            this.logger.debug("Failed to load the file [{}] as a resource using the Servlet Context."
                + " Trying to load it as classpath resource...", this.configurationLocation);

            xwikicfgis = Thread.currentThread().getContextClassLoader().getResourceAsStream("xwiki.cfg");
        }

        return xwikicfgis;
    }

    /**
     * @return the configuration location
     */
    public String getConfigurationLocation()
    {
        return this.configurationLocation;
    }

    private <T> T convert(String value, Class<T> targetClass, T defaultValue)
    {
        try {
            if (targetClass == String[].class) {
                // Retro compatibility from old XWikiConfig class
                return (T) StringUtils.split(value, " ,");
            } else {
                return this.converter.convert(targetClass, value);
            }
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * @return the properties instance
     */
    public Properties getProperties()
    {
        return this.properties;
    }

    // ConfigurationSource

    @Override
    public <T> T getProperty(String key, T defaultValue)
    {
        String value = getProperty(key);

        if (value == null) {
            return defaultValue;
        }

        return defaultValue == null ? (T) value : convert(value, (Class<T>) defaultValue.getClass(), defaultValue);
    }

    @Override
    public <T> T getProperty(String key, Class<T> valueClass)
    {
        String value = getProperty(key);

        return convert(value, valueClass, null);
    }

    @Override
    public <T> T getProperty(String key)
    {
        return (T) StringUtils.trim(this.properties.getProperty(key));
    }

    @Override
    public List<String> getKeys()
    {
        return EnumerationUtils.toList((Enumeration<String>) this.properties.propertyNames());
    }

    @Override
    public boolean containsKey(String key)
    {
        return this.properties.containsKey(key);
    }

    @Override
    public boolean isEmpty()
    {
        return this.properties.isEmpty();
    }

    /**
     * @param properties change the internal {@link Properties} instance
     */
    public void set(Properties properties)
    {
        this.properties = properties;
    }
}
