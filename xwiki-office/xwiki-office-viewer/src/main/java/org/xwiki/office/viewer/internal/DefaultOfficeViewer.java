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
package org.xwiki.office.viewer.internal;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.officeimporter.builder.PresentationBuilder;
import org.xwiki.officeimporter.builder.XDOMOfficeDocumentBuilder;
import org.xwiki.officeimporter.document.XDOMOfficeDocument;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.ImageBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.URLImage;

/**
 * Default implementation of {@link org.xwiki.office.viewer.OfficeViewer}.
 * 
 * @since 2.5M2
 * @version $Id$
 */
@Component
public class DefaultOfficeViewer extends AbstractOfficeViewer
{
    /**
     * File extensions corresponding to presentation office documents.
     */
    private static final List<String> PRESENTATION_FORMATS = Arrays.asList("ppt", "pptx", "odp");

    /**
     * Used to build XDOM documents from office documents.
     */
    @Requirement
    private XDOMOfficeDocumentBuilder documentBuilder;

    /**
     * Used to build XDOM documents from office presentations.
     */
    @Requirement
    private PresentationBuilder presentationBuilder;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractOfficeViewer#createOfficeDocumentView(AttachmentReference, Map)
     */
    protected OfficeDocumentView createOfficeDocumentView(AttachmentReference attachmentReference,
        Map<String, String> parameters) throws Exception
    {
        XDOMOfficeDocument xdomOfficeDocument = createXDOM(attachmentReference, parameters);
        Set<File> temporaryFiles = processImages(attachmentReference, xdomOfficeDocument);

        XDOM xdom = xdomOfficeDocument.getContentDocument();
        String attachmentVersion = documentAccessBridge.getAttachmentVersion(attachmentReference);
        return new OfficeDocumentView(attachmentReference, attachmentVersion, xdom, temporaryFiles);
    }

    /**
     * Processes all the image blocks in the given XDOM and changes image URL to point to a temporary file for those
     * images that are view artifacts.
     * 
     * @param attachmentReference a reference to the office file that is being viewed; this reference is used to compute
     *            the path to the temporary directory holding the image artifacts
     * @param xdomOfficeDocument the XDOM whose image blocks have to be processed
     * @return the set of temporary files corresponding to image artifacts
     */
    private Set<File> processImages(AttachmentReference attachmentReference, XDOMOfficeDocument xdomOfficeDocument)
    {
        XDOM xdom = xdomOfficeDocument.getContentDocument();
        Map<String, byte[]> artifacts = xdomOfficeDocument.getArtifacts();
        Set<File> temporaryFiles = new HashSet<File>();

        // Process all image blocks.
        List<ImageBlock> imgBlocks = xdom.getChildrenByType(ImageBlock.class, true);
        for (ImageBlock imgBlock : imgBlocks) {
            String imageName = imgBlock.getImage().getReference();

            // Check whether there is a corresponding artifact.
            if (artifacts.containsKey(imageName)) {
                try {
                    // Write the image into a temporary file.
                    File tempFile = saveTemporaryFile(attachmentReference, imageName, artifacts.get(imageName));

                    // Build a URLImage which links to above temporary image file.
                    URLImage urlImage = new URLImage(buildURL(attachmentReference, tempFile.getName()));

                    // Replace the old image block with new one backed by the URLImage.
                    Block newImgBlock = new ImageBlock(urlImage, false, imgBlock.getParameters());
                    imgBlock.getParent().replaceChild(Arrays.asList(newImgBlock), imgBlock);

                    // Collect the temporary file so that it can be cleaned up when the view is disposed from cache.
                    temporaryFiles.add(tempFile);
                } catch (Exception ex) {
                    String message = "Error while processing artifact image [%s].";
                    getLogger().error(String.format(message, imageName), ex);
                }
            }
        }

        return temporaryFiles;
    }

    /**
     * Creates a {@link XDOM} representation of the specified office attachment.
     * 
     * @param attachmentReference a reference to the office file to be parsed into XDOM
     * @param parameters build parameters
     * @return the {@link XDOMOfficeDocument} corresponding to the specified office file
     * @throws Exception if building the XDOM fails
     */
    private XDOMOfficeDocument createXDOM(AttachmentReference attachmentReference, Map<String, String> parameters)
        throws Exception
    {
        DocumentReference documentReference = attachmentReference.getDocumentReference();
        InputStream officeFileStream = documentAccessBridge.getAttachmentContent(attachmentReference);
        String officeFileName = attachmentReference.getName();
        if (isPresentation(officeFileName)) {
            return presentationBuilder.build(officeFileStream, officeFileName, documentReference);
        } else {
            boolean filterStyles = Boolean.valueOf(parameters.get("filterStyles"));
            return documentBuilder.build(officeFileStream, officeFileName, documentReference, filterStyles);
        }
    }

    /**
     * Utility method for checking if a file name corresponds to an office presentation.
     * 
     * @param fileName attachment file name
     * @return {@code true} if the file extension represents an office presentation format, {@code false} otherwise
     */
    private boolean isPresentation(String fileName)
    {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1);
        return PRESENTATION_FORMATS.contains(extension);
    }
}
