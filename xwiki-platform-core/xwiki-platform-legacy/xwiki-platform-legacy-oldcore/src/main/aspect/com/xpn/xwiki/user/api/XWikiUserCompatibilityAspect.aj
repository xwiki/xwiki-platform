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
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * Add a backward compatibility layer to the {@link XWikiUser} class.
 *
 * @version $Id$
 * @since 18.7.0RC1
 */
public privileged aspect XWikiUserCompatibilityAspect
{
    /**
     * Check if the user belongs to a group or not. This method only check direct membership (no recursive checking) in
     * the current wiki.
     *
     * @param groupName The group to check.
     * @param context The current {@link XWikiContext context}.
     * @return {@code true} if the user does belong to the specified group, false otherwise or if an exception occurs.
     * @throws XWikiException If an error occurs when checking the groups.
     * @since 1.3
     * @deprecated use the org.xwiki.user.group.GroupManager component instead. Unlike this method, it takes a group
     *             reference (and is thus not restricted to the current wiki) and it can optionally take sub-groups
     *             into account
     */
    @Deprecated(since = "18.7.0RC1")
    public boolean XWikiUser.isUserInGroup(String groupName, XWikiContext context) throws XWikiException
    {
        if (!StringUtils.isEmpty(getUser())) {
            XWikiGroupService groupService = context.getWiki().getGroupService(context);

            DocumentReference groupReference = getCurrentMixedDocumentReferenceResolver().resolve(groupName);

            Collection<DocumentReference> groups =
                groupService.getAllGroupsReferencesForMember(getUserReference(), 0, 0, context);

            if (groups.contains(groupReference)) {
                return true;
            }
        }

        return false;
    }
}
