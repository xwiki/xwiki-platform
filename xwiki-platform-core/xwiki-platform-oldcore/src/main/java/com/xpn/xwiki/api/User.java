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
package com.xpn.xwiki.api;

import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.PasswordClass;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.util.Programming;
import com.xpn.xwiki.web.Utils;

/**
 * Scriptable API for easy handling of users. For the moment this API is very limited, containing only one method. In
 * the future it should be extended to provide useful methods for working with users.
 *
 * @version $Id$
 * @since 1.0
 */
public class User extends Api
{
    /** Logging helper object. */
    protected static final Logger LOGGER = LoggerFactory.getLogger(User.class);

    /** User class reference. */
    private static final EntityReference USERCLASS_REFERENCE = new EntityReference("XWikiUsers", EntityType.DOCUMENT,
        new EntityReference("XWiki", EntityType.SPACE));

    /** Reference resolver. */
    private static final DocumentReferenceResolver<String> REFERENCE_RESOLVER = Utils.getComponent(
        DocumentReferenceResolver.TYPE_STRING, "currentmixed");

    /** The wrapped XWikiUser object. */
    private XWikiUser user;

    /**
     * Constructs a wrapper for the given protected XWikiUser object.
     *
     * @param user The XWikiUser object that should be wrapper.
     * @param context The current {@link XWikiContext context}.
     */
    public User(XWikiUser user, XWikiContext context)
    {
        super(context);
        this.user = user;
    }

    /**
     * Expose the wrapped XWikiUser object. Requires programming rights.
     *
     * @return The wrapped XWikiUser object, or {@code null} if the user does not have programming rights.
     */
    @Programming
    public XWikiUser getUser()
    {
        if (hasProgrammingRights()) {
            return this.user;
        }
        return null;
    }

    /**
     * Set the disabled status of a user.
     * @param disabledStatus true to disable an user, false to enable it back.
     * @since 11.7RC1
     */
    public void setDisabledStatus(boolean disabledStatus)
    {
        if (hasAdminRights()) {
            this.user.setDisabled(disabledStatus, getXWikiContext());
        }
    }

    /**
     * @return {@code true} if the user is disabled, false if it is enabled.
     * @since 11.8RC1
     */
    public boolean isDisabled()
    {
        return this.user.isDisabled(getXWikiContext());
    }

    /**
     * Check if the user belongs to a group or not. This method only check direct membership (no recursive checking) in
     * the current wiki.
     *
     * @param groupName The group to check.
     * @return {@code true} if the user does belong to the specified group, false otherwise or if an exception occurs.
     */
    public boolean isUserInGroup(String groupName)
    {
        boolean result = false;
        try {
            if (this.user == null) {
                LOGGER.warn("User considered not part of group [{}] since user is null", groupName);
            } else {
                result = this.user.isUserInGroup(groupName, getXWikiContext());
            }
        } catch (Exception ex) {
            LOGGER.warn(new MessageFormat("Unhandled exception while checking if user {0}"
                + " belongs to group {1}").format(new java.lang.Object[] { this.user, groupName }), ex);
        }
        return result;
    }

    /**
     * <p>
     * See if the user is global (i.e. registered in the main wiki) or local to a virtual wiki.
     * </p>
     * <p>
     * This method is not public, as the underlying implementation is not fully functional
     * </p>
     *
     * @return {@code true} if the user is global, false otherwise or if an exception occurs.
     */
    protected boolean isMain()
    {
        return this.user.isMain();
    }

    /**
     * API to retrieve the e-mail address of this user. This e-mail address is taken from the user profile. If the user
     * hasn't changed his profile, then this is the e-mail address he filled in the registration form.
     *
     * @return The e-mail address from the user profile, or {@code null} if there is an error retrieving the email.
     * @since 1.1.3
     * @since 1.2.2
     * @since 1.3M2
     */
    public String getEmail()
    {
        XWikiDocument userDoc;
        try {
            userDoc = getXWikiContext().getWiki().getDocument(this.user.getUser(), getXWikiContext());
            BaseObject obj = userDoc.getObject("XWiki.XWikiUsers");
            return obj.getStringValue("email");
        } catch (Exception e) {
            // APIs should never throw errors, as velocity cannot catch them, and scripts should be
            // as robust as possible. Instead, the code using this should know that null means there
            // was an error, if it really needs to report these exceptions.
            return null;
        }
    }

    /**
     * Check if the password passed as argument is the user password. This method is used when a user wants to change
     * its password. To make sure that it wouldn't be used to perform brute force attacks, we ensure that this is only
     * used to check the current user password on its profile page.
     *
     * @param password Password submitted.
     * @return true if password is really the user password.
     * @throws XWikiException error if authorization denied.
     */
    public boolean checkPassword(String password) throws XWikiException
    {
        EntityReference userReference = REFERENCE_RESOLVER.resolve(this.user.getUser());
        EntityReference docReference = getXWikiContext().getDoc().getDocumentReference();
        if (userReference.equals(getXWikiContext().getUserReference()) && userReference.equals(docReference)) {
            try {
                boolean result = false;

                XWikiDocument userDoc = getXWikiContext().getWiki().getDocument(userReference, getXWikiContext());
                BaseObject obj = userDoc.getXObject(USERCLASS_REFERENCE);
                // We only allow empty password from users having a XWikiUsers object.
                if (obj != null) {
                    final String stored = obj.getStringValue("password");
                    result = new PasswordClass().getEquivalentPassword(stored, password).equals(stored);
                }

                return result;
            } catch (Throwable e) {
                LOGGER.error("Failed to check password", e);
                return false;
            }
        } else {
            throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS, XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                "You cannot use this method for checking another user password.", null);
        }
    }

}
