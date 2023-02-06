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

import java.util.Collections;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.office.viewer.OfficeResourceViewer;
import org.xwiki.office.viewer.OfficeViewerScriptService;
import org.xwiki.officeimporter.converter.OfficeConverter;
import org.xwiki.officeimporter.server.OfficeServer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.office.viewer.script.DefaultOfficeViewerScriptService}.
 * 
 * @version $Id$
 */
@ComponentTest
class DefaultOfficeViewerScriptServiceTest
{
    @InjectMockComponents(role = OfficeViewerScriptService.class)
    private DefaultOfficeViewerScriptService scriptService;

    @MockComponent
    private OfficeServer officeServer;

    @MockComponent
    private Execution execution;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @MockComponent
    private TransformationManager transformationManager;

    @MockComponent
    private OfficeResourceViewer officeResourceViewer;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @MockComponent
    @Named("xhtml/1.0")
    private BlockRenderer blockRenderer;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Test
    void isMimeTypeSupported() throws Exception
    {
        OfficeConverter officeConverter = mock(OfficeConverter.class);
        when(officeServer.getConverter()).thenReturn(officeConverter);
        when(officeConverter.isConversionSupported("foo/bar", "text/html")).thenReturn(true);
        assertTrue(this.scriptService.isMimeTypeSupported("foo/bar"));
        verify(officeConverter).isConversionSupported("foo/bar", "text/html");
    }

    @Test
    void view() throws Exception
    {
        when(execution.getContext()).thenReturn(new ExecutionContext());

        AttachmentReference attachmentReference =
            new AttachmentReference("file.odt", new DocumentReference("wiki", "Space", "Page"));
        when(this.entityReferenceSerializer.serialize(attachmentReference)).thenReturn("attachment:file.odt");

        DocumentModelBridge document = mock(DocumentModelBridge.class);
        when(documentAccessBridge.isDocumentViewable(attachmentReference.getDocumentReference())).thenReturn(true);
        when(documentAccessBridge.getTranslatedDocumentInstance(attachmentReference.getDocumentReference()))
            .thenReturn(document);
        when(document.getSyntax()).thenReturn(Syntax.TEX_1_0);

        XDOM xdom = new XDOM(Collections.<Block> emptyList());
        when(this.officeResourceViewer.createView(new ResourceReference("attachment:file.odt", ResourceType.ATTACHMENT),
            Collections.<String, String> emptyMap())).thenReturn(xdom);

        assertEquals("", scriptService.view(attachmentReference));
        verify(transformationManager).performTransformations(eq(xdom), any(TransformationContext.class));

        verify(this.blockRenderer).render(eq(xdom), any(WikiPrinter.class));
    }

    @Test
    void viewThrowingException()
    {
        ExecutionContext executionContext = new ExecutionContext();
        executionContext.setProperty("officeView.caughtException", "before");

        when(execution.getContext()).thenReturn(executionContext);
        assertNull(scriptService.view(null));

        assertEquals("Failed to view office document: null", this.logCapture.getMessage(0));
    }
}
