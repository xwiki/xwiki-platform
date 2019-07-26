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
package com.xpn.xwiki.doc;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.diff.internal.DefaultDiffManager;
import org.xwiki.logging.LogLevel;
import org.xwiki.logging.event.LogEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.annotation.ComponentList;

import com.xpn.xwiki.doc.merge.MergeConfiguration;
import com.xpn.xwiki.doc.merge.MergeException;
import com.xpn.xwiki.doc.merge.MergeResult;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;
import com.xpn.xwiki.test.MockitoOldcoreRule;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

/**
 * Validate {@link XWikiDocument#merge(XWikiDocument, XWikiDocument, MergeConfiguration, com.xpn.xwiki.XWikiContext)}.
 * 
 * @version $Id$
 */
@ComponentList(value = {
    DefaultDiffManager.class
})
@ReferenceComponentList
public class XWikiDocumentMergeTest
{
    @Rule
    public MockitoOldcoreRule oldcore = new MockitoOldcoreRule();

    private XWikiDocument currentDocument;

    private XWikiDocument previousDocument;

    private XWikiDocument nextDocument;

    private BaseObject xobject;

    private BaseClass xclass;

    private MergeConfiguration configuration;

    @Before
    public void before() throws Exception
    {
        this.oldcore.registerMockEnvironment();

        this.currentDocument = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        this.previousDocument = this.currentDocument.clone();
        this.nextDocument = this.currentDocument.clone();

        this.xclass = new BaseClass();
        this.xclass.setDocumentReference(new DocumentReference("wiki", "classspace", "class"));
        this.xclass.addTextField("string", "String", 30);
        this.xclass.addTextAreaField("area", "Area", 10, 10);
        this.xclass.addTextAreaField("puretextarea", "Pure text area", 10, 10);
        // set the text areas an non interpreted content
        ((TextAreaClass) this.xclass.getField("puretextarea")).setContentType("puretext");
        this.xclass.addPasswordField("passwd", "Password", 30);
        this.xclass.addBooleanField("boolean", "Boolean", "yesno");
        this.xclass.addNumberField("int", "Int", 10, "integer");
        this.xclass.addStaticListField("stringlist", "StringList", "value1, value2");

        this.xobject = new BaseObject();
        this.xobject.setXClassReference(this.xclass.getDocumentReference());
        this.xobject.setStringValue("string", "string");
        this.xobject.setLargeStringValue("area", "area");
        this.xobject.setStringValue("passwd", "passwd");
        this.xobject.setIntValue("boolean", 1);
        this.xobject.setIntValue("int", 42);
        this.xobject.setStringListValue("stringlist", Arrays.asList("VALUE1", "VALUE2"));

        this.configuration = new MergeConfiguration();
    }

    private MergeResult merge() throws Exception
    {
        MergeResult result =
            this.currentDocument.merge(this.previousDocument, this.nextDocument, this.configuration,
                this.oldcore.getXWikiContext());

        List<LogEvent> exception = result.getLog().getLogs(LogLevel.ERROR);
        if (!exception.isEmpty()) {
            throw new MergeException(exception.get(0).getFormattedMessage(), exception.get(0).getThrowable());
        }

        return result;
    }

    // Tests

    // #merge

    @Test
    public void testMergeContent() throws Exception
    {
        this.previousDocument.setContent("some content");
        this.nextDocument.setContent("some new content");
        this.currentDocument.setContent("some content");

        merge();

        Assert.assertEquals("some new content", this.currentDocument.getContent());
    }

    @Test
    public void testMergeDefaultLocale() throws Exception
    {
        this.previousDocument.setDefaultLocale(Locale.ENGLISH);
        this.nextDocument.setDefaultLocale(Locale.FRENCH);
        this.currentDocument.setDefaultLocale(Locale.ENGLISH);

        merge();

        Assert.assertEquals(Locale.FRENCH, this.currentDocument.getDefaultLocale());
    }

    @Test
    public void testMergeContentModified() throws Exception
    {
        this.previousDocument.setContent("some content");
        this.nextDocument.setContent("some content\nafter");
        this.currentDocument.setContent("before\nsome content");

        merge();

        Assert.assertEquals("before\nsome content\nafter", this.currentDocument.getContent());

        this.previousDocument.setContent("some content");
        this.nextDocument.setContent("some content\nafter");
        this.currentDocument.setContent("some content");

        merge();

        Assert.assertEquals("some content\nafter", this.currentDocument.getContent());
    }

    @Test
    public void testMergeNewObjectAdded() throws Exception
    {
        this.nextDocument.addXObject(this.xobject);

        merge();

        Assert.assertSame(this.xobject, this.currentDocument.getXObject(this.xclass.getReference(), 0));
    }

    @Test
    public void testMergeNewObjectRemoved() throws Exception
    {
        this.previousDocument.addXObject(this.xobject);
        this.currentDocument.addXObject(this.xobject.clone());

        merge();

        Assert.assertNull(this.currentDocument.getXObject(this.xclass.getReference(), 0));
    }

    @Test
    public void testMergeObjectModified() throws Exception
    {
        BaseObject previousobj = this.xobject;
        previousobj.setStringValue("test", "test1");
        this.previousDocument.addXObject(previousobj);

        BaseObject obj = this.xobject.clone();
        obj.setStringValue("test", "test1");
        this.currentDocument.addXObject(obj);

        BaseObject newobj = this.xobject.clone();
        newobj.setStringValue("test", "test2");
        this.nextDocument.addXObject(newobj);

        merge();

        BaseObject mergedobj = this.currentDocument.getXObject(this.xclass.getReference(), 0);

        Assert.assertNotNull(mergedobj);
        Assert.assertEquals("test2", mergedobj.getStringValue("test"));
    }

    @Test
    public void testMergeCurrentObjectRemoved() throws Exception
    {
        this.xobject.setStringValue("test", "");
        this.xobject.setStringValue("previoustest", "previoustest");
        this.previousDocument.addXObject(this.xobject);

        BaseObject newobj = this.xobject.clone();
        newobj.setStringValue("test", "test2");
        newobj.setStringValue("newtest", "newtest");
        this.nextDocument.addXObject(newobj);

        merge();

        BaseObject mergedobj = this.currentDocument.getXObject(this.xclass.getReference(), 0);

        Assert.assertNull(mergedobj);
    }

    @Test
    public void testMergeAttachmentEquals() throws Exception
    {
        XWikiAttachment attachment = new XWikiAttachment();

        attachment.setContent(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 });
        attachment.setLongSize(10);
        attachment.setFilename("file");

        this.previousDocument.addAttachment(attachment);
        this.nextDocument.addAttachment((XWikiAttachment) attachment.clone());
        this.currentDocument.addAttachment((XWikiAttachment) attachment.clone());

        MergeResult result = merge();

        Assert.assertFalse(result.isModified());
    }

    @Test
    public void testMergeAttachmentEqualsDeletedCurrent() throws Exception
    {
        XWikiAttachment attachment = new XWikiAttachment();

        attachment.setContent(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 });
        attachment.setLongSize(10);
        attachment.setFilename("file");

        this.previousDocument.addAttachment(attachment);
        this.nextDocument.addAttachment((XWikiAttachment) attachment.clone());

        MergeResult result = merge();

        Assert.assertFalse(result.isModified());
    }

    @Test
    public void testMergeAttachmentEqualsAddedCurrent() throws Exception
    {
        XWikiAttachment attachment = new XWikiAttachment();

        attachment.setContent(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 });
        attachment.setLongSize(10);
        attachment.setFilename("file");

        this.currentDocument.addAttachment(attachment);

        MergeResult result = merge();

        Assert.assertFalse(result.isModified());
    }

    @Test
    public void testMergeAttachmentEqualsModifiedCurrent() throws Exception
    {
        XWikiAttachment attachment = new XWikiAttachment();

        attachment.setContent(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 });
        attachment.setLongSize(10);
        attachment.setFilename("file");

        this.previousDocument.addAttachment(attachment);
        this.nextDocument.addAttachment((XWikiAttachment) attachment.clone());

        attachment = (XWikiAttachment) attachment.clone();
        attachment.setContent(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8 });
        attachment.setLongSize(9);

        this.currentDocument.addAttachment(attachment);

        MergeResult result = merge();

        Assert.assertFalse(result.isModified());
    }

    @Test
    public void testMergeAttachmentNew() throws Exception
    {
        XWikiAttachment attachment = new XWikiAttachment();

        attachment.setContent(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 });
        attachment.setLongSize(10);
        attachment.setFilename("file");

        this.nextDocument.addAttachment(attachment);

        MergeResult result = merge();

        Assert.assertTrue(result.isModified());

        XWikiAttachment newAttachment = this.currentDocument.getAttachment("file");

        Assert.assertNotNull(newAttachment);
        Assert.assertEquals(10, newAttachment.getLongSize());
        Assert.assertArrayEquals(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }, newAttachment.getContent(null));
    }

    @Test
    public void testMergeAttachmentDeleted() throws Exception
    {
        XWikiAttachment attachment = new XWikiAttachment();

        attachment.setContent(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 });
        attachment.setLongSize(10);
        attachment.setFilename("file");

        this.currentDocument.addAttachment(attachment);
        this.previousDocument.addAttachment((XWikiAttachment) attachment.clone());

        MergeResult result = merge();

        Assert.assertTrue(result.isModified());

        XWikiAttachment newAttachment = this.currentDocument.getAttachment("file");

        Assert.assertNull(newAttachment);
    }

    @Test
    public void testMergeAttachmentModified() throws Exception
    {
        XWikiAttachment attachment = new XWikiAttachment();

        attachment.setContent(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 });
        attachment.setLongSize(10);
        attachment.setFilename("file");

        this.currentDocument.addAttachment(attachment);
        this.previousDocument.addAttachment((XWikiAttachment) attachment.clone());

        attachment = (XWikiAttachment) attachment.clone();
        attachment.setContent(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8 });
        attachment.setLongSize(9);

        this.nextDocument.addAttachment(attachment);

        MergeResult result = merge();

        Assert.assertTrue(result.isModified());

        XWikiAttachment newAttachment = this.currentDocument.getAttachment("file");

        Assert.assertNotNull(newAttachment);
        Assert.assertEquals(9, newAttachment.getLongSize());
        Assert.assertArrayEquals(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8 }, newAttachment.getContent(null));
    }

    // #apply

    @Test
    public void testApplyWithUnmodifiedObject()
    {
        this.previousDocument.addXObject(this.xobject);
        this.currentDocument.addXObject(this.xobject.clone());

        Assert.assertFalse(this.previousDocument.apply(this.currentDocument, true));
    }

    @Test
    public void testApplyWithModifiedObjectAndClean()
    {
        this.previousDocument.addXObject(this.xobject);
        BaseObject modifiedObject = this.xobject.clone();
        modifiedObject.setStringValue("string", "string2");
        this.currentDocument.addXObject(modifiedObject);

        Assert.assertTrue(this.previousDocument.apply(this.currentDocument, true));
        Assert.assertEquals("string2", this.xobject.getStringValue("string"));
    }

    @Test
    public void testMergeWithAddedSameObject() throws Exception
    {
        this.currentDocument.addXObject(this.xobject);
        this.nextDocument.addXObject(this.xobject.clone());

        MergeResult result = merge();
        Assert.assertFalse(result.isModified());
        Assert.assertTrue(result.getLog().getLogs(LogLevel.ERROR).isEmpty());
    }

    @Test
    public void testMergeWithAddedSameProperty() throws Exception
    {
        this.previousDocument.addXObject(xobject);
        BaseObject xobj = this.xobject.clone();
        xobj.setStringValue("another prop", "foo");
        this.currentDocument.addXObject(xobj);
        this.nextDocument.addXObject(xobj.clone());

        MergeResult result = merge();
        Assert.assertFalse(result.isModified());
        Assert.assertTrue(result.getLog().getLogs(LogLevel.ERROR).isEmpty());
    }
}
