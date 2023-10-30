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
package org.xwiki.configuration.internal;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSaveException;
import org.xwiki.environment.Environment;

/**
 * Manipulate the configuration stored in the permanent directory {@code <permdir>/configuration.properties}.
 * 
 * @version $Id$
 * @since 15.9
 * @since 15.5.4
 * @since 14.10.19
 */
@Component
@Named("permanent")
@Singleton
public class PermanentConfigurationSource extends CommonsConfigurationSource implements Initializable
{
    private static final String XWIKI_PROPERTIES_FILE = "configuration.properties";

    /**
     * the Environment from where to get the XWiki properties file.
     */
    @Inject
    private Environment environment;

    private FileBasedConfigurationBuilder<PropertiesConfiguration> builder;

    @Override
    public void initialize() throws InitializationException
    {
        setConfiguration(loadConfiguration());
    }

    private Configuration loadConfiguration() throws InitializationException
    {
        File permanentDirectory = this.environment.getPermanentDirectory();
        File permanentConfiguration = new File(permanentDirectory, XWIKI_PROPERTIES_FILE).getAbsoluteFile();

        // If it does not exist, create it
        if (!permanentConfiguration.exists()) {
            try {
                FileUtils.write(permanentConfiguration, "", StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new InitializationException("Failed to create the file [" + permanentConfiguration + "]", e);
            }
        }

        try {
            // Create the configuration builder
            this.builder = new Configurations().propertiesBuilder(permanentConfiguration);

            // Build the configuration
            return this.builder.getConfiguration();
        } catch (ConfigurationException e) {
            throw new InitializationException(
                "Failed to create the Configuration for file [" + permanentConfiguration + "]", e);
        }
    }

    private void addProperty(String key, Object value)
    {
        if (value != null) {
            Class<?> valueClass = value.getClass();

            Object convertedValue = value;
            if (valueClass != String.class && !List.class.isAssignableFrom(valueClass)
                && !Properties.class.isAssignableFrom(valueClass)) {
                convertedValue = this.converterManager.convert(String.class, value);
            }
            getConfiguration().addProperty(key, convertedValue);
        }
    }

    @Override
    public void setProperty(String key, Object value) throws ConfigurationSaveException
    {
        this.lock.writeLock().lock();

        try {
            getConfiguration().clearProperty(key);
            addProperty(key, value);

            // Update the configuration file
            save();
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    @Override
    public void setProperties(Map<String, Object> properties) throws ConfigurationSaveException
    {
        this.lock.writeLock().lock();

        try {
            getConfiguration().clear();
            properties.forEach(this::addProperty);

            // Update the configuration file
            save();
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    private void save() throws ConfigurationSaveException
    {
        try {
            this.builder.save();
        } catch (ConfigurationException e) {
            throw new ConfigurationSaveException("Failed to save the configuration file", e);
        }
    }
}
