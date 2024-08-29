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
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.security.authorization.AuthorizationManagerConfiguration;
import org.xwiki.security.authorization.AuthorizationSettler;

/**
 * Provide the configured authorization settler.
 *
 * @version $Id$
 * @since 4.0M2 
 */
@Component
@Singleton
public class AuthorizationSettlerProvider implements Provider<AuthorizationSettler>, Initializable
{
    /** Authorisation manager configuration to retrieve the settler hint. */
    @Inject
    private AuthorizationManagerConfiguration authorizationManagerConfiguration;

    /** Component manager to lookup the appropriate settler. */
    @Inject
    private ComponentManager componentManager;

    /** Settle the access rights. */
    private AuthorizationSettler authorizationSettler;

    @Override
    public void initialize() throws InitializationException
    {
        String settlerHint = authorizationManagerConfiguration.getAuthorizationSettler();
        try {
            authorizationSettler = componentManager.getInstance(AuthorizationSettler.class, settlerHint);
        } catch (ComponentLookupException e) {
            throw new InitializationException(
                String.format("Unable to lookup the authorization settler hinted [%s].", settlerHint), e);
        }
    }

    @Override
    public AuthorizationSettler get()
    {
        return authorizationSettler;
    }
}
