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
package org.xwiki.wysiwyg.internal.cleaner;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xwiki.component.annotation.Component;

/**
 * Removes attributes with no value. Precisely, elements like {@code <span style="" title="Title">text</span>} are
 * transformed into {@code <span title="Title">text</span>}.
 * 
 * @version $Id$
 */
@Component(roles = {HTMLFilter.class})
@Named("emptyAttribute")
@Singleton
public class EmptyAttributeFilter extends AbstractHTMLFilter
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
            XPath xpath = XPathFactory.newInstance().newXPath();

            NodeList emptyAttributes =
                (NodeList) xpath.compile("//@*[. = '']").evaluate(document, XPathConstants.NODESET);
            for (int i = emptyAttributes.getLength() - 1; i >= 0; i--) {
                Attr emptyAttribute = (Attr) emptyAttributes.item(i);
                emptyAttribute.getOwnerElement().removeAttributeNode(emptyAttribute);
            }
        } catch (XPathExpressionException e) {
            this.logger.error("Exception while filtering empty attributes.", e);
        }
    }
}
