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
package com.xpn.xwiki.wysiwyg.client.plugin.misc;

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

public class MiscPlugin extends AbstractPlugin implements ClickListener
{
    private PushButton unformat;

    private PushButton charmap;

    private final FocusWidgetUIExtension toolBarExtension = new FocusWidgetUIExtension("toolbar");

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#init(Wysiwyg, XRichTextArea, Config)
     */
    public void init(Wysiwyg wysiwyg, XRichTextArea textArea, Config config)
    {
        super.init(wysiwyg, textArea, config);

        unformat = new PushButton(Images.INSTANCE.removeFormat().createImage(), this);
        unformat.setTitle(Strings.INSTANCE.removeFormat());
        toolBarExtension.addFeature("unformat", unformat);

        charmap = new PushButton(Images.INSTANCE.charmap().createImage(), this);
        charmap.setTitle(Strings.INSTANCE.charmap());
        toolBarExtension.addFeature("charmap", charmap);

        getUIExtensionList().add(toolBarExtension);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#destroy()
     */
    public void destroy()
    {
        unformat.removeFromParent();
        unformat.removeClickListener(this);
        unformat = null;

        charmap.removeFromParent();
        charmap.removeClickListener(this);
        charmap = null;

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
        if (sender == unformat) {
            onUnformat();
        } else if (sender == charmap) {
            onCharMap();
        }
    }

    public void onUnformat()
    {
        // TODO
    }

    public void onCharMap()
    {
        // TODO
    }
}
