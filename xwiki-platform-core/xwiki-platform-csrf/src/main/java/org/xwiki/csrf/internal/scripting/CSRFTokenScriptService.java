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
package org.xwiki.csrf.internal.scripting;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.script.service.ScriptService;

/**
 * Script service wrapping a {@link CSRFToken} component.
 * 
 * @version $Id$
 * @since 2.5M2
 */
@Component(roles = ScriptService.class)
@Named("csrf")
@Singleton
public class CSRFTokenScriptService implements CSRFToken, ScriptService
{
    /** Wrapped CSRF token component. */
    @Inject
    private CSRFToken csrf;

    @Override
    public String getToken()
    {
        return this.csrf.getToken();
    }

    @Override
    public void clearToken()
    {
        this.csrf.clearToken();
    }

    @Override
    public boolean isTokenValid(String token)
    {
        return this.csrf.isTokenValid(token);
    }

    @Override
    public String getResubmissionURL()
    {
        return this.csrf.getResubmissionURL();
    }
}
