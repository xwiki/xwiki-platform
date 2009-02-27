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

import java.util.logging.Level;

import org.restlet.Context;
import org.restlet.Guard;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Form;
import org.restlet.data.Request;
import org.xwiki.rest.resources.BrowserAuthenticationResource;

import com.noelios.restlet.http.HttpConstants;
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
        if (request.getResourceRef().getPath().endsWith(BrowserAuthenticationResource.URI_PATTERN)) {
            return super.authenticate(request);
        }

        XWikiContext xwikiContext = (XWikiContext) getContext().getAttributes().get(Constants.XWIKI_CONTEXT);
        XWiki xwiki = (XWiki) getContext().getAttributes().get(Constants.XWIKI);

        Form headers = (Form) request.getAttributes().get(HttpConstants.ATTRIBUTE_HEADERS);

        if (headers.getValues(HttpConstants.HEADER_AUTHORIZATION) == null) {
            /*
             * If there isn't an authorization header, check if the context contains an already authenticated session.
             * If it's the case use the previously authenticated user.
             */
            try {
                XWikiUser xwikiUser = xwiki.getAuthService().checkAuth(xwikiContext);
                if (xwikiUser != null) {
                    xwikiContext.setUser(xwikiUser.getUser());
                    getLogger().log(Level.FINE, String.format("Authenticated as '%s'.", xwikiUser.getUser()));

                    getContext().getAttributes().put(Constants.XWIKI_USER, xwikiUser.getUser());
                }
            } catch (XWikiException e) {
                getLogger().log(Level.WARNING, "Exception occurred while authenticating.", e);
            }

            return 1;
        }

        return super.authenticate(request);
    }

    @Override
    public boolean checkSecret(Request request, String identifier, char[] secret)
    {
        XWikiContext xwikiContext = (XWikiContext) getContext().getAttributes().get(Constants.XWIKI_CONTEXT);
        XWiki xwiki = (XWiki) getContext().getAttributes().get(Constants.XWIKI);

        try {
            if (xwiki.getAuthService().authenticate(identifier, new String(secret), xwikiContext) != null) {
                String xwikiUser = String.format("XWiki.%s", identifier);

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
