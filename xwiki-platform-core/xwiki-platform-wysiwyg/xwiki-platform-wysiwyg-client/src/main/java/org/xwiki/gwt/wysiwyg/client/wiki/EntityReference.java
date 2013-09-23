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

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * References a server side entity.
 * 
 * @version $Id$
 */
public class EntityReference implements IsSerializable
{
    /**
     * Represents a type of entity that can be referenced from the client.
     */
    public static enum EntityType
    {
        /**
         * Represents a Wiki Entity.
         */
        WIKI,

        /**
         * Represents a Space Entity.
         */
        SPACE,

        /**
         * Represents a Document Entity.
         */
        DOCUMENT,

        /**
         * Represents an Attachment Entity.
         */
        ATTACHMENT,

        /**
         * Represents an external entity, usually identified by an URI.
         */
        EXTERNAL;

        /**
         * @param expected the expected type
         * @param actual the actual type
         * @return {@code true} if the given types are equal, {@code false} otherwise
         */
        public static boolean areEqual(EntityType expected, EntityType actual)
        {
            return expected == actual || (expected != null && expected.equals(actual));
        }
    }

    /**
     * The entity reference components.
     */
    protected Map<String, String> components = new HashMap<String, String>();

    /**
     * The type of entity being referenced.
     */
    private EntityType type;

    /**
     * @return the type of entity being referenced
     */
    public EntityType getType()
    {
        return type;
    }

    /**
     * Sets the type of entity being referenced.
     * 
     * @param type the entity type
     */
    public void setType(EntityType type)
    {
        this.type = type;
    }

    /**
     * @param componentName the name of a reference component
     * @return the value of the specified reference component
     */
    public String getComponent(String componentName)
    {
        return components.get(componentName);
    }

    /**
     * Sets the value of a reference component.
     * 
     * @param componentName the name of the reference component
     * @param componentValue the value of the reference component
     */
    public void setComponent(String componentName, String componentValue)
    {
        components.put(componentName, componentValue);
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
        result = prime * result + components.hashCode();
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof EntityReference)) {
            return false;
        }
        EntityReference other = (EntityReference) obj;
        return components.equals(other.components) && EntityType.areEqual(type, other.type);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Object#clone()
     */
    public EntityReference clone()
    {
        EntityReference clone = new EntityReference();
        clone.setType(type);
        clone.components.putAll(components);
        return clone;
    }
}
