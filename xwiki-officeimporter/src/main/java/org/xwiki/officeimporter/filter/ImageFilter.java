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
package org.xwiki.officeimporter.filter;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xwiki.bridge.DocumentAccessBridge;

/**
 * This filter will replace the {@code <img>} tag with corresponding xwiki xhtml syntax. This is required because
 * ordinary xhtml {@code <img>} tags does not render inside wiki pages correctly.
 * 
 * @version $Id: ImageTagFilter.java 13998 2008-11-06 06:29:46Z asiri $
 * @since 1.8M1
 */
public class ImageFilter implements HTMLFilter
{
    private DocumentAccessBridge docBridge;

    private String targetDocument;

    public ImageFilter(DocumentAccessBridge docBridge, String targetDocument)
    {
        this.docBridge = docBridge;
        this.targetDocument = targetDocument;
    }

    /**
     * {@inheritDoc}
     */
    public void filter(Document htmlDocument)
    {
        if (null != targetDocument && null != docBridge) {
            NodeList images = htmlDocument.getElementsByTagName("img");
            for (int i = 0; i < images.getLength(); i++) {
                Node image = images.item(i);
                Node src = null;
                if (image.hasAttributes() && (src = image.getAttributes().getNamedItem("src")) != null) {
                    String fileName = src.getNodeValue();
                    // TODO : We might have to verify that fileName is indeed a file name. (a.k.a not a
                    // url). There might be cases where documents have embedded urls (images).
                    Comment beforeComment = htmlDocument.createComment("startimage:" + fileName);
                    Comment afterComment = htmlDocument.createComment("stopimage");
                    try {
                        src.setNodeValue(docBridge.getAttachmentURL(targetDocument, fileName));
                    } catch (Exception ex) {
                        // Do nothing.
                    }
                    image.getParentNode().insertBefore(beforeComment, image);
                    image.getParentNode().insertBefore(afterComment, image.getNextSibling());
                }
            }
        }
    }
}
