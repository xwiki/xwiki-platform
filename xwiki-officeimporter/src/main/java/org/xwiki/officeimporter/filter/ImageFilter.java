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

import java.util.List;
import java.util.Map;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.xml.html.filter.AbstractHTMLFilter;

/**
 * This filter will replace the {@code <img>} tag with corresponding xwiki xhtml syntax. This is required because
 * ordinary xhtml {@code <img>} tags does not render inside wiki pages correctly.
 * 
 * @version $Id$
 * @since 1.8M1
 */
@Component("officeimporter/image")
public class ImageFilter extends AbstractHTMLFilter
{
    /**
     * The {@link DocumentAccessBridge}
     */
    @Requirement
    private DocumentAccessBridge docBridge;

    /**
     * {@inheritDoc}
     */
    public void filter(Document htmlDocument, Map<String, String> cleaningParams)
    {
        String targetDocument = cleaningParams.get("targetDocument");
        List<Element> images = filterDescendants(htmlDocument.getDocumentElement(), new String[] {TAG_IMG});
        for (Element image : images) {
            String src = image.getAttribute(ATTRIBUTE_SRC);
            // TODO : We might have to verify that src is a file name. (a.k.a not a
            // url). There might be cases where documents have embedded urls (images).
            if (!src.equals("") && null != targetDocument && null != docBridge) {
                try {
                    image.setAttribute(ATTRIBUTE_SRC, docBridge.getAttachmentURL(targetDocument, src));
                } catch (Exception ex) {
                    // Do nothing.
                }
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
            Comment beforeComment = htmlDocument.createComment("startimage:" + src);
            Comment afterComment = htmlDocument.createComment("stopimage");
            image.getParentNode().insertBefore(beforeComment, image);
            image.getParentNode().insertBefore(afterComment, image.getNextSibling());
        }
    }
}
