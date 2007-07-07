/*
 * Copyright 2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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

import org.jmock.cglib.MockObjectTestCase;
import org.jmock.Mock;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;

import java.security.Principal;

/**
 * Unit tests for {@link com.xpn.xwiki.user.impl.xwiki.XWikiAuthServiceImpl}.
 *
 * @version $Id: $
 */
public class XWikiAuthServiceImplTest extends MockObjectTestCase
{
    private XWikiAuthServiceImpl authService;
    private XWikiContext context;
    private Mock mockXWiki;

    protected void setUp()
    {
        this.authService = new XWikiAuthServiceImpl();
        this.context = new XWikiContext();

        this.mockXWiki = mock(XWiki.class, new Class[] {XWikiConfig.class, XWikiContext.class},
            new Object[] {new XWikiConfig(), context});
        this.context.setWiki((XWiki) this.mockXWiki.proxy());
    }

    /**
     * Test that it's not possible to log in with a "superadmin" user when the superadmin password
     * configuration is turned off.
     */
    public void testAuthenticateWithSuperAdminWhenSuperAdminPasswordIsTurnedOff()
        throws Exception
    {
        this.mockXWiki.expects(once()).method("Param").with(eq("xwiki.superadminpassword")).will(
            returnValue(null));
        Principal principal = this.authService.authenticate("superadmin", "whatever", this.context);
        assertNull(principal);
    }

    /**
     * Test that it's not possible to log in with a "superadmin" user when the superadmin password
     * configuration is turned off.
     */
    public void testAuthenticateWithSuperAdminPrefixedWithXWikiWhenSuperAdminPasswordIsTurnedOff()
        throws Exception
    {
        this.mockXWiki.stubs().method("Param").with(eq("xwiki.superadminpassword")).will(
            returnValue(null));
        Principal principal = this.authService.authenticate("XWiki.superadmin", "whatever",
            this.context);
        assertNull(principal);
    }
}
