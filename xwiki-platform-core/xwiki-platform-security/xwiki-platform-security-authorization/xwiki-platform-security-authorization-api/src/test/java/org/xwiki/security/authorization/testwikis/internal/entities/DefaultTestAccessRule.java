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
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.RuleState;
import org.xwiki.security.authorization.testwikis.SecureTestEntity;
import org.xwiki.security.authorization.testwikis.TestAccessRule;
import org.xwiki.security.authorization.testwikis.TestEntity;

/**
 * Entity for access rule definitions.
 *
 * @version $Id$
 * @since 5.0M2
 */
public class DefaultTestAccessRule extends AbstractTestEntity implements TestAccessRule
{
    /** The type of reference used by this class. */
    public static final EntityType TYPE = EntityType.OBJECT;

    /** The reference to the user/group concerned by this access rule entity. */
    private final DocumentReference userReference;

    /** The right defined by this access rule entity. */
    private final Right right;

    /** The state defined by this access rule entity. */
    private final RuleState state;

    /** True for a user access rule, and false for a group access rule. */
    private final boolean isUser;

    /**
     * Create a new access rule entity.
     * @param user serialized reference to the user/group concerned by this access rule entity.
     * @param userReference reference to the user/group concerned by this access rule entity.
     * @param right right defined by this rule.
     * @param state state defined by this rule.
     * @param parent parent entity of this entity.
     */
    public DefaultTestAccessRule(String user, EntityReference userReference, Right right, boolean state,
        boolean isUser, TestEntity parent) {
        super(
            new EntityReference(String.format("%s@@%s@@%b", user, right.getName(), state),
                TYPE, parent.getReference()),
            parent);

        this.userReference = new DocumentReference(userReference);
        this.right = right;
        this.state = state ? RuleState.ALLOW : RuleState.DENY;
        this.isUser = isUser;
    }

    @Override
    protected void addToParent(TestEntity parent) {
        ((SecureTestEntity) parent).addSecurityRules(this);
    }

    @Override
    public EntityType getType()
    {
        return TYPE;
    }

    @Override
    public DocumentReference getUser()
    {
        return userReference;
    }

    @Override
    public Right getRight()
    {
        return right;
    }

    @Override
    public RuleState getState()
    {
        return state;
    }

    @Override
    public boolean isUser()
    {
        return isUser;
    }
}
