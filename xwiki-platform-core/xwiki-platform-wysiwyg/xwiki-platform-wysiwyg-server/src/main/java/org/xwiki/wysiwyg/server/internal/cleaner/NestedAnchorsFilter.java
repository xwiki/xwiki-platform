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
package org.xwiki.wysiwyg.server.internal.cleaner;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.xml.transform.TransformerException;

import org.apache.xpath.XPathAPI;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xwiki.component.annotation.Component;

/**
 * Removes nested anchors. Since anchors in anchors are not valid in XHTML and the parser does not accept them, we use
 * this filter to remove all the anchors inside other anchors, preserving their content.
 * 
 * @version $Id$
 */
@Component(roles = { HTMLFilter.class })
@Named("nestedAnchors")
@Singleton
public class NestedAnchorsFilter extends AbstractHTMLFilter
{
    /**
     * Logger.
     */
    @Inject
    private Logger logger;

    @Override
    public void filter(Document document, Map<String, String> parameters)
    {
        try {
            // all anchors descendants of other anchors
            NodeList nestedAnchors = XPathAPI.selectNodeList(document, "//a//a");
            for (int i = 0; i < nestedAnchors.getLength(); i++) {
                Element nestedAnchor = (Element) nestedAnchors.item(i);
                unwrap(nestedAnchor);
            }
        } catch (TransformerException e) {
            logger.error("Exception while filtering nested anchors.", e);
        }
    }

    /**
     * Removes an element from its parent, replacing it with all its children.
     * 
     * @param element the element to unwrap
     */
    private void unwrap(Element element)
    {
        Document ownerDocument = element.getOwnerDocument();
        if (ownerDocument == null) {
            return;
        }
        Element parent = (Element) element.getParentNode();
        if (parent == null) {
            return;
        }
        DocumentFragment children = element.getOwnerDocument().createDocumentFragment();
        while (element.hasChildNodes()) {
            children.appendChild(element.getChildNodes().item(0));
        }
        parent.replaceChild(children, element);
    }
}
