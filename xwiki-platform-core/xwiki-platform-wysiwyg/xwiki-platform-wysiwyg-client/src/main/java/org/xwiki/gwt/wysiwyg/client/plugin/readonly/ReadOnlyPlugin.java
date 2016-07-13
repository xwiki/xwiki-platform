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
package org.xwiki.gwt.wysiwyg.client.plugin.readonly;

import java.util.Arrays;
import java.util.List;

import org.xwiki.gwt.dom.client.PasteEvent;
import org.xwiki.gwt.dom.client.PasteHandler;
import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.user.client.ui.rta.cmd.CommandListener;
import org.xwiki.gwt.user.client.ui.rta.cmd.CommandManager;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.AbstractPlugin;
import org.xwiki.gwt.wysiwyg.client.plugin.macro.MacroPlugin;

import com.google.gwt.core.client.GWT;

/**
 * Controls the user interaction with read-only regions within the rich text area content. These read-only regions are
 * marked using the {@code readOnly} CSS class.
 * 
 * @version $Id$
 */
public class ReadOnlyPlugin extends AbstractPlugin implements CommandListener, PasteHandler
{
    /**
     * The list of commands that can be executes while the selection is inside or touches a read-only section.
     */
    private static final List<Command> ALLOWED_COMMANDS =
        Arrays.asList(Command.ENABLE, Command.REDO, Command.RESET, new Command("submit"), Command.UNDO, Command.UPDATE,
            MacroPlugin.REFRESH, MacroPlugin.COLLAPSE, MacroPlugin.EXPAND, MacroPlugin.INSERT);

    /**
     * The object that handles the keyboard events concerning read-only regions inside the rich text area.
     */
    private final ReadOnlyKeyboardHandler keyboardHandler = GWT.create(ReadOnlyKeyboardHandler.class);

    /**
     * Utility methods concerning read-only regions inside the rich text area.
     */
    private final ReadOnlyUtils readOnlyUtils = new ReadOnlyUtils();

    @Override
    public void init(RichTextArea textArea, Config config)
    {
        super.init(textArea, config);

        saveRegistration(getTextArea().addKeyDownHandler(keyboardHandler));
        saveRegistration(getTextArea().addKeyPressHandler(keyboardHandler));
        saveRegistration(getTextArea().addKeyUpHandler(keyboardHandler));
        saveRegistration(getTextArea().addPasteHandler(this));
        getTextArea().getCommandManager().addCommandListener(this);
    }

    @Override
    public void destroy()
    {
        getTextArea().getCommandManager().removeCommandListener(this);

        super.destroy();
    }

    @Override
    public boolean onBeforeCommand(CommandManager sender, Command command, String param)
    {
        return !ALLOWED_COMMANDS.contains(command)
            && readOnlyUtils.isSelectionBoundaryInsideReadOnlyElement(getTextArea().getDocument());
    }

    @Override
    public void onCommand(CommandManager sender, Command command, String param)
    {
        // Ignore.
    }

    @Override
    public void onPaste(PasteEvent event)
    {
        if (readOnlyUtils.isSelectionBoundaryInsideReadOnlyElement(getTextArea().getDocument())) {
            event.preventDefault();
        }
    }
}
