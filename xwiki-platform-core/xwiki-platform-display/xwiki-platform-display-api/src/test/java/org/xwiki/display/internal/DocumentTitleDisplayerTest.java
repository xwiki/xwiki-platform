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
package org.xwiki.display.internal;

import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;

import javax.inject.Named;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.security.authorization.DocumentAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.VelocityTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DocumentTitleDisplayer}.
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList(DocumentReferenceDequeContext.class)
class DocumentTitleDisplayerTest
{
    @InjectMockComponents
    private DocumentTitleDisplayer documentTitleDisplayer;

    @MockComponent
    private Execution execution;

    @MockComponent
    private EntityReferenceProvider defaultEntityReferenceProvider;

    @MockComponent
    @Named("plain/1.0")
    private Parser plainTextParser;

    @MockComponent
    private ModelContext modelContext;

    @MockComponent
    private DocumentAuthorizationManager authorizationManager;

    @MockComponent
    private DocumentAccessBridge dab;

    @MockComponent
    private VelocityManager velocityManager;

    @BeforeEach
    void configure()
    {
        // The execution context is expected to have the "xwikicontext" property set.
        ExecutionContext executionContext = new ExecutionContext();
        executionContext.setProperty("xwikicontext", new HashMap<String, Object>());
        Mockito.when(this.execution.getContext()).thenReturn(executionContext);
    }

    @Test
    void fallbackOnSpaceNameWhenSpaceHomePageTitleIsEmpty() throws Exception
    {
        when(this.defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT)).thenReturn(
            new EntityReference("Page", EntityType.DOCUMENT));

        DocumentModelBridge document = mock(DocumentModelBridge.class);
        when(document.getDocumentReference()).thenReturn(new DocumentReference("wiki", List.of("Space"), "Page"));

        XDOM titleXDOM = new XDOM(List.of(new WordBlock("Space")));

        when(this.plainTextParser.parse(any(StringReader.class))).thenReturn(titleXDOM);

        DocumentDisplayerParameters params = new DocumentDisplayerParameters();
        params.setTitleDisplayed(true);

        assertSame(titleXDOM, this.documentTitleDisplayer.display(document, params));

        ArgumentCaptor<Reader> argument = ArgumentCaptor.forClass(Reader.class);
        verify(this.plainTextParser).parse(argument.capture());
        assertEquals("Space", IOUtils.toString(argument.getValue()));
    }

    @Test
    void whenSettingTheContextDocumentTheContextWikiIsAlsoSet() throws Exception
    {
        when(this.defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT)).thenReturn(
            new EntityReference("Page", EntityType.DOCUMENT));

        DocumentModelBridge document = mock(DocumentModelBridge.class);
        DocumentReference documentReference = new DocumentReference("wiki", List.of("Space"), "Page");
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.getTitle()).thenReturn("title");

        XDOM titleXDOM = new XDOM(List.of(new WordBlock("title")));

        when(this.plainTextParser.parse(any(StringReader.class))).thenReturn(titleXDOM);

        WikiReference currentWikiReference = new WikiReference("currentWiki");
        when(this.modelContext.getCurrentEntityReference()).thenReturn(currentWikiReference);

        when(this.authorizationManager.hasAccess(eq(Right.SCRIPT), eq(EntityType.DOCUMENT), any(), any()))
            .thenReturn(true);

        VelocityEngine velocityEngine = mock();
        when(this.velocityManager.getVelocityEngine()).thenReturn(velocityEngine);

        doAnswer(invocationOnMock -> {
            Writer output = invocationOnMock.getArgument(1);
            output.write("title");
            return null;
        }).when(velocityEngine).evaluate(any(), any(), any(), any(VelocityTemplate.class));

        DocumentDisplayerParameters params = new DocumentDisplayerParameters();
        params.setTitleDisplayed(true);
        params.setExecutionContextIsolated(true);

        this.documentTitleDisplayer.display(document, params);

        // Check that the context is set.
        verify(this.dab).pushDocumentInContext(any(), same(document));
        verify(this.modelContext).setCurrentEntityReference(documentReference.getWikiReference());

        // Check that the context is restored.
        verify(this.dab).popDocumentFromContext(any());
        verify(this.modelContext).setCurrentEntityReference(currentWikiReference);
    }
}
