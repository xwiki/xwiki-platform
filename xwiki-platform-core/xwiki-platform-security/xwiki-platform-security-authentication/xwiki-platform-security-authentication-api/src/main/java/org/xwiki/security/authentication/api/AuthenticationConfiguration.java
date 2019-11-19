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
package org.xwiki.security.authentication.api;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Configuration of the authentication properties.
 *
 * @since 11.6RC1
 * @version $Id$
 */
@Role
@Unstable
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
}
