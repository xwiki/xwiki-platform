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

import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPAttributeSet;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPSearchResults;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;

public class LDAPPlugin extends XWikiDefaultPlugin implements XWikiPluginInterface
{
    private static final Log log = LogFactory.getLog(LDAPPlugin.class);

    public LDAPPlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
        init(context);
    }

    public String getName()
    {
        return "ldap";
    }

    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new LDAPPluginApi((LDAPPlugin) plugin, context);
    }

    public void flushCache()
    {
    }

    public void init(XWikiContext context)
    {
        super.init(context);
    }

    protected String getParam(String name, XWikiContext context)
    {
        String param = "";
        try {
            param = context.getWiki().getXWikiPreference(name, context);
        } catch (Exception e) {
        }
        if (param == null || "".equals(param)) {
            try {
                param = context.getWiki().Param("xwiki.authentication." + StringUtils.replace(name, "ldap_", "ldap."));
            } catch (Exception e) {
            }
        }
        if (param == null)
            param = "";
        return param;
    }

    protected int getLDAPPort(XWikiContext context)
    {
        try {
            return context.getWiki().getXWikiPreferenceAsInt("ldap_port", context);
        } catch (Exception e) {
            return (int) context.getWiki().ParamAsLong("xwiki.authentication.ldap.port", LDAPConnection.DEFAULT_PORT);
        }
    }

    public LDAPConnection connect(HashMap connection, XWikiContext context) throws LDAPException
    {
        int ldapPort;
        int ldapVersion = LDAPConnection.LDAP_V3;
        String ldapHost, ldapBindDN, ldapBindPassword;
        ldapHost = (connection == null) ? null : (String) connection.get("server");
        if (ldapHost != null) {
            try {
                ldapPort = Integer.parseInt((String) connection.get("port"));
            } catch (Exception e) {
                ldapPort = 389;
            }
            ldapBindDN = (String) connection.get("bind_DN");
            ldapBindPassword = (String) connection.get("bind_pass");
        } else {
            ldapHost = getParam("ldap_server", context);
            ldapPort = getLDAPPort(context);
            ldapBindDN = getParam("ldap_bind_DN", context);
            ldapBindPassword = getParam("ldap_bind_pass", context);
        }

        return connect(ldapHost, ldapPort, ldapVersion, ldapBindDN, ldapBindPassword);
    }

    public LDAPConnection connect(String ldapHost, int ldapPort, int ldapVersion, String ldapBindDN,
        String ldapBindPassword) throws LDAPException
    {
        if (log.isDebugEnabled())
            log.debug("LDAP Connect starting to " + ldapHost + " port " + ldapPort);
        LDAPConnection lc = new LDAPConnection();
        try {
            lc.connect(ldapHost, ldapPort);
            if (log.isDebugEnabled())
                log.debug("LDAP Connect successfull");
        } catch (LDAPException e) {
            if (log.isErrorEnabled())
                log.error("LDAP Connect failed with Exception " + e.getMessage());
            throw e;
        }
        if (ldapBindDN != null) {
            if (log.isDebugEnabled())
                log.debug("LDAP Bind starting to " + ldapBindDN);
            try {
                lc.bind(ldapVersion, ldapBindDN, ldapBindPassword.getBytes());
                if (log.isDebugEnabled())
                    log.debug("LDAP Bind successfull");
            } catch (LDAPException e) {
                if (log.isErrorEnabled())
                    log.error("LDAP Bind failed with Exception " + e.getMessage());
                throw e;
            }
        } else {
            if (log.isDebugEnabled())
                log.debug("LDAP Bind bypassed");
        }
        return lc;
    }

    public HashMap search(String searchstr, String[] params, XWikiContext context) throws LDAPException
    {
        return search(searchstr, LDAPConnection.SCOPE_SUB, params, null, context);
    }

    public HashMap search(String searchstr, int scope, String[] params, XWikiContext context) throws LDAPException
    {
        return search(searchstr, scope, params, null, context);
    }

    public HashMap search(String searchstr, XWikiContext context) throws LDAPException
    {
        return search(searchstr, LDAPConnection.SCOPE_SUB, null, null, context);
    }

    public HashMap search(String searchstr, String[] params, HashMap connection, XWikiContext context)
        throws LDAPException
    {
        return search(searchstr, LDAPConnection.SCOPE_SUB, params, connection, context);
    }

    public HashMap search(String searchstr, HashMap connection, XWikiContext context) throws LDAPException
    {
        return search(searchstr, LDAPConnection.SCOPE_SUB, null, connection, context);
    }

    public HashMap search(String searchstr, int scope, String[] params, HashMap connection, XWikiContext context)
        throws LDAPException
    {
        HashMap hashmap = new HashMap();
        String baseDN = (connection == null) ? getParam("ldap_base_DN", context) : (String) connection.get("base_DN");
        LDAPConnection lc = connect(connection, context);
        try {
            LDAPSearchResults results = lc.search(baseDN, scope, searchstr, params, false);
            if (results == null)
                return hashmap;
            while (results.hasMore()) {
                LDAPEntry entry;
                try {
                    entry = results.next();
                } catch (LDAPException e) {
                    log.debug("Error while reading ldap entry", e);
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

    public HashMap getEntry(String dn, XWikiContext context) throws LDAPException
    {
        return getEntry(dn, null, context);
    }

    public HashMap getEntry(String dn, HashMap connection, XWikiContext context) throws LDAPException
    {
        LDAPConnection lc = connect(connection, context);
        try {
            LDAPEntry entry = lc.read(dn);
            return getEntryAsHashMap(entry);
        } finally {
            lc.disconnect();
        }
    }

    public HashMap getEntryAsHashMap(LDAPEntry entry)
    {
        HashMap entryhash = new HashMap();
        if (entry == null)
            return entryhash;
        entryhash.put("dn", entry.getDN());
        LDAPAttributeSet attributeSet = entry.getAttributeSet();
        Iterator allAttributes = attributeSet.iterator();

        while (allAttributes.hasNext()) {
            LDAPAttribute attribute = (LDAPAttribute) allAttributes.next();
            String attributeName = attribute.getName().toLowerCase();
            if (attribute.size() <= 1)
                entryhash.put(attributeName, attribute.getStringValue());
            else
                entryhash.put(attributeName, attribute.getStringValueArray());
        }
        return entryhash;
    }

    /**
     * Method to create an XWiki user from LDAP information. Information is retrieved from the LDAP server specified in
     * the XWiki Preferences. Bind to the LDAP server can be done using Admin binding (in this case bindusernamd and
     * bindpassword are not used) or User binding (in this case bindusername and bindpassword are used)
     * 
     * @param wikiname Wiki page name to use for the user. If null it will be generated from LDAP
     * @param uid UID to search user information in LDAP
     * @param bindusername bind username if binding is user binding
     * @param bindpassword bind password if binding is user binding
     * @param context XWiki Context
     * @return success or failure of create user
     * @throws XWikiException
     */
    public boolean createUserFromLDAP(String wikiname, String uid, String bindusername, String bindpassword,
        XWikiContext context) throws XWikiException
    {
        if (log.isDebugEnabled())
            log.debug("Check LDAP");

        LDAPConnection lc = new LDAPConnection();
        String foundDN;
        HashMap attributes = new HashMap();

        try {
            if (log.isDebugEnabled())
                log.debug("LDAP Password check for user " + uid);

            int ldapPort = getLDAPPort(context);
            int ldapVersion = LDAPConnection.LDAP_V3;
            String ldapHost = getParam("ldap_server", context);
            String bindDNFormat = getParam("ldap_bind_DN", context);
            String bindPasswordFormat = getParam("ldap_bind_pass", context);
            Object[] arguments = {bindusername, bindpassword};
            String bindDN = MessageFormat.format(bindDNFormat, arguments);
            String bindPassword = MessageFormat.format(bindPasswordFormat, arguments);
            String baseDN = getParam("ldap_base_DN", context);

            // Connect and bind to LDAP server
            lc = connect(ldapHost, ldapPort, ldapVersion, bindDN, bindPassword);

            if (lc != null) {
                String searchquery = "(" + getParam("ldap_UID_attr", context) + "=" + uid + ")";

                if (log.isDebugEnabled())
                    log.debug("LDAP searching for user with query " + searchquery);

                LDAPSearchResults searchResults = lc.search(baseDN, LDAPConnection.SCOPE_SUB, searchquery, null, // return
                                                                                                                 // all
                                                                                                                 // attributes
                    false); // return attrs and values

                if (searchResults.hasMore()) {
                    if (log.isDebugEnabled())
                        log.debug("LDAP searching found user");

                    LDAPEntry nextEntry = searchResults.next();
                    foundDN = nextEntry.getDN();

                    if (log.isDebugEnabled())
                        log.debug("LDAP searching found DN: " + foundDN);

                    if (log.isDebugEnabled())
                        log.debug("LDAP adding user attributes");

                    LDAPAttributeSet attributeSet = nextEntry.getAttributeSet();
                    Iterator allAttributes = attributeSet.iterator();

                    while (allAttributes.hasNext()) {
                        LDAPAttribute attribute = (LDAPAttribute) allAttributes.next();
                        String attributeName = attribute.getName();

                        Enumeration allValues = attribute.getStringValues();

                        if (allValues != null) {
                            while (allValues.hasMoreElements()) {
                                if (log.isDebugEnabled())
                                    log.debug("LDAP adding user attribute " + attributeName);

                                String Value = (String) allValues.nextElement();
                                attributes.put(attributeName, Value);
                            }
                        }
                    }
                    attributes.put("dn", foundDN);
                    if (createUserFromLDAP(wikiname, attributes, context)) {
                        if (log.isInfoEnabled()) {
                            log.info("LDAP create user for user " + uid + " successfull");
                        }
                        return true;
                    } else {
                        if (log.isInfoEnabled()) {
                            log.info("LDAP create user for user " + uid + " failed");
                        }
                        return false;
                    }
                } else {
                    if (log.isDebugEnabled())
                        log.debug("LDAP search user failed");
                    return false;
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.info("LDAP connect failed");
                }
                return false;
            }
        } catch (LDAPException e) {
            if (log.isInfoEnabled())
                log.info("LDAP create user for user " + uid + " failed with exception " + e.getMessage());
        } catch (Throwable e) {
            if (log.isErrorEnabled())
                log.error("LDAP create user for user " + uid + " failed with exception " + e.getMessage());
        } finally {
            if (log.isDebugEnabled())
                log.debug("LDAP create user in finally block");

            try {
                lc.disconnect();
            } catch (LDAPException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Method allowing to create a user from LDAP attributes stored in a HashMap The mapping is declared in the XWiki
     * Parameters The 'name' mapping is used to define which ldap field to use to define the wiki page name All special
     * characters are cleared to generate the wiki page name
     * 
     * @param wikiname Wiki page name to use. If null generate from ldap fields
     * @param attributes
     * @param context
     * @throws XWikiException
     */
    public boolean createUserFromLDAP(String wikiname, HashMap attributes, XWikiContext context) throws XWikiException
    {
        String ldapFieldMapping = getParam("ldap_fields_mapping", context);
        if (log.isDebugEnabled())
            log.debug("Ready to create user from LDAP with field " + ldapFieldMapping);
        if (ldapFieldMapping != null && ldapFieldMapping.length() > 0) {
            String[] fields = ldapFieldMapping.split(",");
            BaseClass bclass = context.getWiki().getUserClass(context);
            BaseObject bobj = new BaseObject();
            bobj.setClassName(bclass.getName());
            String fullwikiname = null;
            if (wikiname != null) {
                fullwikiname = "XWiki." + wikiname;
                bobj.setName(fullwikiname);
            }
            for (int i = 0; i < fields.length; i++) {
                String[] field = fields[i].split("=");
                if (2 == field.length) {
                    String fieldName = field[0];
                    if (log.isDebugEnabled())
                        log.debug("Create user from LDAP looking at field " + fieldName);
                    if (attributes.containsKey(field[1])) {
                        String fieldValue;
                        fieldValue = (String) attributes.get(field[1]);
                        if ((wikiname == null) && (fieldName.equals("name"))) {
                            wikiname = context.getWiki().clearName(fieldValue, true, true, context);
                            fullwikiname = "XWiki." + wikiname;
                            bobj.setName(fullwikiname);
                        } else {
                            log.debug("Create user from LDAP setting field " + fieldName);
                            bobj.setStringValue(fieldName, fieldValue);
                        }
                    }
                }
            }

            if (wikiname != null && wikiname.length() > 0) {
                XWikiDocument doc = context.getWiki().getDocument(fullwikiname, context);
                doc.setParent("");
                doc.addObject(bclass.getName(), bobj);
                if (!context.getWiki().getDefaultDocumentSyntax().equals(XWikiDocument.XWIKI10_SYNTAXID)) {
                    doc.setContent("{{include document=\"XWiki.XWikiUserSheet\"/}}");
                    doc.setSyntaxId(XWikiDocument.XWIKI20_SYNTAXID);
                } else {
                    doc.setContent("#includeForm(\"XWiki.XWikiUserSheet\")");
                    doc.setSyntaxId(XWikiDocument.XWIKI10_SYNTAXID);
                }
                context.getWiki().protectUserPage(fullwikiname, "edit", doc, context);
                context.getWiki().saveDocument(doc, context.getMessageTool().get("core.comment.createdUser"), context);
                context.getWiki().setUserDefaultGroup(fullwikiname, context);
                return true;
            }
        }
        return false;
    }

}
