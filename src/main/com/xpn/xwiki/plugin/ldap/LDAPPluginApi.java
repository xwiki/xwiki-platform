/**
 * ===================================================================
 *
 * Copyright (c) 2003,2004 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at 
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.

 * Created by
 * User: Ludovic Dubost
 * Date: 23 avr. 2005
 * Time: 00:57:33
 */
package com.xpn.xwiki.plugin.ldap;

import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.XWikiContext;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPConnection;
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
