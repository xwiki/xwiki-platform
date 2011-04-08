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
package org.xwiki.gwt.wysiwyg.client.plugin.separator;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;

/**
 * User interface extension that provides ways of separating tool bar entries. We currently support a vertical bar
 * separator and a new line separator.
 * 
 * @version $Id$
 */
public class ToolBarSeparator extends AbstractSeparator
{
    /**
     * The string used in configurations to place a vertical bar on the tool bar.
     */
    public static final String VERTICAL_BAR = "|";

    /**
     * The string used in configurations to split the tool bar in multiple lines.
     */
    public static final String LINE_BREAK = "/";

    /**
     * Creates a new tool bar separator.
     */
    public ToolBarSeparator()
    {
        // The extension point is the tool bar
        super("toolbar");
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSeparator#getFeatures()
     */
    public String[] getFeatures()
    {
        return new String[] {VERTICAL_BAR, LINE_BREAK};
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSeparator#getUIObject(String)
     */
    public UIObject getUIObject(String feature)
    {
        if (VERTICAL_BAR.equals(feature)) {
            return newVerticalBar();
        } else if (LINE_BREAK.equals(feature)) {
            return newLineBreak();
        } else {
            return null;
        }
    }

    /**
     * @return a new vertical bar to separate the widgets placed on the tool bar.
     */
    public Widget newVerticalBar()
    {
        FlowPanel separator = new FlowPanel();
        separator.addStyleName("separator");
        return separator;
    }

    /**
     * @return a new line break to split the tool bar in multiple lines.
     */
    public Widget newLineBreak()
    {
        FlowPanel lineBreak = new FlowPanel();
        lineBreak.addStyleName("clearfloats");
        return lineBreak;
    }
}
