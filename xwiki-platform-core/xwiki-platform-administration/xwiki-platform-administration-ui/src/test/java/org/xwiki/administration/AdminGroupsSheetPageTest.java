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
import org.xwiki.localization.script.LocalizationScriptService;
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
 * Page test of the content of the delete group modal of {@code XWiki.AdminGroupsSheet}.
 *
 * @version $Id$
 */
@HTML50ComponentList
@XWikiSyntax21ComponentList
@ComponentList({
    TestNoScriptMacro.class,
    ModelScriptService.class
})
class AdminGroupsSheetPageTest extends PageTest
{
    private static final DocumentReference ADMIN_GROUPS_SHEET =
        new DocumentReference("xwiki", "XWiki", "AdminGroupsSheet");

    private static final DocumentReference GROUP_REFERENCE = new DocumentReference("xwiki", "XWiki", "MyGroup");

    private XarExtensionScriptService xarExtensionScriptService;

    @BeforeEach
    void setUp() throws Exception
    {
        ExtensionManagerScriptService extensionScriptService = mock(ExtensionManagerScriptService.class);
        this.componentManager.registerComponent(ScriptService.class, ExtensionManagerScriptService.ROLEHINT,
            extensionScriptService);
        this.xarExtensionScriptService = mock(XarExtensionScriptService.class);
        doReturn(this.xarExtensionScriptService).when(extensionScriptService).get("xar");

        // Use a confirmation message with the group name placeholder, so that we can check how the name is displayed.
        LocalizationScriptService localizationScriptService =
            this.componentManager.getInstance(ScriptService.class, "localization");
        when(localizationScriptService.render("rightsmanager.confirmdeletegroup")).thenReturn("Delete __name__?");

        this.request.put("data", "deleteGroupModalContent");
    }

    @Test
    void deleteGroupModalContentWithoutProtection() throws Exception
    {
        when(this.xarExtensionScriptService.getDeleteSecurityLevel(any(), eq(GROUP_REFERENCE)))
            .thenReturn(ProtectionLevel.NONE);
        this.request.put("groupReference", "XWiki.MyGroup");

        Document document = renderHTMLPage(ADMIN_GROUPS_SHEET);

        assertEquals("Delete XWiki.MyGroup?", document.selectFirst("p").text());
        assertNull(document.selectFirst(".deleteWarningExtensions"));
    }

    @Test
    void deleteGroupModalContentWithExtensionProtectionWarning() throws Exception
    {
        when(this.xarExtensionScriptService.getDeleteSecurityLevel(any(), eq(GROUP_REFERENCE)))
            .thenReturn(ProtectionLevel.WARNING);
        InstalledExtension namedExtension = mockExtension("org.xwiki:named", "Named Extension");
        InstalledExtension unnamedExtension = mockExtension("org.xwiki:unnamed", null);
        when(this.xarExtensionScriptService.getInstalledExtensions(GROUP_REFERENCE))
            .thenReturn(List.of(unnamedExtension, namedExtension));
        this.request.put("groupReference", "XWiki.MyGroup");

        Document document = renderHTMLPage(ADMIN_GROUPS_SHEET);

        Element warning = document.selectFirst(".box.warningmessage.deleteWarningExtensions");
        assertNotNull(warning);
        assertEquals("job.question.ExtensionBreakingQuestion.refactoring/delete.title",
            warning.selectFirst(".box-title").text());
        // The extensions are sorted by name, and identified by their id when they don't have a name.
        assertEquals(List.of("org.xwiki:unnamed", "Named Extension"),
            warning.select(".deleteWarningExtensionsList li").eachText());
        assertEquals("/xwiki/bin/admin/XWiki/XWikiPreferences?section=XWiki.Extensions&search=&repo=installed",
            warning.selectFirst("a").attr("href"));
    }

    @Test
    void deleteGroupModalContentEscaping() throws Exception
    {
        String groupReference = "XWiki.<strong>My</strong>Group{{/html}}{{noscript/}}";
        this.request.put("groupReference", groupReference);

        Document document = renderHTMLPage(ADMIN_GROUPS_SHEET);

        assertEquals(groupReference, document.selectFirst(".groupName").text());
        assertNull(document.selectFirst("strong"));
    }

    private InstalledExtension mockExtension(String id, String name)
    {
        InstalledExtension extension = mock(InstalledExtension.class);
        when(extension.getId()).thenReturn(new ExtensionId(id, "1.0"));
        when(extension.getName()).thenReturn(name);
        return extension;
    }
}
