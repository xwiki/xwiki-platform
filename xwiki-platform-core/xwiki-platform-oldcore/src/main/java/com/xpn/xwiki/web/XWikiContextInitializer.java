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
package com.xpn.xwiki.web;

import java.io.IOException;
import java.lang.reflect.Type;

import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletContainerException;
import org.xwiki.container.servlet.ServletContainerInitializer;
import org.xwiki.context.Execution;
import org.xwiki.jakartabridge.servlet.JakartaServletBridge;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.user.api.XWikiUser;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * This filter can be used to initialize the XWiki context before processing a request.
 * <p>
 * While the class is much older, the since annotation was moved to 42.0.0 because it implement a completely different
 * API from Java point of view.
 * 
 * @version $Id$
 * @since 42.0.0
 */
public class XWikiContextInitializer implements Filter
{
    /**
     * XWiki context mode.
     */
    private int mode;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException
    {
        try {
            // Only HTTP requests are supported.
            if (request instanceof HttpServletRequest) {
                initializeXWikiContext(request, response);
            }
            chain.doFilter(request, response);
        } finally {
            if (request instanceof HttpServletRequest) {
                cleanupComponents();
            }
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
        try {
            this.mode = Integer.parseInt(filterConfig.getInitParameter("mode"));
        } catch (Exception e) {
            this.mode = -1;
        }
    }

    /**
     * Initializes the XWiki context.
     * 
     * @param request the request being processed
     * @param response the response
     * @throws ServletException if the initialization fails
     * @deprecated use {@link #initializeXWikiContext(ServletRequest, ServletResponse)} instead
     */
    @Deprecated(since = "42.0.0")
    protected void initializeXWikiContext(javax.servlet.ServletRequest request, javax.servlet.ServletResponse response)
        throws ServletException
    {
        initializeXWikiContext(JakartaServletBridge.toJakarta(request), JakartaServletBridge.toJakarta(response));
    }

    /**
     * Initializes the XWiki context.
     * 
     * @param request the request being processed
     * @param response the response
     * @throws ServletException if the initialization fails
     * @since 42.0.0
     */
    protected void initializeXWikiContext(ServletRequest request, ServletResponse response) throws ServletException
    {
        try {
            // Not all request types specify an action (e.g. GWT-RPC) so we default to the empty string.
            String action = "";
            XWikiServletContext xwikiEngine =
                new XWikiServletContext(JakartaServletBridge.toJavax(request.getServletContext()));
            XWikiServletRequest xwikiRequest =
                new XWikiServletRequest(JakartaServletBridge.toJavax((HttpServletRequest) request));
            XWikiServletResponse xwikiResponse =
                new XWikiServletResponse(JakartaServletBridge.toJavax((HttpServletResponse) response));

            // Create the XWiki context.
            XWikiContext context = Utils.prepareContext(action, xwikiRequest, xwikiResponse, xwikiEngine);

            // Overwrite the context mode set in the prepareContext() call just above if the mode filter initialization
            // parameter is specified.
            if (this.mode >= 0) {
                context.setMode(this.mode);
            }

            // Initialize the Container component which is the new way of transporting the Context in the new component
            // architecture. Further initialization might require the Container component.
            initializeContainerComponent(context);

            // Initialize the XWiki database. XWiki#getXWiki(XWikiContext) calls XWikiContext.setWiki(XWiki).
            XWiki xwiki = XWiki.getXWiki(context);

            // Initialize the URL factory.
            context.setURLFactory(xwiki.getURLFactoryService().createURLFactory(context.getMode(), context));

            // Prepare the localized resources, according to the selected language.
            xwiki.prepareResources(context);

            // Initialize the current user.
            XWikiUser user = context.getWiki().checkAuth(context);
            if (user != null) {
                DocumentReference userReference = user.getUserReference();
                context.setUserReference(
                    XWikiRightService.GUEST_USER.equals(userReference.getName()) ? null : userReference);
            }
        } catch (XWikiException e) {
            throw new ServletException("Failed to initialize the XWiki context.", e);
        }
    }

    /**
     * @param context the XWiki context
     * @throws ServletException if the container component initialization fails
     */
    protected void initializeContainerComponent(XWikiContext context) throws ServletException
    {
        // Initialize the Container fields (request, response, session). Note that this is a bridge between the old core
        // and the component architecture. In the new component architecture we use ThreadLocal to transport the
        // request, response and session to components which require them.
        ServletContainerInitializer containerInitializer = Utils.getComponent((Type) ServletContainerInitializer.class);

        try {
            containerInitializer.initializeRequest(context.getRequest().getHttpServletRequest(), context);
            containerInitializer.initializeResponse(context.getResponse());
            containerInitializer.initializeSession(context.getRequest().getHttpServletRequest());
        } catch (ServletContainerException e) {
            throw new ServletException("Failed to initialize Request/Response or Session", e);
        }
    }

    /**
     * We must ensure we clean the ThreadLocal variables located in the Container and Execution components as otherwise
     * we will have a potential memory leak.
     */
    protected void cleanupComponents()
    {
        Container container = Utils.getComponent((Type) Container.class);
        container.removeRequest();
        container.removeResponse();
        container.removeSession();

        Execution execution = Utils.getComponent((Type) Execution.class);
        execution.removeContext();
    }
}
