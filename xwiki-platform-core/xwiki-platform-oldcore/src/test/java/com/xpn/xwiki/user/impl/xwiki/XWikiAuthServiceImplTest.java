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

import java.net.URL;
import java.security.Principal;

import org.jmock.Mock;
import org.jmock.core.Invocation;
import org.jmock.core.stub.CustomStub;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.web.XWikiResponse;

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

        BaseClass userClass = new BaseClass();
        userClass.setDocumentReference(new DocumentReference(getContext().getWikiId(), "XWiki", "XWikiUsers"));
        userClass.addPasswordField("password", "Password", 10);

        this.mockXWiki.stubs().method("getUserClass").will(returnValue(userClass));
    }

    /**
     * Test that it's not possible to log in with a superadmin user when the superadmin password configuration is turned
     * off.
     */
    public void testAuthenticateWithSuperAdminWhenSuperAdminPasswordIsTurnedOff() throws Exception
    {
        this.mockXWiki.expects(once()).method("Param").with(eq("xwiki.superadminpassword")).will(returnValue(null));
        Principal principal =
            this.authService.authenticate(XWikiRightService.SUPERADMIN_USER, "whatever", getContext());
        assertNull(principal);
    }

    /**
     * Test that it's not possible to log in with a superadmin user when the superadmin password configuration is turned
     * off.
     */
    public void testAuthenticateWithSuperAdminPrefixedWithXWikiWhenSuperAdminPasswordIsTurnedOff() throws Exception
    {
        this.mockXWiki.stubs().method("Param").with(eq("xwiki.superadminpassword")).will(returnValue(null));
        Principal principal =
            this.authService.authenticate(XWikiRightService.SUPERADMIN_USER_FULLNAME, "whatever", getContext());
        assertNull(principal);
    }

    public void testAuthenticateWithSuperAdminWithWhiteSpacesWhenSuperAdminPasswordIsTurnedOff() throws Exception
    {
        this.mockXWiki.stubs().method("Param").with(eq("xwiki.superadminpassword")).will(returnValue(null));
        Principal principal =
            this.authService.authenticate(" " + XWikiRightService.SUPERADMIN_USER + " ", "whatever", getContext());
        assertNull(principal);
    }

    /**
     * Test that superadmin is authenticated as superadmin whatever the case.
     */
    public void testAuthenticateWithSuperAdminWithDifferentCase() throws Exception
    {
        this.mockXWiki.stubs().method("Param").with(eq("xwiki.superadminpassword")).will(returnValue("pass"));

        Principal principal =
            this.authService.authenticate(XWikiRightService.SUPERADMIN_USER.toUpperCase(), "pass", getContext());
        assertNotNull(principal);
        assertEquals(XWikiRightService.SUPERADMIN_USER_FULLNAME, principal.getName());
    }

    /** Test that SomeUser is correctly authenticated as XWiki.SomeUser when xwiki:SomeUser is entered as username. */
    public void testLoginWithWikiPrefix() throws Exception
    {
        // Setup a simple user profile document
        XWikiDocument userDoc = new XWikiDocument("XWiki", "SomeUser");
        // Mock the XWikiUsers object, since a real objects requires more mocking on the XWiki object
        Mock mockUserObj = mock(BaseObject.class, new Class[] {}, new Object[] {});
        mockUserObj.stubs().method("setDocumentReference");
        mockUserObj.stubs().method("setNumber");
        mockUserObj.stubs().method("getStringValue").with(eq("password")).will(returnValue("pass"));
        mockUserObj.stubs().method("setOwnerDocument");
        userDoc.addObject("XWiki.XWikiUsers", (BaseObject) mockUserObj.proxy());

        // Make a simple XWiki.XWikiUsers class that will contain a default password field
        BaseClass userClass = new BaseClass();
        userClass.addPasswordField("password", "Password", 20);
        userClass.setClassName("XWiki.XWikiUsers");

        // Prepare the XWiki mock
        this.mockXWiki.stubs().method("getDocument").with(eq("XWiki.SomeUser"), eq(this.getContext()))
            .will(returnValue(userDoc));
        this.mockXWiki.stubs().method("getClass").with(eq("XWiki.XWikiUsers"), eq(this.getContext()))
            .will(returnValue(userClass));
        this.mockXWiki.stubs().method("exists").will(returnValue(true));

        // Finally run the test: Using xwiki:Admin should correctly authenticate the Admin user
        Principal principal = this.authService.authenticate("xwiki:SomeUser", "pass", this.getContext());
        assertNotNull(principal);
        assertEquals("xwiki:XWiki.SomeUser", principal.getName());
    }

    /**
     * Test that user is authenticated with a global account when a local one with the same name exists and the username
     * contains a wiki prefix.
     */
    public void testLogintoVirtualXwikiWithWikiPrefixUsername() throws Exception
    {
        // Setup simple user profile documents
        XWikiDocument userDocLocal = new XWikiDocument(new DocumentReference("local", "XWiki", "Admin"));

        // Make a simple XWiki.XWikiUsers class that will contain a default password field
        BaseClass userClass = new BaseClass();
        userClass.addPasswordField("password", "Password", 20);
        userClass.setClassName("XWiki.XWikiUsers");

        // Mock the XWikiUsers object, since a real objects requires more mocking on the XWiki object
        Mock mockUserObj = mock(BaseObject.class, new Class[] {}, new Object[] {});
        mockUserObj.stubs().method("setDocumentReference");
        mockUserObj.stubs().method("setOwnerDocument");
        mockUserObj.stubs().method("setNumber");
        mockUserObj.stubs().method("getStringValue").with(eq("password")).will(returnValue("admin"));
        userDocLocal.addObject("XWiki.XWikiUsers", (BaseObject) mockUserObj.proxy());

        // Prepare the XWiki mock for local
        this.mockXWiki.stubs().method("getDocument").with(eq("XWiki.Admin"), ANYTHING).will(returnValue(userDocLocal));
        this.mockXWiki.stubs().method("getClass").with(eq("XWiki.XWikiUsers"), eq(this.getContext()))
            .will(returnValue(userClass));
        this.mockXWiki.stubs().method("exists").will(returnValue(true));

        // Run the test: Using Xwiki.Admin should correctly authenticate the Admin user
        Principal principalLocal = this.authService.authenticate("XWiki.Admin", "admin", this.getContext());
        assertNotNull(principalLocal);
        assertEquals("XWiki.Admin", principalLocal.getName());

        // Set the database name to local.
        this.getContext().setWikiId("local");

        // Finally run the test: Using xwiki:Xwiki.Admin should correctly authenticate the Admin user
        Principal principalVirtual = this.authService.authenticate("xwiki:XWiki.Admin", "admin", this.getContext());
        assertNotNull(principalVirtual);
        assertEquals("xwiki:XWiki.Admin", principalVirtual.getName());
    }

    public void testStripContextPathFromURLWithSlashAfter() throws Exception
    {
        this.mockXWiki.stubs().method("getWebAppPath").will(returnValue("xwiki/"));

        assertEquals("/something",
            this.authService.stripContextPathFromURL(new URL("http://localhost:8080/xwiki/something"), getContext()));
    }

    public void testStripContextPathFromURLWhenRootContextPathWithSlash() throws Exception
    {
        this.mockXWiki.stubs().method("getWebAppPath").will(returnValue("/"));

        assertEquals("/something",
            this.authService.stripContextPathFromURL(new URL("http://localhost:8080/something"), getContext()));
    }

    public void testStripContextPathFromURLWhenRootContextPathWithoutSlash() throws Exception
    {
        this.mockXWiki.stubs().method("getWebAppPath").will(returnValue(""));

        assertEquals("/something",
            this.authService.stripContextPathFromURL(new URL("http://localhost:8080/something"), getContext()));
    }

    /**
     * Simulates the use case when the {@code HttpServletResponse.encodeURL()} changes the context path.
     */
    public void testStripContextPathFromURLWhenOutBoundRewriteRuleChangingContextPath() throws Exception
    {
        this.mockXWiki.stubs().method("getWebAppPath").will(returnValue("xwiki/"));

        Mock xwikiResponse = mock(XWikiResponse.class);
        xwikiResponse.stubs().method("setLocale");
        xwikiResponse.stubs().method("encodeURL").will(
            new CustomStub("Implements XWikiResponse.encodeURL")
            {
                @Override
                public Object invoke(Invocation invocation) throws Throwable
                {
                    return "http://localhost:8080/anothercontext"
                        + ";jsessionid=0AF95AFB8997826B936C0397DF6A0C7F?language=en";
                }
            });
        getContext().setResponse((XWikiResponse) xwikiResponse.proxy());

        // Note: the passed URL to stripContextPathFromURL() has also gone through encodeURL() which is why its
        // context path has been changed from "xwiki" to "anothercontext".
        assertEquals("/something", this.authService.stripContextPathFromURL(
            new URL("http://localhost:8080/anothercontext/something"), getContext()));

    }

    public void testStripContextPathFromURLWithSlashBefore() throws Exception
    {
        this.mockXWiki.stubs().method("getWebAppPath").will(returnValue("/xwiki"));

        assertEquals("/something",
            this.authService.stripContextPathFromURL(new URL("http://localhost:8080/xwiki/something"), getContext()));
    }

    public void testStripContextPathFromURLWhenRootWebAppAndJSessionId() throws Exception
    {
        this.mockXWiki.stubs().method("getWebAppPath").will(returnValue(""));

        // Simulate a rewrite filter that would add a jsession id and add a leading slash!
        Mock xwikiResponse = mock(XWikiResponse.class);
        xwikiResponse.stubs().method("encodeURL").with(eq("http://localhost:8080")).will(
            new CustomStub("Implements XWikiResponse.encodeURL")
            {
                @Override
                public Object invoke(Invocation invocation) throws Throwable
                {
                    return "http://localhost:8080/;jsessionid=0AF95AFB8997826B936C0397DF6A0C7F";
                }
            });
        getContext().setResponse((XWikiResponse) xwikiResponse.proxy());

        assertEquals("/something",
            this.authService.stripContextPathFromURL(new URL("http://localhost:8080/something"), getContext()));
    }
}
