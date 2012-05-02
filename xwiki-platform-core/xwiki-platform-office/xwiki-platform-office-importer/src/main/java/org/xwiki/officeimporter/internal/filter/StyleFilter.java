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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xwiki.component.annotation.Component;
import org.xwiki.xml.html.filter.AbstractHTMLFilter;

/**
 * This particular filter searches HTML tags containing style attributes and removes such attributes if present.
 * 
 * @version $Id$
 * @since 1.8M1
 */
@Component("officeimporter/style")
public class StyleFilter extends AbstractHTMLFilter
{
    /**
     * Separator character used for grouping attribute names.
     */
    private static final String ATTRIBUTE_SEPARATOR = "|";

    /**
     * The html_tag_name->allowed_attribute_names mappings for strict filtering mode. This is used to filter out all
     * unnecessary attributes. The mapped object is a '|' separated string of all allowed attributes. If a particular
     * tag name is not present in this map, all of it's attributes will be filtered.
     */
    private Map<String, String> attributeMappingsStrict;

    /**
     * Constructs a {@link StyleFilter}.
     */
    public StyleFilter()
    {
        this.attributeMappingsStrict = new HashMap<String, String>();
        this.attributeMappingsStrict.put(TAG_A, "|href|name|");
        this.attributeMappingsStrict.put(TAG_IMG, "|alt|src|height|width|");
        this.attributeMappingsStrict.put(TAG_TD, "|colspan|rowspan|");
        this.attributeMappingsStrict.put(TAG_TH, "|colspan|");
    }

    @Override
    public void filter(Document document, Map<String, String> cleaningParams)
    {
        String mode = cleaningParams.get("filterStyles");
        if (null != mode && mode.equals("strict")) {
            filter(document.getDocumentElement(), this.attributeMappingsStrict);
        }
    }

    /**
     * Removes style attributes from this node and it's children recursively.
     * 
     * @param node node being filtered.
     * @param attributeMappings attribute map to be used for filtering.
     */
    private void filter(Node node, Map<String, String> attributeMappings)
    {
        if (node instanceof Element) {
            Element element = (Element) node;
            String allowedAttributes = attributeMappings.get(element.getNodeName().toLowerCase());
            NamedNodeMap currentAttributes = element.getAttributes();
            if (null == allowedAttributes) {
                // Strip off all attributes.
                while (currentAttributes.getLength() > 0) {
                    currentAttributes.removeNamedItem(currentAttributes.item(0).getNodeName());
                }
            } else {
                // Collect those attributes that need to be removed.
                List<String> attributesToBeRemoved = new ArrayList<String>();
                for (int i = 0; i < currentAttributes.getLength(); i++) {
                    String attributeName = currentAttributes.item(i).getNodeName();
                    String pattern = ATTRIBUTE_SEPARATOR + attributeName.toLowerCase() + ATTRIBUTE_SEPARATOR;
                    if (allowedAttributes.indexOf(pattern) == -1) {
                        attributesToBeRemoved.add(attributeName);
                    }
                }

                // Remove those attributes collected above.
                for (String attribute : attributesToBeRemoved) {
                    currentAttributes.removeNamedItem(attribute);
                }
            }
            if (node.hasChildNodes()) {
                NodeList children = node.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    filter(children.item(i), attributeMappings);
                }
            }
        }
    }
}
