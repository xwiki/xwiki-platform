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
package org.xwiki.office.preview.internal;

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
import org.xwiki.officeimporter.builder.XDOMOfficeDocumentBuilder;
import org.xwiki.officeimporter.document.XDOMOfficeDocument;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.ImageBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.URLImage;

/**
 * Default implementation of {@link org.xwiki.office.preview.OfficePreviewBuilder}.
 * 
 * @since 2.5M2
 * @version $Id$
 */
@Component
public class DefaultOfficePreviewBuilder extends AbstractOfficePreviewBuilder
{
    /**
     * Used to build xdom documents from office documents.
     */
    @Requirement
    private XDOMOfficeDocumentBuilder builder;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractOfficePreviewBuilder#build(AttachmentReference, String, InputStream, Map)
     */
    protected OfficeDocumentPreview build(AttachmentReference attachmentReference, String version, InputStream data,
        Map<String, String> parameters) throws Exception
    {
        DocumentReference documentReference = attachmentReference.getDocumentReference();

        boolean filterStyles = Boolean.valueOf(parameters.get("filterStyles"));
        XDOMOfficeDocument xdomOfficeDoc =
            builder.build(data, attachmentReference.getName(), documentReference, filterStyles);

        XDOM xdom = xdomOfficeDoc.getContentDocument();
        Map<String, byte[]> artifacts = xdomOfficeDoc.getArtifacts();
        Set<File> temporaryFiles = new HashSet<File>();

        // Process all image blocks.
        List<ImageBlock> imgBlocks = xdom.getChildrenByType(ImageBlock.class, true);
        for (ImageBlock imgBlock : imgBlocks) {
            String imageName = imgBlock.getImage().getName();

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

                    // Collect the temporary file so that it can be cleaned up when the preview is disposed.
                    temporaryFiles.add(tempFile);
                } catch (Exception ex) {
                    String message = "Error while processing artifact image [%s].";
                    getLogger().error(String.format(message, imageName), ex);
                }
            }
        }

        return new OfficeDocumentPreview(attachmentReference, version, xdom, temporaryFiles);
    }
}
