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
package org.xwiki.test.page;

import java.util.Properties;

import org.apache.velocity.runtime.RuntimeConstants;
import org.xwiki.velocity.XWikiWebappResourceLoader;
import org.xwiki.velocity.internal.ResourceLoaderInitializer;

/**
 * A custom version of XWikiWebappResourceLoaderInitializer which does not fail Velocity when "/templates/macros.vm" is
 * not available.
 * 
 * @version $Id$
 */
public class PageTestResourceLoaderInitializer implements ResourceLoaderInitializer
{
    private static final String RESOURCE_LOADER_ID = "xwiki";

    @Override
    public void initialize(Properties velocityProperties)
    {
        if (getClass().getClassLoader().getResource("templates/macros.vm") != null) {
            // Inject XWikiWebappResourceLoader as ResourceLoader
            velocityProperties.setProperty(RuntimeConstants.RESOURCE_LOADERS, RESOURCE_LOADER_ID);
            velocityProperties.setProperty(RuntimeConstants.RESOURCE_LOADER + '.' + RESOURCE_LOADER_ID + '.'
                + RuntimeConstants.RESOURCE_LOADER_CLASS, XWikiWebappResourceLoader.class.getName());

            // Add macros.vm as default template
            velocityProperties.put(RuntimeConstants.VM_LIBRARY, "/templates/macros.vm");
        }
    }
}
