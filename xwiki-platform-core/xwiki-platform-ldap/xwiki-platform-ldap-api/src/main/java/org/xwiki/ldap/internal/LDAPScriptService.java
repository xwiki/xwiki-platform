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
package org.xwiki.ldap.internal;

import javax.inject.Inject;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.script.service.ScriptService;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.ldap.XWikiLDAPUtils;

/**
 * Script service to expose LDAP utilities to script and templating languages.
 * 
 * @version $Id$
 */
@Component("ldap")
public class LDAPScriptService implements ScriptService
{

    /**
     * Execution, needed to retrieve the legacy XWiki context.
     */
    @Inject
    private Execution execution;

    /**
     * @return the XWiki context associated with this execution.
     */
    private XWikiContext getXWikiContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
    }

    // API

    /**
     * @return {@code true} if the currently configured authentication class extends or is an instance of
     *         {@link com.xpn.xwiki.user.impl.LDAP.XWikiLDAPAuthServiceImpl}. Returns {@code false} otherwise.
     */
    public boolean isXWikiLDAPAuthenticator()
    {
        return com.xpn.xwiki.user.impl.LDAP.XWikiLDAPAuthServiceImpl.class.isAssignableFrom(getXWikiContext().getWiki()
            .getAuthService().getClass());
    }

    /**
     * Force to empty the cache containing LDAP groups.
     * 
     * @since 4.1M1
     */
    public void resetGroupCache()
    {
        XWikiLDAPUtils.resetGroupCache();
    }
}
