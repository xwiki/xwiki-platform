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

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.user.client.Timer;

/**
 * Unit tests for {@link Document}.
 * 
 * @version $Id$
 */
public class DocumentTest extends DOMTestCase
{
    /**
     * Unit test for {@link Document#fireInnerHTMLChange(Element)}.
     */
    public void testFireInnerHTMLChange()
    {
        final List<Element> elements = new ArrayList<Element>();
        InnerHTMLListener listener = new InnerHTMLListener()
        {
            public void onInnerHTMLChange(Element element)
            {
                elements.add(element);
            }
        };

        Element element = getDocument().createDivElement().cast();
        element.xSetInnerHTML("1");

        getDocument().addInnerHTMLListener(listener);
        element.xSetInnerHTML("2");

        getDocument().removeInnerHTMLListener(listener);
        element.xSetInnerHTML("3");

        assertEquals(1, elements.size());
        assertEquals(element, elements.get(0));
    }

    /**
     * Unit test for {@link Document#setDesignMode(boolean)} and {@link Document#isDesignMode()}.
     */
    public void testDesignMode()
    {
        final IFrameElement iframe = getDocument().createIFrameElement();
        iframe.setSrc("about:blank");
        getContainer().appendChild(iframe);

        // We have to delay the test finish because the in-line frame is loaded asynchronously.
        delayTestFinish(400);
        (new Timer()
        {
            public void run()
            {
                // The in-line frame should be loaded by now.
                Document iframeDoc = (Document) iframe.getContentDocument();
                assertFalse(iframeDoc.isDesignMode());

                iframeDoc.setDesignMode(true);
                assertTrue(iframeDoc.isDesignMode());

                iframeDoc.setDesignMode(false);
                assertFalse(iframeDoc.isDesignMode());

                finishTest();
            }
        }).schedule(300);
    }
}
