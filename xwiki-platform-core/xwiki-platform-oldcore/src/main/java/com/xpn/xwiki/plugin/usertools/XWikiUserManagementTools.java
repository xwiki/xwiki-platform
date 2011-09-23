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
package com.xpn.xwiki.plugin.usertools;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

public interface XWikiUserManagementTools {

    public String inviteUser(String name, String email, XWikiContext context) throws XWikiException;
    public boolean resendInvitation(String email, XWikiContext context) throws XWikiException;
    public String getUserSpace(XWikiContext context);
    public String getUserPage(String email, XWikiContext context);
    public boolean isValidEmail(String email);
    public String getUserName(String userPage, XWikiContext context) throws XWikiException;
    public String getEmail(String userPage, XWikiContext context) throws XWikiException;
}
