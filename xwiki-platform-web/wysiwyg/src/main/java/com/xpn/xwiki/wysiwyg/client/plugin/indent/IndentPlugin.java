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
import com.xpn.xwiki.wysiwyg.client.plugin.Config;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.AbstractPlugin;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.FocusWidgetUIExtension;
import com.xpn.xwiki.wysiwyg.client.ui.Images;
import com.xpn.xwiki.wysiwyg.client.ui.Strings;
import com.xpn.xwiki.wysiwyg.client.ui.XRichTextArea;
import com.xpn.xwiki.wysiwyg.client.ui.XRichTextEditor;
import com.xpn.xwiki.wysiwyg.client.ui.cmd.Command;

/**
 * {@link XRichTextEditor} plug-in for indenting or outdenting text. It installs two toggle buttons on the tool bar and
 * updates their status depending on the current cursor position.
 */
public class IndentPlugin extends AbstractPlugin implements ClickListener
{
    private PushButton indent;

    private PushButton outdent;

    private final FocusWidgetUIExtension toolBarExtension = new FocusWidgetUIExtension("toolbar");

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#init(Wysiwyg, XRichTextArea, Config)
     */
    public void init(Wysiwyg wysiwyg, XRichTextArea textArea, Config config)
    {
        super.init(wysiwyg, textArea, config);

        if (getTextArea().getCommandManager().isSupported(Command.INDENT)) {
            indent = new PushButton(Images.INSTANCE.indent().createImage(), this);
            indent.setTitle(Strings.INSTANCE.indent());
            toolBarExtension.addFeature("indent", indent);
        }

        if (getTextArea().getCommandManager().isSupported(Command.OUTDENT)) {
            outdent = new PushButton(Images.INSTANCE.outdent().createImage(), this);
            outdent.setTitle(Strings.INSTANCE.outdent());
            toolBarExtension.addFeature("outdent", outdent);
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

    public void onIndent()
    {
        if (indent.isEnabled()) {
            getTextArea().getCommandManager().execute(Command.INDENT);
        }
    }

    public void onOutdent()
    {
        if (outdent.isEnabled()) {
            getTextArea().getCommandManager().execute(Command.OUTDENT);
        }
    }
}
