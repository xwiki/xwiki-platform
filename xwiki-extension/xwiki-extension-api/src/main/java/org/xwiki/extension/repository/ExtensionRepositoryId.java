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
package org.xwiki.extension.repository;

import java.net.URI;

public class ExtensionRepositoryId
{
    private String id;

    private String type;

    private URI uri;

    public ExtensionRepositoryId(String id, String type, URI uri)
    {
        this.id = id;
        this.type = type;
        this.uri = uri;
    }

    public String getId()
    {
        return id;
    }

    public String getType()
    {
        return type;
    }

    public URI getURI()
    {
        return uri;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof ExtensionRepositoryId) {
            ExtensionRepositoryId extensionId = (ExtensionRepositoryId) obj;

            return id.equals(extensionId.getId()) && type.equals(extensionId.getType())
                && uri.equals(extensionId.getURI());
        }

        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        return toString().hashCode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return this.id + " (" + this.uri + ')';
    }
}
