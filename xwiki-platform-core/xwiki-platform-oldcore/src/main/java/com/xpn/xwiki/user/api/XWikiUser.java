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
package com.xpn.xwiki.user.api;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.web.Utils;

public class XWikiUser
{
    /**
     * @see com.xpn.xwiki.internal.model.reference.CurrentMixedStringDocumentReferenceResolver
     */
    private DocumentReferenceResolver<String> currentMixedDocumentReferenceResolver = Utils.getComponent(
        DocumentReferenceResolver.TYPE_STRING, "currentmixed");

    private String user;

    private boolean main;

    public XWikiUser(String user)
    {
        this(user, false);
    }

    public XWikiUser(String user, boolean main)
    {
        setUser(user);
        setMain(main);
    }

    public String getUser()
    {
        return this.user;
    }

    private DocumentReference getUserReference(XWikiContext context)
    {
        return this.currentMixedDocumentReferenceResolver.resolve(getUser());
    }

    public void setUser(String user)
    {
        this.user = user;
    }

    /**
     * Check if the user belongs to a group or not. This method only check direct membership (no recursive checking) in
     * the current wiki.
     *
     * @param groupName The group to check.
     * @param context The current {@link XWikiContext context}.
     * @return <tt>true</tt> if the user does belong to the specified group, false otherwise or if an exception occurs.
     * @throws XWikiException If an error occurs when checking the groups.
     * @since Platform-1.3
     */
    public boolean isUserInGroup(String groupName, XWikiContext context) throws XWikiException
    {
        if (!StringUtils.isEmpty(getUser())) {
            XWikiGroupService groupService = context.getWiki().getGroupService(context);

            DocumentReference groupReference = this.currentMixedDocumentReferenceResolver.resolve(groupName);

            Collection<DocumentReference> groups =
                groupService.getAllGroupsReferencesForMember(getUserReference(context), 0, 0, context);

            if (groups.contains(groupReference)) {
                return true;
            }
        }

        return false;
    }

    public boolean isMain()
    {
        return this.main;
    }

    public void setMain(boolean main)
    {
        this.main = main;
    }

    @Override
    public String toString()
    {
        return getUser();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (super.equals(obj)) {
            return true;
        }

        boolean equals;
        if (obj instanceof XWikiUser) {
            XWikiUser otherUser = (XWikiUser) obj;
            equals = otherUser.main == this.main && this.user.equals(otherUser.user);
        } else {
            equals = false;
        }

        return equals;
    }

    @Override
    public int hashCode()
    {
        HashCodeBuilder builder = new HashCodeBuilder();

        builder.appendSuper(super.hashCode());
        builder.append(main);
        builder.append(user);

        return builder.toHashCode();
    }
}
