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
package org.xwiki.security.query.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.security.internal.AbstractSecurityConfiguration;
import org.xwiki.security.query.QueryConfiguration;

/**
 * Configuration for {@link QueryConfiguration}.
 *
 * @version $Id$
 * @since 13.7RC1
 */
@Component
@Singleton
public class DefaultQueryConfiguration extends AbstractSecurityConfiguration
    implements QueryConfiguration
{
    /** Prefix for query limit configuration keys. */
    private static final String QUERY = AbstractSecurityConfiguration.SECURITY + ".query";

    /** Prefix for query limit configuration keys. */
    private static final String MAX_LIMIT = QUERY + ".maxlimit";

    /** Default hint for component manager. */
    private static final int DEFAULT_MAX_LIMIT = 100;

    /** Obtain configuration from the xwiki.properties file. */
    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configuration;

    @Override
    public int getLimit()
    {
        return configuration.getProperty(MAX_LIMIT, DEFAULT_MAX_LIMIT);
    }
}
