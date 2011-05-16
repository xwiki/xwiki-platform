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
package org.xwiki.officeimporter;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.officeimporter.builder.PresentationBuilder;
import org.xwiki.officeimporter.builder.XDOMOfficeDocumentBuilder;
import org.xwiki.officeimporter.builder.XHTMLOfficeDocumentBuilder;
import org.xwiki.officeimporter.document.XDOMOfficeDocument;
import org.xwiki.officeimporter.document.XHTMLOfficeDocument;
import org.xwiki.officeimporter.openoffice.OpenOfficeConfiguration;
import org.xwiki.officeimporter.openoffice.OpenOfficeManager;
import org.xwiki.officeimporter.openoffice.OpenOfficeManagerException;
import org.xwiki.officeimporter.openoffice.OpenOfficeManager.ManagerState;
import org.xwiki.officeimporter.splitter.TargetDocumentDescriptor;
import org.xwiki.officeimporter.splitter.XDOMOfficeDocumentSplitter;

/**
 * A bridge between velocity and office importer.
 * 
 * @version $Id$
 * @since 1.8M1
 */
public class OfficeImporterVelocityBridge
{
    /**
     * The logger to log.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(OfficeImporterVelocityBridge.class);
    /**
     * File extensions corresponding to slide presentations.
     */
    public static final List<String> PRESENTATION_FORMAT_EXTENSIONS = Arrays.asList("ppt", "pptx", "odp");

    /**
     * The key used to place any error messages while importing office documents.
     */
    public static final String OFFICE_IMPORTER_ERROR = "OFFICE_IMPORTER_ERROR";

    /**
     * The {@link Execution} component.
     */
    private Execution execution;

    /**
     * The {@link DocumentAccessBridge} component.
     */
    private DocumentAccessBridge docBridge;

    /**
     * Used for converting string document names to objects.
     */
    private DocumentReferenceResolver<String> currentMixedDocumentReferenceResolver;

    /**
     * Used to query openoffice server status.
     */
    private OpenOfficeManager officeManager;

    /**
     * Used to query the OpenOffice server configuration.
     */
    private OpenOfficeConfiguration officeConfiguration;

    /**
     * Used for building {@link XHTMLOfficeDocument} instances from office documents.
     */
    private XHTMLOfficeDocumentBuilder xhtmlBuilder;

    /**
     * Used for building {@link XDOMOfficeDocument} instances from office documents.
     */
    private XDOMOfficeDocumentBuilder xdomBuilder;

    /**
     * Used for building {@link XDOMOfficeDocument} instances from office presentations.
     */
    private PresentationBuilder presentationBuilder;

    /**
     * Used to split {@link XDOMOfficeDocument} documents.
     */
    private XDOMOfficeDocumentSplitter xdomSplitter;

    /**
     * Default constructor.
     * 
     * @param componentManager used to lookup for other necessary components.
     * @throws OfficeImporterException if an error occurs while looking up for other required components.
     */
    public OfficeImporterVelocityBridge(ComponentManager componentManager)
        throws OfficeImporterException
    {
        try {
            this.execution = componentManager.lookup(Execution.class);
            this.docBridge = componentManager.lookup(DocumentAccessBridge.class);
            this.currentMixedDocumentReferenceResolver =
                componentManager.lookup(DocumentReferenceResolver.class, "currentmixed");
            this.officeManager = componentManager.lookup(OpenOfficeManager.class);
            this.officeConfiguration = componentManager.lookup(OpenOfficeConfiguration.class);
            this.xhtmlBuilder = componentManager.lookup(XHTMLOfficeDocumentBuilder.class);
            this.xdomBuilder = componentManager.lookup(XDOMOfficeDocumentBuilder.class);
            this.presentationBuilder = componentManager.lookup(PresentationBuilder.class);
            this.xdomSplitter = componentManager.lookup(XDOMOfficeDocumentSplitter.class);
        } catch (Exception ex) {
            throw new OfficeImporterException("Error while initializing office importer velocity bridge.", ex);
        }
    }

    /**
     * Imports the given office document into an {@link XHTMLOfficeDocument}.
     * 
     * @param officeFileStream binary data stream corresponding to input office document.
     * @param officeFileName name of the input office document, this argument is mainly used for determining input
     *            document format where necessary.
     * @param referenceDocument reference wiki document w.r.t which import process is carried out. This argument affects
     *            the attachment URLs generated during the import process where all references to attachments will be
     *            calculated assuming that the attachments are contained on the reference document.
     * @param filterStyles whether to filter styling information associated with the office document's content or not.
     * @return {@link XHTMLOfficeDocument} containing xhtml result of the import operation or null if an error occurs.
     * @since 2.2M1
     */
    public XHTMLOfficeDocument officeToXHTML(InputStream officeFileStream, String officeFileName,
        String referenceDocument, boolean filterStyles)
    {
        try {
            assertConnected();
            return xhtmlBuilder.build(officeFileStream, officeFileName, currentMixedDocumentReferenceResolver
                .resolve(referenceDocument), filterStyles);
        } catch (Exception ex) {
            setErrorMessage(ex.getMessage());
            LOGGER.error(ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * Imports the given {@link XHTMLOfficeDocument} into an {@link XDOMOfficeDocument}.
     * 
     * @param xhtmlOfficeDocument {@link XHTMLOfficeDocument} to be imported.
     * @return {@link XDOMOfficeDocument} containing {@link org.xwiki.rendering.block.XDOM} result of the import
     *         operation or null if an error occurs.
     * @since 2.2M1
     */
    public XDOMOfficeDocument xhtmlToXDOM(XHTMLOfficeDocument xhtmlOfficeDocument)
    {
        try {
            return xdomBuilder.build(xhtmlOfficeDocument);
        } catch (OfficeImporterException ex) {
            setErrorMessage(ex.getMessage());
            LOGGER.error(ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * Imports the given office document into an {@link XDOMOfficeDocument}.
     * 
     * @param officeFileStream binary data stream corresponding to input office document.
     * @param officeFileName name of the input office document, this argument is mainly is used for determining input
     *            document format where necessary.
     * @param referenceDocument reference wiki document w.r.t which import process is carried out. This srgument affects
     *            the attachment URLs generated during the import process where all references to attachments will be
     *            calculated assuming that the attachments are contained on the reference document.
     * @param filterStyles whether to filter styling information associated with the office document's content or not.
     * @return {@link XDOMOfficeDocument} containing {@link org.xwiki.rendering.block.XDOM} result of the import
     *         operation or null if an error occurs.
     * @since 2.2M1
     */
    public XDOMOfficeDocument officeToXDOM(InputStream officeFileStream, String officeFileName,
        String referenceDocument, boolean filterStyles)
    {
        try {
            assertConnected();
            DocumentReference reference = currentMixedDocumentReferenceResolver.resolve(referenceDocument);
            if (isPresentation(officeFileName)) {
                return presentationBuilder.build(officeFileStream, officeFileName, reference);
            } else {
                return xdomBuilder.build(officeFileStream, officeFileName, reference, filterStyles);
            }
        } catch (Exception ex) {
            setErrorMessage(ex.getMessage());
            LOGGER.error(ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * Splits the given {@link XDOMOfficeDocument} into multiple {@link XDOMOfficeDocument} instances according to the
     * specified criterion. This method is useful when a single office document has to be imported and split into
     * multiple wiki pages. An auto generated TOC structure will be returned associated to <b>rootDocumentName</b>
     * {@link org.xwiki.officeimporter.splitter.TargetDocumentDescriptor} entry.
     * 
     * @param xdomDocument {@link XDOMOfficeDocument} to be split.
     * @param headingLevels heading levels defining the split points on the original document.
     * @param namingCriterionHint hint indicating the child pages naming criterion.
     * @param rootDocumentName name of the root document w.r.t which splitting will occur. In the results set the entry
     *            corresponding to <b>rootDocumentName</b> {@link TargetDocumentDescriptor} will hold an auto-generated
     *            TOC structure.
     * @return a map holding {@link XDOMOfficeDocument} fragments against corresponding {@link TargetDocumentDescriptor}
     *         instances or null if an error occurs.
     * @since 2.2M1
     */
    public Map<TargetDocumentDescriptor, XDOMOfficeDocument> split(XDOMOfficeDocument xdomDocument,
        String[] headingLevels, String namingCriterionHint, String rootDocumentName)
    {
        int[] splitLevels = new int[headingLevels.length];
        for (int i = 0; i < headingLevels.length; i++) {
            splitLevels[i] = Integer.parseInt(headingLevels[i]);
        }
        try {
            return xdomSplitter.split(xdomDocument, splitLevels, namingCriterionHint,
                currentMixedDocumentReferenceResolver.resolve(rootDocumentName));
        } catch (OfficeImporterException ex) {
            setErrorMessage(ex.getMessage());
            LOGGER.error(ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * Attempts to save the given {@link XDOMOfficeDocument} into the target wiki page specified by arguments.
     * 
     * @param doc {@link XDOMOfficeDocument} to be saved.
     * @param target name of the target wiki page.
     * @param syntaxId syntax of the target wiki page.
     * @param parent name of the parent wiki page or null.
     * @param title title of the target wiki page or null.
     * @param append whether to append content if the target wiki page exists.
     * @return true if the operation completes successfully, false otherwise.
     */
    public boolean save(XDOMOfficeDocument doc, String target, String syntaxId, String parent, String title,
        boolean append)
    {
        try {
            DocumentReference docReference = currentMixedDocumentReferenceResolver.resolve(target);
            DocumentReference parentReference = currentMixedDocumentReferenceResolver.resolve(parent);

            // First check if the user has edit rights on the target document.
            if (!docBridge.isDocumentEditable(docReference)) {
                String message = "You do not have edit rights on [%s] document.";
                throw new OfficeImporterException(String.format(message, target));
            }

            // Save.
            if (docBridge.exists(docReference) && append) {
                // Check whether existing document's syntax is same as target syntax.
                String currentSyntaxId = docBridge.getDocument(docReference).getSyntax().toIdString();
                if (!currentSyntaxId.equals(syntaxId)) {
                    String message =
                        "Target document [%s] exists but it's sytax [%s] is different from specified syntax [%s]";
                    throw new OfficeImporterException(String.format(message, target, currentSyntaxId, syntaxId));
                }

                // Append the content.
                String currentContent = docBridge.getDocumentContent(docReference, null);
                String newContent = currentContent + "\n" + doc.getContentAsString(syntaxId);
                docBridge.setDocumentContent(docReference, newContent, "Updated by office importer.", false);
            } else {
                docBridge.setDocumentSyntaxId(docReference, syntaxId);
                docBridge.setDocumentContent(docReference, doc.getContentAsString(syntaxId),
                    "Created by office importer.", false);

                // Set parent if provided.
                if (null != parent) {
                    docBridge.setDocumentParentReference(docReference, parentReference);
                }

                // If no title is specified, try to extract one.
                String docTitle = (null == title) ? doc.getTitle() : title;

                // Set title if applicable.
                if (null != docTitle) {
                    docBridge.setDocumentTitle(docReference, docTitle);
                }
            }

            // Finally attach all the artifacts into target document.
            attachArtifacts(doc.getArtifacts(), docReference);

            return true;
        } catch (OfficeImporterException ex) {
            setErrorMessage(ex.getMessage());
            LOGGER.error(ex.getMessage(), ex);
        } catch (Exception ex) {
            String message = "Error while saving document [%s].";
            message = String.format(message, target);
            setErrorMessage(message);
            LOGGER.error(message, ex);
        }
        return false;
    }

    /**
     * @return an error message set inside current execution (during import process) or null.
     */
    public String getErrorMessage()
    {
        return (String) execution.getContext().getProperty(OFFICE_IMPORTER_ERROR);
    }

    /**
     * Utility method for setting an error message inside current execution.
     * 
     * @param message error message.
     */
    private void setErrorMessage(String message)
    {
        execution.getContext().setProperty(OFFICE_IMPORTER_ERROR, message);
    }

    /**
     * Checks if the connection to the OpenOffice server has been established. If the OpenOffice server has been
     * configured to start automatically then we make an attempt to start it (usually this means that the OpenOffice
     * server failed to start when XE started and so we try one more time to connect).
     * 
     * @throws OfficeImporterException if the connection to the OpenOffice server is not established
     * @throws OpenOfficeManagerException if the attempt to start the OpenOffice server failed
     */
    private void assertConnected() throws OfficeImporterException, OpenOfficeManagerException
    {
        boolean connected = officeManager.getState().equals(ManagerState.CONNECTED);
        if (!connected) {
            // Check if the OpenOffice server was configured to start automatically.
            if (officeConfiguration.isAutoStart()) {
                // The OpenOffice server probably failed to start automatically when XE started. Try one more time to
                // connect.
                officeManager.start();
                connected = officeManager.getState().equals(ManagerState.CONNECTED);
            }
            if (!connected) {
                throw new OfficeImporterException("OpenOffice server unavailable.");
            }
        }
    }

    /**
     * Utility method for checking if a file name corresponds to an office presentation.
     * 
     * @param officeFileName office file name.
     * @return true if the file name / extension represents an office presentation format.
     */
    private boolean isPresentation(String officeFileName)
    {
        String extension = officeFileName.substring(officeFileName.lastIndexOf('.') + 1);
        return PRESENTATION_FORMAT_EXTENSIONS.contains(extension);
    }

    /**
     * Utility method for attaching artifacts into a wiki page.
     * 
     * @param artifacts map of artifact content against their names.
     * @param targetDocumentReference target wiki page into which artifacts are to be attached.
     */
    private void attachArtifacts(Map<String, byte[]> artifacts, DocumentReference targetDocumentReference)
    {
        for (Map.Entry<String, byte[]> artifact : artifacts.entrySet()) {
            AttachmentReference attachmentReference =
                new AttachmentReference(artifact.getKey(), targetDocumentReference);
            try {
                docBridge.setAttachmentContent(attachmentReference, artifact.getValue());
            } catch (Exception ex) {
                // Log the error and skip the artifact.
                LOGGER.error("Error while attaching artifact.", ex);
            }
        }
    }
}
