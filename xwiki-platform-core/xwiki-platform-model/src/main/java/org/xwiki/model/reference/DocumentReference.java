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

import org.xwiki.model.EntityType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a reference to a document (wiki, space and page names).
 * 
 * @version $Id$
 * @since 2.2M1
 */
public class DocumentReference extends EntityReference
{
    /**
     * Special constructor that transforms a generic entity reference into a {@link DocumentReference}. It checks the
     * validity of the passed reference (ie correct type and correct parent).
     *
     * @exception IllegalArgumentException if the passed reference is not a valid document reference
     */
    public DocumentReference(EntityReference reference)
    {
        super(reference.getName(), reference.getType(), reference.getParent());
    }

    public DocumentReference(String wikiName, String spaceName, String pageName)
    {
        this(pageName, new SpaceReference(spaceName, new WikiReference(wikiName)));
    }

    public DocumentReference(String wikiName, List<String> spaceNames, String pageName)
    {
        super(pageName, EntityType.DOCUMENT, constructSpaceReference(wikiName, spaceNames));
    }

    public DocumentReference(String pageName, SpaceReference parent)
    {
        super(pageName, EntityType.DOCUMENT, parent);
    }

    /**
     * {@inheritDoc}
     *
     * Overridden in order to verify the validity of the passed parent
     *
     * @see org.xwiki.model.reference.EntityReference#setParent(EntityReference)
     * @exception IllegalArgumentException if the passed parent is not a valid document reference parent (ie a space
     *            reference)
     */
    @Override public void setParent(EntityReference parent)
    {
        if (parent == null || parent.getType() != EntityType.SPACE) {
            throw new IllegalArgumentException("Invalid parent reference [" + parent + "] for a document reference");
        }

        super.setParent(new SpaceReference(parent));
    }

    /**
     * {@inheritDoc}
     *
     * Overridden in order to verify the validity of the passed type
     *
     * @see org.xwiki.model.reference.EntityReference#setType(org.xwiki.model.EntityType)
     * @exception IllegalArgumentException if the passed type is not a document type
     */
    @Override public void setType(EntityType type)
    {
        if (type != EntityType.DOCUMENT) {
            throw new IllegalArgumentException("Invalid type [" + type + "] for a document reference");
        }

        super.setType(EntityType.DOCUMENT);
    }

    public WikiReference getWikiReference()
    {
        return (WikiReference) extractReference(EntityType.WIKI);
    }

    public void setWikiReference(WikiReference newWikiReference)
    {
        EntityReference wikiReference = extractReference(EntityType.WIKI);
        wikiReference.getChild().setParent(newWikiReference);
    }

    public SpaceReference getLastSpaceReference()
    {
        return (SpaceReference) extractReference(EntityType.SPACE);
    }

    public List<SpaceReference> getSpaceReferences()
    {
        List<SpaceReference> references = new ArrayList<SpaceReference>();

        EntityReference reference = this;
        while (reference != null) {
            if (reference.getType() == EntityType.SPACE) {
                references.add((SpaceReference) reference);
            }
            reference = reference.getParent();
        }
        // Reverse the array so that the last entry is the parent of the Document Reference
        Collections.reverse(references);

        return references;
    }

    private static EntityReference constructSpaceReference(String wikiName, List<String> spaceNames)
    {
        EntityReference spaceReference = null;
        EntityReference parent = new EntityReference(wikiName, EntityType.WIKI);
        for (String spaceName : spaceNames) {
            spaceReference = new EntityReference(spaceName, EntityType.SPACE, parent);
            parent = spaceReference;
        }
        return spaceReference;
    }
}
