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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utils to manage LDAP test setup.
 * 
 * @version $Id$
 */
public class LDAPTestSetup
{
    public static final String LDAP_SERVER = "127.0.0.1";

    /**
     * The LDAP base DN from where to executes LDAP queries.
     */
    public static final String LDAP_BASEDN = "o=sevenSeas";

    public static final String LDAP_BINDDN_CN = "cn={0},ou=people,o=sevenSeas";

    public static final String LDAP_BINDPASS_CN = "{1}";

    /**
     * The name of the LDAP property containing user unique id (cn).
     */
    public static final String LDAP_USERUID_FIELD = "cn";

    /**
     * The name of the LDAP property containing user unique id (uid).
     */
    public static final String LDAP_USERUID_FIELD_UID = "uid";

    /**
     * The name of the system property containing the LDAP embedded server port.
     */
    public static final String SYSPROPNAME_LDAPPORT = "ldap_port";

    // Somes datas examples

    /**
     * The OU containing the users.
     */
    public static final String USERS_OU = "ou=people,o=sevenSeas";

    /**
     * The LDAP DN of user Horatio Hornblower.
     */
    public static final String HORATIOHORNBLOWER_DN = "cn=Horatio Hornblower," + USERS_OU;

    /**
     * The LDAP unique id of user Horatio Hornblower.
     */
    public static final String HORATIOHORNBLOWER_CN = "Horatio Hornblower";

    /**
     * The LDAP password of user Horatio Hornblower.
     */
    public static final String HORATIOHORNBLOWER_PWD = "pass";

    /**
     * The value of the LDAP sn for user Horatio Hornblower.
     */
    public static final String HORATIOHORNBLOWER_SN = "Hornblower";

    /**
     * The value of the LDAP givenName for user Horatio Hornblower.
     */
    public static final String HORATIOHORNBLOWER_GIVENNAME = "Horatio";

    /**
     * The value of the LDAP mail for user Horatio Hornblower.
     */
    public static final String HORATIOHORNBLOWER_MAIL = "hhornblo@royalnavy.mod.uk";

    public static final List<String> HORATIOHORNBLOWER_DESCRIPTION = Arrays.<String>asList("Capt. Horatio Hornblower, R.N", "value2", "value3");

    /**
     * The LDAP DN of user Thomas Quist.
     */
    public static final String THOMASQUIST_DN = "cn=Thomas Quist," + USERS_OU;

    /**
     * The LDAP common name of user Thomas Quist.
     */
    public static final String THOMASQUIST_CN = "Thomas Quist";

    /**
     * The LDAP password of user Thomas Quist.
     */
    public static final String THOMASQUIST_PWD = "pass";

    /**
     * The LDAP DN of user William Bush.
     */
    public static final String WILLIAMBUSH_DN = "cn=William Bush," + USERS_OU;

    /**
     * The LDAP password of user William Bush.
     */
    public static final String WILLIAMBUSH_PWD = "pass";

    /**
     * The LDAP unique id of user William Bush.
     */
    public static final String WILLIAMBUSH_UID = "wbush";

    /**
     * The LDAP unique id with mixed case of user William Bush.
     */
    public static final String WILLIAMBUSH_UID_MIXED = "wBush";

    /**
     * The LDAP DN of user User.With.Points.
     */
    public static final String USERWITHPOINTS_DN = "cn=User.With.Points," + USERS_OU;

    /**
     * The LDAP password of user User.With.Points.
     */
    public static final String USERWITHPOINTS_PWD = "pass";

    /**
     * The LDAP common name of user User.With.Points.
     */
    public static final String USERWITHPOINTS_CN = "User.With.Points";

    /**
     * The LDAP unique id of user User.With.Points.
     */
    public static final String USERWITHPOINTS_UID = "user.with.points";

    /**
     * The LDAP DN of user User.WithPoints.
     */
    public static final String OTHERUSERWITHPOINTS_DN = "cn=User.WithPoints,ou=people,o=sevenSeas";

    /**
     * The LDAP password of user User.WithPoints.
     */
    public static final String OTHERUSERWITHPOINTS_PWD = "pass";

    /**
     * The LDAP common name of user User.WithPoints.
     */
    public static final String OTHERUSERWITHPOINTS_CN = "User.WithPoints";

    /**
     * The LDAP unique id of user User.WithPoints.
     */
    public static final String OTHERUSERWITHPOINTS_UID = "user.withpoints";

    /**
     * The LDAP DN of group HMS Bounty.
     */
    public static final String HMSBOUNTY_DN = "cn=HMS Bounty,ou=crews,ou=groups,o=sevenSeas";

    /**
     * The LDAP DN of group HMS Lydia.
     */
    public static final String HMSLYDIA_DN = "cn=HMS Lydia,ou=crews,ou=groups,o=sevenSeas";

    /**
     * The LDAP DN of group to exclude from login.
     */
    public static final String EXCLUSIONGROUP_DN = "cn=Exlude Group,ou=crews,ou=groups,o=sevenSeas";

    /**
     * The LDAP members of group HMS Lydia.
     */
    public static final Set<String> HMSLYDIA_MEMBERS = new HashSet<String>();

    static {
        HMSLYDIA_MEMBERS.add(HORATIOHORNBLOWER_DN.toLowerCase());
        HMSLYDIA_MEMBERS.add(WILLIAMBUSH_DN.toLowerCase());
        HMSLYDIA_MEMBERS.add(THOMASQUIST_DN.toLowerCase());
        HMSLYDIA_MEMBERS.add("cn=Moultrie Crystal,ou=people,o=sevenSeas".toLowerCase());
        HMSLYDIA_MEMBERS.add("cn=User.With.Points,ou=people,o=sevenSeas".toLowerCase());
    }

    /**
     * The LDAP DN of group Top group.
     */
    public static final String TOPGROUP_DN = "cn=Top group,ou=crews,ou=groups,o=sevenSeas";

    /**
     * The LDAP members of group Top group.
     */
    public static final Set<String> TOPGROUP_MEMBERS = new HashSet<String>();

    /**
     * The LDAP members of group Top group when subgroups are not resolved.
     */
    public static final Set<String> TOPGROUP_MEMBERS_NORESOLVE = new HashSet<String>();

    static {
        TOPGROUP_MEMBERS.addAll(HMSLYDIA_MEMBERS);

        TOPGROUP_MEMBERS.add("cn=Horatio Nelson,ou=people,o=sevenSeas".toLowerCase());
        TOPGROUP_MEMBERS.add("cn=Thomas Masterman Hardy,ou=people,o=sevenSeas".toLowerCase());
        TOPGROUP_MEMBERS.add("cn=Cornelius Buckley,ou=people,o=sevenSeas".toLowerCase());

        TOPGROUP_MEMBERS.add("cn=William Bligh,ou=people,o=sevenSeas".toLowerCase());
        TOPGROUP_MEMBERS.add("cn=Fletcher Christian,ou=people,o=sevenSeas".toLowerCase());
        TOPGROUP_MEMBERS.add("cn=John Fryer,ou=people,o=sevenSeas".toLowerCase());
        TOPGROUP_MEMBERS.add("cn=John Hallett,ou=people,o=sevenSeas".toLowerCase());

        TOPGROUP_MEMBERS_NORESOLVE.add(HMSLYDIA_DN.toLowerCase());
        TOPGROUP_MEMBERS_NORESOLVE.add(HMSBOUNTY_DN.toLowerCase());
        TOPGROUP_MEMBERS_NORESOLVE.add("cn=Horatio Nelson,ou=people,o=sevenSeas".toLowerCase());
        TOPGROUP_MEMBERS_NORESOLVE.add("cn=Thomas Masterman Hardy,ou=people,o=sevenSeas".toLowerCase());
        TOPGROUP_MEMBERS_NORESOLVE.add("cn=Cornelius Buckley,ou=people,o=sevenSeas".toLowerCase());
    }

    // ///

    /**
     * Tool to start and stop embedded LDAP server.
     */
    private static LDAPRunner ldap = new LDAPRunner();

    /**
     * @return return the port of the current instance of LDAP server.
     */
    public static int getLDAPPort()
    {
        return Integer.parseInt(System.getProperty(SYSPROPNAME_LDAPPORT));
    }

    public static void before() throws Exception
    {
        ldap.start();
    }

    public static void after() throws Exception
    {
        ldap.stop();
    }
}
