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
package com.xpn.xwiki.wysiwyg.client.plugin.format;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.Wysiwyg;
import com.xpn.xwiki.wysiwyg.client.editor.RichTextEditor;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.AbstractPlugin;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.AbstractStatefulPlugin;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.FocusWidgetUIExtension;
import com.xpn.xwiki.wysiwyg.client.util.Config;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;

/**
 * {@link RichTextEditor} plug-in for formatting text. It can be used to format text as heading 1 to 5. It installs a
 * select on the tool bar and updates its status depending on the current cursor position.
 */
public class FormatPlugin extends AbstractStatefulPlugin implements ChangeListener
{
    private static final Map<String, Integer> INDEX;

    private ListBox levels;

    private final FocusWidgetUIExtension toolBarExtension = new FocusWidgetUIExtension("toolbar");

    static {
        int i = 0;
        INDEX = new HashMap<String, Integer>();
        INDEX.put("p", new Integer(i++));
        INDEX.put("h1", new Integer(i++));
        INDEX.put("h2", new Integer(i++));
        INDEX.put("h3", new Integer(i++));
        INDEX.put("h4", new Integer(i++));
        INDEX.put("h5", new Integer(i++));
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#init(Wysiwyg, RichTextArea, Config)
     */
    public void init(Wysiwyg wysiwyg, RichTextArea textArea, Config config)
    {
        super.init(wysiwyg, textArea, config);

        if (getTextArea().getCommandManager().isSupported(Command.FORMAT_BLOCK)) {
            levels = new ListBox(false);
            levels.addChangeListener(this);
            levels.setVisibleItemCount(1);
            levels.setTitle(Strings.INSTANCE.format());

            levels.addItem(Strings.INSTANCE.normal(), "p");
            levels.addItem(Strings.INSTANCE.h1(), "h1");
            levels.addItem(Strings.INSTANCE.h2(), "h2");
            levels.addItem(Strings.INSTANCE.h3(), "h3");
            levels.addItem(Strings.INSTANCE.h4(), "h4");
            levels.addItem(Strings.INSTANCE.h5(), "h5");

            toolBarExtension.addFeature("format", levels);
            getUIExtensionList().add(toolBarExtension);

            getTextArea().addMouseListener(this);
            getTextArea().addKeyboardListener(this);
            getTextArea().getCommandManager().addCommandListener(this);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#destroy()
     */
    public void destroy()
    {
        if (levels != null) {
            levels.removeFromParent();
            levels.removeChangeListener(this);
            levels = null;

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
        if (sender == levels) {
            onFormat();
        }
    }

    public void onFormat()
    {
        if (levels.isEnabled()) {
            String level = levels.getValue(levels.getSelectedIndex());
            getTextArea().getCommandManager().execute(Command.FORMAT_BLOCK, level);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractStatefulPlugin#onUpdate()
     */
    public void onUpdate()
    {
        if (levels != null) {
            String level = getTextArea().getCommandManager().getStringValue(Command.FORMAT_BLOCK);
            boolean failSafe = true;
            if (level != null) {
                Integer index = INDEX.get(level.toLowerCase());
                if (index != null) {
                    levels.setSelectedIndex(index.intValue());
                    failSafe = false;
                }
            }
            if (failSafe) {
                // In case no formatting has been specified (before executing format block command) we consider
                // it to be a paragraph.
                levels.setSelectedIndex(0);
            }
        }
    }
}
