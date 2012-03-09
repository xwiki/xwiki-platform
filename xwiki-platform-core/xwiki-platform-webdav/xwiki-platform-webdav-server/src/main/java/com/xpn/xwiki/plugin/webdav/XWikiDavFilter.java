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
package com.xpn.xwiki.plugin.webdav;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.jackrabbit.webdav.DavMethods;

/**
 * The filter used to bypass the DefaultServlet of the servlet container.
 * 
 * @version $Id$
 */
public class XWikiDavFilter implements Filter
{
    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
        // Nothing to be initialized.
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * Here we 'steal' both OPTIONS and PROPFIND request types and direct them to webdav servlet.
     * </p>
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException
    {
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            int methodCode = DavMethods.getMethodCode(httpRequest.getMethod());
            if (httpRequest.getRequestURI().equals("/xwiki/")
                && (methodCode == DavMethods.DAV_OPTIONS || methodCode == DavMethods.DAV_PROPFIND)) {
                httpRequest.getRequestDispatcher("webdav").forward(request, response);
                return;
            }
        }
        chain.doFilter(request, response);
    }
    
    @Override
    public void destroy()
    {
        // Nothing to be destroyed.
    }
}
