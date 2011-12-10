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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A simple action that redirects to the main page of the wiki. This is to allow users to enter a URL like
 * <code>http://localhost:8080/xwiki</code> and be redirected automatically to
 * <code>http://localhost:8080/xwiki/bin/view/Main/</code>.
 */
public class HomePageRedirectServlet extends HttpServlet
{
    /** The address to use as a home page where the users are redirected. */
    private String home = "bin/view/Main/";

    /**
     * {@inheritDoc}
     * 
     * @see javax.servlet.GenericServlet#init()
     */
    @Override
    public void init() throws ServletException
    {
        super.init();
        // TODO: we cannot use the XWiki API to determine the right URL, because this is a servlet and the core
        // is reachable mainly from Struts. Getting access to the core requires too much duplication, so for the
        // moment we're going the easy way: hardcoded values.
        String homeParameter = getInitParameter("homePage");
        if (homeParameter != null) {
            this.home = homeParameter;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        response.sendRedirect(this.home);
    }
}
