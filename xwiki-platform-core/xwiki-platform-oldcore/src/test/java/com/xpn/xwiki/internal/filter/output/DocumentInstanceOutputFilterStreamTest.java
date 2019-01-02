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

import org.junit.jupiter.api.Test;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.instance.output.DocumentInstanceOutputProperties;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxType;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.filter.AbstractInstanceFilterStreamTest;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.NumberClass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validate {@link DocumentInstanceOutputFilterStream}.
 * 
 * @version $Id$
 */
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

        XWikiDocument document = this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space", "page"),
            this.oldcore.getXWikiContext());

        assertFalse(document.isNew());

        assertEquals(Locale.ENGLISH, document.getDefaultLocale());
        assertEquals(new DocumentReference("wiki", "space", "parent"), document.getParentReference());
        assertEquals("customclass", document.getCustomClass());
        assertEquals("title", document.getTitle());
        assertEquals("defaultTemplate", document.getDefaultTemplate());
        assertEquals("validationScript", document.getValidationScript());
        assertEquals(new Syntax(new SyntaxType("syntax", "syntax"), "1.0"), document.getSyntax());
        assertEquals(true, document.isHidden());
        assertEquals("content", document.getContent());

        assertEquals(new DocumentReference("wiki", "XWiki", "creator"), document.getCreatorReference());
        assertEquals(toDate("2000-01-01 00:00:00.0 UTC"), document.getCreationDate());
        assertEquals(new DocumentReference("wiki", "XWiki", "author"), document.getAuthorReference());
        assertEquals(toDate("2000-01-02 00:00:00.0 UTC"), document.getDate());
        assertEquals(toDate("2000-01-03 00:00:00.0 UTC"), document.getContentUpdateDate());
        assertEquals(new DocumentReference("wiki", "XWiki", "contentAuthor"), document.getContentAuthorReference());
        assertEquals(true, document.isMinorEdit());
        assertEquals("comment", document.getComment());
        assertEquals("1.42", document.getVersion());

        // Attachment

        assertEquals(1, document.getAttachmentList().size());
        XWikiAttachment attachment = document.getAttachment("attachment.txt");
        assertEquals("attachment.txt", attachment.getFilename());
        assertEquals(10, attachment.getLongSize());
        assertTrue(Arrays.equals(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 },
            attachment.getContent(this.oldcore.getXWikiContext())));

        assertEquals("XWiki.attachmentAuthor", attachment.getAuthor());
        assertEquals(toDate("2000-01-05 00:00:00.0 UTC"), attachment.getDate());
        assertEquals("15.1", attachment.getVersion());
        assertEquals("attachment comment", attachment.getComment());

        // XClass

        BaseClass xclass = document.getXClass();
        assertEquals(1, xclass.getFieldList().size());
        assertEquals("customClass", xclass.getCustomClass());
        assertEquals("customMapping", xclass.getCustomMapping());
        assertEquals("defaultViewSheet", xclass.getDefaultViewSheet());
        assertEquals("defaultEditSheet", xclass.getDefaultEditSheet());
        assertEquals("defaultWeb", xclass.getDefaultWeb());
        assertEquals("nameField", xclass.getNameField());
        assertEquals("validationScript", xclass.getValidationScript());

        NumberClass numberFiled = (NumberClass) xclass.getField("prop1");
        assertEquals("prop1", numberFiled.getName());
        assertEquals(false, numberFiled.isDisabled());
        assertEquals(1, numberFiled.getNumber());
        assertEquals("long", numberFiled.getNumberType());
        assertEquals("Prop1", numberFiled.getPrettyName());
        assertEquals(30, numberFiled.getSize());
        assertEquals(false, numberFiled.isUnmodifiable());

        // Objects

        Map<DocumentReference, List<BaseObject>> objects = document.getXObjects();
        assertEquals(2, objects.size());

        // Object 1

        List<BaseObject> documentObjects = objects.get(new DocumentReference("wiki", "space", "page"));
        assertEquals(1, documentObjects.size());
        BaseObject documentObject = documentObjects.get(0);
        assertEquals(0, documentObject.getNumber());
        assertEquals(new DocumentReference("wiki", "space", "page"), documentObject.getXClassReference());
        assertEquals("e2167721-2a64-430c-9520-bac1c0ee68cb", documentObject.getGuid());

        assertEquals(1, documentObject.getFieldList().size());
        assertEquals(1, documentObject.getIntValue("prop1"));

        // Object 2

        List<BaseObject> otherObjects = objects.get(new DocumentReference("wiki", "otherspace", "otherclass"));
        assertEquals(1, otherObjects.size());
        BaseObject otherObject = otherObjects.get(0);
        assertEquals(0, otherObject.getNumber());
        assertEquals(new DocumentReference("wiki", "otherspace", "otherclass"), otherObject.getXClassReference());
        assertEquals("8eaeac52-e2f2-47b2-87e1-bc6909597b39", otherObject.getGuid());

        assertEquals(1, otherObject.getFieldList().size());
        assertEquals(2, otherObject.getIntValue("prop2"));
    }

    @Test
    public void testImportDocumentsWithoutLocaleAndRevision() throws FilterException, XWikiException, ParseException
    {
        DocumentInstanceOutputProperties outputProperties = new DocumentInstanceOutputProperties();

        outputProperties.setVersionPreserved(true);
        outputProperties.setVerbose(false);

        importFromXML("document1-2", outputProperties);

        XWikiDocument document = this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space", "page"),
            this.oldcore.getXWikiContext());

        assertFalse(document.isNew());

        assertEquals(Locale.ENGLISH, document.getDefaultLocale());
        assertEquals(new DocumentReference("wiki", "space", "parent"), document.getParentReference());
        assertEquals("customclass", document.getCustomClass());
        assertEquals("title", document.getTitle());
        assertEquals("defaultTemplate", document.getDefaultTemplate());
        assertEquals("validationScript", document.getValidationScript());
        assertEquals(new Syntax(new SyntaxType("syntax", "syntax"), "1.0"), document.getSyntax());
        assertEquals(true, document.isHidden());
        assertEquals("content", document.getContent());

        assertEquals(new DocumentReference("wiki", "XWiki", "creator"), document.getCreatorReference());
        assertEquals(toDate("2000-01-01 00:00:00.0 UTC"), document.getCreationDate());
        assertEquals(new DocumentReference("wiki", "XWiki", "author"), document.getAuthorReference());
        assertEquals(toDate("2000-01-02 00:00:00.0 UTC"), document.getDate());
        assertEquals(toDate("2000-01-03 00:00:00.0 UTC"), document.getContentUpdateDate());
        assertEquals(new DocumentReference("wiki", "XWiki", "contentAuthor"), document.getContentAuthorReference());
        assertEquals(true, document.isMinorEdit());
        assertEquals("comment", document.getComment());
        assertEquals("1.1", document.getVersion());

        // Attachment

        assertEquals(1, document.getAttachmentList().size());
        XWikiAttachment attachment = document.getAttachment("attachment.txt");
        assertEquals("attachment.txt", attachment.getFilename());
        assertEquals(10, attachment.getLongSize());
        assertTrue(Arrays.equals(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 },
            attachment.getContent(this.oldcore.getXWikiContext())));

        assertEquals("XWiki.attachmentAuthor", attachment.getAuthor());
        assertEquals(toDate("2000-01-05 00:00:00.0 UTC"), attachment.getDate());
        assertEquals("1.1", attachment.getVersion());
        assertEquals("attachment comment", attachment.getComment());

        // XClass

        BaseClass xclass = document.getXClass();
        assertEquals("customClass", xclass.getCustomClass());
        assertEquals("customMapping", xclass.getCustomMapping());
        assertEquals("defaultViewSheet", xclass.getDefaultViewSheet());
        assertEquals("defaultEditSheet", xclass.getDefaultEditSheet());
        assertEquals("defaultWeb", xclass.getDefaultWeb());
        assertEquals("nameField", xclass.getNameField());
        assertEquals("validationScript", xclass.getValidationScript());
        assertEquals(1, xclass.getFieldList().size());

        NumberClass numberFiled = (NumberClass) xclass.getField("prop1");
        assertEquals("prop1", numberFiled.getName());
        assertEquals(false, numberFiled.isDisabled());
        assertEquals(1, numberFiled.getNumber());
        assertEquals("long", numberFiled.getNumberType());
        assertEquals("Prop1", numberFiled.getPrettyName());
        assertEquals(30, numberFiled.getSize());
        assertEquals(false, numberFiled.isUnmodifiable());

        // Objects

        Map<DocumentReference, List<BaseObject>> objects = document.getXObjects();
        assertEquals(2, objects.size());

        // Object 1

        List<BaseObject> documentObjects = objects.get(new DocumentReference("wiki", "space", "page"));
        assertEquals(1, documentObjects.size());
        BaseObject documentObject = documentObjects.get(0);
        assertEquals(0, documentObject.getNumber());
        assertEquals(new DocumentReference("wiki", "space", "page"), documentObject.getXClassReference());
        assertEquals("e2167721-2a64-430c-9520-bac1c0ee68cb", documentObject.getGuid());

        assertEquals(1, documentObject.getFieldList().size());
        assertEquals(1, documentObject.getIntValue("prop1"));

        // Object 2

        List<BaseObject> otherObjects = objects.get(new DocumentReference("wiki", "otherspace", "otherclass"));
        assertEquals(1, otherObjects.size());
        BaseObject otherObject = otherObjects.get(0);
        assertEquals(0, otherObject.getNumber());
        assertEquals(new DocumentReference("wiki", "otherspace", "otherclass"), otherObject.getXClassReference());
        assertEquals("8eaeac52-e2f2-47b2-87e1-bc6909597b39", otherObject.getGuid());

        assertEquals(1, otherObject.getFieldList().size());
        assertEquals(2, otherObject.getIntValue("prop2"));
    }

    @Test
    public void testDocumentwithunexistingobjectproperty() throws FilterException, XWikiException
    {
        DocumentInstanceOutputProperties outputProperties = new DocumentInstanceOutputProperties();

        outputProperties.setVerbose(false);

        importFromXML("documentwithunexistingobjectproperty", outputProperties);

        XWikiDocument document = this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space", "page"),
            this.oldcore.getXWikiContext());

        assertFalse(document.isNew());

        // Objects

        Map<DocumentReference, List<BaseObject>> objects = document.getXObjects();
        assertEquals(1, objects.size());

        List<BaseObject> documentObjects = objects.get(new DocumentReference("wiki", "space", "page"));
        assertEquals(1, documentObjects.size());
        BaseObject documentObject = documentObjects.get(0);
        assertEquals(0, documentObject.getNumber());
        assertEquals(new DocumentReference("wiki", "space", "page"), documentObject.getXClassReference());

        assertEquals(1, documentObject.getFieldList().size());
        assertEquals(1, documentObject.getIntValue("prop1"));
    }

    @Test
    public void testDocumentwithnumberversion() throws FilterException, XWikiException
    {
        DocumentInstanceOutputProperties outputProperties = new DocumentInstanceOutputProperties();

        outputProperties.setVerbose(false);

        importFromXML("documentwithnumberversion", outputProperties);

        XWikiDocument document = this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space", "page"),
            this.oldcore.getXWikiContext());

        assertFalse(document.isNew());

        // Version

        assertEquals("1.1", document.getVersion());
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

        XWikiDocument document = this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space", "page"),
            this.oldcore.getXWikiContext());

        assertFalse(document.isNew());

        assertEquals(contextUser, document.getCreatorReference());
        assertEquals(contextUser, document.getAuthorReference());
        assertEquals(contextUser, document.getContentAuthorReference());
    }

    @Test
    public void testDocumentwithattachmentwithoutdate() throws FilterException, XWikiException
    {
        DocumentInstanceOutputProperties outputProperties = new DocumentInstanceOutputProperties();

        outputProperties.setVerbose(false);

        importFromXML("documentwithattachmentwithoutdate", outputProperties);

        XWikiDocument document = this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space", "page"),
            this.oldcore.getXWikiContext());

        assertFalse(document.isNew());

        // Attachment

        assertEquals(1, document.getAttachmentList().size());
        XWikiAttachment attachment = document.getAttachment("attachment.txt");
        assertEquals("attachment.txt", attachment.getFilename());
        assertEquals(10, attachment.getLongSize());
        assertTrue(Arrays.equals(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 },
            attachment.getContent(this.oldcore.getXWikiContext())));

        assertNotNull(attachment.getDate());
        assertEquals("1.1", attachment.getVersion());
        assertEquals("", attachment.getComment());
        assertEquals("text/plain", attachment.getMimeType());
    }


    @Test
    public void testDocumentwithattachmentwithoutcontent() throws FilterException, XWikiException
    {
        DocumentInstanceOutputProperties outputProperties = new DocumentInstanceOutputProperties();

        outputProperties.setVerbose(false);

        importFromXML("documentwithattachmentwithoutcontent", outputProperties);

        XWikiDocument document = this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space", "page"),
            this.oldcore.getXWikiContext());

        assertFalse(document.isNew());

        // Attachment

        assertEquals(1, document.getAttachmentList().size());
        XWikiAttachment attachment = document.getAttachment("attachment.txt");
        assertEquals("attachment.txt", attachment.getFilename());
        assertEquals(10, attachment.getLongSize());
    }

    @Test
    public void testDocumentwithunknownclassproperty() throws FilterException, XWikiException
    {
        DocumentInstanceOutputProperties outputProperties = new DocumentInstanceOutputProperties();

        outputProperties.setVerbose(false);

        importFromXML("documentwithunknownClassproperty", outputProperties);

        XWikiDocument document = this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space", "page"),
            this.oldcore.getXWikiContext());

        assertFalse(document.isNew());

        // Objects

        Map<DocumentReference, List<BaseObject>> objects = document.getXObjects();
        assertEquals(1, objects.size());

        List<BaseObject> documentObjects = objects.get(new DocumentReference("wiki", "space", "page"));
        assertEquals(1, documentObjects.size());
        BaseObject documentObject = documentObjects.get(0);
        assertEquals(0, documentObject.getNumber());
        assertEquals(new DocumentReference("wiki", "space", "page"), documentObject.getXClassReference());

        assertEquals(1, documentObject.getFieldList().size());
        assertEquals(1, documentObject.getIntValue("prop1"));
    }

    @Test
    public void testDocumentwithobjectwithoutnumberandclass() throws FilterException, XWikiException
    {
        DocumentInstanceOutputProperties outputProperties = new DocumentInstanceOutputProperties();

        outputProperties.setVerbose(false);

        importFromXML("documentwithobjectwithoutnumberandclass", outputProperties);

        XWikiDocument document = this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space", "page"),
            this.oldcore.getXWikiContext());

        assertFalse(document.isNew());

        // Objects

        Map<DocumentReference, List<BaseObject>> objects = document.getXObjects();
        assertEquals(1, objects.size());

        List<BaseObject> documentObjects = objects.get(new DocumentReference("wiki", "otherspace", "otherclass"));
        assertEquals(1, documentObjects.size());
        BaseObject documentObject = documentObjects.get(0);
        assertEquals(0, documentObject.getNumber());
        assertEquals(1, documentObject.getFieldList().size());
        assertEquals("propvalue", documentObject.getStringValue("prop"));
    }
}
