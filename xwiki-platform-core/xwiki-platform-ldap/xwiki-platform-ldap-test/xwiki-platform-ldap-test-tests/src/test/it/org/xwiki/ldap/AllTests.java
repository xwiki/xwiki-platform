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

import java.util.List;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.runner.RunWith;
import org.xwiki.ldap.framework.LDAPTestSetup;
import org.xwiki.test.integration.XWikiExecutor;
import org.xwiki.test.integration.XWikiExecutorSuite;
import org.xwiki.test.ui.PageObjectSuite;

/**
 * Runs all functional tests found in the classpath. This allows to start/stop XWiki only once.
 * 
 * @version $Id$
 */
@RunWith(PageObjectSuite.class)
public class AllTests
{
    @XWikiExecutorSuite.PreStart
    public void preStart(List<XWikiExecutor> executors) throws Exception
    {
        // Start LDAP Server
        LDAPTestSetup.before();

        // Setup executor
        XWikiExecutor executor = executors.get(0);

        // Prepare xwiki.cfg properties

        Properties currentXWikiConf = executor.loadXWikiCfg();

        currentXWikiConf.setProperty("xwiki.authentication.ldap", "1");
        currentXWikiConf.setProperty("xwiki.authentication.authclass",
            "com.xpn.xwiki.user.impl.LDAP.XWikiLDAPAuthServiceImpl");
        currentXWikiConf.setProperty("xwiki.authentication.ldap.server", LDAPTestSetup.LDAP_SERVER);
        currentXWikiConf.setProperty("xwiki.authentication.ldap.port", "" + LDAPTestSetup.getLDAPPort());
        currentXWikiConf.setProperty("xwiki.authentication.ldap.base_DN", LDAPTestSetup.LDAP_BASEDN);
        currentXWikiConf.setProperty("xwiki.authentication.ldap.bind_DN", LDAPTestSetup.LDAP_BINDDN_CN);
        currentXWikiConf.setProperty("xwiki.authentication.ldap.bind_pass", LDAPTestSetup.LDAP_BINDPASS_CN);
        currentXWikiConf.setProperty("xwiki.authentication.ldap.UID_attr", LDAPTestSetup.LDAP_USERUID_FIELD);
        currentXWikiConf.setProperty("xwiki.authentication.ldap.fields_mapping", "name="
            + LDAPTestSetup.LDAP_USERUID_FIELD + ",last_name=sn,first_name=givenname,fullname=description,email=mail");
        /*
         * CURRENTXWIKICONF.setProperty("xwiki.authentication.ldap.group_mapping", "XWiki.XWikiAdminGroup=cn=HMS
         * Lydia,ou=crews,ou=groups,o=sevenSeas");
         */
        currentXWikiConf.setProperty("xwiki.authentication.ldap.groupcache_expiration", "1");
        currentXWikiConf.setProperty("xwiki.authentication.ldap.user_group", LDAPTestSetup.HMSLYDIA_DN);
        currentXWikiConf.setProperty("xwiki.authentication.ldap.exclude_group", LDAPTestSetup.EXCLUSIONGROUP_DN);
        currentXWikiConf.setProperty("xwiki.authentication.ldap.validate_password", "0");
        currentXWikiConf.setProperty("xwiki.authentication.ldap.update_user", "1");
        currentXWikiConf.setProperty("xwiki.authentication.ldap.trylocal", "1");
        currentXWikiConf.setProperty("xwiki.authentication.ldap.mode_group_sync", "always");
        currentXWikiConf.setProperty("xwiki.authentication.ldap.ssl", "0");
        currentXWikiConf.setProperty("xwiki.authentication.ldap.ssl.keystore", "");

        executor.saveXWikiCfg(currentXWikiConf);
    }

    @AfterClass
    public static void afterClass() throws Exception
    {
        LDAPTestSetup.after();
    }
}
