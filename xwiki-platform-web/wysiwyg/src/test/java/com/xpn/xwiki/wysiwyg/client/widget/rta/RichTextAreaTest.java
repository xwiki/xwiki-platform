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
package com.xpn.xwiki.wysiwyg.client.widget.rta;

import com.google.gwt.user.client.Timer;

/**
 * Unit tests for {@link RichTextArea}.
 * 
 * @version $Id$
 */
public class RichTextAreaTest extends AbstractRichTextAreaTest
{
    /**
     * Unit test for {@link RichTextArea#setHTML(String)}. We test the workaround we use for Issue 3147.
     * 
     * @see http://code.google.com/p/google-web-toolkit/issues/detail?id=3147
     */
    public void testSetHTML()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                rta.setFocus(true);
                doTestSetHTML();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Unit test for {@link RichTextArea#setHTML(String)}. We test the workaround we use for Issue 3147.
     * 
     * @see http://code.google.com/p/google-web-toolkit/issues/detail?id=3147
     */
    private void doTestSetHTML()
    {
        String html = "<!--x--><em>test</em>";
        rta.setHTML(html);
        assertEquals(html, clean(rta.getHTML()));
    }
}
