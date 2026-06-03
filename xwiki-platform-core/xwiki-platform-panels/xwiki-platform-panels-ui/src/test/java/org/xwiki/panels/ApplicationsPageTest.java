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
package org.xwiki.panels;

import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.internal.bridge.DefaultWikiObjectComponentManagerEventListener;
import org.xwiki.context.internal.concurrent.DefaultContextStoreManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.panels.internal.PanelClassDocumentInitializer;
import org.xwiki.security.script.SecurityScriptServiceComponentList;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.TestNoScriptMacro;
import org.xwiki.test.page.XWikiSyntax21ComponentList;
import org.xwiki.uiextension.internal.UIExtensionClassDocumentInitializer;
import org.xwiki.uiextension.script.UIExtensionScriptServiceComponentList;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.xwiki.uiextension.internal.WikiUIExtensionConstants.EXTENSION_POINT_ID_PROPERTY;
import static org.xwiki.uiextension.internal.WikiUIExtensionConstants.ID_PROPERTY;
import static org.xwiki.uiextension.internal.WikiUIExtensionConstants.PARAMETERS_PROPERTY;
import static org.xwiki.uiextension.internal.WikiUIExtensionConstants.SCOPE_PROPERTY;
import static org.xwiki.uiextension.internal.WikiUIExtensionConstants.UI_EXTENSION_CLASS;

/**
 * Page test for {@code Panels.Applications}.
 *
 * @version $Id$
 */
@ComponentList({
    UIExtensionClassDocumentInitializer.class,
    DefaultContextStoreManager.class,
    TestNoScriptMacro.class,
    PanelClassDocumentInitializer.class,
})
@UIExtensionScriptServiceComponentList
@HTML50ComponentList
@XWikiSyntax21ComponentList
@SecurityScriptServiceComponentList
class ApplicationsPageTest extends PageTest
{
    private static final DocumentReference PANEL_REFERENCE = new DocumentReference("xwiki", "Panels", "Applications");

    @BeforeEach
    void setUp() throws Exception
    {
        // Initialize the UIExtension class.
        this.xwiki.initializeMandatoryDocuments(this.context);

        // Register the component manager as wiki and user component manager so that the UIX can be registered on the
        // wiki with user scope.
        this.componentManager.registerComponent(ComponentManager.class, "wiki", this.componentManager);
        this.componentManager.registerComponent(ComponentManager.class, "user", this.componentManager);
    }

    @Test
    void displayApplicationPanel() throws Exception
    {
        // Create a document with a UI extension to test on.
        DocumentReference docRef = new DocumentReference("xwiki", "XWiki", "Test");
        XWikiDocument document = new XWikiDocument(docRef);
        BaseObject uiExtensionObject = document.newXObject(UI_EXTENSION_CLASS, this.context);
        uiExtensionObject.setStringValue(EXTENSION_POINT_ID_PROPERTY, "org.xwiki.platform.panels.Applications");
        uiExtensionObject.setStringValue(ID_PROPERTY, "test.id");
        uiExtensionObject.setLargeStringValue(PARAMETERS_PROPERTY, """
            icon=apps
            label=L
            target=Main.WebHome
            targetQueryString=q">{{/html}}{{noscript/}}{{html}}x
            """);
        uiExtensionObject.setStringValue(SCOPE_PROPERTY, "user");
        this.xwiki.saveDocument(document, this.context);

        // The event listeners are not registered by default. We trigger it manually so that the UIX is registered and
        // can be found and rendered.
        this.componentManager
            .<EventListener>getInstance(EventListener.class,
                DefaultWikiObjectComponentManagerEventListener.EVENT_LISTENER_NAME)
            .onEvent(new DocumentCreatedEvent(), document, null);

        // Load the sheet.
        loadPage(new DocumentReference("PanelSheet", PANEL_REFERENCE.getLastSpaceReference()));

        // Render the page.
        Document result = renderHTMLPage(PANEL_REFERENCE);

        assertEquals("/xwiki/bin/view/Main/?q\">{{/html}}{{noscript/}}{{html}}x",
            result.select(".applicationsPanel.nav a").attr("href"));
    }
}
