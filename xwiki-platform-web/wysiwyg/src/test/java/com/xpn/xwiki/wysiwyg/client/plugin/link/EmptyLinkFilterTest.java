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
package com.xpn.xwiki.wysiwyg.client.plugin.link;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.user.client.Timer;
import com.xpn.xwiki.wysiwyg.client.widget.rta.AbstractRichTextAreaTest;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;

/**
 * Unit tests for the {@link EmptyLinkFilter}, to check client side handling of empty links.
 * 
 * @version $Id$
 */
public class EmptyLinkFilterTest extends AbstractRichTextAreaTest
{
    /**
     * The submit command used to simulate a submit for the link filter.
     */
    private static final Command SUBMIT_COMMAND = new Command("submit");

    /**
     * The anchor tag name.
     */
    private static final String ANCHOR_TAG = "a";

    /**
     * The href attribute name.
     */
    private static final String HREF_ATTRIBUTE = "href";

    /**
     * The link filter to test.
     */
    private EmptyLinkFilter linkFilter;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void gwtSetUp() throws Exception
    {
        super.gwtSetUp();
        linkFilter = new EmptyLinkFilter(rta);
    }

    /**
     * Tests the simple case of an empty anchor being filtered away.
     */
    public void testEmptyLinkIsFiltered()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestEmptyLinkIsFiltered();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * @see #testEmptyLinkIsFiltered()
     */
    private void doTestEmptyLinkIsFiltered()
    {
        rta.setHTML("This is a test <a metadata='<!--startwikilink:http://www.xwiki.org-->"
            + "<span class=\"wikiexternallink\">org.xwiki.gwt.dom.client.Element#placeholder</span>"
            + "<!--stopwikilink-->' href=\"http://www.xwiki.org\"></a>with an empty link");

        // simulate a submit command
        linkFilter.onBeforeCommand(rta.getCommandManager(), SUBMIT_COMMAND, null);

        assertEquals("this is a test with an empty link", removeNonBreakingSpaces(clean(rta.getHTML())));
    }

    /**
     * Tests the case of an anchor with an empty span inside which should be filtered.
     */
    public void testEmptyLinkWithSpanInsideIsFiltered()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestEmptyLinkWithSpanInsideIsFiltered();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * @see #testEmptyLinkWithSpanInsideIsFiltered()
     */
    public void doTestEmptyLinkWithSpanInsideIsFiltered()
    {
        rta.setHTML("Testing a link <a href=\"http://www.xwiki.org\" class=\"wikimodel-freestanding\" "
            + "metadata='<!--startwikilink:http://www.xwiki.org--><span class=\"wikiexternallink\">"
            + "org.xwiki.gwt.dom.client.Element#placeholder</span><!--stopwikilink-->'>"
            + "<span class=\"wikigeneratedlinkcontent\"></span></a>with a span inside");

        // simulate a submit command
        linkFilter.onBeforeCommand(rta.getCommandManager(), SUBMIT_COMMAND, null);

        assertEquals("testing a link with a span inside", removeNonBreakingSpaces(clean(rta.getHTML())));
    }

    /**
     * Test that non-empty links are not filtered away.
     */
    public void testNonEmptyLinkIsNotFiltered()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestNonEmptyLinkIsNotFiltered();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * @see #testNonEmptyLinkIsNotFiltered()
     */
    private void doTestNonEmptyLinkIsNotFiltered()
    {
        String hrefAttribute = "href";
        rta.setHTML("Testing a link <a href=\"http://www.xwiki.com\" class=\"wikimodel-freestanding\" "
            + "metadata='<!--startwikilink:http://www.xwiki.com--><span class=\"wikiexternallink\">"
            + "org.xwiki.gwt.dom.client.Element#placeholder</span><!--stopwikilink-->'><span "
            + "class=\"wikigeneratedlinkcontent\">http://www.xwiki.com</span></a>with a span inside");
        Element anchorBefore = rta.getDocument().getElementsByTagName(ANCHOR_TAG).getItem(0);

        // simulate a submit command
        linkFilter.onBeforeCommand(rta.getCommandManager(), SUBMIT_COMMAND, null);

        // try to find the anchor in a manner which is browser independent, since the inner HTML is different on
        // the various browsers
        NodeList<Element> anchorsAfter = rta.getDocument().getElementsByTagName(ANCHOR_TAG);
        assertTrue(anchorsAfter.getLength() > 0);
        Element anchorAfter = anchorsAfter.getItem(0);
        assertEquals(anchorBefore.getAttribute(hrefAttribute), anchorAfter.getAttribute(hrefAttribute));
        assertEquals(anchorBefore.getClassName(), anchorAfter.getClassName());
    }

    /**
     * Test an anchor without a href (only with a name attribute) is not filtered out.
     */
    public void testAnchorIsNotFiltered()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestAnchorIsNotFiltered();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * @see #testAnchorIsNotFiltered()
     */
    private void doTestAnchorIsNotFiltered()
    {
        String nameAttribute = "name";
        rta.setHTML("This is an anchor <a name=\"anchor\"></a> which shouldn't be filtered");
        Element anchorBefore = rta.getDocument().getElementsByTagName(ANCHOR_TAG).getItem(0);

        // simulate a submit command
        linkFilter.onBeforeCommand(rta.getCommandManager(), SUBMIT_COMMAND, null);

        // try to find the anchor in a manner which is browser independent, since the inner HTML is different on
        // the various browsers
        NodeList<Element> anchorsAfter = rta.getDocument().getElementsByTagName(ANCHOR_TAG);
        assertTrue(anchorsAfter.getLength() > 0);
        Element anchorAfter = anchorsAfter.getItem(0);
        assertEquals(anchorBefore.getAttribute(nameAttribute), anchorAfter.getAttribute(nameAttribute));
    }
}
