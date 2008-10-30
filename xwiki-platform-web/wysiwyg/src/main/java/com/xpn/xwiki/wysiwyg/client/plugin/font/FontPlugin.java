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

import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.Wysiwyg;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.AbstractPlugin;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.FocusWidgetUIExtension;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.StatefulPlugin;
import com.xpn.xwiki.wysiwyg.client.ui.Strings;
import com.xpn.xwiki.wysiwyg.client.ui.XRichTextArea;
import com.xpn.xwiki.wysiwyg.client.ui.cmd.Command;
import com.xpn.xwiki.wysiwyg.client.util.Config;

public class FontPlugin extends StatefulPlugin implements ChangeListener
{
    public static final FontFamily[] FAMILIES;

    private ListBox families;

    private ListBox sizes;

    private final FocusWidgetUIExtension toolBarExtension = new FocusWidgetUIExtension("toolbar");

    static {
        FAMILIES =
            new FontFamily[] {new FontFamily("Andale Mono", "andale mono,times"),
            new FontFamily("Arial", "arial,helvetica,sans-serif"),
            new FontFamily("Arial Black", "arial black,avant garde"),
            new FontFamily("Book Antiqua", "book antiqua,palatino"),
            new FontFamily("Comic Sans MS", "comic sans ms,sand"),
            new FontFamily("Courier New", "courier new,courier"), new FontFamily("Georgia", "georgia,palatino"),
            new FontFamily("Helvetica", "helvetica"), new FontFamily("Impact", "impact,chicago"),
            new FontFamily("Symbol", "symbol"), new FontFamily("Tahoma", "tahoma,arial,helvetica,sans-serif"),
            new FontFamily("Terminal", "terminal,monaco"), new FontFamily("Times New Roman", "times new roman,times"),
            new FontFamily("Trebuchet MS", "trebuchet ms,geneva"), new FontFamily("Verdana", "verdana,geneva"),
            new FontFamily("Webdings", "webdings"), new FontFamily("Wingdings", "wingdings,zapf dingbats")};
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#init(Wysiwyg, XRichTextArea, Config)
     */
    public void init(Wysiwyg wysiwyg, XRichTextArea textArea, Config config)
    {
        super.init(wysiwyg, textArea, config);

        if (getTextArea().getCommandManager().isSupported(Command.FONT_NAME)) {
            families = new ListBox(false);
            families.addChangeListener(this);
            families.setVisibleItemCount(1);
            families.setTitle(Strings.INSTANCE.font());

            for (int i = 0; i < FAMILIES.length; i++) {
                families.addItem(FAMILIES[i].getName());
            }

            toolBarExtension.addFeature("fontname", families);
        }

        if (getTextArea().getCommandManager().isSupported(Command.FONT_SIZE)) {
            sizes = new ListBox(false);
            sizes.addChangeListener(this);
            sizes.setVisibleItemCount(1);
            sizes.setTitle(Strings.INSTANCE.fontSize());

            sizes.addItem("8pt");
            sizes.addItem("10pt");
            sizes.addItem("12pt");
            sizes.addItem("14pt");
            sizes.addItem("18pt");
            sizes.addItem("24pt");
            sizes.addItem("36pt");

            toolBarExtension.addFeature("fontsize", sizes);
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
        if (families != null) {
            families.removeFromParent();
            families.removeChangeListener(this);
            families = null;
        }

        if (sizes != null) {
            sizes.removeFromParent();
            sizes.removeChangeListener(this);
            sizes = null;
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
     * @see ChangeListener#onChange(Widget)
     */
    public void onChange(Widget sender)
    {
        if (sender == families) {
            onFamily();
        } else if (sender == sizes) {
            onSize();
        }
    }

    public void onFamily()
    {
        if (families.isEnabled()) {
            String selectedFamily = FAMILIES[families.getSelectedIndex()].getStringValue();
            getTextArea().getCommandManager().execute(Command.FONT_NAME, selectedFamily);
        }
    }

    public void onSize()
    {
        if (sizes.isEnabled()) {
            int size = sizes.getSelectedIndex() + 1;
            getTextArea().getCommandManager().execute(Command.FONT_SIZE, size);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see StatefulPlugin#onUpdate()
     */
    public void onUpdate()
    {
        if (families != null) {
            String family = getTextArea().getCommandManager().getStringValue(Command.FONT_NAME);
            int index = indexOfFamily(family);
            if (index < 0) {
                // We cannot detect the default font name (before font name command is executed) so we suppose
                // it is "sans-serif". This value should match the one present in the RichTextArea.css file or the
                // browser's default value.
                families.setSelectedIndex(1);
            } else {
                families.setSelectedIndex(index);
            }
        }

        if (sizes != null) {
            Integer size = getTextArea().getCommandManager().getIntegerValue(Command.FONT_SIZE);
            if (size != null && size.intValue() > 0 && size.intValue() < 8) {
                sizes.setSelectedIndex(size.intValue() - 1);
            } else {
                // We cannot detect the default font size (before font size command is executed) so we suppose
                // it is 3 (which corresponds to 12pt). This value should match the one present in the RichTextArea.css
                // file or the browser's default value.
                sizes.setSelectedIndex(2);
            }
        }
    }

    private static int indexOfFamily(String family)
    {
        if (family == null) {
            return -1;
        }
        String[] alt = family.trim().toLowerCase().split("\\s*,\\s*");
        int index = 0;
        int max = FAMILIES[0].match(alt);
        for (int i = 1; i < FAMILIES.length; i++) {
            int match = FAMILIES[i].match(alt);
            if (match > max) {
                max = match;
                index = i;
            }
        }
        if (max > 0) {
            return index;
        } else {
            return -1;
        }
    }
}
