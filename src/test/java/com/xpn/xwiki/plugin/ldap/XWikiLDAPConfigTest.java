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
package com.xpn.xwiki.plugin.ldap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.web.XWikiEngineContext;

/**
 * Test {@link XWikiLDAPConfig};
 * 
 * @version $Id: $
 */
public class XWikiLDAPConfigTest extends TestCase
{
    private XWikiContext prefContext;

    private XWikiContext cfgContext;

    private static final Map PREFERENCES = new HashMap();

    private static final XWikiConfig CONFIG = new XWikiConfig();
    
    private static final Map RESULT_CFG_USERMAPPING = new Hashtable();
    private static final Map RESULT_PREF_USERMAPPING = new Hashtable();
    
    private static final Map RESULT_CFG_GROUPMAPPING = new Hashtable();
    private static final Map RESULT_PREF_GROUPMAPPING = new Hashtable();

    private static void addProperty(String prefName, String cfgName, String prefValue,
        String cfgValue)
    {
        PREFERENCES.put(prefName, prefValue);
        CONFIG.setProperty(cfgName, cfgValue);
    }

    static {
        CONFIG.setProperty("xwiki.authentication.ldap.authclass",
            "com.xpn.xwiki.user.impl.LDAP.LDAPAuthServiceImpl");

        addProperty("ldap", "xwiki.authentication.ldap", "0", "1");
        addProperty("ldap_server", "xwiki.authentication.ldap.server", "localhost", "127.0.0.1");
        addProperty("ldap_port", "xwiki.authentication.ldap.port", "10000", "11111");
        addProperty("ldap_check_level", "xwiki.authentication.ldap.check_level", "0", "1");
        addProperty("ldap_base_DN", "xwiki.authentication.ldap.base_DN", "o=sevenSeas",
            "o=sevenSeas2");
        addProperty("ldap_bind_DN", "xwiki.authentication.ldap.bind_DN",
            "cn={0},ou=people,o=sevenSeas", "cn={0},ou=people,o=sevenSeas2");
        addProperty("ldap_bind_pass", "xwiki.authentication.ldap.bind_pass", "{1}", "{1}2");
        addProperty("ldap_UID_attr", "xwiki.authentication.ldap.UID_attr", "uid", "uid2");
        addProperty("ldap_groupcache_expiration",
            "xwiki.authentication.ldap.groupcache_expiration", "10000", "11111");
        addProperty("ldap_user_group", "xwiki.authentication.ldap.user_group", "0", "1");
        addProperty("ldap_validate_password", "xwiki.authentication.ldap.validate_password", "1",
            "0");
        addProperty("ldap_update_user", "xwiki.authentication.ldap.update_user", "0", "1");
        addProperty("ldap_trylocal", "xwiki.authentication.ldap.trylocal", "0", "1");
        addProperty("ldap_mode_group_sync", "xwiki.authentication.ldap.mode_group_sync",
            "always", "create");

        addProperty("ldap_fields_mapping", "xwiki.authentication.ldap.fields_mapping",
            "name=uid,last_name=sn", "name=uid2,last_name=sn2");
        
        RESULT_PREF_USERMAPPING.put("uid", "name");
        RESULT_PREF_USERMAPPING.put("sn", "last_name");
        RESULT_CFG_USERMAPPING.put("uid2", "name");
        RESULT_CFG_USERMAPPING.put("sn2", "last_name");
        
        addProperty("ldap_group_mapping", "xwiki.authentication.ldap.group_mapping",
            "XWiki.XWikiAdminGroup=cn=HMS Toto,ou=crews,ou=groups,o=sevenSeas|XWiki.XWikiAdminGroup=cn=HMS Titi,ou=crews,ou=groups,o=sevenSeas",
            "XWiki.XWikiAdminGroup=cn=HMS Toto,ou=crews,ou=groups,o=sevenSeas2|XWiki.XWikiAdminGroup=cn=HMS Titi,ou=crews,ou=groups,o=sevenSeas2");
        
        RESULT_PREF_GROUPMAPPING.put("cn=HMS Toto,ou=crews,ou=groups,o=sevenSeas", "XWiki.XWikiAdminGroup");
        RESULT_PREF_GROUPMAPPING.put("cn=HMS Titi,ou=crews,ou=groups,o=sevenSeas", "XWiki.XWikiAdminGroup");
        RESULT_CFG_GROUPMAPPING.put("cn=HMS Toto,ou=crews,ou=groups,o=sevenSeas2", "XWiki.XWikiAdminGroup");
        RESULT_CFG_GROUPMAPPING.put("cn=HMS Titi,ou=crews,ou=groups,o=sevenSeas2", "XWiki.XWikiAdminGroup");
    }

    protected void setUp() throws XWikiException
    {
        this.prefContext = new XWikiContext();

        new XWiki(new XWikiConfig(), this.prefContext)
        {
            public void initXWiki(XWikiConfig config, XWikiContext context,
                XWikiEngineContext engine_context, boolean noupdate) throws XWikiException
            {
                context.setWiki(this);
                setConfig(config);
            }

            public String getXWikiPreference(String prefname, String default_value,
                XWikiContext context)
            {
                return PREFERENCES.get(prefname).toString();
            }
        };

        this.cfgContext = new XWikiContext();

        new XWiki(CONFIG, this.cfgContext)
        {
            public void initXWiki(XWikiConfig config, XWikiContext context,
                XWikiEngineContext engine_context, boolean noupdate) throws XWikiException
            {
                context.setWiki(this);
                setConfig(config);
            }

            public String getXWikiPreference(String prefname, String default_value,
                XWikiContext context)
            {
                return default_value;
            }
        };
    }

    // ///////////////////////////////////////////////////////////////////////////////////////:
    // Tests

    public void testGetLDAPParam1()
    {
        assertEquals("0", XWikiLDAPConfig.getInstance().getLDAPParam("ldap",
            "xwiki.authentication.ldap", null, prefContext));
        assertEquals("1", XWikiLDAPConfig.getInstance().getLDAPParam("ldap",
            "xwiki.authentication.ldap", null, cfgContext));
    }

    public void testGetLDAPParam2()
    {
        assertEquals("localhost", XWikiLDAPConfig.getInstance().getLDAPParam("ldap_server", null,
            prefContext));
        assertEquals("127.0.0.1", XWikiLDAPConfig.getInstance().getLDAPParam("ldap_server", null,
            cfgContext));
    }

    public void testIsLDAPEnabled()
    {
        assertEquals(false, XWikiLDAPConfig.getInstance().isLDAPEnabled(prefContext));
        assertEquals(true, XWikiLDAPConfig.getInstance().isLDAPEnabled(cfgContext));
    }

    public void testGetLDAPPort()
    {
        assertEquals(10000, XWikiLDAPConfig.getInstance().getLDAPPort(prefContext));
        assertEquals(11111, XWikiLDAPConfig.getInstance().getLDAPPort(cfgContext));
    }

    public void testGetGroupMappings()
    {
        Map prefMapping = XWikiLDAPConfig.getInstance().getGroupMappings(prefContext);
        
        assertEquals(RESULT_PREF_GROUPMAPPING, prefMapping);
        
        Map cfgMapping = XWikiLDAPConfig.getInstance().getGroupMappings(cfgContext);
        
        assertEquals(RESULT_CFG_GROUPMAPPING, cfgMapping);
    }

    public void testGetUserMappings()
    {
        List prefAttrList = new ArrayList();

        Map prefMapping = XWikiLDAPConfig.getInstance().getUserMappings(prefAttrList, prefContext);

        assertEquals("uid", (String) prefAttrList.get(0));
        assertEquals("sn", (String) prefAttrList.get(1));
        
        assertEquals(RESULT_PREF_USERMAPPING, prefMapping);
        
        // ///

        List cfgAttrList = new ArrayList();

        Map cfgMapping = XWikiLDAPConfig.getInstance().getUserMappings(cfgAttrList, cfgContext);

        assertEquals("uid2", (String) cfgAttrList.get(0));
        assertEquals("sn2", (String) cfgAttrList.get(1));
        
        assertEquals(RESULT_CFG_USERMAPPING, cfgMapping);
    }

    public void testGetCacheExpiration()
    {
        assertEquals(10000, XWikiLDAPConfig.getInstance().getCacheExpiration(prefContext));
        assertEquals(11111, XWikiLDAPConfig.getInstance().getCacheExpiration(cfgContext));
    }
}
