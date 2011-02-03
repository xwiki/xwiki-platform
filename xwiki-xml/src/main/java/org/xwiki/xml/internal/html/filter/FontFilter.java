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
package org.xwiki.xml.internal.html.filter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xwiki.component.annotation.Component;
import org.xwiki.xml.html.filter.AbstractHTMLFilter;

/**
 * Replaces invalid &lt;font&gt; tags with equivalent &lt;span&gt; tags using inline css rules.
 * 
 * @version $Id$
 * @since 1.8RC2
 */
@Component("font")
public class FontFilter extends AbstractHTMLFilter
{
    /**
     * A map holding the translation from 'size' attribute of html font tag to 'font-size' css property.
     */
    private static final Map<String, String> FONT_SIZE_MAP;

    static {
        FONT_SIZE_MAP = new HashMap<String, String>();
        FONT_SIZE_MAP.put("1", "0.6em");
        FONT_SIZE_MAP.put("2", "0.8em");
        FONT_SIZE_MAP.put("3", "1.0em");
        FONT_SIZE_MAP.put("4", "1.2em");
        FONT_SIZE_MAP.put("5", "1.4em");
        FONT_SIZE_MAP.put("6", "1.6em");
        FONT_SIZE_MAP.put("7", "1.8em");
        FONT_SIZE_MAP.put("-3", "0.4em");
        FONT_SIZE_MAP.put("-2", FONT_SIZE_MAP.get("1"));
        FONT_SIZE_MAP.put("-1", FONT_SIZE_MAP.get("2"));
        FONT_SIZE_MAP.put("+1", FONT_SIZE_MAP.get("4"));
        FONT_SIZE_MAP.put("+2", FONT_SIZE_MAP.get("5"));
        FONT_SIZE_MAP.put("+3", FONT_SIZE_MAP.get("6"));
    }

    /**
     * {@inheritDoc}
     * 
     * <p>The {@link FontFilter} does not use any cleaningParameters passed in.</p>
     */
    public void filter(Document document, Map<String, String> cleaningParameters)
    {
        List<Element> fontTags = filterDescendants(document.getDocumentElement(), new String[] {TAG_FONT});
        for (Element fontTag : fontTags) {
            Element span = document.createElement(TAG_SPAN);
            moveChildren(fontTag, span);
            StringBuffer buffer = new StringBuffer();
            if (fontTag.hasAttribute(ATTRIBUTE_FONTCOLOR)) {
                buffer.append(String.format("color:%s;", fontTag.getAttribute(ATTRIBUTE_FONTCOLOR)));
            }
            if (fontTag.hasAttribute(ATTRIBUTE_FONTFACE)) {
                buffer.append(String.format("font-family:%s;", fontTag.getAttribute(ATTRIBUTE_FONTFACE)));
            }
            if (fontTag.hasAttribute(ATTRIBUTE_FONTSIZE)) {
                String fontSize = fontTag.getAttribute(ATTRIBUTE_FONTSIZE);
                String fontSizeCss = FONT_SIZE_MAP.get(fontSize);
                fontSizeCss = (fontSizeCss != null) ? fontSizeCss : fontSize;
                buffer.append(String.format("font-size:%s;", fontSizeCss));
            }
            if (fontTag.hasAttribute(ATTRIBUTE_STYLE) && fontTag.getAttribute(ATTRIBUTE_STYLE).trim().length() == 0) {
                buffer.append(fontTag.getAttribute(ATTRIBUTE_STYLE));
            }
            if (buffer.length() > 0) {
                span.setAttribute(ATTRIBUTE_STYLE, buffer.toString());
            }
            fontTag.getParentNode().insertBefore(span, fontTag);
            fontTag.getParentNode().removeChild(fontTag);
        }
    }
}
