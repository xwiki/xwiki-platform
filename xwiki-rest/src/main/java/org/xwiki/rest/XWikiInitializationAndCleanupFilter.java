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

import org.restlet.Context;
import org.restlet.Filter;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.xwiki.component.manager.ComponentManager;
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
 * This is a filter that setups the XWiki context before servicing a request and cleans things up after the request has
 * been serviced.
 * 
 * @version $Id$
 */
public class XWikiInitializationAndCleanupFilter extends Filter
{
    private ComponentManager componentManager;

    public XWikiInitializationAndCleanupFilter(ComponentManager componentManager, Context context)
    {
        super(context);
        this.componentManager = componentManager;
    }

    /**
     * Initialize the XWiki context and put relevant objects in the Restlet context attributes.
     */
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

            /* XWiki platform objects are stocked in the Restlet context so all Restlet components can retrieve them. */
            Map<String, Object> attributes = getContext().getAttributes();
            attributes.put(Constants.XWIKI_CONTEXT, xwikiContext);
            attributes.put(Constants.XWIKI, xwiki);
            attributes.put(Constants.XWIKI_API, xwikiApi);
        } catch (Exception e) {
            e.printStackTrace();
            if (xwikiContext != null) {
                cleanupComponents();
            }

            getLogger().log(Level.SEVERE, "Error while initializing XWiki context.");

            /* In case of exception do not relay the request to components behind this filter. */
            return Filter.STOP;
        }

        getLogger().log(Level.FINE, "XWiki context initialized.");

        return Filter.CONTINUE;
    }

    @Override
    protected void afterHandle(Request request, Response response)
    {
        Utils.cleanupResource(request, componentManager, getLogger());

        XWikiContext xwikiContext = (XWikiContext) getContext().getAttributes().get(Constants.XWIKI_CONTEXT);
        if (xwikiContext != null) {
            getLogger().log(Level.FINE, "XWiki context cleaned up.");
            cleanupComponents();
        }

        super.afterHandle(request, response);
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

    private void initializeContainerComponent(XWikiContext context) throws ServletException
    {
        // Initialize the Container fields (request, response, session).
        // Note that this is a bridge between the old core and the component
        // architecture.
        // In the new component architecture we use ThreadLocal to transport the
        // request,
        // response and session to components which require them.
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

        // We must ensure we clean the ThreadLocal variables located in the
        // Container and Execution
        // components as otherwise we will have a potential memory leak.
        container.removeRequest();
        container.removeResponse();
        container.removeSession();
        execution.removeContext();
    }

}
