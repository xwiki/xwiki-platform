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
import java.util.logging.Level;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.restlet.Filter;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletContainerException;
import org.xwiki.container.servlet.ServletContainerInitializer;
import org.xwiki.context.Execution;

import com.noelios.restlet.ext.servlet.ServletCall;
import com.noelios.restlet.ext.servlet.ServletContextAdapter;
import com.noelios.restlet.http.HttpCall;
import com.noelios.restlet.http.HttpRequest;
import com.noelios.restlet.http.HttpResponse;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;
import com.xpn.xwiki.web.XWikiServletContext;
import com.xpn.xwiki.web.XWikiServletRequest;
import com.xpn.xwiki.web.XWikiServletResponse;
import com.xpn.xwiki.web.XWikiURLFactory;

/**
 * @version $Id$
 */
public class XWikiSetupCleanupFilter extends Filter
{
    @Override
    protected int beforeHandle(Request request, Response response)
    {
        XWikiContext xwikiContext = null;
        XWikiRequest xwikiRequest = new XWikiServletRequest(getHttpRequest(request));
        XWikiResponse xwikiResponse = new XWikiServletResponse(getHttpResponse(response));
        ServletContextAdapter adapter = (ServletContextAdapter) getContext();
        ServletContext servletContext = adapter.getServletContext();
        XWikiServletContext xwikiServletContext = new XWikiServletContext(servletContext);

        try {
            xwikiContext = com.xpn.xwiki.web.Utils.prepareContext("", xwikiRequest, xwikiResponse, xwikiServletContext);

            initializeContainerComponent(xwikiContext);
            XWiki xwiki = XWiki.getXWiki(xwikiContext);

            XWikiURLFactory urlf = xwiki.getURLFactoryService().createURLFactory(xwikiContext.getMode(), xwikiContext);
            xwikiContext.setURLFactory(urlf);

            com.xpn.xwiki.api.XWiki xwikiApi = new com.xpn.xwiki.api.XWiki(xwiki, xwikiContext);

            String xwikiUser = "XWiki.XWikiGuest";
            xwikiContext.setUser(xwikiUser);

            /* XWiki platform objects are stocked in the Restlet context so all Restlet components can retrieve them. */
            Map<String, Object> attributes = getContext().getAttributes();
            attributes.put(Constants.XWIKI_CONTEXT, xwikiContext);
            attributes.put(Constants.XWIKI, xwiki);
            attributes.put(Constants.XWIKI_API, xwikiApi);
            attributes.put(Constants.XWIKI_USER, xwikiUser);
        } catch (Exception e) {
            if (xwikiContext != null) {
                cleanupComponents();
            }

            getLogger().log(Level.SEVERE, "Cannot initialize XWiki context.", e);

            return Filter.STOP;
        }

        getLogger().log(Level.FINE, "XWiki context initialized.");

        return Filter.CONTINUE;
    }

    @Override
    protected void afterHandle(Request request, Response response)
    {
        XWikiContext xwikiContext = (XWikiContext) getContext().getAttributes().get(Constants.XWIKI_CONTEXT);
        if (xwikiContext != null) {
            getLogger().log(Level.FINE, "XWiki context cleaned up.");
            cleanupComponents();
        }

        Map<String, Object> attributes = getContext().getAttributes();
        attributes.remove(Constants.XWIKI_CONTEXT);
        attributes.remove(Constants.XWIKI);
        attributes.remove(Constants.XWIKI_API);
        attributes.remove(Constants.XWIKI_USER);

        /* Avoid that empty entities make the engine forward the response creation to the XWiki servlet. */
        if (response.getEntity() != null) {
            if (!response.getEntity().isAvailable()) {
                response.setEntity(null);
            }
        }
    }

    private void initializeContainerComponent(XWikiContext context) throws ServletException
    {
        ServletContainerInitializer containerInitializer =
            (ServletContainerInitializer) com.xpn.xwiki.web.Utils.getComponent(ServletContainerInitializer.ROLE);

        try {
            containerInitializer.initializeRequest(context.getRequest().getHttpServletRequest(), context);
            containerInitializer.initializeResponse(context.getResponse().getHttpServletResponse());
            containerInitializer.initializeSession(context.getRequest().getHttpServletRequest());
        } catch (ServletContainerException e) {
            throw new ServletException("Failed to initialize request/response or session", e);
        }
    }

    private void cleanupComponents()
    {
        Container container = (Container) com.xpn.xwiki.web.Utils.getComponent(Container.ROLE);
        Execution execution = (Execution) com.xpn.xwiki.web.Utils.getComponent(Execution.ROLE);

        container.removeRequest();
        container.removeResponse();
        container.removeSession();
        execution.removeContext();
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

    /**
     * Builds the servlet response.
     * 
     * @param res The response.
     * @return httpServletResponse The http servlet response.
     */
    protected static HttpServletResponse getHttpResponse(Response res)
    {
        if (res instanceof HttpResponse) {
            HttpResponse httpResponse = (HttpResponse) res;
            HttpCall httpCall = httpResponse.getHttpCall();
            if (httpCall instanceof ServletCall) {
                return ((ServletCall) httpCall).getResponse();
            }
        }
        return null;
    }
}
