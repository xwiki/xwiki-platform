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
package org.xwiki.rendering.internal.macro.code.source;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.internal.model.reference.CurrentPageReferenceDocumentReferenceResolver;
import org.xwiki.mail.GeneralMailConfiguration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.transformation.macro.CurrentMacroEntityReferenceResolver;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.code.source.CodeMacroSource;
import org.xwiki.rendering.macro.code.source.CodeMacroSourceFactory;
import org.xwiki.rendering.macro.source.MacroContentSourceReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.TextAreaClass.ContentType;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * Validate the various entity macro source factories.
 * 
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
@ComponentList(value = {CurrentMacroEntityReferenceResolver.class, DocumentAttachmentCodeMacroSourceLoader.class,
    DocumentCodeMacroSourceLoader.class, DocumentObjectPropertyCodeMacroSourceLoader.class,
    CurrentPageReferenceDocumentReferenceResolver.class, MacroCodeEntitySoureConfiguration.class})
class EntityCodeMacroSourceFactoryTest
{
    private static final DocumentReference CURRENT_USER = new DocumentReference("xwiki", "XWiki", "user");

    private static final DocumentReference CURRENT_AUTHOR = new DocumentReference("xwiki", "XWiki", "author");

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @InjectMockComponents
    private DocumentCodeMacroSourceFactory documentFactory;

    @InjectMockComponents
    private PageCodeMacroSourceFactory pageFactory;

    @InjectMockComponents
    private DocumentAttachmentCodeMacroSourceFactory documentAttachmentFactory;

    @InjectMockComponents
    private PageAttachmentCodeMacroSourceFactory pageAttachmentFactory;

    @InjectMockComponents
    private DocumentObjectPropertyCodeMacroSourceFactory documentObjectPropertyFactory;

    @InjectMockComponents
    private PageObjectPropertyCodeMacroSourceFactory pageObjectPropertyFactory;

    @MockComponent
    private GeneralMailConfiguration mailConfiguration;

    @MockComponent
    private AuthorizationManager authorization;

    private MacroTransformationContext macroContext;

    @BeforeEach
    public void beforeEach()
    {
        this.macroContext = new MacroTransformationContext();

        MacroBlock macro = new MacroBlock("code", Map.of(), false);
        this.macroContext.setCurrentMacroBlock(macro);

        XDOM xdom = new XDOM(List.of(macro));
        this.macroContext.setXDOM(xdom);

        this.oldcore.getXWikiContext().setUserReference(CURRENT_USER);
        XWikiDocument sdoc = new XWikiDocument(new DocumentReference("wiki", "space", "sdoc"));
        sdoc.setContentAuthorReference(CURRENT_AUTHOR);
        this.oldcore.getXWikiContext().put(XWikiDocument.CKEY_SDOC, sdoc);
    }

    private void assertThrowsCodeMacroSource(CodeMacroSourceFactory factory, MacroContentSourceReference reference)
    {
        assertThrows(MacroExecutionException.class, () -> factory.getContent(reference, this.macroContext));
    }

    private void assertCodeMacroSource(CodeMacroSourceFactory factory, MacroContentSourceReference reference,
        String expectedContent, String expectedLanguage) throws MacroExecutionException
    {
        assertEquals(new CodeMacroSource(reference, expectedContent, expectedLanguage),
            factory.getContent(reference, this.macroContext));
    }

    private void assertFailCodeMacroSource(CodeMacroSourceFactory factory, MacroContentSourceReference reference)
    {
        assertThrows(MacroExecutionException.class, () -> factory.getContent(reference, this.macroContext));
    }

    @Test
    void getContentDocument() throws MacroExecutionException, XWikiException, AccessDeniedException
    {
        assertFailCodeMacroSource(this.documentFactory,
            new MacroContentSourceReference("document", "wiki:Space.Document"));
        assertFailCodeMacroSource(this.pageFactory, new MacroContentSourceReference("page", "wiki:Space/Document"));

        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Document");

        XWikiDocument document =
            this.oldcore.getSpyXWiki().getDocument(documentReference, this.oldcore.getXWikiContext());
        document.setContent("document content");
        this.oldcore.getSpyXWiki().saveDocument(document, this.oldcore.getXWikiContext());

        assertThrowsCodeMacroSource(this.documentFactory,
            new MacroContentSourceReference("document", "wiki:Space.Document"));
        assertThrowsCodeMacroSource(this.documentFactory,
            new MacroContentSourceReference("page", "wiki:Space/Document"));

        when(this.authorization.hasAccess(Right.VIEW, CURRENT_USER, new DocumentReference("wiki", "Space", "Document")))
            .thenReturn(true);

        assertCodeMacroSource(this.documentFactory, new MacroContentSourceReference("document", "wiki:Space.Document"),
            "document content", null);
        assertCodeMacroSource(this.pageFactory, new MacroContentSourceReference("page", "wiki:Space/Document"),
            "document content", null);

        document.setSyntax(Syntax.HTML_5_0);
        this.oldcore.getSpyXWiki().saveDocument(document, this.oldcore.getXWikiContext());

        assertCodeMacroSource(this.documentFactory, new MacroContentSourceReference("document", "wiki:Space.Document"),
            "document content", "html");
        assertCodeMacroSource(this.pageFactory, new MacroContentSourceReference("page", "wiki:Space/Document"),
            "document content", "html");

        doThrow(AccessDeniedException.class).when(this.authorization).checkAccess(Right.VIEW, CURRENT_AUTHOR,
            new DocumentReference("wiki", "Space", "Document"));

        assertThrowsCodeMacroSource(this.documentFactory,
            new MacroContentSourceReference("document", "wiki:Space.Document"));
        assertThrowsCodeMacroSource(this.documentFactory,
            new MacroContentSourceReference("page", "wiki:Space/Document"));
    }

    @Test
    void getContentAttachment() throws MacroExecutionException, XWikiException, IOException
    {
        assertFailCodeMacroSource(this.documentAttachmentFactory,
            new MacroContentSourceReference("attachment", "wiki:Space.Document@attachment.ext"));
        assertFailCodeMacroSource(this.pageAttachmentFactory,
            new MacroContentSourceReference("page_attachment", "wiki:Space/Document/attachment.ext"));

        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Document");

        XWikiDocument document =
            this.oldcore.getSpyXWiki().getDocument(documentReference, this.oldcore.getXWikiContext());
        XWikiAttachment attachment = document.setAttachment("attachment.ext",
            new ByteArrayInputStream("attachment content".getBytes(StandardCharsets.UTF_8)),
            this.oldcore.getXWikiContext());
        this.oldcore.getSpyXWiki().saveDocument(document, this.oldcore.getXWikiContext());

        assertThrowsCodeMacroSource(this.documentAttachmentFactory,
            new MacroContentSourceReference("attachment", "wiki:Space.Document@attachment.ext"));
        assertThrowsCodeMacroSource(this.pageAttachmentFactory,
            new MacroContentSourceReference("page_attachment", "wiki:Space/Document/attachment.ext"));

        when(this.authorization.hasAccess(Right.VIEW, CURRENT_USER, new DocumentReference("wiki", "Space", "Document")))
            .thenReturn(true);

        assertCodeMacroSource(this.documentAttachmentFactory,
            new MacroContentSourceReference("attachment", "wiki:Space.Document@attachment.ext"), "attachment content",
            null);
        assertCodeMacroSource(this.pageAttachmentFactory,
            new MacroContentSourceReference("page_attachment", "wiki:Space/Document/attachment.ext"),
            "attachment content", null);

        attachment.setMimeType("text/html");
        this.oldcore.getSpyXWiki().saveDocument(document, this.oldcore.getXWikiContext());

        assertCodeMacroSource(this.documentAttachmentFactory,
            new MacroContentSourceReference("attachment", "wiki:Space.Document@attachment.ext"), "attachment content",
            "html");
        assertCodeMacroSource(this.pageAttachmentFactory,
            new MacroContentSourceReference("page_attachment", "wiki:Space/Document/attachment.ext"),
            "attachment content", "html");
    }

    @Test
    void getContentObjectProperty() throws MacroExecutionException, XWikiException
    {
        assertFailCodeMacroSource(this.documentObjectPropertyFactory,
            new MacroContentSourceReference("object_property", "wiki:Space.Document^Space.Class.property"));

        LocalDocumentReference classLocalReference = new LocalDocumentReference("Space", "Class");
        DocumentReference classDocumentReference =
            new DocumentReference(classLocalReference, new WikiReference("wiki"));
        XWikiDocument classDocument =
            this.oldcore.getSpyXWiki().getDocument(classDocumentReference, this.oldcore.getXWikiContext());
        BaseClass xclass = classDocument.getXClass();
        xclass.addPasswordField("password", "Password", 30);
        xclass.addEmailField("email", "Email", 30);
        xclass.addTextAreaField("textareasyntax", "Syntax", 5, 30);
        xclass.addTextAreaField("textareaplain", "PURE_TEXT", 5, 30, ContentType.PURE_TEXT);
        xclass.addTextAreaField("textareavelocitywiki", "VELOCITYWIKI", 5, 30, ContentType.VELOCITYWIKI);
        xclass.addTextAreaField("textareavelocitycode", "VELOCITY_CODE", 5, 30, ContentType.VELOCITY_CODE);
        xclass.addTextField("text", "Text", 30);
        xclass.addNumberField("number", "Number", 30, "integer");
        this.oldcore.getSpyXWiki().saveDocument(classDocument, this.oldcore.getXWikiContext());

        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Document");
        XWikiDocument document =
            this.oldcore.getSpyXWiki().getDocument(documentReference, this.oldcore.getXWikiContext());
        BaseObject object = document.newXObject(classLocalReference, this.oldcore.getXWikiContext());
        object.setStringValue("password", "password content");
        object.setStringValue("email", "email content");
        object.setStringValue("text", "text content");
        object.setLargeStringValue("textareasyntax", "syntax content");
        object.setLargeStringValue("textareaplain", "plain content");
        object.setLargeStringValue("textareavelocitywiki", "velocity wiki content");
        object.setLargeStringValue("textareavelocitycode", "velocity code content");
        object.setIntValue("number", 42);
        object.setStringValue("other", "other content");
        this.oldcore.getSpyXWiki().saveDocument(document, this.oldcore.getXWikiContext());

        assertThrowsCodeMacroSource(this.documentObjectPropertyFactory,
            new MacroContentSourceReference("object_property", "wiki:Space.Document^Space.Class.text"));

        when(this.authorization.hasAccess(Right.VIEW, CURRENT_USER, new DocumentReference("wiki", "Space", "Document")))
            .thenReturn(true);

        // text
        assertCodeMacroSource(this.documentObjectPropertyFactory,
            new MacroContentSourceReference("object_property", "wiki:Space.Document^Space.Class.text"), "text content",
            null);
        // number
        assertCodeMacroSource(this.documentObjectPropertyFactory,
            new MacroContentSourceReference("object_property", "wiki:Space.Document^Space.Class.number"), "42", null);
        // other
        assertCodeMacroSource(this.documentObjectPropertyFactory,
            new MacroContentSourceReference("object_property", "wiki:Space.Document^Space.Class.other"),
            "other content", null);
        // email
        assertCodeMacroSource(this.documentObjectPropertyFactory,
            new MacroContentSourceReference("object_property", "wiki:Space.Document^Space.Class.email"),
            "email content", null);
        // textareasyntax
        assertCodeMacroSource(this.documentObjectPropertyFactory,
            new MacroContentSourceReference("object_property", "wiki:Space.Document^Space.Class.textareasyntax"),
            "syntax content", null);
        // textareaplain
        assertCodeMacroSource(this.documentObjectPropertyFactory,
            new MacroContentSourceReference("object_property", "wiki:Space.Document^Space.Class.textareaplain"),
            "plain content", null);
        // textareavelocitywiki
        assertCodeMacroSource(this.documentObjectPropertyFactory,
            new MacroContentSourceReference("object_property", "wiki:Space.Document^Space.Class.textareavelocitywiki"),
            "velocity wiki content", "velocity");
        // textareavelocitycode
        assertCodeMacroSource(this.documentObjectPropertyFactory,
            new MacroContentSourceReference("object_property", "wiki:Space.Document^Space.Class.textareavelocitycode"),
            "velocity code content", "velocity");
        // password
        assertFailCodeMacroSource(this.documentObjectPropertyFactory,
            new MacroContentSourceReference("object_property", "wiki:Space.Document^Space.Class.passwords"));

        document.setSyntax(Syntax.HTML_5_0);
        this.oldcore.getSpyXWiki().saveDocument(document, this.oldcore.getXWikiContext());

        // textareasyntax
        assertCodeMacroSource(this.documentObjectPropertyFactory,
            new MacroContentSourceReference("object_property", "wiki:Space.Document^Space.Class.textareasyntax"),
            "syntax content", "html");
        // textareaplain
        assertCodeMacroSource(this.documentObjectPropertyFactory,
            new MacroContentSourceReference("object_property", "wiki:Space.Document^Space.Class.textareaplain"),
            "plain content", null);

        when(this.mailConfiguration.shouldObfuscate()).thenReturn(true);

        // email
        assertFailCodeMacroSource(this.documentObjectPropertyFactory,
            new MacroContentSourceReference("object_property", "wiki:Space.Document^Space.Class.email"));
    }
}
