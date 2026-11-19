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

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.internal.bridge.DefaultWikiObjectComponentManagerEventListener;
import org.xwiki.logging.script.LoggingScriptService;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.rendering.RenderingScriptServiceComponentList;
import org.xwiki.rendering.internal.configuration.DefaultRenderingConfigurationComponentList;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.descriptor.DefaultMacroDescriptor;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax20ComponentList;
import org.xwiki.test.page.XWikiSyntax21ComponentList;
import org.xwiki.uiextension.internal.macro.UIExtensionMacro;
import org.xwiki.uiextension.script.UIExtensionScriptServiceComponentList;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.XWikiUsersDocumentInitializer;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
@XWikiSyntax21ComponentList
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

    private static final DocumentReference USER_PROFILE_MENU_GROUPS =
        new DocumentReference("xwiki", "XWiki", "UserProfileMenuGroups");

    private static final LocalDocumentReference UIX_CLASS = new LocalDocumentReference("XWiki", "UIExtensionClass");

    private static final String MENU_EXTENSION_POINT = "org.xwiki.plaftorm.user.profile.menu";

    private static final String MENU_GROUP_EXTENSION_POINT = "org.xwiki.platform.user.profile.menuGroup";

    private static final String MENU_ITEM_SELECTOR = "#user-profile-menu .list-group-item";

    private static final String DATA_ID = "data-id";

    @Mock
    private Macro<Object> attachmentSelectorMacro;

    @Mock
    private Logger logger;

    private XWikiDocument menuGroupsPage;

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
        this.menuGroupsPage = registerUIX(USER_PROFILE_MENU_GROUPS);
        registerUIX(USER_PREFERENCES_SHEET);

        LoggingScriptService loggingScriptService = mock(LoggingScriptService.class);
        this.componentManager.registerComponent(ScriptService.class, "logging", loggingScriptService);
        when(loggingScriptService.getLogger("XWiki.XWikiUserSheet")).thenReturn(this.logger);
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

    @Test
    void menuGroups() throws Exception
    {
        // A group without items is hidden.
        registerUIX("emptyGroup", MENU_GROUP_EXTENSION_POINT, "id=empty\npriority=150");
        // A group with the id of an existing group, but a higher priority, is ignored.
        registerUIX("duplicateGroup", MENU_GROUP_EXTENSION_POINT, "id=profile\npriority=400");
        registerUIX("profileItem", MENU_EXTENSION_POINT, "id=first\ngroup=profile\npriority=10");
        registerUIX("secondProfileItem", MENU_EXTENSION_POINT, "id=second\ngroup=profile\npriority=30");
        registerUIX("inactiveItem", MENU_EXTENSION_POINT,
            "id=inactive\ngroup=profile\nisActive=false\npriority=5");
        // Items without a group, or with an unknown group, go in the "other" group.
        registerUIX("itemWithoutGroup", MENU_EXTENSION_POINT, "id=noGroup\npriority=40");
        registerUIX("itemWithUnknownGroup", MENU_EXTENSION_POINT, "id=unknownGroup\ngroup=unknown\npriority=50");
        createUser(new HashMap<>());

        Document dom = renderSheet(USER_SHEET);

        assertEquals(List.of("profile", "settings", "other"), getMenuGroups(dom));
        assertEquals(List.of("first", "second"), getMenuItems(dom, "profile"));
        assertEquals(List.of("preferences"), getMenuItems(dom, "settings"));
        assertEquals(List.of("noGroup", "unknownGroup"), getMenuItems(dom, "other"));
        // The first item is displayed by default, so its group is the only one expanded.
        assertEquals(List.of("first"), dom.select(MENU_ITEM_SELECTOR + ".active").eachAttr(DATA_ID));
        assertEquals(List.of("user-profile-menu-group-profile"),
            dom.select("#user-profile-menu .panel-collapse.in").eachAttr("id"));
        assertEquals(List.of("firstPane"), dom.select(".user-page-pane:not(.hidden)").eachAttr("id"));
        verifyNoInteractions(this.logger);
    }

    @Test
    void menuItemsDroppedWithoutOtherGroup() throws Exception
    {
        // Replace the default groups, which include the "other" group, with a single group.
        unregisterUIX(this.menuGroupsPage);
        registerUIX("profileGroup", MENU_GROUP_EXTENSION_POINT, "id=profile\npriority=100");
        // A group without an id is ignored, so its items have no group either.
        registerUIX("groupWithoutId", MENU_GROUP_EXTENSION_POINT, "priority=50");
        registerUIX("profileItem", MENU_EXTENSION_POINT, "id=first\ngroup=profile\npriority=10");
        registerUIX("itemWithoutGroup", MENU_EXTENSION_POINT, "id=noGroup\npriority=40");
        registerUIX("itemOfGroupWithoutId", MENU_EXTENSION_POINT,
            "id=groupWithoutIdItem\ngroup=test.groupWithoutId\npriority=60");
        createUser(new HashMap<>());

        Document dom = renderSheet(USER_SHEET);

        assertEquals(List.of("profile"), getMenuGroups(dom));
        assertEquals(List.of("first"), getMenuItems(dom, "profile"));
        assertEquals(List.of("firstPane"), dom.select(".user-page-pane").eachAttr("id"));
        String message = "The user profile menu item [{}] is not displayed because its group [{}] doesn't exist and"
            + " there's no [other] group to fall back on.";
        verify(this.logger).warn(message, "preferences", "settings");
        verify(this.logger).warn(message, "noGroup", null);
        verify(this.logger).warn(message, "groupWithoutIdItem", "test.groupWithoutId");
        verify(this.logger).warn("The user profile menu group [{}] is not displayed because it has no [id] parameter.",
            "test.groupWithoutId");
    }

    private List<String> getMenuGroups(Document dom)
    {
        return dom.select("#user-profile-menu > .panel > .panel-collapse").eachAttr("id").stream()
            .map(id -> StringUtils.removeStart(id, "user-profile-menu-group-")).toList();
    }

    private List<String> getMenuItems(Document dom, String groupId)
    {
        return dom.select("#user-profile-menu-group-" + groupId + " .list-group-item").eachAttr(DATA_ID);
    }

    private void registerUIX(String name, String extensionPointId, String parameters) throws Exception
    {
        XWikiDocument document = new XWikiDocument(new DocumentReference("xwiki", "XWiki", name));
        BaseObject uix = document.newXObject(UIX_CLASS, this.context);
        uix.setStringValue("name", "test." + name);
        uix.setStringValue("extensionPointId", extensionPointId);
        uix.setLargeStringValue("parameters", parameters);
        uix.setStringValue("scope", "wiki");
        document.setAuthorReference(ADMIN_REFERENCE);
        this.xwiki.saveDocument(document, this.context);
        registerUIX(document);
    }

    private XWikiDocument registerUIX(DocumentReference documentReference) throws Exception
    {
        XWikiDocument document = loadPage(documentReference);
        registerUIX(document);
        return document;
    }

    private void registerUIX(XWikiDocument document) throws Exception
    {
        // The event listeners are not registered by default. We trigger it manually so that the UIX is registered and
        // can be found and rendered.
        getWikiObjectComponentListener().onEvent(new DocumentCreatedEvent(), document, null);
    }

    private void unregisterUIX(XWikiDocument document) throws Exception
    {
        XWikiDocument deletedDocument = new XWikiDocument(document.getDocumentReference());
        deletedDocument.setOriginalDocument(document);
        getWikiObjectComponentListener().onEvent(new DocumentDeletedEvent(), deletedDocument, null);
    }

    private EventListener getWikiObjectComponentListener() throws Exception
    {
        return this.componentManager.getInstance(EventListener.class,
            DefaultWikiObjectComponentManagerEventListener.EVENT_LISTENER_NAME);
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
