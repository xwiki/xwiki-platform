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
package org.xwiki.container;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @version $Id$
 * @deprecated use the notion of {@link org.xwiki.environment.Environment} instead
 */
@Deprecated(since = "3.5M1")
public interface ApplicationContext
{
    InputStream getResourceAsStream(String resourceName);

    URL getResource(String resourceName) throws MalformedURLException;

    /**
     * Gets the directory which the container must provide for storing temporary data. The contents of this directory
     * may be deleted between container restarts (<em>temporary</em>, as the name implies), so it is not a safe place to
     * store permanent/important data.
     * 
     * @return a {@link File} object pointing to a directory that the application can use for storing temporary files
     */
    File getTemporaryDirectory();

    /**
     * Gets the root directory which the container must provide for storing persisting data. The content of this
     * directory will remained unchanged after a restart of the container.
     * 
     * @return a {@link File} object pointing to the root folder of the work directory
     */
    File getPermanentDirectory();
}
