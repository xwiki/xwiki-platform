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
package org.xwiki.rendering.script;

import java.io.StringReader;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.internal.ContextComponentManagerProvider;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.configuration.ExtendedRenderingConfiguration;
import org.xwiki.rendering.configuration.RenderingConfiguration;
import org.xwiki.rendering.internal.util.XWikiSyntaxEscaper;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroCategoryManager;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.MacroIdFactory;
import org.xwiki.rendering.macro.MacroManager;
import org.xwiki.rendering.macro.descriptor.MacroDescriptor;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.SyntaxRegistry;
import org.xwiki.rendering.transformation.TransformationException;
import org.xwiki.rendering.transformation.TransformationManager;
import org.xwiki.rendering.transformation.XWikiTransformationContext;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorExecutor;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.test.mockito.StringReaderMatcher;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceSerializer;

import ch.qos.logback.classic.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.xwiki.rendering.syntax.Syntax.PLAIN_1_0;
import static org.xwiki.rendering.syntax.Syntax.XHTML_1_0;
import static org.xwiki.rendering.syntax.Syntax.XWIKI_1_0;
import static org.xwiki.rendering.syntax.Syntax.XWIKI_2_0;
import static org.xwiki.rendering.syntax.Syntax.XWIKI_2_1;

/**
 * Unit tests for {@link RenderingScriptService}.
 *
 * @version $Id$
 * @since 3.2M3
 */
@ComponentTest
@ComponentList({ ContextComponentManagerProvider.class, XWikiSyntaxEscaper.class })
class RenderingScriptServiceTest
{
    @InjectMockComponents
    private RenderingScriptService renderingScriptService;

    @MockComponent
    private RenderingConfiguration baseConfiguration;

    @MockComponent
    private ExtendedRenderingConfiguration extendedConfiguration;

    @MockComponent
    private MacroManager macroManager;

    @MockComponent
    private MacroCategoryManager macroCategoryManager;

    @MockComponent
    private MacroIdFactory macroIdFactory;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    @Named("plain/1.0")
    private Parser parser;

    @MockComponent
    @Named("xwiki/2.0")
    private BlockRenderer blockRenderer;

    @MockComponent
    private SyntaxRegistry syntaxRegistry;

    @MockComponent
    private TransformationManager transformationManager;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @MockComponent
    private AuthorExecutor authorExecutor;

    @MockComponent
    @Named("document")
    private UserReferenceSerializer<DocumentReference> userReferenceSerializer;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Test
    void parseAndRender() throws Exception
    {
        when(this.parser.parse(argThat(new StringReaderMatcher("some [[TODO]] stuff"))))
            .thenReturn(new XDOM(List.of()));

        doAnswer(invocationOnMock -> {
            ((WikiPrinter) invocationOnMock.getArguments()[1]).print("some ~[~[TODO]] stuff");
            return null;
        }).when(this.blockRenderer).render(any(XDOM.class), any());

        XDOM xdom = this.renderingScriptService.parse("some [[TODO]] stuff", "plain/1.0");
        assertEquals("some ~[~[TODO]] stuff", this.renderingScriptService.render(xdom, "xwiki/2.0"));
    }

    @Test
    void parseAndRenderWhenErrorInParse() throws Exception
    {
        when(this.parser.parse(new StringReader("some [[TODO]] stuff"))).thenThrow(new ParseException(("error")));
        assertNull(this.renderingScriptService.parse("some [[TODO]] stuff", "plain/1.0"));
    }

    @Test
    void parseAndRenderWhenErrorInRender() throws Exception
    {
        when(this.parser.parse(new StringReader("some [[TODO]] stuff"))).thenReturn(new XDOM(List.of()));

        XDOM xdom = this.renderingScriptService.parse("some [[TODO]] stuff", "plain/1.0");
        assertNull(this.renderingScriptService.render(xdom, "unknown"));
    }

    @Test
    void resolveSyntax() throws Exception
    {
        when(this.syntaxRegistry.resolveSyntax("xwiki/2.1")).thenReturn(XWIKI_2_1);
        assertEquals(XWIKI_2_1, this.renderingScriptService.resolveSyntax("xwiki/2.1"));
    }

    @Test
    void resolveSyntaxWhenInvalid() throws Exception
    {
        when(this.syntaxRegistry.resolveSyntax("unknown")).thenThrow(new ParseException("Invalid syntax"));
        assertNull(this.renderingScriptService.resolveSyntax("unknown"));
    }

    @Test
    void escape10Syntax()
    {
        // Since the logic is pretty simple (prepend every character with an escape character), the below tests are
        // mostly for exemplification.
        // Note: Java escaped string "\\" == "\" (real string).
        assertEquals("\\\\", this.renderingScriptService.escape("\\", XWIKI_1_0));
        assertEquals("\\*\\t\\e\\s\\t\\*", this.renderingScriptService.escape("*test*", XWIKI_1_0));
        assertEquals("\\a\\\\\\\\\\[\\l\\i\\n\\k\\>\\X\\.\\Y\\]",
            this.renderingScriptService.escape("a\\\\[link>X.Y]", XWIKI_1_0));
        assertEquals("\\{\\p\\r\\e\\}\\v\\e\\r\\b\\a\\t\\i\\m\\{\\/\\p\\r\\e\\}",
            this.renderingScriptService.escape("{pre}verbatim{/pre}", XWIKI_1_0));
        assertEquals("\\{\\m\\a\\c\\r\\o\\:\\s\\o\\m\\e\\=\\p\\a\\r\\a\\m\\e\\t\\e\\r\\}"
                + "\\c\\o\\n\\t\\e\\n\\t" + "\\{\\m\\a\\c\\r\\o\\}",
            this.renderingScriptService.escape("{macro:some=parameter}content{macro}", XWIKI_1_0));
    }

    @Test
    void escape20Syntax()
    {
        // Since the logic is pretty simple (prepend every character with an escape character), the below tests are
        // mostly for exemplification.
        assertEquals("~~", this.renderingScriptService.escape("~", XWIKI_2_0));
        assertEquals("~*~*~t~e~s~t~*~*", this.renderingScriptService.escape("**test**", XWIKI_2_0));
        // Note: Java escaped string "\\" == "\" (real string).
        assertEquals("~a~\\~\\~[~[~l~i~n~k~>~>~X~.~Y~]~]",
            this.renderingScriptService.escape("a\\\\[[link>>X.Y]]", XWIKI_2_0));
        assertEquals("~{~{~{~v~e~r~b~a~t~i~m~}~}~}", this.renderingScriptService.escape("{{{verbatim}}}", XWIKI_2_0));
        assertEquals("~{~{~m~a~c~r~o~ ~s~o~m~e~=~'~p~a~r~a~m~e~t~e~r~'~}~}~c~o~n~t~e~n~t~{~{~/~m~a~c~r~o~}~}",
            this.renderingScriptService.escape("{{macro some='parameter'}}content{{/macro}}", XWIKI_2_0));
    }

    @Test
    void escape21Syntax()
    {
        // Since the logic is pretty simple (prepend every character with an escape character), the below tests are
        // mostly for exemplification.
        assertEquals("~~", this.renderingScriptService.escape("~", XWIKI_2_1));
        assertEquals("~*~*~t~e~s~t~*~*", this.renderingScriptService.escape("**test**", XWIKI_2_1));
        // Note: Java escaped string "\\" == "\" (real string).
        assertEquals("~a~\\~\\~[~[~l~i~n~k~>~>~X~.~Y~]~]",
            this.renderingScriptService.escape("a\\\\[[link>>X.Y]]", XWIKI_2_1));
        assertEquals("~{~{~{~v~e~r~b~a~t~i~m~}~}~}", this.renderingScriptService.escape("{{{verbatim}}}", XWIKI_2_1));
        assertEquals("~{~{~m~a~c~r~o~ ~s~o~m~e~=~'~p~a~r~a~m~e~t~e~r~'~}~}~c~o~n~t~e~n~t~{~{~/~m~a~c~r~o~}~}",
            this.renderingScriptService.escape("{{macro some='parameter'}}content{{/macro}}", XWIKI_2_1));
    }

    @Test
    void escapeSpaces()
    {
        assertEquals("\\a\\ \\*\\t\\e\\s\\t\\*", this.renderingScriptService.escape("a *test*", XWIKI_1_0));
        assertEquals("~a~ ~*~*~t~e~s~t~*~*", this.renderingScriptService.escape("a **test**", XWIKI_2_0));
        assertEquals("~a~ ~*~*~t~e~s~t~*~*", this.renderingScriptService.escape("a **test**", XWIKI_2_1));
    }

    @Test
    void escapeNewLines()
    {
        assertEquals("\\a\\\n\\b", this.renderingScriptService.escape("a\nb", XWIKI_1_0));
        assertEquals("~a~\n~b", this.renderingScriptService.escape("a\nb", XWIKI_2_0));
        assertEquals("~a~\n~b", this.renderingScriptService.escape("a\nb", XWIKI_2_1));
    }

    @Test
    void escapeWithNullInput()
    {
        assertNull(this.renderingScriptService.escape(null, XWIKI_2_1), "Unexpected non-null output for null input");
    }

    @Test
    void escapeWithEmptyInput()
    {
        assertEquals("", this.renderingScriptService.escape("", XWIKI_2_1));
    }

    @Test
    void escapeWithNullSyntax()
    {
        assertNull(this.renderingScriptService.escape("anything", null), "Unexpected non-null output for null syntax");
    }

    @Test
    void escapeWithNullInputAndSyntax()
    {
        assertNull(this.renderingScriptService.escape(null, null),
            "Unexpected non-null output for null input and syntax");
    }

    @Test
    void escapeWithUnsupportedSyntax()
    {
        assertNull(this.renderingScriptService.escape("unsupported", XHTML_1_0),
            "Unexpected non-null output for unsupported syntax");
    }

    @Test
    void getMacroDescriptors() throws Exception
    {
        MacroId macroId = new MacroId("macroid");
        when(this.macroManager.getMacroIds(XWIKI_2_1)).thenReturn(Collections.singleton(macroId));
        Macro macro = mock(Macro.class);
        when(this.macroManager.getMacro(macroId)).thenReturn(macro);
        MacroDescriptor descriptor = mock(MacroDescriptor.class);
        when(macro.getDescriptor()).thenReturn(descriptor);

        List<MacroDescriptor> descriptors = this.renderingScriptService.getMacroDescriptors(XWIKI_2_1);
        assertEquals(1, descriptors.size());
        assertSame(descriptor, descriptors.get(0));
    }

    @Test
    void resolveMacroId() throws Exception
    {
        MacroId macroId = new MacroId("info", XWIKI_2_1);
        when(this.macroIdFactory.createMacroId(macroId.toString())).thenReturn(macroId);

        assertSame(macroId, this.renderingScriptService.resolveMacroId(macroId.toString()));
    }

    @Test
    void resolveMacroIdInvalid() throws Exception
    {
        when(this.macroIdFactory.createMacroId("foo")).thenThrow(new ParseException("Invalid macro id"));

        assertNull(this.renderingScriptService.resolveMacroId("foo"));
        assertEquals("Failed to resolve macro id [foo]. Root cause is: [ParseException: Invalid macro id]",
            this.logCapture.getMessage(0));
        assertEquals(Level.WARN, this.logCapture.getLogEvent(0).getLevel());
    }

    @Test
    void getMacroDescriptor() throws Exception
    {
        MacroId macroId = new MacroId("macroId");
        Macro macro = mock(Macro.class);
        MacroDescriptor macroDescriptor = mock(MacroDescriptor.class);

        when(this.macroManager.exists(macroId)).thenReturn(true);
        when(this.macroManager.getMacro(macroId)).thenReturn(macro);
        when(macro.getDescriptor()).thenReturn(macroDescriptor);

        assertSame(macroDescriptor, this.renderingScriptService.getMacroDescriptor(macroId));
    }

    @Test
    void getMacroDescriptorNotFound()
    {
        MacroId macroId = new MacroId("macroId");
        when(this.macroManager.exists(macroId)).thenReturn(false);

        assertNull(this.renderingScriptService.getMacroDescriptor(macroId));
    }

    @Test
    void getMacroCategories()
    {
        MacroId macroId = new MacroId("macroId");
        this.renderingScriptService.getMacroCategories(macroId);
        verify(this.macroCategoryManager).getMacroCategories(macroId);
    }

    @Test
    void createTransformationContext()
    {
        assertNotNull(this.renderingScriptService.createTransformationContext());
    }

    @Test
    void transform() throws Exception
    {
        DocumentReference currentUser = new DocumentReference("xwiki", "XWiki", "CurrentUser");
        when(this.documentAccessBridge.getCurrentUserReference()).thenReturn(currentUser);
        executeCallable();

        DocumentReference contentDocument = new DocumentReference("wiki", "Space", "Page");
        XWikiTransformationContext transformationContext = new XWikiTransformationContext();
        transformationContext.setContentEntityReference(contentDocument);
        Block block = new WordBlock("content");

        this.renderingScriptService.transform(block, transformationContext);

        verify(this.authorization).checkAccess(Right.PROGRAM);
        verify(this.transformationManager).performTransformations(block, transformationContext);
        verify(this.authorExecutor).call(any(), eq(currentUser), eq(contentDocument));
    }

    @Test
    void transformWithoutContentEntityReference() throws Exception
    {
        DocumentReference currentUser = new DocumentReference("xwiki", "XWiki", "CurrentUser");
        when(this.documentAccessBridge.getCurrentUserReference()).thenReturn(currentUser);
        executeCallable();

        XWikiTransformationContext transformationContext = new XWikiTransformationContext();
        Block block = new WordBlock("content");

        this.renderingScriptService.transform(block, transformationContext);

        verify(this.transformationManager).performTransformations(block, transformationContext);
        verify(this.authorExecutor).call(any(), eq(currentUser), isNull());
    }

    @Test
    void transformWithoutProgrammingRight() throws Exception
    {
        AccessDeniedException accessDeniedException = new AccessDeniedException(Right.PROGRAM, null, null);
        doThrow(accessDeniedException).when(this.authorization).checkAccess(Right.PROGRAM);

        XWikiTransformationContext transformationContext = new XWikiTransformationContext();
        Block block = new WordBlock("content");

        AccessDeniedException thrown = assertThrows(AccessDeniedException.class,
            () -> this.renderingScriptService.transform(block, transformationContext));
        assertSame(accessDeniedException, thrown);
        verifyNoInteractions(this.transformationManager, this.authorExecutor);
    }

    @Test
    void transformPropagatesTransformationException() throws Exception
    {
        when(this.documentAccessBridge.getCurrentUserReference())
            .thenReturn(new DocumentReference("xwiki", "XWiki", "CurrentUser"));
        executeCallable();

        XWikiTransformationContext transformationContext = new XWikiTransformationContext();
        Block block = new WordBlock("content");
        TransformationException transformationException = new TransformationException("failed");
        doThrow(transformationException).when(this.transformationManager)
            .performTransformations(block, transformationContext);

        TransformationException thrown = assertThrows(TransformationException.class,
            () -> this.renderingScriptService.transform(block, transformationContext));
        assertSame(transformationException, thrown);
    }

    @Test
    void transformWrapsUnexpectedException() throws Exception
    {
        when(this.documentAccessBridge.getCurrentUserReference())
            .thenReturn(new DocumentReference("xwiki", "XWiki", "CurrentUser"));
        RuntimeException runtimeException = new RuntimeException("boom");
        doThrow(runtimeException).when(this.authorExecutor).call(any(), any(), any());

        XWikiTransformationContext transformationContext = new XWikiTransformationContext();
        Block block = new WordBlock("content");

        TransformationException thrown = assertThrows(TransformationException.class,
            () -> this.renderingScriptService.transform(block, transformationContext));
        assertEquals("Failed to perform transformations", thrown.getMessage());
        assertSame(runtimeException, thrown.getCause());
    }

    @Test
    void convertWithDocumentContentAuthor() throws Exception
    {
        String source = "the content";
        XDOM xdom = setupConversion(source, "rendered");

        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        DocumentModelBridge document = mock();
        when(this.documentAccessBridge.getDocumentInstance(documentReference)).thenReturn(document);
        when(document.getContent()).thenReturn(source);
        DocumentAuthors authors = mock();
        when(document.getAuthors()).thenReturn(authors);
        UserReference contentAuthor = mock();
        when(authors.getContentAuthor()).thenReturn(contentAuthor);
        DocumentReference authorReference = new DocumentReference("wiki", "XWiki", "Author");
        when(this.userReferenceSerializer.serialize(contentAuthor)).thenReturn(authorReference);
        executeCallable();

        XWikiTransformationContext transformationContext = transformationContext(documentReference);

        assertEquals("rendered", this.renderingScriptService.convert(source, transformationContext));

        verify(this.authorization).checkAccess(Right.PROGRAM);
        // The parsed content is made available to the transformations through the context.
        assertSame(xdom, transformationContext.getXDOM());
        verify(this.transformationManager).performTransformations(xdom, transformationContext);
        verify(this.authorExecutor).call(any(), eq(authorReference), eq(documentReference));
    }

    @Test
    void convertWithModifiedDocumentContent() throws Exception
    {
        String source = "the modified content";
        setupConversion(source, "rendered");

        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        DocumentModelBridge document = mock();
        when(this.documentAccessBridge.getDocumentInstance(documentReference)).thenReturn(document);
        when(document.getContent()).thenReturn("the stored content");
        DocumentReference currentUser = new DocumentReference("wiki", "XWiki", "CurrentUser");
        when(this.userReferenceSerializer.serialize(CurrentUserReference.INSTANCE)).thenReturn(currentUser);
        executeCallable();

        XWikiTransformationContext transformationContext = transformationContext(documentReference);

        assertEquals("rendered", this.renderingScriptService.convert(source, transformationContext));

        // Since the content was modified, it is attributed to the current user, not the content author.
        verify(document, never()).getAuthors();
        verify(this.authorExecutor).call(any(), eq(currentUser), eq(documentReference));
    }

    @Test
    void convertWithObjectPropertyAuthor() throws Exception
    {
        String source = "the property content";
        XDOM xdom = setupConversion(source, "rendered");

        ObjectPropertyReference propertyReference =
            new ObjectPropertyReference("wiki", "Space", "Page", "MyClass", "content");
        when(this.documentAccessBridge.getProperty(propertyReference)).thenReturn(source);
        DocumentModelBridge document = mock();
        when(this.documentAccessBridge.getDocumentInstance(propertyReference)).thenReturn(document);
        DocumentAuthors authors = mock();
        when(document.getAuthors()).thenReturn(authors);
        UserReference metadataAuthor = mock();
        when(authors.getEffectiveMetadataAuthor()).thenReturn(metadataAuthor);
        DocumentReference authorReference = new DocumentReference("wiki", "XWiki", "Author");
        when(this.userReferenceSerializer.serialize(metadataAuthor)).thenReturn(authorReference);
        executeCallable();

        XWikiTransformationContext transformationContext = transformationContext(propertyReference);

        assertEquals("rendered", this.renderingScriptService.convert(source, transformationContext));

        verify(this.transformationManager).performTransformations(xdom, transformationContext);
        verify(this.authorExecutor).call(any(), eq(authorReference),
            eq(new DocumentReference("wiki", "Space", "Page")));
    }

    @Test
    void convertWithNullReference() throws Exception
    {
        String source = "content";
        setupConversion(source, "rendered");
        DocumentReference currentUser = new DocumentReference("wiki", "XWiki", "CurrentUser");
        when(this.userReferenceSerializer.serialize(CurrentUserReference.INSTANCE)).thenReturn(currentUser);
        executeCallable();

        XWikiTransformationContext transformationContext = transformationContext(null);

        assertEquals("rendered", this.renderingScriptService.convert(source, transformationContext));

        verify(this.authorExecutor).call(any(), eq(currentUser), isNull());
    }

    @Test
    void convertWithUnrecognizedReference() throws Exception
    {
        String source = "content";
        setupConversion(source, "rendered");
        DocumentReference currentUser = new DocumentReference("wiki", "XWiki", "CurrentUser");
        when(this.userReferenceSerializer.serialize(CurrentUserReference.INSTANCE)).thenReturn(currentUser);
        executeCallable();

        XWikiTransformationContext transformationContext = transformationContext(new WikiReference("wiki"));

        TransformationException exception = assertThrows(TransformationException.class, () ->
            this.renderingScriptService.convert(source, transformationContext));

        assertEquals("Unsupported content entity reference type [WIKI]", exception.getMessage());
    }

    @Test
    void convertWithoutProgrammingRight() throws Exception
    {
        AccessDeniedException accessDeniedException = new AccessDeniedException(Right.PROGRAM, null, null);
        doThrow(accessDeniedException).when(this.authorization).checkAccess(Right.PROGRAM);

        AccessDeniedException thrown = assertThrows(AccessDeniedException.class,
            () -> this.renderingScriptService.convert("content", transformationContext(null)));
        assertSame(accessDeniedException, thrown);
        verifyNoInteractions(this.transformationManager);
    }

    @Test
    void convertWithNullSource()
    {
        NullPointerException thrown = assertThrows(NullPointerException.class,
            () -> this.renderingScriptService.convert(null, transformationContext(null)));
        assertEquals("The source text to convert must be specified", thrown.getMessage());
    }

    @Test
    void convertWithNullContext()
    {
        NullPointerException thrown = assertThrows(NullPointerException.class,
            () -> this.renderingScriptService.convert("content", null));
        assertEquals("The transformation context must be specified", thrown.getMessage());
    }

    @Test
    void convertWithoutSourceSyntax()
    {
        XWikiTransformationContext transformationContext = new XWikiTransformationContext();
        transformationContext.setTargetSyntax(XWIKI_2_0);

        NullPointerException thrown = assertThrows(NullPointerException.class,
            () -> this.renderingScriptService.convert("content", transformationContext));
        assertEquals("The source syntax must be specified in the transformation context", thrown.getMessage());
    }

    @Test
    void convertWithoutTargetSyntax()
    {
        XWikiTransformationContext transformationContext = new XWikiTransformationContext();
        transformationContext.setSyntax(PLAIN_1_0);

        NullPointerException thrown = assertThrows(NullPointerException.class,
            () -> this.renderingScriptService.convert("content", transformationContext));
        assertEquals("The target syntax must be specified in the transformation context", thrown.getMessage());
    }

    @Test
    void convertWrapsUnexpectedException() throws Exception
    {
        String source = "content";
        when(this.parser.parse(argThat(new StringReaderMatcher(source)))).thenThrow(new ParseException("bad"));

        XWikiTransformationContext transformationContext = transformationContext(null);

        TransformationException thrown = assertThrows(TransformationException.class,
            () -> this.renderingScriptService.convert(source, transformationContext));
        assertEquals(ParseException.class, thrown.getCause().getClass());
    }

    @Test
    void convertWithUnknownSourceSyntax()
    {
        XWikiTransformationContext transformationContext = new XWikiTransformationContext();
        // No parser is registered for this syntax.
        transformationContext.setSyntax(XWIKI_2_1);
        transformationContext.setTargetSyntax(XWIKI_2_0);

        assertThrows(ComponentLookupException.class,
            () -> this.renderingScriptService.convert("content", transformationContext));
    }

    private void executeCallable() throws Exception
    {
        doAnswer(invocation -> invocation.getArgument(0, Callable.class).call())
            .when(this.authorExecutor).call(any(), any(), any());
    }

    private XWikiTransformationContext transformationContext(EntityReference contentReference)
    {
        XWikiTransformationContext transformationContext = new XWikiTransformationContext();
        transformationContext.setSyntax(PLAIN_1_0);
        transformationContext.setTargetSyntax(XWIKI_2_0);
        transformationContext.setContentEntityReference(contentReference);
        return transformationContext;
    }

    private XDOM setupConversion(String source, String rendered) throws Exception
    {
        XDOM xdom = new XDOM(List.of(new WordBlock("parsed")));
        when(this.parser.parse(argThat(new StringReaderMatcher(source)))).thenReturn(xdom);
        doAnswer(invocation -> {
            ((WikiPrinter) invocation.getArgument(1)).print(rendered);
            return null;
        }).when(this.blockRenderer).render(eq(xdom), any());
        return xdom;
    }
}
