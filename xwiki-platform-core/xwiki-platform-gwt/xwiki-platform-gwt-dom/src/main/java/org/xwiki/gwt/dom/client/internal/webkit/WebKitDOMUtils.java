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
package org.xwiki.gwt.dom.client.internal.webkit;

import org.xwiki.gwt.dom.client.DOMUtils;
import org.xwiki.gwt.dom.client.Element;

/**
 * Extends {@link DOMUtils} with implementations specific WebKit-based browsers.
 * 
 * @version $Id$
 */
public class WebKitDOMUtils extends DOMUtils
{
    /**
     * {@inheritDoc}
     * <p>
     * Handle BR elements separately because WebKit doesn't provide layout information for them: both offset top and
     * offset left are always 0. As a consequence BR elements cannot be scrolled into view by default because we cannot
     * compute their position. The workaround is to insert a temporary in-line element before the BR element and scroll
     * that element into view, then remove it.
     * </p>
     */
    @Override
    public void scrollIntoView(Element element)
    {
        if (element != null && element.getParentNode() != null && "br".equalsIgnoreCase(element.getTagName())) {
            Element span = Element.as(element.getOwnerDocument().createSpanElement());
            // Add a non-breaking space to make the span visible.
            span.appendChild(element.getOwnerDocument().createTextNode("\u00A0"));
            element.getParentNode().insertBefore(span, element);
            super.scrollIntoView(span);
            span.removeFromParent();
        } else {
            super.scrollIntoView(element);
        }
    }
}
