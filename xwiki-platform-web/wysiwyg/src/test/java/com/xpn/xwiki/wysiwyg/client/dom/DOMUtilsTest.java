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
package com.xpn.xwiki.wysiwyg.client.dom;

import com.google.gwt.dom.client.Node;
import com.xpn.xwiki.wysiwyg.client.AbstractWysiwygClientTest;

/**
 * Unit tests for {@link DOMUtils}.
 * 
 * @version $Id$
 */
public class DOMUtilsTest extends AbstractWysiwygClientTest
{
    /**
     * Unit test for {@link DOMUtils#getTextRange(Range)}.
     */
    public void testGetTextRange()
    {
        Document doc = Document.get().cast();

        Element container = doc.createSpanElement().cast();
        container.setInnerHTML("<a href=\"http://www.xwiki.org\"><strong>ab</strong>cd<em>ef</em></a>");
        doc.getBody().appendChild(doc.createTextNode("before"));
        doc.getBody().appendChild(container);
        doc.getBody().appendChild(doc.createTextNode("after"));

        Range range = doc.createRange();
        range.setStart(container, 0);
        range.setEnd(container, 1);

        Range textRange = DOMUtils.getInstance().getTextRange(range);

        assertEquals(range.toString(), textRange.toString());

        assertEquals(Node.TEXT_NODE, textRange.getStartContainer().getNodeType());
        assertEquals(0, textRange.getStartOffset());

        assertEquals(Node.TEXT_NODE, textRange.getEndContainer().getNodeType());
        assertEquals(2, textRange.getEndOffset());
    }
}
