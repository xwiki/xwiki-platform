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
    public DocumentReference(EntityReference reference)
    {
        super(reference.getName(), EntityType.DOCUMENT, reference.getParent());
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

    public WikiReference getWikiReference()
    {
        return new WikiReference(extractReference(EntityType.WIKI));
    }

    public SpaceReference getLastSpaceReference()
    {
        return new SpaceReference(extractReference(EntityType.SPACE));
    }

    public List<SpaceReference> getSpaceReferences()
    {
        List<SpaceReference> references = new ArrayList<SpaceReference>();

        EntityReference reference = this;
        while (reference != null) {
            if (reference.getType() == EntityType.SPACE) {
                references.add(new SpaceReference(reference));
            }
            reference = reference.getParent();
        }
        // Reverse the array so that the last entry is the parent of the Document Reference
        Collections.reverse(references);

        return references;
    }

    private static final EntityReference constructSpaceReference(String wikiName, List<String> spaceNames)
    {
        EntityReference spaceReference = null;
        EntityReference parent = new WikiReference(wikiName);
        for (String spaceName : spaceNames) {
            spaceReference = new EntityReference(spaceName, EntityType.SPACE, parent);
            parent = spaceReference;
        }
        return spaceReference;
    }
}
