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
package org.xwiki.gwt.wysiwyg.client.wiki;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * An entity in the wiki model.
 * 
 * @version $Id$
 */
public class Entity implements IsSerializable
{
    /**
     * The entity reference uniquely identifies and locates the entity.
     */
    private EntityReference reference;

    /**
     * The URL that can be used to access the entity.
     */
    private String url;

    /**
     * @return the entity reference
     */
    public EntityReference getReference()
    {
        return reference;
    }

    /**
     * Sets the entity reference.
     * 
     * @param reference the new entity reference
     */
    public void setReference(EntityReference reference)
    {
        this.reference = reference;
    }

    /**
     * @return the entity URL
     */
    public String getUrl()
    {
        return url;
    }

    /**
     * Sets the entity URL.
     * 
     * @param url the new entity URL
     */
    public void setUrl(String url)
    {
        this.url = url;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((reference == null) ? 0 : reference.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Entity)) {
            return false;
        }
        Entity other = (Entity) obj;
        if (reference == null) {
            if (other.reference != null) {
                return false;
            }
        } else if (!reference.equals(other.reference)) {
            return false;
        }
        return true;
    }
}
