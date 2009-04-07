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
package com.xpn.xwiki.user.impl.xwiki;

import java.security.Principal;

import org.jmock.Mock;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;

/**
 * Unit tests for {@link com.xpn.xwiki.user.impl.xwiki.XWikiAuthServiceImpl}.
 * 
 * @version $Id$
 */
public class XWikiAuthServiceImplTest extends AbstractBridgedXWikiComponentTestCase
{
    private XWikiAuthServiceImpl authService;

    private Mock mockXWiki;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        this.authService = new XWikiAuthServiceImpl();

        this.mockXWiki = mock(XWiki.class);
        getContext().setWiki((XWiki) this.mockXWiki.proxy());
    }

    /**
     * Test that it's not possible to log in with a "superadmin" user when the superadmin password configuration is
     * turned off.
     */
    public void testAuthenticateWithSuperAdminWhenSuperAdminPasswordIsTurnedOff() throws Exception
    {
        this.mockXWiki.expects(once()).method("Param").with(eq("xwiki.superadminpassword")).will(returnValue(null));
        Principal principal = this.authService.authenticate("superadmin", "whatever", getContext());
        assertNull(principal);
    }

    /**
     * Test that it's not possible to log in with a "superadmin" user when the superadmin password configuration is
     * turned off.
     */
    public void testAuthenticateWithSuperAdminPrefixedWithXWikiWhenSuperAdminPasswordIsTurnedOff() throws Exception
    {
        this.mockXWiki.stubs().method("Param").with(eq("xwiki.superadminpassword")).will(returnValue(null));
        Principal principal = this.authService.authenticate("XWiki.superadmin", "whatever", getContext());
        assertNull(principal);
    }

    public void testAuthenticateWithSuperAdminWithWhiteSpacesWhenSuperAdminPasswordIsTurnedOff() throws Exception
    {
        this.mockXWiki.stubs().method("Param").with(eq("xwiki.superadminpassword")).will(returnValue(null));
        Principal principal = this.authService.authenticate(" superadmin ", "whatever", getContext());
        assertNull(principal);
    }

    /**
     * Test that superadmin is authenticated as superadmin whatever the case.
     */
    public void testAuthenticateWithSuperAdminWithDifferentCase() throws Exception
    {
        this.mockXWiki.stubs().method("Param").with(eq("xwiki.superadminpassword")).will(returnValue("pass"));
        this.mockXWiki.stubs().method("isVirtualMode").will(returnValue(false));

        Principal principal = this.authService.authenticate("SuperaDmin ", "pass", getContext());
        assertNotNull(principal);
        assertEquals("XWiki.superadmin", principal.getName());
    }
}
