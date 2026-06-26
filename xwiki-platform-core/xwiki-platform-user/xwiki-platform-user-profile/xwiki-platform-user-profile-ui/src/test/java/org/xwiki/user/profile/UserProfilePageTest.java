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
package org.xwiki.user.profile;

import java.util.HashMap;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.internal.bridge.DefaultWikiObjectComponentManagerEventListener;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.rendering.RenderingScriptServiceComponentList;
import org.xwiki.rendering.internal.configuration.DefaultRenderingConfigurationComponentList;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax20ComponentList;
import org.xwiki.uiextension.script.UIExtensionScriptServiceComponentList;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.XWikiUsersDocumentInitializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Page test of {@code XWiki.XWikiUserSheet} that is used to display the user profile.
 * 
 * @version $Id$
 * @since 14.10.5
 * @since 15.1RC1
 */
@HTML50ComponentList
@XWikiSyntax20ComponentList
@RenderingScriptServiceComponentList
@DefaultRenderingConfigurationComponentList
@UIExtensionScriptServiceComponentList
@ComponentList({XWikiUsersDocumentInitializer.class})
class UserProfilePageTest extends PageTest
{
    private static final DocumentReference USER_SHEET = new DocumentReference("xwiki", "XWiki", "XWikiUserSheet");

    private static final DocumentReference USER_PREFERENCES_SHEET =
        new DocumentReference("xwiki", "XWiki", "XWikiUserPreferencesSheet");

    private static final DocumentReference ADMIN_REFERENCE = new DocumentReference("xwiki", "XWiki", "Admin");

    private static final DocumentReference ALICE_REFERENCE = new DocumentReference("xwiki", "XWiki", "alice");

    @BeforeEach
    void setUp() throws Exception
    {
        // Initialize the XWiki.XWikiUsers class (required in order to be able to display user properties).
        this.xwiki.initializeMandatoryDocuments(this.context);

        when(this.oldcore.getMockDocumentAuthorizationManager()
            .hasAccess(any(), any(), eq(ADMIN_REFERENCE), any())).thenReturn(true);

        // Register the user profile UI extensions at wiki level (requires administration rights).
        this.componentManager.registerComponent(ComponentManager.class, "wiki", this.componentManager);
        this.context.setUserReference(ADMIN_REFERENCE);
        registerUIX(USER_PREFERENCES_SHEET);
    }

    @Test
    void timeZoneDisplay() throws Exception
    {
        Map<String, Object> userProperties = new HashMap<>();
        userProperties.put("timezone", "Europe/<em>Bucharest</em>");
        createUser(userProperties);

        Document dom = renderSheet(USER_SHEET);
        Element timeZone = dom.selectFirst(".userPreferences dd[data-user-property='timezone']");
        assertEquals(userProperties.get("timezone"), timeZone.text());
    }

    private void registerUIX(DocumentReference documentReference) throws Exception
    {
        XWikiDocument document = loadPage(documentReference);

        // The event listeners are not registered by default. We trigger it manually so that the UIX is registered and
        // can be found and rendered.
        this.componentManager
            .<EventListener>getInstance(EventListener.class,
                DefaultWikiObjectComponentManagerEventListener.EVENT_LISTENER_NAME)
            .onEvent(new DocumentCreatedEvent(), document, null);
    }

    private XWikiDocument createUser(Map<String, Object> userProperties) throws Exception
    {
        this.xwiki.createUser(ALICE_REFERENCE.getName(), userProperties, this.context);
        XWikiDocument userDocument = this.xwiki.getDocument(ALICE_REFERENCE, this.context);

        // Setup the script context as if we are logged in with the created user on its user profile.
        this.context.setDoc(userDocument);
        this.context.setUserReference(ALICE_REFERENCE);

        return userDocument;
    }

    private Document renderSheet(DocumentReference sheetReference) throws Exception
    {
        XWikiDocument sheet = loadPage(sheetReference);

        return Jsoup.parse(sheet.getRenderedContent(this.context));
    }
}
