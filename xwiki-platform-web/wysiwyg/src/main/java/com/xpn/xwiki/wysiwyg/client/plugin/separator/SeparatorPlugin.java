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
package com.xpn.xwiki.wysiwyg.client.plugin.separator;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.Wysiwyg;
import com.xpn.xwiki.wysiwyg.client.plugin.Config;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.AbstractPlugin;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.CompositeUIExtension;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.FocusWidgetUIExtension;
import com.xpn.xwiki.wysiwyg.client.ui.Images;
import com.xpn.xwiki.wysiwyg.client.ui.Strings;
import com.xpn.xwiki.wysiwyg.client.ui.XRichTextArea;
import com.xpn.xwiki.wysiwyg.client.ui.cmd.Command;

/**
 * Utility plug-in for separating tool bar entries, menu entries and so on.
 */
public class SeparatorPlugin extends AbstractPlugin implements ClickListener
{
    private PushButton hr;

    private final FocusWidgetUIExtension toolBarFocusWidgets = new FocusWidgetUIExtension("toolbar");

    private final CompositeUIExtension toolBarExtension = new CompositeUIExtension("toolbar");

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#init(Wysiwyg, XRichTextArea, Config)
     */
    public void init(Wysiwyg wysiwyg, XRichTextArea textArea, Config config)
    {
        super.init(wysiwyg, textArea, config);

        // User interface extension that provides ways of separating tool bar entries.
        toolBarExtension.addUIExtension(new ToolBarSeparator());
        // User interface extension for separator widgets that can be focused.
        toolBarExtension.addUIExtension(toolBarFocusWidgets);

        if (getTextArea().getCommandManager().isSupported(Command.INSERT_HORIZONTAL_RULE)) {
            hr = new PushButton(Images.INSTANCE.hr().createImage(), this);
            hr.setTitle(Strings.INSTANCE.hr());
            toolBarFocusWidgets.addFeature("hr", hr);
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
        if (hr != null) {
            hr.removeFromParent();
            hr.removeClickListener(this);
            hr = null;
        }

        toolBarFocusWidgets.clearFeatures();

        super.destroy();
    }

    /**
     * {@inheritDoc}
     * 
     * @see ClickListener#onClick(Widget)
     */
    public void onClick(Widget sender)
    {
        if (sender == hr) {
            onHorizontalRule();
        }
    }

    public void onHorizontalRule()
    {
        if (hr.isEnabled()) {
            getTextArea().getCommandManager().execute(Command.INSERT_HORIZONTAL_RULE);
        }
    }
}
