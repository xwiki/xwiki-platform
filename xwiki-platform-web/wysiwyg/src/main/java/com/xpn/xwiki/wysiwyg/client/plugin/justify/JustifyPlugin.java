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
package com.xpn.xwiki.wysiwyg.client.plugin.justify;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.Wysiwyg;
import com.xpn.xwiki.wysiwyg.client.editor.Images;
import com.xpn.xwiki.wysiwyg.client.editor.RichTextEditor;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.AbstractPlugin;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.FocusWidgetUIExtension;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.StatefulPlugin;
import com.xpn.xwiki.wysiwyg.client.util.Config;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;

/**
 * {@link RichTextEditor} plug-in for justifying text. It can be used to align text to the left, to the right, in center
 * or to make it expand to fill the entire line. It installs four toggle buttons on the tool bar and updates their
 * status depending on the current cursor position.
 */
public class JustifyPlugin extends StatefulPlugin
{
    private ToggleButton left;

    private ToggleButton center;

    private ToggleButton right;

    private ToggleButton full;

    private final FocusWidgetUIExtension toolBarExtension = new FocusWidgetUIExtension("toolbar");

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#init(Wysiwyg, RichTextArea, Config)
     */
    public void init(Wysiwyg wysiwyg, RichTextArea textArea, Config config)
    {
        super.init(wysiwyg, textArea, config);

        if (getTextArea().getCommandManager().isSupported(Command.JUSTIFY_LEFT)) {
            left = new ToggleButton(Images.INSTANCE.justifyLeft().createImage(), this);
            left.setTitle(Strings.INSTANCE.justifyLeft());
            toolBarExtension.addFeature("justifyleft", left);
        }

        if (getTextArea().getCommandManager().isSupported(Command.JUSTIFY_CENTER)) {
            center = new ToggleButton(Images.INSTANCE.justifyCenter().createImage(), this);
            center.setTitle(Strings.INSTANCE.justifyCenter());
            toolBarExtension.addFeature("justifycenter", center);
        }

        if (getTextArea().getCommandManager().isSupported(Command.JUSTIFY_RIGHT)) {
            right = new ToggleButton(Images.INSTANCE.justifyRight().createImage(), this);
            right.setTitle(Strings.INSTANCE.justifyRight());
            toolBarExtension.addFeature("justifyright", right);
        }

        if (getTextArea().getCommandManager().isSupported(Command.JUSTIFY_FULL)) {
            full = new ToggleButton(Images.INSTANCE.justifyFull().createImage(), this);
            full.setTitle(Strings.INSTANCE.justifyFull());
            toolBarExtension.addFeature("justifyfull", full);
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
        if (left != null) {
            left.removeFromParent();
            left.removeClickListener(this);
            left = null;
        }

        if (center != null) {
            center.removeFromParent();
            center.removeClickListener(this);
            center = null;
        }

        if (right != null) {
            right.removeFromParent();
            right.removeClickListener(this);
            right = null;
        }

        if (full != null) {
            full.removeFromParent();
            full.removeClickListener(this);
            full = null;
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
        if (sender == left) {
            onJustifyLeft();
        } else if (sender == center) {
            onJustifyCenter();
        } else if (sender == right) {
            onJustifyRight();
        } else if (sender == full) {
            onJustifyFull();
        } else {
            super.onClick(sender);
        }
    }

    public void onJustifyLeft()
    {
        if (left.isEnabled()) {
            getTextArea().getCommandManager().execute(Command.JUSTIFY_LEFT);
        }
    }

    public void onJustifyCenter()
    {
        if (center.isEnabled()) {
            getTextArea().getCommandManager().execute(Command.JUSTIFY_CENTER);
        }
    }

    public void onJustifyRight()
    {
        if (right.isEnabled()) {
            getTextArea().getCommandManager().execute(Command.JUSTIFY_RIGHT);
        }
    }

    public void onJustifyFull()
    {
        if (full.isEnabled()) {
            getTextArea().getCommandManager().execute(Command.JUSTIFY_FULL);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see StatefulPlugin#onUpdate()
     */
    public void onUpdate()
    {
        if (left != null) {
            left.setDown(getTextArea().getCommandManager().isExecuted(Command.JUSTIFY_LEFT));
        }
        if (center != null) {
            center.setDown(getTextArea().getCommandManager().isExecuted(Command.JUSTIFY_CENTER));
        }
        if (right != null) {
            right.setDown(getTextArea().getCommandManager().isExecuted(Command.JUSTIFY_RIGHT));
        }
        if (full != null) {
            full.setDown(getTextArea().getCommandManager().isExecuted(Command.JUSTIFY_FULL));
        }
    }
}
