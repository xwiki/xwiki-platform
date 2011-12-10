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
package org.xwiki.extension.repository.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.xwiki.extension.CoreExtensionFile;

/**
 * Default implementation of {@link CoreExtensionFile}.
 * 
 * @version $Id$
 */
public class DefaultCoreExtensionFile implements CoreExtensionFile
{
    /**
     * @see #getURL()
     */
    private URL url;

    /**
     * @see #getConnection()
     */
    private URLConnection connection;

    /**
     * @param url the URL of the core extension
     */
    public DefaultCoreExtensionFile(URL url)
    {
        this.url = url;
    }

    /**
     * @return the URL connection
     */
    private URLConnection getConnection()
    {
        if (this.connection == null) {
            try {
                this.connection = this.url.openConnection();
            } catch (IOException e) {
                throw new RuntimeException("Failed to open URL [" + url + "]");
            }
        }

        return this.connection;
    }

    @Override
    public long getLength()
    {
        return getConnection().getContentLength();
    }

    @Override
    public InputStream openStream() throws IOException
    {
        return getConnection().getInputStream();
    }

    @Override
    public URL getURL()
    {
        return this.url;
    }

}
