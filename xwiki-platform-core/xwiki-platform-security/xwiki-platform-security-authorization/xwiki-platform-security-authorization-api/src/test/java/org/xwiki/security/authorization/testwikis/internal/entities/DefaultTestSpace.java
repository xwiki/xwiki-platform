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

package org.xwiki.security.authorization.testwikis.internal.entities;

import java.util.Collection;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.security.authorization.testwikis.TestDocument;
import org.xwiki.security.authorization.testwikis.TestEntity;
import org.xwiki.security.authorization.testwikis.TestSpace;

/**
 * Entity representing a space.
 *
 * @version $Id$
 * @since 5.0M2
 */
public class DefaultTestSpace extends AbstractSecureTestEntity implements TestSpace
{
    /** The type of reference used by this class. */
    public static final EntityType TYPE = EntityType.SPACE;

    /** The alternate description of this entity. */
    private final String description;

    /**
     * Create a new space entity.
     * @param reference reference of document represented by this entity.
     * @param description alternate description of this entity.
     * @param parent parent entity of this entity.
     */
    public DefaultTestSpace(EntityReference reference, String description, TestEntity parent) {
        super(reference, parent);

        this.description = description;
    }

    @Override
    public EntityType getType()
    {
        return TYPE;
    }

    @Override
    public SpaceReference getSpaceReference()
    {
        return new SpaceReference(getReference());
    }

    @Override
    public TestDocument getDocument(String name)
    {
        return getDocument(new DocumentReference(name, getSpaceReference()));
    }

    @Override
    public TestDocument getDocument(DocumentReference reference)
    {
        return (TestDocument) getEntity(reference);
    }

    @Override
    public Collection<TestDocument> getDocuments()
    {
        return TypeFilteredCollection.getNewInstance(getEntities(), TestDocument.class);
    }

    @Override
    public String getDescription()
    {
        return description;
    }
}
