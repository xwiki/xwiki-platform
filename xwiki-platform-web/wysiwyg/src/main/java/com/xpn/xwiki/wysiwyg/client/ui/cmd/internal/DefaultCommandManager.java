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
package com.xpn.xwiki.wysiwyg.client.ui.cmd.internal;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FocusWidget;
import com.xpn.xwiki.wysiwyg.client.ui.cmd.Command;
import com.xpn.xwiki.wysiwyg.client.ui.cmd.CommandManager;
import com.xpn.xwiki.wysiwyg.client.ui.cmd.Executable;

public class DefaultCommandManager extends AbstractCommandManager
{
    protected FocusWidget widget;

    private final Map<Command, Executable> executables;

    public final static Map<Command, Executable> EXECUTABLES;

    static {
        EXECUTABLES = new HashMap<Command, Executable>();
        EXECUTABLES.put(Command.BACK_COLOR, (Executable) GWT.create(BackColorExecutable.class));
        EXECUTABLES.put(Command.BOLD, new BoldExecutable());
        EXECUTABLES.put(Command.FONT_NAME, new DefaultExecutable(Command.FONT_NAME.toString()));
        EXECUTABLES.put(Command.FONT_SIZE, new DefaultExecutable(Command.FONT_SIZE.toString()));
        EXECUTABLES.put(Command.FORE_COLOR, new DefaultExecutable(Command.FORE_COLOR.toString()));
        EXECUTABLES.put(Command.FORMAT_BLOCK, (Executable) GWT.create(FormatBlockExecutable.class));
        EXECUTABLES.put(Command.INDENT, new DefaultExecutable(Command.INDENT.toString()));
        EXECUTABLES.put(Command.INSERT_BR_ON_RETURN, new DefaultExecutable(Command.INSERT_BR_ON_RETURN.toString()));
        EXECUTABLES.put(Command.INSERT_HORIZONTAL_RULE,
            new DefaultExecutable(Command.INSERT_HORIZONTAL_RULE.toString()));
        EXECUTABLES.put(Command.INSERT_HTML, (Executable) GWT.create(InsertHTMLExecutable.class));
        EXECUTABLES.put(Command.INSERT_IMAGE, new DefaultExecutable(Command.INSERT_IMAGE.toString()));
        EXECUTABLES.put(Command.INSERT_ORDERED_LIST, new DefaultExecutable(Command.INSERT_ORDERED_LIST.toString()));
        EXECUTABLES.put(Command.INSERT_PARAGRAPH, new DefaultExecutable(Command.INSERT_PARAGRAPH.toString()));
        EXECUTABLES.put(Command.INSERT_UNORDERED_LIST, new DefaultExecutable(Command.INSERT_UNORDERED_LIST.toString()));
        EXECUTABLES.put(Command.ITALIC, new StyleExecutable("em", "italic", "font-style", "italic", Command.ITALIC
            .toString()));
        EXECUTABLES.put(Command.JUSTIFY_CENTER, new DefaultExecutable(Command.JUSTIFY_CENTER.toString()));
        EXECUTABLES.put(Command.JUSTIFY_FULL, new DefaultExecutable(Command.JUSTIFY_FULL.toString()));
        EXECUTABLES.put(Command.JUSTIFY_LEFT, new DefaultExecutable(Command.JUSTIFY_LEFT.toString()));
        EXECUTABLES.put(Command.JUSTIFY_RIGHT, new DefaultExecutable(Command.JUSTIFY_RIGHT.toString()));
        EXECUTABLES.put(Command.OUTDENT, new DefaultExecutable(Command.OUTDENT.toString()));
        EXECUTABLES.put(Command.REMOVE_FORMAT, new DefaultExecutable(Command.REMOVE_FORMAT.toString()));
        EXECUTABLES.put(Command.STRIKE_THROUGH, new StyleExecutable("del",
            null,
            "text-decoration",
            "line-through",
            Command.STRIKE_THROUGH.toString()));
        EXECUTABLES.put(Command.STYLE_WITH_CSS, new StyleWithCssExecutable());
        EXECUTABLES.put(Command.SUB_SCRIPT, new DefaultExecutable(Command.SUB_SCRIPT.toString()));
        EXECUTABLES.put(Command.SUPER_SCRIPT, new DefaultExecutable(Command.SUPER_SCRIPT.toString()));
        EXECUTABLES.put(Command.TELETYPE, new StyleExecutable("tt", null, "font-family", "monospace", Command.TELETYPE
            .toString()));
        EXECUTABLES.put(Command.UNDERLINE, new StyleExecutable("span",
            "underline",
            "text-decoration",
            "underline",
            Command.UNDERLINE.toString()));
    }

    public DefaultCommandManager(FocusWidget widget)
    {
        this(widget, EXECUTABLES);
    }

    public DefaultCommandManager(FocusWidget widget, Map<Command, Executable> executables)
    {
        this.widget = widget;
        this.executables = new HashMap<Command, Executable>(executables);
    }

    /**
     * {@inheritDoc}
     * 
     * @see CommandManager#execute(Command, String)
     */
    public boolean execute(Command cmd, String param)
    {
        Executable executable = executables.get(cmd);
        if (executable == null) {
            return false;
        }
        widget.setFocus(true);
        boolean success = executable.execute(widget.getElement(), param);
        if (success) {
            commandListeners.fireCommand(this, cmd, param);
        }
        return success;
    }

    /**
     * {@inheritDoc}
     * 
     * @see CommandManager#isEnabled(Command)
     */
    public boolean isEnabled(Command cmd)
    {
        Executable executable = executables.get(cmd);
        if (executable == null) {
            return false;
        }
        widget.setFocus(true);
        return executable.isEnabled(widget.getElement());
    }

    /**
     * {@inheritDoc}
     * 
     * @see CommandManager#isExecuted(Command)
     */
    public boolean isExecuted(Command cmd)
    {
        Executable executable = executables.get(cmd);
        if (executable == null) {
            return false;
        }
        widget.setFocus(true);
        return executable.isExecuted(widget.getElement());
    }

    /**
     * {@inheritDoc}
     * 
     * @see CommandManager#isSupported(Command)
     */
    public boolean isSupported(Command cmd)
    {
        Executable executable = executables.get(cmd);
        if (executable == null) {
            return false;
        }
        widget.setFocus(true);
        return executable.isSupported(widget.getElement());
    }

    /**
     * {@inheritDoc}
     * 
     * @see CommandManager#getStringValue(Command)
     */
    public String getStringValue(Command cmd)
    {
        Executable executable = executables.get(cmd);
        if (executable == null) {
            return null;
        }
        widget.setFocus(true);
        return executable.getParameter(widget.getElement());
    }

    public Executable registerCommand(Command command, Executable executable)
    {
        return executables.put(command, executable);
    }

    public Executable unregisterCommand(Command command)
    {
        return executables.remove(command);
    }
}
