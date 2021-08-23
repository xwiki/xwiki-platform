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
package org.xwiki.security.query.script;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.query.QueryConfiguration;
import org.xwiki.security.script.SecurityScriptService;

/**
 * Security Query Script Service.
 *
 * @version $Id$
 * @since 13.7
 */
@Component
@Named(SecurityScriptService.ROLEHINT + '.' + SecurityQueryScriptService.ID)
@Singleton
public class SecurityQueryScriptService implements ScriptService
{
    /**
     * The role hint of this component.
     */
    public static final String ID = "query";

    @Inject
    private QueryConfiguration queryConfiguration;

    /**
     * Get the maximum size limit for a query to retrieve items.
     *
     * @return {@code DefaultQueryConfiguration.MAX_LIMIT}
     */
    public int getLimit()
    {
        return queryConfiguration.getLimit();
    }
}

