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

import java.util.logging.Level;

import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Form;
import org.restlet.engine.http.header.HeaderConstants;
import org.restlet.security.ChallengeAuthenticator;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rest.internal.resources.BrowserAuthenticationResource;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.user.api.XWikiUser;

/**
 * <p>
 * The authentication filter is called before serving any request and it is responsible to set in the XWiki context the
 * user that is carrying on the request. It implements the following logic:
 * </p>
 * <ul>
 * <li>If authorization header is present in the HTTP request then it is used to authenticate the user. If the
 * authentication is successful then the user is set in the XWikiContext associated to the request. Otherwise an
 * UNAUTHORIZED response is sent to the client.</li>
 * <li>If no authorization header is present in the HTTP request then:</li>
 * <ul>
 * <li>If session information about a previously authenticated user is present in the request, and it is valid, then
 * that user is assumed carrying out the request.
 * <li>If there is no session information in the request or it is invalid then XWiki.Guest is assumed carrying out the
 * request.</li>
 * </ul>
 * </ul>
 * 
 * @version $Id$
 */
public class XWikiAuthentication extends ChallengeAuthenticator
{
    public XWikiAuthentication(Context context) throws IllegalArgumentException
    {
        super(context, true, ChallengeScheme.CUSTOM, "XWiki");
    }

    @Override
    public boolean authenticate(Request request, Response response)
    {
        /*
         * Browser authentication resource is a special resource that allows to trigger the authentication dialog box in
         * web browsers
         */
        if (request.getResourceRef().getPath().endsWith(BrowserAuthenticationResource.URI_PATTERN)) {
            return super.authenticate(request, response);
        }

        ComponentManager componentManager =
            (ComponentManager) getContext().getAttributes().get(Constants.XWIKI_COMPONENT_MANAGER);
        XWikiContext xwikiContext = Utils.getXWikiContext(componentManager);
        XWiki xwiki = Utils.getXWiki(componentManager);

        DocumentReferenceResolver<String> resolver;
        EntityReferenceSerializer<String> serializer;
        try {
            resolver = componentManager.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");
            serializer = componentManager.getInstance(EntityReferenceSerializer.TYPE_STRING);
        } catch (ComponentLookupException e1) {
            return false;
        }

        /* By default set XWiki.Guest as the user that is sending the request. */
        xwikiContext.setUserReference(null);

        /*
         * After performing the authentication we should add headers to the response to allow applications to verify if
         * the authentication is still valid We are also adding the XWiki version at the same moment.
         */
        Form responseHeaders = (Form) response.getAttributes().get(HeaderConstants.ATTRIBUTE_HEADERS);
        if (responseHeaders == null) {
            responseHeaders = new Form();
            response.getAttributes().put(HeaderConstants.ATTRIBUTE_HEADERS, responseHeaders);
        }
        responseHeaders.add("XWiki-User", serializer.serialize(xwikiContext.getUserReference()));
        responseHeaders.add("XWiki-Version", xwikiContext.getWiki().getVersion());

        // Try with standard XWiki auth
        try {
            XWikiUser xwikiUser = xwiki.checkAuth(xwikiContext);
            if (xwikiUser != null) {
                // Make sure the user is in the context
                xwikiContext.setUserReference(resolver.resolve(xwikiUser.getUser()));

                getLogger().fine(String.format("Authenticated as '%s'.", xwikiUser.getUser()));

                // the user has changed so we need to reset the header
                responseHeaders.set("XWiki-User", serializer.serialize(xwikiContext.getUserReference()));

                return true;
            }
        } catch (XWikiException e) {
            getLogger().log(Level.WARNING, "Exception occurred while authenticating.", e);
        }

        // Falback on restlet auth
        return super.authenticate(request, response);
    }

}
