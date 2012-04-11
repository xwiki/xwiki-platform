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
package org.xwiki.wysiwyg.server.internal.plugin.importer;

import java.io.InputStream;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.gwt.wysiwyg.client.plugin.importer.ImportService;
import org.xwiki.gwt.wysiwyg.client.wiki.Attachment;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.officeimporter.builder.PresentationBuilder;
import org.xwiki.officeimporter.builder.XDOMOfficeDocumentBuilder;
import org.xwiki.officeimporter.document.XDOMOfficeDocument;
import org.xwiki.wysiwyg.server.wiki.EntityReferenceConverter;
import org.xwiki.xml.html.HTMLCleaner;
import org.xwiki.xml.html.HTMLCleanerConfiguration;
import org.xwiki.xml.html.HTMLUtils;

/**
 * XWiki specific implementation of {@link ImportService}.
 * 
 * @version $Id$
 */
@Component
@Singleton
public class XWikiImportService implements ImportService
{
    /**
     * File extensions corresponding to slide presentations.
     */
    private static final List<String> PRESENTATION_FORMAT_EXTENSIONS = Arrays.asList("ppt", "pptx", "odp");

    /**
     * Logger.
     */
    @Inject
    private Logger logger;

    /**
     * The component manager. We need it because we have to access some components dynamically.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * The component used to access the content of the office attachments.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * The component used to convert office presentations to XDOM.
     */
    @Inject
    private PresentationBuilder presentationBuilder;

    /**
     * The component used to convert office text documents to XDOM.
     */
    @Inject
    private XDOMOfficeDocumentBuilder documentBuilder;

    /**
     * The object used to convert between client and server entity reference.
     */
    private final EntityReferenceConverter entityReferenceConverter = new EntityReferenceConverter();

    @Override
    public String cleanOfficeHTML(String htmlPaste, String cleanerHint, Map<String, String> cleaningParams)
    {
        try {
            HTMLCleaner cleaner = componentManager.getInstance(HTMLCleaner.class, cleanerHint);
            HTMLCleanerConfiguration configuration = cleaner.getDefaultConfiguration();
            configuration.setParameters(cleaningParams);
            Document cleanedDocument = cleaner.clean(new StringReader(htmlPaste), configuration);
            HTMLUtils.stripHTMLEnvelope(cleanedDocument);
            return HTMLUtils.toString(cleanedDocument, true, true);
        } catch (Exception e) {
            this.logger.error("Exception while cleaning office HTML content.", e);
            throw new RuntimeException(e.getLocalizedMessage());
        }
    }

    @Override
    public String officeToXHTML(Attachment attachment, Map<String, String> cleaningParams)
    {
        org.xwiki.gwt.wysiwyg.client.wiki.AttachmentReference clientAttachmentReference =
            new org.xwiki.gwt.wysiwyg.client.wiki.AttachmentReference(attachment.getReference());
        try {
            return importAttachment(entityReferenceConverter.convert(clientAttachmentReference), cleaningParams);
        } catch (Exception e) {
            this.logger.error("Exception while importing office document [{}]",
                clientAttachmentReference.getFileName(), e);
            throw new RuntimeException(e.getLocalizedMessage());
        }
    }

    /**
     * Imports an office document that was previously attached to a wiki page.
     * 
     * @param attachmentReference specifies the office document to import
     * @param parameters import parameters; {@code filterStyles} controls whether styles are filtered when importing
     *            office text documents
     * @return the annotated XHTML text obtained from the specified office document
     * @throws Exception is importing the specified attachment fails
     */
    private String importAttachment(AttachmentReference attachmentReference, Map<String, String> parameters)
        throws Exception
    {
        InputStream officeFileStream = documentAccessBridge.getAttachmentContent(attachmentReference);
        String officeFileName = attachmentReference.getName();
        DocumentReference targetDocRef = attachmentReference.getDocumentReference();
        XDOMOfficeDocument xdomOfficeDocument = null;
        if (isPresentation(attachmentReference.getName())) {
            xdomOfficeDocument = presentationBuilder.build(officeFileStream, officeFileName, targetDocRef);
        } else {
            boolean filterStyles = "strict".equals(parameters.get("filterStyles"));
            xdomOfficeDocument = documentBuilder.build(officeFileStream, officeFileName, targetDocRef, filterStyles);
        }
        // Attach the images extracted from the imported office document to the target wiki document.
        for (Map.Entry<String, byte[]> artifact : xdomOfficeDocument.getArtifacts().entrySet()) {
            AttachmentReference artifactReference = new AttachmentReference(artifact.getKey(), targetDocRef);
            documentAccessBridge.setAttachmentContent(artifactReference, artifact.getValue());
        }
        return xdomOfficeDocument.getContentAsString("annotatedxhtml/1.0");
    }

    /**
     * @param fileName a file name
     * @return {@code true} if the specified file is an office presentation, {@code false} otherwise
     */
    private boolean isPresentation(String fileName)
    {
        String fileExtension = StringUtils.substringAfterLast(fileName, ".");
        return PRESENTATION_FORMAT_EXTENSIONS.contains(fileExtension);
    }
}
