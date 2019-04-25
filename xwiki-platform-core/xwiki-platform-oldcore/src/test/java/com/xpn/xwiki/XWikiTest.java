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
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.Cookie;

import org.apache.commons.collections4.IteratorUtils;
import org.jmock.Mock;
import org.jmock.core.Invocation;
import org.jmock.core.stub.CustomStub;
import org.junit.Assert;
import org.mockito.Mockito;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentDeletingEvent;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.localization.LocalizationContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiHibernateVersioningStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiVersioningStoreInterface;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;
import com.xpn.xwiki.user.api.XWikiAuthService;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.user.impl.xwiki.XWikiGroupServiceImpl;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiServletRequest;
import com.xpn.xwiki.web.XWikiServletRequestStub;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;

/**
 * Unit tests for {@link com.xpn.xwiki.XWiki}.
 *
 * @version $Id$
 */
public class XWikiTest extends AbstractBridgedXWikiComponentTestCase
{
    private XWikiDocument document;

    private XWiki xwiki;

    private Mock mockXWikiStore;

    private Mock mockXWikiVersioningStore;

    private Mock mockWikiDescriptorManager;

    private Map<String, XWikiDocument> docs = new HashMap<String, XWikiDocument>();

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        this.document = new XWikiDocument(new DocumentReference("Wiki", "MilkyWay", "Fidis"));
        getContext().setRequest(new XWikiServletRequestStub());
        getContext().setURL(new URL("http://localhost:8080/xwiki/bin/view/MilkyWay/Fidis"));

        Mock mockLocalizationContext = registerMockComponent(LocalizationContext.class);
        mockLocalizationContext.stubs().method("getCurrentLocale").will(returnValue(Locale.ROOT));

        this.mockWikiDescriptorManager = registerMockComponent(WikiDescriptorManager.class);
        this.mockWikiDescriptorManager.stubs().method("getCurrentWikiId").will(
            new CustomStub("Implements WikiDescriptorManager.getCurrentWikiId")
            {
                @Override
                public Object invoke(Invocation invocation) throws Throwable
                {
                    return getContext().getWikiId();
                }
            });

        this.xwiki = new XWiki(new XWikiConfig(), getContext())
        {
            // Avoid all the error at XWiki initialization
            @Override
            public String getXWikiPreference(String prefname, String defaultValue, XWikiContext context)
            {
                if (prefname.equals("plugins") || prefname.startsWith("macros_")) {
                    return defaultValue;
                } else {
                    return super.getXWikiPreference(prefname, defaultValue, context);
                }
            }
        };
        getContext().setWiki(this.xwiki);

        // Ensure that no Velocity Templates are going to be used when executing Velocity since otherwise
        // the Velocity init would fail (since by default the macros.vm templates wouldn't be found as we're
        // not providing it in our unit test resources).
        getConfigurationSource().setProperty("xwiki.render.velocity.macrolist", "");

        this.mockXWikiStore =
            mock(XWikiHibernateStore.class, new Class[] {XWiki.class, XWikiContext.class}, new Object[] {this.xwiki,
            getContext()});
        this.mockXWikiStore.stubs().method("loadXWikiDoc").will(
            new CustomStub("Implements XWikiStoreInterface.loadXWikiDoc")
            {
                @Override
                public Object invoke(Invocation invocation) throws Throwable
                {
                    XWikiDocument shallowDoc = (XWikiDocument) invocation.parameterValues.get(0);
                    if (XWikiTest.this.docs.containsKey(shallowDoc.getName())) {
                        return XWikiTest.this.docs.get(shallowDoc.getName());
                    } else {
                        return shallowDoc;
                    }
                }
            });
        this.mockXWikiStore.stubs().method("saveXWikiDoc").will(
            new CustomStub("Implements XWikiStoreInterface.saveXWikiDoc")
            {
                @Override
                public Object invoke(Invocation invocation) throws Throwable
                {
                    XWikiDocument document = (XWikiDocument) invocation.parameterValues.get(0);
                    document.setNew(false);
                    document.setStore((XWikiStoreInterface) XWikiTest.this.mockXWikiStore.proxy());
                    XWikiTest.this.docs.put(document.getName(), document);
                    return null;
                }
            });
        this.mockXWikiStore.stubs().method("deleteXWikiDoc").will(
            new CustomStub("Implements XWikiStoreInterface.deleteXWikiDoc")
            {
                @Override
                public Object invoke(Invocation invocation) throws Throwable
                {
                    XWikiDocument document = (XWikiDocument) invocation.parameterValues.get(0);
                    XWikiTest.this.docs.remove(document.getName());
                    return null;
                }
            });
        this.mockXWikiStore.stubs().method("executeRead").will(
            new CustomStub("Implements XWikiStoreInterface.executeRead")
            {
                @Override
                public Object invoke(Invocation invocation) throws Throwable
                {
                    return Collections.emptyList();
                }
            });
        this.mockXWikiStore.stubs().method("getTranslationList").will(returnValue(Collections.EMPTY_LIST));
        this.mockXWikiStore.stubs().method("exists").will(returnValue(true));
        this.mockXWikiStore.stubs().method("getLimitSize").will(returnValue(255));

        this.mockXWikiVersioningStore =
            mock(XWikiHibernateVersioningStore.class, new Class[] {XWiki.class, XWikiContext.class}, new Object[] {
            this.xwiki, getContext()});
        this.mockXWikiVersioningStore.stubs().method("getXWikiDocumentArchive").will(returnValue(null));
        this.mockXWikiVersioningStore.stubs().method("resetRCSArchive").will(returnValue(null));

        this.xwiki.setStore((XWikiStoreInterface) this.mockXWikiStore.proxy());
        this.xwiki.setVersioningStore((XWikiVersioningStoreInterface) this.mockXWikiVersioningStore.proxy());
        this.xwiki.saveDocument(this.document, getContext());

        this.document.setCreator("Condor");
        this.document.setAuthor("Albatross");

        this.xwiki.saveDocument(this.document, getContext());
    }

    public void testUserNotAddedByDefaultToXWikiAllGroupWhenThisGroupImplicit() throws Exception
    {
        //given
        getConfigurationSource().setProperty("xwiki.authentication.group.allgroupimplicit", "1");

        XWikiGroupServiceImpl xWikiGroupService = new XWikiGroupServiceImpl();
        xwiki.setGroupService(xWikiGroupService);

        XWiki spyXWiki = Mockito.spy(xwiki);

        //when
        spyXWiki.setUserDefaultGroup("XWiki.user1", getContext());

        //then
        Mockito.verify(spyXWiki, times(0)).addUserToGroup(anyString(), anyString(), any(XWikiContext.class));
    }

    public void testUserAddedToXWikiAllGroupWhenItsSpecifiedByConfigurationRegardlessXWikiAllGroupIsImplicit() throws Exception
    {
        //given
        getConfigurationSource().setProperty("xwiki.authentication.group.allgroupimplicit", "1");
        getConfigurationSource().setProperty("xwiki.users.initialGroups", "XWiki.XWikiAllGroup");

        XWikiGroupServiceImpl xWikiGroupService = new XWikiGroupServiceImpl();
        xwiki.setGroupService(xWikiGroupService);

        XWiki spyXWiki = Mockito.spy(xwiki);

        //when
        spyXWiki.setUserDefaultGroup("XWiki.user1", getContext());

        //then
        Mockito.verify(spyXWiki, times(1)).addUserToGroup("XWiki.user1", "XWiki.XWikiAllGroup", getContext());
    }

    public void testAuthorAfterDocumentCopy() throws XWikiException
    {
        DocumentReference copyReference =
            new DocumentReference("Lyre", this.document.getDocumentReference().getLastSpaceReference());
        DocumentReference author = this.document.getAuthorReference();
        this.xwiki.copyDocument(this.document.getDocumentReference(), copyReference, getContext());
        XWikiDocument copy = this.xwiki.getDocument(copyReference, getContext());

        assertEquals(author, copy.getAuthorReference());
    }

    public void testCreatorAfterDocumentCopy() throws XWikiException
    {
        DocumentReference copyReference =
            new DocumentReference("Sirius", this.document.getDocumentReference().getLastSpaceReference());
        DocumentReference creator = this.document.getCreatorReference();
        this.xwiki.copyDocument(this.document.getDocumentReference(), copyReference, getContext());
        XWikiDocument copy = this.xwiki.getDocument(copyReference, getContext());

        assertEquals(creator, copy.getCreatorReference());
    }

    public void testCreationDateAfterDocumentCopy() throws XWikiException, InterruptedException
    {
        Date sourceCreationDate = this.document.getCreationDate();
        Thread.sleep(1000);
        DocumentReference copyReference =
            new DocumentReference(this.document.getDocumentReference().getName() + "Copy", this.document
                .getDocumentReference().getLastSpaceReference());
        this.xwiki.copyDocument(this.document.getDocumentReference(), copyReference, getContext());
        XWikiDocument copy = this.xwiki.getDocument(copyReference, getContext());

        assertTrue(copy.getCreationDate().equals(sourceCreationDate));
    }

    public void testParseTemplateConsidersObjectField() throws XWikiException
    {
        DocumentReference skinReference = new DocumentReference("xwiki", "XWiki", "XWikiSkins");
        XWikiDocument skinClass = new XWikiDocument(skinReference);
        skinClass.getXClass().addTextAreaField("template.vm", "template", 80, 20);
        this.xwiki.saveDocument(skinClass, getContext());

        DocumentReference mySkinReference = new DocumentReference("xwiki", "XWiki", "Skin");
        XWikiDocument skinDocument = new XWikiDocument(mySkinReference);
        BaseObject obj = skinDocument.newXObject(skinReference, getContext());
        obj.setLargeStringValue("template.vm", "parsing a field");
        this.xwiki.saveDocument(skinDocument, getContext());

        getContext().put("skin", "XWiki.Skin");
        assertEquals("XWiki.Skin", this.xwiki.getSkin(getContext()));
        assertFalse(this.xwiki.getDocument(mySkinReference, getContext()).isNew());
        assertEquals(skinDocument, this.xwiki.getDocument(mySkinReference, getContext()));
        assertEquals("parsing a field", this.xwiki.parseTemplate("template.vm", getContext()));
    }

    /**
     * See XWIKI-2096
     */
    public void testParseTemplateConsidersAttachment() throws XWikiException
    {
        XWikiDocument skin = new XWikiDocument(new DocumentReference("Wiki", "XWiki", "Skin"));
        XWikiAttachment attachment = new XWikiAttachment();
        skin.getAttachmentList().add(attachment);
        attachment.setContent("parsing an attachment".getBytes());
        attachment.setFilename("template.vm");
        attachment.setDoc(skin);
        this.xwiki.saveDocument(skin, getContext());
        getContext().put("skin", "XWiki.Skin");

        assertEquals("XWiki.Skin", this.xwiki.getSkin(getContext()));
        assertFalse(this.xwiki.getDocument("XWiki.Skin", getContext()).isNew());
        assertEquals(skin, this.xwiki.getDocument("XWiki.Skin", getContext()));
        assertEquals("parsing an attachment", this.xwiki.parseTemplate("template.vm", getContext()));
    }

    /**
     * See XWIKI-2098
     */
    public void testParseTemplateConsidersObjectFieldBeforeAttachment() throws Exception
    {
        DocumentReference skinReference = new DocumentReference("xwiki", "XWiki", "XWikiSkins");
        XWikiDocument skinClass = new XWikiDocument(skinReference);
        skinClass.getXClass().addTextAreaField("template.vm", "template", 80, 20);
        this.xwiki.saveDocument(skinClass, getContext());

        DocumentReference mySkinReference = new DocumentReference("xwiki", "XWiki", "Skin");
        XWikiDocument skinDocument = new XWikiDocument(mySkinReference);
        BaseObject obj = skinDocument.newXObject(skinReference, getContext());
        obj.setLargeStringValue("template.vm", "parsing a field");
        XWikiAttachment attachment = new XWikiAttachment();
        skinDocument.getAttachmentList().add(attachment);
        attachment.setContent(new ByteArrayInputStream("parsing an attachment".getBytes()));
        attachment.setFilename("template.vm");
        attachment.setDoc(skinDocument);
        this.xwiki.saveDocument(skinDocument, getContext());
        getContext().put("skin", "XWiki.Skin");
        assertEquals("XWiki.Skin", this.xwiki.getSkin(getContext()));
        assertFalse(this.xwiki.getDocument(mySkinReference, getContext()).isNew());
        assertEquals(skinDocument, this.xwiki.getDocument(mySkinReference, getContext()));
        assertEquals("parsing a field", this.xwiki.parseTemplate("template.vm", getContext()));
    }

    public void testClearNameWithoutStripDotsWithoutAscii()
    {
        assertEquals("ee{&.txt", this.xwiki.clearName("\u00E9\u00EA{&.txt", false, false, getContext()));
    }

    public void testClearNameWithoutStripDotsWithAscii()
    {
        assertEquals("ee.txt", this.xwiki.clearName("\u00E9\u00EA{&.txt", false, true, getContext()));
    }

    public void testClearNameWithStripDotsWithoutAscii()
    {
        assertEquals("ee{&txt", this.xwiki.clearName("\u00E9\u00EA{&.txt", true, false, getContext()));
    }

    public void testClearNameWithStripDotsWithAscii()
    {
        assertEquals("eetxt", this.xwiki.clearName("\u00E9\u00EA{&.txt", true, true, getContext()));
    }

    /**
     * We only verify here that the saveDocument API calls the Observation component.
     */
    public void testSaveDocumentSendsObservationEvents() throws Exception
    {
        Mock mockListener = mock(EventListener.class);
        mockListener.stubs().method("getName").will(returnValue("testlistener"));
        DocumentReference ref = new DocumentReference("xwikitest", "Some", "Document");
        mockListener.expects(once()).method("getEvents")
            .will(returnValue(Arrays.asList(new DocumentCreatedEvent(ref), new DocumentCreatingEvent(ref))));

        ObservationManager om = getComponentManager().getInstance(ObservationManager.class);
        om.addListener((EventListener) mockListener.proxy());

        XWikiDocument document = new XWikiDocument(new DocumentReference("xwikitest", "Some", "Document"));
        document.setContent("the content");

        // Ensure that the onEvent method has been called before and after the save
        mockListener.expects(once()).method("onEvent").with(isA(DocumentCreatingEvent.class), same(document),
            isA(XWikiContext.class));
        mockListener.expects(once()).method("onEvent").with(isA(DocumentCreatedEvent.class), same(document),
            isA(XWikiContext.class));

        this.xwiki.saveDocument(document, getContext());
    }

    /**
     * We only verify here that the deleteDocument API calls the Observation component.
     */
    public void testDeleteDocumentSendsObservationEvents() throws Exception
    {
        Mock mockListener = mock(EventListener.class);
        mockListener.stubs().method("getName").will(returnValue("testlistener"));
        DocumentReference ref = new DocumentReference("xwikitest", "Another", "Document");
        mockListener.expects(once()).method("getEvents")
            .will(returnValue(Arrays.asList(new DocumentDeletedEvent(ref), new DocumentDeletingEvent(ref))));

        ObservationManager om = getComponentManager().getInstance(ObservationManager.class);
        om.addListener((EventListener) mockListener.proxy());

        XWikiDocument document = new XWikiDocument(new DocumentReference("xwikitest", "Another", "Document"));
        document.setContent("the content");

        // Not expectation on mock Listener since we're not subscribed to Document save events

        this.xwiki.saveDocument(document, getContext());

        // Ensure that the onEvent method has been called before and after the deletion
        mockListener.expects(once()).method("onEvent")
        .with(isA(DocumentDeletingEvent.class), isA(XWikiDocument.class), isA(XWikiContext.class));
        mockListener.expects(once()).method("onEvent")
            .with(isA(DocumentDeletedEvent.class), isA(XWikiDocument.class), isA(XWikiContext.class));

        this.xwiki.deleteDocument(document, false, getContext());
    }

    public void testLanguageSelection() throws Exception
    {
        getContext().setRequest(new XWikiServletRequest(null)
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
        getConfigurationSource().setProperty("multilingual", "1");

        assertEquals("en", this.xwiki.getLanguagePreference(getContext()));
    }

    public void testLanguageSelectionWithSupportedLanguages() throws Exception
    {
        getContext().setRequest(new XWikiServletRequest(null)
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
        getConfigurationSource().setProperty("multilingual", "1");
        getConfigurationSource().setProperty("languages", "en, |fr_FR, |en_US, |fr_CA");

        assertEquals("fr_FR", this.xwiki.getLanguagePreference(getContext()));
    }

    /**
     * XWIKI-8469: Bad default of 1 in XWiki.isMultilingual instead of 0 (when no XWikiPreferences object exists)
     */
    public void testIsMultilingualDefaultFalse() throws Exception
    {
        assertFalse(this.xwiki.isMultiLingual(getContext()));
    }

    public void testGetCurrentContentSyntaxId()
    {
        XWikiDocument doc1 = new XWikiDocument();
        doc1.setSyntax(Syntax.CREOLE_1_0);
        XWikiDocument doc2 = new XWikiDocument();
        doc2.setSyntax(Syntax.PLAIN_1_0);

        assertNull(this.xwiki.getCurrentContentSyntaxId(null, getContext()));
        assertEquals("syntaxId", this.xwiki.getCurrentContentSyntaxId("syntaxId", getContext()));

        getContext().setDoc(doc1);

        assertEquals(Syntax.CREOLE_1_0.toIdString(), this.xwiki.getCurrentContentSyntaxId(null, getContext()));

        getContext().put("sdoc", doc2);

        assertEquals(Syntax.PLAIN_1_0.toIdString(), this.xwiki.getCurrentContentSyntaxId(null, getContext()));
    }

    /**
     * Check that the user validation feature works when the validation key is stored both as plain text and as a hashed
     * field.
     *
     * @throws Exception when any exception occurs inside XWiki
     */
    public void testValidationKeyStorage() throws Exception
    {
        XWikiContext context = getContext();
        context.setLanguage("en");

        // Prepare the request
        Mock request = mock(XWikiRequest.class);
        request.stubs().method("getParameter").with(eq("xwikiname")).will(returnValue("TestUser"));
        request.stubs().method("getParameter").with(eq("validkey")).will(returnValue("plaintextkey"));
        context.setRequest((XWikiRequest) request.proxy());

        // Prepare the user profile
        XWikiDocument testUser = new XWikiDocument(new DocumentReference("Wiki", "XWiki", "TestUser"));
        BaseObject userObject = (BaseObject) this.xwiki.getUserClass(context).newObject(context);
        testUser.addObject("XWiki.XWikiUsers", userObject);
        this.xwiki.saveDocument(testUser, context);

        // Check with a correct plaintext key
        BaseProperty validationKey = new StringProperty();
        validationKey.setValue("plaintextkey");
        userObject.safeput("validkey", validationKey);

        assertEquals(0, this.xwiki.validateUser(false, getContext()));

        // Check with an incorrect plaintext key
        validationKey.setValue("wrong key");

        assertEquals(-1, this.xwiki.validateUser(false, getContext()));

        // Check with a correct hashed key
        validationKey = ((PropertyClass) this.xwiki.getUserClass(context).get("validkey")).fromString("plaintextkey");
        assertTrue(validationKey.getValue().toString().startsWith("hash:"));
        userObject.safeput("validkey", validationKey);

        assertEquals(0, this.xwiki.validateUser(false, getContext()));

        // Check with an incorrect hashed key
        validationKey = ((PropertyClass) this.xwiki.getUserClass(context).get("validkey")).fromString("wrong key");
        assertTrue(validationKey.getValue().toString().startsWith("hash:"));
        userObject.safeput("validkey", validationKey);

        assertEquals(-1, this.xwiki.validateUser(false, getContext()));
    }

    /**
     * Tests that XWiki.XWikiPreferences page is not saved each time XWiki is initialized.
     *
     * @throws Exception when any exception occurs inside XWiki
     */
    public void testGetPrefsClass() throws Exception
    {
        Mock mockStore = registerMockComponent(XWikiStoreInterface.class);
        this.xwiki.setStore((XWikiStoreInterface) mockStore.proxy());
        mockStore.stubs().method("getLimitSize").will(returnValue(255));

        XWikiDocument prefsDoc = new XWikiDocument(new DocumentReference("xwiki", "XWiki", "XWikiPreferences"));
        final Map<DocumentReference, XWikiDocument> documents = new HashMap<DocumentReference, XWikiDocument>();
        documents.put(prefsDoc.getDocumentReference(), prefsDoc);

        mockStore.expects(atLeastOnce()).method("loadXWikiDoc").with(NOT_NULL, same(getContext()))
            .will(new CustomStub("Implements XWikiStoreInterface.loadXWikiDoc")
            {
                @Override
                public Object invoke(Invocation invocation) throws Throwable
                {
                    XWikiDocument document = (XWikiDocument) invocation.parameterValues.get(0);
                    if (!documents.containsKey(document.getDocumentReference())) {
                        documents.put(document.getDocumentReference(), document);
                    } else {
                        document = documents.get(document.getDocumentReference());
                    }
                    return document;
                }
            });
        mockStore.expects(once()).method("saveXWikiDoc").with(same(prefsDoc), same(getContext()));

        this.xwiki.getPrefsClass(getContext());
        this.xwiki.getPrefsClass(getContext());
    }

    /**
     * XWIKI-12398: No layout for login page in a closed wiki
     */
    public void testSkinResourcesAreAlwaysAllowed() throws XWikiException
    {
        // /skin/resources/icons/xwiki/noavatar.png
        XWikiDocument doc1 =
            new XWikiDocument(new DocumentReference("xwiki", Arrays.asList("resources", "icons", "xwiki"),
                "noavatar.png"));
        // /skin/skins/flamingo/style.css
        XWikiDocument doc2 =
            new XWikiDocument(new DocumentReference("xwiki", Arrays.asList("skins", "flamingo", "xwiki"), "style.css"));

        // Register a mock authService just so that we limit the test to a minimum.
        Mock mockAuthService = mock(XWikiAuthService.class);
        mockAuthService.expects(exactly(2)).method("checkAuth").with(same(getContext())).will(returnValue(null));
        this.xwiki.setAuthService((XWikiAuthService) mockAuthService.proxy());

        // Register a mock rights service and make sure it is never called to validate the test's results.
        Mock mockRightService = mock(XWikiRightService.class);
        mockRightService.expects(
            never("Skin action resources inside the 'skins' and 'resources' folders"
                + " should never be checked for rights.")).method("checkAccess");
        this.xwiki.setRightService((XWikiRightService) mockRightService.proxy());

        // Verify the results.
        Assert.assertTrue(this.xwiki.checkAccess("skin", doc1, getContext()));
        Assert.assertTrue(this.xwiki.checkAccess("skin", doc2, getContext()));
    }

    public void testCheckActiveSuperadmin() throws Exception
    {
        // Make sure the check for superadmin stops before looking at the configuration.
        Mock mockConfiguration = registerMockComponent(ConfigurationSource.class, "wiki");
        mockConfiguration.expects(never()).method("getProperty").with(eq("auth_active_check"), eq(String.class));

        int isUserActive = this.xwiki.checkActive(XWikiRightService.SUPERADMIN_USER_FULLNAME, this.getContext());

        Assert.assertEquals(1, isUserActive);
    }

    /**
     * XWIKI-14300: Superadmin is locked out of subwikis with "AUTHENTICATION ACTIVE CHECK" enabled
     */
    public void testCheckActivePrefixedSuperadmin() throws Exception
    {
        // Make sure the check for superadmin stops before looking at the configuration.
        Mock mockConfiguration = registerMockComponent(ConfigurationSource.class, "wiki");
        mockConfiguration.expects(never()).method("getProperty").with(eq("auth_active_check"), eq(String.class));

        // In a subwiki, the superadmin always logs in as a global user.
        int isUserActive =
            this.xwiki.checkActive("xwiki:" + XWikiRightService.SUPERADMIN_USER_FULLNAME, this.getContext());

        Assert.assertEquals(1, isUserActive);
    }

    public void testGetLocalePreferenceWithContext() throws Exception
    {
        getContext().setRequest(new XWikiServletRequest(null));
        getContext().setLocale(Locale.FRENCH);

        assertEquals(Locale.FRENCH, this.xwiki.getLocalePreference(getContext()));
    }

    public void testGetLocalePreferenceDefaultNonMultilingual() throws Exception
    {
        getContext().setRequest(new XWikiServletRequest(null));

        assertEquals(Locale.ENGLISH, this.xwiki.getLocalePreference(getContext()));
    }

    public void testGetLocalePreferenceWithParameterForcingUnset() throws Exception
    {
        getContext().setRequest(new XWikiServletRequest(null)
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
        getConfigurationSource().setProperty("multilingual", "1");

        // we do not force supported languages
        getConfigurationSource().setProperty("xwiki.language.forceSupported", "0");

        assertEquals(Locale.CANADA_FRENCH, this.xwiki.getLocalePreference(getContext()));
    }

    public void testGetLocalePreferenceWithParameter() throws Exception
    {
        getContext().setRequest(new XWikiServletRequest(null)
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
        getConfigurationSource().setProperty("multilingual", "1");

        // only the default language is supported by default
        assertEquals(Locale.ENGLISH, this.xwiki.getLocalePreference(getContext()));
    }

    public void testGetLocalePreferenceWithParameterWithSupportedLanguages() throws Exception
    {
        getContext().setRequest(new XWikiServletRequest(null)
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
        getConfigurationSource().setProperty("multilingual", "1");
        getConfigurationSource().setProperty("languages", "en, |fr_FR, |en_US, |fr_CA");

        // only the default language is supported by default
        assertEquals(Locale.CANADA_FRENCH, this.xwiki.getLocalePreference(getContext()));
    }

    public void testGetLocalePreferenceWithParameterDefault() throws Exception
    {
        getContext().setRequest(new XWikiServletRequest(null)
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
        getConfigurationSource().setProperty("multilingual", "1");

        assertEquals(Locale.ENGLISH, this.xwiki.getLocalePreference(getContext()));
    }

    public void testGetLocalePreferenceWithCookieForcingUnset() throws Exception
    {
        getContext().setRequest(new XWikiServletRequest(null)
        {
            @Override
            public Cookie[] getCookies()
            {
                return new Cookie[]{ new Cookie("language", "fr_CA") };
            }
        });

        // Set the wiki to multilingual mode.
        getConfigurationSource().setProperty("multilingual", "1");

        // we do not force supported languages
        getConfigurationSource().setProperty("xwiki.language.forceSupported", "0");

        assertEquals(Locale.CANADA_FRENCH, this.xwiki.getLocalePreference(getContext()));
    }

    public void testGetLocalePreferenceWithCookie() throws Exception
    {
        getContext().setRequest(new XWikiServletRequest(null)
        {
            @Override
            public Cookie[] getCookies()
            {
                return new Cookie[]{ new Cookie("language", "fr_CA") };
            }
        });

        // Set the wiki to multilingual mode.
        getConfigurationSource().setProperty("multilingual", "1");

        // By default we force only supported languages
        assertEquals(Locale.ENGLISH, this.xwiki.getLocalePreference(getContext()));
    }

    public void testGetLocalePreferenceWithCookieDefaultNotSet() throws Exception
    {
        getContext().setRequest(new XWikiServletRequest(null)
        {
            @Override
            public Cookie[] getCookies()
            {
                return new Cookie[]{ new Cookie("language", "fr_CA") };
            }
        });

        // Set the wiki to multilingual mode.
        getConfigurationSource().setProperty("multilingual", "1");

        // By default we force only supported languages
        assertEquals(Locale.ENGLISH, this.xwiki.getLocalePreference(getContext()));
    }

    public void testGetLocalePreferenceWithCookieDefaultSet() throws Exception
    {
        getContext().setRequest(new XWikiServletRequest(null)
        {
            @Override
            public Cookie[] getCookies()
            {
                return new Cookie[]{ new Cookie("language", "fr_CA") };
            }
        });

        // Set the wiki to multilingual mode.
        getConfigurationSource().setProperty("multilingual", "1");
        getConfigurationSource().setProperty("languages", "en, |fr_FR, |en_US, |fr_CA");

        // By default we force only supported languages
        assertEquals(Locale.CANADA_FRENCH, this.xwiki.getLocalePreference(getContext()));
    }

    public void testGetLocalePreferenceWithNavigatorLanguagesForcingUnset() throws Exception
    {
        getContext().setRequest(new XWikiServletRequest(null)
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
        getConfigurationSource().setProperty("xwiki.language.forceSupported", "0");

        // Set the wiki to multilingual mode.
        getConfigurationSource().setProperty("multilingual", "1");

        assertEquals(Locale.US, this.xwiki.getLocalePreference(getContext()));
    }

    public void testGetLocalePreferenceWithNavigatorLanguages() throws Exception
    {
        getContext().setRequest(new XWikiServletRequest(null)
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
        getConfigurationSource().setProperty("multilingual", "1");

        // it's forced to "en" since it's the only supported language by default
        assertEquals(Locale.ENGLISH, this.xwiki.getLocalePreference(getContext()));
    }

    public void testGetLocalePreferenceWithNavigatorLanguagesDefaultSet() throws Exception
    {
        getContext().setRequest(new XWikiServletRequest(null)
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
        getConfigurationSource().setProperty("multilingual", "1");
        getConfigurationSource().setProperty("languages", "en, |fr_FR, |en_US, |fr_CA");

        // en_US is the first common language between supported and browser supported.
        assertEquals(Locale.US, this.xwiki.getLocalePreference(getContext()));
    }

    public void testGetLocalePreferenceWithNavigatorLanguagesFallback() throws Exception
    {
        getContext().setRequest(new XWikiServletRequest(null));

        // Set the wiki to multilingual mode.
        getConfigurationSource().setProperty("multilingual", "1");

        assertEquals(Locale.ENGLISH, this.xwiki.getLocalePreference(getContext()));
    }
}
