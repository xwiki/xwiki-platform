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
package com.xpn.xwiki.wysiwyg.client.widget;

import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

/**
 * Widget that wraps XWikiExplorer SmartClient-based widget.
 *
 * @version $Id$
 */
public class XWikiExplorer extends Widget {

    /**
     * Constructor.
     *
     * @param width Desired width in pixels.
     * @param height Desired height in pixels.
     * @param defaultNode Node that should be selected when the tree is loaded.
     * @param displayAttachments True if the attachments should be displayed in the tree.
     * @param displayLinks True if the nodes should be links to the actual resource.
     */
    public XWikiExplorer(int width, int height, String defaultNode, boolean displayAttachments, boolean displayLinks)
    {
        Element wrapper = DOM.createElement("div");
        wrapper.setId("explorerWrapper");
        String parameters = "{"
                + "width:" + width
                + ",height:" + height
                + ",defaultNode:'" + defaultNode + "'"
                + ",displayAttachments:" + displayAttachments
                + ",displayLinks:" + displayLinks
                + ",htmlElement:'" + wrapper.getId() + "'"
                + "}";
        Element script = DOM.createElement("script");
        DOM.setElementProperty(script, "type", "text/javascript");
        DOM.setInnerText(script, "XWiki.xwikiExplorer.create(" + parameters + ");");
        wrapper.appendChild(script);
        setElement(wrapper);
    }

    /**
     * Get the value of the explorer. The value is the fullName of the selected resource.
     * Examples: "xwiki:Main.WebHome", "Main.WebHome", "Main.WebHome@attachment.zip".
     *
     * @return FullName of the selected resource.
     */
    public String getValue()
    {
        return TextBox.wrap(DOM.getElementById("xwikiExplorerInput")).getText();
    }
};