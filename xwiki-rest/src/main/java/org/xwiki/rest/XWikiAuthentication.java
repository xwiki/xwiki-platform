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
import org.restlet.Guard;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Request;
import org.xwiki.rest.resources.BrowserAuthenticationResource;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.user.api.XWikiUser;

public class XWikiAuthentication extends Guard
{
    public XWikiAuthentication(Context context) throws IllegalArgumentException
    {
        super(context, ChallengeScheme.HTTP_BASIC, "XWiki");
    }

    @Override
    public int authenticate(Request request)
    {
        /* This is a special case that is used to send back a challenge to popup the authentication dialog in browsers */
        if (request.getResourceRef().getPath().endsWith(BrowserAuthenticationResource.URI_PATTERN)) {
            return super.authenticate(request);
        }

        /*
         * Try to authenticate using information in the request (authorization header or a URI in the form of
         * http://user:password@host/..., etc.)
         */
        if (super.authenticate(request) == 1) {
            /* If it's succesfull then the context has ben set with the authenticated user */
            return 1;
        }

        /* If authentication failed then no information is present in headers or URI, try to check cookies */
        XWikiContext xwikiContext = (XWikiContext) getContext().getAttributes().get(Constants.XWIKI_CONTEXT);
        XWiki xwiki = (XWiki) getContext().getAttributes().get(Constants.XWIKI);

        try {
            XWikiUser xwikiUser = xwiki.getAuthService().checkAuth(xwikiContext);
            if (xwikiUser != null) {
                xwikiContext.setUser(xwikiUser.getUser());
                getLogger().log(Level.FINE, String.format("Authenticated as '%s'.", xwikiUser.getUser()));

                getContext().getAttributes().put(Constants.XWIKI_USER, xwikiUser.getUser());
                
                return 1;
            }
        } catch (XWikiException e) {
            getLogger().log(Level.WARNING, "Exception occurred while authenticating.", e);
        }

        /*
         * If we are here all the previous authentication methods failed, so guest will be the user set for the context
         * of this request.
         */
        String xwikiUser = "XWiki.XWikiGuest";
        xwikiContext.setUser(xwikiUser);
        getContext().getAttributes().put(Constants.XWIKI_USER, xwikiUser);
        
        return 1;
    }

    @Override
    public boolean checkSecret(Request request, String identifier, char[] secret)
    {
        XWikiContext xwikiContext = (XWikiContext) getContext().getAttributes().get(Constants.XWIKI_CONTEXT);
        XWiki xwiki = (XWiki) getContext().getAttributes().get(Constants.XWIKI);

        try {
            Principal principal = xwiki.getAuthService().authenticate(identifier, new String(secret), xwikiContext);
            if (principal != null) {
                String xwikiUser = principal.getName();

                xwikiContext.setUser(xwikiUser);
                getLogger().log(Level.FINE, String.format("Authenticated as '%s'.", identifier));

                getContext().getAttributes().put(Constants.XWIKI_USER, xwikiUser);

                return true;
            }
        } catch (XWikiException e) {
            getLogger().log(Level.WARNING, "Exception occurred while authenticating.", e);
        }

        getLogger().log(Level.WARNING, String.format("Cannot authenticate '%s'.", identifier));

        return false;
    }

}
