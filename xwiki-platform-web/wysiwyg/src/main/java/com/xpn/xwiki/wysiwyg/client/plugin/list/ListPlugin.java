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
package com.xpn.xwiki.wysiwyg.client.plugin.list;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.Wysiwyg;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.AbstractPlugin;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.FocusWidgetUIExtension;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.StatefulPlugin;
import com.xpn.xwiki.wysiwyg.client.ui.Images;
import com.xpn.xwiki.wysiwyg.client.ui.Strings;
import com.xpn.xwiki.wysiwyg.client.ui.XRichTextArea;
import com.xpn.xwiki.wysiwyg.client.ui.XRichTextEditor;
import com.xpn.xwiki.wysiwyg.client.ui.cmd.Command;
import com.xpn.xwiki.wysiwyg.client.util.Config;

/**
 * {@link XRichTextEditor} plug-in for inserting ordered (numbered) and unordered (bullet) lists. It installs two toggle
 * buttons on the tool bar and updates their status depending on the current cursor position.
 */
public class ListPlugin extends StatefulPlugin
{
    private ToggleButton ol;

    private ToggleButton ul;

    private final FocusWidgetUIExtension toolBarExtension = new FocusWidgetUIExtension("toolbar");

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#init(Wysiwyg, XRichTextArea, Config)
     */
    public void init(Wysiwyg wysiwyg, XRichTextArea textArea, Config config)
    {
        super.init(wysiwyg, textArea, config);

        if (getTextArea().getCommandManager().isSupported(Command.INSERT_ORDERED_LIST)) {
            ol = new ToggleButton(Images.INSTANCE.ol().createImage(), this);
            ol.setTitle(Strings.INSTANCE.ol());
            toolBarExtension.addFeature("orderedlist", ol);
        }

        if (getTextArea().getCommandManager().isSupported(Command.INSERT_UNORDERED_LIST)) {
            ul = new ToggleButton(Images.INSTANCE.ul().createImage(), this);
            ul.setTitle(Strings.INSTANCE.ul());
            toolBarExtension.addFeature("unorderedlist", ul);
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
        if (ol != null) {
            ol.removeFromParent();
            ol.removeClickListener(this);
            ol = null;
        }

        if (ul != null) {
            ul.removeFromParent();
            ul.removeClickListener(this);
            ul = null;
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
        if (sender == ol) {
            onOrderedList();
        } else if (sender == ul) {
            onUnorderedList();
        } else {
            super.onClick(sender);
        }
    }

    public void onOrderedList()
    {
        if (ol.isEnabled()) {
            getTextArea().getCommandManager().execute(Command.INSERT_ORDERED_LIST);
        }
    }

    public void onUnorderedList()
    {
        if (ul.isEnabled()) {
            getTextArea().getCommandManager().execute(Command.INSERT_UNORDERED_LIST);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see StatefulPlugin#onUpdate()
     */
    public void onUpdate()
    {
        if (ol != null) {
            ol.setDown(getTextArea().getCommandManager().isExecuted(Command.INSERT_ORDERED_LIST));
        }
        if (ul != null) {
            ul.setDown(getTextArea().getCommandManager().isExecuted(Command.INSERT_UNORDERED_LIST));
        }
    }
}
