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

/**
 * An extension repository identifier.
 * 
 * @version $Id$
 */
public class ExtensionRepositoryId
{
    /**
     * @see #getId()
     */
    private final String id;

    /**
     * @see #getType()
     */
    private final String type;

    /**
     * @see #getURI()
     */
    private final URI uri;

    /**
     * @param id the unique identifier
     * @param type the repository type (maven, xwiki, etc.)
     * @param uri the repository adress
     */
    public ExtensionRepositoryId(String id, String type, URI uri)
    {
        this.id = id;
        this.type = type;
        this.uri = uri;
    }

    /**
     * @return the unique identifier
     */
    public String getId()
    {
        return id;
    }

    /**
     * @return the repository type (maven, xwiki, etc.)
     */
    public String getType()
    {
        return type;
    }

    /**
     * @return the repository adress
     */
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
