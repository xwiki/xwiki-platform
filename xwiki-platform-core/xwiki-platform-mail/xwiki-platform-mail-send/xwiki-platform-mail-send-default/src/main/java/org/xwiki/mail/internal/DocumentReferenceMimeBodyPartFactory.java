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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.display.internal.DocumentDisplayer;
import org.xwiki.display.internal.DocumentDisplayerParameters;
import org.xwiki.mail.MimeBodyPartFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

import com.xpn.xwiki.api.Attachment;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Generates a Mime Body Part to send a wiki page by email, from its reference.
 *
 * @version $Id$
 * @since 6.1RC1
 */
@Component
@Named("xwiki/document")
@Singleton
public class DocumentReferenceMimeBodyPartFactory extends AbstractMimeBodyPartFactory<DocumentReference>
{
    @Inject
    @Named("text/html")
    private MimeBodyPartFactory<String> htmlMimeBodyPartFactory;

    @Inject
    private DocumentAccessBridge docBridge;

    @Inject
    private DocumentDisplayer documentDisplayer;

    @Inject
    @Named("plain/1.0")
    private BlockRenderer plainTextRenderer;

    @Inject
    @Named("xhtml/1.0")
    private BlockRenderer xhtmlRenderer;

    @Inject
    private AttachmentConverter attachmentConverter;

    @Override
    public MimeBodyPart create(DocumentReference reference, Map<String, Object> parameters) throws MessagingException
    {
        // Step 1: Get the XDOM for the document referenced.
        XDOM xdom;
        DocumentModelBridge document;
        try {
            document = this.docBridge.getDocument(reference);
            DocumentDisplayerParameters displayParameters = new DocumentDisplayerParameters();
            displayParameters.setContentTransformed(true);
            displayParameters.setContentTranslated(true);
            xdom = this.documentDisplayer.display(document, displayParameters);
        } catch (Exception e) {
            throw new MessagingException(String.format("Failed to render document for reference [%s]", reference), e);
        }

        // Step 2: Convert the XDOM to both plain text and HTML
        WikiPrinter plainTextPrinter = new DefaultWikiPrinter();
        this.plainTextRenderer.render(xdom, plainTextPrinter);
        String plainText = plainTextPrinter.toString();

        WikiPrinter xhtmlPrinter = new DefaultWikiPrinter();
        this.xhtmlRenderer.render(xdom, xhtmlPrinter);
        String xhtmlText = xhtmlPrinter.toString();

        // Step 2: Get the Document's list of attachments
        // Note: We assume that DocumentModelBridge is actually a XWikiDocument instance. This should be fixed in the
        // the future when the new model will allow retrieving a document's attachments cleanly
        // Since the HTML Body part factory only handles Attachment (and not XWikAttachment) we need to perform the
        // conversion.
        List<Attachment> attachments = convertAttachments(((XWikiDocument) document).getAttachmentList());

        // Step 3: Generate the body part
        Map<String, Object> htmlParameters = new HashMap<>();
        htmlParameters.put("alternative", plainText);
        htmlParameters.put("attachments", attachments);
        return this.htmlMimeBodyPartFactory.create(xhtmlText, htmlParameters);
    }

    private List<Attachment> convertAttachments(List<XWikiAttachment> attachments)
    {
        List<Attachment> attachmentList = new ArrayList<>();
        for (XWikiAttachment attachment : attachments) {
            attachmentList.add(this.attachmentConverter.convert(attachment));
        }
        return attachmentList;
    }
}
