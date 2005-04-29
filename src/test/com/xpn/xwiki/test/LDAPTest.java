package com.xpn.xwiki.test;

import junit.framework.TestCase;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.web.XWikiServletURLFactory;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiAuthService;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiCacheStoreInterface;
import com.novell.ldap.*;

import java.security.Principal;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.net.URL;
import java.net.MalformedURLException;

import org.hibernate.HibernateException;

/**
 * Created by IntelliJ IDEA.
 * User: Alex
 * Date: 18 avr. 2005
 * Time: 16:02:41
 * Test for LDAP authentication
 * ***********************************************************************
 * This test need a TEST local LDAP server
 * ***********************************************************************
 * BEWARE : THIS TEST DELETE ALL LDAP SERVER CONTENTS !
 * NEVER USE IT WITH PRODUCTION SERVER !
 * ***********************************************************************
 * If you use OpenLDAP server, the sldap.conf must containts the statements :
 *
 *       include	/etc/schema/cosine.schema
 *       include	/etc/schema/inetorgperson.schema
 *       access to *
 *          by self read
 *          by anonymous auth
 *          by * none
 *       suffix		"dc=xwiki,dc=com"
 *       rootdn		"cn=Manager,dc=xwiki,dc=com"
 *       rootpw		secret
 * Other default values should work
 *
 * Test works with out of the box ldap database.
 */
public class LDAPTest  extends TestCase {
    private XWiki xwiki;
    private XWikiContext context;

    public static boolean inTest = false;

    public XWikiHibernateStore getHibStore() {
        XWikiStoreInterface store = xwiki.getStore();
        if (store instanceof XWikiCacheStoreInterface)
            return (XWikiHibernateStore)((XWikiCacheStoreInterface)store).getStore();
        else
            return (XWikiHibernateStore) store;
    }

    public void prepareLDAP(boolean addUser, String userPassword)
    {
        int ldapPort = context.getWiki().getXWikiPreferenceAsInt("ldap_port", LDAPConnection.DEFAULT_PORT, context);
        int ldapVersion = LDAPConnection.LDAP_V3;
        String ldapHost = context.getWiki().getXWikiPreference("ldap_server",context);
        String loginDN = context.getWiki().getXWikiPreference("ldap_bind_DN",context);
        String password = context.getWiki().getXWikiPreference("ldap_bind_pass",context);

        String containerName  =  context.getWiki().getXWikiPreference("ldap_base_DN",context);

        try
        {
            LDAPConnection lc = new LDAPConnection();

            lc.connect( ldapHost, ldapPort );
            lc.bind( ldapVersion, loginDN, password.getBytes("UTF8") );

            boolean hasRoot = false;
            boolean attributeOnly = true;
            String attrs[] = {LDAPConnection.NO_ATTRS};
            LDAPSearchResults searchResults =
                lc.search(  containerName,
                            LDAPConnection.SCOPE_SUB,
                            "(objectclass=*)",
                            attrs,
                            attributeOnly);
            if (searchResults.hasMore())
            {
                ArrayList l = new ArrayList();
                // print out all the objects
                while ( searchResults.hasMore()) {
                    LDAPEntry nextEntry = null;
                    try {
                        nextEntry = searchResults.next();
                    }
                    catch(LDAPException e) {
                        System.out.println("Error: " + e.toString());
                        continue;
                    }
                    hasRoot = true;
                    l.add(nextEntry.getDN());
                }

                // Don't delete root
                for(int i=l.size(); --i>0;)
                {
                    String dn = (String)l.get(i);
                    lc.delete(dn);
                }
            }

            LDAPAttribute  attribute = null;
            LDAPEntry newEntry = null;
            LDAPAttributeSet attributeSet = new LDAPAttributeSet();
            String  dn;
            if (!hasRoot)
            {
                // Root creation
                attributeSet.add( new LDAPAttribute(
                                     "dc", new String("xwiki")));
                attributeSet.add( new LDAPAttribute(
                                     "o", new String("XWiki")));
                attributeSet.add( new LDAPAttribute("objectClass",
                        new String[]{"dcObject", "organization", "top"}));
                dn  = containerName;
                newEntry = new LDAPEntry( dn, attributeSet );

                lc.add( newEntry );
            }

            // Add manager branch
            attributeSet = new LDAPAttributeSet();
            attributeSet.add( new LDAPAttribute(
                                 "cn", new String("Manager")));
            attributeSet.add( new LDAPAttribute("objectClass",
                    new String[]{"organizationalRole", "top"}));
            dn  = "cn=Manager," + containerName;
            newEntry = new LDAPEntry( dn, attributeSet );
            lc.add( newEntry );

            if (addUser)
            {
                attributeSet = new LDAPAttributeSet();
                attributeSet.add( new LDAPAttribute(
                                     "cn", new String("akartmann")));
                attributeSet.add( new LDAPAttribute(
                                     "uid", new String("akartmann")));
                attributeSet.add( new LDAPAttribute(
                                     "sn", new String("KARTMANN")));
                attributeSet.add( new LDAPAttribute(
                                     "givenName", new String("Alexis")));
                attributeSet.add( new LDAPAttribute(
                                     "displayName", new String("Alexis KARTMANN")));
                attributeSet.add( new LDAPAttribute(
                                     "mail", new String("alexis@xwiki.com")));
                attributeSet.add( new LDAPAttribute(
                                     "userPassword", userPassword));
                attributeSet.add( new LDAPAttribute("objectClass",
                        new String[]{"inetOrgPerson", "top", "uidObject"}));
                dn  = "cn=akartmann,cn=Manager," + containerName;
                newEntry = new LDAPEntry( dn, attributeSet );
                lc.add( newEntry );
            }


            lc.disconnect();
        }
        catch( LDAPException e ) {
            System.out.println( "Error:  " + e.toString());
        }
        catch( UnsupportedEncodingException e ) {
            System.out.println( "Error: " + e.toString() );
        }
    }

    public void prepareData(boolean withLDAPDN, boolean withpassword) throws XWikiException {
        XWikiDocument doc = new XWikiDocument("XWiki","akartmann");
        try {
            xwiki.getDocument(doc, null, context);
            xwiki.deleteDocument(doc, context);
        } catch (XWikiException e) {
        }

        BaseClass bclass = xwiki.getUserClass(context);
        BaseObject bobj = new BaseObject();
        bobj.setName("XWiki.akartmann");
        bobj.setClassName(bclass.getName());
        bobj.setStringValue("fullname", "Alexis KARTMANN");
        bobj.setStringValue("email", "alexis@xwiki.com");
        if (withLDAPDN)
            bobj.setStringValue("ldap_dn", "cn=akartmann,cn=Manager,dc=xwiki,dc=com");
        if (withpassword)
            bobj.setStringValue("password", "toto");
        doc.setObject(bclass.getName(), 0, bobj);
        doc.setContent("---+ Alexis KARTMANN HomePage");
        xwiki.saveDocument(doc, context);

        doc = new XWikiDocument("XWiki","AdminGroup");
        bclass = xwiki.getGroupClass(context);
        bobj = new BaseObject();
        bobj.setName("XWiki.AdminGroup");
        bobj.setClassName(bclass.getName());
        bobj.setStringValue("member", "XWiki.akartmann");
        doc.setObject(bclass.getName(), 0, bobj);
        doc.setContent("---+ AdminGroup");
        xwiki.saveDocument(doc, context);

        doc = new XWikiDocument("Test","TestDoc");
        doc.setContent("---+ TestDoc");
        xwiki.saveDocument(doc, context);

    }
    public void setUp() throws HibernateException, XWikiException, MalformedURLException {
        context = new XWikiContext();
        context.setDatabase("xwikitest");
        xwiki = new XWiki("./xwiki.cfg", context);
        xwiki.setDatabase("xwikitest");
        context.setWiki(xwiki);
        StoreHibernateTest.cleanUp(getHibStore(), context);
        xwiki.flushCache();
        context.setURLFactory(new XWikiServletURLFactory(new URL("http://www.xwiki.org/"), "xwiki/" , "bin/"));

        Utils.setStringValue("XWiki.XWikiPreferences", "XWiki.XWikiPreferences", "ldap_server", "localhost", context);
        Utils.setStringValue("XWiki.XWikiPreferences", "XWiki.XWikiPreferences", "ldap_port", "389", context);
        Utils.setStringValue("XWiki.XWikiPreferences", "XWiki.XWikiPreferences", "ldap_base_DN", "dc=xwiki,dc=com", context);
        Utils.setStringValue("XWiki.XWikiPreferences", "XWiki.XWikiPreferences", "ldap_bind_DN", "cn=Manager,dc=xwiki,dc=com", context);
        Utils.setStringValue("XWiki.XWikiPreferences", "XWiki.XWikiPreferences", "ldap_bind_pass", "secret", context);
        Utils.setStringValue("XWiki.XWikiPreferences", "XWiki.XWikiPreferences", "ldap_UID_attr", "uid", context);

    }

    public void tearDown() throws HibernateException {
        getHibStore().shutdownHibernate(context);
        xwiki = null;
        context = null;
        System.gc();
    }

    public void testCheckLogonWithBind() throws ClassNotFoundException, IllegalAccessException, InstantiationException, XWikiException {

        prepareLDAP(true, "alexis");
        prepareData(false, false);

        XWikiAuthService service =  (XWikiAuthService) Class.forName("com.xpn.xwiki.user.impl.LDAP.LDAPAuthServiceImpl").newInstance();
        Principal principal = service.authenticate("akartmann", "alexis", context);
        assertNotNull("Authenticate failed", principal);
        assertEquals("Name is not equal", "XWiki.akartmann", principal.getName());
    }

    public void testCheckLogonWithoutBind() throws ClassNotFoundException, IllegalAccessException, InstantiationException, XWikiException {

        prepareLDAP(true, "alexis");
        prepareData(true, false);

        Utils.setStringValue("XWiki.XWikiPreferences", "XWiki.XWikiPreferences", "ldap_bind_DN", "", context);
        Utils.setStringValue("XWiki.XWikiPreferences", "XWiki.XWikiPreferences", "ldap_bind_pass", "", context);

        XWikiAuthService service =  (XWikiAuthService) Class.forName("com.xpn.xwiki.user.impl.LDAP.LDAPAuthServiceImpl").newInstance();
        Principal principal = service.authenticate("akartmann", "alexis", context);
        assertNotNull("Authenticate failed", principal);
        assertEquals("Name is not equal", "XWiki.akartmann", principal.getName());
    }

    public void testCheckLogonWithUserBind() throws ClassNotFoundException, IllegalAccessException, InstantiationException, XWikiException {

        prepareLDAP(true, "alexis");
        prepareData(false, false);

        Utils.setStringValue("XWiki.XWikiPreferences", "XWiki.XWikiPreferences", "ldap_bind_DN", "cn={0},cn=Manager,dc=xwiki,dc=com", context);
        Utils.setStringValue("XWiki.XWikiPreferences", "XWiki.XWikiPreferences", "ldap_bind_pass", "{1}", context);

        XWikiAuthService service =  (XWikiAuthService) Class.forName("com.xpn.xwiki.user.impl.LDAP.LDAPAuthServiceImpl").newInstance();
        Principal principal = service.authenticate("akartmann", "alexis", context);
        assertNotNull("Authenticate failed", principal);
        assertEquals("Name is not equal", "XWiki.akartmann", principal.getName());
    }

    public void testCheckLogonWithBadBind() throws ClassNotFoundException, IllegalAccessException, InstantiationException, XWikiException {

        prepareLDAP(true, "alexis");
        prepareData(true, false);

        Utils.setStringValue("XWiki.XWikiPreferences", "XWiki.XWikiPreferences", "ldap_bind_DN", "cn=nothere", context);
        Utils.setStringValue("XWiki.XWikiPreferences", "XWiki.XWikiPreferences", "ldap_bind_pass", "bad", context);

        XWikiAuthService service =  (XWikiAuthService) Class.forName("com.xpn.xwiki.user.impl.LDAP.LDAPAuthServiceImpl").newInstance();
        Principal principal = service.authenticate("akartmann", "alexis", context);
        assertNotNull("Authenticate failed", principal);
        assertEquals("Name is not equal", "XWiki.akartmann", principal.getName());
    }

    public void testCheckLogonFromWiki() throws ClassNotFoundException, IllegalAccessException, InstantiationException, XWikiException {

        prepareLDAP(false, null);
        prepareData(false, true);

        XWikiAuthService service =  (XWikiAuthService) Class.forName("com.xpn.xwiki.user.impl.LDAP.LDAPAuthServiceImpl").newInstance();
        Principal principal = service.authenticate("akartmann", "toto", context);
        assertNotNull("Authenticate failed", principal);
        assertEquals("Name is not equal", "XWiki.akartmann", principal.getName());
    }

    public void testCheckLogonFromWikiBadLDAP() throws ClassNotFoundException, IllegalAccessException, InstantiationException, XWikiException {

        prepareLDAP(false, null);
        prepareData(true, true);

        XWikiAuthService service =  (XWikiAuthService) Class.forName("com.xpn.xwiki.user.impl.LDAP.LDAPAuthServiceImpl").newInstance();
        Principal principal = service.authenticate("akartmann", "toto", context);
        assertNull("Authenticate failed", principal);
    }

    public void testCheckLogonKOFromWikiPassword() throws ClassNotFoundException, IllegalAccessException, InstantiationException, XWikiException {

        prepareLDAP(true, "alexis");
        prepareData(true, true);

        XWikiAuthService service =  (XWikiAuthService) Class.forName("com.xpn.xwiki.user.impl.LDAP.LDAPAuthServiceImpl").newInstance();
        Principal principal = service.authenticate("akartmann", "toto", context);
        assertNull("Authenticate failed", principal);
    }

    public void testCheckLogonLevel() throws ClassNotFoundException, IllegalAccessException, InstantiationException, XWikiException {

        prepareLDAP(true, "alexis");
        prepareData(false, false);

        // Full check : bind, search, password
        Utils.setStringValue("XWiki.XWikiPreferences", "XWiki.XWikiPreferences", "ldap_check_level", "2", context);

        XWikiAuthService service =  (XWikiAuthService) Class.forName("com.xpn.xwiki.user.impl.LDAP.LDAPAuthServiceImpl").newInstance();
        Principal principal = service.authenticate("akartmann", "alexis", context);
        assertNotNull("Authenticate failed", principal);
        assertEquals("Name is not equal", "XWiki.akartmann", principal.getName());

        // Integrated check : bind, search
        Utils.setStringValue("XWiki.XWikiPreferences", "XWiki.XWikiPreferences", "ldap_check_level", "1", context);
        prepareLDAP(true, "notme");

        service =  (XWikiAuthService) Class.forName("com.xpn.xwiki.user.impl.LDAP.LDAPAuthServiceImpl").newInstance();
        principal = service.authenticate("akartmann", "alexis", context);
        assertNotNull("Authenticate failed", principal);
        assertEquals("Name is not equal", "XWiki.akartmann", principal.getName());

        // Trivial check : bind
        Utils.setStringValue("XWiki.XWikiPreferences", "XWiki.XWikiPreferences", "ldap_check_level", "0", context);
        prepareLDAP(false, null);

        service =  (XWikiAuthService) Class.forName("com.xpn.xwiki.user.impl.LDAP.LDAPAuthServiceImpl").newInstance();
        principal = service.authenticate("akartmann", "alexis", context);
        assertNotNull("Authenticate failed", principal);
        assertEquals("Name is not equal", "XWiki.akartmann", principal.getName());

     }

    public void testTransfertUserFromLDAP() throws ClassNotFoundException, XWikiException, IllegalAccessException, InstantiationException {
        XWikiDocument doc = new XWikiDocument("XWiki","akartmann");
        try {
            xwiki.getDocument(doc, null, context);
            xwiki.deleteDocument(doc, context);
        } catch (XWikiException e) {
        }
        prepareLDAP(true, "alexis");
        assertEquals("getUserName failed", "akartmann", xwiki.getUserName("XWiki.akartmann", context));
        assertEquals("getUserName failed", "akartmann", xwiki.getLocalUserName("XWiki.akartmann", context));
        assertEquals("getUserName failed", "akartmann", xwiki.getLocalUserName("xwiki:XWiki.akartmann", context));

        Utils.setStringValue("XWiki.XWikiPreferences", "XWiki.XWikiPreferences", "ldap_fields_mapping",
                "name=cn,last_name=sn,first_name=givenName,fullname=displayName,mail=mail,ldap_dn=dn", context);

        XWikiAuthService service =  (XWikiAuthService) Class.forName("com.xpn.xwiki.user.impl.LDAP.LDAPAuthServiceImpl").newInstance();
        Principal principal = service.authenticate("akartmann", "alexis", context);
        assertNotNull("Authenticate failed", principal);
        assertEquals("Name is not equal", "XWiki.akartmann", principal.getName());
        String result = xwiki.getUserName("XWiki.akartmann", context);
        assertEquals("getUserName failed", "<span class=\"wikilink\"><a href=\"/xwiki/bin/view/XWiki/akartmann\">Alexis KARTMANN</a></span>", result );
        result = xwiki.getUserName("xwikitest:XWiki.akartmann", context);
        assertEquals("getUserName failed", "<span class=\"wikilink\"><a href=\"/xwiki/bin/view/XWiki/akartmann\">Alexis KARTMANN</a></span>", result);
        result = xwiki.getLocalUserName("XWiki.akartmann", context);
        assertEquals("getLocalUserName failed", "<span class=\"wikilink\"><a href=\"/xwiki/bin/view/XWiki/akartmann\">Alexis KARTMANN</a></span>", result);
        result = xwiki.getLocalUserName("xwikitest:XWiki.akartmann", context);
        assertEquals("getLocalUserName failed", "<span class=\"wikilink\"><a href=\"/xwiki/bin/view/XWiki/akartmann\">Alexis KARTMANN</a></span>", result);
        result = xwiki.getLocalUserName("XWiki.akartmann", "$last_name", context);
        assertEquals("getLocalUserName failed", "<span class=\"wikilink\"><a href=\"/xwiki/bin/view/XWiki/akartmann\">KARTMANN</a></span>", result);
        result = xwiki.getLocalUserName("XWiki.akartmann", "$last_name", false, context);
        assertEquals("getLocalUserName failed", "KARTMANN", result);
        result = xwiki.getLocalUserName("XWiki.akartmann", "$first_name", context);
        assertEquals("getLocalUserName failed", "<span class=\"wikilink\"><a href=\"/xwiki/bin/view/XWiki/akartmann\">Alexis</a></span>", result);
        result = xwiki.getLocalUserName("XWiki.akartmann", "$first_name", false, context);
        assertEquals("getLocalUserName failed", "Alexis", result);
    }
}
