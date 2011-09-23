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

/**
 * Base class for {@link ExtensionDependency} implementations.
 * 
 * @version $Id$
 */
public abstract class AbstractExtensionDependency implements ExtensionDependency
{
    /**
     * @see #getId()
     */
    private String id;

    /**
     * @see #getVersion()
     */
    private String version;

    /**
     * @param id the id of the extension
     * @param version the version of the extension
     */
    public AbstractExtensionDependency(String id, String version)
    {
        this.id = id;
        this.version = version;
    }

    @Override
    public String getId()
    {
        return this.id;
    }

    /**
     * @param id the extension id
     * @see #getId()
     */
    public void setId(String id)
    {
        this.id = id;
    }

    @Override
    public String getVersion()
    {
        return this.version;
    }

    /**
     * @param version the extension version
     * @see #getVersion()
     */
    public void setVersion(String version)
    {
        this.version = version;
    }

    @Override
    public String toString()
    {
        return getId() + '-' + getVersion();
    }
}
