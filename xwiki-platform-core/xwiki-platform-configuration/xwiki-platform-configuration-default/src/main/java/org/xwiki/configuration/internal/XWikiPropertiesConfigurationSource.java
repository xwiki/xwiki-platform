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
import java.net.URL;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.environment.Environment;

/**
 * Looks for configuration data in {@code /WEB-INF/xwiki.properties}.
 * 
 * @version $Id$
 * @since 2.0M1
 */
@Component
@Named("xwikiproperties")
@Singleton
public class XWikiPropertiesConfigurationSource extends CommonsConfigurationSource implements Initializable
{
    private static final String XWIKI_PROPERTIES_FILE = "xwiki.properties";

    private static final String XWIKI_PROPERTIES_WARPATH = "/WEB-INF/" + XWIKI_PROPERTIES_FILE;

    private static final String XWIKI_PROPERTIES_DEFAULT_DIR_SYSTEM_PROPERTY = "xwiki.properties.default.dir";

    /**
     * the Environment from where to get the XWiki properties file.
     */
    @Inject
    private Environment environment;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    @Override
    public void initialize() throws InitializationException
    {
        setConfiguration(loadConfiguration());
    }

    private Configuration loadConfiguration()
    {
        String defaultDir = System.getProperty(XWIKI_PROPERTIES_DEFAULT_DIR_SYSTEM_PROPERTY, "/etc/xwiki");
        // Looking for /etc/xwiki/xwiki.properties first
        File file = new File(defaultDir, XWIKI_PROPERTIES_FILE);
        if (file.exists()) {
            try {
                this.logger.info("loading {} from default location {}", XWIKI_PROPERTIES_FILE, file.getCanonicalPath());
                return new FileBasedConfigurationBuilder<PropertiesConfiguration>(PropertiesConfiguration.class)
                    .configure(new Parameters().properties()
                        .setListDelimiterHandler(new DefaultListDelimiterHandler(',')).setFile(file))
                    .getConfiguration();
            } catch (Exception e) {
                // Note: if we cannot read the configuration file for any reason we log a warning but continue since
                // XWiki will use default values for all configurable elements.
                this.logger.warn("Failed to load configuration file [{}]: {}", file, e.getMessage());
            }
        }

        // Register the Commons Properties Configuration, looking for a xwiki.properties file
        // in the XWiki path somewhere.
        URL xwikiPropertiesUrl = null;
        try {
            xwikiPropertiesUrl = this.environment.getResource(XWIKI_PROPERTIES_WARPATH);
            if (xwikiPropertiesUrl != null) {
                this.logger.info("loading {} from {}", XWIKI_PROPERTIES_FILE, xwikiPropertiesUrl.toExternalForm());
                FileBasedConfigurationBuilder<PropertiesConfiguration> builder =
                    new FileBasedConfigurationBuilder<PropertiesConfiguration>(PropertiesConfiguration.class)
                        .configure(new Parameters().properties()
                            .setListDelimiterHandler(new DefaultListDelimiterHandler(',')).setURL(xwikiPropertiesUrl));
                return builder.getConfiguration();
            } else {
                // We use a debug logging level here since we consider it's ok that there's no XWIKI_PROPERTIES_FILE
                // available, in which case default values are used.
                this.logger.debug("No configuration file [{}] found. Using default configuration values.",
                    XWIKI_PROPERTIES_WARPATH);
            }
        } catch (Exception e) {
            // Note: if we cannot read the configuration file for any reason we log a warning but continue since XWiki
            // will use default values for all configurable elements.
            this.logger.warn(
                "Failed to load configuration file [{}]. Using default configuration values. Internal error [{}]",
                XWIKI_PROPERTIES_WARPATH, e.getMessage());
        }

        // If no Commons Properties Configuration has been set, use a default empty Commons Configuration
        // implementation.
        return new BaseConfiguration();
    }
}
