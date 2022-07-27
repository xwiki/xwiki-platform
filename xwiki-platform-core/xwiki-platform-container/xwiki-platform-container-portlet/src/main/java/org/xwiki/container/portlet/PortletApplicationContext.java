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
package org.xwiki.container.portlet;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.portlet.PortletContext;

import org.xwiki.component.manager.ComponentManager;
import org.xwiki.container.AbstractApplicationContext;

/**
 * @deprecated use the notion of Environment instead
 */
@Deprecated(since = "3.5M1")
public class PortletApplicationContext extends AbstractApplicationContext
{
    private PortletContext portletContext;

    public PortletApplicationContext(PortletContext portletContext, ComponentManager componentManager)
    {
        super(componentManager);

        this.portletContext = portletContext;
    }

    public PortletContext getPortletContext()
    {
        return this.portletContext;
    }

    @Override
    public InputStream getResourceAsStream(String resourceName)
    {
        return getPortletContext().getResourceAsStream(resourceName);
    }

    @Override
    public URL getResource(String resourceName) throws MalformedURLException
    {
        return getPortletContext().getResource(resourceName);
    }

    @Override
    public File getTemporaryDirectory()
    {
        // Section PLT.10.3 from the Portlet 1.0 specification says that this should be available.
        // FIXME: why is this using a servlet specifications variable name ? Is that really valid for a portlet ?
        return (File) this.portletContext.getAttribute("javax.servlet.context.tempdir");
    }
}
