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
package org.xwiki.search.ui;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.internal.bridge.DefaultWikiObjectComponentManagerEventListener;
import org.xwiki.context.internal.concurrent.DefaultContextStoreManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.rendering.RenderingScriptServiceComponentList;
import org.xwiki.rendering.internal.configuration.DefaultRenderingConfigurationComponentList;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.TestNoScriptMacro;
import org.xwiki.test.page.XWikiSyntax21ComponentList;
import org.xwiki.uiextension.internal.UIExtensionClassDocumentInitializer;
import org.xwiki.uiextension.internal.WikiUIExtensionConstants;
import org.xwiki.uiextension.script.UIExtensionScriptServiceComponentList;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Page test for {@code XWiki.SearchAdmin}.
 *
 * @version $Id$
 */
@ComponentList({ UIExtensionClassDocumentInitializer.class, DefaultContextStoreManager.class, TestNoScriptMacro.class })
@UIExtensionScriptServiceComponentList
@RenderingScriptServiceComponentList
@DefaultRenderingConfigurationComponentList
@HTML50ComponentList
@XWikiSyntax21ComponentList
class SearchAdminPageTest extends PageTest
{
    private static final String WIKI_NAME = "xwiki";

    private static final String XWIKI_SPACE = "XWiki";

    private static final DocumentReference SEARCH_ADMIN_SHEET =
        new DocumentReference(WIKI_NAME, XWIKI_SPACE, "SearchAdmin");

    private static final DocumentReference ADMIN_REFERENCE = new DocumentReference(WIKI_NAME, XWIKI_SPACE, "Admin");

    @BeforeEach
    void setUp() throws Exception
    {
        // Initialize the UIExtension class.
        this.xwiki.initializeMandatoryDocuments(this.context);

        when(this.oldcore.getMockAuthorizationManager().hasAccess(any(), eq(ADMIN_REFERENCE), any())).thenReturn(true);

        // Register the component manager as wiki component manager so that the UIX can be registered on the wiki.
        this.componentManager.registerComponent(ComponentManager.class, "wiki", this.componentManager);
    }

    @Test
    void escapesExtensionProperties() throws Exception
    {
        // Create a document with a UI extension to test on.
        DocumentReference docRef = new DocumentReference(WIKI_NAME, XWIKI_SPACE, "Test");
        XWikiDocument document = new XWikiDocument(docRef);
        // Set the author to admin so the UIX can be registered at wiki level.
        document.setAuthorReference(ADMIN_REFERENCE);
        BaseObject uiExtensionObject =
            document.newXObject(WikiUIExtensionConstants.UI_EXTENSION_CLASS, this.context);
        uiExtensionObject.setStringValue(WikiUIExtensionConstants.EXTENSION_POINT_ID_PROPERTY,
            "org.xwiki.platform.search");
        String id = "\")}}{{noscript /}}";
        uiExtensionObject.setStringValue(WikiUIExtensionConstants.ID_PROPERTY, id);
        String label = "Label {{noscript /}}";
        String adminPageName = "\"}}{{noscript /}}Admin";
        String adminPage = XWIKI_SPACE + "." + adminPageName;
        uiExtensionObject.setLargeStringValue(WikiUIExtensionConstants.PARAMETERS_PROPERTY, "label=" + label + "\n"
            + "admin=" + adminPage);
        uiExtensionObject.setStringValue(WikiUIExtensionConstants.SCOPE_PROPERTY, "wiki");
        this.xwiki.saveDocument(document, this.context);

        // The event listeners are not registered by default. We trigger it manually so that the UIX is registered and
        // can be found and rendered.
        this.componentManager
            .<EventListener>getInstance(EventListener.class,
                DefaultWikiObjectComponentManagerEventListener.EVENT_LISTENER_NAME)
            .onEvent(new DocumentCreatedEvent(), document, null);

        // Create the fake search admin page.
        DocumentReference searchAdminReference = new DocumentReference(WIKI_NAME, XWIKI_SPACE, adminPageName);
        XWikiDocument searchAdminDocument = new XWikiDocument(searchAdminReference);
        String searchAdminPageContent = "Search Admin Page Content";
        searchAdminDocument.setContent(searchAdminPageContent);
        this.xwiki.saveDocument(searchAdminDocument, this.context);

        // Load XWiki.SearchCode and XWiki.SearchConfigClass as the SearchAdmin page uses them.
        loadPage(new DocumentReference(WIKI_NAME, XWIKI_SPACE, "SearchCode"));
        loadPage(new DocumentReference(WIKI_NAME, XWIKI_SPACE, "SearchConfigClass"));

        Document htmlPage = renderHTMLPage(SEARCH_ADMIN_SHEET);

        Element optionElement = htmlPage.selectFirst("select option");
        assertNotNull(optionElement);
        assertEquals(id, optionElement.attr("value"));
        assertEquals(label, optionElement.text());

        Element tabLink = htmlPage.selectFirst("ul.nav-tabs li a");
        assertNotNull(tabLink);
        String tabId = String.format("%sConfig", id);
        assertEquals("#" + tabId, tabLink.attr("href"));
        assertEquals(tabId, tabLink.attr("aria-controls"));
        assertEquals(label, tabLink.text());

        Element tabContent = htmlPage.selectFirst("div.tab-content div.tab-pane");
        assertNotNull(tabContent);
        assertEquals(tabId, tabContent.attr("id"));
        assertEquals(searchAdminPageContent, tabContent.text());
    }
}
