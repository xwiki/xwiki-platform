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

import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.authorization.testwikis.TestEntity;
import org.xwiki.security.authorization.testwikis.TestGroup;
import org.xwiki.security.authorization.testwikis.TestUserDocument;

/**
 * Entity representing a document that represent a user.
 *
 * @version $Id$
 * @since 5.0M2
 */
public class DefaultTestUserDocument extends DefaultTestDocument implements TestUserDocument
{
    /**
     * Create a new document entity for a user.
     * @param reference reference of document represented by this entity.
     * @param creator creator of this document.
     * @param description alternate description of this entity.
     * @param parent parent entity of this entity.
     */
    public DefaultTestUserDocument(EntityReference reference, EntityReference creator, String description,
        TestEntity parent) {
        super(reference, creator, description, parent);
    }

    @Override
    public Collection<TestGroup> getGroups()
    {
        return TypeFilteredCollection.getNewInstance(getEntities(), TestGroup.class);
    }
}
