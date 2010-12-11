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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.Cookie;

import org.apache.commons.collections.IteratorUtils;
import org.jmock.Mock;
import org.jmock.core.Invocation;
import org.jmock.core.stub.CustomStub;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentDeletingEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.rendering.syntax.Syntax;

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
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiServletRequest;

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

    private Map<String, XWikiDocument> docs = new HashMap<String, XWikiDocument>();

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        this.document = new XWikiDocument("MilkyWay", "Fidis");
        this.xwiki = new XWiki(new XWikiConfig(), getContext())
        {
            protected void registerWikiMacros()
            {
            }

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
        this.xwiki.getConfig().setProperty("xwiki.render.velocity.macrolist", "");

        this.mockXWikiStore =
            mock(XWikiHibernateStore.class, new Class[] {XWiki.class, XWikiContext.class}, new Object[] {this.xwiki,
            getContext()});
        this.mockXWikiStore.stubs().method("loadXWikiDoc").will(
            new CustomStub("Implements XWikiStoreInterface.loadXWikiDoc")
            {
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
                public Object invoke(Invocation invocation) throws Throwable
                {
                    XWikiDocument document = (XWikiDocument) invocation.parameterValues.get(0);
                    XWikiTest.this.docs.remove(document.getName());
                    return null;
                }
            });
        this.mockXWikiStore.stubs().method("getTranslationList").will(returnValue(Collections.EMPTY_LIST));
        this.mockXWikiStore.stubs().method("exists").will(returnValue(true));

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

    public void testAuthorAfterDocumentCopy() throws XWikiException
    {
        String copyName = "Lyre";
        String author = this.document.getAuthor();
        this.xwiki.copyDocument(this.document.getName(), this.document.getSpace() + "." + copyName, getContext());
        XWikiDocument copy = this.xwiki.getDocument(copyName, getContext());

        assertTrue(author.equals(copy.getAuthor()));
    }

    public void testCreatorAfterDocumentCopy() throws XWikiException
    {
        String copyName = "Sirius";
        String creator = this.document.getCreator();
        this.xwiki.copyDocument(this.document.getName(), this.document.getSpace() + "." + copyName, getContext());
        XWikiDocument copy = this.xwiki.getDocument(copyName, getContext());

        assertTrue(creator.equals(copy.getCreator()));
    }

    public void testCreationDateAfterDocumentCopy() throws XWikiException, InterruptedException
    {
        Date sourceCreationDate = this.document.getCreationDate();
        Thread.sleep(1000);
        String copyName = this.document.getName() + "Copy";
        this.xwiki.copyDocument(this.document.getName(), this.document.getSpace() + "." + copyName, getContext());
        XWikiDocument copy = this.xwiki.getDocument(copyName, getContext());

        assertTrue(copy.getCreationDate().equals(sourceCreationDate));
    }

    public void testParseTemplateConsidersObjectField() throws XWikiException
    {
        XWikiDocument skinClass = new XWikiDocument("XWiki", "XWikiSkins");
        skinClass.getxWikiClass().addTextAreaField("template.vm", "template", 80, 20);
        this.xwiki.saveDocument(skinClass, getContext());
        XWikiDocument skin = new XWikiDocument("XWiki", "Skin");
        BaseObject obj = skin.newObject("XWiki.XWikiSkins", getContext());
        obj.setLargeStringValue("template.vm", "parsing a field");
        this.xwiki.saveDocument(skin, getContext());
        getContext().put("skin", "XWiki.Skin");
        assertEquals("XWiki.Skin", this.xwiki.getSkin(getContext()));
        assertFalse(this.xwiki.getDocument("XWiki.Skin", getContext()).isNew());
        assertEquals(skin, this.xwiki.getDocument("XWiki.Skin", getContext()));
        assertEquals("parsing a field", this.xwiki.parseTemplate("template.vm", getContext()));
    }

    /**
     * See XWIKI-2096
     */
    public void testParseTemplateConsidersAttachment() throws XWikiException
    {
        XWikiDocument skin = new XWikiDocument("XWiki", "Skin");
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
    public void testParseTemplateConsidersObjectFieldBeforeAttachment() throws XWikiException
    {
        XWikiDocument skinClass = new XWikiDocument("XWiki", "XWikiSkins");
        skinClass.getxWikiClass().addTextAreaField("template.vm", "template", 80, 20);
        this.xwiki.saveDocument(skinClass, getContext());
        XWikiDocument skin = new XWikiDocument("XWiki", "Skin");
        BaseObject obj = skin.newObject("XWiki.XWikiSkins", getContext());
        obj.setLargeStringValue("template.vm", "parsing a field");
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

    public void testGetDocumentNameFromPath()
    {
        assertEquals("Main.WebHome", this.xwiki.getDocumentNameFromPath("", getContext()));
        assertEquals("Main.WebHome", this.xwiki.getDocumentNameFromPath("/", getContext()));
        assertEquals("Main.Document", this.xwiki.getDocumentNameFromPath("/Document", getContext()));
        assertEquals("Space.WebHome", this.xwiki.getDocumentNameFromPath("/Space/", getContext()));
        assertEquals("Space.Document", this.xwiki.getDocumentNameFromPath("/Space/Document", getContext()));
        assertEquals("Space.WebHome", this.xwiki.getDocumentNameFromPath("/view/Space/", getContext()));
        assertEquals("Space.Document", this.xwiki.getDocumentNameFromPath("/view/Space/Document", getContext()));
        assertEquals("Space.Document", this.xwiki.getDocumentNameFromPath("/view/Space/Document/", getContext()));
        assertEquals("Space.Document", this.xwiki.getDocumentNameFromPath("/view/Space/Document/some/ignored/paths",
            getContext()));

        // Test URL encoding and verify an encoded forward slash ("/" - encoded as %2F) works too.
        assertEquals("My Space.My/Document",
            this.xwiki.getDocumentNameFromPath("/My%20Space/My%2FDocument", getContext()));
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

        ObservationManager om = getComponentManager().lookup(ObservationManager.class);
        om.addListener((EventListener) mockListener.proxy());

        XWikiDocument document = new XWikiDocument("xwikitest", "Some", "Document");
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

        ObservationManager om = getComponentManager().lookup(ObservationManager.class);
        om.addListener((EventListener) mockListener.proxy());

        XWikiDocument document = new XWikiDocument("xwikitest", "Another", "Document");
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

    public void testLanguageSelection()
    {
        getContext().setRequest(new XWikiServletRequest(null)
        {
            @SuppressWarnings("unchecked")
            @Override
            public Enumeration getLocales()
            {
                ArrayList<Locale> locales = new ArrayList<Locale>();
                locales.add(new Locale("*"));
                locales.add(new Locale("en_US"));
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
        assertEquals("fr", this.xwiki.getLanguagePreference(getContext()));
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
        XWikiDocument testUser = new XWikiDocument("XWiki", "TestUser");
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
}
