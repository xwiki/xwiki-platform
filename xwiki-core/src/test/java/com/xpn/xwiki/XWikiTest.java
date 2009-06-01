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
import org.jmock.core.constraint.IsSame;
import org.jmock.core.stub.CustomStub;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.DocumentDeleteEvent;
import org.xwiki.observation.event.DocumentSaveEvent;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiHibernateVersioningStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiVersioningStoreInterface;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;
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
        this.xwiki = new XWiki(new XWikiConfig(), getContext());

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
        assertEquals("Space.Document", this.xwiki.getDocumentNameFromPath("/view/Space/Document/attachment.pdf",
            getContext()));
        assertEquals("From.Space", this.xwiki.getDocumentNameFromPath("/Some:Document:From/Some:Space", getContext()));
        assertEquals("From.Space", this.xwiki.getDocumentNameFromPath("/Some:Document:From/Some:Other%3ASpace",
            getContext()));
    }

    public void testGetDocumentNameFromPathUsesDefaultSpaceAndDocument()
    {
        this.xwiki.getDefaultPage(getContext());
        this.xwiki.getConfig().setProperty("xwiki.defaultpage", "Default");
        assertEquals("Main.Default", this.xwiki.getDocumentNameFromPath("/", getContext()));
        this.xwiki.getConfig().setProperty("xwiki.defaultweb", "Content");
        assertEquals("Content.Default", this.xwiki.getDocumentNameFromPath("/", getContext()));
        assertEquals("Space.Default", this.xwiki.getDocumentNameFromPath("/Space/", getContext()));
    }

    /**
     * We only verify here that the saveDocument API calls the Observation component.
     */
    public void testSaveDocumentSendsObservationEvents() throws Exception
    {
        Mock mockListener = mock(EventListener.class);
        mockListener.stubs().method("getName").will(returnValue("testlistener"));
        mockListener.expects(once()).method("getEvents").will(returnValue(Arrays.asList(new DocumentSaveEvent("xwikitest:Some.Document"))));

        ObservationManager om = (ObservationManager) getComponentManager().lookup(ObservationManager.class);
        om.addListener((EventListener) mockListener.proxy());

        XWikiDocument document = new XWikiDocument("xwikitest", "Some", "Document");
        document.setContent("the content");

        // Ensure that the onEvent method has been called
        mockListener.expects(once()).method("onEvent").with(isA(DocumentSaveEvent.class), same(document), isA(XWikiContext.class));

        this.xwiki.saveDocument(document, getContext());
    }

    /**
     * We only verify here that the deleteDocument API calls the Observation component.
     */
    public void testDeleteDocumentSendsObservationEvents() throws Exception
    {
        Mock mockListener = mock(EventListener.class);
        mockListener.stubs().method("getName").will(returnValue("testlistener"));
        mockListener.expects(once()).method("getEvents").will(returnValue(Arrays.asList(new DocumentDeleteEvent("xwikitest:Another.Document"))));

        ObservationManager om = (ObservationManager) getComponentManager().lookup(ObservationManager.class);
        om.addListener((EventListener) mockListener.proxy());

        XWikiDocument document = new XWikiDocument("xwikitest", "Another", "Document");
        document.setContent("the content");

        // Not expectation on mock Listener since we're not subscribed to Document save events

        this.xwiki.saveDocument(document, getContext());

        // Ensure that the onEvent method has been called
        mockListener.expects(once()).method("onEvent").with(isA(DocumentDeleteEvent.class), isA(XWikiDocument.class), isA(XWikiContext.class));

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
        doc1.setSyntaxId("syntax1");
        XWikiDocument doc2 = new XWikiDocument();
        doc2.setSyntaxId("syntax2");
        
        assertEquals(null, this.xwiki.getCurrentContentSyntaxId(null, getContext()));
        assertEquals("syntax", this.xwiki.getCurrentContentSyntaxId("syntax", getContext()));
        
        getContext().setDoc(doc1);
        
        assertEquals("syntax1", this.xwiki.getCurrentContentSyntaxId(null, getContext()));
        
        getContext().put("sdoc", doc2);
        
        assertEquals("syntax2", this.xwiki.getCurrentContentSyntaxId(null, getContext()));
    }
}
