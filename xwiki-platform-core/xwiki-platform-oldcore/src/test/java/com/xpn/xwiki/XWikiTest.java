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
package com.xpn.xwiki;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.inject.Provider;
import javax.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentDeletingEvent;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.query.QueryExecutor;
import org.xwiki.refactoring.ReferenceRenamer;
import org.xwiki.refactoring.internal.ReferenceUpdater;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.wiki.WikiModel;
import org.xwiki.test.annotation.AfterComponent;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserProperties;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.XWikiCfgConfigurationSource;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.store.XWikiRecycleBinStoreInterface;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.user.impl.xwiki.XWikiGroupServiceImpl;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiServletRequest;
import com.xpn.xwiki.web.XWikiServletRequestStub;
import com.xpn.xwiki.web.XWikiServletResponseStub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link XWiki}.
 *
 * @version $Id$
 */
@OldcoreTest(mockXWiki = false)
@AllComponents
class XWikiTest
{
    @InjectMockitoOldcore
    MockitoOldcore oldcore;

    @MockComponent
    private XWikiRecycleBinStoreInterface recycleBinStoreInterface;

    @MockComponent
    private ReferenceRenamer referenceRenamer;

    @MockComponent
    private ReferenceUpdater referenceUpdater;

    private static final String DOCWIKI = "Wiki";

    private static final String DOCSPACE = "MilkyWay";

    private static final String DOCNAME = "Fidis";

    private XWikiDocument document;

    private XWiki xwiki;

    @AfterComponent
    public void afterComponent() throws Exception
    {
        MockitoComponentManager componentManager = this.oldcore.getMocker();

        // Unregister XWikiCfgConfigurationSource so that it's mocked by MockitoOldcore
        componentManager.unregisterComponent(ConfigurationSource.class, XWikiCfgConfigurationSource.ROLEHINT);

        // Mock the HQL query executor because we don't need it and it can cause problems
        componentManager.registerMockComponent(QueryExecutor.class, "hql");

        componentManager.unregisterComponent(new DefaultParameterizedType(null, Provider.class, ComponentManager.class)
            , "context");
        Provider<ComponentManager> componentManagerProvider = componentManager
            .registerMockComponent(new DefaultParameterizedType(null, Provider.class, ComponentManager.class)
                , "context");
        when(componentManagerProvider.get()).thenReturn(componentManager);
        componentManager.registerMockComponent(WikiModel.class);
    }

    @BeforeEach
    protected void beforeEach() throws Exception
    {
        this.document = new XWikiDocument(new DocumentReference("Wiki", "MilkyWay", "Fidis"));
        this.oldcore.getXWikiContext().setRequest(new XWikiServletRequestStub());
        this.oldcore.getXWikiContext().setResponse(new XWikiServletResponseStub());
        this.oldcore.getXWikiContext().setURL(new URL("http://localhost:8080/xwiki/bin/view/MilkyWay/Fidis"));

        this.oldcore.getXWikiContext().setLocale(null);

        this.xwiki = this.oldcore.getSpyXWiki();

        // Ensure that no Velocity Templates are going to be used when executing Velocity since otherwise
        // the Velocity init would fail (since by default the macros.vm templates wouldn't be found as we're
        // not providing it in our unit test resources).
        this.oldcore.getMockXWikiCfg().setProperty("xwiki.render.velocity.macrolist", "");

        this.xwiki.saveDocument(this.document, this.oldcore.getXWikiContext());

        this.document.setCreator("Condor");
        this.document.setAuthor("Albatross");

        this.xwiki.saveDocument(this.document, this.oldcore.getXWikiContext());
        this.xwiki.setRecycleBinStore(this.recycleBinStoreInterface);
        this.oldcore.getXWikiContext().put("isInRenderingEngine", true);
    }

    @Test
    void userNotAddedByDefaultToXWikiAllGroupWhenThisGroupImplicit() throws Exception
    {
        // given
        this.oldcore.getMockXWikiCfg().setProperty("xwiki.authentication.group.allgroupimplicit", "1");

        XWikiGroupServiceImpl xWikiGroupService = new XWikiGroupServiceImpl();
        this.xwiki.setGroupService(xWikiGroupService);

        // when
        this.xwiki.setUserDefaultGroup("XWiki.user1", this.oldcore.getXWikiContext());

        // then
        verify(this.xwiki, times(0)).addUserToGroup(anyString(), anyString(), any(XWikiContext.class));
    }

    @Test
    void userAddedToXWikiAllGroupWhenItsSpecifiedByConfigurationRegardlessXWikiAllGroupIsImplicit() throws Exception
    {
        // given
        this.oldcore.getMockXWikiCfg().setProperty("xwiki.authentication.group.allgroupimplicit", "1");
        this.oldcore.getMockXWikiCfg().setProperty("xwiki.users.initialGroups", "XWiki.XWikiAllGroup");

        XWikiGroupServiceImpl xWikiGroupService = new XWikiGroupServiceImpl();
        this.xwiki.setGroupService(xWikiGroupService);

        // when
        this.xwiki.setUserDefaultGroup("XWiki.user1", this.oldcore.getXWikiContext());

        // then
        verify(this.xwiki, times(1)).addUserToGroup("XWiki.user1", "XWiki.XWikiAllGroup",
            this.oldcore.getXWikiContext());
    }

    @Test
    void authorAfterDocumentCopy() throws XWikiException
    {
        DocumentReference copyReference =
            new DocumentReference("Lyre", this.document.getDocumentReference().getLastSpaceReference());
        DocumentReference author = this.document.getAuthorReference();
        this.xwiki.copyDocument(this.document.getDocumentReference(), copyReference, this.oldcore.getXWikiContext());
        XWikiDocument copy = this.xwiki.getDocument(copyReference, this.oldcore.getXWikiContext());

        assertEquals(author, copy.getAuthorReference());
    }

    @Test
    void creatorAfterDocumentCopy() throws XWikiException
    {
        DocumentReference copyReference =
            new DocumentReference("Sirius", this.document.getDocumentReference().getLastSpaceReference());
        DocumentReference creator = this.document.getCreatorReference();
        this.xwiki.copyDocument(this.document.getDocumentReference(), copyReference, this.oldcore.getXWikiContext());
        XWikiDocument copy = this.xwiki.getDocument(copyReference, this.oldcore.getXWikiContext());

        assertEquals(creator, copy.getCreatorReference());
    }

    @Test
    void creationDateAfterDocumentCopy() throws XWikiException, InterruptedException
    {
        Date sourceCreationDate = this.document.getCreationDate();
        Thread.sleep(1000);
        DocumentReference copyReference = new DocumentReference(this.document.getDocumentReference().getName() + "Copy",
            this.document.getDocumentReference().getLastSpaceReference());
        this.xwiki.copyDocument(this.document.getDocumentReference(), copyReference, this.oldcore.getXWikiContext());
        XWikiDocument copy = this.xwiki.getDocument(copyReference, this.oldcore.getXWikiContext());

        assertEquals(copy.getCreationDate(), sourceCreationDate);
    }

    @Test
    void parseTemplateConsidersObjectField() throws XWikiException
    {
        DocumentReference skinReference =
            new DocumentReference(this.oldcore.getXWikiContext().getWikiId(), "XWiki", "XWikiSkins");
        XWikiDocument skinClass = new XWikiDocument(skinReference);
        skinClass.getXClass().addTextAreaField("template.vm", "template", 80, 20);
        this.xwiki.saveDocument(skinClass, this.oldcore.getXWikiContext());

        DocumentReference mySkinReference =
            new DocumentReference(this.oldcore.getXWikiContext().getWikiId(), "XWiki", "Skin");
        XWikiDocument skinDocument = new XWikiDocument(mySkinReference);
        BaseObject obj = skinDocument.newXObject(skinReference, this.oldcore.getXWikiContext());
        obj.setLargeStringValue("template.vm", "parsing a field");
        this.xwiki.saveDocument(skinDocument, this.oldcore.getXWikiContext());

        this.oldcore.getXWikiContext().put("skin", "XWiki.Skin");
        assertEquals("XWiki.Skin", this.xwiki.getSkin(this.oldcore.getXWikiContext()));
        assertFalse(this.xwiki.getDocument(mySkinReference, this.oldcore.getXWikiContext()).isNew());
        assertEquals(skinDocument, this.xwiki.getDocument(mySkinReference, this.oldcore.getXWikiContext()));
        assertEquals("parsing a field", this.xwiki.parseTemplate("template.vm", this.oldcore.getXWikiContext()));
    }

    /**
     * See XWIKI-2096
     */
    @Test
    void parseTemplateConsidersAttachment() throws XWikiException
    {
        XWikiDocument skin =
            new XWikiDocument(new DocumentReference(this.oldcore.getXWikiContext().getWikiId(), "XWiki", "Skin"));
        XWikiAttachment attachment = new XWikiAttachment();
        skin.getAttachmentList().add(attachment);
        attachment.setContent("parsing an attachment".getBytes());
        attachment.setFilename("template.vm");
        attachment.setDoc(skin);
        this.xwiki.saveDocument(skin, this.oldcore.getXWikiContext());
        this.oldcore.getXWikiContext().put("skin", "XWiki.Skin");

        assertEquals("XWiki.Skin", this.xwiki.getSkin(this.oldcore.getXWikiContext()));
        assertFalse(this.xwiki.getDocument("XWiki.Skin", this.oldcore.getXWikiContext()).isNew());
        assertEquals(skin, this.xwiki.getDocument("XWiki.Skin", this.oldcore.getXWikiContext()));
        assertEquals("parsing an attachment", this.xwiki.parseTemplate("template.vm", this.oldcore.getXWikiContext()));
    }

    /**
     * See XWIKI-2098
     */
    @Test
    void parseTemplateConsidersObjectFieldBeforeAttachment() throws Exception
    {
        DocumentReference skinReference = new DocumentReference("xwiki", "XWiki", "XWikiSkins");
        XWikiDocument skinClass = new XWikiDocument(skinReference);
        skinClass.getXClass().addTextAreaField("template.vm", "template", 80, 20);
        this.xwiki.saveDocument(skinClass, this.oldcore.getXWikiContext());

        DocumentReference mySkinReference = new DocumentReference("xwiki", "XWiki", "Skin");
        XWikiDocument skinDocument = new XWikiDocument(mySkinReference);
        BaseObject obj = skinDocument.newXObject(skinReference, this.oldcore.getXWikiContext());
        obj.setLargeStringValue("template.vm", "parsing a field");
        XWikiAttachment attachment = new XWikiAttachment();
        skinDocument.getAttachmentList().add(attachment);
        attachment.setContent(new ByteArrayInputStream("parsing an attachment".getBytes()));
        attachment.setFilename("template.vm");
        attachment.setDoc(skinDocument);
        this.xwiki.saveDocument(skinDocument, this.oldcore.getXWikiContext());
        this.oldcore.getXWikiContext().put("skin", "XWiki.Skin");
        assertEquals("XWiki.Skin", this.xwiki.getSkin(this.oldcore.getXWikiContext()));
        assertFalse(this.xwiki.getDocument(mySkinReference, this.oldcore.getXWikiContext()).isNew());
        assertEquals(skinDocument, this.xwiki.getDocument(mySkinReference, this.oldcore.getXWikiContext()));
        assertEquals("parsing a field", this.xwiki.parseTemplate("template.vm", this.oldcore.getXWikiContext()));
    }

    @Test
    void clearNameWithoutStripDotsWithoutAscii()
    {
        assertEquals("ee{&.txt", this.xwiki.clearName("éê{&.txt", false, false, this.oldcore.getXWikiContext()));
    }

    @Test
    void clearNameWithoutStripDotsWithAscii()
    {
        assertEquals("ee.txt", this.xwiki.clearName("éê{&.txt", false, true, this.oldcore.getXWikiContext()));
    }

    @Test
    void clearNameWithStripDotsWithoutAscii()
    {
        assertEquals("ee{&txt", this.xwiki.clearName("éê{&.txt", true, false, this.oldcore.getXWikiContext()));
    }

    @Test
    void clearNameWithStripDotsWithAscii()
    {
        assertEquals("eetxt", this.xwiki.clearName("éê{&.txt", true, true, this.oldcore.getXWikiContext()));
    }

    /**
     * We only verify here that the saveDocument API calls the Observation component.
     */
    @Test
    void saveDocumentSendsObservationEvents() throws Exception
    {
        EventListener mockListener = mock(EventListener.class);
        when(mockListener.getName()).thenReturn("testlistener");
        DocumentReference ref = new DocumentReference("xwikitest", "Some", "Document");
        when(mockListener.getEvents())
            .thenReturn(Arrays.asList(new DocumentCreatedEvent(ref), new DocumentCreatingEvent(ref)));

        ObservationManager om = this.oldcore.getMocker().getInstance(ObservationManager.class);
        om.addListener(mockListener);

        verify(mockListener).getEvents();

        XWikiDocument document = new XWikiDocument(ref);
        document.setContent("the content");

        this.xwiki.saveDocument(document, this.oldcore.getXWikiContext());

        // Ensure that the onEvent method has been called before and after the save
        verify(mockListener).onEvent(any(DocumentCreatingEvent.class), any(XWikiDocument.class),
            same(this.oldcore.getXWikiContext()));
        verify(mockListener).onEvent(any(DocumentCreatedEvent.class), any(XWikiDocument.class),
            same(this.oldcore.getXWikiContext()));
    }

    @Test
    void saveDocumentWithListenerSwitchDirtyFlagToTrue() throws Exception
    {
        EventListener mockListener = mock(EventListener.class);
        when(mockListener.getName()).thenReturn("testlistener");
        DocumentReference ref = new DocumentReference("xwikitest", "Some", "Document");
        when(mockListener.getEvents())
            .thenReturn(List.of(new DocumentCreatingEvent(ref)));
        doAnswer(invocation -> {
            invocation.<XWikiDocument>getArgument(1).setContent("modified content");
            return null;
        }).when(mockListener).onEvent(any(), any(), any());

        doAnswer(invocation -> {
            XWikiDocument document = invocation.getArgument(0);
            assertFalse(document.isMetaDataDirty());
            assertFalse(document.isContentDirty());

            return null;
        }).when(this.oldcore.getMockStore()).saveXWikiDoc(any(), any());

        ObservationManager om = this.oldcore.getMocker().getInstance(ObservationManager.class);
        om.addListener(mockListener);

        XWikiDocument document = new XWikiDocument(ref);
        document.setContent("the content");
        document.setMetaDataDirty(false);
        document.setContentDirty(false);

        this.xwiki.saveDocument(document, this.oldcore.getXWikiContext());

        // Ensure that the onEvent method has been called
        verify(mockListener).onEvent(any(DocumentCreatingEvent.class), any(XWikiDocument.class),
            same(this.oldcore.getXWikiContext()));
    }

    @Test
    void saveDocumentWithListenerSwitchDirtyFlagToFalse() throws Exception
    {
        EventListener mockListener = mock(EventListener.class);
        when(mockListener.getName()).thenReturn("testlistener");
        DocumentReference ref = new DocumentReference("xwikitest", "Some", "Document");
        when(mockListener.getEvents())
            .thenReturn(List.of(new DocumentCreatingEvent(ref)));
        doAnswer(invocation -> {
            invocation.<XWikiDocument>getArgument(1).setMetaDataDirty(false);
            invocation.<XWikiDocument>getArgument(1).setContentDirty(false);

            return null;
        }).when(mockListener).onEvent(any(), any(), any());

        doAnswer(invocation -> {
            XWikiDocument document = invocation.getArgument(0);
            assertFalse(document.isMetaDataDirty());
            assertFalse(document.isContentDirty());

            return null;
        }).when(this.oldcore.getMockStore()).saveXWikiDoc(any(), any());

        ObservationManager om = this.oldcore.getMocker().getInstance(ObservationManager.class);
        om.addListener(mockListener);

        XWikiDocument document = new XWikiDocument(ref);
        document.setContent("the content");

        this.xwiki.saveDocument(document, this.oldcore.getXWikiContext());

        // Ensure that the onEvent method has been called
        verify(mockListener).onEvent(any(DocumentCreatingEvent.class), any(XWikiDocument.class),
            same(this.oldcore.getXWikiContext()));
    }

    /**
     * We only verify here that the renameDocument API calls the Observation component.
     */
    @Test
    void renameDocumentSendsObservationEvents() throws Exception
    {
        DocumentReference sourceReference = new DocumentReference("xwikitest", "Some", "Source");
        DocumentReference targetReference = new DocumentReference("xwikitest", "Some", "Target");

        XWikiDocument sourceDocument = new XWikiDocument(sourceReference);
        sourceDocument.setSyntax(Syntax.PLAIN_1_0);
        this.xwiki.saveDocument(sourceDocument, this.oldcore.getXWikiContext());

        EventListener mockListener = mock(EventListener.class);
        when(mockListener.getName()).thenReturn("testlistener");
        when(mockListener.getEvents()).thenReturn(
            Arrays.asList(new DocumentCreatedEvent(targetReference), new DocumentCreatingEvent(targetReference),
                new DocumentDeletingEvent(sourceReference), new DocumentDeletedEvent(sourceReference)));

        ObservationManager om = this.oldcore.getMocker().getInstance(ObservationManager.class);
        om.addListener(mockListener);

        verify(mockListener).getEvents();

        this.xwiki.renameDocument(sourceReference, targetReference, false, Collections.emptyList(), null,
            this.oldcore.getXWikiContext());

        // Ensure that the onEvent method has been called before and after the rename
        verify(mockListener).onEvent(any(DocumentCreatingEvent.class), any(XWikiDocument.class),
            same(this.oldcore.getXWikiContext()));
        verify(mockListener).onEvent(any(DocumentCreatedEvent.class), any(XWikiDocument.class),
            same(this.oldcore.getXWikiContext()));
        verify(mockListener).onEvent(any(DocumentDeletingEvent.class), any(XWikiDocument.class),
            same(this.oldcore.getXWikiContext()));
        verify(mockListener).onEvent(any(DocumentDeletedEvent.class), any(XWikiDocument.class),
            same(this.oldcore.getXWikiContext()));
    }

    /**
     * We only verify here that the deleteDocument API calls the Observation component.
     */
    @Test
    void deleteDocumentSendsObservationEvents() throws Exception
    {
        EventListener mockListener = mock(EventListener.class);
        when(mockListener.getName()).thenReturn("testlistener");
        DocumentReference ref = new DocumentReference("xwikitest", "Another", "Document");
        when(mockListener.getEvents())
            .thenReturn(Arrays.asList(new DocumentDeletedEvent(ref), new DocumentDeletingEvent(ref)));

        ObservationManager om = this.oldcore.getMocker().getInstance(ObservationManager.class);
        om.addListener(mockListener);

        verify(mockListener).getEvents();

        XWikiDocument document = new XWikiDocument(ref);
        document.setContent("the content");

        // Not expectation on mock Listener since we're not subscribed to Document save events

        this.xwiki.saveDocument(document, this.oldcore.getXWikiContext());

        this.xwiki.deleteDocument(document, false, this.oldcore.getXWikiContext());

        // Ensure that the onEvent method has been called before and after the save
        verify(mockListener).onEvent(any(DocumentDeletingEvent.class), any(XWikiDocument.class),
            same(this.oldcore.getXWikiContext()));
        verify(mockListener).onEvent(any(DocumentDeletedEvent.class), any(XWikiDocument.class),
            same(this.oldcore.getXWikiContext()));
    }

    @Test
    void languageSelection()
    {
        this.oldcore.getXWikiContext().setRequest(new XWikiServletRequest(null)
        {
            @Override
            public Enumeration<Locale> getLocales()
            {
                return Collections.enumeration(List.of(
                    new Locale("*"),
                    new Locale("en", "US"),
                    new Locale("fr"),
                    new Locale("de")
                ));
            }

            @Override
            public String getHeader(String s)
            {
                if ("language".equals(s)) {
                    return null;
                }
                return "en";
            }

            @Override
            public Cookie getCookie(String cookieName)
            {
                return null;
            }
        });

        // Set the wiki to multilingual mode.
        this.oldcore.getMockWikiConfigurationSource().setProperty("multilingual", "1");

        assertEquals("en", this.xwiki.getLanguagePreference(this.oldcore.getXWikiContext()));
    }

    @Test
    void languageSelectionWithSupportedLanguages()
    {
        this.oldcore.getXWikiContext().setRequest(new XWikiServletRequest(null)
        {
            @Override
            public Enumeration<Locale> getLocales()
            {
                return Collections.enumeration(List.of(
                    new Locale("*"),
                    new Locale("fr", "FR"),
                    new Locale("de")
                ));
            }

            @Override
            public String getHeader(String s)
            {
                if ("language".equals(s)) {
                    return null;
                }
                return "en";
            }

            @Override
            public Cookie getCookie(String cookieName)
            {
                return null;
            }
        });

        // Set the wiki to multilingual mode.
        this.oldcore.getMockWikiConfigurationSource().setProperty("multilingual", "1");
        this.oldcore.getMockWikiConfigurationSource().setProperty("languages", "en, |fr_FR, |en_US, |fr_CA");

        assertEquals("fr_FR", this.xwiki.getLanguagePreference(this.oldcore.getXWikiContext()));
    }

    /**
     * XWIKI-8469: Bad default of 1 in XWiki.isMultilingual instead of 0 (when no XWikiPreferences object exists)
     */
    @Test
    void isMultilingualDefaultFalse()
    {
        assertFalse(this.xwiki.isMultiLingual(this.oldcore.getXWikiContext()));
    }

    @Test
    void getCurrentContentSyntaxId()
    {
        XWikiDocument doc1 = new XWikiDocument();
        doc1.setSyntax(Syntax.CREOLE_1_0);
        XWikiDocument doc2 = new XWikiDocument();
        doc2.setSyntax(Syntax.PLAIN_1_0);

        assertNull(this.xwiki.getCurrentContentSyntaxId(null, this.oldcore.getXWikiContext()));
        assertEquals("syntaxId", this.xwiki.getCurrentContentSyntaxId("syntaxId", this.oldcore.getXWikiContext()));

        this.oldcore.getXWikiContext().setDoc(doc1);

        assertEquals(Syntax.CREOLE_1_0.toIdString(),
            this.xwiki.getCurrentContentSyntaxId(null, this.oldcore.getXWikiContext()));

        this.oldcore.getXWikiContext().put("sdoc", doc2);

        assertEquals(Syntax.PLAIN_1_0.toIdString(),
            this.xwiki.getCurrentContentSyntaxId(null, this.oldcore.getXWikiContext()));
    }

    /**
     * Check that the user validation feature works when the validation key is stored both as plain text and as a hashed
     * field.
     *
     * @throws Exception when any exception occurs inside XWiki
     */
    @Test
    void validationKeyStorage() throws Exception
    {
        XWikiContext context = this.oldcore.getXWikiContext();
        context.setLanguage("en");

        // Prepare the request
        XWikiRequest request = mock(XWikiRequest.class);
        when(request.getParameter("xwikiname")).thenReturn("TestUser");
        when(request.getParameter("validkey")).thenReturn("plaintextkey");
        context.setRequest(request);

        // Prepare the user profile
        XWikiDocument testUser =
            new XWikiDocument(new DocumentReference(this.oldcore.getXWikiContext().getWikiId(), "XWiki", "TestUser"));
        BaseObject userObject = (BaseObject) this.xwiki.getUserClass(context).newObject(context);

        // Check with a correct plaintext key
        BaseProperty validationKey = new StringProperty();
        validationKey.setValue("plaintextkey");
        userObject.safeput("validkey", validationKey);
        testUser.addObject("XWiki.XWikiUsers", userObject);

        this.xwiki.saveDocument(testUser, context);

        assertEquals(0, this.xwiki.validateUser(false, this.oldcore.getXWikiContext()));
        XWikiDocument reloadedDocument = this.xwiki.getDocument(testUser, context);
        assertEquals("", reloadedDocument.getObject("XWiki.XWikiUsers").getStringValue("validkey"));

        // Check with an incorrect plaintext key
        validationKey.setValue("wrong key");
        this.xwiki.saveDocument(testUser, context);

        assertEquals(-1, this.xwiki.validateUser(false, this.oldcore.getXWikiContext()));

        // Check with a correct hashed key
        validationKey = ((PropertyClass) this.xwiki.getUserClass(context).get("validkey")).fromString("plaintextkey");
        assertTrue(validationKey.getValue().toString().startsWith("hash:"));
        userObject.safeput("validkey", validationKey);
        this.xwiki.saveDocument(testUser, context);

        assertEquals(0, this.xwiki.validateUser(false, this.oldcore.getXWikiContext()));

        // Check with an incorrect hashed key
        validationKey = ((PropertyClass) this.xwiki.getUserClass(context).get("validkey")).fromString("wrong key");
        assertTrue(validationKey.getValue().toString().startsWith("hash:"));
        userObject.safeput("validkey", validationKey);
        this.xwiki.saveDocument(testUser, context);

        assertEquals(-1, this.xwiki.validateUser(false, this.oldcore.getXWikiContext()));
    }

    /**
     * Tests that XWiki.XWikiPreferences page is not saved each time XWiki is initialized.
     *
     * @throws Exception when any exception occurs inside XWiki
     */
    @Test
    void getPrefsClass() throws Exception
    {
        when(this.oldcore.getMockStore().getLimitSize(any(), any(), any())).thenReturn(255);

        XWikiDocument prefsDoc = new XWikiDocument(new DocumentReference("xwiki", "XWiki", "XWikiPreferences"));
        final Map<DocumentReference, XWikiDocument> documents = new HashMap<DocumentReference, XWikiDocument>();
        documents.put(prefsDoc.getDocumentReference(), prefsDoc);

        when(this.oldcore.getMockStore().loadXWikiDoc(notNull(), same(this.oldcore.getXWikiContext())))
            .then(invocation -> {
                XWikiDocument document = invocation.getArgument(0);
                if (!documents.containsKey(document.getDocumentReference())) {
                    documents.put(document.getDocumentReference(), document);
                } else {
                    document = documents.get(document.getDocumentReference());
                }
                return document;
            });

        this.xwiki.getPrefsClass(this.oldcore.getXWikiContext());
        this.xwiki.getPrefsClass(this.oldcore.getXWikiContext());

        verify(this.oldcore.getMockStore(), atLeastOnce()).loadXWikiDoc(notNull(),
            same(this.oldcore.getXWikiContext()));
        verify(this.oldcore.getMockStore()).saveXWikiDoc(same(prefsDoc), same(this.oldcore.getXWikiContext()));
    }

    /**
     * XWIKI-12398: No layout for login page in a closed wiki
     */
    @Test
    void skinResourcesAreAlwaysAllowed() throws XWikiException
    {
        // /skin/resources/icons/xwiki/noavatar.png
        XWikiDocument doc1 = new XWikiDocument(
            new DocumentReference("xwiki", Arrays.asList("resources", "icons", "xwiki"), "noavatar.png"));
        // /skin/skins/flamingo/style.css
        XWikiDocument doc2 =
            new XWikiDocument(new DocumentReference("xwiki", Arrays.asList("skins", "flamingo", "xwiki"), "style.css"));

        // Verify the results.
        assertTrue(this.xwiki.checkAccess("skin", doc1, this.oldcore.getXWikiContext()));
        assertTrue(this.xwiki.checkAccess("skin", doc2, this.oldcore.getXWikiContext()));

        // Make sure it is never called to validate the test's results.
        verify(this.oldcore.getMockRightService(), never()).checkAccess(any(), any(), any());
        verify(this.oldcore.getMockAuthService(), times(2)).checkAuth(this.oldcore.getXWikiContext());
    }

    @Test
    void checkActiveSuperadmin() throws Exception
    {
        int isUserActive =
            this.xwiki.checkActive(XWikiRightService.SUPERADMIN_USER_FULLNAME, this.oldcore.getXWikiContext());

        assertEquals(1, isUserActive);
    }

    /**
     * XWIKI-14300: Superadmin is locked out of subwikis with "AUTHENTICATION ACTIVE CHECK" enabled
     */
    @Test
    void checkActivePrefixedSuperadmin() throws Exception
    {
        // In a subwiki, the superadmin always logs in as a global user.
        int isUserActive = this.xwiki.checkActive("xwiki:" + XWikiRightService.SUPERADMIN_USER_FULLNAME,
            this.oldcore.getXWikiContext());

        assertEquals(1, isUserActive);
    }

    @Test
    void getLocalePreferenceWithContext()
    {
        this.oldcore.getXWikiContext().setRequest(new XWikiServletRequest(null));
        this.oldcore.getXWikiContext().setLocale(Locale.FRENCH);

        assertEquals(Locale.FRENCH, this.xwiki.getLocalePreference(this.oldcore.getXWikiContext()));
    }

    @Test
    void getLocalePreferenceDefaultNonMultilingual()
    {
        this.oldcore.getXWikiContext().setRequest(new XWikiServletRequest(null));

        assertEquals(Locale.ENGLISH, this.xwiki.getLocalePreference(this.oldcore.getXWikiContext()));
    }

    @Test
    void getLocalePreferenceWithParameterForcingUnset()
    {
        this.oldcore.getXWikiContext().setRequest(new XWikiServletRequest(null)
        {
            @Override
            public String getParameter(String s)
            {
                if ("language".equals(s)) {
                    return "fr_CA";
                }
                return null;
            }
        });

        // Set the wiki to multilingual mode.
        this.oldcore.getMockWikiConfigurationSource().setProperty("multilingual", "1");

        // we do not force supported languages
        this.oldcore.getMockXWikiCfg().setProperty("xwiki.language.forceSupported", "0");

        assertEquals(Locale.CANADA_FRENCH, this.xwiki.getLocalePreference(this.oldcore.getXWikiContext()));
    }

    @Test
    void getLocalePreferenceWithParameter()
    {
        this.oldcore.getXWikiContext().setRequest(new XWikiServletRequest(null)
        {
            @Override
            public String getParameter(String s)
            {
                if ("language".equals(s)) {
                    return "fr_CA";
                }
                return null;
            }
        });

        // Set the wiki to multilingual mode.
        this.oldcore.getMockWikiConfigurationSource().setProperty("multilingual", "1");

        // only the default language is supported by default
        assertEquals(Locale.ENGLISH, this.xwiki.getLocalePreference(this.oldcore.getXWikiContext()));
    }

    @Test
    void getLocalePreferenceWithParameterWithSupportedLanguages()
    {
        this.oldcore.getXWikiContext().setRequest(new XWikiServletRequest(null)
        {
            @Override
            public String getParameter(String s)
            {
                if ("language".equals(s)) {
                    return "fr_CA";
                }
                return null;
            }
        });

        // Set the wiki to multilingual mode.
        this.oldcore.getMockWikiConfigurationSource().setProperty("multilingual", "1");
        this.oldcore.getMockWikiConfigurationSource().setProperty("languages", "en, |fr_FR, |en_US, |fr_CA");

        // only the default language is supported by default
        assertEquals(Locale.CANADA_FRENCH, this.xwiki.getLocalePreference(this.oldcore.getXWikiContext()));
    }

    @Test
    void getLocalePreferenceWithParameterDefault()
    {
        this.oldcore.getXWikiContext().setRequest(new XWikiServletRequest(null)
        {
            @Override
            public String getParameter(String s)
            {
                if ("language".equals(s)) {
                    return "default";
                }
                return null;
            }
        });

        // Set the wiki to multilingual mode.
        this.oldcore.getMockWikiConfigurationSource().setProperty("multilingual", "1");

        assertEquals(Locale.ENGLISH, this.xwiki.getLocalePreference(this.oldcore.getXWikiContext()));
    }

    @Test
    void getLocalePreferenceWithCookieForcingUnset()
    {
        this.oldcore.getXWikiContext().setRequest(new XWikiServletRequest(null)
        {
            @Override
            public Cookie[] getCookies()
            {
                return new Cookie[] { new Cookie("language", "fr_CA") };
            }
        });

        // Set the wiki to multilingual mode.
        this.oldcore.getMockWikiConfigurationSource().setProperty("multilingual", "1");

        // we do not force supported languages
        this.oldcore.getMockXWikiCfg().setProperty("xwiki.language.forceSupported", "0");

        assertEquals(Locale.CANADA_FRENCH, this.xwiki.getLocalePreference(this.oldcore.getXWikiContext()));
    }

    @Test
    void getLocalePreferenceWithCookie()
    {
        this.oldcore.getXWikiContext().setRequest(new XWikiServletRequest(null)
        {
            @Override
            public Cookie[] getCookies()
            {
                return new Cookie[] { new Cookie("language", "fr_CA") };
            }
        });

        // Set the wiki to multilingual mode.
        this.oldcore.getMockWikiConfigurationSource().setProperty("multilingual", "1");

        // By default, we force only supported languages
        assertEquals(Locale.ENGLISH, this.xwiki.getLocalePreference(this.oldcore.getXWikiContext()));
    }

    @Test
    void getLocalePreferenceWithCookieDefaultNotSet()
    {
        this.oldcore.getXWikiContext().setRequest(new XWikiServletRequest(null)
        {
            @Override
            public Cookie[] getCookies()
            {
                return new Cookie[] { new Cookie("language", "fr_CA") };
            }
        });

        // Set the wiki to multilingual mode.
        this.oldcore.getMockWikiConfigurationSource().setProperty("multilingual", "1");

        // By default, we force only supported languages
        assertEquals(Locale.ENGLISH, this.xwiki.getLocalePreference(this.oldcore.getXWikiContext()));
    }

    @Test
    void getLocalePreferenceWithCookieDefaultSet()
    {
        this.oldcore.getXWikiContext().setRequest(new XWikiServletRequest(null)
        {
            @Override
            public Cookie[] getCookies()
            {
                return new Cookie[] { new Cookie("language", "fr_CA") };
            }
        });

        // Set the wiki to multilingual mode.
        this.oldcore.getMockWikiConfigurationSource().setProperty("multilingual", "1");
        this.oldcore.getMockWikiConfigurationSource().setProperty("languages", "en, |fr_FR, |en_US, |fr_CA");

        // By default, we force only supported languages
        assertEquals(Locale.CANADA_FRENCH, this.xwiki.getLocalePreference(this.oldcore.getXWikiContext()));
    }

    @Test
    void getLocalePreferenceWithNavigatorLanguagesForcingUnset()
    {
        this.oldcore.getXWikiContext().setRequest(new XWikiServletRequest(null)
        {
            @Override
            public Enumeration<Locale> getLocales()
            {
                return Collections.enumeration(List.of(
                    new Locale("*"),
                    new Locale("en", "US"),
                    new Locale("fr"),
                    new Locale("de")
                ));
            }
        });

        // we do not force supported languages
        this.oldcore.getMockXWikiCfg().setProperty("xwiki.language.forceSupported", "0");

        // Set the wiki to multilingual mode.
        this.oldcore.getMockWikiConfigurationSource().setProperty("multilingual", "1");

        assertEquals(Locale.US, this.xwiki.getLocalePreference(this.oldcore.getXWikiContext()));
    }

    @Test
    void getLocalePreferenceWithNavigatorLanguages()
    {
        this.oldcore.getXWikiContext().setRequest(new XWikiServletRequest(null)
        {
            @Override
            public Enumeration<Locale> getLocales()
            {
                return Collections.enumeration(List.of(
                    new Locale("*"),
                    new Locale("en", "US"),
                    new Locale("fr"),
                    new Locale("de")
                ));
            }
        });

        // Set the wiki to multilingual mode.
        this.oldcore.getMockWikiConfigurationSource().setProperty("multilingual", "1");

        // it's forced to "en" since it's the only supported language by default
        assertEquals(Locale.ENGLISH, this.xwiki.getLocalePreference(this.oldcore.getXWikiContext()));
    }

    @Test
    void getLocalePreferenceWithNavigatorLanguagesDefaultSet()
    {
        this.oldcore.getXWikiContext().setRequest(new XWikiServletRequest(null)
        {
            @Override
            public Enumeration<Locale> getLocales()
            {
                return Collections.enumeration(List.of(
                    new Locale("*"),
                    new Locale("en", "US"),
                    new Locale("fr"),
                    new Locale("de")
                ));
            }
        });

        // Set the wiki to multilingual mode.
        this.oldcore.getMockWikiConfigurationSource().setProperty("multilingual", "1");
        this.oldcore.getMockWikiConfigurationSource().setProperty("languages", "en, |fr_FR, |en_US, |fr_CA");

        // en_US is the first common language between supported and browser supported.
        assertEquals(Locale.US, this.xwiki.getLocalePreference(this.oldcore.getXWikiContext()));
    }

    @Test
    void getLocalePreferenceWithNavigatorLanguagesFallback()
    {
        this.oldcore.getXWikiContext().setRequest(new XWikiServletRequest(null));

        // Set the wiki to multilingual mode.
        this.oldcore.getMockWikiConfigurationSource().setProperty("multilingual", "1");

        assertEquals(Locale.ENGLISH, this.xwiki.getLocalePreference(this.oldcore.getXWikiContext()));
    }

    @Test
    void atomicRename() throws Exception
    {
        XWikiContext xWikiContext = this.oldcore.getXWikiContext();
        doAnswer(invocationOnMock -> {
            XWikiDocument sourceDoc = invocationOnMock.getArgument(0);
            DocumentReference targetReference = invocationOnMock.getArgument(1);
            XWikiContext context = invocationOnMock.getArgument(2);
            XWikiDocument targetDoc = sourceDoc.cloneRename(targetReference, context);
            this.xwiki.saveDocument(targetDoc, context);
            this.xwiki.deleteDocument(sourceDoc, true, context);
            return null;
        }).when(this.oldcore.getMockStore()).renameXWikiDoc(any(), any(), any());
        this.document.setContent("[[doc:pageinsamespace]]");
        this.document.setSyntax(Syntax.XWIKI_2_1);
        this.xwiki.saveDocument(this.document, xWikiContext);

        DocumentReference sourceReference = this.document.getDocumentReference();
        XWikiDocument frenchTranslation =
            new XWikiDocument(sourceReference, Locale.FRENCH);
        this.xwiki.saveDocument(frenchTranslation, xWikiContext);

        XWikiDocument germanTranslation =
            new XWikiDocument(sourceReference, Locale.GERMAN);
        this.xwiki.saveDocument(germanTranslation, xWikiContext);

        DocumentReference reference1 = new DocumentReference(DOCWIKI, DOCSPACE, "Page1");
        XWikiDocument doc1 = new XWikiDocument(reference1);
        doc1.setContent("[[doc:" + DOCWIKI + ":" + DOCSPACE + "." + DOCNAME + "]] [[someName>>doc:" + DOCSPACE + "."
            + DOCNAME + "]] [[doc:" + DOCNAME + "]]");
        doc1.setSyntax(Syntax.XWIKI_2_1);
        this.xwiki.saveDocument(doc1, xWikiContext);

        DocumentReference reference2 = new DocumentReference("newwikiname", DOCSPACE, "Page2");
        XWikiDocument doc2 = new XWikiDocument(reference2);
        doc2.setContent("[[doc:" + DOCWIKI + ":" + DOCSPACE + "." + DOCNAME + "]]");
        doc2.setSyntax(Syntax.XWIKI_2_1);
        this.xwiki.saveDocument(doc2, xWikiContext);

        DocumentReference reference3 = new DocumentReference("newwikiname", "newspace", "Page3");
        XWikiDocument doc3 = new XWikiDocument(reference3);
        doc3.setContent("[[doc:" + DOCWIKI + ":" + DOCSPACE + "." + DOCNAME + "]]");
        doc3.setSyntax(Syntax.XWIKI_2_1);
        this.xwiki.saveDocument(doc3, xWikiContext);

        // Test to make sure it also drags children along.
        DocumentReference reference4 = new DocumentReference(DOCWIKI, DOCSPACE, "Page4");
        XWikiDocument doc4 = new XWikiDocument(reference4);
        doc4.setParent(DOCSPACE + "." + DOCNAME);
        this.xwiki.saveDocument(doc4, xWikiContext);

        DocumentReference reference5 = new DocumentReference("newwikiname", "newspace", "Page5");
        XWikiDocument doc5 = new XWikiDocument(reference5);
        doc5.setParent(DOCWIKI + ":" + DOCSPACE + "." + DOCNAME);
        this.xwiki.saveDocument(doc5, xWikiContext);

        DocumentReference targetReference = new DocumentReference("newwikiname", "newspace", "newpage");
        this.xwiki.renameDocument(sourceReference,
            targetReference, true,
            Arrays.asList(reference1, reference2, reference3), Arrays.asList(reference4, reference5),
            xWikiContext);

        // ensure document and translations are renamed
        verify(this.oldcore.getMockStore()).renameXWikiDoc(this.document, targetReference, xWikiContext);
        verify(this.oldcore.getMockStore()).renameXWikiDoc(frenchTranslation,
            new DocumentReference(targetReference, Locale.FRENCH), xWikiContext);
        verify(this.oldcore.getMockStore()).renameXWikiDoc(germanTranslation,
            new DocumentReference(targetReference, Locale.GERMAN), xWikiContext);

        // Test links
        verify(this.referenceUpdater).update(targetReference, sourceReference, targetReference, Set.of());
        verify(this.referenceRenamer).renameReferences(doc1.getXDOM(), reference1, sourceReference,
            targetReference, false, Set.of());
        verify(this.referenceRenamer).renameReferences(doc2.getXDOM(), reference2, sourceReference,
            targetReference, false, Set.of());
        verify(this.referenceRenamer).renameReferences(doc3.getXDOM(), reference3, sourceReference,
            targetReference, false, Set.of());

        assertTrue(this.xwiki
            .getDocument(new DocumentReference(DOCWIKI, DOCSPACE, DOCNAME), this.oldcore.getXWikiContext()).isNew());

        // Test parents
        assertEquals("newwikiname:newspace.newpage",
            this.xwiki.getDocument(reference4, this.oldcore.getXWikiContext()).getParent());
        assertEquals(new DocumentReference("newwikiname", "newspace", "newpage"),
            this.xwiki.getDocument(reference5, this.oldcore.getXWikiContext()).getParentReference());
    }

    @Test
    void formatDate()
    {
        Date date = new Date(1718887539000L);
        String expectedResult = "2024/06/20 12:45";
        this.oldcore.getMockWikiConfigurationSource().setProperty("default_language", "fr_CA");
        this.oldcore.getMockXWikiCfg().setProperty("xwiki.timezone", "BRT");
        when(this.oldcore.getMockAllUserPropertiesResolver().resolve(CurrentUserReference.INSTANCE))
            .thenReturn(mock(UserProperties.class));
        assertEquals(expectedResult, this.xwiki.formatDate(date, null, this.oldcore.getXWikiContext()));

        String format = "EEEE dd MMMM YYYY HH:mm:ss";
        assertEquals("jeudi 20 juin 2024 12:45:39",
            this.xwiki.formatDate(date, format, this.oldcore.getXWikiContext()));
    }
}
