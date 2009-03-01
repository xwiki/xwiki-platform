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

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.Wysiwyg;
import com.xpn.xwiki.wysiwyg.client.dom.Element;
import com.xpn.xwiki.wysiwyg.client.editor.Images;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.AbstractStatefulPlugin;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.FocusWidgetUIExtension;
import com.xpn.xwiki.wysiwyg.client.plugin.list.exec.ListExecutable;
import com.xpn.xwiki.wysiwyg.client.util.Config;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;

/**
 * Plug-in for inserting ordered (numbered) and unordered (bullet) lists. It installs two toggle buttons on the tool bar
 * and updates their status depending on the current cursor position.
 * 
 * @version $Id$
 */
public class ListPlugin extends AbstractStatefulPlugin implements ClickListener
{
    /**
     * The tool bar button that toggles the order list.
     */
    private ToggleButton ol;

    /**
     * The tool bar button that toggles the unordered list.
     */
    private ToggleButton ul;

    /**
     * User interface extension for the editor menu bar.
     */
    private final FocusWidgetUIExtension toolBarExtension = new FocusWidgetUIExtension("toolbar");

    /**
     * List behaviour adjuster to manage valid lists correctly.
     */
    private ListBehaviorAdjuster behaviorAdjuster;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractStatefulPlugin#init(Wysiwyg, RichTextArea, Config)
     */
    public void init(Wysiwyg wysiwyg, RichTextArea textArea, Config config)
    {
        super.init(wysiwyg, textArea, config);

        if (getTextArea().getCommandManager().isSupported(Command.INSERT_ORDERED_LIST)) {
            ol = new ToggleButton(Images.INSTANCE.ol().createImage(), this);
            ol.setTitle(Strings.INSTANCE.ol());
            toolBarExtension.addFeature("orderedlist", ol);
            // overwrite default executable with the ListExecutable, to handle valid html lists
            getTextArea().getCommandManager().registerCommand(Command.INSERT_ORDERED_LIST, new ListExecutable(true));
        }

        if (getTextArea().getCommandManager().isSupported(Command.INSERT_UNORDERED_LIST)) {
            ul = new ToggleButton(Images.INSTANCE.ul().createImage(), this);
            ul.setTitle(Strings.INSTANCE.ul());
            toolBarExtension.addFeature("unorderedlist", ul);
            // overwrite the default list command executables with the ListExecutable, to handle valid html lists
            getTextArea().getCommandManager().registerCommand(Command.INSERT_UNORDERED_LIST, new ListExecutable(false));
        }

        if (toolBarExtension.getFeatures().length > 0) {
            getTextArea().addMouseListener(this);
            getTextArea().addKeyboardListener(this);
            getTextArea().getCommandManager().addCommandListener(this);
            getUIExtensionList().add(toolBarExtension);

            // Initialize the behavior adjuster and set it up with this text area
            behaviorAdjuster = (ListBehaviorAdjuster) GWT.create(ListBehaviorAdjuster.class);
            behaviorAdjuster.setTextArea(getTextArea());
            // handle all list item elements in the loaded document
            behaviorAdjuster.onInnerHTMLChange(Element.as(getTextArea().getDocument().getBody()));
            // add key listener to the rta
            getTextArea().addKeyboardListener(behaviorAdjuster);
            getTextArea().getCommandManager().addCommandListener(behaviorAdjuster);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractStatefulPlugin#destroy()
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
            getTextArea().removeMouseListener(this);
            getTextArea().removeKeyboardListener(this);
            getTextArea().getCommandManager().removeCommandListener(this);
            toolBarExtension.clearFeatures();
            // if a behaviorAdjuster was created and attached, remove it
            if (behaviorAdjuster != null) {
                getTextArea().removeKeyboardListener(behaviorAdjuster);
                getTextArea().getCommandManager().removeCommandListener(behaviorAdjuster);
                behaviorAdjuster = null;
            }
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
        }
    }

    /**
     * Toggles the ordered list.
     */
    public void onOrderedList()
    {
        if (ol.isEnabled()) {
            getTextArea().getCommandManager().execute(Command.INSERT_ORDERED_LIST);
        }
    }

    /**
     * Toggles the unordered list.
     */
    public void onUnorderedList()
    {
        if (ul.isEnabled()) {
            getTextArea().getCommandManager().execute(Command.INSERT_UNORDERED_LIST);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractStatefulPlugin#update()
     */
    public void update()
    {
        if (ol != null) {
            ol.setDown(getTextArea().getCommandManager().isExecuted(Command.INSERT_ORDERED_LIST));
        }
        if (ul != null) {
            ul.setDown(getTextArea().getCommandManager().isExecuted(Command.INSERT_UNORDERED_LIST));
        }
    }
}
