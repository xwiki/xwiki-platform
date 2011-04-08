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
package com.xpn.xwiki.web;

import org.xwiki.csrf.CSRFToken;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

public class LogoutAction extends XWikiAction
{
    public boolean action(XWikiContext context) throws XWikiException
    {
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();

        // clear CSRF token
        CSRFToken csrf = Utils.getComponent(CSRFToken.class);
        csrf.clearToken();

        String redirect;
        redirect = context.getRequest().getParameter("xredirect");
        if ((redirect == null) || (redirect.equals("")))
            redirect =
                context.getURLFactory().createURL("Main", "WebHome", "view", context).toString();
        sendRedirect(response, redirect);
        return false;
    }
}
