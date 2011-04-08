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
package org.xwiki.gwt.wysiwyg.client.plugin.line;

import org.xwiki.gwt.dom.client.DOMUtils;
import org.xwiki.gwt.dom.client.Event;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.wysiwyg.client.RichTextAreaTestCase;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.user.client.Command;

/**
 * Unit tests for {@link LinePlugin}.
 * 
 * @version $Id$
 */
public class LinePluginTest extends RichTextAreaTestCase
{
    /**
     * The name of the paragraph HTML element.
     */
    private static final String PARAGRAPH = "p";

    /**
     * The name of the anchor HTML element.
     */
    private static final String ANCHOR = "a";

    /**
     * The name of the line break HTML element.
     */
    private static final String BR = "br";

    /**
     * The plug-in being tested.
     */
    private LinePlugin plugin;

    /**
     * {@inheritDoc}
     * 
     * @see RichTextAreaTestCase#onLoad(LoadEvent)
     */
    public void onLoad(LoadEvent event)
    {
        super.onLoad(event);

        plugin = GWT.create(LinePlugin.class);
        plugin.init(rta, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see RichTextAreaTestCase#gwtTearDown()
     */
    protected void gwtTearDown() throws Exception
    {
        plugin.destroy();

        super.gwtTearDown();
    }

    /**
     * @see XWIKI-3283: Hitting enter twice between 2 titles doesn't create new line in IE6
     */
    public void testEnterBetweenHeadingsWithInnerSpan()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                doTestEnterBetweenHeadingsWithInnerSpan();
            }
        });
    }

    /**
     * @see XWIKI-3283: Hitting enter twice between 2 titles doesn't create new line in IE6
     */
    private void doTestEnterBetweenHeadingsWithInnerSpan()
    {
        rta.setHTML("<h2><span>title 2</span></h2><h3><span>title 3</span></h3>");

        // Place the caret at the end of the first heading.
        Range range = rta.getDocument().createRange();
        range.selectNodeContents(getBody().getFirstChild().getFirstChild().getFirstChild());
        range.collapse(false);
        select(range);

        // Type Enter.
        rta
            .onBrowserEvent((Event) rta.getDocument()
                .createKeyDownEvent(false, false, false, false, KeyCodes.KEY_ENTER));

        // Check if the caret is inside a span which is inside paragraph.
        range = rta.getDocument().getSelection().getRangeAt(0);
        assertNotNull(DOMUtils.getInstance().getFirstAncestor(range.getStartContainer(), "span"));
        assertNotNull(DOMUtils.getInstance().getFirstAncestor(range.getStartContainer(), PARAGRAPH));
    }

    /**
     * Tests if the caret is placed inside a new paragraph after pressing Enter between two headings that contain only
     * text.
     */
    public void testEnterBetweenHeadingsWithoutInnerSpan()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                rta.setHTML("<h2>title 2</h2><h3>title 3/h3>");

                // Place the caret at the end of the first heading.
                Range range = rta.getDocument().createRange();
                range.selectNodeContents(getBody().getFirstChild().getFirstChild());
                range.collapse(false);
                select(range);

                // Type Enter.
                rta.onBrowserEvent((Event) rta.getDocument().createKeyDownEvent(false, false, false, false,
                    KeyCodes.KEY_ENTER));

                // Check if the caret is inside a paragraph.
                range = rta.getDocument().getSelection().getRangeAt(0);
                assertNotNull(DOMUtils.getInstance().getFirstAncestor(range.getStartContainer(), PARAGRAPH));
            }
        });
    }

    /**
     * Tests if the caret is placed inside a new paragraph after pressing Enter inside a text node that is a direct
     * child of body.
     */
    public void testEnterInBodyWithPlainText()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                doTestEnterInBodyWithPlainText();
            }
        });
    }

    /**
     * Tests if the caret is placed inside a new paragraph after pressing Enter inside a text node that is a direct
     * child of body.
     */
    private void doTestEnterInBodyWithPlainText()
    {
        rta.setHTML("amazing!");

        // Place the caret at the end of the first heading.
        Range range = rta.getDocument().createRange();
        range.setStart(getBody().getFirstChild(), 1);
        range.setEnd(getBody().getFirstChild(), 3);
        select(range);

        // Type Enter.
        rta
            .onBrowserEvent((Event) rta.getDocument()
                .createKeyDownEvent(false, false, false, false, KeyCodes.KEY_ENTER));

        // Check if the caret is inside a paragraph.
        range = rta.getDocument().getSelection().getRangeAt(0);
        assertNotNull(DOMUtils.getInstance().getFirstAncestor(range.getStartContainer(), PARAGRAPH));

        // Check the result.
        assertEquals("a<p>zing!</p>", clean(rta.getHTML()));
    }

    /**
     * Tests if the caret is placed inside a new paragraph after pressing Enter inside a text node that is a descendant
     * of body with only in-line parents.
     */
    public void testEnterInBodyWithStyledText()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                doTestEnterInBodyWithStyledText();
            }
        });
    }

    /**
     * Tests if the caret is placed inside a new paragraph after pressing Enter inside a text node that is a descendant
     * of body with only in-line parents.
     */
    private void doTestEnterInBodyWithStyledText()
    {
        rta.setHTML("<strong>xwiki</strong>enterprise");

        // Place the caret at the end of the first heading.
        Range range = rta.getDocument().createRange();
        range.selectNode(getBody().getFirstChild().getFirstChild());
        range.collapse(false);
        select(range);

        // Type Enter.
        rta
            .onBrowserEvent((Event) rta.getDocument()
                .createKeyDownEvent(false, false, false, false, KeyCodes.KEY_ENTER));

        // Check if the caret is inside a strong element which is inside a paragraph.
        range = rta.getDocument().getSelection().getRangeAt(0);
        assertNotNull(DOMUtils.getInstance().getFirstAncestor(range.getStartContainer(), "strong"));
        assertNotNull(DOMUtils.getInstance().getFirstAncestor(range.getStartContainer(), PARAGRAPH));
    }

    /**
     * Tests that pressing Enter before a link doesn't leave an empty link on the previous line.
     * 
     * @see XWIKI-4193: When hitting Return at the end of the link the new line should not be a link
     */
    public void testEnterBeforeLink()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                doTestEnterBeforeLink();
            }
        });
    }

    /**
     * @see #testEnterBeforeLink()
     */
    private void doTestEnterBeforeLink()
    {
        rta.setHTML("<p>1<a href=\"http://www.xwiki.org\"><!--*-->2</a></p>");

        // Place the caret before the link.
        Range range = rta.getDocument().createRange();
        range.selectNode(getBody().getFirstChild().getLastChild().getLastChild());
        range.collapse(true);
        select(range);

        // Type Enter.
        rta
            .onBrowserEvent((Event) rta.getDocument()
                .createKeyDownEvent(false, false, false, false, KeyCodes.KEY_ENTER));

        // We should have two paragraphs and one link.
        assertEquals(2, getBody().getElementsByTagName(PARAGRAPH).getLength());
        assertEquals(1, getBody().getElementsByTagName(ANCHOR).getLength());
        // The link should be in the second paragraph.
        assertEquals(1, getBody().getElementsByTagName(PARAGRAPH).getItem(1).getElementsByTagName(ANCHOR).getLength());
    }

    /**
     * Tests that pressing Enter after a link doesn't create an empty link on the new line.
     * 
     * @see XWIKI-4193: When hitting Return at the end of the link the new line should not be a link
     */
    public void testEnterAfterLink()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                doTestEnterAfterLink();
            }
        });
    }

    /**
     * @see #testEnterAfterLink()
     */
    private void doTestEnterAfterLink()
    {
        rta.setHTML("<p><a href=\"http://www.xwiki.org\"><em>XWiki<!--*--></em></a></p>");

        // Place the caret after the link.
        Range range = rta.getDocument().createRange();
        range.selectNode(getBody().getFirstChild().getFirstChild().getFirstChild().getFirstChild());
        range.collapse(false);
        select(range);

        // Type Enter.
        rta
            .onBrowserEvent((Event) rta.getDocument()
                .createKeyDownEvent(false, false, false, false, KeyCodes.KEY_ENTER));

        // We should have two paragraphs and one link.
        assertEquals(2, getBody().getElementsByTagName(PARAGRAPH).getLength());
        assertEquals(1, getBody().getElementsByTagName(ANCHOR).getLength());
        // The link should be in the first paragraph.
        assertEquals(1, getBody().getElementsByTagName(PARAGRAPH).getItem(0).getElementsByTagName(ANCHOR).getLength());
    }

    /**
     * Tests if pressing Enter in the middle of a link splits that link in two.
     * 
     * @see XWIKI-4193: When hitting Return at the end of the link the new line should not be a link
     */
    public void testEnterInLink()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                doTestEnterInLink();
            }
        });
    }

    /**
     * @see #testEnterInLink()
     */
    private void doTestEnterInLink()
    {
        rta.setHTML("<p><a href=\"http://www.xwiki.org\">XWiki</a></p>");

        // Place the caret inside the link.
        Range range = rta.getDocument().createRange();
        range.setStart(getBody().getFirstChild().getFirstChild().getFirstChild(), 3);
        range.collapse(true);
        select(range);

        // Type Enter.
        rta
            .onBrowserEvent((Event) rta.getDocument()
                .createKeyDownEvent(false, false, false, false, KeyCodes.KEY_ENTER));

        // We should have two paragraphs and two links.
        assertEquals(2, getBody().getElementsByTagName(PARAGRAPH).getLength());
        assertEquals(2, getBody().getElementsByTagName(ANCHOR).getLength());
        // Each paragraph should contain one link.
        assertEquals(1, getBody().getElementsByTagName(PARAGRAPH).getItem(0).getElementsByTagName(ANCHOR).getLength());
        assertEquals(1, getBody().getElementsByTagName(PARAGRAPH).getItem(1).getElementsByTagName(ANCHOR).getLength());
    }

    /**
     * Tests that pressing Shift+Enter before a link inserts a line break and doesn't break the link.
     * 
     * @see XWIKI-4193: When hitting Return at the end of the link the new line should not be a link
     */
    public void testShiftEnterBeforeLink()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                doTestShiftEnterBeforeLink();
            }
        });
    }

    /**
     * @see #testShiftEnterBeforeLink()
     */
    private void doTestShiftEnterBeforeLink()
    {
        rta.setHTML("<p>2<a href=\"http://www.xwiki.org\"><strong>1</strong></a></p>");

        // Place the caret before the link.
        Range range = rta.getDocument().createRange();
        range.selectNode(getBody().getFirstChild().getLastChild().getFirstChild().getFirstChild());
        range.collapse(true);
        select(range);

        // Type Shift+Enter.
        rta.onBrowserEvent((Event) rta.getDocument().createKeyDownEvent(false, false, true, false, KeyCodes.KEY_ENTER));

        // We should have one paragraph, one link and one line break.
        assertEquals(1, getBody().getElementsByTagName(PARAGRAPH).getLength());
        assertEquals(1, getBody().getElementsByTagName(ANCHOR).getLength());
        assertEquals(1, getBody().getElementsByTagName(BR).getLength());

        // Type Enter.
        rta
            .onBrowserEvent((Event) rta.getDocument()
                .createKeyDownEvent(false, false, false, false, KeyCodes.KEY_ENTER));

        // We should have two paragraphs and one link.
        assertEquals(2, getBody().getElementsByTagName(PARAGRAPH).getLength());
        assertEquals(1, getBody().getElementsByTagName(ANCHOR).getLength());
        // The link should be in the second paragraph.
        assertEquals(1, getBody().getElementsByTagName(PARAGRAPH).getItem(1).getElementsByTagName(ANCHOR).getLength());
    }

    /**
     * Tests that pressing Enter after a link inserts a line break and doesn't break the link.
     * 
     * @see XWIKI-4193: When hitting Return at the end of the link the new line should not be a link
     */
    public void testShiftEnterAfterLink()
    {
        deferTest(new Command()
        {
            public void execute()
            {
                doTestShiftEnterAfterLink();
            }
        });
    }

    /**
     * @see #testShiftEnterAfterLink()
     */
    private void doTestShiftEnterAfterLink()
    {
        rta.setHTML("<p><a href=\"http://www.xwiki.org\">XWiki<!--*--></a></p>");

        // Place the caret after the link.
        Range range = rta.getDocument().createRange();
        range.selectNode(getBody().getFirstChild().getFirstChild().getFirstChild());
        range.collapse(false);
        select(range);

        // Type Shift+Enter.
        rta.onBrowserEvent((Event) rta.getDocument().createKeyDownEvent(false, false, true, false, KeyCodes.KEY_ENTER));

        // We should have one paragraph and one link.
        assertEquals(1, getBody().getElementsByTagName(PARAGRAPH).getLength());
        assertEquals(1, getBody().getElementsByTagName(ANCHOR).getLength());
        // We should have at least one BR for a line break. Some browser need another one to serve as an empty line.
        assertTrue(getBody().getElementsByTagName(BR).getLength() > 0);

        // Type Enter.
        rta
            .onBrowserEvent((Event) rta.getDocument()
                .createKeyDownEvent(false, false, false, false, KeyCodes.KEY_ENTER));

        // We should have two paragraphs and one link.
        assertEquals(2, getBody().getElementsByTagName(PARAGRAPH).getLength());
        assertEquals(1, getBody().getElementsByTagName(ANCHOR).getLength());
        // The link should be in the first paragraph.
        assertEquals(1, getBody().getElementsByTagName(PARAGRAPH).getItem(0).getElementsByTagName(ANCHOR).getLength());
    }
}
