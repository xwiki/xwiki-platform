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
package com.xpn.xwiki.wysiwyg.client.plugin.link.ui;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.gwt.api.client.Document;
import com.xpn.xwiki.wysiwyg.client.util.StringUtils;

/**
 * Widget to create a preview of an XWiki document.
 * 
 * @version $Id$
 */
public class PagePreviewWidget extends Composite
{
    /**
     * The document for which this widget creates a preview.
     */
    protected Document doc;

    /**
     * Builds a preview widget for the passed document.
     * 
     * @param doc the document for which the preview is created
     */
    public PagePreviewWidget(Document doc)
    {
        this.doc = doc;
        initWidget(getUI());
    }

    /**
     * @return the ui of this preview widget. To be overriden by subclasses to provide specific UI.
     */
    protected Widget getUI()
    {
        Label pageName = new Label(doc.getFullName());
        pageName.addStyleName("xPagePreviewFullname");
        Label title = new Label(doc.getTitle());
        title.addStyleName("xPagePreviewTitle");

        FlowPanel ui = new FlowPanel();
        if (!StringUtils.isEmpty(doc.getTitle())) {
            ui.add(title);
        }
        String prettyName = StringUtils.isEmpty(doc.getTitle()) ? "" : doc.getTitle() + " - ";
        prettyName += doc.getFullName();
        ui.setTitle(prettyName);
        ui.add(pageName);
        ui.addStyleName("xPagePreview");
        return ui;
    }

    /**
     * @return the doc
     */
    public Document getDocument()
    {
        return doc;
    }
}
