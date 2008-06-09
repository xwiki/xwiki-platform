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

package com.xpn.xwiki.plugin.ldap;

import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPException;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;

import java.util.HashMap;

public class LDAPPluginApi extends Api {
        private LDAPPlugin plugin;

        public LDAPPluginApi(LDAPPlugin plugin, XWikiContext context) {
            super(context);
            setPlugin(plugin);
        }

    public LDAPPlugin getPlugin() {
        if (hasProgrammingRights()) {
            return plugin;
        }
        return null;
    }

    public void setPlugin(LDAPPlugin plugin) {
        this.plugin = plugin;
    }

    public HashMap search(String searchstr, String[] params) throws LDAPException {
        return plugin.search(searchstr, LDAPConnection.SCOPE_SUB, params, null, getXWikiContext());
    }

    public HashMap search(String searchstr, int scope, String[] params) throws LDAPException {
        return plugin.search(searchstr, scope, params, null, getXWikiContext());
    }

    public HashMap search(String searchstr, String[] params, HashMap connection) throws LDAPException {
        return plugin.search(searchstr, LDAPConnection.SCOPE_SUB, params, connection, getXWikiContext());
    }

    public HashMap search(String searchstr) throws LDAPException {
        return plugin.search(searchstr, LDAPConnection.SCOPE_SUB, null, null, getXWikiContext());
    }

    public HashMap search(String searchstr, HashMap connection) throws LDAPException {
        return plugin.search(searchstr, LDAPConnection.SCOPE_SUB, null, connection, getXWikiContext());
    }

    public HashMap search(String searchstr, int scope, String[] params, HashMap connection) throws LDAPException {
        return plugin.search(searchstr, scope, params, connection, getXWikiContext());
    }

    public HashMap getEntry(String dn) throws LDAPException {
        return plugin.getEntry(dn, getXWikiContext());
    }

    public HashMap getEntry(String dn, HashMap connection) throws LDAPException {
        return plugin.getEntry(dn, connection, getXWikiContext());
    }

    /**
     * Method to create an XWiki user from LDAP information. Information is retrieved from the LDAP server specified in the XWiki Preferences.
     * Bind to the LDAP server can be done using Admin binding (in this case bindusernamd and bindpassword are not used)
     * or User binding (in this case bindusername and bindpassword are used)
     * @param wikiname Wiki page name to use for the user. If null it will be generated from LDAP
     * @param uid UID to search user information in LDAP
     * @param bindusername bind username if binding is user binding
     * @param bindpassword bind password if binding is user binding
     * @return  success or failure of create user
     * @throws com.xpn.xwiki.XWikiException
     */
    public boolean createUserFromLDAP(String wikiname, String uid, String bindusername, String bindpassword) throws XWikiException {
        if (hasProgrammingRights())
            return createUserFromLDAP(wikiname, uid, bindusername, bindpassword);
        else
            return false;
    }

    /**
     * Method allowing to create a user from LDAP attributes stored in a HashMap
     * The mapping is declared in the XWiki Parameters
     * The 'name' mapping is used to define which ldap field to use to define the wiki page name
     * All special characters are cleared to generate the wiki page name
     * @param wikiname Wiki page name to use. If null generate from ldap fields
     * @param attributes
     * @throws XWikiException
     */
    public boolean createUserFromLDAP(String wikiname, HashMap attributes) throws XWikiException {
        if (hasProgrammingRights()) {
            plugin.createUserFromLDAP(wikiname, attributes, context);
            return true;
        }
        else
            return false;
    }
}
