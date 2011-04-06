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
package com.xpn.xwiki.plugin.usertools;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;


public class XWikiUserManagementToolsAPI extends Api{
    private XWikiUserManagementTools userMngtTools;

    public XWikiUserManagementToolsAPI(XWikiContext context) {
        super(context);
    }

    public XWikiUserManagementToolsAPI(XWikiUserManagementTools xWikiUserManagementTools, XWikiContext context) {
        this(context);
        this.userMngtTools = xWikiUserManagementTools;
    }

    public String inviteUser(String name, String email) throws XWikiException {
        return userMngtTools.inviteUser(name, email, getXWikiContext());
    }

    public boolean resendInvitation(String email) throws XWikiException {
        return userMngtTools.resendInvitation(email, getXWikiContext());
    }

    public String getUserSpace() {
        return userMngtTools.getUserSpace(getXWikiContext());
    }

    public String getUserPage(String email) {
        return userMngtTools.getUserPage(email, getXWikiContext());
    }

    public boolean isValidEmail(String email) {
        return userMngtTools.isValidEmail(email);
    }

    public String getUserName(String userPage) throws XWikiException {
        return userMngtTools.getUserName(userPage, getXWikiContext());
    }

    public String getEmail(String userPage) throws XWikiException {
        return userMngtTools.getEmail(userPage, getXWikiContext());
    }
}
