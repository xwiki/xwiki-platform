package com.xpn.xwiki.user.impl.LDAP;

import com.xpn.xwiki.user.api.XWikiAuthService;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.user.impl.xwiki.*;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.XWikiDocument;
import com.novell.ldap.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.StringUtils;
import org.securityfilter.authenticator.Authenticator;
import org.securityfilter.config.SecurityConfig;
import org.securityfilter.filter.SecurityRequestWrapper;
import org.securityfilter.realm.SimplePrincipal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Alex
 * Date: 18 avr. 2005
 * Time: 16:18:50
 * To change this template use File | Settings | File Templates.
 */
public class LDAPAuthServiceImpl extends XWikiAuthServiceImpl {
    private static final Log log = LogFactory.getLog(LDAPAuthServiceImpl.class);

    public Principal authenticate(String username, String password, XWikiContext context) throws XWikiException {
        Principal principal = null;

        if (username==null)
            return null;

        String superadmin = "superadmin";
        if (username.equals(superadmin)) {
            String superadminpassword = context.getWiki().Param("xwiki.superadminpassword");
            if ((superadminpassword!=null)&&(superadminpassword.equals(password))) {
                principal = new SimplePrincipal("XWiki.superadmin");
                return principal;
            } else {
                return null;
            }
        }

        // If we have the context then we are using direct mode
        // then we should specify the database
        // This is needed for virtual mode to work
        if (context!=null) {
            String susername = username;
            int i = username.indexOf(".");
            if (i!=-1)
                susername = username.substring(i+1);

           String DN = getLDAP_DN(susername, context);

           if (DN != null && DN.length()!=0)
           {
               if (checkDNPassword(DN, susername, password, context))
               {
                   principal = GetUserPrincipal(susername, context);
               }
           }
           else
            {
               if (checkUserPassword(susername, password, context))
               {
                   principal = GetUserPrincipal(susername, context);
               }
            }
        }
        return principal;
    }

    protected Principal GetUserPrincipal(String susername, XWikiContext context) {
        Principal principal = null;

        // First we check in the local database
        try {
            String user = findUser(susername, context);
            if (user!=null) {
                principal = new SimplePrincipal(user);
            }
        } catch (Exception e) {}

        if (context.isVirtual()) {
            if (principal==null) {
                // Then we check in the main database
                String db = context.getDatabase();
                try {
                    context.setDatabase(context.getWiki().getDatabase());
                    try {
                        String user = findUser(susername, context);
                        if (user!=null)
                            principal = new SimplePrincipal(context.getDatabase() + ":" + user);
                    } catch (Exception e) {}
                } finally {
                    context.setDatabase(db);
                }
            }
        }
        return principal;
    }

    public String getLDAP_DN(String susername, XWikiContext context)
    {
        String DN=null;
        if (context!=null) {
            // First we check in the local database
            try {
                String user = findUser(susername, context);
                if (user!=null && user.length()!=0) {
                    DN = readLDAP_DN(user, context);
                }
            } catch (Exception e) {}

            if (context.isVirtual()) {
                if (DN==null && DN.length()!=0) {
                    // Then we check in the main database
                    String db = context.getDatabase();
                    try {
                        context.setDatabase(context.getWiki().getDatabase());
                        try {
                            String user = findUser(susername, context);
                            if (user!=null && user.length()!=0)
                                DN = readLDAP_DN(user, context);
                        } catch (Exception e) {}
                    } finally {
                        context.setDatabase(db);
                    }
                }
            }
        }
        return DN;
    }

    private String readLDAP_DN(String username, XWikiContext context) {
        String DN = null;
        try {
            XWikiDocument doc = context.getWiki().getDocument(username, context);
            // We only allow empty password from users having a XWikiUsers object.
            if (doc.getObject("XWiki.XWikiUsers")!=null) {
              DN = doc.getStringValue("XWiki.XWikiUsers", "ldap_dn");
            }

        } catch (Throwable e) {}
        return DN;
    }

    protected boolean checkUserPassword(String username, String password, XWikiContext context) throws XWikiException {
        LDAPConnection lc = new LDAPConnection();
        boolean result = false;
        boolean notinLDAP = false;

        try {

            int ldapPort = context.getWiki().getXWikiPreferenceAsInt("ldap_port", LDAPConnection.DEFAULT_PORT, context);
            int ldapVersion = LDAPConnection.LDAP_V3;
            String ldapHost = context.getWiki().getXWikiPreference("ldap_server",context);
            String bindDN = context.getWiki().getXWikiPreference("ldap_bind_DN",context);
            String bindPassword = context.getWiki().getXWikiPreference("ldap_bind_pass",context);
            String baseDN = context.getWiki().getXWikiPreference("ldap_base_DN",context);

            lc.connect( ldapHost, ldapPort );

            // authenticate to the server
            Bind(bindDN, bindPassword, "", "", lc, ldapVersion);

            LDAPSearchResults searchResults =
                lc.search(  baseDN,
                            LDAPConnection.SCOPE_SUB ,
                            "("+ context.getWiki().getXWikiPreference("ldap_UID_attr",context) +
                               "=" + username + ")",
                            null,          // return all attributes
                            false);        // return attrs and values

            if (searchResults.hasMore())
            {
                LDAPEntry nextEntry = searchResults.next();

                String objectDN = nextEntry.getDN();

                LDAPAttribute attr = new LDAPAttribute(
                                                "userPassword", password );
                result = lc.compare( objectDN, attr );
            }
            else
                notinLDAP = true;

            if (log.isDebugEnabled()) {
                if (result)
                 log.debug("(debug) Password check for user " + username + " successfull");
                else
                 log.debug("(debug) Password check for user " + username + " failed");
            }
        }
        catch( LDAPException e ) {
            if ( e.getResultCode() == LDAPException.NO_SUCH_OBJECT ) {
                notinLDAP = true;
            } else if ( e.getResultCode() ==
                                        LDAPException.NO_SUCH_ATTRIBUTE ) {
                notinLDAP = true;
            }
        }
        catch (Throwable e) {
            e.printStackTrace();
        }
        finally
        {
            try {
                lc.disconnect();
            } catch (LDAPException e) {
                e.printStackTrace();
            }
        }
        if (notinLDAP)
        {
            // Use XWiki password if user not in LDAP
            result = checkPassword(username, password, context);
        }

        return result;
    }

    protected boolean checkDNPassword(String DN, String username, String password, XWikiContext context) throws XWikiException {
        LDAPConnection lc = new LDAPConnection();
        boolean result = false;
        boolean notinLDAP = false;
        try {

            int ldapPort = context.getWiki().getXWikiPreferenceAsInt("ldap_port", LDAPConnection.DEFAULT_PORT, context);
            int ldapVersion = LDAPConnection.LDAP_V3;
            String ldapHost = context.getWiki().getXWikiPreference("ldap_server",context);
            String bindDN = context.getWiki().getXWikiPreference("ldap_bind_DN",context);
            String bindPassword = context.getWiki().getXWikiPreference("ldap_bind_pass",context);
            String baseDN = context.getWiki().getXWikiPreference("ldap_base_DN",context);

            lc.connect( ldapHost, ldapPort );

            // authenticate to the server
            Bind(bindDN, bindPassword, DN, password, lc, ldapVersion);

            LDAPAttribute attr = new LDAPAttribute(
                                                "userPassword", password );
            result = lc.compare( DN, attr );

            if (log.isDebugEnabled()) {
                if (result)
                 log.debug("(debug) Password check for user " + DN + " successfull");
                else
                 log.debug("(debug) Password check for user " + DN + " failed");
            }
        }
        catch( LDAPException e ) {
            if ( e.getResultCode() == LDAPException.NO_SUCH_OBJECT ) {
                notinLDAP = true;
            } else if ( e.getResultCode() ==
                                        LDAPException.NO_SUCH_ATTRIBUTE ) {
                notinLDAP = true;
            }
        }
        catch (Throwable e) {
            e.printStackTrace();
        }
        finally
        {
            try {
                lc.disconnect();
            } catch (LDAPException e) {
                e.printStackTrace();
            }
        }
        if (notinLDAP)
        {
            // Use XWiki password if user not in LDAP
            result = checkPassword(username, password, context);
        }
        return result;
    }

    private boolean Bind(String bindDN, String bindPassword, String userDN, String userPassword, LDAPConnection lc, int ldapVersion) throws UnsupportedEncodingException {
        boolean bound = false;
        if (bindDN != null && bindDN.length() > 0 && bindPassword != null)
        {
            try
            {
                lc.bind( ldapVersion, bindDN, bindPassword.getBytes("UTF8") );
                bound = true;
            }
            catch(LDAPException e) { };
        }

        if (!bound)
        {
            try
            {
                // Anonymously connect if no bind info is not set or incorrect
                lc.bind( ldapVersion, userDN, userPassword.getBytes("UTF8") );
                bound = true;
            }
            catch(LDAPException e) { };
        }
        return bound;
    }
}
