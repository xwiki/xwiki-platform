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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.instance.output.DocumentInstanceOutputProperties;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxType;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiDocumentArchive;
import com.xpn.xwiki.internal.filter.AbstractInstanceFilterStreamTest;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
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
class DocumentInstanceOutputFilterStreamTest extends AbstractInstanceFilterStreamTest
{
    // Tests

    private void assertDocument1PreserveVersion(XWikiDocument document) throws XWikiException, ParseException
    {
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
        assertTrue(Arrays.equals(new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
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
    void importDocument1WithPreserveVersion() throws FilterException, XWikiException, ParseException
    {
        DocumentInstanceOutputProperties outputProperties = new DocumentInstanceOutputProperties();

        outputProperties.setVersionPreserved(true);
        outputProperties.setVerbose(false);

        importFromXML("document1", outputProperties);

        XWikiDocument document = this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space", "page"),
            this.oldcore.getXWikiContext());

        assertFalse(document.isNew());

        assertDocument1PreserveVersion(document);
    }

    @Test
    void importDocument1WithDeletePreviousAndPreserveVersion() throws FilterException, XWikiException, ParseException
    {
        DocumentInstanceOutputProperties outputProperties = new DocumentInstanceOutputProperties();

        outputProperties.setPreviousDeleted(true);
        outputProperties.setVersionPreserved(true);
        outputProperties.setVerbose(false);

        XWikiDocument document = this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space", "page"),
            this.oldcore.getXWikiContext());
        this.oldcore.getSpyXWiki().saveDocument(document, this.oldcore.getXWikiContext());

        assertFalse(this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki", "space", "page"), this.oldcore.getXWikiContext()).isNew());

        importFromXML("document1", outputProperties);

        document = this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space", "page"),
            this.oldcore.getXWikiContext());

        assertFalse(document.isNew());

        assertDocument1PreserveVersion(document);
    }

    @Test
    void importDocumentsWithoutLocaleAndRevision() throws FilterException, XWikiException, ParseException
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
        assertEquals(toDate("2000-01-02 00:00:00.0 UTC"), document.getDate());
        assertEquals(toDate("2000-01-03 00:00:00.0 UTC"), document.getContentUpdateDate());
        assertEquals(new DocumentReference("wiki", "XWiki", "author"), document.getAuthorReference());
        assertEquals(new DocumentReference("wiki", "XWiki", "contentAuthor"), document.getContentAuthorReference());
        assertEquals(true, document.isMinorEdit());
        assertEquals("comment", document.getComment());
        assertEquals("1.1", document.getVersion());

        // Attachment

        assertEquals(1, document.getAttachmentList().size());
        XWikiAttachment attachment = document.getAttachment("attachment.txt");
        assertEquals("attachment.txt", attachment.getFilename());
        assertEquals(10, attachment.getLongSize());
        assertTrue(Arrays.equals(new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
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
    void documentwithunexistingobjectproperty() throws FilterException, XWikiException
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
    void documentwithnumberversion() throws FilterException, XWikiException
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
    void documentwithshortusers() throws FilterException, XWikiException
    {
        DocumentInstanceOutputProperties outputProperties = new DocumentInstanceOutputProperties();

        outputProperties.setVerbose(false);

        importFromXML("documentwithshortusers", outputProperties);

        XWikiDocument document = this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space", "page"),
            this.oldcore.getXWikiContext());

        assertFalse(document.isNew());

        assertEquals(new DocumentReference("wiki", "XWiki", "creator"), document.getCreatorReference());
        assertEquals(new DocumentReference("wiki", "XWiki", "author"), document.getAuthorReference());
        assertEquals(new DocumentReference("wiki", "XWiki", "contentAuthor"), document.getContentAuthorReference());

        XWikiAttachment attachment = document.getAttachment("attachment.txt");
        assertEquals("XWiki.attachmentAuthor", attachment.getAuthor());
    }

    @Test
    void documentwithoutauthorandcreator() throws Exception
    {
        DocumentReference contextUser = new DocumentReference("wiki", "XWiki", "contextuser");
        UserReference contextUserReference = mockUserReference(contextUser);
        this.oldcore.getXWikiContext().setUserReference(contextUser);

        DocumentInstanceOutputProperties outputProperties = new DocumentInstanceOutputProperties();

        outputProperties.setVerbose(false);
        outputProperties.setAuthorPreserved(true);

        importFromXML("documentwithoutauthorandcreator", outputProperties);

        XWikiDocument document = this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space", "page"),
            this.oldcore.getXWikiContext());

        assertFalse(document.isNew());

        DocumentAuthors authors = document.getAuthors();
        assertEquals(contextUserReference, authors.getCreator());
        assertEquals(contextUserReference, authors.getEffectiveMetadataAuthor());
        assertEquals(contextUserReference, authors.getOriginalMetadataAuthor());
        assertEquals(contextUserReference, authors.getCreator());
        assertEquals(contextUserReference, authors.getContentAuthor());
    }

    @Test
    void documentwithattachmentwithoutdate() throws FilterException, XWikiException
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
        assertTrue(Arrays.equals(new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
            attachment.getContent(this.oldcore.getXWikiContext())));

        assertNotNull(attachment.getDate());
        assertEquals("1.1", attachment.getVersion());
        assertEquals("", attachment.getComment());
        assertEquals("text/plain", attachment.getMimeType());
    }

    @Test
    void documentwithattachmentwithoutcontent() throws FilterException, XWikiException
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
    public void documentwithunknownClassproperty() throws FilterException, XWikiException
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
    void documentwithobjectwithoutnumberandclass() throws FilterException, XWikiException
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

    @Test
    void documentwithobjectmissingapropertyfromanotexistingclass() throws FilterException, XWikiException
    {
        DocumentInstanceOutputProperties outputProperties = new DocumentInstanceOutputProperties();

        outputProperties.setVerbose(false);

        importFromXML("documentwithobjectmissingapropertyfromanotexistingclass", outputProperties);

        XWikiDocument document = this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space", "page"),
            this.oldcore.getXWikiContext());

        assertFalse(document.isNew());

        // Objects

        Map<DocumentReference, List<BaseObject>> objects = document.getXObjects();
        assertEquals(1, objects.size());

        List<BaseObject> documentObjects = objects.get(new DocumentReference("wiki", "missingspace", "missingclass"));
        assertEquals(1, documentObjects.size());
        BaseObject documentObject = documentObjects.get(0);
        assertEquals(0, documentObject.getNumber());
        assertEquals(2, documentObject.getFieldList().size());
        assertEquals("propvalue", documentObject.getStringValue("prop"));
        assertEquals("", documentObject.getStringValue("missingprop"));
    }

    @Test
    void documentwithobjectwithinconsistentclasses() throws FilterException, XWikiException
    {
        DocumentInstanceOutputProperties outputProperties = new DocumentInstanceOutputProperties();

        outputProperties.setVerbose(false);

        importFromXML("documentwithobjectwithinconsistentclasses", outputProperties);

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
        assertEquals(4, documentObject.getFieldList().size());
        assertEquals("propvalue", documentObject.getStringValue("prop"));
        assertEquals("42", ((BaseProperty) documentObject.get("prop2")).getValue());
        assertEquals("", ((BaseProperty) documentObject.get("missingprop1")).getValue());
        assertEquals("", ((BaseProperty) documentObject.get("missingprop2")).getValue());
    }

    @Test
    void documentwithoutversionsandnocreator() throws Exception
    {
        DocumentReference contextUser = new DocumentReference("wiki", "XWiki", "contextuser");
        mockUserReference(contextUser);
        this.oldcore.getXWikiContext().setUserReference(contextUser);

        DocumentReference author1 = new DocumentReference("wiki", "XWiki", "author1");
        UserReference author1Reference = mockUserReference(author1);
        DocumentReference author2 = new DocumentReference("wiki", "XWiki", "author2");
        UserReference author2Reference = mockUserReference(author2);

        DocumentInstanceOutputProperties outputProperties = new DocumentInstanceOutputProperties();

        outputProperties.setVerbose(false);

        importFromXML("documentwithoutversionsandnocreator", outputProperties);

        XWikiDocument document2 = this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space", "page"),
            this.oldcore.getXWikiContext());

        assertFalse(document2.isNew());

        assertEquals("2.1", document2.getVersion());
        assertEquals(author2Reference, document2.getAuthors().getEffectiveMetadataAuthor());
        assertEquals(author1Reference, document2.getAuthors().getCreator());

        Version[] versions = document2.getRevisions(this.oldcore.getXWikiContext());

        assertEquals(2, versions.length);

        XWikiDocumentArchive archive = document2.getDocumentArchive(this.oldcore.getXWikiContext());

        XWikiDocument document1 = archive.loadDocument(versions[0], this.oldcore.getXWikiContext());

        assertEquals("1.1", document1.getVersion());
        assertEquals(author1Reference, document1.getAuthors().getEffectiveMetadataAuthor());
        assertEquals(author1Reference, document1.getAuthors().getCreator());
    }

    @Test
    void importAttachmentWithRevisions() throws FilterException, XWikiException, IOException
    {
        DocumentInstanceOutputProperties outputProperties = new DocumentInstanceOutputProperties();

        outputProperties.setVersionPreserved(true);
        outputProperties.setVerbose(false);

        importFromXML("documentwithattachmentrevisions", outputProperties);

        XWikiDocument document = this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space", "page"),
            this.oldcore.getXWikiContext());

        assertFalse(document.isNew());

        XWikiAttachment attachment = document.getAttachment("attachment.txt");

        assertNotNull(attachment);

        assertEquals(10, attachment.getContentLongSize(null));
        assertTrue(Arrays.equals(new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
            attachment.getContent(this.oldcore.getXWikiContext())));

        XWikiAttachment attachment11 = attachment.getAttachmentRevision("1.1", null);

        assertEquals(3, attachment11.getContentLongSize(null));
        assertContentEquals("1.1", attachment11);
        assertEquals("Comment 1.1", attachment11.getComment());

        XWikiAttachment attachment12 = attachment.getAttachmentRevision("1.2", null);

        assertEquals(3, attachment12.getContentLongSize(null));
        assertContentEquals("1.1", attachment12);
        assertEquals("Comment 1.2", attachment12.getComment());

        XWikiAttachment attachment13 = attachment.getAttachmentRevision("1.3", null);

        assertEquals(10, attachment13.getContentLongSize(null));
        assertTrue(Arrays.equals(new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
            attachment13.getContent(this.oldcore.getXWikiContext())));
        assertEquals("Comment 1.3", attachment13.getComment());
    }

    private void assertContentEquals(String expected, XWikiAttachment attachment) throws IOException, XWikiException
    {
        try (InputStream stream = attachment.getContentInputStream(null)) {
            assertEquals(expected, IOUtils.toString(stream, StandardCharsets.UTF_8));
        }
    }

    @Test
    void importDocumentWithRevisionsAndPreserveVersion() throws FilterException, XWikiException
    {
        DocumentInstanceOutputProperties outputProperties = new DocumentInstanceOutputProperties();

        outputProperties.setVersionPreserved(true);
        outputProperties.setVerbose(false);

        importFromXML("documentwithrevisions", outputProperties);

        XWikiDocument document = this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space", "page"),
            this.oldcore.getXWikiContext());

        assertFalse(document.isNew());

        assertEquals("42.3", document.getVersion());

        Version[] versions = document.getRevisions(this.oldcore.getXWikiContext());

        assertEquals(3, versions.length);

        XWikiDocumentArchive archive = document.getDocumentArchive(this.oldcore.getXWikiContext());

        XWikiDocument document1 = archive.loadDocument(versions[0], this.oldcore.getXWikiContext());

        assertEquals("42.1", document1.getVersion());

        XWikiDocument document2 = archive.loadDocument(versions[1], this.oldcore.getXWikiContext());

        assertEquals("42.2", document2.getVersion());

        XWikiDocument document3 = archive.loadDocument(versions[2], this.oldcore.getXWikiContext());

        assertEquals("42.3", document3.getVersion());
    }
}
