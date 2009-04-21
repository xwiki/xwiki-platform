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
package com.xpn.xwiki.wysiwyg.client.plugin.font;

import java.util.HashMap;
import java.util.Map;

import org.xwiki.gwt.dom.client.Style;

import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.Wysiwyg;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.AbstractStatefulPlugin;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.FocusWidgetUIExtension;
import com.xpn.xwiki.wysiwyg.client.util.Config;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.internal.InlineStyleExecutable;

/**
 * Plug-in for manipulating the font size and font family used while editing.
 * 
 * @version $Id$
 */
public class FontPlugin extends AbstractStatefulPlugin implements ChangeListener
{
    /**
     * The association between pickers and the commands that are executed on change events.
     */
    private final Map<Picker, Command> pickers = new HashMap<Picker, Command>();

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

        // Register custom executables.
        getTextArea().getCommandManager().registerCommand(Command.FONT_NAME,
            new InlineStyleExecutable(Style.FONT_FAMILY));
        getTextArea().getCommandManager()
            .registerCommand(Command.FONT_SIZE, new InlineStyleExecutable(Style.FONT_SIZE));

        addFontNameFeature();
        addFontSizeFeature();

        if (toolBarExtension.getFeatures().length > 0) {
            getTextArea().addMouseListener(this);
            getTextArea().addKeyboardListener(this);
            getTextArea().getCommandManager().addCommandListener(this);
            getUIExtensionList().add(toolBarExtension);
        }
    }

    /**
     * Makes the font name feature available to be used on the tool bar.
     */
    private void addFontNameFeature()
    {
        if (getTextArea().getCommandManager().isSupported(Command.FONT_NAME)) {
            FontFamilyPicker picker = new FontFamilyPicker();
            picker.setTitle(Strings.INSTANCE.font());
            picker.addChangeListener(this);

            picker.addItem("andale mono");
            picker.addItem("arial");
            picker.addItem("arial black");
            picker.addItem("book antiqua");
            picker.addItem("comic sans ms");
            picker.addItem("courier new");
            picker.addItem("georgia");
            picker.addItem("helvetica");
            picker.addItem("impact");
            picker.addItem("symbol");
            picker.addItem("tahoma");
            picker.addItem("terminal");
            picker.addItem("times new roman");
            picker.addItem("trebuchet ms");
            picker.addItem("verdana");
            picker.addItem("webdings");
            picker.addItem("wingdings");

            toolBarExtension.addFeature("fontname", picker);
            pickers.put(picker, Command.FONT_NAME);
        }
    }

    /**
     * Makes the font size feature available to be used on the tool bar.
     */
    private void addFontSizeFeature()
    {
        if (getTextArea().getCommandManager().isSupported(Command.FONT_SIZE)) {
            FontSizePicker picker = new FontSizePicker();
            picker.setTitle(Strings.INSTANCE.fontSize());
            picker.addChangeListener(this);

            picker.addItem("8pt");
            picker.addItem("10pt");
            picker.addItem("12pt");
            picker.addItem("14pt");
            picker.addItem("18pt");
            picker.addItem("24pt");
            picker.addItem("36pt");

            toolBarExtension.addFeature("fontsize", picker);
            pickers.put(picker, Command.FONT_SIZE);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractStatefulPlugin#destroy()
     */
    public void destroy()
    {
        for (Picker picker : pickers.keySet()) {
            ((Widget) picker).removeFromParent();
            picker.removeChangeListener(this);
        }
        pickers.clear();

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
     * @see ChangeListener#onChange(Widget)
     */
    public void onChange(Widget sender)
    {
        Command command = pickers.get(sender);
        if (command != null && ((FocusWidget) sender).isEnabled()) {
            getTextArea().getCommandManager().execute(command, ((Picker) sender).getSelectedValue());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractStatefulPlugin#update()
     */
    public void update()
    {
        for (Map.Entry<Picker, Command> entry : pickers.entrySet()) {
            entry.getKey().setSelectedValue(getTextArea().getCommandManager().getStringValue(entry.getValue()));
        }
    }
}
