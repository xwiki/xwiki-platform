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

import java.util.HashMap;
import java.util.Map;

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
        public String entityName;

        public EntityType childrenType;

        public Map<String, EntityReferenceEntry> children;

        public EntityReferenceEntry()
        {
        }

        public EntityReferenceEntry(String entityName)
        {
            this.entityName = entityName;
        }

        public EntityReferenceEntry(EntityReferenceEntry entry)
        {
            this.entityName = entry.entityName;
            this.childrenType = entry.childrenType;
            this.children = entry.children;
        }

        public EntityReferenceEntry add()
        {
            return add((String) null);
        }

        public void add(EntityReferenceEntry entry)
        {
            if (this.children == null) {
                this.children = new HashMap<String, EntityReferenceEntry>();
            }

            this.children.put(entry.entityName, entry);
        }

        public EntityReferenceEntry add(String name)
        {
            if (this.children == null) {
                this.children = new HashMap<String, EntityReferenceEntry>();
            }

            EntityReferenceEntry entry = this.children.get(name);

            if (entry == null) {
                entry = new EntityReferenceEntry(name);

                this.children.put(name, entry);
            }

            return entry;
        }

        public void reset()
        {
            this.childrenType = null;
            this.children = null;
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
                currentEntry.add(reference.getName());
            } else {
                EntityReferenceEntry newEntry = new EntityReferenceEntry(currentEntry);
                currentEntry.reset();
                currentEntry.add(newEntry);
            }
            currentEntry.childrenType = reference.getType();
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

    private boolean matches(EntityReference reference, EntityReferenceEntry entry, boolean def)
    {
        if (entry == null) {
            return def;
        }

        EntityReferenceEntry currentEntry = entry;
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
            }
        }

        return true;
    }

    /**
     * @param reference the reference
     * @return the entity reference set
     */
    public boolean matches(EntityReference reference)
    {
        return matches(reference, this.includes, true) && !matches(reference, this.excludes, false);
    }
}
