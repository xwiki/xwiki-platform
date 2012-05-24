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

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.doc.merge.MergeConfiguration;
import com.xpn.xwiki.doc.merge.MergeResult;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;
import com.xpn.xwiki.test.AbstractBridgedComponentTestCase;

/**
 * Validate {@link XWikiDocument#merge(XWikiDocument, XWikiDocument, MergeConfiguration, com.xpn.xwiki.XWikiContext)}.
 * 
 * @version $Id$
 */
public class XWikiDocumentMergeTest extends AbstractBridgedComponentTestCase
{
    private XWikiDocument document;

    private XWikiDocument previousDocument;

    private XWikiDocument newDocument;

    private BaseObject xobject;

    private BaseClass xclass;

    private MergeConfiguration configuration;

    @Override
    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        this.document = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        this.previousDocument = this.document.clone();
        this.newDocument = this.document.clone();

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

    private void merge() throws Exception
    {
        MergeResult result =
            this.document.merge(this.previousDocument, this.newDocument, this.configuration, getContext());

        List<Exception> exception = result.getErrors();
        if (!exception.isEmpty()) {
            throw exception.get(0);
        }
    }

    @Test
    public void testContent() throws Exception
    {
        this.previousDocument.setContent("some content");
        this.newDocument.setContent("some new content");
        this.document.setContent("some content");

        merge();

        Assert.assertEquals("some new content", this.document.getContent());
    }

    @Test
    public void testContentModified() throws Exception
    {   
        this.previousDocument.setContent("some content");
        this.newDocument.setContent("some content\nafter");
        this.document.setContent("before\nsome content");

        merge();

        Assert.assertEquals("before\nsome content\nafter", this.document.getContent());
        
        this.previousDocument.setContent("some content");
        this.newDocument.setContent("some content\nafter");
        this.document.setContent("some content");

        merge();

        Assert.assertEquals("some content\nafter", this.document.getContent());
    }

    @Test
    public void testNewObjectAdded() throws Exception
    {
        this.newDocument.addXObject(this.xobject);

        merge();

        Assert.assertSame(this.xobject, this.document.getXObject(this.xclass.getReference(), 0));
    }

    @Test
    public void testNewObjectRemoved() throws Exception
    {
        this.previousDocument.addXObject(this.xobject);
        this.document.addXObject(this.xobject.clone());

        merge();

        Assert.assertNull(this.document.getXObject(this.xclass.getReference(), 0));
    }
}
