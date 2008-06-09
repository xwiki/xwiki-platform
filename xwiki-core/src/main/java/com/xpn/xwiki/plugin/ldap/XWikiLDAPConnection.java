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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.io.UnsupportedEncodingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPAttributeSet;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPJSSESecureSocketFactory;
import com.novell.ldap.LDAPSearchConstraints;
import com.novell.ldap.LDAPSearchResults;
import com.novell.ldap.LDAPSocketFactory;
import com.xpn.xwiki.XWikiContext;

import java.security.Security;

/**
 * LDAP communication tool.
 * 
 * @version $Id$
 * @since 1.3 M2
 */
public class XWikiLDAPConnection
{
    /**
     * Logging tool.
     */
    private static final Log LOG = LogFactory.getLog(XWikiLDAPConnection.class);

    /**
     * The LDAP connection.
     */
    private LDAPConnection connection;

    /**
     * @return the {@link LDAPConnection}.
     */
    public LDAPConnection getConnection()
    {
        return connection;
    }

    /**
     * Open a LDAP connection.
     * 
     * @param ldapUserName the user name to connect to LDAP server.
     * @param password the password to connect to LDAP server.
     * @param context the XWiki context.
     * @return true if connection succeed, false otherwise.
     * @throws XWikiLDAPException error when trying to open connection.
     */
    public boolean open(String ldapUserName, String password, XWikiContext context)
        throws XWikiLDAPException
    {
        XWikiLDAPConfig config = XWikiLDAPConfig.getInstance();

        // open LDAP
        int ldapPort = config.getLDAPPort(context);
        String ldapHost = config.getLDAPParam("ldap_server", "localhost", context);
        String bindDNFormat = config.getLDAPParam("ldap_bind_DN", "{0}", context);
        String bindPasswordFormat = config.getLDAPParam("ldap_bind_pass", "{1}", context);

        Object[] arguments = {ldapUserName, password};

        // allow to use the given user and password also as the LDAP bind user and password
        String bindDN = MessageFormat.format(bindDNFormat, arguments);
        String bindPassword = MessageFormat.format(bindPasswordFormat, arguments);

        boolean bind;
        if ("1".equals(config.getLDAPParam("ldap_ssl", "0", context))) {
            String keyStore = config.getLDAPParam("ldap_ssl.keystore", "", context);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Connecting to LDAP using SSL");
            }

            bind = open(ldapHost, ldapPort, bindDN, bindPassword, keyStore, true, context);
        } else {
            bind = open(ldapHost, ldapPort, bindDN, bindPassword, null, false, context);
        }

        return bind;
    }

    /**
     * Open LDAP connection.
     * 
     * @param ldapHost the host of the server to connect to.
     * @param ldapPort the port of the server to connect to.
     * @param loginDN the user DN to connect to LDAP server.
     * @param password the password to connect to LDAP server.
     * @param pathToKeys the patch to SSL keystore to use.
     * @param ssl if true connect using SSL.
     * @param context the XWiki context.
     * @return true if the connection succeed, false otherwise.
     * @throws XWikiLDAPException error when trying to open connection.
     */
    public boolean open(String ldapHost, int ldapPort, String loginDN, String password,
        String pathToKeys, boolean ssl, XWikiContext context) throws XWikiLDAPException
    {
        boolean succeed = false;

        int port = ldapPort;

        if (port <= 0) {
            port = ssl ? LDAPConnection.DEFAULT_SSL_PORT : LDAPConnection.DEFAULT_PORT;
        }

        int ldapVersion = LDAPConnection.LDAP_V3;

        try {
            if (ssl) {
                XWikiLDAPConfig config = XWikiLDAPConfig.getInstance();

                // Dynamically set JSSE as a security provider
                Security.addProvider(config.getSecureProvider(context));

                if (pathToKeys != null && pathToKeys.length() > 0) {
                    // Dynamically set the property that JSSE uses to identify
                    // the keystore that holds trusted root certificates

                    System.setProperty("javax.net.ssl.trustStore", pathToKeys);
                    // obviously unnecessary: sun default pwd = "changeit"
                    // System.setProperty("javax.net.ssl.trustStorePassword", sslpwd);
                }

                LDAPSocketFactory ssf = new LDAPJSSESecureSocketFactory();

                // Set the socket factory as the default for all future connections
                // LDAPConnection.setSocketFactory(ssf);

                // Note: the socket factory can also be passed in as a parameter
                // to the constructor to set it for this connection only.
                this.connection = new LDAPConnection(ssf);
            } else {
                this.connection = new LDAPConnection();
            }

            // connect to the server
            this.connection.connect(ldapHost, port);

            // authenticate to the server
            this.connection.bind(ldapVersion, loginDN, password.getBytes("UTF8"));

            succeed =
                this.connection.isConnected() && this.connection.isConnectionAlive()
                    && this.connection.isBound();
        } catch (UnsupportedEncodingException e) {
            throw new XWikiLDAPException("LDAP bind failed with UnsupportedEncodingException.", e);
        } catch (LDAPException e) {
            throw new XWikiLDAPException("LDAP bind failed with LDAPException.", e);
        }

        return succeed;
    }

    /**
     * Close LDAP connection.
     */
    public void close()
    {
        try {
            if (this.connection != null) {
                this.connection.disconnect();
            }
        } catch (LDAPException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("LDAP close failed.", e);
            }
        }
    }

    /**
     * Check if provided password is correct provided users's password.
     * 
     * @param userDN the user.
     * @param password the password.
     * @return true if the password is valid, false otherwise.
     */
    public boolean checkPassword(String userDN, String password)
    {
        return checkPassword(userDN, password, "userPassword");
    }

    /**
     * Check if provided password is correct provided users's password.
     * 
     * @param userDN the user.
     * @param password the password.
     * @param passwordField the name of the LDAP field containing the password.
     * @return true if the password is valid, false otherwise.
     */
    public boolean checkPassword(String userDN, String password, String passwordField)
    {
        try {
            LDAPAttribute attribute = new LDAPAttribute(passwordField, password);
            return this.connection.compare(userDN, attribute);
        } catch (LDAPException e) {
            if (e.getResultCode() == LDAPException.NO_SUCH_OBJECT) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Unable to locate user_dn:" + userDN, e);
                }
            } else if (e.getResultCode() == LDAPException.NO_SUCH_ATTRIBUTE) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(
                        "Unable to verify password because userPassword attribute not found.", e);
                }
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Unable to verify password", e);
                }
            }
        }

        return false;
    }

    /**
     * Execute a LDAP search query.
     * 
     * @param baseDN the root DN where to search.
     * @param query the LDAP query.
     * @param attr the attributes names of values to return.
     * @param ldapScope {@link LDAPConnection#SCOPE_SUB} oder {@link LDAPConnection#SCOPE_BASE}.
     * @return the found LDAP attributes.
     */
    public List<XWikiLDAPSearchAttribute> searchLDAP(String baseDN, String query, String[] attr,
        int ldapScope)
    {
        List<XWikiLDAPSearchAttribute> searchAttributeList =
            new ArrayList<XWikiLDAPSearchAttribute>();

        try {
            LDAPSearchConstraints cons = new LDAPSearchConstraints();
            cons.setTimeLimit(1000);

            // filter return all attributes return attrs and values time out value
            LDAPSearchResults searchResults =
                this.connection.search(baseDN, ldapScope, query, attr, false, cons);

            if (!searchResults.hasMore()) {
                return null;
            }

            LDAPEntry nextEntry = searchResults.next();
            String foundDN = nextEntry.getDN();
            searchAttributeList.add(new XWikiLDAPSearchAttribute("dn", foundDN));

            LDAPAttributeSet attributeSet = nextEntry.getAttributeSet();

            for (Object attributeItem : attributeSet) {
                LDAPAttribute attribute = (LDAPAttribute) attributeItem;
                String attributeName = attribute.getName();

                Enumeration allValues = attribute.getStringValues();

                if (allValues != null) {
                    while (allValues.hasMoreElements()) {

                        String value = (String) allValues.nextElement();
                        searchAttributeList
                            .add(new XWikiLDAPSearchAttribute(attributeName, value));
                    }
                }
            }
        } catch (LDAPException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("LDAP Search failed", e);
            }
        }

        return searchAttributeList;
    }
}
