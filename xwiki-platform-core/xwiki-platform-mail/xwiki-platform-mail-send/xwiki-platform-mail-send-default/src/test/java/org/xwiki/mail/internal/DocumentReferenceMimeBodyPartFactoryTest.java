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
package org.xwiki.mail.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.display.internal.DocumentDisplayer;
import org.xwiki.display.internal.DocumentDisplayerParameters;
import org.xwiki.mail.MimeBodyPartFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.api.Attachment;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link org.xwiki.mail.internal.DocumentReferenceMimeBodyPartFactory}.
 *
 * @version $Id$
 * @since 6.1RC1
 */
public class DocumentReferenceMimeBodyPartFactoryTest
{
    @Rule
    public MockitoComponentMockingRule<DocumentReferenceMimeBodyPartFactory> mocker =
        new MockitoComponentMockingRule<>(DocumentReferenceMimeBodyPartFactory.class);

    @Test
    public void create() throws Exception
    {
        XDOM xdom = new XDOM(Collections.<Block>emptyList());

        XWikiDocument document = mock(XWikiDocument.class);
        XWikiAttachment embeddedXWikiAttachment = mock(XWikiAttachment.class, "embedded");
        XWikiAttachment normalXWikiAttachment = mock(XWikiAttachment.class, "normal");
        when(document.getAttachmentList()).thenReturn(Arrays.asList(embeddedXWikiAttachment, normalXWikiAttachment));

        AttachmentConverter attachmentConverter = this.mocker.getInstance(AttachmentConverter.class);
        Attachment embeddedAttachment = mock(Attachment.class, "embedded");
        Attachment normalAttachment = mock(Attachment.class, "normal");
        List<Attachment> attachments = Arrays.asList(embeddedAttachment, normalAttachment);
        when(attachmentConverter.convert(embeddedXWikiAttachment)).thenReturn(embeddedAttachment);
        when(attachmentConverter.convert(normalXWikiAttachment)).thenReturn(normalAttachment);

        DocumentReference reference = new DocumentReference("wiki", "space", "page");
        DocumentAccessBridge bridge = this.mocker.getInstance(DocumentAccessBridge.class);
        when(bridge.getDocument(reference)).thenReturn(document);

        DocumentDisplayer displayer = this.mocker.getInstance(DocumentDisplayer.class);
        when(displayer.display(same(document), any(DocumentDisplayerParameters.class))).thenReturn(xdom);

        BlockRenderer plainTextRenderer = this.mocker.getInstance(BlockRenderer.class, "plain/1.0");
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                ((WikiPrinter) args[1]).print("some text");
                return null;
            }}
        ).when(plainTextRenderer).render(eq(xdom), any(WikiPrinter.class));

        BlockRenderer xhtmlTextRenderer = this.mocker.getInstance(BlockRenderer.class, "xhtml/1.0");
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
             Object[] args = invocation.getArguments();
             ((WikiPrinter) args[1]).print("<img src=\"image.png\"/>");
             return null;
            }}
        ).when(xhtmlTextRenderer).render(eq(xdom), any(WikiPrinter.class));

        MimeBodyPartFactory<String> htmlMimeBodyPartFactory = this.mocker.getInstance(
            new DefaultParameterizedType(null, MimeBodyPartFactory.class, String.class), "text/html");

        this.mocker.getComponentUnderTest().create(reference);

        // The real test is here, we verify that the HTML Body Part Factory is called with the correct parameters.
        Map<String, Object> htmlParameters = new HashMap<>();
        htmlParameters.put("alternative", "some text");
        htmlParameters.put("attachments", attachments);
        verify(htmlMimeBodyPartFactory).create("<img src=\"image.png\"/>", htmlParameters);
    }
}
