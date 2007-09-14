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
 *
 */
package org.xwiki.container.servlet;

import org.xwiki.container.ApplicationContext;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.container.Response;
import org.xwiki.container.Session;
import org.xwiki.url.URLFactory;
import org.xwiki.url.InvalidURLException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ServletContainer implements Container
{
    private URLFactory urlFactory;
    
    private ApplicationContext applicationContext;
    private ServletRequest request;
    private Response response;
    private Session session;
    
    /**
     * Called by XWikiServletContextListener when XWiki starts.
     */
    public void initialize(ServletContext servletContext) throws ServletContainerException
    {
        this.applicationContext = new ServletApplicationContext(servletContext);
    }

    public void initialize(HttpServletRequest httpServletRequest,
        HttpServletResponse httpServletResponse) throws ServletContainerException
    {
        this.request = initializeServletRequest(httpServletRequest);
        this.response = new ServletResponse(httpServletResponse);
        this.session = new ServletSession(httpServletRequest);
    }

    private ServletRequest initializeServletRequest(HttpServletRequest httpServletRequest)
        throws ServletContainerException
    {
        ServletRequest request = new ServletRequest(httpServletRequest);
        try {
            request.setXWikiURL(
                this.urlFactory.createURL(httpServletRequest.getRequestURL().toString()));
        } catch (InvalidURLException e) {
            // TODO: If the URL specified is invalid, then we should redirect to some error action
            // somehow. Need to architecte mechanism for this.
            throw new ServletContainerException("Failed to parse Servlet request URL ["
                + httpServletRequest.getRequestURL().toString() + "]", e);
        }
        return request;        
    }

    public ApplicationContext getApplicationContext()
    {
        return this.applicationContext;    
    }

    public Request getRequest()
    {
        return this.request;
    }

    public Response getResponse()
    {
        return this.response;
    }

    public Session getSession()
    {
        return this.session;
    }
}
