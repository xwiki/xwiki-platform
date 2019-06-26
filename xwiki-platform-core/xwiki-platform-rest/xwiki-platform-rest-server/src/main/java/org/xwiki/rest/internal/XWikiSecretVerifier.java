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
package org.xwiki.rest.internal;

import java.io.IOException;
import java.security.Principal;
import java.util.logging.Level;

import javax.servlet.http.HttpServletResponse;

import org.restlet.Context;
import org.restlet.security.SecretVerifier;
import org.securityfilter.filter.SecurityRequestWrapper;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.security.authentication.api.AuthenticationFailureManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.web.XWikiResponse;

/**
 * Secret verifier that authenticates the user against XWiki authentication system.
 * 
 * @version $Id$
 * @since 3.2M1
 */
public class XWikiSecretVerifier extends SecretVerifier
{

    /**
     * XWiki component manager. Used to access legacy XWiki context.
     */
    private ComponentManager componentManager;

    /**
     * Restlet context, used to access Restlet logger.
     */
    private Context context;

    /**
     * Constructor.
     *
     * @param context the Restlet context in which to verify the secrect
     * @param manager XWiki's component manager
     */
    public XWikiSecretVerifier(Context context, ComponentManager manager)
    {
        this.context = context;
        this.componentManager = manager;
    }

    @Override
    public int verify(String identifier, char[] secret) throws IllegalArgumentException
    {
        XWikiContext xwikiContext = Utils.getXWikiContext(this.componentManager);
        XWiki xwiki = Utils.getXWiki(this.componentManager);
        SecurityRequestWrapper securityRequestWrapper =
            new SecurityRequestWrapper(xwikiContext.getRequest(), null, null, "CHALLENGE");

        try {
            AuthenticationFailureManager authenticationFailureManager =
                this.componentManager.getInstance(AuthenticationFailureManager.class);
            Principal principal = (secret == null) ? null : xwiki.getAuthService().authenticate(identifier,
                    new String(secret), xwikiContext);
            if (principal != null && authenticationFailureManager.validateForm(identifier, securityRequestWrapper)) {
                authenticationFailureManager.resetAuthenticationFailureCounter(identifier);
                String xwikiUser = principal.getName();

                xwikiContext.setUser(xwikiUser);

                this.context.getLogger().log(Level.FINE, String.format("Authenticated as '%s'.", identifier));

                return RESULT_VALID;
            } else {
                authenticationFailureManager.recordAuthenticationFailure(identifier);
                if (principal != null) {
                    String responseMessage = "Your account may be blocked after too many attempts to login. \n"
                                                + "Please go to the xwiki login page to get more information.";
                    XWikiResponse response = xwikiContext.getResponse();
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, responseMessage);
                }
            }
        } catch (XWikiException | ComponentLookupException | IOException e) {
            this.context.getLogger().log(Level.WARNING, "Exception occurred while authenticating.", e);
        }

        this.context.getLogger().log(Level.WARNING, String.format("Cannot authenticate '%s'.", identifier));

        return RESULT_INVALID;
    }

}
