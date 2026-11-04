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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSaveException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;
import org.xwiki.security.authentication.AuthenticationConfiguration;

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
    /**
     * The example values of the keys documented in xwiki.cfg.vm, which must not be used in production.
     */
    private static final Set<String> EXAMPLE_KEYS =
        Set.of("totototototototototototototototo", "titititititititititititititititi");

    /**
     * The prefix of the environment variables overwriting an xwiki.cfg property (the dots of the property name being
     * replaced by underscores), as supported by the {@code xwikicfg} configuration source.
     */
    private static final String XCONF_ENV_PREFIX = "XCONF_xwikicfg_";

    /**
     * The prefix of the Java system properties overwriting an xwiki.cfg property, as supported by the {@code xwikicfg}
     * configuration source.
     */
    private static final String XCONF_SYSTEM_PROPERTY_PREFIX = "xconf.xwikicfg.";

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
    private RemoteObservationManagerConfiguration remoteObservationConfiguration;

    @Inject
    private Logger logger;

    private String validationKey;

    private String encryptionKey;

    /**
     * The names of the keys for which the clustering misconfiguration has already been reported, so that the error is
     * logged only once per key.
     */
    private final Set<String> reportedClusteringKeys = ConcurrentHashMap.newKeySet();

    /**
     * The names of the keys for which the use of an example value has already been reported, so that the warning is
     * logged only once per key.
     */
    private final Set<String> reportedExampleKeys = ConcurrentHashMap.newKeySet();

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
        return this.xwikiCfgConfiguration.getProperty("xwiki.authentication.cookiedomains", List.class,
            List.<String>of());
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
        // Try xwiki.cfg (a blank value is considered as not set)
        String key = this.xwikiCfgConfiguration.getProperty(name, String.class);

        if (StringUtils.isBlank(key)) {
            // Try the permanent configuration
            key = this.permanentConfiguration.getProperty(name, String.class);

            // If still not found, generate one and store it in the permanent configuration
            if (StringUtils.isBlank(key)) {
                key = RandomStringUtils.secure().next(32);
                try {
                    this.permanentConfiguration.setProperty(name, key);
                } catch (ConfigurationSaveException e) {
                    this.logger.error("Failed to store the key, it will be generated at each restart", e);
                }
            }

            // A key which is not shared through xwiki.cfg is a problem when several instances are clustered
            checkClustering(name);
        }

        checkExampleValue(name, key);

        return key;
    }

    private void checkClustering(String name)
    {
        if (this.remoteObservationConfiguration.isEnabled() && this.reportedClusteringKeys.add(name)) {
            this.logger.error("The authentication key [{}] is not configured but clustering is enabled "
                + "(observation.remote.enabled=true). It comes from configuration.properties in the permanent "
                + "directory (randomly generated at this node's first start unless it was put there), so it differs "
                + "between nodes that do not share that file: users are logged out whenever the load balancer "
                + "switches node. Set the same value on all nodes, in xwiki.cfg or, for example from a secret store, "
                + "with the environment variable [{}] or the Java system property [{}].", name,
                XCONF_ENV_PREFIX + name.replace('.', '_'), XCONF_SYSTEM_PROPERTY_PREFIX + name);
        }
    }

    private void checkExampleValue(String name, String key)
    {
        if (EXAMPLE_KEYS.contains(key) && this.reportedExampleKeys.add(name)) {
            this.logger.warn("The authentication key [{}] is set to one of the example values documented in "
                + "xwiki.cfg. Such a well-known key weakens the protection of the authentication cookies: set a "
                + "random value instead.", name);
        }
    }
}
