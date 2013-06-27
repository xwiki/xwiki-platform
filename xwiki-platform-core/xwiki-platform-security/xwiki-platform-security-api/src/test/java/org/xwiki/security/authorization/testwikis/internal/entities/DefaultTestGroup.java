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

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.authorization.testwikis.TestEntity;
import org.xwiki.security.authorization.testwikis.TestGroup;

/**
 * Entity representing links between groups and users.
 * Each user keep a collection of these entities for their group membership.
 *
 * @version $Id$
 * @since 5.0M2
 */
public class DefaultTestGroup extends AbstractTestEntity implements TestGroup
{
    /** The type of reference used by this class. */
    public static final EntityType TYPE = EntityType.OBJECT;

    /** The reference to the group this entity link to. */
    private final DocumentReference groupReference;

    /**
     * Create a new link entity for a given group.
     * @param group serialized reference to the group this entity link to.
     * @param groupReference reference to the group this entity link to.
     * @param parent parent entity of this entity.
     */
    public DefaultTestGroup(String group, EntityReference groupReference, TestEntity parent) {
        super(new EntityReference(group, TYPE, parent.getReference()), parent);

        this.groupReference = new DocumentReference(groupReference);
    }

    @Override
    public EntityType getType()
    {
        return TYPE;
    }

    @Override
    public DocumentReference getGroupReference()
    {
        return groupReference;
    }
}
