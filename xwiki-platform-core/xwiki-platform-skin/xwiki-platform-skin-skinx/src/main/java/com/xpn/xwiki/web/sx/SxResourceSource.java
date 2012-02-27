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
package com.xpn.xwiki.web.sx;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

/**
 * JAR resource source for Skin Extensions.
 * 
 * @version $Id$
 * @since 1.7M2
 */
public class SxResourceSource implements SxSource
{
    /** The full path of the resource to use as extension. Example: {@code path/to/hello.js}. */
    private String resourceName;

    /**
     * Constructor of this source.
     * 
     * @param resourceName the full path of the resource to use as extension. Example: {@code path/to/hello.js}
     */
    public SxResourceSource(String resourceName)
    {
        this.resourceName = resourceName;
    }

    @Override
    public CachePolicy getCachePolicy()
    {
        return CachePolicy.DEFAULT;
    }

    @Override
    public String getContent()
    {
        try {
            InputStream in = this.getClass().getResourceAsStream("/" + this.resourceName);
            return IOUtils.toString(in);
        } catch (NullPointerException e) {
            // This happens when the file was not found. Forward an IAE so that the sx action returns 404
            throw new IllegalArgumentException(e);
        } catch (IOException e) {
            return "";
        }
    }

    @Override
    public long getLastModifiedDate()
    {
        // There is no easy way to know the last modification date of a resource file.
        // return 0, which will make the action not set any Last-Modified date in the response.
        return 0;
    }

}
