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

import javax.xml.transform.TransformerException;

import org.apache.xpath.XPathAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xwiki.xml.html.filter.HTMLFilter;

/**
 * Removes attributes with no value. Precisely, elements like {@code <span style="" title="Title">text</span>} are
 * transformed into {@code <span title="Title">text</span>}.
 * 
 * @version $Id$
 */
public class EmptyAttributeFilter implements HTMLFilter
{
    /**
     * Default XWiki logger to report errors correctly.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(EmptyAttributeFilter.class);

    /**
     * {@inheritDoc}
     * 
     * @see HTMLFilter#filter(Document, Map)
     */
    public void filter(Document document, Map<String, String> parameters)
    {
        try {
            NodeList emptyAttributes = XPathAPI.selectNodeList(document, "//@*[. = '']");
            for (int i = emptyAttributes.getLength() - 1; i >= 0; i--) {
                Attr emptyAttribute = (Attr) emptyAttributes.item(i);
                emptyAttribute.getOwnerElement().removeAttributeNode(emptyAttribute);
            }
        } catch (TransformerException e) {
            LOGGER.error("Exception while filtering empty attributes.", e);
        }
    }
}
