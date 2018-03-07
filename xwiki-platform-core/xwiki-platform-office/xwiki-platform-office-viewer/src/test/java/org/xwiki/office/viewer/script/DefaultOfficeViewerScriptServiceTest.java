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
package org.xwiki.office.viewer.script;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;

import org.artofsolving.jodconverter.document.DefaultDocumentFormatRegistry;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.office.viewer.OfficeViewer;
import org.xwiki.office.viewer.OfficeViewerScriptService;
import org.xwiki.officeimporter.converter.OfficeConverter;
import org.xwiki.officeimporter.server.OfficeServer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationManager;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

/**
 * Unit tests for {@link org.xwiki.office.viewer.script.DefaultOfficeViewerScriptService}.
 * 
 * @version $Id$
 */
public class DefaultOfficeViewerScriptServiceTest
{
    /**
     * A component manager that automatically mocks all dependencies of {@link org.xwiki.office.viewer.script.DefaultOfficeViewerScriptService}.
     */
    @Rule
    public MockitoComponentMockingRule<OfficeViewerScriptService> mocker =
        new MockitoComponentMockingRule<OfficeViewerScriptService>(DefaultOfficeViewerScriptService.class,
            ScriptService.class, "officeviewer");

    @Test
    public void isMimeTypeSupported() throws Exception
    {
        OfficeServer officeServer = mocker.getInstance(OfficeServer.class);
        OfficeConverter officeConverter = mock(OfficeConverter.class);
        when(officeServer.getConverter()).thenReturn(officeConverter);
        when(officeConverter.getFormatRegistry()).thenReturn(new DefaultDocumentFormatRegistry());

        for (String mediaType : Arrays.asList("application/vnd.oasis.opendocument.text", "application/msword",
            "application/vnd.oasis.opendocument.presentation", "application/vnd.ms-powerpoint",
            "application/vnd.oasis.opendocument.spreadsheet", "application/vnd.ms-excel")) {
            Assert.assertTrue(mocker.getComponentUnderTest().isMimeTypeSupported(mediaType));
        }
        for (String mediaType : Arrays.asList("foo/bar", "application/pdf")) {
            Assert.assertFalse(mocker.getComponentUnderTest().isMimeTypeSupported(mediaType));
        }
    }

    @Test
    public void view() throws Exception
    {
        Execution execution = mocker.getInstance(Execution.class);
        when(execution.getContext()).thenReturn(new ExecutionContext());

        AttachmentReference attachmentReference =
            new AttachmentReference("file.odt", new DocumentReference("wiki", "Space", "Page"));
        DocumentModelBridge document = mock(DocumentModelBridge.class);
        DocumentAccessBridge documentAccessBridge = mocker.getInstance(DocumentAccessBridge.class);
        when(documentAccessBridge.isDocumentViewable(attachmentReference.getDocumentReference())).thenReturn(true);
        when(documentAccessBridge.getTranslatedDocumentInstance(attachmentReference.getDocumentReference()))
            .thenReturn(document);
        when(document.getSyntax()).thenReturn(Syntax.TEX_1_0);

        XDOM xdom = new XDOM(Collections.<Block> emptyList());
        OfficeViewer viewer = mocker.getInstance(OfficeViewer.class);
        when(viewer.createView(attachmentReference, Collections.<String, String> emptyMap())).thenReturn(xdom);

        BlockRenderer xhtmlRenderer = mocker.registerMockComponent(BlockRenderer.class, "xhtml/1.0");

        Assert.assertEquals("", mocker.getComponentUnderTest().view(attachmentReference));

        TransformationManager transformationManager = mocker.getInstance(TransformationManager.class);
        verify(transformationManager).performTransformations(eq(xdom), notNull(TransformationContext.class));

        verify(xhtmlRenderer).render(eq(xdom), notNull(WikiPrinter.class));
    }

    @Test
    public void viewThrowingException() throws Exception
    {
        ExecutionContext executionContext = new ExecutionContext();
        executionContext.setProperty("officeView.caughtException", "before");

        Execution execution = mocker.getInstance(Execution.class);
        when(execution.getContext()).thenReturn(executionContext);

        Assert.assertNull(mocker.getComponentUnderTest().view(null));

        Exception e = mocker.getComponentUnderTest().getCaughtException();
        Assert.assertTrue(e instanceof NullPointerException);

        verify(mocker.getMockedLogger()).error("Failed to view office document: null", e);
    }
}
