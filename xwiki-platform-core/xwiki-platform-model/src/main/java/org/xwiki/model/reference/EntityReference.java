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
package org.xwiki.model.reference;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.xwiki.model.EntityType;

import java.io.Serializable;

/**
 * Represents a reference to an Entity (Document, Attachment, Space, Wiki, etc).
 *  
 * @version $Id$
 * @since 2.2M1
 */
public class EntityReference implements Serializable, Cloneable, Comparable<EntityReference>
{
    /**
     * The version identifier for this Serializable class. Increment only if the <i>serialized</i> form of the class
     * changes.
     */
    private static final long serialVersionUID = 1L;
    
    private String name;

    private EntityReference parent;

    private EntityReference child;

    private EntityType type;

    public EntityReference(String name, EntityType type)
    {
        this(name, type, null);
    }

    public EntityReference(String name, EntityType type, EntityReference parent)
    {
        setName(name);
        setType(type);
        setParent(parent);
    }

    /**
     * @exception IllegalArgumentException if the passed name is null or empty
     */
    public void setName(String name)
    {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("An Entity Reference name cannot be null or empty");
        }
        this.name = name;
    }

    public String getName()
    {
        return this.name;
    }

    public void setParent(EntityReference parent)
    {
        this.parent = parent;
        if (parent != null) {
            parent.setChild(this);
        }
    }

    public EntityReference getParent()
    {
        return this.parent;
    }

    public void setChild(EntityReference child)
    {
        this.child = child;
    }

    public EntityReference getChild()
    {
        return this.child;
    }

    /**
     * @exception IllegalArgumentException if the passed type is null
     */
    public void setType(EntityType type)
    {
        if (type == null) {
            throw new IllegalArgumentException("An Entity Reference type cannot be null");
        }
        this.type = type;
    }

    public EntityType getType()
    {
        return this.type;
    }

    public EntityReference getRoot()
    {
        EntityReference reference = this;
        while (reference.getParent() != null) {
            reference = reference.getParent();
        }
        return reference;
    }

    public EntityReference extractReference(EntityType type)
    {
        EntityReference reference = this;

        while (reference != null && reference.getType() != type) {
            reference = reference.getParent();
        }

        return reference;
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return "name = [" + getName() + "], type = [" + getType() + "], parent = [" + getParent() + "]";
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object#equals(Object) 
     */
    @Override
    public boolean equals(Object obj)
    {
        boolean equals = false;

        if (obj == this) {
            equals = true;
        } else if (obj instanceof EntityReference) {
            EntityReference entityReference = (EntityReference) obj;

            equals =
                (entityReference.getName() == null ? getName() == null : entityReference.getName().equals(getName()))
                    && (entityReference.getParent() == null ? getParent() == null : entityReference.getParent().equals(
                        getParent()))
                    && (entityReference.getType() == null ? getType() == null : entityReference.getType().equals(
                        getType())); 
        }

        return equals;
    }

    /**
     * {@inheritDoc}
     *
     * @see Object#hashCode() 
     */
    @Override
    public int hashCode()
    {
        return toString().hashCode();
    }

    /**
     * {@inheritDoc}
     *
     * @see Object#clone() 
     */
    @Override public EntityReference clone()
    {
        EntityReference reference;
        try {
            reference = (EntityReference) super.clone();
        } catch (CloneNotSupportedException e) {
            // Should never happen
            throw new RuntimeException("Failed to clone object", e);
        }
        reference.setName(getName());
        reference.setType(getType());
        if (getParent() != null) {
            reference.setParent(getParent().clone());
        }
        return reference;
    }

    /**
     * {@inheritDoc}
     * @see Comparable#compareTo(Object)
     */
    public int compareTo(EntityReference reference)
    {
        return new CompareToBuilder().append(toString(), reference.toString()).toComparison();
    }
}
