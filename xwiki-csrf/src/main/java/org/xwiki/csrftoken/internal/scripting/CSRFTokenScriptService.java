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
package org.xwiki.csrftoken.internal.scripting;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.csrftoken.CSRFToken;
import org.xwiki.script.service.ScriptService;

/**
 * Script service wrapping a {@link CSRFToken} component.
 * 
 * @version $Id$
 * @since 2.5M2
 */
@Component(roles = ScriptService.class, hints = "csrf")
public class CSRFTokenScriptService extends AbstractLogEnabled implements CSRFToken, ScriptService
{
    /** Wrapped CSRF token component. */
    @Requirement
    private CSRFToken csrf;

    /**
     * {@inheritDoc}
     * 
     * @see CSRFToken#isTokenValid(String)
     */
    public String getToken()
    {
        return this.csrf.getToken();
    }

    /**
     * {@inheritDoc}
     * 
     * @see CSRFToken#clearToken()
     */
    public void clearToken()
    {
        this.csrf.clearToken();
    }

    /**
     * {@inheritDoc}
     * 
     * @see CSRFToken#isTokenValid(String)
     */
    public boolean isTokenValid(String token)
    {
        return this.csrf.isTokenValid(token);
    }

    /**
     * {@inheritDoc}
     * 
     * @see CSRFToken#getResubmissionURL()
     */
    public String getResubmissionURL()
    {
        return this.csrf.getResubmissionURL();
    }
}
