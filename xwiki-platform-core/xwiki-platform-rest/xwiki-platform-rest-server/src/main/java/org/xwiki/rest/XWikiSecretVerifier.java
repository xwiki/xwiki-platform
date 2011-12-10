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
package org.xwiki.rest;

import java.security.Principal;
import java.util.logging.Level;

import org.restlet.Context;
import org.restlet.security.SecretVerifier;
import org.xwiki.component.manager.ComponentManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

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
    public boolean verify(String identifier, char[] secret) throws IllegalArgumentException
    {
        XWikiContext xwikiContext = Utils.getXWikiContext(this.componentManager);
        XWiki xwiki = Utils.getXWiki(this.componentManager);

        try {
            Principal principal = xwiki.getAuthService().authenticate(identifier, new String(secret), xwikiContext);
            if (principal != null) {
                String xwikiUser = principal.getName();

                xwikiContext.setUser(xwikiUser);

                this.context.getLogger().log(Level.FINE, String.format("Authenticated as '%s'.", identifier));

                return true;
            }
        } catch (XWikiException e) {
            this.context.getLogger().log(Level.WARNING, "Exception occurred while authenticating.", e);
        }

        this.context.getLogger().log(Level.WARNING, String.format("Cannot authenticate '%s'.", identifier));

        return false;
    }

}
