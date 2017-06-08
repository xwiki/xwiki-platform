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
package org.xwiki.test.wysiwyg;

import org.junit.Test;
import org.xwiki.test.wysiwyg.framework.AbstractWysiwygTestCase;

/**
 * Integration tests for embedded object support inside the WYSIWYG editor.
 * 
 * @version $Id$
 */
public class EmbedTest extends AbstractWysiwygTestCase
{
    /**
     * @see XWIKI-3975: Issue when inserting a video using the {{html}} macro when AdBlockPlus is enabled.
     */
    @Test
    public void testEmbedFlashUsingHTMLMacro()
    {
        switchToSource();
        String sourceText =
            "before\n\n{{html}}\n<div><object width=\"480\" height=\"381\"><param name=\"movie\" "
                + "value=\"http://www.dailymotion.com/swf/x5wi6m_xwiki-repository-against-codeswarm_tech&related=1\"></param>"
                + "<param name=\"allowFullScreen\" value=\"true\"></param><param name=\"allowScriptAccess\" value=\"always\"></param>"
                + "<embed src=\"http://www.dailymotion.com/swf/x5wi6m_xwiki-repository-against-codeswarm_tech&related=1\" "
                + "type=\"application/x-shockwave-flash\" width=\"480\" height=\"381\" allowFullScreen=\"true\" "
                + "allowScriptAccess=\"always\"></embed></object></div>\n{{/html}}\n\nafter";
        setSourceText(sourceText);
        switchToWysiwyg();
        typeText("1 2");
        // The tool bar is not updated right away. We have to wait for the undo push button to become enabled.
        waitForPushButton(TOOLBAR_BUTTON_UNDO_TITLE, true);
        clickUndoButton(2);
        switchToSource();
        assertSourceText("1" + sourceText);
    }
}
