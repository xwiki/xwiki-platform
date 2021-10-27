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
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authorization.testwikis.TestDocument;
import org.xwiki.security.authorization.testwikis.TestEntity;
import org.xwiki.security.authorization.testwikis.TestGroupDocument;
import org.xwiki.security.authorization.testwikis.TestSpace;
import org.xwiki.security.authorization.testwikis.TestUserDocument;
import org.xwiki.security.authorization.testwikis.TestWiki;
import org.xwiki.security.authorization.testwikis.internal.parser.XWikiConstants;

/**
 * Entity representing a wiki.
 *
 * @version $Id$
 * @since 5.0M2
 */
public class DefaultTestWiki extends AbstractSecureTestEntity implements TestWiki
{
    /** The type of reference used by this class. */
    public static final EntityType TYPE = EntityType.WIKI;

    /** When true, this entity represent the main wiki. */
    private final boolean isMainWiki;

    /** The owner of this wiki. */
    private final DocumentReference owner;

    /** The alternate description of this entity. */
    private final String description;

    /**
     * Create a new wiki entity.
     * @param reference reference of document represented by this entity.
     * @param isMainWiki true if this entity represent the main wiki.
     * @param owner owner of this wiki.
     * @param description alternate description of this entity.
     * @param parent parent entity of this entity.
     */
    public DefaultTestWiki(EntityReference reference, boolean isMainWiki, EntityReference owner, String description,
        TestEntity parent) {
        super(reference, parent);

        this.isMainWiki = isMainWiki;
        this.owner = (owner != null) ? new DocumentReference(owner)
                : new DocumentReference(XWikiConstants.SUPERADMIN, new SpaceReference(XWikiConstants.XWIKI_SPACE,
                    getWikiReference()));
        ;
        this.description = description;

        if (isMainWiki) {
            ((DefaultTestDefinition) parent).setMainWiki(this);
        }
    }

    @Override
    public EntityType getType()
    {
        return TYPE;
    }

    @Override
    public WikiReference getWikiReference()
    {
        return new WikiReference(getReference());
    }

    @Override
    public DocumentReference getOwner()
    {
        return owner;
    }

    @Override
    public boolean isMainWiki()
    {
        return isMainWiki;
    }

    @Override
    public TestSpace getSpace(String name)
    {
        return getSpace(new SpaceReference(name, getWikiReference()));
    }

    @Override
    public TestSpace getSpace(SpaceReference reference)
    {
        return (TestSpace) getEntity(reference);
    }

    @Override
    public Collection<TestSpace> getSpaces()
    {
        return TypeFilteredCollection.getNewInstance(getEntities(), TestSpace.class);
    }

    @Override
    public String getDescription()
    {
        return description;
    }

    @Override
    public TestUserDocument getUser(String name)
    {
        SpaceReference spaceRef = new SpaceReference(XWikiConstants.XWIKI_SPACE, getWikiReference());
        TestSpace space = getSpace(spaceRef);
        if (space == null) {
            return null;
        }
        TestDocument user = space.getDocument(new DocumentReference(name, spaceRef));
        return (user instanceof TestUserDocument) ? (TestUserDocument) user : null;
    }

    @Override
    public TestGroupDocument getGroup(String name)
    {
        TestDocument group = getUser(name);
        return (group instanceof TestGroupDocument) ? (TestGroupDocument) group : null;
    }

    @Override
    public Collection<TestUserDocument> getUsers()
    {
        TestSpace space = getSpace(new SpaceReference(XWikiConstants.XWIKI_SPACE, getWikiReference()));
        return TypeFilteredCollection.getNewInstance(space.getEntities(), TestUserDocument.class);
    }

    @Override
    public Collection<TestGroupDocument> getGroups()
    {
        TestSpace space = getSpace(new SpaceReference(XWikiConstants.XWIKI_SPACE, getWikiReference()));
        return TypeFilteredCollection.getNewInstance(space.getEntities(), TestGroupDocument.class);
    }

}
