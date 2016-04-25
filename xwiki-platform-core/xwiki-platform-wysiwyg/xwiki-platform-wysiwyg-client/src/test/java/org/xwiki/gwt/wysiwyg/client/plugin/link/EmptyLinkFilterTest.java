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
package org.xwiki.gwt.wysiwyg.client.plugin.link;

import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.wysiwyg.client.RichTextAreaTestCase;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;

/**
 * Unit tests for the {@link EmptyLinkFilter}, to check client side handling of empty links.
 * 
 * @version $Id$
 */
public class EmptyLinkFilterTest extends RichTextAreaTestCase
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
     * The link filter to test.
     */
    private EmptyLinkFilter linkFilter;

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
        deferTest(new com.google.gwt.user.client.Command()
        {
            public void execute()
            {
                rta.setHTML("This is a test <a metadata='<!--startwikilink:http://www.xwiki.org-->"
                    + "<span class=\"wikiexternallink\">org.xwiki.gwt.dom.client.Element#placeholder</span>"
                    + "<!--stopwikilink-->' href=\"http://www.xwiki.org\"></a>with an empty link");

                // simulate a submit command
                linkFilter.onBeforeCommand(rta.getCommandManager(), SUBMIT_COMMAND, null);

                assertEquals("this is a test with an empty link", removeNonBreakingSpaces(clean(rta.getHTML())));
            }
        });
    }

    /**
     * Tests the case of an anchor with an empty span inside which should be filtered.
     */
    public void testEmptyLinkWithSpanInsideIsFiltered()
    {
        deferTest(new com.google.gwt.user.client.Command()
        {
            public void execute()
            {
                rta.setHTML("Testing a link <a href=\"http://www.xwiki.org\" class=\"wikimodel-freestanding\" "
                    + "metadata='<!--startwikilink:http://www.xwiki.org--><span class=\"wikiexternallink\">"
                    + "org.xwiki.gwt.dom.client.Element#placeholder</span><!--stopwikilink-->'>"
                    + "<span class=\"wikigeneratedlinkcontent\"></span></a>with a span inside");

                // simulate a submit command
                linkFilter.onBeforeCommand(rta.getCommandManager(), SUBMIT_COMMAND, null);

                assertEquals("testing a link with a span inside", removeNonBreakingSpaces(clean(rta.getHTML())));
            }
        });
    }

    /**
     * Tests the case of an anchor with an empty span inside which should be filtered.
     */
    public void testMultipleEmptyLinksAreRemoved()
    {
        deferTest(new com.google.gwt.user.client.Command()
        {
            public void execute()
            {
                rta.setHTML("Testing one link <a href=\"http://xwiki.org\" class=\"wikimodel-freestanding\" "
                    + "metadata='<!--startwikilink:http://xwiki.org--><span class=\"wikiexternallink\">"
                    + "org.xwiki.gwt.dom.client.Element#placeholder</span>"
                    + "<!--stopwikilink-->'><span class=\"wikigeneratedlinkcontent\"></span></a>and another "
                    + "<a metadata='<!--startwikilink:http://www.xwiki.org--><span class=\"wikiexternallink\">"
                    + "org.xwiki.gwt.dom.client.Element#placeholder</span><!--stopwikilink-->' "
                    + "href=\"http://www.xwiki.org\"></a>link");

                // simulate a submit command
                linkFilter.onBeforeCommand(rta.getCommandManager(), SUBMIT_COMMAND, null);

                assertEquals("testing one link and another link", removeNonBreakingSpaces(clean(rta.getHTML())));
            }
        });
    }

    /**
     * Test that non-empty links are not filtered away.
     */
    public void testNonEmptyLinkIsNotFiltered()
    {
        deferTest(new com.google.gwt.user.client.Command()
        {
            public void execute()
            {
                doTestNonEmptyLinkIsNotFiltered();
            }
        });
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
        deferTest(new com.google.gwt.user.client.Command()
        {
            public void execute()
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
        });
    }

    /**
     * @see XWIKI-4541: Links are removed when a macro is collapsed and the editor looses focus.
     */
    public void testHiddenAnchorsAreNotFiltered()
    {
        deferTest(new com.google.gwt.user.client.Command()
        {
            public void execute()
            {
                // Link is hidden.
                rta.setHTML("1<a style=\"display:none;\" href=\"http://www.xwiki.org\">2</a>3");
                linkFilter.onBeforeCommand(rta.getCommandManager(), SUBMIT_COMMAND, null);
                assertEquals(1, getBody().getElementsByTagName(ANCHOR_TAG).getLength());

                // Link container is hidden.
                rta.setHTML("1<span style=\"display:none;\">2<a href=\"http://www.xwiki.org\">3</a>4</span>5");
                linkFilter.onBeforeCommand(rta.getCommandManager(), SUBMIT_COMMAND, null);
                assertEquals(1, getBody().getElementsByTagName(ANCHOR_TAG).getLength());
            }
        });
    }

    /**
     * @see XWIKI-5588: Wysiwyg removes image when image is left align and has a link around it
     */
    public void testAnchorWithFloatedContentIsNotFiltered()
    {
        deferTest(new com.google.gwt.user.client.Command()
        {
            public void execute()
            {
                rta.setHTML("<a href=\"http://www.xwiki.org\"><img src=\"missing.gif\""
                    + " alt=\"test\" style=\"float:left\"/></a>");
                linkFilter.onBeforeCommand(rta.getCommandManager(), SUBMIT_COMMAND, null);
                assertEquals(1, getBody().getElementsByTagName(ANCHOR_TAG).getLength());
            }
        });
    }
}
