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

import com.novell.ldap.*;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Iterator;

public class LDAPPlugin extends XWikiDefaultPlugin implements XWikiPluginInterface {
        private static Log mLogger =
                LogFactory.getFactory().getInstance(com.xpn.xwiki.plugin.ldap.LDAPPlugin.class);

        public LDAPPlugin(String name, String className, XWikiContext context) {
            super(name, className, context);
            init(context);
        }

    public String getName() {
        return "ldap";
    }

    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context) {
        return new LDAPPluginApi((LDAPPlugin) plugin, context);
    }

    public void flushCache() {
    }

    public void init(XWikiContext context) {
        super.init(context);
    }

    protected String getParam(String name, XWikiContext context) {
        String param = "";
        try {
         param = context.getWiki().getXWikiPreference(name,context);
        } catch (Exception e) {}
        if (param == null || "".equals(param))
        {
            try{
             param = context.getWiki().Param("xwiki.authentication." + StringUtils.replace(name, "ldap_","ldap."));
            } catch (Exception e) {}
        }
        if (param == null)
            param = "";
        return param;
    }

    protected int getLDAPPort(XWikiContext context) {
        try {
         return context.getWiki().getXWikiPreferenceAsInt("ldap_port", context);
        } catch (Exception e) {
         return (int)context.getWiki().ParamAsLong("xwiki.authentication.ldap.port", LDAPConnection.DEFAULT_PORT);
        }
    }

    public LDAPConnection connect(HashMap connection, XWikiContext context) throws LDAPException {
        int ldapPort;
        int ldapVersion = LDAPConnection.LDAP_V3;
        String ldapHost, ldapBindDN, ldapBindPassword;
        ldapHost = (connection==null) ? null : (String) connection.get("server");
        if (ldapHost!=null) {
            try {
            ldapPort = Integer.parseInt((String) connection.get("port"));
            } catch (Exception e) {
                ldapPort = 389;
            }
            ldapBindDN = (String)connection.get("bind_DN");
            ldapBindPassword = (String)connection.get("bind_pass");
        } else {
          ldapHost = getParam("ldap_server", context);
          ldapPort = getLDAPPort(context);
          ldapBindDN = getParam("ldap_bind_DN", context);
          ldapBindPassword = getParam("ldap_bind_pass", context);
        }

        LDAPConnection lc = new LDAPConnection();
        lc.connect( ldapHost, ldapPort );
        if (ldapBindDN!=null)
         lc.bind(ldapVersion, ldapBindDN, ldapBindPassword.getBytes());
        return lc;
    }

    public HashMap search(String searchstr, String[] params, XWikiContext context) throws LDAPException {
        return search(searchstr, LDAPConnection.SCOPE_SUB, params, null, context);
    }

    public HashMap search(String searchstr, int scope, String[] params, XWikiContext context) throws LDAPException {
       return search(searchstr, scope, params, null, context);
    }

    public HashMap search(String searchstr, XWikiContext context) throws LDAPException {
       return search(searchstr, LDAPConnection.SCOPE_SUB, null, null, context);
    }

    public HashMap search(String searchstr, String[] params, HashMap connection, XWikiContext context) throws LDAPException {
        return search(searchstr, LDAPConnection.SCOPE_SUB, params, connection, context);
    }

    public HashMap search(String searchstr, HashMap connection, XWikiContext context) throws LDAPException {
       return search(searchstr, LDAPConnection.SCOPE_SUB, null, connection, context);
    }

    public HashMap search(String searchstr, int scope, String[] params, HashMap connection, XWikiContext context) throws LDAPException {
        HashMap hashmap = new HashMap();
        String baseDN = (connection==null) ? getParam("ldap_base_DN",context) : (String)connection.get("base_DN");
        LDAPConnection lc = connect(connection, context);
        try {
            LDAPSearchResults results =
                    lc.search(  baseDN,
                            scope ,
                            searchstr,
                            params,
                            false);
            if (results==null)
                return hashmap;
            while (results.hasMore()) {
                LDAPEntry entry;
                try {
                    entry = results.next();
                } catch(LDAPException e) {
                    mLogger.debug("Error while reading ldap entry", e);
                    // Exception is thrown, go for next entry
                    continue;
                }
                HashMap entryhash = getEntryAsHashMap(entry);
                hashmap.put(entry.getDN(), entryhash);
            }
            return hashmap;
        } finally {
            lc.disconnect();
        }
    }

    public HashMap getEntry(String dn, XWikiContext context) throws LDAPException {
        return getEntry(dn, null, context);
    }

    public HashMap getEntry(String dn, HashMap connection, XWikiContext context) throws LDAPException {
        LDAPConnection lc = connect(connection, context);
        try {
            LDAPEntry entry = lc.read(dn);
            return getEntryAsHashMap(entry);
        } finally {
            lc.disconnect();
        }
    }

    public HashMap getEntryAsHashMap(LDAPEntry entry) {
        HashMap entryhash = new HashMap();
        if (entry==null)
         return entryhash;
        entryhash.put("dn", entry.getDN());
        LDAPAttributeSet attributeSet = entry.getAttributeSet();
        Iterator allAttributes = attributeSet.iterator();

        while (allAttributes.hasNext()) {
            LDAPAttribute attribute =
                        (LDAPAttribute)allAttributes.next();
            String attributeName = attribute.getName().toLowerCase();
            if (attribute.size()<=1)
                entryhash.put(attributeName,attribute.getStringValue());
            else
                entryhash.put(attributeName,attribute.getStringValueArray());
            }
        return entryhash;
    }
}
