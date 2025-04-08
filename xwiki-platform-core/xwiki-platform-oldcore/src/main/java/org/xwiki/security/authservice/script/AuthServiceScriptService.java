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
package org.xwiki.security.authservice.script;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.DocumentAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authservice.XWikiAuthServiceComponent;
import org.xwiki.security.authservice.internal.AuthServiceConfiguration;
import org.xwiki.security.authservice.internal.AuthServiceManager;
import org.xwiki.security.script.SecurityScriptService;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiAuthService;

/**
 * The script service used to manipulate the registered {@link XWikiAuthService} instances.
 *
 * @version $Id$
 * @since 15.3RC1
 */
@Component
@Named(SecurityScriptService.ROLEHINT + '.' + AuthServiceScriptService.ID)
@Singleton
public class AuthServiceScriptService implements ScriptService
{
    /**
     * The role hint of this component.
     */
    public static final String ID = "authService";

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    @Named("xwikicfg")
    private ConfigurationSource xwikicfg;

    @Inject
    private AuthServiceConfiguration configuration;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private DocumentAuthorizationManager authorization;

    @Inject
    private AuthServiceManager authServices;

    private void checkWikiAdmin() throws AccessDeniedException
    {
        XWikiContext xcontext = this.contextProvider.get();

        XWikiDocument sdoc = xcontext.getSecureDocument();
        DocumentReference contextDocumentReference = sdoc != null ? sdoc.getDocumentReference() : null;

        // Make sure current author has wiki admin right to use this API
        this.authorization.checkAccess(Right.ADMIN, EntityType.WIKI, xcontext.getAuthorReference(),
            contextDocumentReference);
    }

    /**
     * Get the {@link XWikiAuthService} according to {@link XWiki#getAuthService()}.
     * 
     * @return the main {@link XWikiAuthService}
     * @throws AccessDeniedException when the current author is not authorized to use this API
     */
    public XWikiAuthService getAuthService() throws AccessDeniedException
    {
        checkWikiAdmin();

        XWikiContext xcontext = this.contextProvider.get();

        return xcontext.getWiki().getAuthService();
    }

    /**
     * @return the authentication service class indicated in xwiki.cfg
     * @throws AccessDeniedException when the current author is not authorized to use this API
     */
    public String getConfiguredAuthClass() throws AccessDeniedException
    {
        checkWikiAdmin();

        return this.xwikicfg.getProperty("xwiki.authentication.authclass");
    }

    /**
     * @return true if the auth service is a component, false when it's based on the old xwiki.cfg authClass property or
     *         if it's directly injected through {@link XWiki#setAuthService(Class)}
     */
    public boolean isAuthServiceComponent()
    {
        XWikiContext xcontext = this.contextProvider.get();

        return xcontext.getWiki().isAuthServiceComponent();
    }

    /**
     * Get all the available component based authentication services.
     * 
     * @return the available authentication services
     * @throws AccessDeniedException when the current author is not authorized to use this API
     * @throws ComponentLookupException when failing to looking the authentication services
     */
    public List<XWikiAuthServiceComponent> getAuthServices() throws AccessDeniedException, ComponentLookupException
    {
        checkWikiAdmin();

        return this.authServices.getAuthServices();
    }

    /**
     * Set the authentication service in the wiki configuration.
     * 
     * @param id the identifier of the authentication service
     * @throws AccessDeniedException when the current author is not authorized to use this API
     * @throws XWikiException when failing to get the authentication service
     */
    public void setAuthService(String id) throws AccessDeniedException, XWikiException
    {
        checkWikiAdmin();

        this.configuration.setAuthServiceId(id);
    }
}
