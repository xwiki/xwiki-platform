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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.internal.bridge.DefaultWikiObjectComponentManagerEventListener;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.rendering.RenderingScriptServiceComponentList;
import org.xwiki.rendering.internal.configuration.DefaultRenderingConfigurationComponentList;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.descriptor.DefaultMacroDescriptor;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax20ComponentList;
import org.xwiki.uiextension.internal.macro.UIExtensionMacro;
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
@ComponentList({XWikiUsersDocumentInitializer.class, UIExtensionMacro.class})
class UserProfilePageTest extends PageTest
{
    private static final DocumentReference USER_SHEET = new DocumentReference("xwiki", "XWiki", "XWikiUserSheet");

    private static final DocumentReference USER_PREFERENCES_SHEET =
        new DocumentReference("xwiki", "XWiki", "XWikiUserPreferencesSheet");

    private static final DocumentReference ADMIN_REFERENCE = new DocumentReference("xwiki", "XWiki", "Admin");

    private static final DocumentReference ALICE_REFERENCE = new DocumentReference("xwiki", "XWiki", "alice");

    private static final DocumentReference USER_PROFILE_SHEET =
        new DocumentReference("xwiki", "XWiki", "XWikiUserProfileSheet");

    @Mock
    private Macro<Object> attachmentSelectorMacro;

    @Mock
    private Macro<Object> notificationsMacro;

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

    @Test
    void avatarAttachmentSelectorFilter() throws Exception
    {
        // Collect the file extensions accepted by the attachment selectors displayed by the sheet.
        List<String> filters = new ArrayList<>();
        when(this.attachmentSelectorMacro.supportsInlineMode()).thenReturn(true);
        when(this.attachmentSelectorMacro.getDescriptor()).thenReturn(
            new DefaultMacroDescriptor(new MacroId("attachmentSelector"), "Attachment Selector Macro"));
        when(this.attachmentSelectorMacro.execute(any(), any(), any())).then(invocation -> {
            filters.add(invocation.getArgument(2, MacroTransformationContext.class).getCurrentMacroBlock()
                .getParameter("filter"));
            return Collections.emptyList();
        });
        this.componentManager.registerComponent(Macro.class, "attachmentSelector", this.attachmentSelectorMacro);

        createUser(new HashMap<>());
        renderSheet(USER_SHEET);

        assertEquals(List.of("png,jpg,jpeg,gif,webp,avif,jxl"), filters);
    }

    @Test
    void notificationsUsersWithSpecialCharactersInUserName() throws Exception
    {
        // Collect the users passed to the notifications macro displayed by the user profile.
        List<String> users = new ArrayList<>();
        when(this.notificationsMacro.supportsInlineMode()).thenReturn(true);
        when(this.notificationsMacro.getDescriptor())
            .thenReturn(new DefaultMacroDescriptor(new MacroId("notifications"), "Notifications Macro"));
        when(this.notificationsMacro.execute(any(), any(), any())).then(invocation -> {
            users.add(invocation.getArgument(2, MacroTransformationContext.class).getCurrentMacroBlock()
                .getParameter("users"));
            return Collections.emptyList();
        });
        this.componentManager.registerComponent(Macro.class, "notifications", this.notificationsMacro);
        registerUIX(USER_PROFILE_SHEET);

        DocumentReference userReference =
            new DocumentReference("xwiki", "XWiki", "a\" /}}{{notifications users=\"b");
        createUser(userReference, new HashMap<>());
        renderSheet(USER_SHEET);

        assertEquals(List.of("xwiki:XWiki.a\" /}}{{notifications users=\"b"), users);
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
        return createUser(ALICE_REFERENCE, userProperties);
    }

    private XWikiDocument createUser(DocumentReference userReference, Map<String, Object> userProperties)
        throws Exception
    {
        this.xwiki.createUser(userReference.getName(), userProperties, this.context);
        XWikiDocument userDocument = this.xwiki.getDocument(userReference, this.context);

        // Setup the script context as if we are logged in with the created user on its user profile.
        this.context.setDoc(userDocument);
        this.context.setUserReference(userReference);

        return userDocument;
    }

    private Document renderSheet(DocumentReference sheetReference) throws Exception
    {
        XWikiDocument sheet = loadPage(sheetReference);

        return Jsoup.parse(sheet.getRenderedContent(this.context));
    }
}
