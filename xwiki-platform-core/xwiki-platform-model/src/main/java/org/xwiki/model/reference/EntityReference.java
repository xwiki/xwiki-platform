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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

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

    /**
     * Parameter key for specifying the Locale of the referenced Entity.
     * @see #getLocale() 
     */
    private static final String LOCALE_PARAMETER_KEY = "language";
    
    private String name;

    private EntityReference parent;

    private EntityReference child;

    private EntityType type;

    /**
     * @see #getParameter(String)
     */
    private Map<String, Object> parameters = new HashMap<String, Object>();

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

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("name = [").append(getName()).append("]");
        builder.append(", type = [").append(getType()).append("]");
        builder.append(", parent = [").append(getParent()).append("]");
        builder.append(", parameters = [");
        Iterator<Map.Entry<String, Object>> it = this.parameters.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> parameter = it.next();
            builder.append("[").append(parameter.getKey()).append("] = [").append(parameter.getValue()).append("]");
            if (it.hasNext()) {
                builder.append(", ");
            }
        }
        builder.append((']'));
        return builder.toString();
    }

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

            // Iterate over all parameters
            equals = equals && (this.parameters.size() == entityReference.parameters.size());
            if (equals) {
                for (Map.Entry<String, Object> parameter : this.parameters.entrySet()) {
                    equals = equals
                        && (parameter.getValue() == null ? entityReference.getParameter(parameter.getKey()) == null :
                            parameter.getValue().equals(entityReference.getParameter(parameter.getKey())));
                }
            }
        }

        return equals;
    }

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
    @Override
    public EntityReference clone()
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
        reference.parameters = new HashMap<String, Object>(this.parameters);

        return reference;
    }

    @Override
    public int compareTo(EntityReference reference)
    {
        return new CompareToBuilder().append(toString(), reference.toString()).toComparison();
    }

    /**
     * @param key the key for the parameter value to add
     * @param value the parameter value to add
     * @see #getParameter(String)
     */
    public void addParameter(String key, Object value)
    {
        this.parameters.put(key, value);
    }

    /**
     * @param key the key for which to return the parameter value
     * @return the parameter corresponding to the passed key or null if no such parameter exist (typical parameters can
     *         be for example the version of the referenced Entity or the language of the referenced Entity). Since
     *         those parameters are optional it's up to the code using this Entity Reference to check if the parameter
     *         exists
     */
    public Object getParameter(String key)
    {
        return this.parameters.get(key);
    }

    /**
     * @return the Locale of the referenced Entity or null if no such information is available
     */
    public Locale getLocale()
    {
        Locale result = null;
        Object value = getParameter(LOCALE_PARAMETER_KEY);
        if (Locale.class.isAssignableFrom(value.getClass())) {
            result = (Locale) value;
        }
        return result;
    }

    /**
     * @param locale see {@link #getLocale()}
     */
    public void setLocale(Locale locale)
    {
        addParameter(LOCALE_PARAMETER_KEY, locale);
    }
}
