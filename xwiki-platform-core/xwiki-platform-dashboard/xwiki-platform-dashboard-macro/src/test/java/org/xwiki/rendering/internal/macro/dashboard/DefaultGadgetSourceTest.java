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
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.executor.ContentExecutor;
import org.xwiki.rendering.macro.dashboard.Gadget;
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
    @InjectMockComponents
    private DefaultGadgetSource defaultGadgetSource;

    private static final String testSource = "XWiki.Test";

    private static final String transformationId = "fooId";

    @MockComponent
    private AuthorExecutor authorExecutor;

    @MockComponent
    private DocumentAuthorizationManager authorizationManager;

    @Mock
    private DocumentReference documentReference;

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
        when(currentReferenceResolver.resolve(testSource)).thenReturn(documentReference);

        Execution execution = componentManager.getInstance(Execution.class);
        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(execution.getContext()).thenReturn(executionContext);
        when(executionContext.getProperty("xwikicontext")).thenReturn(xWikiContext);

        when(xWikiContext.getWiki()).thenReturn(xwiki);
        when(xwiki.getDocument(documentReference, xWikiContext)).thenReturn(xWikiDocument);

        DocumentReferenceResolver<EntityReference> currentReferenceEntityResolver =
            componentManager.getInstance(DocumentReferenceResolver.TYPE_REFERENCE, "current");

        when(currentReferenceEntityResolver.resolve(DefaultGadgetSource.GADGET_CLASS)).thenReturn(gadgetClassReference);
        TransformationContext transformationContext = mock(TransformationContext.class);
        when(macroTransformationContext.getTransformationContext()).thenReturn(transformationContext);
        when(transformationContext.getId()).thenReturn(transformationId);

        VelocityManager velocityManager = componentManager.getInstance(VelocityManager.class);
        when(velocityManager.getVelocityEngine()).thenReturn(velocityEngine);

        when(authorExecutor.call(any(), eq(ownerAuthorReference), eq(ownerSourceReference))).then(invocationOnMock -> {
            Callable callable = (Callable) invocationOnMock.getArguments()[0];
            return callable.call();
        });

        this.contentExecutor =
            componentManager.getInstance(ContentExecutor.TYPE_MACRO_TRANSFORMATION);
        when(contentExecutor.execute(any(), any(), any(), any())).then((Answer<XDOM>) invocationOnMock -> {
            String content = invocationOnMock.getArgument(0);
            XDOM xdom = new XDOM(Collections.singletonList(new WordBlock(content)));
            return xdom;
        });
        when(ownerDocument.getAuthorReference()).thenReturn(ownerAuthorReference);
        when(ownerDocument.getDocumentReference()).thenReturn(ownerSourceReference);
    }

    @Test
    void getGadgets() throws Exception
    {
        assertEquals(new ArrayList<>(), this.defaultGadgetSource.getGadgets(testSource, macroTransformationContext));

        BaseObject gadgetObject1 = mock(BaseObject.class);
        when(xWikiDocument.getXObjects(gadgetClassReference)).thenReturn(Collections.singletonList(gadgetObject1));
        when(gadgetObject1.getOwnerDocument()).thenReturn(ownerDocument);
        when(gadgetObject1.getStringValue("title")).thenReturn("Gadget 1");
        when(gadgetObject1.getLargeStringValue("content")).thenReturn("Some content");
        when(gadgetObject1.getStringValue("position")).thenReturn("0");
        when(gadgetObject1.getNumber()).thenReturn(42);
        when(this.authorizationManager.hasAccess(Right.SCRIPT, EntityType.DOCUMENT, ownerAuthorReference,
            ownerSourceReference)).thenReturn(true);
        when(this.velocityEngine.evaluate(any(), any(), any(), eq("Gadget 1"))).then((Answer<Void>) invocation -> {
            Object[] args = invocation.getArguments();
            StringWriter stringWriter = (StringWriter) args[1];
            String title = "Evaluated velocity version of gadget 1";
            stringWriter.append(title);
            return null;
        });

        List<Gadget> gadgets = this.defaultGadgetSource.getGadgets(testSource, macroTransformationContext);
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
        assertEquals(new ArrayList<>(), this.defaultGadgetSource.getGadgets(testSource, macroTransformationContext));

        BaseObject gadgetObject1 = mock(BaseObject.class);
        when(xWikiDocument.getXObjects(gadgetClassReference)).thenReturn(Collections.singletonList(gadgetObject1));
        when(gadgetObject1.getOwnerDocument()).thenReturn(ownerDocument);
        when(gadgetObject1.getStringValue("title")).thenReturn("Gadget 2");
        when(gadgetObject1.getLargeStringValue("content")).thenReturn("Some other content");
        when(gadgetObject1.getStringValue("position")).thenReturn("2");
        when(gadgetObject1.getNumber()).thenReturn(12);
        when(this.authorizationManager.hasAccess(Right.SCRIPT, EntityType.DOCUMENT, ownerAuthorReference,
            ownerSourceReference)).thenReturn(false);

        List<Gadget> gadgets = this.defaultGadgetSource.getGadgets(testSource, macroTransformationContext);
        assertEquals(1, gadgets.size());
        Gadget gadget = gadgets.get(0);
        assertEquals("Gadget 2", gadget.getTitle().get(0).toString());
        assertEquals("Some other content", gadget.getContent().get(0).toString());
        assertEquals("12", gadget.getId());
        verify(this.contentExecutor)
            .execute(eq("Gadget 2"), any(), any(), any());
        verify(this.contentExecutor)
            .execute(eq("Some other content"), any(), any(), any());
    }
}
