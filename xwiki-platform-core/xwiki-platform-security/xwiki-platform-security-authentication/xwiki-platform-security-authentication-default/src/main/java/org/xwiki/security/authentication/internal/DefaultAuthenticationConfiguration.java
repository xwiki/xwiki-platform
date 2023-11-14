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
package org.xwiki.security.authentication.internal;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSaveException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.security.authentication.AuthenticationConfiguration;

import static java.util.stream.Collectors.toList;

/**
 * Default implementation for {@link AuthenticationConfiguration}.
 *
 * @version $Id$
 * @since 11.6RC1
 */
@Component
@Singleton
public class DefaultAuthenticationConfiguration implements AuthenticationConfiguration
{
    private static final String COOKIE_PREFIX = ".";

    /**
     * Defines from where to read the Resource configuration data.
     */
    @Inject
    @Named("authentication")
    private ConfigurationSource configuration;

    @Inject
    @Named("xwikicfg")
    private ConfigurationSource xwikiCfgConfiguration;

    @Inject
    @Named("permanent")
    private ConfigurationSource permanentConfiguration;

    @Inject
    private Logger logger;

    private String validationKey;

    private String encryptionKey;

    @Override
    public int getMaxAuthorizedAttempts()
    {
        return configuration.getProperty("maxAuthorizedAttempts", 3);
    }

    @Override
    public int getTimeWindow()
    {
        return configuration.getProperty("timeWindowAttempts", 300);
    }

    @Override
    public String[] getFailureStrategies()
    {
        String strategies = configuration.getProperty("failureStrategy", "");
        if (!StringUtils.isEmpty(strategies)) {
            return strategies.split(",");
        } else {
            return new String[0];
        }
    }

    @Override
    public boolean isAuthenticationSecurityEnabled()
    {
        return configuration.getProperty("isAuthenticationSecurityEnabled", true);
    }

    @Override
    public List<String> getCookieDomains()
    {
        List<?> rawValues = this.xwikiCfgConfiguration.getProperty("xwiki.authentication.cookiedomains", List.class,
            List.of());
        return rawValues.stream()
            .map(Object::toString)
            .map(cookie -> StringUtils.startsWith(cookie, COOKIE_PREFIX) ? cookie : COOKIE_PREFIX + cookie)
            .collect(toList());
    }

    @Override
    public String getValidationKey()
    {
        if (this.validationKey == null) {
            this.validationKey = getGeneratedKey("xwiki.authentication.validationKey");
        }

        return this.validationKey;
    }

    @Override
    public String getEncryptionKey()
    {
        if (this.encryptionKey == null) {
            this.encryptionKey = getGeneratedKey("xwiki.authentication.encryptionKey");
        }

        return this.encryptionKey;
    }

    private synchronized String getGeneratedKey(String name)
    {
        // Try xwiki.cfg
        String generatedKey = this.xwikiCfgConfiguration.getProperty(name, String.class);

        // Try the permanent configuration
        if (generatedKey == null) {
            generatedKey = this.permanentConfiguration.getProperty(name, String.class);
        }

        // If still not found, generate one and store it in the permanent configuration
        if (generatedKey == null) {
            generatedKey = RandomStringUtils.random(32);
            try {
                this.permanentConfiguration.setProperty(name, generatedKey);
            } catch (ConfigurationSaveException e) {
                this.logger.error("Failed to store the key, it will be generated at each restart", e);
            }
        }

        return generatedKey;
    }
}
