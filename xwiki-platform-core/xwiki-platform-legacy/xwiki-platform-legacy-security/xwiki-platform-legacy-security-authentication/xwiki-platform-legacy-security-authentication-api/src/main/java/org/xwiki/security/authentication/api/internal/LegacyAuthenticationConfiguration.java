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
package org.xwiki.security.authentication.api.internal;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.security.authentication.api.AuthenticationConfiguration;

/**
 * Default implementation of {@link AuthenticationConfiguration} that relies on
 * {@link org.xwiki.security.authentication.AuthenticationConfiguration}.
 *
 * @version $Id$
 * @since 13.1RC1
 * @deprecated Since 13.1RC1. This component is only provided to allow injecting the deprecated role,
 * but should not be used.
 */
@Deprecated
@Component
@Singleton
public class LegacyAuthenticationConfiguration implements AuthenticationConfiguration
{
    @Inject
    private org.xwiki.security.authentication.AuthenticationConfiguration authenticationConfiguration;

    @Override
    public int getMaxAuthorizedAttempts()
    {
        return this.authenticationConfiguration.getMaxAuthorizedAttempts();
    }

    @Override
    public int getTimeWindow()
    {
        return this.authenticationConfiguration.getTimeWindow();
    }

    @Override
    public String[] getFailureStrategies()
    {
        return this.authenticationConfiguration.getFailureStrategies();
    }

    @Override
    public boolean isAuthenticationSecurityEnabled()
    {
        return this.authenticationConfiguration.isAuthenticationSecurityEnabled();
    }
}
