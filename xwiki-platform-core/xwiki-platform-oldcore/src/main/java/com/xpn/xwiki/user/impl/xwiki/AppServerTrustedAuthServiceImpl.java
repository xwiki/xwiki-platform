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
package com.xpn.xwiki.user.impl.xwiki;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.user.api.XWikiUser;

/**
 * Implements an authentication mechanism which is trusting the App Server authentication. If it fails it falls back to
 * the standard XWiki authentication.
 * 
 * @version $Id$
 */
public class AppServerTrustedAuthServiceImpl extends XWikiAuthServiceImpl
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AppServerTrustedAuthServiceImpl.class);

    @Override
    public XWikiUser checkAuth(XWikiContext context) throws XWikiException
    {
        String user = context.getRequest().getRemoteUser();
        if ((user == null) || user.equals("")) {
            return super.checkAuth(context);
        } else {
            LOGGER.debug("Launching create user for [{}]", user);
            createUser(user, context);
            LOGGER.debug("Create user done for [{}]", user);
            user = "XWiki." + user;
        }
        context.setUser(user);

        return new XWikiUser(user);
    }

    /**
     * We cannot authenticate locally since we need to trust the app server for authentication.
     */
    @Override
    public XWikiUser checkAuth(String username, String password, String rememberme, XWikiContext context)
        throws XWikiException
    {
        String user = context.getRequest().getRemoteUser();
        if ((user == null) || user.equals("")) {
            return super.checkAuth(username, password, rememberme, context);
        } else {
            createUser(user, context);
            user = "XWiki." + user;
        }
        context.setUser(user);

        return new XWikiUser(user);
    }
}
