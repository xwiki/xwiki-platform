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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.xwiki.jakartabridge.servlet.JakartaServletBridge;

import com.xpn.xwiki.XWikiContext;

/**
 * This filter can be used to initialize the XWiki context before processing a request.
 * 
 * @version $Id$
 * @since 13.4RC1
 * @deprecated use {@link XWikiContextInitializer} instead
 */
@Deprecated(since = "42.0.0")
public class XWikiContextInitializationFilter implements Filter
{
    private final XWikiContextInitializer jakarta = new XWikiContextInitializer();

    @Override
    public void destroy()
    {
        this.jakarta.destroy();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException
    {
        try {
            this.jakarta.doFilter(JakartaServletBridge.toJakarta(request), JakartaServletBridge.toJakarta(response),
                JakartaServletBridge.toJakarta(chain));
        } catch (jakarta.servlet.ServletException e) {
            throw new ServletException(e);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
        try {
            this.jakarta.init(JakartaServletBridge.toJakarta(filterConfig));
        } catch (jakarta.servlet.ServletException e) {
            throw new ServletException(e);
        }
    }

    /**
     * @param context the XWiki context
     * @throws ServletException if the container component initialization fails
     */
    protected void initializeContainerComponent(XWikiContext context) throws ServletException
    {
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
        
    }

    /**
     * We must ensure we clean the ThreadLocal variables located in the Container and Execution components as otherwise
     * we will have a potential memory leak.
     */
    protected void cleanupComponents()
    {
    }
}
