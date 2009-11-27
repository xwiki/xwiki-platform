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

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.restlet.Filter;
import org.restlet.data.Request;
import org.restlet.data.Response;

import com.noelios.restlet.ext.servlet.ServletCall;
import com.noelios.restlet.http.HttpCall;
import com.noelios.restlet.http.HttpRequest;

/**
 * The Setup cleanup filter is used to initialize the XWiki context before serving the request, and to clean it up after
 * the request has been served. This filter also populates the Restlet context with XWiki-related variables so that they
 * are available to resources.
 * 
 * @version $Id$
 */
public class XWikiSetupCleanupFilter extends Filter
{
    @Override
    protected int beforeHandle(Request request, Response response)
    {
        /*
         * We put the original HTTP request in context attributes because this is needed for reading
         * application/www-form-urlencoded POSTs. In fact servlet filters might use getParameters which invalidates the
         * request body, making Restlet unable to process it. In the case we need to use getParameters as well instead
         * of reading from the input stream, and for doing this we need the HTTP request object. This is basically a
         * hack that should be removed as soon as the Restlet JAX-RS extension will support the injection of the request
         * object via the @Context annotation
         */
        getContext().getAttributes().put(Constants.HTTP_REQUEST, getHttpRequest(request));

        return Filter.CONTINUE;
    }

    @Override
    protected void afterHandle(Request request, Response response)
    {
        Map<String, Object> attributes = getContext().getAttributes();
        attributes.remove(Constants.RELEASABLE_COMPONENT_REFERENCES);
        attributes.remove(Constants.HTTP_REQUEST);

        /* Avoid that empty entities make the engine forward the response creation to the XWiki servlet. */
        if (response.getEntity() != null) {
            if (!response.getEntity().isAvailable()) {
                response.setEntity(null);
            }
        }
    }

    /**
     * Builds the servlet request.
     * 
     * @param req The request to handle.
     * @return httpServletRequest The http servlet request.
     */
    protected static HttpServletRequest getHttpRequest(Request req)
    {
        if (req instanceof HttpRequest) {
            HttpRequest httpRequest = (HttpRequest) req;
            HttpCall httpCall = httpRequest.getHttpCall();
            if (httpCall instanceof ServletCall) {
                return ((ServletCall) httpCall).getRequest();
            }
        }
        return null;
    }

}
