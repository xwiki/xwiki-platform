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
import com.xpn.xwiki.wysiwyg.client.editor.Images;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.AbstractStatefulPlugin;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.FocusWidgetUIExtension;
import com.xpn.xwiki.wysiwyg.client.util.Config;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;

/**
 * Plug-in for making text superscript or subscript. It installs two toggle buttons on the tool bar and updates their
 * status depending on the current cursor position and the direction of the navigation using the arrow keys. For
 * instance, if you navigate from a subscript region to a superscript one and you type a character it will be subscript.
 * <p>
 * <b>Known issues:</b> When you navigate backwards, from right to left, using the arrow keys, the status of the toggle
 * buttons is not synchronized with the text area. The text area behaves properly though.
 * 
 * @version $Id$
 */
public class VerticalAlignPlugin extends AbstractStatefulPlugin implements ClickListener
{
    /**
     * The tool bar button that toggles the superscript style.
     */
    private ToggleButton superScript;

    /**
     * The tool bar button that toggles the subscript style.
     */
    private ToggleButton subScript;

    /**
     * User interface extension for the editor tool bar.
     */
    private final FocusWidgetUIExtension toolBarExtension = new FocusWidgetUIExtension("toolbar");

    /**
     * {@inheritDoc}
     * 
     * @see AbstractStatefulPlugin#init(Wysiwyg, RichTextArea, Config)
     */
    public void init(Wysiwyg wysiwyg, RichTextArea textArea, Config config)
    {
        super.init(wysiwyg, textArea, config);

        if (getTextArea().getCommandManager().isSupported(Command.SUPER_SCRIPT)) {
            superScript = new ToggleButton(Images.INSTANCE.superscript().createImage(), this);
            superScript.setTitle(Strings.INSTANCE.superscript());
            toolBarExtension.addFeature("superscript", superScript);
        }

        if (getTextArea().getCommandManager().isSupported(Command.SUB_SCRIPT)) {
            subScript = new ToggleButton(Images.INSTANCE.subscript().createImage(), this);
            subScript.setTitle(Strings.INSTANCE.subscript());
            toolBarExtension.addFeature("subscript", subScript);
        }

        if (toolBarExtension.getFeatures().length > 0) {
            getTextArea().addMouseListener(this);
            getTextArea().addKeyboardListener(this);
            getTextArea().getCommandManager().addCommandListener(this);
            getUIExtensionList().add(toolBarExtension);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractStatefulPlugin#destroy()
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
            getTextArea().removeMouseListener(this);
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
        }
    }

    /**
     * Toggles superscript style.
     */
    public void onSuperScript()
    {
        if (superScript.isEnabled()) {
            getTextArea().getCommandManager().execute(Command.SUPER_SCRIPT);
        }
    }

    /**
     * Toggles subscript style.
     */
    public void onSubScript()
    {
        if (subScript.isEnabled()) {
            getTextArea().getCommandManager().execute(Command.SUB_SCRIPT);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractStatefulPlugin#update()
     */
    public void update()
    {
        if (superScript != null) {
            superScript.setDown(getTextArea().getCommandManager().isExecuted(Command.SUPER_SCRIPT));
        }
        if (subScript != null) {
            subScript.setDown(getTextArea().getCommandManager().isExecuted(Command.SUB_SCRIPT));
        }
    }
}
