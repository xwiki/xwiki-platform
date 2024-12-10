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
package org.xwiki.security.authservice;

import java.security.Principal;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.user.api.XWikiAuthService;
import com.xpn.xwiki.user.api.XWikiUser;

/**
 * Helper to redirect all {@link XWikiAuthService} calls to another {@link XWikiAuthService}.
 * 
 * @version $Id$
 * @since 15.3RC1
 */
public abstract class AbstractXWikiAuthServiceWrapper implements XWikiAuthService
{
    private final XWikiAuthService service;

    protected AbstractXWikiAuthServiceWrapper(XWikiAuthService service)
    {
        this.service = service;
    }

    @Override
    public XWikiUser checkAuth(XWikiContext context) throws XWikiException
    {
        return this.service.checkAuth(context);
    }

    @Override
    public XWikiUser checkAuth(String username, String password, String rememberme, XWikiContext context)
        throws XWikiException
    {
        return this.service.checkAuth(username, password, rememberme, context);
    }

    @Override
    public void showLogin(XWikiContext context) throws XWikiException
    {
        this.service.showLogin(context);
    }

    @Override
    public Principal authenticate(String username, String password, XWikiContext context) throws XWikiException
    {
        return this.service.authenticate(username, password, context);
    }
}
