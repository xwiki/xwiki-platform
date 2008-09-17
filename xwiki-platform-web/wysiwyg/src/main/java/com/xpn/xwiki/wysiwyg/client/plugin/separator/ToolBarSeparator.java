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

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.plugin.UIExtension;

/**
 * User interface extension that provides ways of separating tool bar entries. We currently support a vertical bar
 * separator and a new line separator.
 */
public class ToolBarSeparator extends AbstractSeparator
{
    public static final String VERTICAL_BAR = "|";

    public static final String LINE_BREAK = "/";

    public ToolBarSeparator()
    {
        // The extension point is the tool bar
        super("toolbar");
    }

    /**
     * {@inheritDoc}
     * 
     * @see UIExtension#getFeatures()
     */
    public String[] getFeatures()
    {
        return new String[] {VERTICAL_BAR, LINE_BREAK};
    }

    /**
     * {@inheritDoc}
     * 
     * @see UIExtension#getUIObject(String)
     */
    public UIObject getUIObject(String feature)
    {
        if ("|".equals(feature)) {
            return newVerticalBar();
        } else if ("/".equals(feature)) {
            return newLineBreak();
        } else {
            return null;
        }
    }

    public Widget newVerticalBar()
    {
        FlowPanel separator = new FlowPanel();
        separator.addStyleName("separator");
        return separator;
    }

    public Widget newLineBreak()
    {
        FlowPanel lineBreak = new FlowPanel();
        lineBreak.addStyleName("clearfloats");
        return lineBreak;
    }
}
