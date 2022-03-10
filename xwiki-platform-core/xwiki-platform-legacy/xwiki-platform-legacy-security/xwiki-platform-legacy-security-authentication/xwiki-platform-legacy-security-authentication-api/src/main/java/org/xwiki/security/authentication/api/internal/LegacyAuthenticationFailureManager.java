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
import javax.servlet.http.HttpServletRequest;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authentication.api.AuthenticationFailureManager;

/**
 * Default implementation of {@link AuthenticationFailureManager} that relies on
 * {@link org.xwiki.security.authentication.AuthenticationFailureManager}.
 *
 * @version $Id$
 * @since 13.1RC1
 * @deprecated Since 13.1RC1. This component is only provided to allow injecting the deprecated role,
 * but should not be used.
 */
@Deprecated
@Component
@Singleton
public class LegacyAuthenticationFailureManager implements AuthenticationFailureManager
{
    @Inject
    private org.xwiki.security.authentication.AuthenticationFailureManager authenticationFailureManager;

    @Override
    public boolean recordAuthenticationFailure(String username, HttpServletRequest request)
    {
        return this.authenticationFailureManager.recordAuthenticationFailure(username, request);
    }

    @Override
    public void resetAuthenticationFailureCounter(String username)
    {
        this.authenticationFailureManager.resetAuthenticationFailureCounter(username);
    }

    @Override
    public String getForm(String username, HttpServletRequest request)
    {
        return this.authenticationFailureManager.getForm(username, request);
    }

    @Override
    public boolean validateForm(String username, HttpServletRequest request)
    {
        return this.authenticationFailureManager.validateForm(username, request);
    }

    @Override
    public String getErrorMessage(String username)
    {
        return this.authenticationFailureManager.getErrorMessage(username);
    }

    @Override
    public DocumentReference findUser(String username)
    {
        return this.authenticationFailureManager.findUser(username);
    }

    @Override
    public void resetAuthenticationFailureCounter(DocumentReference user)
    {
        this.authenticationFailureManager.resetAuthenticationFailureCounter(user);
    }
}
