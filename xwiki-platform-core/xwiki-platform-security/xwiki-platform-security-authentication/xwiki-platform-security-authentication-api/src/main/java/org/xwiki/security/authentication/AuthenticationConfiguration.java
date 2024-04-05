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
package org.xwiki.security.authentication;

import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Configuration of the authentication properties.
 *
 * @since 13.1RC1
 * @version $Id$
 */
@Role
public interface AuthenticationConfiguration
{
    /**
     * @return the number of authorized authentication failure before the strategies are activated.
     */
    int getMaxAuthorizedAttempts();

    /**
     * @return the time window in seconds during which the authentication failures should occur to activate
     *         the failure strategy.
     */
    int getTimeWindow();

    /**
     * @return the names of the {@link AuthenticationFailureStrategy} to activate, each name is a strategy hint.
     */
    String[] getFailureStrategies();

    /**
     * @return {@code true} if the authentication security mechanism is enabled.
     * @since 11.10
     */
    default boolean isAuthenticationSecurityEnabled()
    {
        return true;
    }

    /**
     * @return the list of cookie domains to use for the authentication cookies. Domains are prefix with a dot.
     * @since 14.10.15
     * @since 15.5.1
     * @since 15.6
     */
    @Unstable
    default List<String> getCookieDomains()
    {
        return List.of();
    }

    /**
     * Get from the configuration or generate a Validation Key used to generate hash values for the the cookies.
     * 
     * @return the Validation Key
     * @since 15.9
     * @since 15.5.4
     * @since 14.10.19
     */
    default String getValidationKey()
    {
        return RandomStringUtils.random(32);
    }

    /**
     * Get from the configuration of generate Encryption Key used to create a secret key used to encrypt/decrypt value
     * for the cookies.
     * 
     * @return the Encryption Key
     * @since 15.9
     * @since 15.5.4
     * @since 14.10.19
     */
    default String getEncryptionKey()
    {
        return RandomStringUtils.random(32);
    }
}
