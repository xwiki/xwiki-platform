/**
 * ===================================================================
 *
 * Copyright (c) 2003,2004 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details, published at
 * http://www.gnu.org/copyleft/gpl.html or in gpl.txt in the
 * root folder of this distribution.

 * Created by
 * User: Ludovic Dubost
 * Date: 2 févr. 2004
 * Time: 16:41:56
 */
package com.xpn.xwiki.user;

import com.opensymphony.module.user.EntityNotFoundException;
import com.opensymphony.module.user.User;
import com.opensymphony.module.user.UserManager;
import com.xpn.xwiki.XWiki;
import org.securityfilter.realm.SecurityRealmInterface;

import java.security.Principal;

public class XWikiRealmAdapter  implements SecurityRealmInterface {
    private XWiki xwiki;

    public XWikiRealmAdapter() {
    }

    public XWikiRealmAdapter(XWiki xwiki) {
        setxWiki(xwiki);
    }

    /**
     * Set xwiki used for authentication
     */
    public void setxWiki(XWiki xwiki) {
        this.xwiki = xwiki;
    }

    public UserManager getUserManager() {
        return xwiki.getUsermanager();
    }

    /**
     * Authenticate a user.
     *
     * @param username a username
     * @param password a plain text password, as entered by the user
     *
     * @return a Principal object representing the user if successful, false otherwise
     */
    public Principal authenticate(String username, String password) {
        try {
            User user = getUserManager().getUser(username);

            if (user.authenticate(password)) {
                return user;
            }
        } catch (EntityNotFoundException e) {
        }

        return null;
    }

    /**
     * Test for role membership.
     *
     * Use Principal.getName() to get the username from the principal object.
     *
     * @param principal Principal object representing a user
     * @param rolename name of a role to test for membership
     * @return true if the user is in the role, false otherwise
     */
    public boolean isUserInRole(Principal principal, String rolename) {
        try {
            User user = getUserManager().getUser(principal.getName());

            return user.inGroup(rolename);
        } catch (EntityNotFoundException e) {
            return false;
        }
    }
}
