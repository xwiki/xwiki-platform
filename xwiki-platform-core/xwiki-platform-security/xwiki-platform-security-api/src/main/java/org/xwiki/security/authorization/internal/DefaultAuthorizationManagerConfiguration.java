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
package org.xwiki.security.authorization.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.security.authorization.AuthorizationManagerConfiguration;
import org.xwiki.security.internal.AbstractSecurityConfiguration;

/**
 * Configuration for the {@link org.xwiki.security.authorization.AuthorizationManager}.
 *
 * @version $Id$
 * @since 4.0M2
 */
@Component
@Singleton
public class DefaultAuthorizationManagerConfiguration extends AbstractSecurityConfiguration
    implements AuthorizationManagerConfiguration
{
    /** Prefix for right resolver configuration keys. */
    private static final String AUTHORIZATION = AbstractSecurityConfiguration.SECURITY + ".authorization";

    /** Prefix for right resolver configuration keys. */
    private static final String SETTLER = AUTHORIZATION + ".settler";

    /** Default hint for component manager. */
    private static final String DEFAULT_SETTLER = "default";

    /** Obtain configuration from the xwiki.properties file. */
    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configuration;

    @Override
    public String getAuthorizationSettler()
    {
        return configuration.getProperty(SETTLER, DEFAULT_SETTLER);
    }
}
