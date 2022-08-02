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
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.security.authentication.api.AuthenticationFailureStrategy;

/**
 * Default implementation of {@link AuthenticationFailureStrategy} that relies on
 * {@link org.xwiki.security.authentication.AuthenticationFailureStrategy}.
 *
 * @version $Id$
 * @since 13.1RC1
 * @deprecated Since 13.1RC1. This component is only provided to allow injecting the deprecated role,
 * but should not be used.
 */
@Deprecated
@Component(hints = {"captcha", "disableAccount"})
@Singleton
public class LegacyAuthenticationFailureStrategy implements AuthenticationFailureStrategy, Initializable
{
    @Inject
    private ComponentDescriptor<AuthenticationFailureStrategy> componentDescriptor;

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    private org.xwiki.security.authentication.AuthenticationFailureStrategy failureStrategy;

    @Override
    public void initialize() throws InitializationException
    {
        String roleHint = this.componentDescriptor.getRoleHint();
        try {
            this.failureStrategy = this.componentManager.getInstance(
                org.xwiki.security.authentication.AuthenticationFailureStrategy.class, roleHint);
        } catch (ComponentLookupException e) {
            throw new InitializationException(
                String.format("Error while trying to retrieve the AuthenticationFailureStrategy with hint [%s].",
                    roleHint), e);
        }
    }

    @Override
    public String getErrorMessage(String username)
    {
        return this.failureStrategy.getErrorMessage(username);
    }

    @Override
    public String getForm(String username)
    {
        return this.failureStrategy.getForm(username);
    }

    @Override
    public boolean validateForm(String username, HttpServletRequest request)
    {
        return this.failureStrategy.validateForm(username, request);
    }

    @Override
    public void notify(String username)
    {
        this.failureStrategy.notify(username);
    }


}
