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
package com.xpn.xwiki.wysiwyg.client.plugin.list;

import com.google.gwt.user.client.Timer;
import com.xpn.xwiki.wysiwyg.client.widget.rta.AbstractRichTextAreaTest;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;

/**
 * Unit tests for the {@link ListBehaviorAdjuster}.
 * 
 * @version $Id$
 */
public class ListBehaviorAdjusterTest extends AbstractRichTextAreaTest
{
    /**
     * The behavior adjuster being tested.
     */
    private ListBehaviorAdjuster behaviorAdjuster;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractRichTextAreaTest#gwtSetUp()
     */
    protected void gwtSetUp() throws Exception
    {
        super.gwtSetUp();

        if (behaviorAdjuster == null) {
            behaviorAdjuster = new ListBehaviorAdjuster();
            behaviorAdjuster.setTextArea(rta);
        }
    }

    /**
     * Unit test for
     * {@link ListBehaviorAdjuster#onCommand(com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.CommandManager, 
     * com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command, String)}
     * , for the case when a list element appears in another list element as the first child, in which case, the list
     * element inside needs to be wrapped in an empty list item.
     */
    public void testUnorderedListInListFirstItem()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestUnorderedListInListFirstItem();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * @see #testListInListFirstItem()
     */
    private void doTestUnorderedListInListFirstItem()
    {
        rta.setHTML("<ul><ul><li>one</li><li>two</li></ul><li>three</li><li>four</li></ul>");

        // simulate an unordered list command
        behaviorAdjuster.onCommand(rta.getCommandManager(), Command.INSERT_UNORDERED_LIST, null);

        assertEquals("<ul><li><br><ul><li>one</li><li>two</li></ul></li><li>three</li><li>four</li></ul>",
            removeNonBreakingSpaces(clean(rta.getHTML())));
    }

    /**
     * Unit test for
     * {@link ListBehaviorAdjuster#onCommand(com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.CommandManager, 
     * com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command, String)}
     * , for the case when a list element appears in another list element after another list item, in which case, the
     * list element needs to be appended as a child of its previous list item.
     */
    public void testOrderedListInListAfterItem()
    {
        delayTestFinish(FINISH_DELAY);
        (new Timer()
        {
            public void run()
            {
                doTestOrderedListInListAfterItem();
                finishTest();
            }
        }).schedule(START_DELAY);
    }

    /**
     * @see #testListInListFirstItem()
     */
    private void doTestOrderedListInListAfterItem()
    {
        rta.setHTML("<ul><li>three</li><ol><li>one</li><li>two</li></ol><li>four</li></ul>");

        // simulate an ordered list command
        behaviorAdjuster.onCommand(rta.getCommandManager(), Command.INSERT_ORDERED_LIST, null);

        assertEquals("<ul><li>three<ol><li>one</li><li>two</li></ol></li><li>four</li></ul>",
            removeNonBreakingSpaces(clean(rta.getHTML())));
    }
}
