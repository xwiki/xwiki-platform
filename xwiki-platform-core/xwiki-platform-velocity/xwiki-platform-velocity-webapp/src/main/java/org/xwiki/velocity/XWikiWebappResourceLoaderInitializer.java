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

import java.util.Properties;

import javax.inject.Singleton;

import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;
import org.xwiki.component.annotation.Component;
import org.xwiki.velocity.internal.ResourceLoaderInitializer;

/**
 * Inject a Servlet based {@link ResourceLoader} in the Velocity configuration.
 * 
 * @version $Id$
 * @since 12.0RC1
 */
@Component
@Singleton
public class XWikiWebappResourceLoaderInitializer implements ResourceLoaderInitializer
{
    private static final String RESOURCE_LOADER_ID = "xwiki";

    @Override
    public void initialize(Properties velocityProperties)
    {
        // Inject XWikiWebappResourceLoader as ResourceLoader
        velocityProperties.setProperty(RuntimeConstants.RESOURCE_LOADERS, RESOURCE_LOADER_ID);
        velocityProperties.setProperty(
            RuntimeConstants.RESOURCE_LOADER + '.' + RESOURCE_LOADER_ID + '.' + RuntimeConstants.RESOURCE_LOADER_CLASS,
            XWikiWebappResourceLoader.class.getName());
    }
}
