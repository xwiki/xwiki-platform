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
import java.util.HashMap;
import java.util.Map;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.security.authorization.testwikis.TestDocument;
import org.xwiki.security.authorization.testwikis.TestEntity;
import org.xwiki.security.authorization.testwikis.TestRequiredRight;
import org.xwiki.security.authorization.testwikis.internal.parser.XWikiConstants;

/**
 * Entity representing documents.
 *
 * @version $Id$
 * @since 5.0M2
 */
public class DefaultTestDocument extends AbstractSecureTestEntity implements TestDocument
{
    /** The type of reference used by this class. */
    public static final EntityType TYPE = EntityType.DOCUMENT;

    /** The alternate description of this entity. */
    private final String description;

    /** The creator of this document. */
    private final DocumentReference creator;

    /** Map of security rules. */
    private final Map<EntityReference, TestRequiredRight> requiredRights = new HashMap<>();

    private final boolean enforceRequiredRights;

    /**
     * Create a new document entity.
     * @param reference reference of document represented by this entity.
     * @param creator creator of this document.
     * @param description alternate description of this entity.
     * @param parent parent entity of this entity.
     */
    public DefaultTestDocument(EntityReference reference, EntityReference creator, String description,
        TestEntity parent)
    {
        this(reference, creator, description, false, parent);
    }

    /**
     * Create a new document entity.
     * @param reference reference of document represented by this entity.
     * @param creator creator of this document.
     * @param description alternate description of this entity.
     * @param enforceRequiredRights enforce required rights
     * @param parent parent entity of this entity.
     */
    public DefaultTestDocument(EntityReference reference, EntityReference creator, String description,
        boolean enforceRequiredRights, TestEntity parent)
    {
        super(reference, parent);

        this.creator = (creator != null) ? new DocumentReference(creator)
            : new DocumentReference(XWikiConstants.SUPERADMIN, new SpaceReference(XWikiConstants.XWIKI_SPACE,
            getDocumentReference().getWikiReference()));
        this.description = description;
        this.enforceRequiredRights = enforceRequiredRights;
    }

    @Override
    public EntityType getType()
    {
        return TYPE;
    }

    @Override
    public DocumentReference getDocumentReference()
    {
        return new DocumentReference(getReference());
    }

    @Override
    public DocumentReference getCreator()
    {
        return creator;
    }

    @Override
    public String getDescription()
    {
        return description;
    }

    @Override
    public boolean isEnforceRequiredRights()
    {
        return this.enforceRequiredRights;
    }

    @Override
    public void addRequiredRight(TestRequiredRight requiredRight)
    {
        this.requiredRights.put(requiredRight.getReference(), requiredRight);
    }

    @Override
    public Collection<TestRequiredRight> getRequiredRights()
    {
        return this.requiredRights.values();
    }
}
