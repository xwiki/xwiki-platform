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
package com.xpn.xwiki.wysiwyg.server.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletContainerException;
import org.xwiki.container.servlet.ServletContainerInitializer;
import org.xwiki.context.Execution;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiEngineContext;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;
import com.xpn.xwiki.web.XWikiServletContext;
import com.xpn.xwiki.web.XWikiServletRequest;
import com.xpn.xwiki.web.XWikiServletResponse;

/**
 * This filter can be used to initialize the XWiki context before processing a request.
 * 
 * @version $Id$
 */
public class XWikiContextInitializationFilter implements Filter
{
    /**
     * The filter configuration object.
     */
    private FilterConfig filterConfig;

    /**
     * {@inheritDoc}
     * 
     * @see Filter#destroy()
     */
    public void destroy()
    {
        filterConfig = null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
        ServletException
    {
        try {
            initializeXWikiContext(request, response);
            chain.doFilter(request, response);
        } finally {
            cleanupComponents();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Filter#init(FilterConfig)
     */
    public void init(FilterConfig filterConfig) throws ServletException
    {
        this.filterConfig = filterConfig;
    }

    /**
     * Initializes the XWiki context.
     * 
     * @param request the request being processed
     * @param response the response
     * @throws ServletException if the initialization fails
     */
    protected void initializeXWikiContext(ServletRequest request, ServletResponse response) throws ServletException
    {
        XWikiEngineContext xwikiEngine = new XWikiServletContext(filterConfig.getServletContext());
        XWikiRequest xwikiRequest = new XWikiServletRequest((HttpServletRequest) request);
        XWikiResponse xwikiResponse = new XWikiServletResponse((HttpServletResponse) response);

        XWikiContext context;
        try {
            // Create the XWiki context.
            context = Utils.prepareContext("", xwikiRequest, xwikiResponse, xwikiEngine);
            // Initialize the XWiki database. The following method calls context.setWiki(XWiki).
            XWiki.getXWiki(context);
        } catch (XWikiException e) {
            throw new ServletException("Failed to prepare the XWiki context.", e);
        }

        // Initialize the Container component which is the new way of transporting the Context in the new component
        // architecture.
        initializeContainerComponent(context);
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
        ServletContainerInitializer containerInitializer = Utils.getComponent(ServletContainerInitializer.class);

        try {
            containerInitializer.initializeRequest(context.getRequest().getHttpServletRequest(), context);
            containerInitializer.initializeResponse(context.getResponse().getHttpServletResponse());
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
        Container container = Utils.getComponent(Container.class);
        container.removeRequest();
        container.removeResponse();
        container.removeSession();

        Execution execution = Utils.getComponent(Execution.class);
        execution.removeContext();
    }
}
