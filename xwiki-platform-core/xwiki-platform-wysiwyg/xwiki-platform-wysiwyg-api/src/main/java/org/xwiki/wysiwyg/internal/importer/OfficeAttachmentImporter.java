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
package org.xwiki.wysiwyg.internal.importer;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.officeimporter.OfficeImporterException;
import org.xwiki.officeimporter.builder.PresentationBuilder;
import org.xwiki.officeimporter.builder.XDOMOfficeDocumentBuilder;
import org.xwiki.officeimporter.document.OfficeDocumentArtifact;
import org.xwiki.officeimporter.document.XDOMOfficeDocument;
import org.xwiki.officeimporter.server.OfficeServer;
import org.xwiki.officeimporter.server.OfficeServer.ServerState;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.store.TemporaryAttachmentSessionsManager;
import org.xwiki.wysiwyg.importer.AttachmentImporter;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;

/**
 * Component used to import office attachments into the content of a WYSIWYG editor.
 * 
 * @version $Id$
 * @since 9.8
 */
@Component
@Singleton
@Named("office")
public class OfficeAttachmentImporter implements AttachmentImporter
{
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
     * Used to access the document converter.
     */
    @Inject
    private OfficeServer officeServer;

    @Inject
    private OfficeMacroImporter officeMacroImporter;

    @Inject
    private ContextualAuthorizationManager authorization;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private TemporaryAttachmentSessionsManager temporaryAttachmentSessionsManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Override
    public String toHTML(AttachmentReference attachmentReference, Map<String, Object> parameters) throws Exception
    {
        boolean filterStyles = Boolean.valueOf(String.valueOf(parameters.get("filterStyles")));
        if (Boolean.valueOf(String.valueOf(parameters.get("useOfficeViewer")))) {
            return this.officeMacroImporter
                .render(this.officeMacroImporter.buildXDOM(attachmentReference, filterStyles));
        } else {
            return maybeConvertAttachmentContent(attachmentReference, filterStyles);
        }
    }

    private String maybeConvertAttachmentContent(AttachmentReference attachmentReference, boolean filterStyles)
        throws Exception
    {
        if (this.authorization.hasAccess(Right.EDIT, attachmentReference)) {
            if (this.documentAccessBridge.getAttachmentVersion(attachmentReference) != null
                || this.temporaryAttachmentSessionsManager.getUploadedAttachment(attachmentReference).isPresent()) {
                if (this.officeServer.getState() == ServerState.CONNECTED) {
                    return convertAttachmentContent(attachmentReference, filterStyles);
                } else {
                    throw new RuntimeException("The office server is not connected.");
                }
            } else {
                throw new RuntimeException(String.format("Attachment not found: [%s].",
                    this.entityReferenceSerializer.serialize(attachmentReference)));
            }
        } else {
            throw new RuntimeException(String.format("Edit right is required in order to import [%s].",
                this.entityReferenceSerializer.serialize(attachmentReference)));
        }
    }

    /**
     * Converts the content of the specified office file to wiki syntax.
     * 
     * @param attachmentReference specifies the office file whose content should be converted
     * @param filterStyles controls whether styles are filtered when converting the HTML produced by the office server
     *            to wiki syntax
     * @return the annotated XHTML text obtained from the specified office document
     * @throws Exception if converting the content of the specified attachment fails
     */
    private String convertAttachmentContent(AttachmentReference attachmentReference, boolean filterStyles)
        throws Exception
    {
        InputStream officeFileStream;
        if (this.documentAccessBridge.getAttachmentVersion(attachmentReference) != null) {
            officeFileStream = documentAccessBridge.getAttachmentContent(attachmentReference);
        } else {
            Optional<XWikiAttachment> uploadedAttachment =
                this.temporaryAttachmentSessionsManager.getUploadedAttachment(attachmentReference);
            if (uploadedAttachment.isPresent()) {
                officeFileStream = uploadedAttachment.get().getContentInputStream(this.contextProvider.get());
            } else {
                throw new OfficeImporterException(
                    String.format("Cannot find temporary uplodaded attachment [%s]", attachmentReference));
            }
        }
        String officeFileName = attachmentReference.getName();
        DocumentReference targetDocRef = attachmentReference.getDocumentReference();
        XDOMOfficeDocument xdomOfficeDocument;
        if (isPresentation(attachmentReference.getName())) {
            xdomOfficeDocument = presentationBuilder.build(officeFileStream, officeFileName, targetDocRef);
        } else {
            xdomOfficeDocument = documentBuilder.build(officeFileStream, officeFileName, targetDocRef, filterStyles);
        }
        // Attach the images extracted from the imported office document to the target wiki document.
        for (Map.Entry<String, OfficeDocumentArtifact> entry : xdomOfficeDocument.getArtifactsMap().entrySet()) {
            String filename = entry.getKey();
            OfficeDocumentArtifact artifact = entry.getValue();
            AttachmentReference artifactReference = new AttachmentReference(filename, targetDocRef);
            try (InputStream is = artifact.getContentInputStream()) {
                this.documentAccessBridge.setAttachmentContent(artifactReference, is);
            }
        }
        String result = xdomOfficeDocument.getContentAsString("annotatedxhtml/1.0");
        xdomOfficeDocument.close();
        return result;
    }

    /**
     * @param fileName a file name
     * @return {@code true} if the specified file is an office presentation, {@code false} otherwise
     */
    private boolean isPresentation(String fileName)
    {
        if (officeServer.getConverter() != null) {
            return officeServer.getConverter().isPresentation(fileName);
        }
        return false;
    }
}
