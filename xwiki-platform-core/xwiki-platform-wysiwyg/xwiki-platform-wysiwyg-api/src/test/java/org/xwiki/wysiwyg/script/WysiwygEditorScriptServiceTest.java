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
package org.xwiki.wysiwyg.script;

import java.util.concurrent.Callable;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.MacroIdFactory;
import org.xwiki.rendering.macro.MacroLookupException;
import org.xwiki.rendering.macro.MacroManager;
import org.xwiki.rendering.macro.descriptor.MacroDescriptor;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.security.authorization.AuthorExecutor;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceSerializer;
import org.xwiki.wysiwyg.converter.HTMLConverter;
import org.xwiki.wysiwyg.internal.macro.MacroDescriptorUIFactory;
import org.xwiki.wysiwyg.macro.MacroDescriptorUI;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;
import com.xpn.xwiki.web.XWikiServletRequestStub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link WysiwygEditorScriptService}.
 *
 * @version $Id$
 * @since 17.5.0
 */
@OldcoreTest
@ReferenceComponentList
@SuppressWarnings({ "checkstyle:MultipleStringLiterals", "checkstyle:ClassFanOutComplexity" })
class WysiwygEditorScriptServiceTest
{
    private static final Syntax SYNTAX = Syntax.XWIKI_2_1;

    private static final String EXPECTED_HTML = "obtained source";

    private static final String LONG_TEXT_PROPERTY = "longText1";

    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("xwiki", "Sandbox", "MyPage");

    private static final DocumentReference CLASS_REFERENCE = new DocumentReference("xwiki", "Sandbox", "MyClass");

    @InjectMockComponents
    private WysiwygEditorScriptService editorScriptService;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @MockComponent
    private MacroIdFactory macroIdFactory;

    @MockComponent
    private MacroManager macroManager;

    @MockComponent
    private MacroDescriptorUIFactory macroDescriptorUIFactory;

    @MockComponent
    @Named("document")
    private UserReferenceSerializer<DocumentReference> userDocumentReferenceSerializer;

    @MockComponent
    private AuthorExecutor authorExecutor;

    @MockComponent
    private HTMLConverter htmlConverter;

    @BeforeEach
    void setup() throws Exception
    {
        // Run the code wrapped by the author executor with the current thread so that we can assert its result.
        when(this.authorExecutor.call(any(), any(), any()))
            .then(invocation -> ((Callable<?>) invocation.getArgument(0)).call());

        XWikiContext context = this.oldcore.getXWikiContext();
        // A context document and request are needed to build the security document.
        context.setDoc(new XWikiDocument(new DocumentReference("xwiki", "XWiki", "ContextDocument")));
        context.setRequest(new XWikiServletRequestStub());
    }

    @Test
    void getMacroDescriptorUI() throws ParseException, MacroLookupException
    {
        String macroIdString = "macroId";
        when(this.macroIdFactory.createMacroId(macroIdString)).thenReturn(null);
        assertNull(this.editorScriptService.getMacroDescriptorUI(macroIdString));

        MacroId macroId = mock();
        when(this.macroIdFactory.createMacroId(macroIdString)).thenReturn(macroId);
        when(this.macroManager.exists(macroId, true)).thenReturn(false);
        assertNull(this.editorScriptService.getMacroDescriptorUI(macroIdString));

        when(this.macroManager.exists(macroId, true)).thenReturn(true);
        MacroDescriptor macroDescriptor = mock();
        Macro macro = mock();
        when(macro.getDescriptor()).thenReturn(macroDescriptor);
        when(this.macroManager.getMacro(macroId)).thenReturn(macro);
        MacroDescriptorUI macroDescriptorUI = mock();
        when(this.macroDescriptorUIFactory.buildMacroDescriptorUI(macroDescriptor)).thenReturn(macroDescriptorUI);
        assertEquals(macroDescriptorUI, this.editorScriptService.getMacroDescriptorUI(macroIdString));
    }

    @Test
    void toAnnotatedXHTML() throws Exception
    {
        String source = "some source";

        // The content of the document matches the source, so the document's content author is used.
        UserReference contentAuthor = mock();
        DocumentReference contentAuthorReference = new DocumentReference("xwiki", "XWiki", "ContentAuthor");
        when(this.userDocumentReferenceSerializer.serialize(contentAuthor)).thenReturn(contentAuthorReference);

        XWikiContext context = this.oldcore.getXWikiContext();
        XWikiDocument sourceDocument = new XWikiDocument(DOCUMENT_REFERENCE);
        sourceDocument.setContent(source);
        // Saving a document with dirty content sets the content author to the effective metadata author, so we set
        // both to end up with the expected content author.
        sourceDocument.getAuthors().setEffectiveMetadataAuthor(contentAuthor);
        sourceDocument.getAuthors().setContentAuthor(contentAuthor);
        this.oldcore.getSpyXWiki().saveDocument(sourceDocument, context);

        when(this.htmlConverter.toHTML(source, SYNTAX, DOCUMENT_REFERENCE, false)).thenReturn(EXPECTED_HTML);

        assertEquals(EXPECTED_HTML,
            this.editorScriptService.toAnnotatedXHTML(source, SYNTAX, DOCUMENT_REFERENCE, false));
        verify(this.authorExecutor).call(any(), eq(contentAuthorReference), eq(DOCUMENT_REFERENCE));
    }

    @Test
    void toAnnotatedXHTMLForObjectProperty() throws Exception
    {
        String source = "some source";
        ObjectPropertyReference sourceReference = new ObjectPropertyReference(LONG_TEXT_PROPERTY,
            new ObjectReference("Sandbox.MyClass[0]", DOCUMENT_REFERENCE));

        // The content matches the stored value of the property, so the document's effective metadata author is used.
        UserReference metadataAuthor = mock();
        DocumentReference metadataAuthorReference = new DocumentReference("xwiki", "XWiki", "MetadataAuthor");
        when(this.userDocumentReferenceSerializer.serialize(metadataAuthor)).thenReturn(metadataAuthorReference);

        saveDocumentWithProperty(source, metadataAuthor);

        // The converter and the author executor work at the document level, so the document reference is resolved from
        // the object property reference.
        when(this.htmlConverter.toHTML(source, SYNTAX, DOCUMENT_REFERENCE, false)).thenReturn(EXPECTED_HTML);

        assertEquals(EXPECTED_HTML,
            this.editorScriptService.toAnnotatedXHTML(source, SYNTAX, sourceReference, false));
        verify(this.authorExecutor).call(any(), eq(metadataAuthorReference), eq(DOCUMENT_REFERENCE));
    }

    @Test
    void toAnnotatedXHTMLForModifiedObjectProperty() throws Exception
    {
        String source = "modified source";
        ObjectPropertyReference sourceReference = new ObjectPropertyReference(LONG_TEXT_PROPERTY,
            new ObjectReference("Sandbox.MyClass[0]", DOCUMENT_REFERENCE));

        DocumentReference currentUser = new DocumentReference("xwiki", "XWiki", "CurrentUser");
        this.oldcore.getXWikiContext().setUserReference(currentUser);

        // The content doesn't match the stored value of the property (it was modified by the current user), so the
        // current user's rights are used instead of the author's, to prevent privilege escalation.
        saveDocumentWithProperty("stored source", mock());

        when(this.htmlConverter.toHTML(source, SYNTAX, DOCUMENT_REFERENCE, false)).thenReturn(EXPECTED_HTML);

        assertEquals(EXPECTED_HTML,
            this.editorScriptService.toAnnotatedXHTML(source, SYNTAX, sourceReference, false));
        verify(this.authorExecutor).call(any(), eq(currentUser), eq(DOCUMENT_REFERENCE));
    }

    /**
     * Creates an XClass with a TextArea property, adds an object of that class holding the given value to the source
     * document, and saves it in the mock store.
     *
     * @param propertyValue the value stored in the TextArea property
     * @param metadataAuthor the effective metadata author of the source document
     */
    private void saveDocumentWithProperty(String propertyValue, UserReference metadataAuthor) throws Exception
    {
        XWikiContext context = this.oldcore.getXWikiContext();

        XWikiDocument classDocument = new XWikiDocument(CLASS_REFERENCE);
        classDocument.getXClass().addTextAreaField(LONG_TEXT_PROPERTY, "Long Text", 40, 10);
        this.oldcore.getSpyXWiki().saveDocument(classDocument, context);

        XWikiDocument sourceDocument = new XWikiDocument(DOCUMENT_REFERENCE);
        BaseObject object = sourceDocument.newXObject(CLASS_REFERENCE, context);
        object.setLargeStringValue(LONG_TEXT_PROPERTY, propertyValue);
        sourceDocument.getAuthors().setEffectiveMetadataAuthor(metadataAuthor);
        this.oldcore.getSpyXWiki().saveDocument(sourceDocument, context);
    }
}
