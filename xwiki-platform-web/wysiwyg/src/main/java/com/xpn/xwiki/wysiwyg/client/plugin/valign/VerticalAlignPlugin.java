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
package com.xpn.xwiki.wysiwyg.client.plugin.valign;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.Wysiwyg;
import com.xpn.xwiki.wysiwyg.client.plugin.Config;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.AbstractPlugin;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.FocusWidgetUIExtension;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.StatefulPlugin;
import com.xpn.xwiki.wysiwyg.client.ui.Images;
import com.xpn.xwiki.wysiwyg.client.ui.Strings;
import com.xpn.xwiki.wysiwyg.client.ui.XRichTextArea;
import com.xpn.xwiki.wysiwyg.client.ui.XRichTextEditor;
import com.xpn.xwiki.wysiwyg.client.ui.cmd.Command;

/**
 * {@link XRichTextEditor} plug-in for making text superscript or subscript. It installs two toggle buttons on the tool
 * bar and updates their status depending on the current cursor position and the direction of the navigation using the
 * arrow keys. For instance, if you navigate from a subscript region to a superscript one and you type a character it
 * will be subscript.<br/> <b>Known issues:</b> When you navigate backwards, from right to left, using the arrow keys,
 * the status of the toggle buttons is not synchronized with the text area. The text area behaves properly though.
 */
public class VerticalAlignPlugin extends StatefulPlugin
{
    private ToggleButton superScript;

    private ToggleButton subScript;

    private final FocusWidgetUIExtension toolBarExtension = new FocusWidgetUIExtension("toolbar");

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#init(Wysiwyg, XRichTextArea, Config)
     */
    public void init(Wysiwyg wysiwyg, XRichTextArea textArea, Config config)
    {
        super.init(wysiwyg, textArea, config);

        if (getTextArea().getCommandManager().queryCommandSupported(Command.SUPER_SCRIPT)) {
            superScript = new ToggleButton(Images.INSTANCE.superscript().createImage(), this);
            superScript.setTitle(Strings.INSTANCE.superscript());
            toolBarExtension.addFeature("superscript", superScript);
        }

        if (getTextArea().getCommandManager().queryCommandSupported(Command.SUB_SCRIPT)) {
            subScript = new ToggleButton(Images.INSTANCE.subscript().createImage(), this);
            subScript.setTitle(Strings.INSTANCE.subscript());
            toolBarExtension.addFeature("subscript", subScript);
        }

        if (toolBarExtension.getFeatures().length > 0) {
            getTextArea().addClickListener(this);
            getTextArea().addKeyboardListener(this);
            getTextArea().getCommandManager().addCommandListener(this);
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
        if (superScript != null) {
            superScript.removeFromParent();
            superScript.removeClickListener(this);
            superScript = null;
        }

        if (subScript != null) {
            subScript.removeFromParent();
            subScript.removeClickListener(this);
            subScript = null;
        }

        if (toolBarExtension.getFeatures().length > 0) {
            getTextArea().removeClickListener(this);
            getTextArea().removeKeyboardListener(this);
            getTextArea().getCommandManager().removeCommandListener(this);
            toolBarExtension.clearFeatures();
        }

        super.destroy();
    }

    /**
     * {@inheritDoc}
     * 
     * @see ClickListener#onClick(Widget)
     */
    public void onClick(Widget sender)
    {
        if (sender == superScript) {
            onSuperScript();
        } else if (sender == subScript) {
            onSubScript();
        } else {
            super.onClick(sender);
        }
    }

    public void onSuperScript()
    {
        if (superScript.isEnabled()) {
            getTextArea().getCommandManager().execCommand(Command.SUPER_SCRIPT);
        }
    }

    public void onSubScript()
    {
        if (subScript.isEnabled()) {
            getTextArea().getCommandManager().execCommand(Command.SUB_SCRIPT);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see StatefulPlugin#onUpdate()
     */
    public void onUpdate()
    {
        if (superScript != null) {
            superScript.setDown(getTextArea().getCommandManager().queryCommandState(Command.SUPER_SCRIPT));
        }
        if (subScript != null) {
            subScript.setDown(getTextArea().getCommandManager().queryCommandState(Command.SUB_SCRIPT));
        }
    }
}
