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

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.officeimporter.filter.common.AbstractHTMLFilter;
import org.xwiki.officeimporter.internal.cleaner.WysiwygHTMLCleaner;

/**
 * This filter will replace the {@code <img>} tag with corresponding xwiki xhtml syntax. This is required because
 * ordinary xhtml {@code <img>} tags does not render inside wiki pages correctly.
 * 
 * @version $Id: ImageTagFilter.java 13998 2008-11-06 06:29:46Z asiri $
 * @since 1.8M1
 */
public class ImageFilter extends AbstractHTMLFilter
{
    /**
     * The {@link DocumentAccessBridge}
     */
    private DocumentAccessBridge docBridge;

    /**
     * Target document name.
     */
    private String targetDocument;

    /**
     * Constructs an {@link ImageFilter} with the given {@link DocumentAccessBridge} and the targetDocument. If this
     * constructor is used, all cleaning operations will be done w.r.t the targetDocument.
     * 
     * @param docBridge the {@link DocumentAccessBridge}
     * @param targetDocument target document.
     */
    public ImageFilter(DocumentAccessBridge docBridge, String targetDocument)
    {
        this.docBridge = docBridge;
        this.targetDocument = targetDocument;
    }

    /**
     * Default constructor. This constructor is used by {@link WysiwygHTMLCleaner} to strip images from html.
     */
    public ImageFilter()
    {

    }

    /**
     * {@inheritDoc}
     */
    public void filter(Document htmlDocument)
    {
        List<Element> images = filterChildren(htmlDocument.getDocumentElement(), "img");
        for (Element image : images) {
            String src = image.getAttribute("src");
            // TODO : We might have to verify that src is a file name. (a.k.a not a
            // url). There might be cases where documents have embedded urls (images).
            if (!src.equals("") && null != targetDocument && null != docBridge) {
                try {
                    image.setAttribute("src", docBridge.getAttachmentURL(targetDocument, src));
                } catch (Exception ex) {
                    // Do nothing.
                }
                // The 'align' attribute of images creates a lot of problems. First, OO server has a problem with
                // center aligning images (it aligns them to left). Next, OO server uses <br clear"xxx"> for
                // avoiding content wrapping around images which is not valid xhtml. There for, to be consistent and
                // simple we will remove the 'align' attribute of all the images so that they are all left aligned.
                image.removeAttribute("align");
            } else if (src.startsWith("file://")) {
                src = "Missing.png";
                image.setAttribute("src", src);
                image.setAttribute("alt", src);
            }
            Comment beforeComment = htmlDocument.createComment("startimage:" + src);
            Comment afterComment = htmlDocument.createComment("stopimage");
            image.getParentNode().insertBefore(beforeComment, image);
            image.getParentNode().insertBefore(afterComment, image.getNextSibling());
        }
    }
}
