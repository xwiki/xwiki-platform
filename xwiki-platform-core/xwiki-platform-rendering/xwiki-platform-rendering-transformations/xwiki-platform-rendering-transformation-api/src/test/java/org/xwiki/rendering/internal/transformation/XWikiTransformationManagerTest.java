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
package org.xwiki.rendering.internal.transformation;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.configuration.RenderingConfiguration;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.XWikiTransformationContext;
import org.xwiki.security.authorization.AuthorExecutor;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link XWikiTransformationManager}.
 *
 * @version $Id$
 */
@ComponentTest
class XWikiTransformationManagerTest
{
    @InjectMockComponents
    private XWikiTransformationManager transformationManager;

    @MockComponent
    private Container container;

    @MockComponent
    private RenderingConfiguration configuration;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    private AuthorExecutor authorExecutor;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @Mock
    private Request request;

    private DocumentReference currentUserReference = new DocumentReference("xwiki", "XWiki", "CurrentUser");

    private DocumentReference contentDocumentReference = new DocumentReference("xwiki", "Test", "ContentDocument");

    @BeforeComponent
    void before() throws Exception
    {
        Provider<ComponentManager> componentManagerProvider = this.componentManager.registerMockComponent(
            new DefaultParameterizedType(null, Provider.class, ComponentManager.class), "context");
        when(componentManagerProvider.get()).thenReturn(this.componentManager);
    }

    @BeforeEach
    void beforeEach()
    {
        when(this.documentAccessBridge.getCurrentUserReference()).thenReturn(this.currentUserReference);
    }

    @Test
    void getTransformationsWhenInQueryString() throws Exception
    {
        when(this.request.getParameter("transformations")).thenReturn("tx1,tx2");
        when(this.container.getRequest()).thenReturn(this.request);

        Transformation tx1 = this.componentManager.registerMockComponent(Transformation.class, "tx1");
        Transformation tx2 = this.componentManager.registerMockComponent(Transformation.class, "tx2");

        List<Transformation> transformations = this.transformationManager.getTransformations();
        assertEquals(2, transformations.size());
        assertSame(tx1, transformations.get(0));
        assertSame(tx2, transformations.get(1));
    }

    @Test
    void getTransformationsWhenInQueryStringAndEmpty()
    {
        when(this.request.getParameter("transformations")).thenReturn("");
        when(this.container.getRequest()).thenReturn(this.request);

        // Specify a config to make sure it's not used (it would fail the test if it were).
        when(this.configuration.getTransformationNames()).thenReturn(Arrays.asList("tx1"));

        List<Transformation> transformations = this.transformationManager.getTransformations();
        assertEquals(0, transformations.size());
    }

    @Test
    void getTransformationsWhenInConfiguration() throws Exception
    {
        when(this.request.getParameter("transformations")).thenReturn(null);
        when(this.container.getRequest()).thenReturn(this.request);
        when(this.configuration.getTransformationNames()).thenReturn(Arrays.asList("tx1"));

        Transformation tx1 = this.componentManager.registerMockComponent(Transformation.class, "tx1");

        List<Transformation> transformations = this.transformationManager.getTransformations();
        assertEquals(1, transformations.size());
        assertSame(tx1, transformations.get(0));
    }

    @Test
    void getTransformationsWhenNoRequest()
    {
        when(this.container.getRequest()).thenReturn(null);

        List<Transformation> transformations = this.transformationManager.getTransformations();
        assertEquals(0, transformations.size());
    }

    @Test
    @SuppressWarnings("null")
    void performTransformations() throws Exception
    {
        XWikiTransformationManager transformationManagerSpy = spy(this.transformationManager);
        doNothing().when(transformationManagerSpy).superPerformTransformations(any(), any());

        doAnswer(invocation -> {
            return invocation.getArgument(0, Callable.class).call();
        }).when(this.authorExecutor).call(any(), eq(this.currentUserReference), eq(this.contentDocumentReference));

        XDOM xdom = new XDOM(List.of(new WordBlock("content")));

        // Call with a generic transformation context.
        TransformationContext transformationContext = new TransformationContext();
        transformationManagerSpy.performTransformations(xdom, transformationContext);

        verify(transformationManagerSpy).superPerformTransformations(xdom, transformationContext);
        verify(this.authorExecutor, never()).call(any(), any(), any());

        // Call with an XWiki transformation context.
        XWikiTransformationContext xwikiTransformationContext = new XWikiTransformationContext();
        xwikiTransformationContext.setContentDocumentReference(this.contentDocumentReference);
        transformationManagerSpy.performTransformations(xdom, xwikiTransformationContext);

        verify(transformationManagerSpy).superPerformTransformations(xdom, xwikiTransformationContext);
        verify(this.authorExecutor).call(any(), eq(this.currentUserReference), eq(this.contentDocumentReference));
    }
}
