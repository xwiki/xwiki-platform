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
package org.xwiki.extension;

import java.io.Serializable;

import org.xwiki.extension.version.Version;
import org.xwiki.extension.version.internal.DefaultVersion;

/**
 * The combination of properties which makes an extension unique.
 * 
 * @version $Id$
 */
public class ExtensionId implements Serializable
{
    /**
     * Serialization identifier.
     */
    private static final long serialVersionUID = 1L;

    /**
     * @see #getId()
     */
    private final String id;

    /**
     * @see #getVersion()
     */
    private final Version version;

    /**
     * @param id the extension identifier
     * @param version the extension version
     */
    public ExtensionId(String id, String version)
    {
        this(id, new DefaultVersion(version));
    }

    /**
     * @param id the extension identifier
     * @param version the extension version
     */
    public ExtensionId(String id, Version version)
    {
        this.id = id;
        this.version = version;
    }

    /**
     * @return the extension identifier
     */
    public String getId()
    {
        return this.id;
    }

    /**
     * @return the extension version
     */
    public Version getVersion()
    {
        return this.version;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof ExtensionId) {
            ExtensionId extensionId = (ExtensionId) obj;

            return getId().equals(extensionId.getId()) && getVersion().equals(extensionId.getVersion());
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        return toString().hashCode();
    }

    @Override
    public String toString()
    {
        return getId() + '-' + getVersion();
    }
}
