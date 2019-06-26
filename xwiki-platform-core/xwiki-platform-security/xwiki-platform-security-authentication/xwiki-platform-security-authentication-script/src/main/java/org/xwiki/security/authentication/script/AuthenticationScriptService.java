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
package org.xwiki.security.authentication.script;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authentication.api.AuthenticationFailureManager;
import org.xwiki.security.script.SecurityScriptService;
import org.xwiki.stability.Unstable;

/**
 * Security Authentication Script service.
 *
 * @version $Id$
 * @since 11.6RC1
 */
@Unstable
@Component
@Named(SecurityScriptService.ROLEHINT + '.' + AuthenticationScriptService.ID)
@Singleton
public class AuthenticationScriptService implements ScriptService
{
    /**
     * The role hint of this component.
     */
    public static final String ID = "authentication";

    @Inject
    private AuthenticationFailureManager authenticationFailureManager;

    /**
     * @param username the login used in the request for authentication.
     * @return the aggregated form field to validate for the authentication
     *          (see {@link AuthenticationFailureManager#getForm(String)}).
     */
    public String getForm(String username)
    {
        return this.authenticationFailureManager.getForm(username);
    }

    /**
     * @param username the login used in the request for authentication.
     * @return the aggregated error messages to display for the user
     *          (see {@link AuthenticationFailureManager#getErrorMessage(String)}).
     */
    public String getErrorMessage(String username)
    {
        return this.authenticationFailureManager.getErrorMessage(username);
    }
}
