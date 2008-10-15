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

import java.security.Principal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.securityfilter.realm.SimplePrincipal;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.user.api.XWikiAuthService;

/**
 * Common methods useful to all Authentication services implementations.
 * 
 * @version $Id$
 */
public abstract class AbstractXWikiAuthService implements XWikiAuthService
{
    /**
     * Logging tool.
     */
    private static final Log LOG = LogFactory.getLog(AbstractXWikiAuthService.class);

    /**
     * The Superadmin username.
     */
    private static final String SUPERADMIN = "superadmin";

    /**
     * The XWiki config property for storing the superadmin password.
     */
    private static final String SUPERADMIN_PASSWORD_CONFIG = "xwiki.superadminpassword";

    /**
     * The Superadmin full name.
     */
    private static final String SUPERADMIN_FULLNAME = "XWiki.superadmin";

    /**
     * @param username the username to check for superadmin access
     * @return true if the username is that of the superadmin (whatever the case) or false otherwise
     */
    protected boolean isSuperAdmin(String username)
    {
        String lowerUserName = username.toLowerCase();

        return (lowerUserName.equals(SUPERADMIN) || lowerUserName.endsWith("." + SUPERADMIN));
    }

    /**
     * @param password the superadmin password to check against the superadmin password located in XWiki's config file
     * @param context the XWiki context object, allowing access to XWiki's config
     * @return a null Principal is the user hasn't been validated as Superadmin or a Super Admin Principal otherwise
     */
    protected Principal authenticateSuperAdmin(String password, XWikiContext context)
    {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Authenticate superadmin");
        }

        Principal principal;

        // Security check: only decide that the passed user is the super admin if the
        // super admin password is configured in XWiki's configuration.
        String superadminpassword = context.getWiki().Param(SUPERADMIN_PASSWORD_CONFIG);
        if ((superadminpassword != null) && (superadminpassword.equals(password))) {
            if (context.isMainWiki()) {
                principal = new SimplePrincipal(SUPERADMIN_FULLNAME);
            } else {
                principal = new SimplePrincipal(context.getMainXWiki() + ":" + SUPERADMIN_FULLNAME);
            }
        } else {
            principal = null;
            context.put("message", "wrongpassword");
        }

        return principal;
    }
}
