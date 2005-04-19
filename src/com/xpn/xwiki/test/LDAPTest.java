package com.xpn.xwiki.test;

import junit.framework.TestCase;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiAuthService;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiCacheStoreInterface;
import com.opensymphony.module.access.provider.osuser.OSUserUserProvider;
import net.sf.hibernate.HibernateException;

import java.security.Principal;

/**
 * Created by IntelliJ IDEA.
 * User: Alex
 * Date: 18 avr. 2005
 * Time: 16:02:41
 * To change this template use File | Settings | File Templates.
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

    public void prepareData() throws XWikiException {
        XWikiDocument doc = new XWikiDocument("XWiki","akartmann");
        BaseClass bclass = xwiki.getUserClass(context);
        BaseObject bobj = new BaseObject();
        bobj.setName("XWiki.akartmann");
        bobj.setClassName(bclass.getName());
        bobj.setStringValue("fullname", "Alexis KARTMANN");
        bobj.setStringValue("email", "alexis@xwiki.com");
//        bobj.setStringValue("password", "toto");
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
    public void setUp() throws HibernateException, XWikiException {
        OSUserUserProvider toto = new OSUserUserProvider();
        context = new XWikiContext();
        context.setDatabase("xwikitest");
        xwiki = new XWiki("./xwiki.cfg", context);
        xwiki.setDatabase("xwikitest");
        context.setWiki(xwiki);
        StoreHibernateTest.cleanUp(getHibStore(), context);
        xwiki.flushCache();
    }

    public void tearDown() throws HibernateException {
        getHibStore().shutdownHibernate(context);
        xwiki = null;
        context = null;
        System.gc();
    }

    public void testCheckLogon() throws ClassNotFoundException, IllegalAccessException, InstantiationException, XWikiException {

        prepareData();
        Utils.setStringValue("XWiki.XWikiPreferences", "XWiki.XWikiPreferences", "ldap_server", "localhost", context);
        Utils.setStringValue("XWiki.XWikiPreferences", "XWiki.XWikiPreferences", "ldap_port", "389", context);
        Utils.setStringValue("XWiki.XWikiPreferences", "XWiki.XWikiPreferences", "ldap_base_DN", "dc=xwiki,dc=com", context);
        Utils.setStringValue("XWiki.XWikiPreferences", "XWiki.XWikiPreferences", "ldap_bind_DN", "cn=Manager,dc=xwiki,dc=com", context);
        Utils.setStringValue("XWiki.XWikiPreferences", "XWiki.XWikiPreferences", "ldap_bind_pass", "secret", context);
        Utils.setStringValue("XWiki.XWikiPreferences", "XWiki.XWikiPreferences", "ldap_UID_attr", "uid", context);

        XWikiAuthService service =  (XWikiAuthService) Class.forName("com.xpn.xwiki.user.impl.LDAP.LDAPAuthServiceImpl").newInstance();
        Principal principal = service.authenticate("akartmann", "alexis", context);
        assertNotNull("Authenticate failed", principal);
        assertEquals("Name is not equal", "XWiki.akartmann", principal.getName());
    }
}
