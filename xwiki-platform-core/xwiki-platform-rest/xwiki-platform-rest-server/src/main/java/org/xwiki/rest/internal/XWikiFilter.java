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

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Header;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.engine.header.HeaderConstants;
import org.restlet.ext.servlet.ServletUtils;
import org.restlet.routing.Filter;
import org.restlet.util.Series;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.xpn.xwiki.XWikiContext;

/**
 * The filter is called before serving any request and it is responsible to set in user in the response header. The user
 * is expected to be authenticate in a preceding filter along with the XWikiContext initialization.
 * 
 * @version $Id$
 * @since 13.4RC1
 */
public class XWikiFilter extends Filter
{
    /**
     * Content-types that are allowed in a
     * <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS#simple_requests">simple request</a> that don't
     * trigger a CORS preflight request in browsers.
     */
    private static final List<String> SIMPLE_CONTENT_TYPES = List.of(
        "application/x-www-form-urlencoded", "multipart/form-data", "text/plain"
    );

    private static final String FORM_TOKEN_HEADER = "XWiki-Form-Token";

    /**
     * Constructor.
     * 
     * @param context The context.
     */
    public XWikiFilter(Context context) throws IllegalArgumentException
    {
        super(context);
    }

    @Override
    protected int beforeHandle(Request request, Response response)
    {
        ComponentManager componentManager =
            (ComponentManager) getContext().getAttributes().get(Constants.XWIKI_COMPONENT_MANAGER);
        XWikiContext xwikiContext = Utils.getXWikiContext(componentManager);
        CSRFToken csrfToken = null;

        try {
            csrfToken = componentManager.getInstance(CSRFToken.class);
        } catch (ComponentLookupException e) {
            getLogger().warning("Failed to lookup CSRF token validator: " + ExceptionUtils.getRootCauseMessage(e));
        }

        try {
            EntityReferenceSerializer<String> serializer =
                componentManager.getInstance(EntityReferenceSerializer.TYPE_STRING);

            /*
             * We add headers to the response to allow applications to verify if the authentication is still valid. We
             * are also adding the XWiki version at the same moment.
             */
            Series<Header> responseHeaders =
                (Series<Header>) response.getAttributes().get(HeaderConstants.ATTRIBUTE_HEADERS);
            if (responseHeaders == null) {
                responseHeaders = new Series<>(Header.class);
                response.getAttributes().put(HeaderConstants.ATTRIBUTE_HEADERS, responseHeaders);
            }
            responseHeaders.add("XWiki-User", serializer.serialize(xwikiContext.getUserReference()));
            responseHeaders.add("XWiki-Version", xwikiContext.getWiki().getVersion());

            if (csrfToken != null) {
                responseHeaders.add(FORM_TOKEN_HEADER, csrfToken.getToken());
            }
        } catch (ComponentLookupException e) {
            getLogger()
                .warning("Failed to lookup the entity reference serializer: " + ExceptionUtils.getRootCauseMessage(e));
        }

        int result = CONTINUE;

        HttpServletRequest servletRequest = ServletUtils.getRequest(Request.getCurrent());

        // Require a CSRF token for requests that browsers allow through HTML forms and across origins.
        // See https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS for more information.
        // Compare to the method from the servlet request to avoid the automatic conversion from POST to PUT request.
        // Check for a prefix match to make sure it matches regardless of the supplied parameters (like charset).
        if ("POST".equals(servletRequest.getMethod()) && SIMPLE_CONTENT_TYPES.stream().anyMatch(expectedType ->
            StringUtils.startsWith(StringUtils.lowerCase(servletRequest.getContentType()), expectedType)))
        {
            Series<Header> requestHeaders = request.getHeaders();
            String formToken = requestHeaders.getFirstValue(FORM_TOKEN_HEADER, true);

            // Skip the main request handler but allow cleanup if either the CSRF validator failed or the token is
            // invalid.
            if (csrfToken == null) {
                response.setStatus(Status.SERVER_ERROR_INTERNAL);
                response.setEntity("Failed to lookup the CSRF token validator.", MediaType.TEXT_PLAIN);
                result = SKIP;
            } else if (!csrfToken.isTokenValid(formToken)) {
                response.setStatus(Status.CLIENT_ERROR_FORBIDDEN);
                response.setEntity("Invalid or missing form token.", MediaType.TEXT_PLAIN);
                result = SKIP;
            }
        }

        return result;
    }
}
