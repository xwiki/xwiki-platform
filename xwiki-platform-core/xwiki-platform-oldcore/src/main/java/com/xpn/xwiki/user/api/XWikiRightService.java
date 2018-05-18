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

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

public interface XWikiRightService
{
    /**
     * The Superadmin username.
     */
    String SUPERADMIN_USER = "superadmin";

    /**
     * The Superadmin full name.
     */
    String SUPERADMIN_USER_FULLNAME = "XWiki." + SUPERADMIN_USER;

    /**
     * The Guest username.
     */
    String GUEST_USER = "XWikiGuest";

    /**
     * The Guest full name.
     */
    String GUEST_USER_FULLNAME = "XWiki." + GUEST_USER;

    /**
     * The AllGroup username.
     */
    String ALLGROUP_GROUP = "XWikiAllGroup";

    /**
     * The AllGroup full name.
     */
    String ALLGROUP_GROUP_FULLNAME = "XWiki." + ALLGROUP_GROUP;

    /**
     * @param userReference the user reference
     * @return true if the passed reference belong to superadmin
     * @since 9.2RC1
     */
    static boolean isSuperAdmin(EntityReference userReference)
    {
        return userReference != null
            && StringUtils.equalsIgnoreCase(userReference.getName(), XWikiRightService.SUPERADMIN_USER);
    }

    /**
     * @param userReference the user reference
     * @return true if the passed reference belong to superadmin
     * @since 9.2RC1
     */
    static boolean isGuest(EntityReference userReference)
    {
        return userReference == null
            || StringUtils.equalsIgnoreCase(userReference.getName(), XWikiRightService.GUEST_USER);
    }

    /**
     * Checks if the wiki current user has the right to execute (@code action} on the document {@code doc}, along with
     * redirecting to the login if it's not the case and there is no logged in user (the user is the guest user).
     *
     * @param action the action to be executed on the document
     * @param doc the document to perform action on
     * @param context the xwiki context in which to perform the verification (from which to get the user, for example)
     * @return {@code true} if the user has right to execute {@code action} on {@code doc}, {@code false} otherwise
     *         <strong> and requests the login from the authentication service (redirecting to the login page in the
     *         case of a form authenticator, for example) when no user is logged in. </strong>
     * @throws XWikiException if something goes wrong during the rights checking process
     */
    boolean checkAccess(String action, XWikiDocument doc, XWikiContext context) throws XWikiException;

    /**
     * Verifies if the user identified by {@code username} has the access level identified by {@code right} on the
     * document with the name {@code docname}.
     *
     * @param right the access level to check (for example, 'view' or 'edit' or 'comment').
     * @param username the name of the user to check the right for
     * @param docname the document on which to check the right
     * @param context the xwiki context in which to perform the verification
     * @return {@code true} if the user has the specified right on the document, {@code false} otherwise
     * @throws XWikiException if something goes wrong during the rights checking process
     * @deprecated since 4.0M2, use
     *             {@link org.xwiki.security.authorization.AuthorizationManager#hasAccess(org.xwiki.security.authorization.Right, org.xwiki.model.reference.DocumentReference, org.xwiki.model.reference.EntityReference)}
     *             instead
     */
    @Deprecated
    boolean hasAccessLevel(String right, String username, String docname, XWikiContext context) throws XWikiException;

    /**
     * Checks if the author of the context document (last editor of the content of the document) has programming rights
     * (used to determine if the protected calls in the script contained in the document should be executed or not).
     *
     * @param context the xwiki context of this request
     * @return {@code true} if the author of the context document has programming rights, {@code false} otherwise.
     * @deprecated since 6.1RC1, use
     *             {@link org.xwiki.security.authorization.ContextualAuthorizationManager#hasAccess(org.xwiki.security.authorization.Right)}
     *             instead
     */
    @Deprecated
    boolean hasProgrammingRights(XWikiContext context);

    /**
     * Checks if the author of the passed document (last editor of the content of the document) has programming rights
     * (used to determine if the protected calls in the script contained in the document should be executed or not).
     *
     * @param doc the document to check programming rights for
     * @param context the xwiki context of this request
     * @return {@code true} if the author of {@code doc} has programming rights, {@code false} otherwise.
     * @deprecated since 6.1RC1, use
     *             {@link org.xwiki.security.authorization.ContextualAuthorizationManager#hasAccess(org.xwiki.security.authorization.Right, org.xwiki.model.reference.EntityReference)}
     *             instead
     */
    @Deprecated
    boolean hasProgrammingRights(XWikiDocument doc, XWikiContext context);

    /**
     * Checks that the current user in the context (the currently authenticated user) has administration rights either
     * on the current wiki or on the current space.
     *
     * @param context the xwiki context of this request
     * @return {@code true} if the current user in the context has the {@code admin} right, {@code false} otherwise
     * @deprecated since 6.1RC1, use
     *             {@link org.xwiki.security.authorization.ContextualAuthorizationManager#hasAccess(org.xwiki.security.authorization.Right)}
     *             instead
     */
    @Deprecated
    boolean hasAdminRights(XWikiContext context);

    /**
     * Checks that the current user in the context (the currently authenticated user) has administration rights on the
     * current wiki, regardless of any space admin rights that might also be available.
     *
     * @param context the xwiki context of this request
     * @return {@code true} if the current user in the context has the {@code admin} right, {@code false} otherwise
     * @since 3.2M3
     * @deprecated since 6.1RC1, use
     *             {@link org.xwiki.security.authorization.ContextualAuthorizationManager#hasAccess(org.xwiki.security.authorization.Right, org.xwiki.model.reference.EntityReference)}
     *             instead
     */
    @Deprecated
    boolean hasWikiAdminRights(XWikiContext context);

    /**
     * @param context the xwiki context of this request
     * @return the list of all the known access levels
     * @throws XWikiException if something goes wrong during the rights checking process
     */
    List<String> listAllLevels(XWikiContext context) throws XWikiException;
}
