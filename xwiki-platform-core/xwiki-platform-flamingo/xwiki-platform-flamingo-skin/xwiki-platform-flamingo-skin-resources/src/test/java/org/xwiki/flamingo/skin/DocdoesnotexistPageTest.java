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
package org.xwiki.flamingo.skin;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Named;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.localization.script.LocalizationScriptService;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.Right;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDeletedDocument;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiRecycleBinStoreInterface;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test the {@code docdoesnotexist.vm} template of the Flamingo skin, which displays the "Page not found" and the "Page
 * moved to recycle bin" pages.
 *
 * @version $Id$
 */
@HTML50ComponentList
@XWikiSyntax21ComponentList
class DocdoesnotexistPageTest extends PageTest
{
    private static final Path TEMPLATE = Path.of("src/main/resources/flamingo/docdoesnotexist.vm");

    private static final Path MENUS_MACROS = Path.of("src/main/resources/flamingo/menus_macros.vm");

    private static final String DELETED_INFO = "core.viewers.content.deleted.info";

    private static final String DELETER_LINK = "<a class=\"deleter\" href=\"/xwiki/bin/view/XWiki/Deleter\">Deleter</a>";

    private static final String OTHER_DELETER_LINK =
        "<a class=\"deleter\" href=\"/xwiki/bin/view/XWiki/OtherDeleter\">Other deleter</a>";

    private static final String HINT = ".docdoesnotexist-hint";

    private static final String CREATE_BUTTON = "#contentmenu #tmEdit > a.btn";

    // Required to check the rights on the deleted documents.
    @MockComponent
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    private XWikiRecycleBinStoreInterface recycleBinStore;

    private boolean hasEdit = true;

    private boolean isAdvancedUser = true;

    private boolean displayEditMenu = true;

    @BeforeEach
    void setUp() throws Exception
    {
        this.recycleBinStore = mock(XWikiRecycleBinStoreInterface.class);
        doReturn(this.recycleBinStore).when(this.xwiki).getRecycleBinStore();
        doReturn(new XWikiDeletedDocument[0]).when(this.xwiki).getDeletedDocuments(anyString(), anyString(), any());
        doReturn(DELETER_LINK).when(this.xwiki).getUserName(eq("XWiki.Deleter"), any(), any());
        doReturn(OTHER_DELETER_LINK).when(this.xwiki).getUserName(eq("XWiki.OtherDeleter"), any(), any());
        doReturn("formatted date").when(this.xwiki).formatDate(any(), any(), any());

        setCurrentDocument(new DocumentReference("xwiki", "Space", "Page"));
    }

    @Test
    void missingPage() throws Exception
    {
        Document result = render();

        assertEquals("core.viewers.content.doesnotexist.title", result.selectFirst("#document-title h1").text());
        assertEquals("core.viewers.content.doesnotexist.hint", result.selectFirst(HINT).text());
        Element createButton = result.selectFirst(CREATE_BUTTON);
        assertEquals("/xwiki/bin/create/Space/Page", createButton.attr("href"));
        assertEquals("core.viewers.content.doesnotexist.create", createButton.selectFirst(".btn-label").text());
        assertEquals(List.of("tmEditWiki", "tmEditInline", "tmEditRights", "tmEditObject", "tmEditClass"),
            result.select("#tmEdit .dropdown-menu a").eachAttr("id"));
        assertNull(result.selectFirst(".recyclebin-list"));
    }

    @Test
    void missingPageForSimpleUser() throws Exception
    {
        this.isAdvancedUser = false;

        Document result = render();

        assertNotNull(result.selectFirst(CREATE_BUTTON));
        assertNull(result.selectFirst("#tmEdit .dropdown-toggle"));
    }

    @Test
    void missingPageWithoutEditRight() throws Exception
    {
        this.hasEdit = false;

        Document result = render();

        assertEquals("core.viewers.content.doesnotexist.hintNoCreate", result.selectFirst(HINT).text());
        assertNull(result.selectFirst(".document-menu"));
    }

    @Test
    void missingPageWhenTheLayoutHidesTheEditMenu() throws Exception
    {
        this.displayEditMenu = false;

        Document result = render();

        assertEquals("core.viewers.content.doesnotexist.hint", result.selectFirst(HINT).text());
        assertNull(result.selectFirst(".document-menu"));
    }

    @Test
    void deletedPageThatCanBeRestored() throws Exception
    {
        XWikiDeletedDocument deletedDocument = deletedDocument("Space.Page", "XWiki.Deleter", new Date());
        allow(Right.EDIT, deletedDocument);
        setDeletedDocuments("Space.Page", deletedDocument);

        Document result = render();

        assertEquals("core.viewers.content.deleted.title", result.selectFirst("#document-title h1").text());
        assertEquals("core.viewers.content.deleted.hint [Page]", result.selectFirst(HINT).text());
        assertNotNull(result.selectFirst(".recyclebin-list.docs .action-restore"));
        assertNotNull(result.selectFirst(CREATE_BUTTON));
    }

    @Test
    void deletedPageThatCanBeRestoredWithoutEditRight() throws Exception
    {
        this.hasEdit = false;
        XWikiDeletedDocument deletedDocument = deletedDocument("Space.Page", "XWiki.Deleter", new Date());
        allow(Right.EDIT, deletedDocument);
        setDeletedDocuments("Space.Page", deletedDocument);

        Document result = render();

        assertEquals("core.viewers.content.deleted.hintNoCreate [Page]", result.selectFirst(HINT).text());
        assertNull(result.selectFirst(".document-menu"));
    }

    @Test
    void deletedPageSummary() throws Exception
    {
        setDeletedDocuments("Space.Page", deletedDocument("Space.Page", "XWiki.Deleter", new Date()));

        Document result = render();

        Element hint = result.selectFirst(HINT);
        assertEquals(DELETED_INFO + " [Page, Deleter, formatted date] core.viewers.content.deleted.infoCreate",
            hint.text());
        assertEquals("/xwiki/bin/view/XWiki/Deleter", hint.selectFirst("a.deleter").attr("href"));
        // Users who can't view, restore or delete the entries of the recycle bin don't get its table.
        assertNull(result.selectFirst(".recyclebin-list"));
    }

    @Test
    void deletedPageSummaryEscapesTheTranslation() throws Exception
    {
        LocalizationScriptService localization = this.componentManager.getInstance(ScriptService.class, "localization");
        when(localization.render(eq(DELETED_INFO), anyCollection())).thenAnswer(invocation -> {
            Object deleter = ((Collection<?>) invocation.getArgument(1)).toArray()[1];
            return "<strong>Deleted</strong> by " + deleter;
        });
        setDeletedDocuments("Space.Page", deletedDocument("Space.Page", "XWiki.Deleter", new Date()));

        Element hint = render().selectFirst(HINT);

        assertNull(hint.selectFirst("strong"));
        assertTrue(hint.text().startsWith("<strong>Deleted</strong> by Deleter"), hint.text());
        assertNotNull(hint.selectFirst("a.deleter"));
    }

    @Test
    void deletedTerminalPageThatCanBeRestored() throws Exception
    {
        setCurrentDocument(new DocumentReference("xwiki", "Space", "WebHome"));
        // The location home page was deleted by someone else, and only the deleted terminal page can be restored.
        XWikiDeletedDocument deletedHomePage = deletedDocument("Space.WebHome", "XWiki.OtherDeleter", new Date(1000));
        XWikiDeletedDocument deletedTerminalPage = deletedDocument("Space", "XWiki.Deleter", new Date(2000));
        allow(Right.EDIT, deletedTerminalPage);
        setDeletedDocuments("Space.WebHome", deletedHomePage);
        setDeletedDocuments("Space", deletedTerminalPage);

        Document result = render();

        assertEquals("core.viewers.content.deleted.hint [Space]", result.selectFirst(HINT).text());
        assertNotNull(result.selectFirst(".recyclebin-list.terminal-docs .action-restore"));
    }

    @Test
    void deletedTerminalPageSummaryNamesTheMostRecentDeletion() throws Exception
    {
        setCurrentDocument(new DocumentReference("xwiki", "Space", "WebHome"));
        setDeletedDocuments("Space.WebHome", deletedDocument("Space.WebHome", "XWiki.OtherDeleter", new Date(1000)));
        setDeletedDocuments("Space", deletedDocument("Space", "XWiki.Deleter", new Date(2000)));

        Element hint = render().selectFirst(HINT);

        assertEquals("/xwiki/bin/view/XWiki/Deleter", hint.selectFirst("a.deleter").attr("href"));
    }

    private void setCurrentDocument(DocumentReference reference) throws XWikiException
    {
        XWikiDocument document = this.xwiki.getDocument(reference, this.context);
        this.context.setDoc(document);
    }

    private XWikiDeletedDocument deletedDocument(String fullName, String deleter, Date date) throws XWikiException
    {
        return new XWikiDeletedDocument(fullName, Locale.ROOT, null, deleter, date, null);
    }

    private void allow(Right right, XWikiDeletedDocument deletedDocument)
    {
        when(this.recycleBinStore.hasAccess(eq(right), any(), same(deletedDocument))).thenReturn(true);
    }

    private void setDeletedDocuments(String fullName, XWikiDeletedDocument... deletedDocuments) throws XWikiException
    {
        doReturn(deletedDocuments).when(this.xwiki).getDeletedDocuments(eq(fullName), anyString(), any());
    }

    private Document render() throws Exception
    {
        // Only the macros of the template are evaluated, since the rest of it displays the whole page layout. The
        // variables that are normally set by the skin are set first.
        String template = Files.readString(TEMPLATE, StandardCharsets.UTF_8);
        String macros = template.substring(template.indexOf("#macro (showExtensions)"));
        String content = String.format("#set ($hasEdit = %s)#set ($isReadOnly = false)#set ($isAdvancedUser = %s)"
            + "#set ($displayContentMenu = true)#set ($displayEditMenu = %s)%n%s%n%s%n#_displayDocDoesNotExist()",
            this.hasEdit, this.isAdvancedUser, this.displayEditMenu,
            Files.readString(MENUS_MACROS, StandardCharsets.UTF_8), macros);

        TemplateManager templateManager = this.oldcore.getMocker().getInstance(TemplateManager.class);
        StringWriter writer = new StringWriter();
        templateManager.render(templateManager.createStringTemplate("docdoesnotexist.vm", content,
            (DocumentReference) null, (DocumentReference) null), writer);
        return Jsoup.parse(writer.toString());
    }
}
