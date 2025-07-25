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
package org.xwiki.rendering.internal.macro.dashboard;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.executor.ContentExecutor;
import org.xwiki.rendering.macro.dashboard.Gadget;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.security.authorization.AuthorExecutor;
import org.xwiki.security.authorization.DocumentAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
class DefaultGadgetSourceTest
{
    private static final String TEST_SOURCE = "XWiki.Test";

    private static final String TRANSFORMATION_ID = "fooId";

    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("wiki", "XWiki", "Test");

    @InjectMockComponents
    private DefaultGadgetSource defaultGadgetSource;

    @MockComponent
    private AuthorExecutor authorExecutor;

    @MockComponent
    private DocumentAuthorizationManager authorizationManager;

    @MockComponent
    private ContextualLocalizationManager localizationManager;

    @Mock
    private XWikiDocument xWikiDocument;

    @Mock
    private XWikiDocument ownerDocument;

    @Mock
    private DocumentReference ownerAuthorReference;

    @Mock
    private DocumentReference ownerSourceReference;

    @Mock
    private XWiki xwiki;

    @Mock
    private XWikiContext xWikiContext;

    @Mock
    private DocumentReference gadgetClassReference;

    @Mock
    private MacroTransformationContext macroTransformationContext;

    @Mock
    private VelocityEngine velocityEngine;

    private ContentExecutor<MacroTransformationContext> contentExecutor;

    @BeforeEach
    void setup(MockitoComponentManager componentManager) throws Exception
    {
        DocumentReferenceResolver<String> currentReferenceResolver =
            componentManager.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");
        when(currentReferenceResolver.resolve(TEST_SOURCE)).thenReturn(DOCUMENT_REFERENCE);

        Execution execution = componentManager.getInstance(Execution.class);
        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(execution.getContext()).thenReturn(executionContext);
        when(executionContext.getProperty("xwikicontext")).thenReturn(this.xWikiContext);

        when(this.xWikiContext.getWiki()).thenReturn(this.xwiki);
        when(this.xwiki.getDocument(DOCUMENT_REFERENCE, this.xWikiContext)).thenReturn(this.xWikiDocument);

        DocumentReferenceResolver<EntityReference> currentReferenceEntityResolver =
            componentManager.getInstance(DocumentReferenceResolver.TYPE_REFERENCE, "current");

        when(currentReferenceEntityResolver.resolve(DefaultGadgetSource.GADGET_CLASS)).thenReturn(
            this.gadgetClassReference);
        TransformationContext transformationContext = mock(TransformationContext.class);
        when(this.macroTransformationContext.getTransformationContext()).thenReturn(transformationContext);
        when(transformationContext.getId()).thenReturn(TRANSFORMATION_ID);

        VelocityManager velocityManager = componentManager.getInstance(VelocityManager.class);
        when(velocityManager.getVelocityEngine()).thenReturn(this.velocityEngine);

        when(this.authorExecutor.call(any(), eq(this.ownerAuthorReference), eq(this.ownerSourceReference)))
            .then(invocationOnMock -> {
                Callable<?> callable = invocationOnMock.getArgument(0);
                return callable.call();
            });

        this.contentExecutor =
            componentManager.getInstance(ContentExecutor.TYPE_MACRO_TRANSFORMATION);
        when(this.contentExecutor.execute(any(), any(), any(), any())).then((Answer<XDOM>) invocationOnMock -> {
            String content = invocationOnMock.getArgument(0);
            return new XDOM(Collections.singletonList(new WordBlock(content)));
        });
        when(this.ownerDocument.getAuthorReference()).thenReturn(this.ownerAuthorReference);
        when(this.ownerDocument.getDocumentReference()).thenReturn(this.ownerSourceReference);
        when(this.xWikiDocument.getSyntax()).thenReturn(Syntax.XWIKI_2_1);
    }

    @Test
    void getGadgets() throws Exception
    {
        assertEquals(List.of(), this.defaultGadgetSource.getGadgets(TEST_SOURCE, this.macroTransformationContext));

        BaseObject gadgetObject1 = mock(BaseObject.class);
        when(this.xWikiDocument.getXObjects(this.gadgetClassReference)).thenReturn(List.of(gadgetObject1));
        when(gadgetObject1.getOwnerDocument()).thenReturn(this.ownerDocument);
        when(gadgetObject1.getStringValue("title")).thenReturn("Gadget 1");
        when(gadgetObject1.getLargeStringValue("content")).thenReturn("Some content");
        when(gadgetObject1.getStringValue("position")).thenReturn("0");
        when(gadgetObject1.getNumber()).thenReturn(42);
        when(this.authorizationManager.hasAccess(Right.SCRIPT, EntityType.DOCUMENT, this.ownerAuthorReference,
            this.ownerSourceReference)).thenReturn(true);
        when(this.velocityEngine.evaluate(any(), any(), any(), eq("Gadget 1"))).then((Answer<Void>) invocation -> {
            Object[] args = invocation.getArguments();
            StringWriter stringWriter = (StringWriter) args[1];
            String title = "Evaluated velocity version of gadget 1";
            stringWriter.append(title);
            return null;
        });

        List<Gadget> gadgets = this.defaultGadgetSource.getGadgets(TEST_SOURCE, this.macroTransformationContext);
        assertEquals(1, gadgets.size());
        Gadget gadget = gadgets.get(0);
        assertEquals("Evaluated velocity version of gadget 1", gadget.getTitle().get(0).toString());
        assertEquals("Some content", gadget.getContent().get(0).toString());
        assertEquals("42", gadget.getId());
        verify(this.contentExecutor)
            .execute(eq("Evaluated velocity version of gadget 1"), any(), any(), any());
        verify(this.contentExecutor)
            .execute(eq("Some content"), any(), any(), any());
    }

    @Test
    void getGadgetWithoutScriptRight() throws Exception
    {
        assertEquals(new ArrayList<>(), this.defaultGadgetSource.getGadgets(TEST_SOURCE, this.macroTransformationContext));

        BaseObject gadgetObject1 = mock(BaseObject.class);
        when(gadgetObject1.getOwnerDocument()).thenReturn(this.ownerDocument);
        when(gadgetObject1.getStringValue("title")).thenReturn("Gadget 2");
        when(gadgetObject1.getLargeStringValue("content")).thenReturn("Some other content");
        when(gadgetObject1.getStringValue("position")).thenReturn("2");
        when(gadgetObject1.getNumber()).thenReturn(12);

        BaseObject gadgetObject2 = mock();
        when(gadgetObject2.getOwnerDocument()).thenReturn(this.ownerDocument);
        when(gadgetObject2.getStringValue("title")).thenReturn("$services.localization.render('xwiki.gadget2')");
        when(gadgetObject2.getLargeStringValue("content")).thenReturn("Localized content");
        when(gadgetObject2.getStringValue("position")).thenReturn("3");
        when(gadgetObject2.getNumber()).thenReturn(13);

        when(this.localizationManager.getTranslationPlain("xwiki.gadget2")).thenReturn("Translated Title");

        when(this.xWikiDocument.getXObjects(this.gadgetClassReference)).thenReturn(List.of(gadgetObject1,
            gadgetObject2));

        when(this.authorizationManager.hasAccess(Right.SCRIPT, EntityType.DOCUMENT, this.ownerAuthorReference,
            this.ownerSourceReference)).thenReturn(false);

        List<Gadget> gadgets = this.defaultGadgetSource.getGadgets(TEST_SOURCE, this.macroTransformationContext);
        assertEquals(2, gadgets.size());
        Gadget gadget = gadgets.get(0);
        assertEquals("Gadget 2", gadget.getTitle().get(0).toString());
        assertEquals("Some other content", gadget.getContent().get(0).toString());
        assertEquals("12", gadget.getId());
        verify(this.contentExecutor)
            .execute(eq("Gadget 2"), any(), any(), any());
        verify(this.contentExecutor)
            .execute(eq("Some other content"), any(), any(), any());

        Gadget gadget2 = gadgets.get(1);
        assertEquals("Translated Title", gadget2.getTitle().get(0).toString());
        assertEquals("Localized content", gadget2.getContent().get(0).toString());
        assertEquals("13", gadget2.getId());
        verify(this.contentExecutor)
            .execute(eq("Translated Title"), any(), any(), any());
        verify(this.contentExecutor)
            .execute(eq("Localized content"), any(), any(), any());
    }

    @Test
    void getDashboardSourceMetadata()
    {
        when(this.xwiki.getURL(DOCUMENT_REFERENCE, "save", "", "", this.xWikiContext))
            .thenReturn("https://example.com/save");
        when(this.xwiki.getURL(DOCUMENT_REFERENCE, "objectremove", "", "", this.xWikiContext))
            .thenReturn("https://example.com/remove");
        when(this.xwiki.getURL(DOCUMENT_REFERENCE, "objectadd", "", "", this.xWikiContext))
            .thenReturn("https://example.com/add");
        when(this.xwiki.getURL(DOCUMENT_REFERENCE, "view", "", "", this.xWikiContext))
            .thenReturn("https://example.com/view");

        List<Block> metadata = this.defaultGadgetSource.getDashboardSourceMetadata(TEST_SOURCE,
            this.macroTransformationContext);

        assertEquals(1, metadata.size());
        GroupBlock metadataContainer = (GroupBlock) metadata.get(0);
        assertEquals("metadata", metadataContainer.getParameter("class"));
        assertEquals("xwiki/2.1", metadataContainer.getParameter("data-source-syntax"));
        assertEquals(7, metadataContainer.getChildren().size());
    }
}
