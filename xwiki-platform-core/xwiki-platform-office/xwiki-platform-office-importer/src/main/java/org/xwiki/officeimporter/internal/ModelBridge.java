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
package org.xwiki.officeimporter.internal;

import java.io.InputStream;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.officeimporter.OfficeImporterException;
import org.xwiki.officeimporter.document.OfficeDocumentArtifact;
import org.xwiki.officeimporter.document.XDOMOfficeDocument;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * Bridge to the XWiki model.
 * 
 * @version $Id$
 * @since 14.10.2
 * @since 15.0RC1
 */
@Component(roles = ModelBridge.class)
@Singleton
public class ModelBridge
{
    /**
     * The object used to log messages.
     */
    @Inject
    private Logger logger;

    @Inject
    private ContextualAuthorizationManager contextualAuthorizationManager;

    /**
     * The {@link DocumentAccessBridge} component.
     */
    @Inject
    private DocumentAccessBridge docBridge;

    /**
     * Attempts to save the given {@link XDOMOfficeDocument} into the target wiki page specified by arguments.
     * 
     * @param doc {@link XDOMOfficeDocument} to be saved
     * @param documentReference the reference of the target wiki page
     * @param syntaxId syntax of the target wiki page
     * @param parentReference the reference of the parent wiki page or {@code null}
     * @param title title of the target wiki page or {@code null}
     * @param append whether to append content if the target wiki page exists
     */
    public void save(XDOMOfficeDocument doc, DocumentReference documentReference, String syntaxId,
        DocumentReference parentReference, String title, boolean append) throws Exception
    {
        // First check if the user has edit rights on the target document.
        if (!this.contextualAuthorizationManager.hasAccess(Right.EDIT, documentReference)) {
            String message = "You do not have edit rights on [%s] document.";
            throw new OfficeImporterException(String.format(message, documentReference));
        }

        // Save.
        if (this.docBridge.exists(documentReference) && append) {
            // Check whether existing document's syntax is same as target syntax.
            String currentSyntaxId =
                this.docBridge.getTranslatedDocumentInstance(documentReference).getSyntax().toIdString();
            if (!currentSyntaxId.equals(syntaxId)) {
                String message =
                    "The target page [%s] exists but its syntax [%s] is different from the specified syntax [%s]";
                throw new OfficeImporterException(String.format(message, documentReference, currentSyntaxId, syntaxId));
            }

            // Append the content.
            String currentContent = this.docBridge.getDocumentContent(documentReference, null);
            String newContent = currentContent + "\n" + doc.getContentAsString(syntaxId);
            this.docBridge.setDocumentContent(documentReference, newContent, "Updated by office importer.", false);
        } else {
            this.docBridge.setDocumentSyntaxId(documentReference, syntaxId);
            this.docBridge.setDocumentContent(documentReference, doc.getContentAsString(syntaxId),
                "Created by office importer.", false);

            // Set parent if provided.
            if (null != parentReference) {
                this.docBridge.setDocumentParentReference(documentReference, parentReference);
            }

            // If no title is specified, try to extract one.
            String docTitle = (null == title) ? doc.getTitle() : title;

            // Set title if applicable.
            if (null != docTitle) {
                this.docBridge.setDocumentTitle(documentReference, docTitle);
            }
        }

        // Finally attach all the artifacts into target document.
        attachArtifacts(doc.getArtifactsMap(), documentReference);
    }

    /**
     * Utility method for attaching artifacts into a wiki page.
     * 
     * @param artifactFiles set of artifact files.
     * @param targetDocumentReference target wiki page into which artifacts are to be attached
     */
    private void attachArtifacts(Map<String, OfficeDocumentArtifact> artifactFiles,
        DocumentReference targetDocumentReference)
    {
        artifactFiles.forEach((filename, artifact) -> {
            AttachmentReference attachmentReference = new AttachmentReference(filename, targetDocumentReference);
            try (InputStream is = artifact.getContentInputStream()) {
                this.docBridge.setAttachmentContent(attachmentReference, is);
            } catch (Exception ex) {
                // Log the error as warning and skip the artifact.
                this.logger.warn("Error while attaching artifact: [{}].", ExceptionUtils.getRootCauseMessage(ex));
            }
        });
    }
}
