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
package org.xwiki.pagetemplate.internal;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.component.internal.ContextComponentManagerProvider;
import org.xwiki.localization.LocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.properties.internal.DefaultConverterManager;
import org.xwiki.properties.internal.converter.ConvertUtilsConverter;
import org.xwiki.properties.internal.converter.EnumConverter;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.refactoring.event.DocumentRenamedEvent;
import org.xwiki.refactoring.job.MoveRequest;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.store.StoreConfiguration;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.xwiki.pagetemplate.internal.TemplateProviderClassDocumentInitializer.CLASS_REFERENCE;
import static org.xwiki.pagetemplate.internal.TemplateProviderClassDocumentInitializer.CREATION_RESTRICTIONS;
import static org.xwiki.pagetemplate.internal.TemplateProviderClassDocumentInitializer.SPACES;
import static org.xwiki.pagetemplate.internal.TemplateProviderClassDocumentInitializer.VISIBILITY_RESTRICTIONS;

/**
 * Unit tests for {@link TemplateProviderRestrictionsRenameListener}.
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
@ComponentList({
    DefaultConverterManager.class,
    ContextComponentManagerProvider.class,
    EnumConverter.class,
    ConvertUtilsConverter.class,
    TemplateProviderRestrictionsUpdater.class
})
class TemplateProviderRestrictionsRenameListenerTest
{
    private static final String WIKI = "xwiki";

    private static final DocumentReference PROVIDER_REFERENCE =
        new DocumentReference(WIKI, List.of("Templates", "Code"), "Provider");

    private static final DocumentReference OLD_HOME = new DocumentReference(WIKI, List.of("Main", "Initial"), "WebHome");

    private static final DocumentReference NEW_HOME = new DocumentReference(WIKI, List.of("Main", "Final"), "WebHome");

    private static final DocumentReference USER_REFERENCE = new DocumentReference(WIKI, "XWiki", "Alice");

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @InjectMockComponents
    private TemplateProviderRestrictionsRenameListener listener;

    @MockComponent
    private LocalizationManager localizationManager;

    @MockComponent
    private ObservationManager observationManager;

    @MockComponent
    private StoreConfiguration storeConfiguration;

    @MockComponent
    private UserReferenceResolver<CurrentUserReference> userReferenceResolver;

    @Mock
    private Query query;

    @BeforeEach
    void setUp() throws Exception
    {
        XWikiContext xcontext = this.oldcore.getXWikiContext();
        XWikiDocument classDocument = new XWikiDocument(new DocumentReference(CLASS_REFERENCE, new WikiReference(WIKI)));
        new TemplateProviderClassDocumentInitializer().createClass(classDocument.getXClass());
        xcontext.getWiki().saveDocument(classDocument, xcontext);

        QueryManager queryManager = this.oldcore.getQueryManager();
        when(queryManager.createQuery(anyString(), eq(Query.XWQL))).thenReturn(this.query);
        when(this.query.setWiki(WIKI)).thenReturn(this.query);
        when(this.query.execute()).thenReturn(List.of("Templates.Code.Provider"));

        when(this.localizationManager.getDefaultLocale()).thenReturn(Locale.ENGLISH);
        when(this.oldcore.getMockContextualAuthorizationManager().hasAccess(Right.EDIT, PROVIDER_REFERENCE))
            .thenReturn(true);
    }

    private void saveProvider(List<String> visibilityRestrictions, List<String> creationRestrictions,
        List<String> spaces) throws Exception
    {
        XWikiContext xcontext = this.oldcore.getXWikiContext();
        XWikiDocument provider = new XWikiDocument(PROVIDER_REFERENCE);
        BaseObject object = provider.newXObject(CLASS_REFERENCE, xcontext);
        object.set(VISIBILITY_RESTRICTIONS, visibilityRestrictions, xcontext);
        object.set(CREATION_RESTRICTIONS, creationRestrictions, xcontext);
        object.set(SPACES, spaces, xcontext);
        xcontext.getWiki().saveDocument(provider, xcontext);
    }

    private BaseObject getProviderObject() throws Exception
    {
        XWikiContext xcontext = this.oldcore.getXWikiContext();
        return xcontext.getWiki().getDocument(PROVIDER_REFERENCE, xcontext).getXObject(CLASS_REFERENCE);
    }

    private String getProviderVersion() throws Exception
    {
        XWikiContext xcontext = this.oldcore.getXWikiContext();
        return xcontext.getWiki().getDocument(PROVIDER_REFERENCE, xcontext).getVersion();
    }

    private MoveRequest createMoveRequest(boolean updateLinks)
    {
        MoveRequest request = new MoveRequest();
        request.setUpdateLinks(updateLinks);
        request.setCheckRights(true);
        request.setUserReference(USER_REFERENCE);
        request.setAuthorReference(USER_REFERENCE);
        return request;
    }

    @Test
    void renameUpdatesMatchingRestrictions() throws Exception
    {
        saveProvider(List.of("Main.Initial", "Other"), List.of("Main.Initial"), List.of("Main.Initial"));
        when(this.oldcore.getMockAuthorizationManager().hasAccess(Right.EDIT, USER_REFERENCE, PROVIDER_REFERENCE))
            .thenReturn(true);

        this.listener.onEvent(new DocumentRenamedEvent(OLD_HOME, NEW_HOME), null, createMoveRequest(true));

        BaseObject object = getProviderObject();
        assertEquals(List.of("Main.Final", "Other"), object.getListValue(VISIBILITY_RESTRICTIONS));
        assertEquals(List.of("Main.Final"), object.getListValue(CREATION_RESTRICTIONS));
        assertEquals(List.of("Main.Final"), object.getListValue(SPACES));
        assertEquals("The restrictions of the template provider [xwiki:Templates.Code.Provider] targeting "
            + "[Space xwiki:Main.Initial] have been updated to target [Space xwiki:Main.Final].",
            this.logCapture.getMessage(0));
    }

    @Test
    void renameOutsideOfRefactoringJobUpdatesRestrictions() throws Exception
    {
        saveProvider(List.of(), List.of("Main.Initial"), List.of());

        this.listener.onEvent(new DocumentRenamedEvent(OLD_HOME, NEW_HOME), null, null);

        assertEquals(List.of("Main.Final"), getProviderObject().getListValue(CREATION_RESTRICTIONS));
        assertEquals("The restrictions of the template provider [xwiki:Templates.Code.Provider] targeting "
            + "[Space xwiki:Main.Initial] have been updated to target [Space xwiki:Main.Final].",
            this.logCapture.getMessage(0));
    }

    @Test
    void moveToAnotherWikiStoresTheWiki() throws Exception
    {
        saveProvider(List.of(), List.of("Main.Initial"), List.of());

        DocumentReference newHome = new DocumentReference("other", List.of("Main", "Initial"), "WebHome");
        this.listener.onEvent(new DocumentRenamedEvent(OLD_HOME, newHome), null, null);

        assertEquals(List.of("other:Main.Initial"), getProviderObject().getListValue(CREATION_RESTRICTIONS));
        assertEquals("The restrictions of the template provider [xwiki:Templates.Code.Provider] targeting "
            + "[Space xwiki:Main.Initial] have been updated to target [Space other:Main.Initial].",
            this.logCapture.getMessage(0));
    }

    @Test
    void renameWithoutUpdatingLinksKeepsRestrictions() throws Exception
    {
        saveProvider(List.of(), List.of("Main.Initial"), List.of());
        String version = getProviderVersion();

        this.listener.onEvent(new DocumentRenamedEvent(OLD_HOME, NEW_HOME), null, createMoveRequest(false));

        assertEquals(List.of("Main.Initial"), getProviderObject().getListValue(CREATION_RESTRICTIONS));
        assertEquals(version, getProviderVersion());
    }

    @Test
    void renameWithoutEditRightKeepsRestrictions() throws Exception
    {
        saveProvider(List.of(), List.of("Main.Initial"), List.of());
        String version = getProviderVersion();

        this.listener.onEvent(new DocumentRenamedEvent(OLD_HOME, NEW_HOME), null, createMoveRequest(true));

        assertEquals(List.of("Main.Initial"), getProviderObject().getListValue(CREATION_RESTRICTIONS));
        assertEquals(version, getProviderVersion());
    }

    @Test
    void renameOfTerminalPageKeepsRestrictions() throws Exception
    {
        saveProvider(List.of(), List.of("Main.Initial"), List.of());
        String version = getProviderVersion();

        // Main.Initial is the space of the renamed terminal page, which doesn't move the space itself.
        this.listener.onEvent(new DocumentRenamedEvent(new DocumentReference(WIKI, List.of("Main", "Initial"), "Page"),
            new DocumentReference(WIKI, List.of("Main", "Final"), "Page")), null, null);

        assertEquals(List.of("Main.Initial"), getProviderObject().getListValue(CREATION_RESTRICTIONS));
        assertEquals(version, getProviderVersion());
    }

    @Test
    void renameOfUnrelatedSpaceKeepsRestrictions() throws Exception
    {
        saveProvider(List.of(), List.of("Main.Initial.Child", "Main"), List.of());
        String version = getProviderVersion();

        this.listener.onEvent(new DocumentRenamedEvent(OLD_HOME, NEW_HOME), null, null);

        assertEquals(List.of("Main.Initial.Child", "Main"), getProviderObject().getListValue(CREATION_RESTRICTIONS));
        assertEquals(version, getProviderVersion());
    }
}
