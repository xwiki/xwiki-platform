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
package org.xwiki.velocity;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.velocity.tools.view.servlet.WebappLoader;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.environment.Environment;
import org.xwiki.environment.internal.ServletEnvironment;

/**
 * Extends the Velocity Tool's {@link WebappLoader} by adding the ServletContext to the Velocity Engine's Application
 * Attributes since this is a prerequisite for {@link WebappLoader} to work correctly. This resource loader depends on
 * the fact that the XWiki Component Manager has been set in Velocity Engine's Application Attribute prior to its
 * initialization.
 *
 * @version $Id$
 * @since 3.0M3
 */
public class XWikiWebappResourceLoader extends WebappLoader
{
    @Override
    public void init(ExtendedProperties configuration)
    {
        Environment environment = getEnvironment();
        if (environment instanceof ServletEnvironment) {
            this.rsvc.setApplicationAttribute("javax.servlet.ServletContext",
                ((ServletEnvironment) environment).getServletContext());
        }

        super.init(configuration);
    }

    /**
     * @return the Environment component implementation retrieved from the Component Manager
     */
    private Environment getEnvironment()
    {
        try {
            return getComponentManager().getInstance(Environment.class);
        } catch (ComponentLookupException e) {
            throw new RuntimeException(
                "Cannot initialize Velocity subsystem: missing Environment component implementation");
        }
    }

    /**
     * @return the Component Manager component implementation retrieved from Velocity Engine's Application Attributes
     */
    private ComponentManager getComponentManager()
    {
        ComponentManager cm =
            (ComponentManager) this.rsvc.getApplicationAttribute(ComponentManager.class.getName());
        if (cm == null) {
            throw new RuntimeException(
                "Cannot initialize Velocity subsystem: missing Component Manager in Velocity Application Attribute");
        }
        return cm;
    }
}
