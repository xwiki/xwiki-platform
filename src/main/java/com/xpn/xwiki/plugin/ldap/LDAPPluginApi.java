/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 * @author sdumitriu
 */

package com.xpn.xwiki.plugin.ldap;

import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPException;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;

import java.util.HashMap;

public class LDAPPluginApi extends Api {
        private LDAPPlugin plugin;

        public LDAPPluginApi(LDAPPlugin plugin, XWikiContext context) {
            super(context);
            setPlugin(plugin);
        }

    public LDAPPlugin getPlugin() {
        return plugin;
    }

    public void setPlugin(LDAPPlugin plugin) {
        this.plugin = plugin;
    }

    public HashMap search(String searchstr, String[] params) throws LDAPException {
        return plugin.search(searchstr, LDAPConnection.SCOPE_SUB, params, null, context);
    }

    public HashMap search(String searchstr, int scope, String[] params) throws LDAPException {
        return plugin.search(searchstr, scope, params, null, context);
    }

    public HashMap search(String searchstr, String[] params, HashMap connection) throws LDAPException {
        return plugin.search(searchstr, LDAPConnection.SCOPE_SUB, params, connection, context);
    }

    public HashMap search(String searchstr) throws LDAPException {
        return plugin.search(searchstr, LDAPConnection.SCOPE_SUB, null, null, context);
    }

    public HashMap search(String searchstr, HashMap connection) throws LDAPException {
        return plugin.search(searchstr, LDAPConnection.SCOPE_SUB, null, connection, context);
    }

    public HashMap search(String searchstr, int scope, String[] params, HashMap connection) throws LDAPException {
        return plugin.search(searchstr, scope, params, connection, context);
    }

    public HashMap getEntry(String dn) throws LDAPException {
        return plugin.getEntry(dn, context);
    }

    public HashMap getEntry(String dn, HashMap connection) throws LDAPException {
        return plugin.getEntry(dn, connection, context);
    }    
}
