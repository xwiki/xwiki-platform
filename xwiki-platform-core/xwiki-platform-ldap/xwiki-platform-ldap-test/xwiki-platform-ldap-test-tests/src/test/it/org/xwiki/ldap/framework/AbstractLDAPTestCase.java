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
package org.xwiki.ldap.framework;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;

import com.xpn.xwiki.test.MockitoOldcoreRule;

/**
 * Start LDAP embedded server if it's not already started.
 * 
 * @version $Id$
 */
public abstract class AbstractLDAPTestCase
{
    @Rule
    public MockitoOldcoreRule mocker = new MockitoOldcoreRule();

    /**
     * Tool to start and stop embedded LDAP server.
     */
    private static LDAPRunner ldap;

    @BeforeClass
    public static void beforeClass() throws Exception
    {
        if (System.getProperty(LDAPTestSetup.SYSPROPNAME_LDAPPORT) == null) {
            ldap = new LDAPRunner();
            ldap.start();
        }
    }

    @AfterClass
    public static void afterClass() throws Exception
    {
        if (ldap != null) {
            ldap.stop();
        }
    }
}
