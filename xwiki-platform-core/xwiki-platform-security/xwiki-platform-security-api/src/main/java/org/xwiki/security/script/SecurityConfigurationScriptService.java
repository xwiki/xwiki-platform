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
package org.xwiki.security.script;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.SecurityConfiguration;
import org.xwiki.stability.Unstable;

/**
 * Security Configuration Script Service.
 *
 * @version $Id$
 * @since 13.8RC1
 */
@Component
@Named("security.config")
@Singleton
@Unstable
public class SecurityConfigurationScriptService implements ScriptService
{
    @Inject
    private SecurityConfiguration securityConfiguration;

    /**
     * Get the query items limit in order to limit how many items are retrieved for example inside Velocity templates
     * through queries. This limit can be customized in xwiki.properties file in order to allow retrieving more items.
     * Default value is 100, the LiveTable items view limit.
     *
     * @return the query items limit number.
     */
    public int getQueryItemsLimit()
    {
        return this.securityConfiguration.getQueryItemsLimit();
    }
}

