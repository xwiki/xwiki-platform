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

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.jmock.Mock;
import org.jmock.core.Invocation;
import org.jmock.core.stub.CustomStub;
import org.xwiki.component.manager.ComponentManager;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiHibernateVersioningStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiVersioningStoreInterface;
import com.xpn.xwiki.test.AbstractXWikiComponentTestCase;

/**
 * Unit tests for {@link com.xpn.xwiki.XWiki}.
 * 
 * @version $Id: $
 */
public class XWikiTest extends AbstractXWikiComponentTestCase
{
    private XWikiContext context;

    private XWikiDocument document;

    private XWiki xwiki;

    private Mock mockXWikiStore;

    private Mock mockXWikiVersioningStore;

    private Map docs = new HashMap();

    protected void setUp() throws Exception
    {
        this.context = new XWikiContext();
        this.document = new XWikiDocument("MilkyWay", "Fidis");
        this.xwiki = new XWiki(new XWikiConfig(), this.context);

        // We need to initialize the Component Manager so that tcomponents can be looked up
        this.context.put(ComponentManager.class.getName(), getComponentManager());

        this.mockXWikiStore =
            mock(XWikiHibernateStore.class, new Class[] {XWiki.class, XWikiContext.class},
                new Object[] {this.xwiki, this.context});
        this.mockXWikiStore.stubs().method("loadXWikiDoc").will(
            new CustomStub("Implements XWikiStoreInterface.loadXWikiDoc")
            {
                public Object invoke(Invocation invocation) throws Throwable
                {
                    XWikiDocument shallowDoc = (XWikiDocument) invocation.parameterValues.get(0);
                    if (docs.containsKey(shallowDoc.getName())) {
                        return (XWikiDocument) docs.get(shallowDoc.getName());
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
        this.mockXWikiStore.stubs().method("getTranslationList").will(
            returnValue(Collections.EMPTY_LIST));

        this.mockXWikiVersioningStore =
            mock(XWikiHibernateVersioningStore.class, new Class[] {XWiki.class,
            XWikiContext.class}, new Object[] {this.xwiki, this.context});
        this.mockXWikiVersioningStore.stubs().method("getXWikiDocumentArchive").will(
            returnValue(null));
        this.mockXWikiVersioningStore.stubs().method("resetRCSArchive").will(returnValue(null));

        this.xwiki.setStore((XWikiStoreInterface) mockXWikiStore.proxy());
        this.xwiki.setVersioningStore((XWikiVersioningStoreInterface) mockXWikiVersioningStore
            .proxy());
        this.xwiki.saveDocument(this.document, this.context);

        this.document.setCreator("Condor");
        this.document.setAuthor("Albatross");
        this.xwiki.saveDocument(this.document, this.context);
    }

    public void testAuthorAfterDocumentCopy() throws XWikiException
    {
        String copyName = "Lyre";
        String author = this.document.getAuthor();
        this.xwiki.copyDocument(this.document.getName(), this.document.getSpace() + "."
            + copyName, this.context);
        XWikiDocument copy = this.xwiki.getDocument(copyName, context);

        assertTrue(author.equals(copy.getAuthor()));
    }

    public void testCreatorAfterDocumentCopy() throws XWikiException
    {
        String copyName = "Sirius";
        String creator = this.document.getCreator();
        this.xwiki.copyDocument(this.document.getName(), this.document.getSpace() + "."
            + copyName, this.context);
        XWikiDocument copy = this.xwiki.getDocument(copyName, context);

        assertTrue(creator.equals(copy.getCreator()));
    }

    public void testCreationDateAfterDocumentCopy() throws XWikiException, InterruptedException
    {
        Date sourceCreationDate = this.document.getCreationDate();
        Thread.sleep(1000);
        String copyName = this.document.getName() + "Copy";
        this.xwiki.copyDocument(this.document.getName(), this.document.getSpace() + "."
            + copyName, this.context);
        XWikiDocument copy = this.xwiki.getDocument(copyName, context);

        assertTrue(copy.getCreationDate().equals(sourceCreationDate));
    }

    public void testParseTemplateConsidersObjectField() throws XWikiException
    {
        XWikiDocument skinClass = new XWikiDocument("XWiki", "XWikiSkins");
        skinClass.getxWikiClass().addTextAreaField("template.vm", "template", 80, 20);
        xwiki.saveDocument(skinClass, context);
        XWikiDocument skin = new XWikiDocument("XWiki", "Skin");
        BaseObject obj = skin.newObject("XWiki.XWikiSkins", context);
        obj.setLargeStringValue("template.vm", "parsing a field");
        xwiki.saveDocument(skin, context);
        context.put("skin", "XWiki.Skin");
        assertEquals("XWiki.Skin", xwiki.getSkin(context));
        assertFalse(xwiki.getDocument("XWiki.Skin", context).isNew());
        assertEquals(skin, xwiki.getDocument("XWiki.Skin", context));
        assertEquals("parsing a field", xwiki.parseTemplate("template.vm", context));
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
        xwiki.saveDocument(skin, context);
        context.put("skin", "XWiki.Skin");
        assertEquals("XWiki.Skin", xwiki.getSkin(context));
        assertFalse(xwiki.getDocument("XWiki.Skin", context).isNew());
        assertEquals(skin, xwiki.getDocument("XWiki.Skin", context));
        assertEquals("parsing an attachment", xwiki.parseTemplate("template.vm", context));
    }

    /**
     * See XWIKI-2098
     */
    public void testParseTemplateConsidersObjectFieldBeforeAttachment() throws XWikiException
    {
        XWikiDocument skinClass = new XWikiDocument("XWiki", "XWikiSkins");
        skinClass.getxWikiClass().addTextAreaField("template.vm", "template", 80, 20);
        xwiki.saveDocument(skinClass, context);
        XWikiDocument skin = new XWikiDocument("XWiki", "Skin");
        BaseObject obj = skin.newObject("XWiki.XWikiSkins", context);
        obj.setLargeStringValue("template.vm", "parsing a field");
        XWikiAttachment attachment = new XWikiAttachment();
        skin.getAttachmentList().add(attachment);
        attachment.setContent("parsing an attachment".getBytes());
        attachment.setFilename("template.vm");
        attachment.setDoc(skin);
        xwiki.saveDocument(skin, context);
        context.put("skin", "XWiki.Skin");
        assertEquals("XWiki.Skin", xwiki.getSkin(context));
        assertFalse(xwiki.getDocument("XWiki.Skin", context).isNew());
        assertEquals(skin, xwiki.getDocument("XWiki.Skin", context));
        assertEquals("parsing a field", xwiki.parseTemplate("template.vm", context));
    }
    
    public void testClearNameWithoutStripDotsWithoutAscii()
    {
        assertEquals("ee{&.txt", this.xwiki.clearName("\u00E9\u00EA{&.txt", false, false, context));
    }

    public void testClearNameWithoutStripDotsWithAscii()
    {
        assertEquals("ee.txt", this.xwiki.clearName("\u00E9\u00EA{&.txt", false, true, context));
    }

    public void testClearNameWithStripDotsWithoutAscii()
    {
        assertEquals("ee{&txt", this.xwiki.clearName("\u00E9\u00EA{&.txt", true, false, context));
    }

    public void testClearNameWithStripDotsWithAscii()
    {
        assertEquals("eetxt", this.xwiki.clearName("\u00E9\u00EA{&.txt", true, true, context));
    }
    
    public void testDocNameFromPathRemovesPrefixes()
    {
        assertEquals("From.Space", this.xwiki.getDocumentNameFromPath("/Some:Document:From/Some:Space", context));
        assertEquals("From.Space", this.xwiki.getDocumentNameFromPath("/Some:Document:From/Some:Other%3ASpace", context));
    }
}
