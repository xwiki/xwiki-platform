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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.GroupSecurityReference;
import org.xwiki.security.UserSecurityReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.RightSet;
import org.xwiki.security.authorization.RuleState;
import org.xwiki.security.authorization.SecurityRule;
import org.xwiki.text.XWikiToStringStyle;

import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.GroupsClass;
import com.xpn.xwiki.objects.classes.UsersClass;

import org.apache.commons.lang3.builder.ToStringBuilder;


/**
 * Wrapper around xwiki rights objects to convert them into security rules.
 *
 * @version $Id$
 * @since 4.0M2
 */
public class XWikiSecurityRule implements SecurityRule
{
    /** The set of users. */
    private final Set<DocumentReference> users;

    /** The set of groups. */
    private final Set<DocumentReference> groups;

    /** The set of right levels. */
    private final RightSet rights;

    /** The state specified by this object. */
    private final RuleState state;

    /**
     * Construct a more manageable java object from the corresponding
     * xwiki object.
     * @param obj An xwiki rights object.
     * @param resolver A document reference resolver for user and group pages.
     * @param wikiReference The name of the current wiki.
     */
    protected XWikiSecurityRule(BaseObject obj, DocumentReferenceResolver<String> resolver,
        WikiReference wikiReference)
    {
        state = (obj.getIntValue(XWikiConstants.ALLOW_FIELD_NAME) == 1) ? RuleState.ALLOW : RuleState.DENY;
        users = new HashSet<DocumentReference>();
        groups = new HashSet<DocumentReference>();
        rights = new RightSet();

        String levels = obj.getStringValue("levels");
        String[] levelsarray = StringUtils.split(levels, " ,|");
        for (String s : levelsarray) {
            Right right = Right.toRight(s);
            if (right != Right.ILLEGAL) {
                rights.add(right);
            }
        }

        for (String user : UsersClass.getListFromString(obj.getStringValue(XWikiConstants.USERS_FIELD_NAME))) {
            DocumentReference ref = resolver.resolve(user, wikiReference);
            this.users.add(ref);
        }

        for (String group : GroupsClass.getListFromString(obj.getStringValue(XWikiConstants.GROUPS_FIELD_NAME))) {
            DocumentReference ref = resolver.resolve(group, wikiReference);
            this.groups.add(ref);
        }
    }

    /** 
     * Constructor used only by test code.
     * @param rights The set of rights.
     * @param state The state of this rights object.
     * @param users The set of users.
     * @param groups The set of groups.
     */
    protected XWikiSecurityRule(RightSet rights,
        RuleState state,
        Set<DocumentReference> users,
        Set<DocumentReference> groups)
    {
        this.users = users;
        this.groups = groups;
        this.rights = rights;
        this.state = state;
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
    public boolean equals(Object object)
    {
        if (object == this) {
            return true;
        }
        if (object == null || object.getClass() != getClass()) {
            return false;
        }

        XWikiSecurityRule other = (XWikiSecurityRule) object;

        return state == other.state
               && rights.equals(other.rights)
               && users.equals(other.users)
               && groups.equals(other.groups);
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder()
            .append(state)
            .append(rights)
            .append(users)
            .append(groups)
            .toHashCode();
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
