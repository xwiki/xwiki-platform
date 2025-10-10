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

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.securityfilter.authenticator.persistent.PersistentLoginManagerInterface;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.web.XWikiResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link com.xpn.xwiki.user.impl.xwiki.XWikiAuthServiceImpl}.
 * 
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
class XWikiAuthServiceImplTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    private XWikiAuthServiceImpl authService;

    @BeforeEach
    void before() throws Exception
    {
        this.oldcore.getMocker().registerMockComponent(
            new DefaultParameterizedType(null, Provider.class, PersistentLoginManagerInterface.class));

        this.authService = new XWikiAuthServiceImpl();

        // Dummy response
        XWikiResponse xwikiResponse = mock(XWikiResponse.class);
        when(xwikiResponse.encodeURL(any())).then(
            (Answer<String>) invocation -> invocation.getArgument(0));
        this.oldcore.getXWikiContext().setResponse(xwikiResponse);
    }

    /**
     * Test that it's not possible to log in with a superadmin user when the superadmin password configuration is turned
     * off.
     */
    @Test
    void authenticateWithSuperAdminWhenSuperAdminPasswordIsTurnedOff() throws Exception
    {
        Principal principal = this.authService.authenticate(XWikiRightService.SUPERADMIN_USER, "whatever",
            this.oldcore.getXWikiContext());

        assertNull(principal);
    }

    /**
     * Test that it's not possible to log in with a superadmin user when the superadmin password configuration is turned
     * off.
     */
    @Test
    void authenticateWithSuperAdminPrefixedWithXWikiWhenSuperAdminPasswordIsTurnedOff() throws Exception
    {
        Principal principal = this.authService.authenticate(XWikiRightService.SUPERADMIN_USER_FULLNAME, "whatever",
            this.oldcore.getXWikiContext());

        assertNull(principal);
    }

    @Test
    void authenticateWithSuperAdminWithWhiteSpacesWhenSuperAdminPasswordIsTurnedOff() throws Exception
    {
        Principal principal = this.authService.authenticate(" " + XWikiRightService.SUPERADMIN_USER + " ", "whatever",
            this.oldcore.getXWikiContext());

        assertNull(principal);
    }

    /**
     * Test that superadmin is authenticated as superadmin whatever the case.
     */
    @Test
    void authenticateWithSuperAdminWithDifferentCase() throws Exception
    {
        this.oldcore.getMockXWikiCfg().setProperty("xwiki.superadminpassword", "pass");

        Principal principal = this.authService.authenticate(XWikiRightService.SUPERADMIN_USER.toUpperCase(), "pass",
            this.oldcore.getXWikiContext());

        assertNotNull(principal);
        assertEquals(XWikiRightService.SUPERADMIN_USER_FULLNAME, principal.getName());
    }

    /**
     * Test the superadmin password can be hashed with bcrypt.
     */
    @Test
    void authenticateWithSuperAdminWithBcryptPassword() throws Exception
    {
        // The password is "pass"
        this.oldcore.getMockXWikiCfg().setProperty("xwiki.superadminpassword",
            "$2y$08$2Mel30blRQ7E.XievLW00.AltivcBuU1HEl2mPG2qRGrd7FmWIwB6");

        Principal principal = this.authService.authenticate(XWikiRightService.SUPERADMIN_USER, "pass",
            this.oldcore.getXWikiContext());

        assertNotNull(principal);
        assertEquals(XWikiRightService.SUPERADMIN_USER_FULLNAME, principal.getName());
    }

    /**
     * Test that SomeUser is correctly authenticated as XWiki.SomeUser when xwiki:SomeUser is entered as username.
     */
    @Test
    void loginWithWikiPrefix() throws Exception
    {
        // Setup a simple user profile document
        XWikiDocument userDoc =
            new XWikiDocument(new DocumentReference(this.oldcore.getXWikiContext().getWikiId(), "XWiki", "SomeUser"));
        BaseObject mockUserObj =
            userDoc.newXObject(new LocalDocumentReference("XWiki", "XWikiUsers"), this.oldcore.getXWikiContext());
        mockUserObj.setStringValue("password", "pass");

        // Save the user
        this.oldcore.getSpyXWiki().saveDocument(userDoc, this.oldcore.getXWikiContext());

        // Finally run the test: Using xwiki:Admin should correctly authenticate the Admin user
        Principal principal = this.authService.authenticate("xwiki:SomeUser", "pass", this.oldcore.getXWikiContext());
        assertNotNull(principal);
        assertEquals("xwiki:XWiki.SomeUser", principal.getName());
    }

    /**
     * Test that user is authenticated with a global account when a local one with the same name exists and the username
     * contains a wiki prefix.
     */
    @Test
    void logintoVirtualXwikiWithWikiPrefixUsername() throws Exception
    {
        // Setup simple user profile documents
        XWikiDocument userDocLocal =
            new XWikiDocument(new DocumentReference(this.oldcore.getXWikiContext().getMainXWiki(), "XWiki", "Admin"));
        BaseObject mockUserObj =
            userDocLocal.newXObject(new LocalDocumentReference("XWiki", "XWikiUsers"), this.oldcore.getXWikiContext());
        mockUserObj.setStringValue("password", "admin");

        // Save the user
        this.oldcore.getSpyXWiki().saveDocument(userDocLocal, this.oldcore.getXWikiContext());

        // Run the test: Using XWiki.Admin should correctly authenticate the Admin user
        Principal principalLocal =
            this.authService.authenticate("XWiki.Admin", "admin", this.oldcore.getXWikiContext());
        assertNotNull(principalLocal);
        assertEquals("XWiki.Admin", principalLocal.getName());

        // Set the database name to local.
        this.oldcore.getXWikiContext().setWikiId("local");

        // Finally run the test: Using xwiki:Xwiki.Admin should correctly authenticate the Admin user
        Principal principalVirtual =
            this.authService.authenticate("xwiki:XWiki.Admin", "admin", this.oldcore.getXWikiContext());
        assertNotNull(principalVirtual);
        assertEquals("xwiki:XWiki.Admin", principalVirtual.getName());
    }

    @Test
    void stripContextPathFromURLWithSlashAfter() throws Exception
    {
        doReturn("xwiki/").when(this.oldcore.getSpyXWiki()).getWebAppPath(any(XWikiContext.class));

        assertEquals("/something", this.authService
            .stripContextPathFromURL(new URL("http://localhost:8080/xwiki/something"), this.oldcore.getXWikiContext()));
    }

    @Test
    void stripContextPathFromURLWhenRootContextPathWithSlash() throws Exception
    {
        doReturn("/").when(this.oldcore.getSpyXWiki()).getWebAppPath(any(XWikiContext.class));

        assertEquals("/something", this.authService.stripContextPathFromURL(new URL("http://localhost:8080/something"),
            this.oldcore.getXWikiContext()));
    }

    @Test
    void stripContextPathFromURLWhenRootContextPathWithoutSlash() throws Exception
    {
        doReturn("").when(this.oldcore.getSpyXWiki()).getWebAppPath(any(XWikiContext.class));

        assertEquals("/something", this.authService.stripContextPathFromURL(new URL("http://localhost:8080/something"),
            this.oldcore.getXWikiContext()));
    }

    /**
     * Simulates the use case when the {@code HttpServletResponse.encodeURL()} changes the context path.
     */
    @Test
    void stripContextPathFromURLWhenOutBoundRewriteRuleChangingContextPath() throws Exception
    {
        doReturn("xwiki/").when(this.oldcore.getSpyXWiki()).getWebAppPath(any(XWikiContext.class));

        XWikiResponse xwikiResponse = mock(XWikiResponse.class);
        when(xwikiResponse.encodeURL(any()))
            .thenReturn("http://localhost:8080/anothercontext;jsessionid=0AF95AFB8997826B936C0397DF6A0C7F?language=en");
        this.oldcore.getXWikiContext().setResponse(xwikiResponse);

        // Note: the passed URL to stripContextPathFromURL() has also gone through encodeURL() which is why its
        // context path has been changed from "xwiki" to "anothercontext".
        assertEquals("/something", this.authService.stripContextPathFromURL(
            new URL("http://localhost:8080/anothercontext/something"), this.oldcore.getXWikiContext()));
    }

    @Test
    void stripContextPathFromURLWithSlashBefore() throws Exception
    {
        doReturn("xwiki/").when(this.oldcore.getSpyXWiki()).getWebAppPath(any(XWikiContext.class));

        assertEquals("/something", this.authService
            .stripContextPathFromURL(new URL("http://localhost:8080/xwiki/something"), this.oldcore.getXWikiContext()));
    }

    @Test
    void stripContextPathFromURLWhenRootWebAppAndJSessionId() throws Exception
    {
        doReturn("").when(this.oldcore.getSpyXWiki()).getWebAppPath(any(XWikiContext.class));

        // Simulate a rewrite filter that would add a jsession id and add a leading slash!
        XWikiResponse xwikiResponse = mock(XWikiResponse.class);
        when(xwikiResponse.encodeURL("http://localhost:8080"))
            .thenReturn("http://localhost:8080/;jsessionid=0AF95AFB8997826B936C0397DF6A0C7F");
        this.oldcore.getXWikiContext().setResponse(xwikiResponse);

        assertEquals("/something", this.authService.stripContextPathFromURL(new URL("http://localhost:8080/something"),
            this.oldcore.getXWikiContext()));
    }
}
