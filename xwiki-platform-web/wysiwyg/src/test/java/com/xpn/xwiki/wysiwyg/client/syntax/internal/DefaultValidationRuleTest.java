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
package com.xpn.xwiki.wysiwyg.client.syntax.internal;

import com.google.gwt.user.client.ui.RootPanel;
import com.xpn.xwiki.wysiwyg.client.WysiwygClientTest;
import com.xpn.xwiki.wysiwyg.client.ui.XRichTextArea;
import com.xpn.xwiki.wysiwyg.client.ui.cmd.Command;
import com.xpn.xwiki.wysiwyg.client.ui.cmd.internal.MockCommandManager;

/**
 * Unit test for {@link DefaultValidationRule}.
 */
public class DefaultValidationRuleTest extends WysiwygClientTest
{
    public void testOutdentRule()
    {
        DefaultValidationRule outdentRule = new DefaultValidationRule("outdent", Command.OUTDENT);
        XRichTextArea textArea = new XRichTextArea(new MockCommandManager());
        RootPanel.get().add(textArea);

        assertFalse(outdentRule.areValid(textArea));

        textArea.getCommandManager().execCommand(Command.INDENT);
        assertTrue(textArea.getCommandManager().queryCommandState(Command.INDENT));
        assertTrue(outdentRule.areValid(textArea));
    }
}
