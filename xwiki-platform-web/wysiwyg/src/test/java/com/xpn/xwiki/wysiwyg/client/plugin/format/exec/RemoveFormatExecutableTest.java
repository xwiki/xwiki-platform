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
package com.xpn.xwiki.wysiwyg.client.plugin.format.exec;

import org.xwiki.gwt.dom.client.Range;

import com.google.gwt.user.client.Timer;
import com.xpn.xwiki.wysiwyg.client.widget.rta.AbstractRichTextAreaTest;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Executable;

/**
 * Unit tests for {@link RemoveFormatExecutable}.
 * 
 * @version $Id$
 */
public class RemoveFormatExecutableTest extends AbstractRichTextAreaTest
{
    /**
     * The executable being tested.
     */
    private Executable executable;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractRichTextAreaTest#gwtSetUp()
     */
    protected void gwtSetUp() throws Exception
    {
        super.gwtSetUp();

        if (executable == null) {
            executable = new RemoveFormatExecutable();
        }
    }

    /**
     * Tests removing one level of formatting made with a formatting tag.
     */
    public void testRemoveFormattingTag()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestRemoveFormattingTag();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Tests removing one level of formatting made with a formatting tag.
     */
    private void doTestRemoveFormattingTag()
    {
        // Selection includes the formatting tag.
        rta.setHTML("a<em>b</em>c");
        Range range = rta.getDocument().createRange();
        range.selectNodeContents(getBody());
        select(range);
        assertTrue(executable.execute(rta, null));
        assertEquals("abc", clean(rta.getHTML()));

        // Selection is inside the formatting tag.
        rta.setHTML("1<em>2</em>3");
        range = rta.getDocument().createRange();
        range.selectNodeContents(getBody().getChildNodes().getItem(1));
        select(range);
        assertTrue(executable.execute(rta, null));
        assertEquals("123", clean(rta.getHTML()));

        // Selection includes the end of a formatting tag.
        rta.setHTML("1<em>23</em>4");
        range = rta.getDocument().createRange();
        range.setStart(getBody().getChildNodes().getItem(1).getFirstChild(), 1);
        range.setEndAfter(getBody().getLastChild());
        select(range);
        assertTrue(executable.execute(rta, null));
        assertEquals("1<em>2</em>34", clean(rta.getHTML()));

        // Selection includes the start of a formatting tag.
        rta.setHTML("4<em>32</em>1");
        range = rta.getDocument().createRange();
        range.setStartBefore(getBody().getFirstChild());
        range.setEnd(getBody().getChildNodes().getItem(1).getFirstChild(), 1);
        select(range);
        assertTrue(executable.execute(rta, null));
        assertEquals("43<em>2</em>1", clean(rta.getHTML()));
    }

    /**
     * Tests removing multiple levels of formatting made with nested formatting tags.
     */
    public void testRemoveFormattingTags()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestRemoveFormattingTags();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Tests removing multiple levels of formatting made with nested formatting tags.
     */
    private void doTestRemoveFormattingTags()
    {
        // Selection includes the formatting tags.
        rta.setHTML("a<em>b<strong>c<ins>d</ins>e</strong>f</em>g");
        Range range = rta.getDocument().createRange();
        range.selectNodeContents(getBody());
        select(range);
        assertTrue(executable.execute(rta, null));
        assertEquals("abcdefg", clean(rta.getHTML()));

        // Selection is inside the formatting tags.
        rta.setHTML("1<em>2<strong>3<ins>4</ins>5</strong>6</em>7");
        range = rta.getDocument().createRange();
        range.selectNodeContents(getBody().getChildNodes().getItem(1).getChildNodes().getItem(1).getChildNodes()
            .getItem(1));
        select(range);
        assertTrue(executable.execute(rta, null));
        assertEquals("1<em>2</em><em><strong>3</strong></em>4<em><strong>5</strong></em><em>6</em>7", clean(rta
            .getHTML()));

        // Selection includes a part of the formatting tags.
        rta.setHTML("7<em>6<strong>5<ins>4</ins>3</strong>2</em>1");
        range = rta.getDocument().createRange();
        range.setStartBefore(getBody().getFirstChild());
        range.setEndAfter(getBody().getChildNodes().getItem(1).getChildNodes().getItem(1).getFirstChild());
        select(range);
        assertTrue(executable.execute(rta, null));
        assertEquals("765<em><strong><ins>4</ins>3</strong></em><em>2</em>1", clean(rta.getHTML()));
    }

    /**
     * Tests removing one level of formatting made with in-line style.
     */
    public void testRemoveInlineStyle()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestRemoveInlineStyle();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Tests removing one level of formatting made with in-line style.
     */
    private void doTestRemoveInlineStyle()
    {
        // Selection includes the element with in-line style.
        rta.setHTML("3<span style=\"color: red; font-size: 18pt\">2</span>1");
        Range range = rta.getDocument().createRange();
        range.selectNodeContents(getBody());
        select(range);
        assertTrue(executable.execute(rta, null));
        assertEquals("321", clean(rta.getHTML()));

        // Selection is inside the element with in-line style.
        rta.setHTML("a<span style=\"color: red\">bcd</span>e");
        range = rta.getDocument().createRange();
        range.setStart(getBody().getChildNodes().getItem(1).getFirstChild(), 1);
        range.setEnd(getBody().getChildNodes().getItem(1).getFirstChild(), 2);
        select(range);
        assertTrue(executable.execute(rta, null));
        assertEquals("a<span style=\"color: red\">b</span>c<span style=\"color: red\">d</span>e", clean(rta.getHTML())
            .replace(String.valueOf(';'), ""));

        // Selection includes a part of the element with in-line style.
        rta.setHTML("a<span style=\"color: red\">bc</span>d");
        range = rta.getDocument().createRange();
        range.setStart(getBody().getChildNodes().getItem(1).getFirstChild(), 1);
        range.setEndAfter(getBody().getLastChild());
        select(range);
        assertTrue(executable.execute(rta, null));
        assertEquals("a<span style=\"color: red\">b</span>cd", clean(rta.getHTML()).replace(";", ""));
    }

    /**
     * Tests removing multiple levels of formatting made with nested in-line style.
     */
    public void testRemoveNestedInlineStyle()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestRemoveNestedInlineStyle();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Tests removing multiple levels of formatting made with nested in-line style.
     */
    private void doTestRemoveNestedInlineStyle()
    {
        // Selection includes the elements with in-line style.
        rta.setHTML("1<span style=\"color: red\">2<span style=\"font-size: 14pt\">3</span>4</span>5");
        Range range = rta.getDocument().createRange();
        range.selectNodeContents(getBody());
        select(range);
        assertTrue(executable.execute(rta, null));
        assertEquals("12345", clean(rta.getHTML()));

        // Selection is inside the elements with in-line style.
        rta.setHTML("1<span style=\"color: red\">23<span style=\"font-size: 14pt\">45</span></span>");
        range = rta.getDocument().createRange();
        range.setStart(getBody().getChildNodes().getItem(1).getFirstChild(), 1);
        range.setEnd(getBody().getLastChild().getLastChild().getFirstChild(), 1);
        select(range);
        assertTrue(executable.execute(rta, null));
        assertEquals("1<span style=\"color: red\">2</span>34"
            + "<span style=\"color: red\"><span style=\"font-size: 14pt\">5</span></span>", clean(rta.getHTML())
                .replace(String.valueOf(';'), ""));

        // Selection includes a part of the elements with in-line style.
        rta.setHTML("1<span style=\"color: red\">2<span style=\"font-size: 14pt\">34</span></span>");
        range = rta.getDocument().createRange();
        range.setStartBefore(getBody().getFirstChild());
        range.setEnd(getBody().getLastChild().getLastChild().getFirstChild(), 1);
        select(range);
        assertTrue(executable.execute(rta, null));
        assertEquals("123<span style=\"color: red\"><span style=\"font-size: 14pt\">4</span></span>", clean(
            rta.getHTML()).replace(String.valueOf(';'), ""));
    }

    /**
     * Tests removing multiple levels of mixed style (in-line style and formatting tags).
     */
    public void testRemoveMixedStyle()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestRemoveMixedStyle();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Tests removing multiple levels of mixed style (both in-line style and formatting tags).
     */
    private void doTestRemoveMixedStyle()
    {
        // Selection includes the mixed style.
        rta.setHTML("e<span style=\"color: red\">d<em>c</em>b</span>a");
        Range range = rta.getDocument().createRange();
        range.selectNodeContents(getBody());
        select(range);
        assertTrue(executable.execute(rta, null));
        assertEquals("edcba", clean(rta.getHTML()));

        // Selection is inside the mixed style.
        rta.setHTML("a<span style=\"color: red\">b<em id=\"x\" style=\"font-size: 10pt\">cde</em>f</span>g");
        range = rta.getDocument().createRange();
        range.setStart(getBody().getChildNodes().getItem(1).getChildNodes().getItem(1).getFirstChild(), 1);
        range.setEnd(getBody().getChildNodes().getItem(1).getChildNodes().getItem(1).getFirstChild(), 2);
        select(range);
        assertTrue(executable.execute(rta, null));
        assertEquals("a<span style=\"color: red\">b</span>"
            + "<span style=\"color: red\"><em style=\"font-size: 10pt\">c</em></span>"
            + "d<span style=\"color: red\"><em style=\"font-size: 10pt\">e</em></span>"
            + "<span style=\"color: red\">f</span>g", clean(rta.getHTML()).replace(String.valueOf(';'), ""));

        // Selection includes a part of the mixed style.
        rta.setHTML("<em style=\"font-size: 10pt\">a<span style=\"color: red\">b</span>c</em>d");
        range = rta.getDocument().createRange();
        range.setStartBefore(getBody().getFirstChild().getChildNodes().getItem(1).getFirstChild());
        range.setEndAfter(getBody().getLastChild());
        select(range);
        assertTrue(executable.execute(rta, null));
        assertEquals("<em style=\"font-size: 10pt\">a</em>bcd", clean(rta.getHTML()).replace(String.valueOf(';'), ""));
    }

    /**
     * Tests that links are not split when removing style.
     */
    public void testLinksAreNotSplit()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestLinksAreNotSplit();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * Tests that links are not split when removing style.
     */
    private void doTestLinksAreNotSplit()
    {
        // Selection includes the link.
        rta.setHTML("x<strong>1<a href=\"about:blank\" style=\"color: red\">y<em>#</em></a></strong>z");
        Range range = rta.getDocument().createRange();
        range.selectNodeContents(getBody());
        select(range);
        assertTrue(executable.execute(rta, null));
        assertEquals("x1<a href=\"about:blank\">y#</a>z", clean(rta.getHTML()));

        // Selection is inside the link.
        rta.setHTML("1<strong style=\"font-size: 18pt\"><a href=\"about:blank\" "
            + "style=\"color: red\">23<em>45</em></a>6</strong>");
        range = rta.getDocument().createRange();
        range.setStart(getBody().getLastChild().getFirstChild().getFirstChild(), 1);
        range.setEnd(getBody().getLastChild().getFirstChild().getLastChild().getFirstChild(), 1);
        select(range);
        assertTrue(executable.execute(rta, null));
        assertEquals("1<a href=\"about:blank\"><strong style=\"font-size: 18pt\"><span style=\"color: red\">"
            + "2</span></strong>34<strong style=\"font-size: 18pt\"><span style=\"color: red\">"
            + "<em>5</em></span></strong></a><strong style=\"font-size: 18pt\">6</strong>", clean(rta.getHTML())
                .replace(String.valueOf(';'), ""));
    }
}
