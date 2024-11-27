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
package org.xwiki.icon.macro.internal;

import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.internal.DocumentContextExecutor;
import org.xwiki.icon.Icon;
import org.xwiki.icon.IconException;
import org.xwiki.icon.IconRenderer;
import org.xwiki.icon.IconSet;
import org.xwiki.icon.IconSetManager;
import org.xwiki.icon.macro.DisplayIconMacroParameters;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.async.internal.block.BlockAsyncRendererConfiguration;
import org.xwiki.rendering.async.internal.block.BlockAsyncRendererExecutor;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.macro.MacroContentParser;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReferenceSerializer;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link DisplayIconMacro}.
 *
 * @version $Id$
 */
@OldcoreTest
class DisplayIconMacroTest
{
    private static final DocumentReference ICON_DOCUMENT_REFERENCE = new DocumentReference("xwiki", "Icon", "Document");

    private static final DocumentReference AUTHOR = new DocumentReference("xwiki", "XWiki", "Author");

    @MockComponent
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @MockComponent
    private IconSetManager iconSetManager;

    @MockComponent
    private IconRenderer iconRenderer;

    @MockComponent
    private MacroContentParser macroContentParser;

    @MockComponent
    private DocumentContextExecutor documentContextExecutor;

    @MockComponent
    private BlockAsyncRendererExecutor blockAsyncRendererExecutor;

    @MockComponent
    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    @MockComponent
    @Named("document")
    private UserReferenceSerializer<DocumentReference> documentUserSerializer;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @InjectMockComponents
    private DisplayIconMacro displayIconMacro;

    private final DisplayIconMacroParameters displayIconMacroParameters = new DisplayIconMacroParameters();

    private XWikiDocument iconDocument;

    @BeforeEach
    public void before(MockitoOldcore oldcore) throws Exception
    {
        this.iconDocument = new XWikiDocument(ICON_DOCUMENT_REFERENCE);
        this.iconDocument.setContentAuthorReference(AUTHOR);
        oldcore.getSpyXWiki().saveDocument(this.iconDocument, oldcore.getXWikiContext());

        IconSet documentIconSet = new IconSet("document");
        documentIconSet.addIcon("home", new Icon("homeIcon"));
        documentIconSet.setRenderWiki("icon $icon context {{contextDocumentAuthor /}}");
        documentIconSet.setSourceDocumentReference(ICON_DOCUMENT_REFERENCE);
        when(this.iconSetManager.getIconSet("document")).thenReturn(documentIconSet);

        this.displayIconMacroParameters.setName("home");
        this.displayIconMacroParameters.setIconSet("document");

        when(this.iconRenderer.render(anyString(), any(IconSet.class)))
            .then(invocation -> invocation.getArgument(0, String.class));
        when(this.macroContentParser.parse(anyString(), eq(Syntax.XWIKI_2_1), any(), eq(false), any(), anyBoolean()))
            .then(invocation -> new XDOM(List.of(new WordBlock(invocation.getArgument(0)))));
        when(this.documentContextExecutor.call(any(), any()))
            .then(invocation -> invocation.getArgument(0, Callable.class).call());
        when(this.blockAsyncRendererExecutor.execute(any())).then(invocation -> invocation.getArgument(0,
            BlockAsyncRendererConfiguration.class).getBlock());
        when(this.defaultEntityReferenceSerializer.serialize(ICON_DOCUMENT_REFERENCE))
            .thenReturn("xwiki:Icon.Document");
        when(this.documentUserSerializer.serialize(any())).thenReturn(AUTHOR);
        when(this.documentAccessBridge.getDocumentInstance(ICON_DOCUMENT_REFERENCE)).thenReturn(this.iconDocument);
    }

    @Test
    void accessDenied()
    {
        when(this.contextualAuthorizationManager.hasAccess(Right.VIEW, ICON_DOCUMENT_REFERENCE)).thenReturn(false);
        this.displayIconMacroParameters.setFallback(false);

        MacroExecutionException executionException = assertThrows(MacroExecutionException.class,
            () -> this.displayIconMacro.execute(this.displayIconMacroParameters, null,
                mock(MacroTransformationContext.class)));
        assertEquals(String.format("Current user [%s] doesn't have view rights on the icon set's document [%s]", null,
            ICON_DOCUMENT_REFERENCE), executionException.getMessage());
    }

    @Test
    void fallbackWhenAccessDenied() throws MacroExecutionException, IconException
    {
        when(this.contextualAuthorizationManager.hasAccess(Right.VIEW, ICON_DOCUMENT_REFERENCE)).thenReturn(false);
        IconSet defaultIconSet = mock(IconSet.class);
        when(this.iconSetManager.getDefaultIconSet()).thenReturn(defaultIconSet);

        MacroTransformationContext context = new MacroTransformationContext();
        context.setInline(true);
        List<Block> result =
            this.displayIconMacro.execute(this.displayIconMacroParameters, null, context);
        assertEquals(result, List.of(new MetaDataBlock(List.of(new WordBlock("home")))));
        verify(this.iconRenderer).render("home", defaultIconSet);
        verifyNoInteractions(this.documentContextExecutor);
    }

    @Test
    void throwsWhenRenderingIconFails() throws IconException
    {
        when(this.contextualAuthorizationManager.hasAccess(Right.VIEW, ICON_DOCUMENT_REFERENCE)).thenReturn(true);

        IconException testException = new IconException("Test");
        when(this.iconRenderer.render("home", this.iconSetManager.getIconSet("document"))).thenThrow(testException);

        MacroExecutionException result = assertThrows(MacroExecutionException.class, () ->
            this.displayIconMacro.execute(this.displayIconMacroParameters, null, new MacroTransformationContext()));

        assertEquals("Failed parsing and executing the icon.", result.getMessage());
        assertSame(testException, result.getCause());
    }

    @Test
    void executesInContext() throws Exception
    {
        when(this.contextualAuthorizationManager.hasAccess(Right.VIEW, ICON_DOCUMENT_REFERENCE)).thenReturn(true);

        MacroTransformationContext context = new MacroTransformationContext();
        context.setInline(true);
        List<Block> result =
            this.displayIconMacro.execute(this.displayIconMacroParameters, null, context);
        assertEquals(result, List.of(new MetaDataBlock(List.of(new WordBlock("home")))));
        verify(this.documentContextExecutor).call(any(), eq(this.iconDocument));
    }
}
