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

import com.xpn.xwiki.wysiwyg.client.AbstractWysiwygClientTest;

/**
 * Unit tests for {@link DocumentFragment}.
 * 
 * @version $Id$
 */
public class DocumentFragmentTest extends AbstractWysiwygClientTest
{
    /**
     * Unit test for {@link DocumentFragment#getInnerHTML()}.
     */
    public void testGetInnerHTML()
    {
        DocumentFragment df = ((Document) Document.get()).createDocumentFragment();

        Element element = Document.get().createDelElement().cast();
        element.setInnerHTML("a<!--x-->b<em>c</em>d");

        Text text = Document.get().createTextNode("#").cast();

        df.appendChild(element);
        df.appendChild(text);
        assertEquals(element.getString() + text.getData(), df.getInnerHTML());
        assertEquals(2, df.getChildNodes().getLength());
    }

    /**
     * Unit test for {@link DocumentFragment#getInnerText()}.
     */
    public void testGetInnerText()
    {
        DocumentFragment df = ((Document) Document.get()).createDocumentFragment();

        Element element = Document.get().createDelElement().cast();
        element.setInnerHTML("d<!--c-->b<em>x</em>a");

        Text text = Document.get().createTextNode("%").cast();

        df.appendChild(element);
        df.appendChild(text);
        assertEquals(element.xGetInnerText() + text.getData(), df.getInnerText());
        assertEquals(2, df.getChildNodes().getLength());
    }
}
