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

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.csrf.script.CSRFTokenScriptService;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.rendering.internal.macro.message.ErrorMessageMacro;
import org.xwiki.rendering.internal.macro.message.InfoMessageMacro;
import org.xwiki.template.internal.macro.TemplateMacro;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax20ComponentList;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
@ComponentList({
    CSRFTokenScriptService.class,
    ErrorMessageMacro.class,
    InfoMessageMacro.class,
    ModelScriptService.class,
    TemplateMacro.class
})
class RegistrationPageTest extends PageTest
{
    private static final DocumentReference REGISTRATION =
        new DocumentReference("xwiki", "XWiki", "Registration");

    private static final String USER_NAME = "JaneDoe";

    @MockComponent
    private CSRFToken csrfToken;

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

        this.request.put("xwikiname", USER_NAME);
        this.request.put("register_first_name", "Jane");
        this.request.put("register_last_name", "Doe");
        this.request.put("register_password", "Passw0rd!Passw0rd!");
        this.request.put("register2_password", "Passw0rd!Passw0rd!");
        this.request.put("register_email", "jane.doe@example.org");
        this.request.put("form_token", "token");

        Document result = renderHTMLPage(REGISTRATION);

        Element successMessage = result.selectFirst(".box.infomessage");
        assertNotNull(successMessage);
        assertEquals("core.register.successful [" + displayedName + ", " + USER_NAME + "]", successMessage.text());
    }
}
