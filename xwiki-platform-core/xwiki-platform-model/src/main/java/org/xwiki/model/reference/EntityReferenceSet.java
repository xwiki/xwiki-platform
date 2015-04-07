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
    private static class EntityReferenceEntry
    {
        public EntityType type;

        public String name;

        public List<Map<String, Serializable>> parameters;

        public EntityType childrenType;

        public Map<String, EntityReferenceEntry> children;

        public EntityReferenceEntry()
        {
        }

        public EntityReferenceEntry(EntityType type, String name)
        {
            this.type = type;
            this.name = name;
        }

        public EntityReferenceEntry(EntityReferenceEntry entry)
        {
            this(entry.type, entry.name);

            this.childrenType = entry.childrenType;
            this.children = entry.children;
            this.parameters = entry.parameters;
        }

        public EntityReferenceEntry add()
        {
            return add(null, null, null);
        }

        public void add(EntityReferenceEntry entry)
        {
            if (this.children == null) {
                this.children = new HashMap<String, EntityReferenceEntry>();
            }

            this.children.put(entry.name, entry);
        }

        public EntityReferenceEntry add(EntityType entityType, String name, Map<String, Serializable> entityParameters)
        {
            if (this.children == null) {
                this.children = new HashMap<String, EntityReferenceEntry>();
            }

            EntityReferenceEntry entry = this.children.get(name);

            if (entry == null) {
                entry = new EntityReferenceEntry(entityType, name);

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

        private void addParameters(Map<String, Serializable> entityParameters)
        {
            if (!entityParameters.isEmpty()) {
                if (this.parameters == null) {
                    this.parameters = new ArrayList<Map<String, Serializable>>();
                }

                this.parameters.add(entityParameters);
            }
        }

        public void reset()
        {
            this.childrenType = null;
            this.children = null;
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
    }

    private EntityReferenceEntry includes;

    private EntityReferenceEntry excludes;

    private void add(EntityReference reference, EntityReferenceEntry entry)
    {
        EntityReferenceEntry currentEntry = entry;
        for (EntityReference element : reference.getReversedReferenceChain()) {
            // Move to the right level in the tree
            while (currentEntry.childrenType != null && currentEntry.childrenType.compareTo(element.getType()) < 0) {
                currentEntry = currentEntry.add();
            }

            if (currentEntry.childrenType == null || currentEntry.childrenType == element.getType()) {
                currentEntry.childrenType = element.getType();
                currentEntry = currentEntry.add(element.getType(), element.getName(), element.getParameters());
            } else {
                EntityReferenceEntry newEntry = new EntityReferenceEntry(currentEntry);
                currentEntry.reset();
                currentEntry.add(newEntry);
            }
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

            // Children have same type
            if (currentEntry.childrenType == element.getType()) {
                EntityReferenceEntry nameEntry = currentEntry.children.get(element.getName());

                if (nameEntry == null) {
                    currentEntry = currentEntry.children.get(null);

                    if (currentEntry == null) {
                        return false;
                    }
                } else {
                    currentEntry = nameEntry;
                }

                if (!currentEntry.matches(element)) {
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

            // Children have same type
            if (currentEntry.childrenType == element.getType()) {
                EntityReferenceEntry nameEntry = currentEntry.children.get(element.getName());

                if (nameEntry == null) {
                    currentEntry = currentEntry.children.get(null);

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
            || (currentEntry.parameters != null && CollectionUtils.intersection(
                getParametersKeys(currentEntry.parameters), reference.getParameters().keySet()).isEmpty());
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
