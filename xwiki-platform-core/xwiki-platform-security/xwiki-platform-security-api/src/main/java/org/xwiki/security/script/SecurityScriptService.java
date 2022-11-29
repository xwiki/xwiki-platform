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
import org.xwiki.script.service.ScriptServiceManager;
import org.xwiki.security.SecurityConfiguration;

/**
 * Entry point for all security related script services and for the generic security script APIs.
 *
 * @version $Id$
 * @since 6.1RC1
 */
@Component
@Named(SecurityScriptService.ROLEHINT)
@Singleton
public class SecurityScriptService implements ScriptService
{
    /**
     * The role hint of this component.
     */
    public static final String ROLEHINT = "security";

    @Inject
    private ScriptServiceManager scriptServiceManager;

    @Inject
    private SecurityConfiguration securityConfiguration;

    /**
     * @param <S> the type of the {@link ScriptService}
     * @param serviceName the name of the sub {@link ScriptService}
     * @return the {@link ScriptService} or null of none could be found
     */
    @SuppressWarnings("unchecked")
    public <S extends ScriptService> S get(String serviceName)
    {
        return (S) this.scriptServiceManager.get("security." + serviceName);
    }

    /**
     * Get the number used to control how many items are retrieved through queries (for example inside Velocity
     * templates). This limit can be customized in the {@code xwiki.properties} file in order to allow retrieving
     * more or less items. Default value is {@code 100} (this number corresponds to the LiveTable/LiveData max items
     * view limit). This is to avoid DOS attacks.
     *
     * @return the query items limit number.
     * @since 13.10RC1
     */
    public int getQueryItemsLimit()
    {
        return this.securityConfiguration.getQueryItemsLimit();
    }
}
