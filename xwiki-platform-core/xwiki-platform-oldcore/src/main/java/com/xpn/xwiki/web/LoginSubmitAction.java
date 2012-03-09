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
package com.xpn.xwiki.web;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * Action for processing login requests sent using the normal web login form. The actual login request processing is
 * done before this action is invoked, the URL will trigger the authenticator automatically. If the authentication was
 * successful, then a proper user is set in the context, and the {@code login.vm} template will redirect to the view
 * mode.
 * 
 * @version $Id$
 */
public class LoginSubmitAction extends XWikiAction
{
    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        String msg = (String) context.get("message");
        if (StringUtils.isNotBlank(msg)) {
            context.getResponse().setStatus(HttpServletResponse.SC_FORBIDDEN);
        }
        return "login";
    }
}
