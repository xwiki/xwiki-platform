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

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * Action for displaying the login form.
 *
 * @version $Id$
 */
@Component
@Named("login")
@Singleton
public class LoginAction extends XWikiAction
{
    private static final String LOGIN = "login";

    /**
     * Default constructor.
     */
    public LoginAction()
    {
        this.waitForXWikiInitialization = false;
    }

    @Override
    public boolean action(XWikiContext context) throws XWikiException
    {
        // Disallow template override with xpage parameter.
        if (!LOGIN.equals(Utils.getPage(context.getRequest(), LOGIN))) {
            throw new XWikiException(XWikiException.MODULE_XWIKI, XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                String.format("Template may not be overriden with 'xpage' in [%s] action.", LOGIN));
        }

        return super.action(context);
    }

    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        // if the request does not come from an explicit login link (such as the login button)
        // then we consider it's coming from a redirect because a guest user tries to access a protected resource
        if (!"1".equals(context.getRequest().getParameter("loginLink"))) {
            context.getResponse().setStatus(401);
        }
        return LOGIN;
    }
}
