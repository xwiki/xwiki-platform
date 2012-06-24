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

import org.apache.velocity.VelocityContext;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * Register xwiki action.
 * 
 * @version $Id$
 */
public class RegisterAction extends XWikiAction
{
    /** Name of the corresponding template and URL parameter. */
    private static final String REGISTER = "register";

    @Override
    public boolean action(XWikiContext context) throws XWikiException
    {
        XWiki xwiki = context.getWiki();
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();

        String register = request.getParameter(REGISTER);
        if (register != null && register.equals("1")) {
            // CSRF prevention
            if (!csrfTokenCheck(context)) {
                return false;
            }

            int useemail = xwiki.getXWikiPreferenceAsInt("use_email_verification", 0, context);
            int result;
            if (useemail == 1) {
                result = xwiki.createUser(true, "edit", context);
            } else {
                result = xwiki.createUser(context);
            }
            VelocityContext vcontext = (VelocityContext) context.get("vcontext");
            vcontext.put("reg", Integer.valueOf(result));

            // Redirect if a redirection parameter is passed.
            String redirect = Utils.getRedirect(request, null);
            if (redirect == null) {
                return true;
            } else {
                sendRedirect(response, redirect);
                return false;
            }
        }

        return true;
    }

    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        return REGISTER;
    }
}
