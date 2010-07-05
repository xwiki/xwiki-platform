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
package com.xpn.xwiki.user.impl.xwiki;

import java.security.Principal;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiAuthService;
import com.xpn.xwiki.user.api.XWikiUser;

public class GroovyAuthServiceImpl extends XWikiAuthServiceImpl
{
    private static final Log log = LogFactory.getLog(GroovyAuthServiceImpl.class);

    protected String getParam(String name, XWikiContext context)
    {
        String param = "";
        try {
            param = context.getWiki().getXWikiPreference(name, context);
        } catch (Exception e) {
        }
        if (param == null || "".equals(param)) {
            try {
                param =
                    context.getWiki().Param("xwiki.authentication." + StringUtils.replace(name, "groovy_", "groovy."));
            } catch (Exception e) {
            }
        }
        if (param == null)
            param = "";
        return param;
    }

    public XWikiAuthService getAuthService(XWikiContext context)
    {
        String authservicepage = getParam("groovy_pagename", context);
        if ((authservicepage == null) || authservicepage.trim().equals("")) {
            if (log.isErrorEnabled())
                log.error("No page specified for auth service implementation");
            return null;
        }

        try {
            XWikiDocument doc = context.getWiki().getDocument(authservicepage, context);
            if (context.getWiki().getRightService().hasProgrammingRights(doc, context))
                return (XWikiAuthService) context.getWiki().parseGroovyFromString(doc.getContent(), context);
            else {
                if (log.isErrorEnabled())
                    log.error("Auth service implementation page " + authservicepage
                        + " missing programming rights, requires ownership by authorized user.");
                return null;
            }
        } catch (XWikiException e) {
            if (log.isErrorEnabled())
                log.error("Exception while parsing groovy authentication service code", e);
            return null;
        }
    }

    public XWikiUser checkAuth(XWikiContext context) throws XWikiException
    {
        XWikiAuthService authservice = getAuthService(context);
        if (authservice == null)
            return super.checkAuth(context);
        else {
            try {
                return authservice.checkAuth(context);
            } catch (Exception e) {
                return super.checkAuth(context);
            }
        }
    }

    public XWikiUser checkAuth(String username, String password, String rememberme, XWikiContext context)
        throws XWikiException
    {
        XWikiAuthService authservice = getAuthService(context);
        if (authservice == null)
            return super.checkAuth(username, password, rememberme, context);
        else {
            try {
                return authservice.checkAuth(username, password, rememberme, context);
            } catch (Exception e) {
                return super.checkAuth(username, password, rememberme, context);
            }
        }
    }

    public void showLogin(XWikiContext context) throws XWikiException
    {
        XWikiAuthService authservice = getAuthService(context);
        if (authservice == null)
            super.showLogin(context);
        else {
            try {
                authservice.showLogin(context);
            } catch (Exception e) {
                super.showLogin(context);
            }
        }
    }

    public Principal authenticate(String username, String password, XWikiContext context) throws XWikiException
    {
        XWikiAuthService authservice = getAuthService(context);
        if (authservice == null)
            return super.authenticate(username, password, context);
        else {
            try {
                return authservice.authenticate(username, password, context);
            } catch (Exception e) {
                return super.authenticate(username, password, context);
            }
        }
    }

}
