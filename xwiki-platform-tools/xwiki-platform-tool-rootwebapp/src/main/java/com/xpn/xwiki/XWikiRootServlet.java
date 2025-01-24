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
package com.xpn.xwiki;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * The root servlet for XWiki. The purpose of this servlet is to respond to WebDAV requests correctly and to redirect
 * get requests on server root appropriately.
 * 
 * @version $Id$
 */
public class XWikiRootServlet extends HttpServlet
{
    /** Class version identifier. Needed for serialization. */
    private static final long serialVersionUID = -4907199005755391420L;

    /**
     * The address to redirect to (the name of the XWiki webapp). Configured using the <code>redirectTo</code>
     * initialization parameter in <code>web.xml</code>.
     */
    private String xwiki = "xwiki";

    @Override
    public void init() throws ServletException
    {
        super.init();
        String redirectParameter = getInitParameter("redirectTo");
        if (redirectParameter != null) {
            this.xwiki = redirectParameter;
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Responds to an OPTIONS requests on / with appropriate headers.
     * </p>
     */
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException,
        IOException
    {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("MS-Author-Via", "DAV");
        response.setHeader("Content-Language", "en");
        response.setHeader("DAV", "1,2");
        response.setHeader("Allow", "OPTIONS, GET, HEAD, PROPFIND, LOCK, UNLOCK");
        response.setHeader("Content-Length", "0");
        response.flushBuffer();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Redirects GET requests on <code>/</code> (the server root) to the XWiki application context. That is usually
     * <code>/xwiki/</code>, but can be configured from <code>web.xml</code> using the <code>redirectTo</code>
     * initialization parameter for this servlet.
     * </p>
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        response.sendRedirect(response.encodeRedirectURL(this.xwiki));
    }
}
