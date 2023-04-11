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
import java.util.Arrays;
import java.util.HashMap;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
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
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DocumentTitleDisplayer}.
 * 
 * @version $Id$
 */
public class DocumentTitleDisplayerTest
{
    @Rule
    public final MockitoComponentMockingRule<DocumentDisplayer> mocker =
        new MockitoComponentMockingRule<DocumentDisplayer>(DocumentTitleDisplayer.class);

    @Before
    public void configure() throws Exception
    {
        // The execution context is expected to have the "xwikicontext" property set.
        Execution mockExecution = this.mocker.getInstance(Execution.class);
        ExecutionContext executionContext = new ExecutionContext();
        executionContext.setProperty("xwikicontext", new HashMap<String, Object>());
        Mockito.when(mockExecution.getContext()).thenReturn(executionContext);
    }

    @Test
    public void fallbackOnSpaceNameWhenSpaceHomePageTitleIsEmpty() throws Exception
    {
        EntityReferenceProvider defaultEntityReferenceProvider = this.mocker.getInstance(EntityReferenceProvider.class);
        when(defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT)).thenReturn(
            new EntityReference("Page", EntityType.DOCUMENT));

        DocumentModelBridge document = mock(DocumentModelBridge.class);
        when(document.getDocumentReference()).thenReturn(new DocumentReference("wiki", Arrays.asList("Space"), "Page"));

        XDOM titleXDOM = new XDOM(Arrays.asList(new WordBlock("Space")));

        Parser plainTextParser = this.mocker.getInstance(Parser.class, "plain/1.0");
        when(plainTextParser.parse(any(StringReader.class))).thenReturn(titleXDOM);

        DocumentDisplayerParameters params = new DocumentDisplayerParameters();
        params.setTitleDisplayed(true);

        assertSame(titleXDOM, this.mocker.getComponentUnderTest().display(document, params));

        ArgumentCaptor<Reader> argument = ArgumentCaptor.forClass(Reader.class);
        verify(plainTextParser).parse(argument.capture());
        assertEquals("Space", IOUtils.toString(argument.getValue()));
    }

    @Test
    public void whenSettingTheContextDocumentTheContextWikiIsAlsoSet() throws Exception
    {
        EntityReferenceProvider defaultEntityReferenceProvider = this.mocker.getInstance(EntityReferenceProvider.class);
        when(defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT)).thenReturn(
            new EntityReference("Page", EntityType.DOCUMENT));

        DocumentModelBridge document = mock(DocumentModelBridge.class);
        DocumentReference documentReference = new DocumentReference("wiki", Arrays.asList("Space"), "Page");
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.getTitle()).thenReturn("title");

        XDOM titleXDOM = new XDOM(Arrays.asList(new WordBlock("title")));

        Parser plainTextParser = this.mocker.getInstance(Parser.class, "plain/1.0");
        when(plainTextParser.parse(any(StringReader.class))).thenReturn(titleXDOM);

        ModelContext modelContext = this.mocker.getInstance(ModelContext.class);
        WikiReference currentWikiReference = new WikiReference("currentWiki");
        when(modelContext.getCurrentEntityReference()).thenReturn(currentWikiReference);

        AuthorizationManager authorizationManager = this.mocker.getInstance(AuthorizationManager.class);
        when(authorizationManager.hasAccess(eq(Right.SCRIPT), any(), any())).thenReturn(true);

        DocumentAccessBridge dab = this.mocker.getInstance(DocumentAccessBridge.class);

        DocumentDisplayerParameters params = new DocumentDisplayerParameters();
        params.setTitleDisplayed(true);
        params.setExecutionContextIsolated(true);

        this.mocker.getComponentUnderTest().display(document, params);

        // Check that the context is set.
        verify(dab).pushDocumentInContext(any(), same(document));
        verify(modelContext).setCurrentEntityReference(documentReference.getWikiReference());

        // Check that the context is restored.
        verify(dab).popDocumentFromContext(any());
        verify(modelContext).setCurrentEntityReference(currentWikiReference);
    }
}
