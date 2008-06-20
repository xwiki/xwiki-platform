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
import org.xwiki.component.manager.ComponentManager;
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
import com.xpn.xwiki.test.AbstractXWikiComponentTestCase;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiServletRequest;

/**
 * Unit tests for {@link com.xpn.xwiki.XWiki}.
 * 
 * @version $Id$
 */
public class XWikiTest extends AbstractXWikiComponentTestCase
{
    private XWikiDocument document;

    private XWiki xwiki;

    private Mock mockXWikiStore;

    private Mock mockXWikiVersioningStore;

    private Map<String, XWikiDocument> docs = new HashMap<String, XWikiDocument>();

    protected void setUp() throws Exception
    {
        super.setUp();

        this.document = new XWikiDocument("MilkyWay", "Fidis");
        this.xwiki = new XWiki(new XWikiConfig(), getContext());

        this.mockXWikiStore =
            mock(XWikiHibernateStore.class, new Class[] {XWiki.class, XWikiContext.class}, new Object[] {this.xwiki,
            getContext()});
        this.mockXWikiStore.stubs().method("loadXWikiDoc").will(
            new CustomStub("Implements XWikiStoreInterface.loadXWikiDoc")
            {
                public Object invoke(Invocation invocation) throws Throwable
                {
                    XWikiDocument shallowDoc = (XWikiDocument) invocation.parameterValues.get(0);
                    if (docs.containsKey(shallowDoc.getName())) {
                        return docs.get(shallowDoc.getName());
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
                    document.setStore((XWikiStoreInterface) mockXWikiStore.proxy());
                    docs.put(document.getName(), document);
                    return null;
                }
            });
        this.mockXWikiStore.stubs().method("deleteXWikiDoc").will(
            new CustomStub("Implements XWikiStoreInterface.deleteXWikiDoc")
            {
                public Object invoke(Invocation invocation) throws Throwable
                {
                    XWikiDocument document = (XWikiDocument) invocation.parameterValues.get(0);
                    docs.remove(document.getName());
                    return null;
                }
            });
        this.mockXWikiStore.stubs().method("getTranslationList").will(returnValue(Collections.EMPTY_LIST));

        this.mockXWikiVersioningStore =
            mock(XWikiHibernateVersioningStore.class, new Class[] {XWiki.class, XWikiContext.class}, new Object[] {
            this.xwiki, getContext()});
        this.mockXWikiVersioningStore.stubs().method("getXWikiDocumentArchive").will(returnValue(null));
        this.mockXWikiVersioningStore.stubs().method("resetRCSArchive").will(returnValue(null));

        this.xwiki.setStore((XWikiStoreInterface) mockXWikiStore.proxy());
        this.xwiki.setVersioningStore((XWikiVersioningStoreInterface) mockXWikiVersioningStore.proxy());
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
        xwiki.saveDocument(skinClass, getContext());
        XWikiDocument skin = new XWikiDocument("XWiki", "Skin");
        BaseObject obj = skin.newObject("XWiki.XWikiSkins", getContext());
        obj.setLargeStringValue("template.vm", "parsing a field");
        xwiki.saveDocument(skin, getContext());
        getContext().put("skin", "XWiki.Skin");
        assertEquals("XWiki.Skin", xwiki.getSkin(getContext()));
        assertFalse(xwiki.getDocument("XWiki.Skin", getContext()).isNew());
        assertEquals(skin, xwiki.getDocument("XWiki.Skin", getContext()));
        assertEquals("parsing a field", xwiki.parseTemplate("template.vm", getContext()));
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
        xwiki.saveDocument(skin, getContext());
        getContext().put("skin", "XWiki.Skin");
        assertEquals("XWiki.Skin", xwiki.getSkin(getContext()));
        assertFalse(xwiki.getDocument("XWiki.Skin", getContext()).isNew());
        assertEquals(skin, xwiki.getDocument("XWiki.Skin", getContext()));
        assertEquals("parsing an attachment", xwiki.parseTemplate("template.vm", getContext()));
    }

    /**
     * See XWIKI-2098
     */
    public void testParseTemplateConsidersObjectFieldBeforeAttachment() throws XWikiException
    {
        XWikiDocument skinClass = new XWikiDocument("XWiki", "XWikiSkins");
        skinClass.getxWikiClass().addTextAreaField("template.vm", "template", 80, 20);
        xwiki.saveDocument(skinClass, getContext());
        XWikiDocument skin = new XWikiDocument("XWiki", "Skin");
        BaseObject obj = skin.newObject("XWiki.XWikiSkins", getContext());
        obj.setLargeStringValue("template.vm", "parsing a field");
        XWikiAttachment attachment = new XWikiAttachment();
        skin.getAttachmentList().add(attachment);
        attachment.setContent("parsing an attachment".getBytes());
        attachment.setFilename("template.vm");
        attachment.setDoc(skin);
        xwiki.saveDocument(skin, getContext());
        getContext().put("skin", "XWiki.Skin");
        assertEquals("XWiki.Skin", xwiki.getSkin(getContext()));
        assertFalse(xwiki.getDocument("XWiki.Skin", getContext()).isNew());
        assertEquals(skin, xwiki.getDocument("XWiki.Skin", getContext()));
        assertEquals("parsing a field", xwiki.parseTemplate("template.vm", getContext()));
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
        xwiki.getDefaultPage(getContext());
        xwiki.getConfig().setProperty("xwiki.defaultpage", "Default");
        assertEquals("Main.Default", this.xwiki.getDocumentNameFromPath("/", getContext()));
        xwiki.getConfig().setProperty("xwiki.defaultweb", "Content");
        assertEquals("Content.Default", this.xwiki.getDocumentNameFromPath("/", getContext()));
        assertEquals("Space.Default", this.xwiki.getDocumentNameFromPath("/Space/", getContext()));
    }

    /**
     * We only verify here that the saveDocument API calls the new Observation component.
     */
    public void testSaveDocumentSendsObservationEvents() throws Exception
    {
        TestSaveEventListener listener = new TestSaveEventListener();
        ObservationManager om = (ObservationManager) getComponentManager().lookup(ObservationManager.ROLE);
        om.addListener(new DocumentSaveEvent("Some.Document"), listener);

        XWikiDocument document = new XWikiDocument("Some", "Document");
        document.setContent("the content");

        this.xwiki.saveDocument(document, getContext());
        assertFalse(document.isNew());
        assertTrue("Listener not called", listener.hasListenerBeenCalled);
    }

    /**
     * We only verify here that the saveDocument API calls the new Observation component.
     */
    public void testDeleteDocumentSendsObservationEvents() throws Exception
    {
        TestDeleteEventListener listener = new TestDeleteEventListener();
        ObservationManager om = (ObservationManager) getComponentManager().lookup(ObservationManager.ROLE);
        om.addListener(new DocumentDeleteEvent("Another.Document"), listener);

        XWikiDocument document = new XWikiDocument("Another", "Document");
        document.setContent("the content");

        xwiki.saveDocument(document, getContext());
        assertFalse(document.isNew());
        assertFalse("Listener called for wrong event", listener.hasListenerBeenCalled);

        xwiki.deleteDocument(document, false, getContext());
        assertTrue("Listener not called", listener.hasListenerBeenCalled);
    }

    private static class TestSaveEventListener implements EventListener
    {
        public boolean hasListenerBeenCalled = false;

        public void onEvent(Event evt, Object source, Object data)
        {
            hasListenerBeenCalled = true;
            assertTrue(source instanceof XWikiDocument);
            XWikiDocument doc = (XWikiDocument) source;
            assertTrue("originalDocument should have been new", doc.getOriginalDocument().isNew());
            assertEquals("the content", doc.getContent());
        }
    }

    private static class TestDeleteEventListener implements EventListener
    {
        public boolean hasListenerBeenCalled = false;

        public void onEvent(Event evt, Object source, Object data)
        {
            hasListenerBeenCalled = true;
            assertTrue(source instanceof XWikiDocument);
            XWikiDocument doc = (XWikiDocument) source;
            assertTrue("New document should have been new", doc.isNew());
            assertFalse("originalDocument should not have been new", doc.getOriginalDocument().isNew());
            assertEquals("the content", doc.getOriginalDocument().getContent());
            assertEquals("\n", doc.getContent());
        }
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
        assertEquals("fr", xwiki.getLanguagePreference(getContext()));
    }
}
