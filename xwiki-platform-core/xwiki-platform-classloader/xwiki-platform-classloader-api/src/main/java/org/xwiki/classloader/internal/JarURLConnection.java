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
package org.xwiki.classloader.internal;

import java.io.IOException;
import java.util.jar.JarFile;

/**
 * Common interface to be implemented by all {@link java.net.URLConnection} implementations that wish to override the
 * {@code getJarFile()} method from {@link java.net.JarURLConnection}.
 *
 * @version $Id$
 * @since 3.1M1
 */
public interface JarURLConnection
{
    /**
     * Return the JAR file for this connection.
     *
     * @return the JAR file for this connection. If the connection is a connection to an entry of a JAR file, the JAR
     *         file object is returned
     * @exception IOException if an IOException occurs while trying to connect to the JAR file for this connection.
     *
     * @see java.net.JarURLConnection#getJarFile()
     */
    JarFile getJarFile() throws IOException;
}
