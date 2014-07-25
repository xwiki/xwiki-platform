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
package org.xwiki.gwt.wysiwyg.client.widget.explorer;

import org.xwiki.gwt.wysiwyg.client.wiki.EntityReference;

import com.google.gwt.core.client.JavaScriptObject;
import com.smartgwt.client.widgets.BaseWidget;
import com.smartgwt.client.widgets.tree.TreeGrid;

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
     * Static method allowing to get the XWikiExplorer, creates the widget from the JavaScriptObject argument if it is
     * not null.
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
    public void setWiki(String wiki)
    {
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
     * @param displayWikiNodesDisabled displayWikiNodesDisabled, Default value is false.
     */
    public void setDisplayWikiNodesDisabled(boolean displayWikiNodesDisabled)
    {
        setAttribute("displayWikiNodesDisabled", displayWikiNodesDisabled, true);
    }

    /**
     * @param displaySpaceNodesDisabled displayAddAttachmentOnTop, Default value is false.
     */
    public void setDisplaySpaceNodesDisabled(boolean displaySpaceNodesDisabled)
    {
        setAttribute("displaySpaceNodesDisabled", displaySpaceNodesDisabled, true);
    }

    /**
     * Get the name of the resource selected in the Tree. Examples: "xwiki", "xwiki:Main", "xwiki:Main.WebHome".
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
        return self.getSelectedResourceProperty("wiki");
    }-*/;

    /**
     * Get the name of the space selected in the Tree.
     * 
     * @return the name of the space selected in the Tree, empty string if none selected.
     */
    public native String getSelectedSpace()
    /*-{
        var self = this.@com.smartgwt.client.widgets.BaseWidget::getOrCreateJsObj()();
        return self.getSelectedResourceProperty("space");
    }-*/;

    /**
     * Get the name of the page selected in the Tree.
     * 
     * @return the name of the page selected in the Tree, empty string if none selected.
     */
    public native String getSelectedPage()
    /*-{
        var self = this.@com.smartgwt.client.widgets.BaseWidget::getOrCreateJsObj()();
        return self.getSelectedResourceProperty("name");
    }-*/;

    /**
     * Get the name of the attachment selected in the Tree.
     * 
     * @return the name of the attachment selected in the Tree, empty string if none selected.
     */
    public native String getSelectedAttachment()
    /*-{
        var self = this.@com.smartgwt.client.widgets.BaseWidget::getOrCreateJsObj()();
        return self.getSelectedResourceProperty("attachment");
    }-*/;

    /**
     * Get the name of the anchor selected in the Tree.
     * 
     * @return the name of the anchor selected in the Tree, empty string if none selected.
     */
    public native String getSelectedAnchor()
    /*-{
        var self = this.@com.smartgwt.client.widgets.BaseWidget::getOrCreateJsObj()();
        return self.getSelectedResourceProperty("anchor");
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
     * @return true if the selected node is a new page, created by clicking on a "New Page" node, false otherwise.
     */
    public native boolean isNewPageSelectedFromTreeNode()
    /*-{
        var self = this.@com.smartgwt.client.widgets.BaseWidget::getOrCreateJsObj()();
        return (self.isNewPage() && (self.getValue() == ""));
    }-*/;

    /**
     * @return true if the selected node is a new page, created by typing text in the suggest input, false otherwise.
     */
    public native boolean isNewPageSelectedFromTextInput()
    /*-{
        var self = this.@com.smartgwt.client.widgets.BaseWidget::getOrCreateJsObj()();
        return (self.isNewPage() && (self.getValue() != ""));
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
     * {@inheritDoc}. Override this function because it sets the height of the inner tree grid, without the explorer
     * input. So, we need this function to take into account the size taken by all elements under the tree (the input,
     * its margin, etc).
     */
    @Override
    public void setHeight(String height)
    {
        String pixelSize = "px";
        if (height.endsWith(pixelSize)) {
            String valueString = height.substring(0, height.length() - 2);
            // because we cannot know the exact space taken by all the other elements besides the input under the tree,
            // we proceed by delta adjustments: compute the difference between the ordered height and the current height
            // and apply this difference on the inner tree
            int desiredsize = Integer.parseInt(valueString);
            // let's hope it will never scroll
            int actualHeight = getWrapperClientHeight();
            int deltaHeight = desiredsize - actualHeight;
            // assume the tree will never have border, or some other things around it and use its offsetHeight. However,
            // correct should be the clientHeight
            super.setHeight((getOffsetHeight() + deltaHeight) + pixelSize);
            // now remove the height of the input
        } else {
            super.setHeight(height);
        }
    }

    /**
     * Get the height of the HTML wrapper element, if it's there. Otherwise return 0.
     * 
     * @return the client height of the html wrapper, if there.
     */
    private native int getWrapperClientHeight()
    /*-{
        var self = this.@com.smartgwt.client.widgets.BaseWidget::getOrCreateJsObj()();
        if (typeof self.htmlElement == "undefined" && self.htmlElement == null) {
           return 0;
        } else {
            return self.htmlElement.clientHeight;
        }
    }-*/;

    /**
     * Selects the tree node corresponding to the specified entity and anchor.
     * 
     * @param entityReference an entity reference
     * @param anchor a string identifying a fragment of the specified entity
     */
    public native void selectEntity(EntityReference entityReference, String anchor)
    /*-{
        var self = this.@com.smartgwt.client.widgets.BaseWidget::getOrCreateJsObj()();
        self.selectResource({
            wiki: entityReference.@org.xwiki.gwt.wysiwyg.client.wiki.EntityReference::getComponent(Ljava/lang/String;)('wikiName'),
            space: entityReference.@org.xwiki.gwt.wysiwyg.client.wiki.EntityReference::getComponent(Ljava/lang/String;)('spaceName'),
            name: entityReference.@org.xwiki.gwt.wysiwyg.client.wiki.EntityReference::getComponent(Ljava/lang/String;)('pageName'),
            attachment: entityReference.@org.xwiki.gwt.wysiwyg.client.wiki.EntityReference::getComponent(Ljava/lang/String;)('fileName'),
            anchor: anchor
        });
    }-*/;
}
