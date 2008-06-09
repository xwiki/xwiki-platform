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
 *
 */
package com.xpn.xwiki.api;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.util.Programming;

/**
 * Scriptable API for easy handling of users. For the moment this API is very limited, containing
 * only one method. In the future it should be extended to provide useful methods for working with
 * users.
 * 
 * @version $Id$
 * @since Platform-1.0
 */
public class User extends Api
{
    /** Logging helper object. */
    protected static final Log LOG = LogFactory.getLog(User.class);

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
     * @return The wrapped XWikiUser object, or <tt>null</tt> if the user does not have
     *         programming rights.
     */
    @Programming
    public XWikiUser getUser()
    {
        if (hasProgrammingRights()) {
            return user;
        }
        return null;
    }

    /**
     * Check if the user belongs to a group or not.
     * 
     * @param groupName The group to check.
     * @return <tt>true</tt> if the user does belong to the specified group, false otherwise or if
     *         an exception occurs.
     */
    public boolean isUserInGroup(String groupName)
    {
        try {
            return user.isUserInGroup(groupName, getXWikiContext());
        } catch (Exception ex) {
            LOG.warn(new MessageFormat("Unhandled exception checking if user {0}"
                + " belongs to group {1}").format(new java.lang.Object[] {user, groupName}), ex);
        }
        return false;
    }

    /**
     * <p>
     * See if the user is global (i.e. registered in the main wiki) or local to a virtual wiki.
     * </p>
     * <p>
     * This method is not public, as the underlying implementation is not fully functional
     * </p>
     * 
     * @return <tt>true</tt> if the user is global, false otherwise or if an exception occurs.
     */
    protected boolean isMain()
    {
        return user.isMain();
    }

    /**
     * API to retrieve the e-mail address of this user. This e-mail address is taken from the user
     * profile. If the user hasn't changed his profile, then this is the e-mail address he filled in
     * the registration form.
     * 
     * @return The e-mail address from the user profile, or <tt>null</tt> if there is an error
     *         retrieving the email.
     * @since 1.1.3
     * @since 1.2.2
     * @since 1.3M2
     */
    public String getEmail()
    {
        XWikiDocument userDoc;
        try {
            userDoc = getXWikiContext().getWiki().getDocument(user.getUser(), getXWikiContext());
            BaseObject obj = userDoc.getObject("XWiki.XWikiUsers");
            return obj.getStringValue("email");
        } catch (Exception e) {
            // APIs should never throw errors, as velocity cannot catch them, and scripts should be
            // as robust as possible. Instead, the code using this should know that null means there
            // was an error, if it really needs to report these exceptions.
            return null;
        }
    }
}
