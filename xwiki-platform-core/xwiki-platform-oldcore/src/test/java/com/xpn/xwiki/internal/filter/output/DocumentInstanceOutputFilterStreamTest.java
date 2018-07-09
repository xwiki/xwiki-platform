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
package com.xpn.xwiki.internal.filter.output;

import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.instance.output.DocumentInstanceOutputProperties;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxType;
import org.xwiki.test.annotation.AllComponents;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.filter.AbstractInstanceFilterStreamTest;
import com.xpn.xwiki.internal.filter.output.DocumentInstanceOutputFilterStream;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.NumberClass;

/**
 * Validate {@link DocumentInstanceOutputFilterStream}.
 * 
 * @version $Id$
 */
@AllComponents
public class DocumentInstanceOutputFilterStreamTest extends AbstractInstanceFilterStreamTest
{
    // Tests

    @Test
    public void testImportDocumentsPreserveVersion() throws FilterException, XWikiException, ParseException
    {
        DocumentInstanceOutputProperties outputProperties = new DocumentInstanceOutputProperties();

        outputProperties.setVersionPreserved(true);
        outputProperties.setVerbose(false);

        importFromXML("document1", outputProperties);

        XWikiDocument document =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space", "page"),
                this.oldcore.getXWikiContext());

        Assert.assertFalse(document.isNew());

        Assert.assertEquals(Locale.ENGLISH, document.getDefaultLocale());
        Assert.assertEquals(new DocumentReference("wiki", "space", "parent"), document.getParentReference());
        Assert.assertEquals("customclass", document.getCustomClass());
        Assert.assertEquals("title", document.getTitle());
        Assert.assertEquals("defaultTemplate", document.getDefaultTemplate());
        Assert.assertEquals("validationScript", document.getValidationScript());
        Assert.assertEquals(new Syntax(new SyntaxType("syntax", "syntax"), "1.0"), document.getSyntax());
        Assert.assertEquals(true, document.isHidden());
        Assert.assertEquals("content", document.getContent());

        Assert.assertEquals(new DocumentReference("wiki", "XWiki", "creator"), document.getCreatorReference());
        Assert.assertEquals(toDate("2000-01-01 00:00:00.0 UTC"), document.getCreationDate());
        Assert.assertEquals(new DocumentReference("wiki", "XWiki", "author"), document.getAuthorReference());
        Assert.assertEquals(toDate("2000-01-02 00:00:00.0 UTC"), document.getDate());
        Assert.assertEquals(toDate("2000-01-03 00:00:00.0 UTC"), document.getContentUpdateDate());
        Assert.assertEquals(new DocumentReference("wiki", "XWiki", "contentAuthor"),
            document.getContentAuthorReference());
        Assert.assertEquals(true, document.isMinorEdit());
        Assert.assertEquals("comment", document.getComment());
        Assert.assertEquals("1.42", document.getVersion());

        // Attachment

        Assert.assertEquals(1, document.getAttachmentList().size());
        XWikiAttachment attachment = document.getAttachment("attachment.txt");
        Assert.assertEquals("attachment.txt", attachment.getFilename());
        Assert.assertEquals(10, attachment.getLongSize());
        Assert.assertTrue(Arrays.equals(new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
            attachment.getContent(this.oldcore.getXWikiContext())));

        Assert.assertEquals("XWiki.attachmentAuthor", attachment.getAuthor());
        Assert.assertEquals(toDate("2000-01-05 00:00:00.0 UTC"), attachment.getDate());
        Assert.assertEquals("15.1", attachment.getVersion());
        Assert.assertEquals("attachment comment", attachment.getComment());

        // XClass

        BaseClass xclass = document.getXClass();
        Assert.assertEquals(1, xclass.getFieldList().size());
        Assert.assertEquals("customClass", xclass.getCustomClass());
        Assert.assertEquals("customMapping", xclass.getCustomMapping());
        Assert.assertEquals("defaultViewSheet", xclass.getDefaultViewSheet());
        Assert.assertEquals("defaultEditSheet", xclass.getDefaultEditSheet());
        Assert.assertEquals("defaultWeb", xclass.getDefaultWeb());
        Assert.assertEquals("nameField", xclass.getNameField());
        Assert.assertEquals("validationScript", xclass.getValidationScript());

        NumberClass numberFiled = (NumberClass) xclass.getField("prop1");
        Assert.assertEquals("prop1", numberFiled.getName());
        Assert.assertEquals(false, numberFiled.isDisabled());
        Assert.assertEquals(1, numberFiled.getNumber());
        Assert.assertEquals("long", numberFiled.getNumberType());
        Assert.assertEquals("Prop1", numberFiled.getPrettyName());
        Assert.assertEquals(30, numberFiled.getSize());
        Assert.assertEquals(false, numberFiled.isUnmodifiable());

        // Objects

        Map<DocumentReference, List<BaseObject>> objects = document.getXObjects();
        Assert.assertEquals(2, objects.size());

        // Object 1

        List<BaseObject> documentObjects = objects.get(new DocumentReference("wiki", "space", "page"));
        Assert.assertEquals(1, documentObjects.size());
        BaseObject documentObject = documentObjects.get(0);
        Assert.assertEquals(0, documentObject.getNumber());
        Assert.assertEquals(new DocumentReference("wiki", "space", "page"), documentObject.getXClassReference());
        Assert.assertEquals("e2167721-2a64-430c-9520-bac1c0ee68cb", documentObject.getGuid());

        Assert.assertEquals(1, documentObject.getFieldList().size());
        Assert.assertEquals(1, documentObject.getIntValue("prop1"));

        // Object 2

        List<BaseObject> otherObjects = objects.get(new DocumentReference("wiki", "otherspace", "otherclass"));
        Assert.assertEquals(1, otherObjects.size());
        BaseObject otherObject = otherObjects.get(0);
        Assert.assertEquals(0, otherObject.getNumber());
        Assert
            .assertEquals(new DocumentReference("wiki", "otherspace", "otherclass"), otherObject.getXClassReference());
        Assert.assertEquals("8eaeac52-e2f2-47b2-87e1-bc6909597b39", otherObject.getGuid());

        Assert.assertEquals(1, otherObject.getFieldList().size());
        Assert.assertEquals(2, otherObject.getIntValue("prop2"));
    }

    @Test
    public void testImportDocumentsWithoutLocaleAndRevision() throws FilterException, XWikiException, ParseException
    {
        DocumentInstanceOutputProperties outputProperties = new DocumentInstanceOutputProperties();

        outputProperties.setVersionPreserved(true);
        outputProperties.setVerbose(false);

        importFromXML("document1-2", outputProperties);

        XWikiDocument document =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space", "page"),
                this.oldcore.getXWikiContext());

        Assert.assertFalse(document.isNew());

        Assert.assertEquals(Locale.ENGLISH, document.getDefaultLocale());
        Assert.assertEquals(new DocumentReference("wiki", "space", "parent"), document.getParentReference());
        Assert.assertEquals("customclass", document.getCustomClass());
        Assert.assertEquals("title", document.getTitle());
        Assert.assertEquals("defaultTemplate", document.getDefaultTemplate());
        Assert.assertEquals("validationScript", document.getValidationScript());
        Assert.assertEquals(new Syntax(new SyntaxType("syntax", "syntax"), "1.0"), document.getSyntax());
        Assert.assertEquals(true, document.isHidden());
        Assert.assertEquals("content", document.getContent());

        Assert.assertEquals(new DocumentReference("wiki", "XWiki", "creator"), document.getCreatorReference());
        Assert.assertEquals(toDate("2000-01-01 00:00:00.0 UTC"), document.getCreationDate());
        Assert.assertEquals(new DocumentReference("wiki", "XWiki", "author"), document.getAuthorReference());
        Assert.assertEquals(toDate("2000-01-02 00:00:00.0 UTC"), document.getDate());
        Assert.assertEquals(toDate("2000-01-03 00:00:00.0 UTC"), document.getContentUpdateDate());
        Assert.assertEquals(new DocumentReference("wiki", "XWiki", "contentAuthor"),
            document.getContentAuthorReference());
        Assert.assertEquals(true, document.isMinorEdit());
        Assert.assertEquals("comment", document.getComment());
        Assert.assertEquals("1.1", document.getVersion());

        // Attachment

        Assert.assertEquals(1, document.getAttachmentList().size());
        XWikiAttachment attachment = document.getAttachment("attachment.txt");
        Assert.assertEquals("attachment.txt", attachment.getFilename());
        Assert.assertEquals(10, attachment.getLongSize());
        Assert.assertTrue(Arrays.equals(new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
            attachment.getContent(this.oldcore.getXWikiContext())));

        Assert.assertEquals("XWiki.attachmentAuthor", attachment.getAuthor());
        Assert.assertEquals(toDate("2000-01-05 00:00:00.0 UTC"), attachment.getDate());
        Assert.assertEquals("1.1", attachment.getVersion());
        Assert.assertEquals("attachment comment", attachment.getComment());

        // XClass

        BaseClass xclass = document.getXClass();
        Assert.assertEquals("customClass", xclass.getCustomClass());
        Assert.assertEquals("customMapping", xclass.getCustomMapping());
        Assert.assertEquals("defaultViewSheet", xclass.getDefaultViewSheet());
        Assert.assertEquals("defaultEditSheet", xclass.getDefaultEditSheet());
        Assert.assertEquals("defaultWeb", xclass.getDefaultWeb());
        Assert.assertEquals("nameField", xclass.getNameField());
        Assert.assertEquals("validationScript", xclass.getValidationScript());
        Assert.assertEquals(1, xclass.getFieldList().size());

        NumberClass numberFiled = (NumberClass) xclass.getField("prop1");
        Assert.assertEquals("prop1", numberFiled.getName());
        Assert.assertEquals(false, numberFiled.isDisabled());
        Assert.assertEquals(1, numberFiled.getNumber());
        Assert.assertEquals("long", numberFiled.getNumberType());
        Assert.assertEquals("Prop1", numberFiled.getPrettyName());
        Assert.assertEquals(30, numberFiled.getSize());
        Assert.assertEquals(false, numberFiled.isUnmodifiable());

        // Objects

        Map<DocumentReference, List<BaseObject>> objects = document.getXObjects();
        Assert.assertEquals(2, objects.size());

        // Object 1

        List<BaseObject> documentObjects = objects.get(new DocumentReference("wiki", "space", "page"));
        Assert.assertEquals(1, documentObjects.size());
        BaseObject documentObject = documentObjects.get(0);
        Assert.assertEquals(0, documentObject.getNumber());
        Assert.assertEquals(new DocumentReference("wiki", "space", "page"), documentObject.getXClassReference());
        Assert.assertEquals("e2167721-2a64-430c-9520-bac1c0ee68cb", documentObject.getGuid());

        Assert.assertEquals(1, documentObject.getFieldList().size());
        Assert.assertEquals(1, documentObject.getIntValue("prop1"));

        // Object 2

        List<BaseObject> otherObjects = objects.get(new DocumentReference("wiki", "otherspace", "otherclass"));
        Assert.assertEquals(1, otherObjects.size());
        BaseObject otherObject = otherObjects.get(0);
        Assert.assertEquals(0, otherObject.getNumber());
        Assert
            .assertEquals(new DocumentReference("wiki", "otherspace", "otherclass"), otherObject.getXClassReference());
        Assert.assertEquals("8eaeac52-e2f2-47b2-87e1-bc6909597b39", otherObject.getGuid());

        Assert.assertEquals(1, otherObject.getFieldList().size());
        Assert.assertEquals(2, otherObject.getIntValue("prop2"));
    }

    @Test
    public void testDocumentwithunexistingobjectproperty() throws FilterException, XWikiException
    {
        DocumentInstanceOutputProperties outputProperties = new DocumentInstanceOutputProperties();

        outputProperties.setVerbose(false);

        importFromXML("documentwithunexistingobjectproperty", outputProperties);

        XWikiDocument document =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space", "page"),
                this.oldcore.getXWikiContext());

        Assert.assertFalse(document.isNew());

        // Objects

        Map<DocumentReference, List<BaseObject>> objects = document.getXObjects();
        Assert.assertEquals(1, objects.size());

        List<BaseObject> documentObjects = objects.get(new DocumentReference("wiki", "space", "page"));
        Assert.assertEquals(1, documentObjects.size());
        BaseObject documentObject = documentObjects.get(0);
        Assert.assertEquals(0, documentObject.getNumber());
        Assert.assertEquals(new DocumentReference("wiki", "space", "page"), documentObject.getXClassReference());

        Assert.assertEquals(1, documentObject.getFieldList().size());
        Assert.assertEquals(1, documentObject.getIntValue("prop1"));
    }

    @Test
    public void testDocumentwithnumberversion() throws FilterException, XWikiException
    {
        DocumentInstanceOutputProperties outputProperties = new DocumentInstanceOutputProperties();

        outputProperties.setVerbose(false);

        importFromXML("documentwithnumberversion", outputProperties);

        XWikiDocument document =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space", "page"),
                this.oldcore.getXWikiContext());

        Assert.assertFalse(document.isNew());

        // Version

        Assert.assertEquals("1.1", document.getVersion());
    }

    @Test
    public void testDocumentwithoutauthorandcreator() throws FilterException, XWikiException
    {
        DocumentReference contextUser = new DocumentReference("wiki", "XWiki", "contextuser");
        this.oldcore.getXWikiContext().setUserReference(contextUser);

        DocumentInstanceOutputProperties outputProperties = new DocumentInstanceOutputProperties();

        outputProperties.setVerbose(false);
        outputProperties.setAuthorPreserved(true);

        importFromXML("documentwithnumberversion", outputProperties);

        XWikiDocument document =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space", "page"),
                this.oldcore.getXWikiContext());

        Assert.assertFalse(document.isNew());

        Assert.assertEquals(contextUser, document.getCreatorReference());
        Assert.assertEquals(contextUser, document.getAuthorReference());
        Assert.assertEquals(contextUser, document.getContentAuthorReference());
    }

    @Test
    public void testDocumentwithattachmentwithoutdate() throws FilterException, XWikiException
    {
        DocumentInstanceOutputProperties outputProperties = new DocumentInstanceOutputProperties();

        outputProperties.setVerbose(false);

        importFromXML("documentwithattachmentwithoutdate", outputProperties);

        XWikiDocument document =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space", "page"),
                this.oldcore.getXWikiContext());

        Assert.assertFalse(document.isNew());

        // Attachment

        Assert.assertEquals(1, document.getAttachmentList().size());
        XWikiAttachment attachment = document.getAttachment("attachment.txt");
        Assert.assertEquals("attachment.txt", attachment.getFilename());
        Assert.assertEquals(10, attachment.getLongSize());
        Assert.assertTrue(Arrays.equals(new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
            attachment.getContent(this.oldcore.getXWikiContext())));

        Assert.assertNotNull(attachment.getDate());
        Assert.assertEquals("1.1", attachment.getVersion());
        Assert.assertEquals("", attachment.getComment());
        Assert.assertEquals("text/plain", attachment.getMimeType());
    }

    @Test
    public void testDocumentwithunknownclassproperty() throws FilterException, XWikiException
    {
        DocumentInstanceOutputProperties outputProperties = new DocumentInstanceOutputProperties();

        outputProperties.setVerbose(false);

        importFromXML("documentwithunknownClassproperty", outputProperties);

        XWikiDocument document =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space", "page"),
                this.oldcore.getXWikiContext());

        Assert.assertFalse(document.isNew());

        // Objects

        Map<DocumentReference, List<BaseObject>> objects = document.getXObjects();
        Assert.assertEquals(1, objects.size());

        List<BaseObject> documentObjects = objects.get(new DocumentReference("wiki", "space", "page"));
        Assert.assertEquals(1, documentObjects.size());
        BaseObject documentObject = documentObjects.get(0);
        Assert.assertEquals(0, documentObject.getNumber());
        Assert.assertEquals(new DocumentReference("wiki", "space", "page"), documentObject.getXClassReference());

        Assert.assertEquals(1, documentObject.getFieldList().size());
        Assert.assertEquals(1, documentObject.getIntValue("prop1"));
    }

    @Test
    public void testDocumentwithobjectwithoutnumberandclass() throws FilterException, XWikiException
    {
        DocumentInstanceOutputProperties outputProperties = new DocumentInstanceOutputProperties();

        outputProperties.setVerbose(false);

        importFromXML("documentwithobjectwithoutnumberandclass", outputProperties);

        XWikiDocument document =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space", "page"),
                this.oldcore.getXWikiContext());

        Assert.assertFalse(document.isNew());

        // Objects

        Map<DocumentReference, List<BaseObject>> objects = document.getXObjects();
        Assert.assertEquals(1, objects.size());

        List<BaseObject> documentObjects = objects.get(new DocumentReference("wiki", "otherspace", "otherclass"));
        Assert.assertEquals(1, documentObjects.size());
        BaseObject documentObject = documentObjects.get(0);
        Assert.assertEquals(0, documentObject.getNumber());
        Assert.assertEquals(1, documentObject.getFieldList().size());
        Assert.assertEquals("propvalue", documentObject.getStringValue("prop"));
    }
}
