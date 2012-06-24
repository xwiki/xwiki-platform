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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
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
import org.xwiki.xml.html.filter.AbstractHTMLFilter;

/**
 * This filter will replace the {@code <img>} tag with corresponding xwiki xhtml syntax. This is required because
 * ordinary xhtml {@code <img>} tags does not render inside wiki pages correctly.
 * 
 * @version $Id$
 * @since 1.8M1
 */
@Component
@Named("officeimporter/image")
@Singleton
public class ImageFilter extends AbstractHTMLFilter
{
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
    private DocumentReferenceResolver<String> documentStringReferenceResolver;

    @Override
    public void filter(Document htmlDocument, Map<String, String> cleaningParams)
    {
        String targetDocument = cleaningParams.get("targetDocument");
        DocumentReference targetDocumentReference = null;

        List<Element> images = filterDescendants(htmlDocument.getDocumentElement(), new String[] {TAG_IMG});
        for (Element image : images) {
            if (targetDocumentReference == null && !StringUtils.isBlank(targetDocument)) {
                targetDocumentReference = this.documentStringReferenceResolver.resolve(targetDocument);
            }
            String src = image.getAttribute(ATTRIBUTE_SRC);
            if (!StringUtils.isBlank(src) && targetDocumentReference != null) {
                // OpenOffice 3.2 server generates relative image paths, extract image name.
                int separator = src.lastIndexOf("/");
                if (-1 != separator) {
                    src = src.substring(separator + 1);
                }
                try {
                    // We have to decode the image file name in case it contains URL special characters.
                    src = URLDecoder.decode(src, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    // This should never happen.
                }

                // Set image source attribute relative to the reference document.
                AttachmentReference attachmentReference = new AttachmentReference(src, targetDocumentReference);
                image.setAttribute(ATTRIBUTE_SRC, this.documentAccessBridge
                    .getAttachmentURL(attachmentReference, false));

                // The 'align' attribute of images creates a lot of problems. First, OO server has a problem with
                // center aligning images (it aligns them to left). Next, OO server uses <br clear"xxx"> for
                // avoiding content wrapping around images which is not valid xhtml. There for, to be consistent and
                // simple we will remove the 'align' attribute of all the images so that they are all left aligned.
                image.removeAttribute(ATTRIBUTE_ALIGN);
            } else if (src.startsWith("file://")) {
                src = "Missing.png";
                image.setAttribute(ATTRIBUTE_SRC, src);
                image.setAttribute(ATTRIBUTE_ALT, src);
            }
            ResourceReference imageReference = new ResourceReference(src, ResourceType.ATTACHMENT);
            imageReference.setTyped(false);
            Comment beforeComment =
                htmlDocument.createComment("startimage:" + this.xhtmlMarkerSerializer.serialize(imageReference));
            Comment afterComment = htmlDocument.createComment("stopimage");
            image.getParentNode().insertBefore(beforeComment, image);
            image.getParentNode().insertBefore(afterComment, image.getNextSibling());
        }
    }
}
