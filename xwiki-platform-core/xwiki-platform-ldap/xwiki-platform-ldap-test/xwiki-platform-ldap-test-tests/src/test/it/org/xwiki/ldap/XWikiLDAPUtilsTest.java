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
package org.xwiki.ldap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.ldap.framework.AbstractLDAPTestCase;
import org.xwiki.ldap.framework.LDAPTestSetup;
import org.xwiki.test.annotation.AfterComponent;
import org.xwiki.test.annotation.AllComponents;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.ldap.XWikiLDAPConnection;
import com.xpn.xwiki.plugin.ldap.XWikiLDAPSearchAttribute;
import com.xpn.xwiki.plugin.ldap.XWikiLDAPUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link XWikiLDAPUtils}.
 * 
 * @version $Id$
 */
// TODO: get rid of @AllComponents
@AllComponents
public class XWikiLDAPUtilsTest extends AbstractLDAPTestCase
{
    /**
     * The LDAP connection tool.
     */
    private XWikiLDAPConnection connection = new XWikiLDAPConnection();

    /**
     * The LDAP tool.
     */
    private XWikiLDAPUtils ldapUtils = new XWikiLDAPUtils(connection);

    @AfterComponent
    public void afterComponent()
    {
        // Unregister xwikicfg component so that it's replaced by a mock
        this.mocker.getMocker().unregisterComponent(ConfigurationSource.class, "xwikicfg");
    }

    @Before
    public void before() throws Exception
    {
        // Make sure to reset group cache so that one test data is not reused in another test
        XWikiLDAPUtils.resetGroupCache();

        this.mocker.getMockXWikiCfg().setProperty("xwiki.authentication.ldap", "1");
        this.mocker.getMockXWikiCfg().setProperty("xwiki.authentication.ldap.server", LDAPTestSetup.LDAP_SERVER);
        this.mocker.getMockXWikiCfg().setProperty("xwiki.authentication.ldap.port", "" + LDAPTestSetup.getLDAPPort());
        this.mocker.getMockXWikiCfg().setProperty("xwiki.authentication.ldap.base_DN", LDAPTestSetup.LDAP_BASEDN);
        this.mocker.getMockXWikiCfg().setProperty("xwiki.authentication.ldap.bind_DN", LDAPTestSetup.LDAP_BINDDN_CN);
        this.mocker.getMockXWikiCfg().setProperty("xwiki.authentication.ldap.bind_pass",
            LDAPTestSetup.LDAP_BINDPASS_CN);
        this.mocker.getMockXWikiCfg().setProperty("xwiki.authentication.ldap.UID_attr",
            LDAPTestSetup.LDAP_USERUID_FIELD);
        this.mocker.getMockXWikiCfg().setProperty("xwiki.authentication.ldap.groupcache_expiration", "1");

        this.ldapUtils.setUidAttributeName(LDAPTestSetup.LDAP_USERUID_FIELD);
        this.ldapUtils.setBaseDN(LDAPTestSetup.LDAP_BASEDN);

        int port = LDAPTestSetup.getLDAPPort();

        this.connection.open("localhost", port, LDAPTestSetup.HORATIOHORNBLOWER_DN, LDAPTestSetup.HORATIOHORNBLOWER_PWD,
            null, false, this.mocker.getXWikiContext());
    }

    @After
    public void tearDown() throws Exception
    {
        this.connection.close();
    }

    // Tests

    /**
     * Verify if the user uid attribute name has been correctly set.
     */
    @Test
    public void testGetUidAttributeName()
    {
        assertSame("Wrong uid attribute name", LDAPTestSetup.LDAP_USERUID_FIELD, this.ldapUtils.getUidAttributeName());
    }

    /**
     * Check that the cache is not created each time it's retrieved and correctly handle refresh time.
     */
    @Test
    public void testCache() throws CacheException
    {
        CacheConfiguration cacheConfigurationGroups = new CacheConfiguration();
        cacheConfigurationGroups.setConfigurationId("ldap.groups");

        Cache<Map<String, String>> tmpCache =
            this.ldapUtils.getGroupCache(cacheConfigurationGroups, this.mocker.getXWikiContext());
        Cache<Map<String, String>> cache =
            this.ldapUtils.getGroupCache(cacheConfigurationGroups, this.mocker.getXWikiContext());

        assertSame("Cache is recreated", tmpCache, cache);
    }

    @Test
    public void testSearchUserAttributesByUid()
    {
        List<XWikiLDAPSearchAttribute> attributes =
            this.ldapUtils.searchUserAttributesByUid("Moultrie Crystal", new String[] {"dn", "cn"});

        Map<String, String> mexpected = new HashMap<String, String>();
        mexpected.put("dn", "cn=Moultrie Crystal,ou=people,o=sevenSeas");
        mexpected.put("cn", "Moultrie Crystal");

        Map<String, String> mresult = new HashMap<String, String>();
        for (XWikiLDAPSearchAttribute att : attributes) {
            mresult.put(att.name, att.value);
        }

        assertEquals(mexpected, mresult);
    }

    @Test
    public void testSearchUserDNByUid()
    {
        String userDN = this.ldapUtils.searchUserDNByUid(LDAPTestSetup.HORATIOHORNBLOWER_CN);

        assertEquals(LDAPTestSetup.HORATIOHORNBLOWER_DN, userDN);
    }

    /**
     * Test {@link XWikiLDAPUtils#getGroupMembers(String, XWikiContext)}.
     * 
     * @throws XWikiException error when getting group members from cache.
     */
    @Test
    public void testGetGroupMembers() throws XWikiException
    {
        // HMS Lydia

        Map<String, String> hmslydiamembers =
            this.ldapUtils.getGroupMembers(LDAPTestSetup.HMSLYDIA_DN, this.mocker.getXWikiContext());

        assertFalse("No member was found", hmslydiamembers.isEmpty());

        assertEquals(LDAPTestSetup.HMSLYDIA_MEMBERS, hmslydiamembers.keySet());

        XWikiLDAPUtils.resetGroupCache();

        hmslydiamembers = this.ldapUtils.getGroupMembers("cn=HMS Lydia", this.mocker.getXWikiContext());

        assertFalse("No member was found", hmslydiamembers.isEmpty());

        XWikiLDAPUtils.resetGroupCache();

        hmslydiamembers = this.ldapUtils.getGroupMembers("(cn=HMS Lydia)", this.mocker.getXWikiContext());

        assertFalse("No member was found", hmslydiamembers.isEmpty());

        assertEquals(LDAPTestSetup.HMSLYDIA_MEMBERS, hmslydiamembers.keySet());

        // Top group

        Map<String, String> topGroupMembers =
            this.ldapUtils.getGroupMembers(LDAPTestSetup.TOPGROUP_DN, this.mocker.getXWikiContext());

        assertFalse("No member was found", topGroupMembers.isEmpty());

        assertEquals(LDAPTestSetup.TOPGROUP_MEMBERS, topGroupMembers.keySet());

        XWikiLDAPUtils.resetGroupCache();

        topGroupMembers = this.ldapUtils.getGroupMembers("Top group", this.mocker.getXWikiContext());

        assertFalse("No member was found", topGroupMembers.isEmpty());

        assertEquals(LDAPTestSetup.TOPGROUP_MEMBERS, topGroupMembers.keySet());

        XWikiLDAPUtils.resetGroupCache();

        topGroupMembers = this.ldapUtils.getGroupMembers("(cn=Top group)", this.mocker.getXWikiContext());

        assertFalse("No member was found", topGroupMembers.isEmpty());

        assertEquals(LDAPTestSetup.TOPGROUP_MEMBERS, topGroupMembers.keySet());

        // Top group with disabled subgroups

        this.ldapUtils.setResolveSubgroups(false);
        XWikiLDAPUtils.resetGroupCache();

        Map<String, String> topGroupMembersNoResolve =
            this.ldapUtils.getGroupMembers(LDAPTestSetup.TOPGROUP_DN, this.mocker.getXWikiContext());

        assertFalse("No member was found", topGroupMembersNoResolve.isEmpty());

        assertEquals(LDAPTestSetup.TOPGROUP_MEMBERS_NORESOLVE, topGroupMembersNoResolve.keySet());

        topGroupMembersNoResolve = this.ldapUtils.getGroupMembers("Top group", this.mocker.getXWikiContext());

        assertFalse("No member was found", topGroupMembersNoResolve.isEmpty());

        assertEquals(LDAPTestSetup.TOPGROUP_MEMBERS_NORESOLVE, topGroupMembersNoResolve.keySet());

        topGroupMembersNoResolve = this.ldapUtils.getGroupMembers("(cn=Top group)", this.mocker.getXWikiContext());

        assertFalse("No member was found", topGroupMembersNoResolve.isEmpty());

        assertEquals(LDAPTestSetup.TOPGROUP_MEMBERS_NORESOLVE, topGroupMembersNoResolve.keySet());

        // Wrong group

        XWikiLDAPUtils.resetGroupCache();

        Map<String, String> wrongGroupMembers =
            this.ldapUtils.getGroupMembers("cn=wronggroupdn,ou=people,o=sevenSeas", this.mocker.getXWikiContext());

        assertNull("Should return null if group does not exists [" + wrongGroupMembers + "]", wrongGroupMembers);
    }

    /**
     * Test {@link XWikiLDAPUtils#isUidInGroup(String, String, XWikiContext)} by passing CN value.
     * 
     * @throws XWikiException error when getting group members from cache.
     */
    @Test
    public void testIsUserInGroupByCN() throws XWikiException
    {
        String userDN = this.ldapUtils.isUidInGroup(LDAPTestSetup.HORATIOHORNBLOWER_CN, LDAPTestSetup.HMSLYDIA_DN,
            this.mocker.getXWikiContext());

        assertNotNull("User " + LDAPTestSetup.HORATIOHORNBLOWER_CN + " not found", userDN);
        assertEquals(LDAPTestSetup.HORATIOHORNBLOWER_DN.toLowerCase(), userDN);
    }

    /**
     * Test {@link XWikiLDAPUtils#isUidInGroup(String, String, XWikiContext)} by passing UID value.
     * 
     * @throws XWikiException error when getting group members from cache.
     */
    @Test
    public void testIsUserInGroupByUID() throws XWikiException
    {
        this.ldapUtils.setUidAttributeName(LDAPTestSetup.LDAP_USERUID_FIELD_UID);

        String userDN = this.ldapUtils.isUidInGroup(LDAPTestSetup.WILLIAMBUSH_UID, LDAPTestSetup.HMSLYDIA_DN,
            this.mocker.getXWikiContext());

        assertNotNull("User " + LDAPTestSetup.WILLIAMBUSH_UID + " not found", userDN);
        assertEquals(LDAPTestSetup.WILLIAMBUSH_DN.toLowerCase(), userDN);
    }

    /**
     * Test {@link XWikiLDAPUtils#isUidInGroup(String, String, XWikiContext)} by passing UID value.
     * 
     * @throws XWikiException error when getting group members from cache.
     */
    @Test
    public void testIsUserInGroupWithWrongId() throws XWikiException
    {
        String wrongUserDN =
            this.ldapUtils.isUidInGroup("wronguseruid", LDAPTestSetup.HMSLYDIA_DN, this.mocker.getXWikiContext());

        assertNull("Should return null if user is not in the group", wrongUserDN);
    }

    /**
     * Test {@link XWikiLDAPUtils#isMemberOfGroup(String, String, XWikiContext)}.
     * 
     * @throws XWikiException error when getting group members from cache.
     */
    @Test
    public void testIsMemberOfGroup() throws XWikiException
    {
        assertTrue(this.ldapUtils.isMemberOfGroup(LDAPTestSetup.HORATIOHORNBLOWER_DN, LDAPTestSetup.HMSLYDIA_DN,
            this.mocker.getXWikiContext()));

        assertFalse(this.ldapUtils.isMemberOfGroup(LDAPTestSetup.HORATIOHORNBLOWER_DN, LDAPTestSetup.EXCLUSIONGROUP_DN,
            this.mocker.getXWikiContext()));
    }

    /**
     * Test {@link XWikiLDAPUtils#isMemberOfGroups(String, java.util.Collection, XWikiContext)}.
     * 
     * @throws XWikiException error when getting group members from cache.
     */
    @Test
    public void testIsMemberOfGroups() throws XWikiException
    {
        assertTrue(this.ldapUtils.isMemberOfGroups(LDAPTestSetup.HORATIOHORNBLOWER_DN,
            Arrays.asList(LDAPTestSetup.HMSLYDIA_DN, LDAPTestSetup.EXCLUSIONGROUP_DN), this.mocker.getXWikiContext()));

        assertTrue(this.ldapUtils.isMemberOfGroups(LDAPTestSetup.HORATIOHORNBLOWER_DN,
            Arrays.asList(LDAPTestSetup.EXCLUSIONGROUP_DN, LDAPTestSetup.HMSLYDIA_DN), this.mocker.getXWikiContext()));
    }
}
