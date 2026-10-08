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

import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.script.ExtensionManagerScriptService;
import org.xwiki.extension.xar.script.XarExtensionScriptService;
import org.xwiki.extension.xar.security.ProtectionLevel;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.TestNoScriptMacro;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Page test of the content of the delete user modal of {@code XWiki.AdminUsersSheet}.
 *
 * @version $Id$
 */
@HTML50ComponentList
@XWikiSyntax21ComponentList
@ComponentList({
    TestNoScriptMacro.class,
    ModelScriptService.class
})
class AdminUsersSheetPageTest extends PageTest
{
    private static final DocumentReference ADMIN_USERS_SHEET =
        new DocumentReference("xwiki", "XWiki", "AdminUsersSheet");

    private static final DocumentReference USER_REFERENCE = new DocumentReference("xwiki", "XWiki", "MyUser");

    private XarExtensionScriptService xarExtensionScriptService;

    @BeforeEach
    void setUp() throws Exception
    {
        ExtensionManagerScriptService extensionScriptService = mock(ExtensionManagerScriptService.class);
        this.componentManager.registerComponent(ScriptService.class, ExtensionManagerScriptService.ROLEHINT,
            extensionScriptService);
        this.xarExtensionScriptService = mock(XarExtensionScriptService.class);
        doReturn(this.xarExtensionScriptService).when(extensionScriptService).get("xar");

        this.request.put("data", "deleteUserModalContent");
        this.request.put("userReference", "XWiki.MyUser");
    }

    @Test
    void deleteUserModalContentWithoutProtection() throws Exception
    {
        when(this.xarExtensionScriptService.getDeleteSecurityLevel(any(), eq(USER_REFERENCE)))
            .thenReturn(ProtectionLevel.NONE);

        Document document = renderHTMLPage(ADMIN_USERS_SHEET);

        assertNotNull(document.selectFirst("p"));
        assertNull(document.selectFirst(".deleteWarningExtensions"));
    }

    @Test
    void deleteUserModalContentWithExtensionProtectionWarning() throws Exception
    {
        when(this.xarExtensionScriptService.getDeleteSecurityLevel(any(), eq(USER_REFERENCE)))
            .thenReturn(ProtectionLevel.WARNING);
        InstalledExtension extension = mockExtension("org.xwiki:users", "Users Extension");
        when(this.xarExtensionScriptService.getInstalledExtensions(USER_REFERENCE)).thenReturn(List.of(extension));

        Document document = renderHTMLPage(ADMIN_USERS_SHEET);

        Element warning = document.selectFirst(".box.warningmessage.deleteWarningExtensions");
        assertNotNull(warning);
        assertEquals("job.question.ExtensionBreakingQuestion.refactoring/delete.title",
            warning.selectFirst(".box-title").text());
        assertEquals(List.of("Users Extension"), warning.select(".deleteWarningExtensionsList li").eachText());
    }

    private InstalledExtension mockExtension(String id, String name)
    {
        InstalledExtension extension = mock(InstalledExtension.class);
        when(extension.getId()).thenReturn(new ExtensionId(id, "1.0"));
        when(extension.getName()).thenReturn(name);
        return extension;
    }
}
