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
package org.xwiki.security.authservice.internal;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.security.authservice.XWikiAuthServiceComponent;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.user.api.XWikiAuthService;

/**
 * Get the configured authenticator in the current context.
 * 
 * @version $Id$
 * @since 15.3RC1
 */
@Component(roles = AuthServiceManager.class)
@Singleton
public class AuthServiceManager
{
    @Inject
    private AuthServiceConfiguration configuration;

    @Inject
    @Named(StandardXWikiAuthServiceComponent.ID)
    private XWikiAuthServiceComponent standardAuthenticator;

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    private Logger logger;

    /**
     * @return the XWikiAuthService in the current context
     * @throws ComponentLookupException when failing to get the current auth service
     * @throws XWikiException when failing to get the current auth service configuration
     */
    public XWikiAuthService getAuthService() throws ComponentLookupException, XWikiException
    {
        // Get the configured authenticator
        String authHint = this.configuration.getAuthService();

        // Resolve the corresponding authenticator
        if (authHint != null) {
            ComponentManager componentManager = this.componentManagerProvider.get();

            if (componentManager.hasComponent(XWikiAuthServiceComponent.class, authHint)) {
                return componentManager.getInstance(XWikiAuthServiceComponent.class, authHint);
            } else {
                this.logger.warn("No authentication service could be found for identifier [{}]. "
                    + "Fallbacking on the standard one.", authHint);
            }
        }

        // Fallback on the standard authenticator
        return this.standardAuthenticator;
    }

    /**
     * @return the auth services available in the current context
     * @throws ComponentLookupException when failing to get the current auth services
     */
    public List<XWikiAuthServiceComponent> getAuthServices() throws ComponentLookupException
    {
        return this.componentManagerProvider.get().getInstanceList(XWikiAuthServiceComponent.class);
    }
}
