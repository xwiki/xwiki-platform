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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.securityfilter.realm.SimplePrincipal;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.user.api.XWikiAuthService;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.web.Utils;

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
     * The XWiki config property for storing the superadmin password.
     */
    private static final String SUPERADMIN_PASSWORD_CONFIG = "xwiki.superadminpassword";

    /**
     * @param username the username to check for superadmin access. Examples: "xwiki:XWiki.superadmin",
     *            "XWiki.superAdmin", "superadmin", etc
     * @return true if the username is that of the superadmin (whatever the case) or false otherwise
     */
    protected boolean isSuperAdmin(String username)
    {
        // FIXME: this method should probably use a XWikiRightService#isSuperadmin(String) method, see
        // XWikiRightServiceImpl#isSuperadmin(String)

        // Note 1: we use the default document reference resolver here but it doesn't matter since we only care about
        //         the resolved page name.
        // Note 2: we use a resolver since the passed username could contain the wiki and/or space too and we want
        //         to retrieve only the page name
        DocumentReference documentReference = Utils.getComponent(DocumentReferenceResolver.class).resolve(username);
        return StringUtils.equalsIgnoreCase(documentReference.getName(), XWikiRightService.SUPERADMIN_USER);
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
                principal = new SimplePrincipal(XWikiRightService.SUPERADMIN_USER_FULLNAME);
            } else {
                principal =
                    new SimplePrincipal(context.getMainXWiki() + ":" + XWikiRightService.SUPERADMIN_USER_FULLNAME);
            }
        } else {
            principal = null;
            context.put("message", "invalidcredentials");
        }

        return principal;
    }
}
