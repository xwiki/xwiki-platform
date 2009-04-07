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
package com.xpn.xwiki.wysiwyg.client.widget.explorer;

import com.google.gwt.core.client.JavaScriptObject;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.BaseWidget;

/**
 * Wrapper for XWikiExplorer SmartClient-based widget.
 *
 * @version $Id$
 */
public class XWikiExplorer extends TreeGrid
{

    /**
     * Constructor.
     */
    public XWikiExplorer()
    {
    }

    /**
     * Constructor. Wraps the JavaScriptObject argument.
     *
     * @param jsObj JavaScript object to wrap.
     */
    public XWikiExplorer(JavaScriptObject jsObj)
    {
        super(jsObj);
    }

    /**
     * Static method allowing to get the XWikiExplorer, creates the widget from the JavaScriptObject argument
     * if it is not null.
     *
     * @param jsObj JavaScript object to wrap if not null.
     * @return XWikiExplorer widget.
     */
    public static XWikiExplorer getOrCreateRef(JavaScriptObject jsObj)
    {
        if (jsObj == null) {
            return null;
        }
        BaseWidget obj = BaseWidget.getRef(jsObj);
        if (obj != null) {
            return (XWikiExplorer) obj;
        } else {
            return new XWikiExplorer(jsObj);
        }
    }

    /**
     * Native JS call that creates the widget.
     *
     * @return XWikiExplorer JavaScript widget.
     */
    protected native JavaScriptObject create()
        /*-{
            var config = this.@com.smartgwt.client.widgets.BaseWidget::getConfig()();
            var widget = $wnd.isc.XWETreeGrid.create(config);
            this.@com.smartgwt.client.widgets.BaseWidget::doInit()();
            return widget;
        }-*/;

    /**
     * @param wiki wiki, Default value is "xwiki".
     */
    public void setWiki(String wiki) {
        setAttribute("wiki", wiki, true);
    }

    /**
     * @param space space, Default value is "".
     */
    public void setSpace(String space)
    {
        setAttribute("space", space, true);
    }

    /**
     * @param defaultValue defaultValue, Default value is "Main.WebHome".
     */
    public void setDefaultValue(String defaultValue)
    {
        setAttribute("defaultValue", defaultValue, true);
    }

    /**
     * @param displaySuggest displaySuggest, Default value is true.
     */
    public void setDisplaySuggest(boolean displaySuggest)
    {
        setAttribute("displaySuggest", displaySuggest, true);
    }

    /**
     * @param displayLinks displayLinks, Default value is true.
     */
    public void setDisplayLinks(boolean displayLinks)
    {
        setAttribute("displayLinks", displayLinks, true);
    }

    /**
     * @param displayAddPage displayAddPage, Default value is false.
     */
    public void setDisplayAddPage(boolean displayAddPage)
    {
        setAttribute("displayAddPage", displayAddPage, true);
    }

    /**
     * @param displayAddPageOnTop displayAddPageOnTop, Default value is true.
     */
    public void setDisplayAddPageOnTop(boolean displayAddPageOnTop)
    {
        setAttribute("displayAddPageOnTop", displayAddPageOnTop, true);
    }

    /**
     * @param displayAttachments displayAttachments, Default value is true.
     */
    public void setDisplayAttachments(boolean displayAttachments)
    {
        setAttribute("displayAttachments", displayAttachments, true);
    }

    /**
     * @param displayAttachmentsOnTop displayAttachmentsOnTop, Default value is true.
     */
    public void setDisplayAttachmentsOnTop(boolean displayAttachmentsOnTop)
    {
        setAttribute("displayAttachmentsOnTop", displayAttachmentsOnTop, true);
    }

    /**
     * @param displayAttachmentsWhenEmpty displayAttachmentsWhenEmpty, Default value is true.
     */
    public void setDisplayAttachmentsWhenEmpty(boolean displayAttachmentsWhenEmpty)
    {
        setAttribute("displayAttachmentsWhenEmpty", displayAttachmentsWhenEmpty, true);
    }

    /**
     * @param displayAddAttachment displayAddAttachment, Default value is false.
     */
    public void setDisplayAddAttachment(boolean displayAddAttachment)
    {
        setAttribute("displayAddAttachment", displayAddAttachment, true);
    }

    /**
     * @param displayAddAttachmentOnTop displayAddAttachmentOnTop, Default value is false.
     */
    public void setDisplayAddAttachmentOnTop(boolean displayAddAttachmentOnTop)
    {
        setAttribute("displayAddAttachmentOnTop", displayAddAttachmentOnTop, true);
    }

    /**
     * Get the name of the resource selected in the Tree.
     * Examples: "xwiki", "xwiki:Main", "xwiki:Main.WebHome".
     *
     * @return Name of the resource selected in the Tree, empty string if none selected.
     */
    public native String getValue()
        /*-{
            var self = this.@com.smartgwt.client.widgets.BaseWidget::getOrCreateJsObj()();
            return self.getValue();
        }-*/;

    /**
     * @param value Set XWikiExplorer suggest input value.
     */
    public native void setValue(String value)
        /*-{
            var self = this.@com.smartgwt.client.widgets.BaseWidget::getOrCreateJsObj()();
            return self.setValue(value);
        }-*/;

    /**
     * Get the name of the wiki selected in the Tree.
     *
     * @return the name of the wiki selected in the Tree, empty string if none selected.
     */
    public native String getSelectedWiki()
        /*-{
            var self = this.@com.smartgwt.client.widgets.BaseWidget::getOrCreateJsObj()();
            return self.getSelectedWiki();
        }-*/;

    /**
     * Get the name of the space selected in the Tree.
     *
     * @return the name of the space selected in the Tree, empty string if none selected.
     */
    public native String getSelectedSpace()
        /*-{
            var self = this.@com.smartgwt.client.widgets.BaseWidget::getOrCreateJsObj()();
            return self.getSelectedSpace();
        }-*/;

    /**
     * Get the name of the page selected in the Tree.
     *
     * @return the name of the page selected in the Tree, empty string if none selected.
     */
    public native String getSelectedPage()
        /*-{
            var self = this.@com.smartgwt.client.widgets.BaseWidget::getOrCreateJsObj()();
            return self.getSelectedPage();
        }-*/;

    /**
     * Get the name of the attachment selected in the Tree.
     *
     * @return the name of the attachment selected in the Tree, empty string if none selected.
     */
    public native String getSelectedAttachment()
        /*-{
            var self = this.@com.smartgwt.client.widgets.BaseWidget::getOrCreateJsObj()();
            return self.getSelectedAttachment();
        }-*/;

    /**
     * Get the name of the anchor selected in the Tree.
     *
     * @return the name of the anchor selected in the Tree, empty string if none selected.
     */
    public native String getSelectedAnchor()
        /*-{
            var self = this.@com.smartgwt.client.widgets.BaseWidget::getOrCreateJsObj()();
            return self.getSelectedAnchor();
        }-*/;

    /**
     * Is the selected resource a new page.
     *
     * @return true if the selected node is a new page.
     */
    public native boolean isNewPage()
        /*-{
            var self = this.@com.smartgwt.client.widgets.BaseWidget::getOrCreateJsObj()();
            return self.isNewPage();
        }-*/;

    /**
     * Is the selected resource a new attachment.
     *
     * @return true if the selected node is a new Attachment.
     */
    public native boolean isNewAttachment()
        /*-{
            var self = this.@com.smartgwt.client.widgets.BaseWidget::getOrCreateJsObj()();
            return self.isNewAttachment();
        }-*/;

    /**
     * Get the URL of the selected node in the Tree.
     *
     * @return the URL of the selected node in the Tree.
     */
    public native String getSelectedUrl()
        /*-{
            var self = this.@com.smartgwt.client.widgets.BaseWidget::getOrCreateJsObj()();
            return self.getSelectedUrl();
        }-*/;

};