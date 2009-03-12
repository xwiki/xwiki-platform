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

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletContext;

import org.xwiki.container.ApplicationContext;

public class ServletApplicationContext implements ApplicationContext
{
    private ServletContext servletContext;

    public ServletApplicationContext(ServletContext servletContext)
    {
        this.servletContext = servletContext;
    }

    public ServletContext getServletContext()
    {
        return this.servletContext;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.container.ApplicationContext#getResourceAsStream(String)
     */
    public InputStream getResourceAsStream(String resourceName)
    {
        return getServletContext().getResourceAsStream(resourceName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.container.ApplicationContext#getResource(String)
     */
    public URL getResource(String resourceName) throws MalformedURLException
    {
        return getServletContext().getResource(resourceName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.container.ApplicationContext#getTemporaryDirectory()
     */
    public File getTemporaryDirectory()
    {
        // Section SRV.4.7.1 of the Servlet 2.5 specification says that this should be available.
        return (File) this.servletContext.getAttribute("javax.servlet.context.tempdir");
    }
}
