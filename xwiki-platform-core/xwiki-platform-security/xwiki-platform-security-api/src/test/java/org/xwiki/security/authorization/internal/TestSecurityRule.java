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
package org.xwiki.security.authorization.internal;

import java.util.HashSet;
import java.util.Set;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.GroupSecurityReference;
import org.xwiki.security.UserSecurityReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.RightSet;
import org.xwiki.security.authorization.RuleState;
import org.xwiki.security.authorization.SecurityRule;
import org.xwiki.text.XWikiToStringStyle;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * This security rule stub implementation is only used for testing.
 *
 * @version $Id$
 * @since 4.0M2
 */
public class TestSecurityRule implements SecurityRule
{
    /** The set of users. */
    private final Set<DocumentReference> users = new HashSet<DocumentReference>();

    /** The set of groups. */
    private final Set<DocumentReference> groups = new HashSet<DocumentReference>();

    /** The set of right levels. */
    private final RightSet rights = new RightSet();

    /** The state specified by this object. */
    private final RuleState state;

    public TestSecurityRule(RuleState state)
    {
        this.state = state;
    }
    
    void add(UserSecurityReference reference) {
        users.add(reference.getOriginalDocumentReference());
    }

    void add(GroupSecurityReference reference) {
        groups.add(reference.getOriginalDocumentReference());
    }
    
    void add(Right right) {
        rights.add(right);
    }

    void addAllUser(Set<UserSecurityReference> referenceSet) {
        for(UserSecurityReference reference : referenceSet) {
            users.add(reference.getOriginalDocumentReference());
        }
    }

    void addAllGroup(Set<GroupSecurityReference> referenceSet) {
        for(GroupSecurityReference reference : referenceSet) {
            groups.add(reference.getOriginalDocumentReference());
        }
    }

    void addAllRight(Set<Right> rightSet) {
        for(Right right : rightSet) {
            rights.add(right);
        }
    }

    public boolean isEmpty() {
        return rights.isEmpty();
    }

    @Override
    public boolean match(Right right)
    {
        return rights.contains(right);
    }

    @Override
    public boolean match(GroupSecurityReference group)
    {
        return groups.contains(group.getOriginalReference());
    }

    @Override
    public boolean match(UserSecurityReference user)
    {
        return users.contains(user.getOriginalReference());
    }

    @Override
    public RuleState getState()
    {
        return state;
    }

    @Override
    public boolean equals(Object other)
    {
        return other == this
            || (other.getClass().isInstance(this)
            && state == ((TestSecurityRule) other).state
            && rights.equals(((TestSecurityRule) other).rights)
            && users.equals(((TestSecurityRule) other).users)
            && groups.equals(((TestSecurityRule) other).groups));
    }

    @Override
    public int hashCode()
    {
        return state.hashCode() + rights.hashCode() + users.hashCode() + groups.hashCode();
    }

    @Override
    public String toString()
    {
        ToStringBuilder builder = new ToStringBuilder(this, new XWikiToStringStyle());

        return builder
            .append("State" , state)
            .append("Rights", rights)
            .append("Users" , users)
            .append("Groups", groups)
            .toString();
    }
}
