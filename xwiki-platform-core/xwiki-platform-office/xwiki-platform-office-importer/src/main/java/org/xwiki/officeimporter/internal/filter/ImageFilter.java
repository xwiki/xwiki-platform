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
package org.xwiki.officeimporter.internal.filter;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.slf4j.Logger;
import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.renderer.reference.ResourceReferenceSerializer;
import org.xwiki.xml.XMLUtils;
import org.xwiki.xml.html.filter.AbstractHTMLFilter;

import com.github.ooxi.jdatauri.DataUri;

/**
 * This filter performs the following transformations on the {@code <img>} tags:
 * <ul>
 * <li>Changes the image source to point to the attached file and adds the XHTML markers (comments) required in order to
 * convert the XHTML to the right wiki syntax. For this you need to specify the "targetDocument" cleaning
 * parameter.</li>
 * <li>Collects the images embedded through the Data URI scheme when the "attachEmbeddedImages" cleaning parameter is
 * set to true. The result can be accessed from the user data associated with the filtered document, under the
 * "embeddedImages" key.</li>
 * <li>Removes the "align" attribute as it can cause problems. First, the office server has a problem with center
 * aligning images (it aligns them to left). Then, the office server uses {@code <br clear"xxx">} to avoid content
 * wrapping around images which is not valid XHTML.</li>
 * </ul>
 * 
 * @version $Id$
 * @since 1.8M1
 */
@Component
@Named("officeimporter/image")
@Singleton
public class ImageFilter extends AbstractHTMLFilter
{
    private static final String UTF_8 = "UTF-8";

    private static final String EMBEDDED_IMAGES = "embeddedImages";

    @Inject
    private Logger logger;

    /**
     * The {@link DocumentAccessBridge} component.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * Used to serialize the image reference as XHTML comment.
     */
    @Inject
    @Named("xhtmlmarker")
    private ResourceReferenceSerializer xhtmlMarkerSerializer;

    /**
     * The component used to parse string document references.
     */
    @Inject
    @Named("currentmixed")
    private DocumentReferenceResolver<String> stringDocumentReferenceResolver;

    @Override
    public void filter(Document htmlDocument, Map<String, String> cleaningParams)
    {
        String targetDocumentName = cleaningParams.get("targetDocument");
        DocumentReference targetDocumentReference =
            targetDocumentName == null ? null : this.stringDocumentReferenceResolver.resolve(targetDocumentName);

        boolean attachEmbeddedImages = Boolean.valueOf(cleaningParams.get("attachEmbeddedImages"));
        if (attachEmbeddedImages) {
            htmlDocument.setUserData(EMBEDDED_IMAGES, new HashMap<String, byte[]>(), null);
        }

        List<Element> images = filterDescendants(htmlDocument.getDocumentElement(), new String[] {TAG_IMG});
        for (Element image : images) {
            Attr source = image.getAttributeNode(ATTRIBUTE_SRC);
            if (source != null && targetDocumentReference != null) {
                filterImageSource(source, targetDocumentReference);
            }

            // The 'align' attribute of images creates a lot of problems. First,the office server has a problem with
            // center aligning images (it aligns them to left). Next, the office server uses <br clear"xxx"> for
            // avoiding content wrapping around images which is not valid XHTML. There for, to be consistent and simple
            // we will remove the 'align' attribute of all the images so that they are all left aligned.
            image.removeAttribute(ATTRIBUTE_ALIGN);
        }
    }

    private void filterImageSource(Attr source, DocumentReference targetDocumentReference)
    {
        String fileName = null;
        try {
            fileName = getFileName(source);
        } catch (Exception e) {
            this.logger.warn("Failed to extract the image file name. Root cause is [{}]",
                ExceptionUtils.getRootCauseMessage(e));
            this.logger.debug("Full stacktrace is: ", e);
        }
        if (StringUtils.isEmpty(fileName)) {
            return;
        }

        // Set image source attribute relative to the reference document.
        AttachmentReference attachmentReference = new AttachmentReference(fileName, targetDocumentReference);
        source.setValue(this.documentAccessBridge.getAttachmentURL(attachmentReference, false));

        ResourceReference imageReference = new ResourceReference(fileName, ResourceType.ATTACHMENT);
        imageReference.setTyped(false);
        Comment beforeComment = source.getOwnerDocument().createComment(
            XMLUtils.escapeXMLComment("startimage:" + this.xhtmlMarkerSerializer.serialize(imageReference)));
        Comment afterComment = source.getOwnerDocument().createComment("stopimage");
        Element image = source.getOwnerElement();
        image.getParentNode().insertBefore(beforeComment, image);
        image.getParentNode().insertBefore(afterComment, image.getNextSibling());
    }

    private String getFileName(Attr source) throws MimeTypeException
    {
        String value = source.getValue();
        String fileName = null;
        @SuppressWarnings("unchecked")
        Map<String, byte[]> embeddedImages =
            (Map<String, byte[]>) source.getOwnerDocument().getUserData(EMBEDDED_IMAGES);
        if (embeddedImages != null && value.startsWith("data:")) {
            // An image embedded using the Data URI scheme.
            DataUri dataURI = DataUri.parse(value, Charset.forName(UTF_8));
            fileName = dataURI.getFilename();
            if (StringUtils.isEmpty(fileName)) {
                fileName = String.valueOf(Math.abs(dataURI.hashCode()));
                if (!StringUtils.isEmpty(dataURI.getMime())) {
                    String extension = MimeTypes.getDefaultMimeTypes().forName(dataURI.getMime()).getExtension();
                    fileName += extension;
                }
            }
            embeddedImages.put(fileName, dataURI.getData());
        } else if (!value.contains("://")) {
            // A relative path.
            int separator = value.lastIndexOf('/');
            fileName = separator < 0 ? value : value.substring(separator + 1);
            try {
                // We have to decode the image file name in case it contains URL special characters.
                fileName = URLDecoder.decode(fileName, UTF_8);
            } catch (Exception e) {
                // This shouldn't happen. Use the encoded image file name.
            }
        }
        return fileName;
    }
}
