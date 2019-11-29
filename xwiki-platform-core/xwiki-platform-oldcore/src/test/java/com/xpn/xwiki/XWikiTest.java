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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.Cookie;

import org.apache.commons.collections4.IteratorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentDeletingEvent;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.annotation.AfterComponent;
import org.xwiki.test.annotation.AllComponents;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.XWikiCfgConfigurationSource;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.classes.PropertyClass;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link com.xpn.xwiki.XWiki}.
 *
 * @version $Id$
 */
@OldcoreTest(mockXWiki = false)
@AllComponents
public class XWikiTest
{
    @InjectMockitoOldcore
    MockitoOldcore oldcore;

    private XWikiDocument document;

    private XWiki xwiki;

    @AfterComponent
    public void afterComponent()
    {
        // Unregister XWikiCfgConfigurationSource so that it's mocked by MockitoOldcore
        this.oldcore.getMocker().unregisterComponent(ConfigurationSource.class, XWikiCfgConfigurationSource.ROLEHINT);
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
    }

    @Test
    public void testUserNotAddedByDefaultToXWikiAllGroupWhenThisGroupImplicit() throws Exception
    {
        // given
        this.oldcore.getMockXWikiCfg().setProperty("xwiki.authentication.group.allgroupimplicit", "1");

        XWikiGroupServiceImpl xWikiGroupService = new XWikiGroupServiceImpl();
        xwiki.setGroupService(xWikiGroupService);

        XWiki spyXWiki = Mockito.spy(xwiki);

        // when
        spyXWiki.setUserDefaultGroup("XWiki.user1", this.oldcore.getXWikiContext());

        // then
        Mockito.verify(spyXWiki, times(0)).addUserToGroup(anyString(), anyString(), any(XWikiContext.class));
    }

    @Test
    public void testUserAddedToXWikiAllGroupWhenItsSpecifiedByConfigurationRegardlessXWikiAllGroupIsImplicit()
        throws Exception
    {
        // given
        this.oldcore.getMockXWikiCfg().setProperty("xwiki.authentication.group.allgroupimplicit", "1");
        this.oldcore.getMockXWikiCfg().setProperty("xwiki.users.initialGroups", "XWiki.XWikiAllGroup");

        XWikiGroupServiceImpl xWikiGroupService = new XWikiGroupServiceImpl();
        xwiki.setGroupService(xWikiGroupService);

        // when
        this.xwiki.setUserDefaultGroup("XWiki.user1", this.oldcore.getXWikiContext());

        // then
        verify(this.xwiki, times(1)).addUserToGroup("XWiki.user1", "XWiki.XWikiAllGroup",
            this.oldcore.getXWikiContext());
    }

    @Test
    public void testAuthorAfterDocumentCopy() throws XWikiException
    {
        DocumentReference copyReference =
            new DocumentReference("Lyre", this.document.getDocumentReference().getLastSpaceReference());
        DocumentReference author = this.document.getAuthorReference();
        this.xwiki.copyDocument(this.document.getDocumentReference(), copyReference, this.oldcore.getXWikiContext());
        XWikiDocument copy = this.xwiki.getDocument(copyReference, this.oldcore.getXWikiContext());

        assertEquals(author, copy.getAuthorReference());
    }

    @Test
    public void testCreatorAfterDocumentCopy() throws XWikiException
    {
        DocumentReference copyReference =
            new DocumentReference("Sirius", this.document.getDocumentReference().getLastSpaceReference());
        DocumentReference creator = this.document.getCreatorReference();
        this.xwiki.copyDocument(this.document.getDocumentReference(), copyReference, this.oldcore.getXWikiContext());
        XWikiDocument copy = this.xwiki.getDocument(copyReference, this.oldcore.getXWikiContext());

        assertEquals(creator, copy.getCreatorReference());
    }

    @Test
    public void testCreationDateAfterDocumentCopy() throws XWikiException, InterruptedException
    {
        Date sourceCreationDate = this.document.getCreationDate();
        Thread.sleep(1000);
        DocumentReference copyReference = new DocumentReference(this.document.getDocumentReference().getName() + "Copy",
            this.document.getDocumentReference().getLastSpaceReference());
        this.xwiki.copyDocument(this.document.getDocumentReference(), copyReference, this.oldcore.getXWikiContext());
        XWikiDocument copy = this.xwiki.getDocument(copyReference, this.oldcore.getXWikiContext());

        assertTrue(copy.getCreationDate().equals(sourceCreationDate));
    }

    @Test
    public void testParseTemplateConsidersObjectField() throws XWikiException
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
    public void testParseTemplateConsidersAttachment() throws XWikiException
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
    public void testParseTemplateConsidersObjectFieldBeforeAttachment() throws Exception
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
    public void testClearNameWithoutStripDotsWithoutAscii()
    {
        assertEquals("ee{&.txt",
            this.xwiki.clearName("\u00E9\u00EA{&.txt", false, false, this.oldcore.getXWikiContext()));
    }

    @Test
    public void testClearNameWithoutStripDotsWithAscii()
    {
        assertEquals("ee.txt", this.xwiki.clearName("\u00E9\u00EA{&.txt", false, true, this.oldcore.getXWikiContext()));
    }

    @Test
    public void testClearNameWithStripDotsWithoutAscii()
    {
        assertEquals("ee{&txt",
            this.xwiki.clearName("\u00E9\u00EA{&.txt", true, false, this.oldcore.getXWikiContext()));
    }

    @Test
    public void testClearNameWithStripDotsWithAscii()
    {
        assertEquals("eetxt", this.xwiki.clearName("\u00E9\u00EA{&.txt", true, true, this.oldcore.getXWikiContext()));
    }

    /**
     * We only verify here that the saveDocument API calls the Observation component.
     */
    @Test
    public void testSaveDocumentSendsObservationEvents() throws Exception
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

    /**
     * We only verify here that the deleteDocument API calls the Observation component.
     */
    @Test
    public void testDeleteDocumentSendsObservationEvents() throws Exception
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
    public void testLanguageSelection() throws Exception
    {
        this.oldcore.getXWikiContext().setRequest(new XWikiServletRequest(null)
        {
            @SuppressWarnings("unchecked")
            @Override
            public Enumeration getLocales()
            {
                ArrayList<Locale> locales = new ArrayList<Locale>();
                locales.add(new Locale("*"));
                locales.add(new Locale("en", "US"));
                locales.add(new Locale("fr"));
                locales.add(new Locale("de"));
                return IteratorUtils.asEnumeration(locales.iterator());
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
    public void testLanguageSelectionWithSupportedLanguages() throws Exception
    {
        this.oldcore.getXWikiContext().setRequest(new XWikiServletRequest(null)
        {
            @SuppressWarnings("unchecked")
            @Override
            public Enumeration getLocales()
            {
                ArrayList<Locale> locales = new ArrayList<Locale>();
                locales.add(new Locale("*"));
                locales.add(new Locale("fr", "FR"));
                locales.add(new Locale("de"));
                return IteratorUtils.asEnumeration(locales.iterator());
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
    public void testIsMultilingualDefaultFalse() throws Exception
    {
        assertFalse(this.xwiki.isMultiLingual(this.oldcore.getXWikiContext()));
    }

    @Test
    public void testGetCurrentContentSyntaxId()
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
    public void testValidationKeyStorage() throws Exception
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
    public void testGetPrefsClass() throws Exception
    {
        when(this.oldcore.getMockStore().getLimitSize(any(), any(), any())).thenReturn(255);

        XWikiDocument prefsDoc = new XWikiDocument(new DocumentReference("xwiki", "XWiki", "XWikiPreferences"));
        final Map<DocumentReference, XWikiDocument> documents = new HashMap<DocumentReference, XWikiDocument>();
        documents.put(prefsDoc.getDocumentReference(), prefsDoc);

        when(this.oldcore.getMockStore().loadXWikiDoc(notNull(), same(this.oldcore.getXWikiContext())))
            .then(new Answer<XWikiDocument>()
            {

                @Override
                public XWikiDocument answer(InvocationOnMock invocation) throws Throwable
                {
                    XWikiDocument document = (XWikiDocument) invocation.getArgument(0);
                    if (!documents.containsKey(document.getDocumentReference())) {
                        documents.put(document.getDocumentReference(), document);
                    } else {
                        document = documents.get(document.getDocumentReference());
                    }
                    return document;
                }
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
    public void testSkinResourcesAreAlwaysAllowed() throws XWikiException
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
    public void testCheckActiveSuperadmin() throws Exception
    {
        int isUserActive =
            this.xwiki.checkActive(XWikiRightService.SUPERADMIN_USER_FULLNAME, this.oldcore.getXWikiContext());

        assertEquals(1, isUserActive);
    }

    /**
     * XWIKI-14300: Superadmin is locked out of subwikis with "AUTHENTICATION ACTIVE CHECK" enabled
     */
    @Test
    public void testCheckActivePrefixedSuperadmin() throws Exception
    {
        // In a subwiki, the superadmin always logs in as a global user.
        int isUserActive = this.xwiki.checkActive("xwiki:" + XWikiRightService.SUPERADMIN_USER_FULLNAME,
            this.oldcore.getXWikiContext());

        assertEquals(1, isUserActive);
    }

    @Test
    public void testGetLocalePreferenceWithContext() throws Exception
    {
        this.oldcore.getXWikiContext().setRequest(new XWikiServletRequest(null));
        this.oldcore.getXWikiContext().setLocale(Locale.FRENCH);

        assertEquals(Locale.FRENCH, this.xwiki.getLocalePreference(this.oldcore.getXWikiContext()));
    }

    @Test
    public void testGetLocalePreferenceDefaultNonMultilingual() throws Exception
    {
        this.oldcore.getXWikiContext().setRequest(new XWikiServletRequest(null));

        assertEquals(Locale.ENGLISH, this.xwiki.getLocalePreference(this.oldcore.getXWikiContext()));
    }

    @Test
    public void testGetLocalePreferenceWithParameterForcingUnset() throws Exception
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
    public void testGetLocalePreferenceWithParameter() throws Exception
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
    public void testGetLocalePreferenceWithParameterWithSupportedLanguages() throws Exception
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
    public void testGetLocalePreferenceWithParameterDefault() throws Exception
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
    public void testGetLocalePreferenceWithCookieForcingUnset() throws Exception
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
    public void testGetLocalePreferenceWithCookie() throws Exception
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

        // By default we force only supported languages
        assertEquals(Locale.ENGLISH, this.xwiki.getLocalePreference(this.oldcore.getXWikiContext()));
    }

    @Test
    public void testGetLocalePreferenceWithCookieDefaultNotSet() throws Exception
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

        // By default we force only supported languages
        assertEquals(Locale.ENGLISH, this.xwiki.getLocalePreference(this.oldcore.getXWikiContext()));
    }

    @Test
    public void testGetLocalePreferenceWithCookieDefaultSet() throws Exception
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

        // By default we force only supported languages
        assertEquals(Locale.CANADA_FRENCH, this.xwiki.getLocalePreference(this.oldcore.getXWikiContext()));
    }

    @Test
    public void testGetLocalePreferenceWithNavigatorLanguagesForcingUnset() throws Exception
    {
        this.oldcore.getXWikiContext().setRequest(new XWikiServletRequest(null)
        {
            @Override
            public Enumeration<Locale> getLocales()
            {
                ArrayList<Locale> locales = new ArrayList<>();
                locales.add(new Locale("*"));
                locales.add(new Locale("en", "US"));
                locales.add(new Locale("fr"));
                locales.add(new Locale("de"));
                return IteratorUtils.asEnumeration(locales.iterator());
            }
        });

        // we do not force supported languages
        this.oldcore.getMockXWikiCfg().setProperty("xwiki.language.forceSupported", "0");

        // Set the wiki to multilingual mode.
        this.oldcore.getMockWikiConfigurationSource().setProperty("multilingual", "1");

        assertEquals(Locale.US, this.xwiki.getLocalePreference(this.oldcore.getXWikiContext()));
    }

    @Test
    public void testGetLocalePreferenceWithNavigatorLanguages() throws Exception
    {
        this.oldcore.getXWikiContext().setRequest(new XWikiServletRequest(null)
        {
            @Override
            public Enumeration<Locale> getLocales()
            {
                ArrayList<Locale> locales = new ArrayList<>();
                locales.add(new Locale("*"));
                locales.add(new Locale("en", "US"));
                locales.add(new Locale("fr"));
                locales.add(new Locale("de"));
                return IteratorUtils.asEnumeration(locales.iterator());
            }
        });

        // Set the wiki to multilingual mode.
        this.oldcore.getMockWikiConfigurationSource().setProperty("multilingual", "1");

        // it's forced to "en" since it's the only supported language by default
        assertEquals(Locale.ENGLISH, this.xwiki.getLocalePreference(this.oldcore.getXWikiContext()));
    }

    @Test
    public void testGetLocalePreferenceWithNavigatorLanguagesDefaultSet() throws Exception
    {
        this.oldcore.getXWikiContext().setRequest(new XWikiServletRequest(null)
        {
            @Override
            public Enumeration<Locale> getLocales()
            {
                ArrayList<Locale> locales = new ArrayList<>();
                locales.add(new Locale("*"));
                locales.add(new Locale("en", "US"));
                locales.add(new Locale("fr"));
                locales.add(new Locale("de"));
                return IteratorUtils.asEnumeration(locales.iterator());
            }
        });

        // Set the wiki to multilingual mode.
        this.oldcore.getMockWikiConfigurationSource().setProperty("multilingual", "1");
        this.oldcore.getMockWikiConfigurationSource().setProperty("languages", "en, |fr_FR, |en_US, |fr_CA");

        // en_US is the first common language between supported and browser supported.
        assertEquals(Locale.US, this.xwiki.getLocalePreference(this.oldcore.getXWikiContext()));
    }

    @Test
    public void testGetLocalePreferenceWithNavigatorLanguagesFallback() throws Exception
    {
        this.oldcore.getXWikiContext().setRequest(new XWikiServletRequest(null));

        // Set the wiki to multilingual mode.
        this.oldcore.getMockWikiConfigurationSource().setProperty("multilingual", "1");

        assertEquals(Locale.ENGLISH, this.xwiki.getLocalePreference(this.oldcore.getXWikiContext()));
    }
}
