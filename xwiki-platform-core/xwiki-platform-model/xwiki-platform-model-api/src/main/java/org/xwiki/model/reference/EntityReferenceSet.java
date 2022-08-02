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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.xwiki.model.EntityType;

/**
 * A set of entities references.
 * 
 * @version $Id$
 * @since 5.2M2
 */
public class EntityReferenceSet
{
    private static class EntityReferenceEntryChildren
    {
        public EntityType childrenType;

        public Map<String, EntityReferenceEntry> children;

        public EntityReferenceEntry add(String name, Map<String, Serializable> entityParameters)
        {
            if (this.children == null) {
                this.children = new HashMap<String, EntityReferenceEntry>();
            }

            EntityReferenceEntry entry = this.children.get(name);

            if (entry == null) {
                entry = new EntityReferenceEntry(this.childrenType, name);

                if (entityParameters != null) {
                    entry.addParameters(entityParameters);
                }

                this.children.put(name, entry);
            } else {
                if (!entry.matches(entityParameters)) {
                    entry.addParameters(entityParameters);
                }
            }

            return entry;
        }

        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder();

            builder.append(this.childrenType);
            builder.append('(');
            builder.append(this.children);

            return builder.toString();
        }
    }

    private static class EntityReferenceEntry
    {
        public EntityType type;

        public String name;

        public List<Map<String, Serializable>> parameters;

        public Map<EntityType, EntityReferenceEntryChildren> children;

        public EntityReferenceEntry()
        {
        }

        public EntityReferenceEntry(EntityType type, String name)
        {
            this.type = type;
            this.name = name;
        }

        public EntityReferenceEntry add()
        {
            return add(null, null, null);
        }

        public EntityReferenceEntryChildren getChildren(EntityType entityType, boolean create)
        {
            if (this.children == null) {
                if (!create) {
                    return null;
                }

                this.children = new HashMap<>();
            }

            EntityReferenceEntryChildren child = this.children.get(entityType);

            if (child == null) {
                if (!create) {
                    return null;
                }

                child = new EntityReferenceEntryChildren();
                child.childrenType = entityType;
                this.children.put(entityType, child);
            }

            return child;
        }

        public EntityReferenceEntryChildren getClosestChildren(EntityType entityType)
        {
            EntityReferenceEntryChildren typedChildren = getChildren(entityType, false);

            if (typedChildren != null) {
                return typedChildren;
            }

            // Search for the closest

            if (this.children == null) {
                return null;
            }

            for (Map.Entry<EntityType, EntityReferenceEntryChildren> entry : this.children.entrySet()) {
                EntityReferenceEntryChildren typedChilrendEntry = entry.getValue();

                if (typedChilrendEntry.childrenType.isAllowedAncestor(entityType)) {
                    // Only return a potential child of the passed type
                    if (typedChildren == null
                        || typedChildren.childrenType.isAllowedAncestor(typedChilrendEntry.childrenType)) {
                        typedChildren = typedChilrendEntry;
                    }
                }
            }

            return typedChildren;
        }

        public EntityReferenceEntry add(EntityType entityType, String entityName,
            Map<String, Serializable> entityParameters)
        {
            EntityReferenceEntryChildren child = getChildren(entityType, true);

            return child.add(entityName, entityParameters);
        }

        private void addParameters(Map<String, Serializable> entityParameters)
        {
            if (!entityParameters.isEmpty()) {
                if (this.parameters == null) {
                    this.parameters = new ArrayList<Map<String, Serializable>>();
                }

                this.parameters.add(entityParameters);
            }
        }

        public boolean matches(EntityReference reference)
        {
            return matches(reference.getParameters());
        }

        public boolean matches(Map<String, Serializable> referenceParameters)
        {
            if (parameters == null) {
                return true;
            }

            boolean matches = parameters.isEmpty();

            for (Map<String, Serializable> parametersMap : parameters) {
                matches |= matches(referenceParameters, parametersMap);
            }

            return matches;
        }

        private boolean matches(Map<String, Serializable> referenceParameters, Map<String, Serializable> map)
        {
            for (Map.Entry<String, Serializable> entry : map.entrySet()) {
                if (referenceParameters.containsKey(entry.getKey())) {
                    if (!Objects.equals(entry.getValue(), referenceParameters.get(entry.getKey()))) {
                        return false;
                    }
                }
            }

            return true;
        }

        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder();

            if (this.type != null) {
                builder.append(this.type);
                builder.append(':');
                builder.append(this.name);
            }

            if (this.children != null) {
                builder.append('(');
                builder.append(this.children);
                builder.append(')');
            }

            return builder.toString();
        }
    }

    private EntityReferenceEntry includes;

    private EntityReferenceEntry excludes;

    private void add(EntityReference reference, EntityReferenceEntry entry)
    {
        EntityReferenceEntry currentEntry = entry;

        for (EntityReference element : reference.getReversedReferenceChain()) {
            // Move the first element to the right level in the tree
            if (currentEntry == entry) {
                while (currentEntry.children != null && currentEntry.getClosestChildren(element.getType()) == null) {
                    currentEntry = currentEntry.add();
                }
            }

            currentEntry = currentEntry.add(element.getType(), element.getName(), element.getParameters());
        }
    }

    /**
     * @param reference the reference
     * @return the entity reference set
     */
    public EntityReferenceSet includes(EntityReference reference)
    {
        if (this.includes == null) {
            this.includes = new EntityReferenceEntry();
        }

        add(reference, this.includes);

        return this;
    }

    /**
     * @param reference the reference
     * @return the entity reference set
     */
    public EntityReferenceSet excludes(EntityReference reference)
    {
        if (this.excludes == null) {
            this.excludes = new EntityReferenceEntry();
        }

        add(reference, this.excludes);

        return this;
    }

    private boolean matchesInclude(EntityReference reference)
    {
        if (this.includes == null) {
            return true;
        }

        EntityReferenceEntry currentEntry = this.includes;
        for (EntityReference element : reference.getReversedReferenceChain()) {
            // No children
            if (currentEntry.children == null) {
                return true;
            }

            EntityReferenceEntryChildren typedChildren = currentEntry.children.get(element.getType());

            if (typedChildren != null) {
                // Children have same type
                EntityReferenceEntry nameEntry = typedChildren.children.get(element.getName());

                if (nameEntry == null) {
                    currentEntry = typedChildren.children.get(null);

                    if (currentEntry == null) {
                        return false;
                    }
                } else {
                    currentEntry = nameEntry;
                }

                if (!currentEntry.matches(element)) {
                    return false;
                }
            } else {
                if (currentEntry.name != null || currentEntry.getClosestChildren(element.getType()) == null) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean matchesExclude(EntityReference reference)
    {
        if (this.excludes == null) {
            return true;
        }

        EntityReferenceEntry currentEntry = this.excludes;
        for (EntityReference element : reference.getReversedReferenceChain()) {
            // No children
            if (currentEntry.children == null) {
                return false;
            }

            EntityReferenceEntryChildren typedChildren = currentEntry.children.get(element.getType());

            if (typedChildren != null) {
                // Children have same type
                EntityReferenceEntry nameEntry = typedChildren.children.get(element.getName());

                if (nameEntry == null) {
                    currentEntry = typedChildren.children.get(null);

                    if (currentEntry == null) {
                        return true;
                    }
                } else {
                    currentEntry = nameEntry;
                }

                if (!currentEntry.matches(element)) {
                    return true;
                }
            }
        }

        // Check if the reference is shorter than the exclude(s)
        return currentEntry.children != null
            // Check if the excluded parameters are the same as the one the reference contains
            || (currentEntry.parameters != null && CollectionUtils
                .intersection(getParametersKeys(currentEntry.parameters), reference.getParameters().keySet())
                .isEmpty());
    }

    private Set<String> getParametersKeys(List<Map<String, Serializable>> parameters)
    {
        if (parameters.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> set = new HashSet<String>();

        for (Map<String, Serializable> map : parameters) {
            set.addAll(map.keySet());
        }

        return set;
    }

    /**
     * @param reference the reference
     * @return the entity reference set
     */
    public boolean matches(EntityReference reference)
    {
        return matchesInclude(reference) && matchesExclude(reference);
    }
}
