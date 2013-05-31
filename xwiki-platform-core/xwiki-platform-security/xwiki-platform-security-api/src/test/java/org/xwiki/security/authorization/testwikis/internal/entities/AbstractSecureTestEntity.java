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

import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.authorization.testwikis.SecureTestEntity;
import org.xwiki.security.authorization.testwikis.TestAccessRule;
import org.xwiki.security.authorization.testwikis.TestEntity;

/**
 * Base class for all test entities that support security rules.
 *
 * @version $Id$
 * @since 5.0M2
 */
public abstract class AbstractSecureTestEntity extends AbstractTestEntity implements SecureTestEntity
{
    /** Map of security rules. */
    private final Map<EntityReference, TestAccessRule> rules = new HashMap<EntityReference, TestAccessRule>();

    /**
     * Create a new secure entity.
     * @param reference reference for this entity.
     * @param parent the parent entity of this entity.
     */
    AbstractSecureTestEntity(EntityReference reference, TestEntity parent) {
        super(reference, parent);
    }

    @Override
    public void addSecurityRules(TestAccessRule entity)
    {
        rules.put(entity.getReference(), entity);
    }

    @Override
    public Collection<TestAccessRule> getAccessRules()
    {
        return rules.values();
    }
}
