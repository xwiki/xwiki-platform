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
package org.xwiki.administration;

import java.text.MessageFormat;
import java.util.Collection;

import javax.script.ScriptContext;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.csrf.script.CSRFTokenScriptService;
import org.xwiki.localization.script.LocalizationScriptService;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.rendering.internal.macro.message.ErrorMessageMacro;
import org.xwiki.rendering.internal.macro.message.InfoMessageMacro;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authentication.RegistrationConfiguration;
import org.xwiki.security.authentication.script.RegistrationScriptService;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.script.SecurityScriptServiceComponentList;
import org.xwiki.template.internal.macro.TemplateMacro;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax20ComponentList;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * Page test of {@code XWiki.Registration}.
 *
 * @version $Id$
 */
@HTML50ComponentList
@XWikiSyntax20ComponentList
@XWikiSyntax21ComponentList
@SecurityScriptServiceComponentList
@ComponentList({
    CSRFTokenScriptService.class,
    ErrorMessageMacro.class,
    InfoMessageMacro.class,
    ModelScriptService.class,
    RegistrationScriptService.class,
    TemplateMacro.class
})
class RegistrationPageTest extends PageTest
{
    private static final DocumentReference REGISTRATION =
        new DocumentReference("xwiki", "XWiki", "Registration");

    private static final DocumentReference REGISTRATION_CONFIG =
        new DocumentReference("xwiki", "XWiki", "RegistrationConfig");

    private static final String USER_NAME = "JaneDoe";

    @MockComponent
    private CSRFToken csrfToken;

    @MockComponent
    private RegistrationConfiguration registrationConfiguration;

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.csrfToken.isTokenValid(any())).thenReturn(true);
        when(this.oldcore.getMockRightService().hasAccessLevel(eq("register"), any(), any(), any())).thenReturn(true);
        doReturn(1).when(this.oldcore.getSpyXWiki()).createUser(anyBoolean(), anyString(), any(XWikiContext.class));
    }

    @Test
    void defaultSuccessMessageDisplaysTheUserName() throws Exception
    {
        // No XWiki.RegistrationConfig page is loaded, so the default success message is used. The displayed name of
        // the user contains text that looks like Velocity code, which must be displayed as is.
        String displayedName = "$registrationDone #if(true)Jane#end";
        doReturn(displayedName).when(this.oldcore.getSpyXWiki())
            .getUserName(eq("XWiki." + USER_NAME), isNull(), anyBoolean(), any(XWikiContext.class));

        putRegistrationRequest("Jane", "Doe");

        Document result = renderHTMLPage(REGISTRATION);

        Element successMessage = result.selectFirst(".box.infomessage");
        assertNotNull(successMessage);
        assertEquals("core.register.successful [" + displayedName + ", " + USER_NAME + "]", successMessage.text());
    }

    @Test
    void successHeadlineDisplaysTheUserNameNextToTheReadableName() throws Exception
    {
        loadPage(REGISTRATION_CONFIG);
        mockWelcomeHeadlineTranslation();
        putRegistrationRequest("Jane", "<em>Doe</em>");

        Document result = renderHTMLPage(REGISTRATION);

        Element headline = result.selectFirst(".registration-success-headline h2");
        assertNotNull(headline);
        // No space is expected between the user name and the exclamation mark, as the English translation has none.
        assertEquals("Welcome, Jane <em>Doe</em> (" + USER_NAME + ")!", headline.text());
        // The readable name is escaped, so the only element of the headline is the one holding the user name.
        assertEquals(1, headline.children().size());
        assertEquals("small", headline.child(0).tagName());
        assertEquals("(" + USER_NAME + ")", headline.child(0).text());
    }

    @Test
    void successHeadlineDisplaysOnlyTheUserNameWithoutReadableName() throws Exception
    {
        loadPage(REGISTRATION_CONFIG);
        mockWelcomeHeadlineTranslation();
        putRegistrationRequest("Jane", "");

        Document result = renderHTMLPage(REGISTRATION);

        Element headline = result.selectFirst(".registration-success-headline h2");
        assertNotNull(headline);
        assertEquals("Welcome, " + USER_NAME + "!", headline.text());
        assertTrue(headline.children().isEmpty());
    }

    private void putRegistrationRequest(String firstName, String lastName)
    {
        this.request.put("xwikiname", USER_NAME);
        this.request.put("register_first_name", firstName);
        this.request.put("register_last_name", lastName);
        this.request.put("register_password", "Passw0rd!Passw0rd!");
        this.request.put("register2_password", "Passw0rd!Passw0rd!");
        this.request.put("register_email", "jane.doe@example.org");
        this.request.put("form_token", "token");
    }

    private void mockWelcomeHeadlineTranslation() throws Exception
    {
        // Use the actual English value, since the test checks the spaces and punctuation around the parameters.
        LocalizationScriptService localization = this.componentManager.getInstance(ScriptService.class,
            "localization");
        when(localization.render(eq("core.register.successful.welcome.headline"), anyCollection()))
            .thenAnswer(invocation -> MessageFormat.format("Welcome, {0}{1}!",
                invocation.<Collection<?>>getArgument(1).toArray()));
    }

    @Test
    void stopsWhenRegistrationIsNotAllowed() throws Exception
    {
        when(this.oldcore.getMockRightService().hasAccessLevel(eq("register"), any(), any(), any())).thenReturn(false);
        this.request.put("xwikiname", USER_NAME);

        Document result = renderHTMLPage(REGISTRATION);

        assertNull(result.getElementById("register"));
        assertNull(result.selectFirst(".box.errormessage"));
    }


    @Test
    void dropsPermissionsWhenThePageIsNotViewable() throws Exception
    {
        when(this.oldcore.getMockRightService().hasProgrammingRights(any())).thenReturn(true);
        when(this.oldcore.getMockContextualAuthorizationManager().hasAccess(same(Right.VIEW), eq(REGISTRATION)))
            .thenReturn(false);

        renderHTMLPage(REGISTRATION);

        assertTrue(this.context.hasDroppedPermissions());
    }


    @Test
    void dropsPermissionsWhenDisplayedThroughAnotherDocument() throws Exception
    {
        when(this.oldcore.getMockRightService().hasProgrammingRights(any())).thenReturn(true);
        XWikiDocument registration = loadPage(REGISTRATION);
        // Displaying the page as the sheet of another document: the context document is not the page itself.
        this.context.setDoc(new XWikiDocument(new DocumentReference("xwiki", "Space", "OtherPage")));

        registration.getRenderedContent(this.context);

        assertTrue(this.context.hasDroppedPermissions());
    }


    @Test
    void keepsPermissionsWhenInvitedRegistrationIsAllowed() throws Exception
    {
        when(this.oldcore.getMockRightService().hasProgrammingRights(any())).thenReturn(true);
        when(this.oldcore.getMockRightService().hasAccessLevel(eq("register"), any(), any(), any())).thenReturn(false);
        when(this.oldcore.getMockContextualAuthorizationManager().hasAccess(Right.PROGRAM)).thenReturn(true);
        XWikiDocument registration = loadPage(REGISTRATION);
        // The Invitation application allows the registration before including the page.
        this.context.setDoc(new XWikiDocument(new DocumentReference("xwiki", "Invitation", "InvitationGuestActions")));
        assertTrue(getRegistrationScriptService().allowInvitedRegistration());

        String result = registration.getRenderedContent(this.context);

        assertFalse(this.context.hasDroppedPermissions());
        // The registration form is displayed although the register right is denied.
        assertTrue(result.contains("id=\"register\""));
        // The permission applies to a single display of the page.
        assertFalse(getRegistrationScriptService().claimInvitedRegistration());
    }

    @Test
    void ignoresInvitedVariable() throws Exception
    {
        when(this.oldcore.getMockRightService().hasProgrammingRights(any())).thenReturn(true);
        when(this.oldcore.getMockRightService().hasAccessLevel(eq("register"), any(), any(), any())).thenReturn(false);
        XWikiDocument registration = loadPage(REGISTRATION);
        // Any script including the page can set Velocity variables, so they cannot allow the registration.
        this.context.setDoc(new XWikiDocument(new DocumentReference("xwiki", "Space", "IncludingPage")));
        this.componentManager.<ScriptContextManager>getInstance(ScriptContextManager.class).getCurrentScriptContext()
            .setAttribute("invited", true, ScriptContext.ENGINE_SCOPE);

        String result = registration.getRenderedContent(this.context);

        assertTrue(this.context.hasDroppedPermissions());
        assertFalse(result.contains("id=\"register\""));
    }

    private RegistrationScriptService getRegistrationScriptService() throws Exception
    {
        return this.componentManager.getInstance(ScriptService.class, "security.registration");
    }
}
