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

import com.google.gwt.dom.client.DivElement;
import com.xpn.xwiki.wysiwyg.client.AbstractWysiwygClientTest;

/**
 * Unit tests for {@link Range}.
 * 
 * @version $Id$
 */
public class RangeTest extends AbstractWysiwygClientTest
{
    /**
     * Unit test for {@link Range#toHTML()}.
     */
    public void testToHTML()
    {
        Document doc = Document.get().cast();

        DivElement container = doc.createDivElement();
        container.setInnerHTML("<strong>aa</strong><em>bb</em>");
        doc.getBody().appendChild(container);

        Range range = doc.createRange();
        range.setStart(container.getFirstChild().getFirstChild(), 1);
        range.setEnd(container.getLastChild().getFirstChild(), 1);

        assertEquals("<strong>a</strong><em>b</em>", range.toHTML());
    }
}
