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
import java.util.regex.Pattern;

import org.securityfilter.realm.SimplePrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.user.SuperAdminUserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.classes.PasswordClass;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractXWikiAuthService.class);

    /**
     * The XWiki config property for storing the superadmin password.
     */
    private static final String SUPERADMIN_PASSWORD_CONFIG = "xwiki.superadminpassword";

    private static final Pattern SUPERADMIN_PASSWORD_PATTERN = Pattern.compile(
        String.format("^\\{(%s)}.*$",
        String.join("|", PasswordClass.SUPPORTED_ALGORITHMS)
    ));

    /**
     * @param username the username to check for superadmin access. Examples: "xwiki:XWiki.superadmin",
     *            "XWiki.superAdmin", "superadmin", etc
     * @return true if the username is that of the superadmin (whatever the case) or false otherwise
     */
    protected boolean isSuperAdmin(String username)
    {
        // We use a resolver since the passed username could contain the wiki and/or the space too.
        return SuperAdminUserReference.isSuperAdmin(Utils
            .<UserReferenceResolver<String>>getComponent(UserReferenceResolver.TYPE_STRING, "document")
            .resolve(username));
    }

    /**
     * @param password the superadmin password to check against the superadmin password located in XWiki's config file
     * @param context the XWiki context object, allowing access to XWiki's config
     * @return a null Principal is the user hasn't been validated as Superadmin or a Super Admin Principal otherwise
     */
    protected Principal authenticateSuperAdmin(String password, XWikiContext context)
    {
        LOGGER.trace("Authenticate superadmin");

        Principal principal;

        // Security check: only decide that the passed user is the super admin if the
        // super admin password is configured in XWiki's configuration.
        String superadminpassword = context.getWiki().Param(SUPERADMIN_PASSWORD_CONFIG);
        if ((superadminpassword != null) && validateSuperAdminPassword(password, superadminpassword)) {
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

    private static boolean validateSuperAdminPassword(String password, String superadminpassword)
    {
        if (SUPERADMIN_PASSWORD_PATTERN.matcher(superadminpassword).matches()) {
            PasswordClass passwordClass = new PasswordClass();
            return passwordClass.arePasswordsMatching(password, superadminpassword);
        }
        return superadminpassword.equals(password);
    }
}
