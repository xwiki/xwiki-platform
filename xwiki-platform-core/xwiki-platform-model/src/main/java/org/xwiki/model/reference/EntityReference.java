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

import java.io.Serializable;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.model.EntityType;

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
    private static final long serialVersionUID = 2L;

    /**
     * Name of this entity.
     */
    private String name;

    /**
     * Parent reference of this entity.
     */
    private EntityReference parent;

    /**
     * Type of this entity.
     */
    private EntityType type;

    /**
     * Parameters of this entity.
     */
    private Map<String, Serializable> parameters;

    /**
     * Clone an EntityReference.
     *
     * @param reference the reference to clone
     * @since 3.3M2
     */
    public EntityReference(EntityReference reference)
    {
        this(reference, reference.parent);
    }

    /**
     * Clone an EntityReference, but use the specified parent for its new parent.
     *
     * @param reference the reference to clone
     * @param parent the new parent to use
     * @since 3.3M2
     */
    public EntityReference(EntityReference reference, EntityReference parent)
    {
        this(reference.name, reference.type, parent, reference.parameters);
    }

    /**
     * Clone an EntityReference, but replace one of the parent in the chain by an other one.
     *
     * @param reference the reference that is cloned
     * @param oldReference the old parent that will be replaced
     * @param newReference the new parent that will replace oldReference in the chain
     * @since 3.3M2
     */
    protected EntityReference(EntityReference reference, EntityReference oldReference, EntityReference newReference)
    {
        if (reference == null) {
            throw new IllegalArgumentException("Cloned reference must not be null");
        }

        setName(reference.name);
        setType(reference.type);
        setParameters(reference.parameters);
        if (reference.parent == null) {
            if (oldReference == null) {
                setParent(newReference);
            } else {
                throw new IllegalArgumentException("The old reference [" + oldReference
                    + "] does not belong to the parents chain of the reference [" + reference + "]");
            }
        } else if (reference.parent.equals(oldReference)) {
            setParent(newReference);
        } else {
            setParent(new EntityReference(reference.parent, oldReference, newReference));
        }
    }

    /**
     * Create a new root EntityReference.
     * Note: Entity reference are immutable since 3.3M2.
     *
     * @param name name for the newly created entity reference, could not be null.
     * @param type type for the newly created entity reference, could not be null.
     */
    public EntityReference(String name, EntityType type)
    {
        this(name, type, null, null);
    }

    /**
     * Create a new EntityReference.
     * Note: Entity reference are immutable since 3.3M2.
     *
     * @param name name for the newly created entity reference, could not be null.
     * @param type type for the newly created entity reference, could not be null.
     * @param parent parent reference for the newly created entity reference, may be null.
     */
    public EntityReference(String name, EntityType type, EntityReference parent)
    {
        setName(name);
        setType(type);
        setParent(parent);
    }

    /**
     * Create a new EntityReference.
     * Note: Entity reference are immutable since 3.3M2.
     *
     * @param name name for the newly created entity, could not be null.
     * @param type type for the newly created entity, could not be null.
     * @param parent parent reference for the newly created entity reference, may be null.
     * @param parameters parameters for this reference, may be null
     * @since 3.3M2
     */
    protected EntityReference(String name, EntityType type, EntityReference parent,
        Map<String, Serializable> parameters)
    {
        setName(name);
        setType(type);
        setParent(parent);
        setParameters(parameters);
    }

    /**
     * Entity reference are immutable since 3.3M2, so this method is now protected.
     * @param name the name for this entity
     * @exception IllegalArgumentException if the passed name is null or empty
     */
    protected void setName(String name)
    {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("An Entity Reference name cannot be null or empty");
        }
        this.name = name;
    }

    /**
     * Returns the name of this entity.
     * This method is final to ensure that name is never null and we use the private field in all other methods of
     * this implementation (faster).
     *
     * @return the name of this entity.
     */
    public final String getName()
    {
        return this.name;
    }

    /**
     * Entity reference are immutable since 3.3M2, so this method is now protected.
     * @param parent the parent for this entity, may be null for a root entity.
     */
    protected void setParent(EntityReference parent)
    {
        this.parent = parent;
    }

    /**
     * @return the parent of this entity, may be null for a root entity.
     */
    public final EntityReference getParent()
    {
        return this.parent;
    }

    /**
     * Entity reference are immutable since 3.3M2, so this method is now protected.
     * @param type the type for this entity
     * @exception IllegalArgumentException if the passed type is null
     */
    protected void setType(EntityType type)
    {
        if (type == null) {
            throw new IllegalArgumentException("An Entity Reference type cannot be null");
        }
        this.type = type;
    }

    /**
     * Returns the type of this entity.
     * This method is final to ensure that type is never null and we use the private field in all other methods of
     * this implementation (faster).
     * @return the type of this entity
     */
    public final EntityType getType()
    {
        return this.type;
    }

    /**
     * Set multiple parameters at once.
     *
     * @param parameters the map of parameter to set
     * @since 3.3M2
     */
    protected void setParameters(Map<String, Serializable> parameters)
    {
        if (parameters != null) {
            for (Map.Entry<String, Serializable> entry : parameters.entrySet()) {
                setParameter(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Add or set a parameter value. Parameters should be immutable objects to prevent any weird behavior.
     *
     * @param name the name of the parameter
     * @param value the value of the parameter
     * @since 3.3M2
     */
    protected void setParameter(String name, Serializable value)
    {
        if (value != null) {
            if (parameters == null) {
                parameters = new TreeMap<String, Serializable>();
            }
            parameters.put(name, value);
        } else if (parameters != null) {
            parameters.remove(name);
            if (parameters.size() == 0) {
                parameters = null;
            }
        }
    }

    /**
     * Get the value of a parameter. Return null if the parameter is not set.
     * This method is final so there is no way to override the map, and the private field in all other methods of
     * this implementation (faster).
     *
     * @param <T> the type of the value of the requested parameter
     * @param name the name of the parameter to get
     * @return the value of the parameter
     * @since 3.3M2
     */
    @SuppressWarnings("unchecked")
    protected final <T> T getParameter(String name)
    {
        return (parameters == null) ? null : (T) parameters.get(name);
    }

    /**
     * @return the root parent of this entity
     */
    public EntityReference getRoot()
    {
        EntityReference reference = this;
        while (reference.getParent() != null) {
            reference = reference.getParent();
        }
        return reference;
    }

    /**
     * @return a list of references in the parents chain of this reference, ordered from root to this reference.
     */
    @SuppressWarnings("unchecked")
    public List<EntityReference> getReversedReferenceChain()
    {
        Deque<EntityReference> referenceList = new LinkedList<EntityReference>();
        EntityReference reference = this;
        do {
            referenceList.push(reference);
            reference = reference.getParent();
        } while (reference != null);
        return (List<EntityReference>) referenceList;
    }

    /**
     * Extract the entity of the given type from this one. This entity may be returned if it has the type requested.
     * @param type the type of the entity to be extracted
     * @return the entity of the given type
     */
    public EntityReference extractReference(EntityType type)
    {
        EntityReference reference = this;

        while (reference != null && reference.getType() != type) {
            reference = reference.getParent();
        }

        return reference;
    }

    /**
     * Return a clone of this reference, but with one of its parent replaced by another one.
     *
     * @param oldParent the old parent that will be replaced
     * @param newParent the new parent that will replace oldParent in the chain. If the same as oldParent,
     *                  this is returned.
     * @return a new reference with a amended parent chain
     * @since 3.3M2
     */
    public EntityReference replaceParent(EntityReference oldParent, EntityReference newParent)
    {
        if (oldParent == newParent) {
            return this;
        }
        return new EntityReference(this, oldParent, newParent);
    }

    /**
     * Return a clone of this reference with a parent appended at the root of its parents chain.
     *
     * @param newParent the parent that became the root parent of this reference (and its parent).
     *                  If null, this is returned.
     * @return a new reference with newParent added to its parent chain
     */
    public EntityReference appendParent(EntityReference newParent)
    {
        if (newParent == null) {
            return this;
        }
        return new EntityReference(this, null, newParent);
    }

    /**
     * Return a clone of this reference truncated to a null parent when it reach the given parent.
     * It is very similar to replaceParent(parent, null), except that it is not overridden.
     *
     * @param oldParent the parent that will be replaced by a null. If null, this is returned.
     * @return a new reference with oldParent and its descendant removed from its parent chain
     * @since 4.0M2
     */
    public EntityReference removeParent(EntityReference oldParent)
    {
        if (oldParent == null) {
            return this;
        }
        return new EntityReference(this, oldParent, null);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(64);
        sb.append("name = [")
            .append(getName())
            .append("], type = [")
            .append(getType())
            .append("], parent = [")
            .append(getParent())
            .append(']');
        if (parameters != null) {
            sb.append(" parameters = {");
            boolean first = true;
            for (Map.Entry<String, Serializable> entry : parameters.entrySet()) {
                if (first) {
                    first = false;
                } else {
                    sb.append(", ");
                }
                sb.append(entry.getKey())
                    .append(" = [")
                    .append(entry.getValue().toString())
                    .append(']');
            }
            sb.append('}');
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof EntityReference)) {
            return false;
        }

        EntityReference ref = (EntityReference) obj;

        return name.equals(ref.name) && type.equals(ref.type)
            && (parent == null ? ref.parent == null : parent.equals(ref.parent))
            && (parameters == null ? ref.parameters == null : parameters.equals(ref.parameters));
    }

    @Override
    public int hashCode()
    {
        return toString().hashCode();
    }

    @Override
    public int compareTo(EntityReference reference)
    {
        if (reference == null) {
            throw new NullPointerException("Provided reference should not be null");
        }

        if (reference == this) {
            return 0;
        }

        int cmp = compareParent(reference);
        if (cmp != 0) {
            return cmp;
        }

        if (!type.equals(reference.type)) {
            return type.compareTo(reference.type);
        }

        if (!name.equals(reference.name)) {
            return name.compareTo(reference.name);
        }

        return compareParameters(reference);
    }

    /**
     * Compare parent references of this reference and another reference.
     *
     * @param reference the other reference to be compare with
     * @return 0 if parents are equals, -1 if this reference has a lower parent, +1 otherwise
     */
    private int compareParent(EntityReference reference)
    {
        if (parent != null) {
            if (reference.parent == null) {
                return 1;
            }

            return parent.compareTo(reference.parent);
        }
        return (reference.parent == null) ? 0 : -1;
    }

    /**
     * Compare parameters of this reference and another reference.
     *
     * @param reference the other reference to be compare with
     * @return 0 if parameters are equals, -1 if this reference has lower parameters, +1 otherwise
     */
    @SuppressWarnings("unchecked")
    private int compareParameters(EntityReference reference)
    {
        if (parameters != null && reference.parameters == null) {
            return 1;
        }

        if (parameters != null) {
            for (Map.Entry<String, Serializable> entry : parameters.entrySet()) {
                Object obj = reference.parameters.get(entry.getKey());
                Object myobj = entry.getValue();
                if (myobj != null && myobj instanceof Comparable) {
                    if (obj == null) {
                        return 1;
                    }
                    return ((Comparable) myobj).compareTo(obj);
                }
            }
        }
        return (reference.parameters == null) ? 0 : -1;
    }
}
