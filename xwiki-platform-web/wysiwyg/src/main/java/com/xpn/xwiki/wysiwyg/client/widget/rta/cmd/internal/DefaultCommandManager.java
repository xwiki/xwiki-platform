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
package com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.internal;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.dom.Style;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Executable;

/**
 * Default command manager.
 * 
 * @version $Id$
 */
public class DefaultCommandManager extends AbstractCommandManager implements FocusListener
{
    /**
     * The map of predefined executable provided by the rich text area.
     */
    public static final Map<Command, Executable> EXECUTABLES;

    /**
     * The underlying rich text area on which this command manager operates. All the commands are execute on this rich
     * text area.
     */
    private final RichTextArea rta;

    /**
     * Specifies if the underlying rich text area has focus.
     */
    private boolean focused;

    static {
        EXECUTABLES = new HashMap<Command, Executable>();
        EXECUTABLES.put(Command.BACK_COLOR, (Executable) GWT.create(BackColorExecutable.class));
        EXECUTABLES.put(Command.BOLD, new BoldExecutable());
        EXECUTABLES.put(Command.CREATE_LINK, new CreateLinkExecutable());
        EXECUTABLES.put(Command.DELETE, new DefaultExecutable(Command.DELETE.toString()));
        EXECUTABLES.put(Command.FONT_NAME, new DefaultExecutable(Command.FONT_NAME.toString()));
        EXECUTABLES.put(Command.FONT_SIZE, new DefaultExecutable(Command.FONT_SIZE.toString()));
        EXECUTABLES.put(Command.FORE_COLOR, new DefaultExecutable(Command.FORE_COLOR.toString()));
        EXECUTABLES.put(Command.FORMAT_BLOCK, new FormatBlockExecutable());
        EXECUTABLES.put(Command.INDENT, new DefaultExecutable(Command.INDENT.toString()));
        EXECUTABLES.put(Command.INSERT_HORIZONTAL_RULE, new InsertHRExecutable());
        EXECUTABLES.put(Command.INSERT_HTML, new InsertHTMLExecutable());
        EXECUTABLES.put(Command.INSERT_ORDERED_LIST, new DefaultExecutable(Command.INSERT_ORDERED_LIST.toString()));
        EXECUTABLES.put(Command.INSERT_PARAGRAPH, new DefaultExecutable(Command.INSERT_PARAGRAPH.toString()));
        EXECUTABLES.put(Command.INSERT_UNORDERED_LIST, new DefaultExecutable(Command.INSERT_UNORDERED_LIST.toString()));
        EXECUTABLES.put(Command.ITALIC, new StyleExecutable("em", null, Style.FONT_STYLE, Style.FontStyle.ITALIC, true,
            false));
        EXECUTABLES.put(Command.JUSTIFY_CENTER, new DefaultExecutable(Command.JUSTIFY_CENTER.toString()));
        EXECUTABLES.put(Command.JUSTIFY_FULL, new DefaultExecutable(Command.JUSTIFY_FULL.toString()));
        EXECUTABLES.put(Command.JUSTIFY_LEFT, new DefaultExecutable(Command.JUSTIFY_LEFT.toString()));
        EXECUTABLES.put(Command.JUSTIFY_RIGHT, new DefaultExecutable(Command.JUSTIFY_RIGHT.toString()));
        EXECUTABLES.put(Command.OUTDENT, new DefaultExecutable(Command.OUTDENT.toString()));
        EXECUTABLES.put(Command.REDO, new RedoExecutable());
        EXECUTABLES.put(Command.REMOVE_FORMAT, new DefaultExecutable(Command.REMOVE_FORMAT.toString()));
        EXECUTABLES.put(Command.STRIKE_THROUGH, new StyleExecutable("del", null, Style.TEXT_DECORATION,
            Style.TextDecoration.LINE_THROUGH, false, true));
        EXECUTABLES.put(Command.SUB_SCRIPT, new DefaultExecutable(Command.SUB_SCRIPT.toString()));
        EXECUTABLES.put(Command.SUPER_SCRIPT, new DefaultExecutable(Command.SUPER_SCRIPT.toString()));
        EXECUTABLES.put(Command.TELETYPE, new StyleExecutable("tt", null, Style.FONT_FAMILY, "monospace", true, true));
        EXECUTABLES.put(Command.UNDERLINE, new StyleExecutable("ins", null, Style.TEXT_DECORATION,
            Style.TextDecoration.UNDERLINE, false, true));
        EXECUTABLES.put(Command.UNLINK, new UnlinkExecutable());
        EXECUTABLES.put(Command.UNDO, new UndoExecutable());
    }

    /**
     * Creates a new command manager for the given rich text area.
     * 
     * @param rta The rich text area on which this manager will operate.
     */
    public DefaultCommandManager(RichTextArea rta)
    {
        this(rta, EXECUTABLES);
    }

    /**
     * Creates a new command manager for the given rich text area, initializing its executables map with the one
     * specified.
     * 
     * @param rta The rich text area on which this manager will operate.
     * @param executables The initial executables this manager will know.
     */
    public DefaultCommandManager(RichTextArea rta, Map<Command, Executable> executables)
    {
        this.rta = rta;
        rta.addFocusListener(this);

        for (Map.Entry<Command, Executable> entry : executables.entrySet()) {
            registerCommand(entry.getKey(), entry.getValue());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractCommandManager#execute(Command, String)
     */
    public boolean execute(Command cmd, String param)
    {
        Executable executable = getExecutable(cmd);
        if (executable == null) {
            return false;
        }
        if (commandListeners.fireBeforeCommand(this, cmd, param)) {
            return false;
        }
        focus();
        boolean success = executable.execute(rta, param);
        if (success) {
            commandListeners.fireCommand(this, cmd, param);
        }
        return success;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractCommandManager#isEnabled(Command)
     */
    public boolean isEnabled(Command cmd)
    {
        Executable executable = getExecutable(cmd);
        if (executable == null) {
            return false;
        }
        focus();
        return executable.isEnabled(rta);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractCommandManager#isExecuted(Command)
     */
    public boolean isExecuted(Command cmd)
    {
        Executable executable = getExecutable(cmd);
        if (executable == null) {
            return false;
        }
        focus();
        return executable.isExecuted(rta);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractCommandManager#isSupported(Command)
     */
    public boolean isSupported(Command cmd)
    {
        Executable executable = getExecutable(cmd);
        if (executable == null) {
            return false;
        }
        focus();
        return executable.isSupported(rta);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractCommandManager#getStringValue(Command)
     */
    public String getStringValue(Command cmd)
    {
        Executable executable = getExecutable(cmd);
        if (executable == null) {
            return null;
        }
        focus();
        return executable.getParameter(rta);
    }

    /**
     * {@inheritDoc}
     * 
     * @see FocusListener#onFocus(Widget)
     */
    public void onFocus(Widget sender)
    {
        if (sender == rta) {
            focused = true;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see FocusListener#onLostFocus(Widget)
     */
    public void onLostFocus(Widget sender)
    {
        if (sender == rta) {
            focused = false;
        }
    }

    /**
     * Focuses the underlying rich text area.
     */
    private void focus()
    {
        if (!focused) {
            rta.setFocus(true);
        }
    }
}
