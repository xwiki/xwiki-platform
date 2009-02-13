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
package com.xpn.xwiki.wysiwyg.client.plugin.indent;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.Wysiwyg;
import com.xpn.xwiki.wysiwyg.client.editor.Images;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.indent.exec.IndentExecutable;
import com.xpn.xwiki.wysiwyg.client.plugin.indent.exec.OutdentExecutable;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.AbstractPlugin;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.FocusWidgetUIExtension;
import com.xpn.xwiki.wysiwyg.client.util.Config;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;

/**
 * Plug-in for indenting or outdenting text. It installs two toggle buttons on the tool bar and updates their status
 * depending on the current cursor position.
 * 
 * @version $Id$
 */
public class IndentPlugin extends AbstractPlugin implements ClickListener
{
    /**
     * The tool bar button used for indenting the current selection.
     */
    private PushButton indent;

    /**
     * The tool bar button used for outdenting the current selection.
     */
    private PushButton outdent;

    /**
     * User interface extension for the editor tool bar.
     */
    private final FocusWidgetUIExtension toolBarExtension = new FocusWidgetUIExtension("toolbar");

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#init(Wysiwyg, RichTextArea, Config)
     */
    public void init(Wysiwyg wysiwyg, RichTextArea textArea, Config config)
    {
        super.init(wysiwyg, textArea, config);

        if (getTextArea().getCommandManager().isSupported(Command.INDENT)) {
            indent = new PushButton(Images.INSTANCE.indent().createImage(), this);
            indent.setTitle(Strings.INSTANCE.indent());
            toolBarExtension.addFeature("indent", indent);
            getTextArea().getCommandManager().registerCommand(Command.INDENT, new IndentExecutable());
        }

        if (getTextArea().getCommandManager().isSupported(Command.OUTDENT)) {
            outdent = new PushButton(Images.INSTANCE.outdent().createImage(), this);
            outdent.setTitle(Strings.INSTANCE.outdent());
            toolBarExtension.addFeature("outdent", outdent);
            getTextArea().getCommandManager().registerCommand(Command.OUTDENT, new OutdentExecutable());
        }

        if (toolBarExtension.getFeatures().length > 0) {
            getUIExtensionList().add(toolBarExtension);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#destroy()
     */
    public void destroy()
    {
        if (indent != null) {
            indent.removeFromParent();
            indent.removeClickListener(this);
            indent = null;
        }

        if (outdent != null) {
            outdent.removeFromParent();
            outdent.removeClickListener(this);
            outdent = null;
        }

        toolBarExtension.clearFeatures();

        super.destroy();
    }

    /**
     * {@inheritDoc}
     * 
     * @see ClickListener#onClick(Widget)
     */
    public void onClick(Widget sender)
    {
        if (sender == indent) {
            onIndent();
        } else if (sender == outdent) {
            onOutdent();
        }
    }

    /**
     * Indents the current selection.
     */
    public void onIndent()
    {
        if (indent.isEnabled()) {
            getTextArea().getCommandManager().execute(Command.INDENT);
        }
    }

    /**
     * Outdents the current selection.
     */
    public void onOutdent()
    {
        if (outdent.isEnabled()) {
            getTextArea().getCommandManager().execute(Command.OUTDENT);
        }
    }
}
