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
import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.testwikis.TestDocument;
import org.xwiki.security.authorization.testwikis.TestEntity;
import org.xwiki.security.authorization.testwikis.TestRequiredRight;

/**
 * Entity for required right definition.
 *
 * @version $Id$
 */
public class DefaultTestRequiredRight extends AbstractTestEntity implements TestRequiredRight
{
    /** The type of reference used by this class. */
    public static final EntityType TYPE = EntityType.OBJECT;

    private final Right right;

    private final EntityType scope;

    /**
     * Create a new required right entity.
     *
     * @param right right to require
     * @param scope scope on which the right shall be required
     * @param parent parent entity of this entity.
     */
    public DefaultTestRequiredRight(Right right, EntityType scope, TestEntity parent)
    {
        super(
            new EntityReference(String.format("%s@@%s", right.getName(), scope),
                TYPE, parent.getReference()), parent);

        this.right = right;
        this.scope = scope;
    }

    @Override
    protected void addToParent(TestEntity parent)
    {
        ((TestDocument) parent).addRequiredRight(this);
    }

    @Override
    public Right getRight()
    {
        return this.right;
    }

    @Override
    public EntityType getScope()
    {
        return this.scope;
    }

    @Override
    public EntityType getType()
    {
        return TYPE;
    }
}
