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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.user.api.XWikiUser;

/**
 * Specialized version of the {@link AppServerTrustedAuthServiceImpl} that extract usernames out of Kerberos principals.
 * 
 * @version $Id$
 * @since 2.6.2
 */
public class AppServerTrustedKerberosAuthServiceImpl extends XWikiAuthServiceImpl
{

    /**
     * A dot. The document space/name separator.
     */
    private static final String DOT = ".";

    /**
     * The name of the XWiki space.
     */
    private static final String XWIKI_SPACE = "XWiki";

    /**
     * The at sign.
     */
    private static final String AT_SIGN = "@";

    /**
     * An anti-slash.
     */
    private static final String ANTI_SLASH = "\\";

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AppServerTrustedKerberosAuthServiceImpl.class);

    @Override
    public XWikiUser checkAuth(XWikiContext context) throws XWikiException
    {
        String user = context.getRequest().getRemoteUser();

        LOGGER.debug("Checking auth for remote user [{}]", user);

        if (StringUtils.isBlank(user)) {
            return super.checkAuth(context);
        } else {
            user = this.extractUsernameFromPrincipal(user);
            user = createUser(user, context);
            user = XWIKI_SPACE + DOT + user;
        }
        context.setUser(user);

        return new XWikiUser(user);
    }

    @Override
    public XWikiUser checkAuth(String username, String password, String rememberme, XWikiContext context)
        throws XWikiException
    {
        return this.checkAuth(context);
    }

    /**
     * Helper method to extract the username part out of a Kerberos principal.
     * 
     * @param principal the principal to extract the username from
     * @return the extracted username
     */
    private String extractUsernameFromPrincipal(final String principal)
    {
        String username = principal;

        // Clears the Kerberos principal, by removing the domain part, to retain only the user name of the
        // authenticated remote user.
        if (username.contains(ANTI_SLASH)) {
            // old domain form
            username = StringUtils.substringAfter(username, ANTI_SLASH);
        }
        if (username.contains(AT_SIGN)) {
            // new domain form
            username = StringUtils.substringBeforeLast(username, AT_SIGN);
        }

        return username;
    }

}
