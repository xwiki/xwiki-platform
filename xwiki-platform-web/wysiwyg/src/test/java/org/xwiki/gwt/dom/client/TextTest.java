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
package org.xwiki.gwt.dom.client;

/**
 * Unit tests for {@link Text}.
 * 
 * @version $Id$
 */
public class TextTest extends AbstractDOMTest
{
    /**
     * Unit test for {@link Text#getOffset()}.
     */
    public void testGetOffset()
    {
        Element container = Document.get().createDivElement().cast();

        container.setInnerHTML("abc");
        assertEquals(0, Text.as(container.getFirstChild()).getOffset());

        container.appendChild(Document.get().createTextNode(""));
        assertEquals(container.getFirstChild().getNodeValue().length(), Text.as(container.getLastChild()).getOffset());

        Element element = Document.get().createSpanElement().cast();
        container.appendChild(element);
        container.appendChild(Document.get().createTextNode("xyz"));
        assertEquals(0, Text.as(container.getLastChild()).getOffset());

        element.setAttribute(Element.META_DATA_ATTR, "");
        assertEquals(container.getFirstChild().getNodeValue().length(), Text.as(container.getLastChild()).getOffset());
    }
}
