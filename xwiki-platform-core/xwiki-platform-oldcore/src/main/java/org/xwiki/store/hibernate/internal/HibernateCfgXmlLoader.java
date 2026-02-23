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
package org.xwiki.store.hibernate.internal;

import java.io.File;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.hibernate.boot.cfgxml.internal.ConfigLoader;
import org.hibernate.boot.cfgxml.spi.LoadedConfig;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.environment.Environment;

import com.xpn.xwiki.internal.store.hibernate.HibernateConfiguration;

/**
 * Utility component to load and adjust Hibernate cfg.xml configuration.
 *
 * @version $Id$
 * @since 18.2.0RC1
 */
@Component(roles = HibernateCfgXmlLoader.class)
@Singleton
public class HibernateCfgXmlLoader
{
    private static final String PROPERTY_PERMANENT_DIRECTORY = "${environment.permanentDirectory}";

    private static final String PROPERTY_TIMEZONE_VARIABLE = "${timezone}";

    @Inject
    private Logger logger;

    @Inject
    private HibernateConfiguration hibernateConfiguration;

    @Inject
    private Environment environment;

    /**
     * @return the resolved Hibernate cfg.xml URL or {@code null} if none is found
     */
    public URL getConfigurationURL()
    {
        String path = this.hibernateConfiguration.getPath();

        if (StringUtils.isEmpty(path)) {
            return null;
        }

        File file = new File(path);
        try {
            if (file.exists()) {
                return file.toURI().toURL();
            }
        } catch (Exception e) {
            this.logger.debug("Failed load resource [{}] using a file path", path);
        }

        try {
            URL res = this.environment.getResource(path);
            if (res != null) {
                return res;
            }
        } catch (Exception e) {
            this.logger.debug("Failed to load resource [{}] using the application context", path);
        }

        URL url = Thread.currentThread().getContextClassLoader().getResource(path);
        if (url == null) {
            this.logger.error("Failed to find hibernate configuration file corresponding to path [{}]", path);
        }

        return url;
    }

    /**
     * Load the cfg.xml configuration and resolve the supported variables.
     *
     * @param bootstrapServiceRegistry the bootstrap registry to use
     * @param configurationURL the cfg.xml URL
     * @return the loaded config
     */
    public LoadedConfig loadConfig(BootstrapServiceRegistry bootstrapServiceRegistry, URL configurationURL)
    {
        ConfigLoader configLoader = new ConfigLoader(bootstrapServiceRegistry);
        LoadedConfig loadedConfig = configLoader.loadConfigXmlUrl(configurationURL);
        @SuppressWarnings("unchecked")
        Map<String, Object> configurationValues = loadedConfig.getConfigurationValues();
        resolveVariables(configurationValues);
        return loadedConfig;
    }

    /**
     * Resolve known variables in the Hibernate configuration.
     *
     * @param hibernateConfiguration the legacy Hibernate configuration
     */
    public void resolveVariables(Configuration hibernateConfiguration)
    {
        resolveJdbcUrl(hibernateConfiguration.getProperty(AvailableSettings.URL))
            .ifPresent(resolved -> hibernateConfiguration.setProperty(AvailableSettings.URL, resolved));
    }

    /**
     * Resolve known variables in the Hibernate configuration map.
     *
     * @param values the configuration values
     */
    public void resolveVariables(Map<String, Object> values)
    {
        Object urlValue = values.get(AvailableSettings.URL);
        if (urlValue instanceof String url) {
            resolveJdbcUrl(url).ifPresent(resolved -> values.put(AvailableSettings.URL, resolved));
        }
    }

    /**
     * @param url the raw JDBC URL
     * @return the resolved URL or {@code null} if no change is required
     */
    private Optional<String> resolveJdbcUrl(String url)
    {
        if (Strings.CS.containsAny(url, PROPERTY_PERMANENT_DIRECTORY, PROPERTY_TIMEZONE_VARIABLE)) {
            String newURL = Strings.CS.replace(url, PROPERTY_PERMANENT_DIRECTORY,
                this.environment.getPermanentDirectory().getAbsolutePath());

            String resolved = Strings.CS.replace(newURL, PROPERTY_TIMEZONE_VARIABLE,
                URLEncoder.encode(TimeZone.getDefault().getID(), StandardCharsets.UTF_8));
            this.logger.debug("Resolved Hibernate URL [{}] to [{}]", url, resolved);
            return Optional.of(resolved);
        }

        return Optional.empty();
    }

    /**
     * Apply the shared DataSource override and clean up conflicting settings.
     *
     * @param values the configuration values
     * @param dataSource the shared data source
     */
    public void applySharedDataSourceOverrides(Map<String, Object> values, DataSource dataSource)
    {
        values.put(AvailableSettings.DATASOURCE, dataSource);
        // Remove any connection provider class since we've already set the DataSource and having a connection
        // provider class is likely to conflict with it.
        values.remove("hibernate.connection.provider_class");
        values.remove("connection.provider_class");
        // Remove any connection URL, username and password since we've already set the DataSource and having them is
        // likely to conflict with it.
        values.remove(AvailableSettings.URL);
        values.remove(AvailableSettings.USER);
        values.remove(AvailableSettings.PASS);
        values.remove("connection.username");
        values.remove("connection.password");
    }
}
