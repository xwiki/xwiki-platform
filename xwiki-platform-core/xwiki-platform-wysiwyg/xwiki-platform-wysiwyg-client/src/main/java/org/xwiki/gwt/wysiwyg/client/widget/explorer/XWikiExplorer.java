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
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Widget;

/**
 * Wrapper for XWikiExplorer SmartClient-based widget.
 * 
 * @version $Id$
 */
public class XWikiExplorer extends Widget implements HasDoubleClickNodeHandlers
{
    /**
     * Whether to refresh the tree content or not, when this widget is attached to the document.
     */
    private boolean refreshOnLoad;

    /**
     * The id of a node that should be selected when the tree is loaded.
     */
    private String nodeToSelectOnLoad;

    /**
     * Creates a new document tree widget.
     * 
     * @param url the URL of the resource that represents the tree
     */
    public XWikiExplorer(String url)
    {
        Element element = DOM.createDiv();
        element.setClassName("xtree jstree-no-links");
        element.setAttribute("data-url", url);
        setElement(element);
    }

    @Override
    protected void onLoad()
    {
        maybeCreateTree();
        super.onLoad();
    }

    @Override
    protected void onUnload()
    {
        maybeCloseFinderSuggestions();
        super.onUnload();
    }

    private native void maybeCreateTree()
    /*-{
        var element = this.@com.google.gwt.user.client.ui.UIObject::getElement()();
        var tree = $wnd.jQuery.jstree.reference(element);
        if (!tree) {
            var self = this;
            $wnd.jQuery(element).xtree({
                core: {
                    // The tree is used to select only one item (e.g. a document or an attachment).
                    multiple: false,
                    // Double click is used to fast-move to the next wizard step.
                    dblclick_toggle: false
                },
                plugins: ['finder']
            }).on('ready.jstree', function(event, data) {
                // Initialize the selection after the tree is ready.
                var nodeId = self.@org.xwiki.gwt.wysiwyg.client.widget.explorer.XWikiExplorer::nodeToSelectOnLoad;
                nodeId && self.@org.xwiki.gwt.wysiwyg.client.widget.explorer.XWikiExplorer::openTo(Lcom/google/gwt/core/client/JavaScriptObject;Ljava/lang/String;)(data.instance, nodeId);
            }).on('dblclick', '.jstree-anchor', function(event) {
                self.@org.xwiki.gwt.wysiwyg.client.widget.explorer.XWikiExplorer::fireDoubleClickNodeEvent()();
            });
        } else if (this.@org.xwiki.gwt.wysiwyg.client.widget.explorer.XWikiExplorer::refreshOnLoad) {
            this.@org.xwiki.gwt.wysiwyg.client.widget.explorer.XWikiExplorer::refreshOnLoad = false;
            tree.refresh(false, true);
        } else {
            // Scroll the selected node back into view because the tree has just been re-attached to the document.
            var selectedNodes = tree.get_selected();
            if (selectedNodes.length > 0) {
                this.@org.xwiki.gwt.wysiwyg.client.widget.explorer.XWikiExplorer::scrollNodeIntoView(Lcom/google/gwt/core/client/JavaScriptObject;Ljava/lang/String;)(tree, selectedNodes[0]);
            }
        }
    }-*/;

    private native void maybeCloseFinderSuggestions()
    /*-{
        $wnd.jQuery('.xtree-finder-suggestions').detach();
    }-*/;

    private native JavaScriptObject getSelectedNode()
    /*-{
        var element = this.@com.google.gwt.user.client.ui.UIObject::getElement()();
        var tree = $wnd.jQuery.jstree.reference(element);
        if (tree) {
            var selectedNodes = tree.get_selected(true);
            if (selectedNodes.length > 0) {
                return selectedNodes[0];
            }
        }
        return null;
    }-*/;

    private native JavaScriptObject getSelectedEntity()
    /*-{
        var element = this.@com.google.gwt.user.client.ui.UIObject::getElement()();
        var tree = $wnd.jQuery.jstree.reference(element);
        var entityNode = this.@org.xwiki.gwt.wysiwyg.client.widget.explorer.XWikiExplorer::getSelectedNode()();
        while (entityNode && !entityNode.data.id) {
            // This is actually a meta node. Check the parent node.
            entityNode = tree.get_node(tree.get_parent(entityNode));
        }
        if (entityNode) {
            var entityType = $wnd.XWiki.EntityType.byName(entityNode.data.type);
            return $wnd.XWiki.Model.resolve(entityNode.data.id, entityType);
        } else {
            // No entity is selected. Try to determine the entity from the value of the finder input.
            var entityId = $wnd.jQuery(element).prev('.xtree-finder').val();
            if (entityId) {
                var parts = entityId.split(':');
                if (parts.length > 1) {
                    var entityType = $wnd.XWiki.EntityType.byName(parts[0]);
                    if (entityType >= 0) {
                        return $wnd.XWiki.Model.resolve(parts.slice(1).join(':'), entityType);
                    }
                }
            }
        }
        return null;
    }-*/;

    private native String getSelectedEntity(int entityType)
    /*-{
        var entityReference = this.@org.xwiki.gwt.wysiwyg.client.widget.explorer.XWikiExplorer::getSelectedEntity()();
        return entityReference ? entityReference.extractReferenceValue(entityType) : null;
    }-*/;

    /**
     * Get the name of the wiki selected in the Tree.
     * 
     * @return the name of the wiki selected in the Tree, empty string if none selected.
     */
    public String getSelectedWiki()
    {
        return getSelectedEntity(0);
    }

    /**
     * Get the name of the space selected in the Tree.
     * 
     * @return the name of the space selected in the Tree, empty string if none selected.
     */
    public String getSelectedSpace()
    {
        return getSelectedEntity(1);
    }

    /**
     * Get the name of the page selected in the Tree.
     * 
     * @return the name of the page selected in the Tree, empty string if none selected.
     */
    public String getSelectedPage()
    {
        return getSelectedEntity(2);
    }

    /**
     * Get the name of the attachment selected in the Tree.
     * 
     * @return the name of the attachment selected in the Tree, empty string if none selected.
     */
    public String getSelectedAttachment()
    {
        return getSelectedEntity(3);
    }

    /**
     * Is the selected resource a new page.
     * 
     * @return true if the selected node is a new page.
     */
    public native boolean isNewPage()
    /*-{
        var selectedNode = this.@org.xwiki.gwt.wysiwyg.client.widget.explorer.XWikiExplorer::getSelectedNode()();
        return selectedNode && selectedNode.data.type === 'addDocument';
    }-*/;

    /**
     * Is the selected resource a new attachment.
     *
     * @return true if the selected node is a new Attachment.
     */
    public native boolean isNewAttachment()
    /*-{
        var selectedNode = this.@org.xwiki.gwt.wysiwyg.client.widget.explorer.XWikiExplorer::getSelectedNode()();
        return selectedNode && selectedNode.data.type === 'addAttachment';
    }-*/;

    /**
     * Selects the tree node corresponding to the specified entity and anchor.
     * 
     * @param entityReference an entity reference
     * @param anchor a string identifying a fragment of the specified entity
     */
    public native void selectEntity(EntityReference entityReference, String anchor)
    /*-{
        var wiki = entityReference.@org.xwiki.gwt.wysiwyg.client.wiki.EntityReference::getComponent(Ljava/lang/String;)('wikiName');
        var space = entityReference.@org.xwiki.gwt.wysiwyg.client.wiki.EntityReference::getComponent(Ljava/lang/String;)('spaceName');
        var page = entityReference.@org.xwiki.gwt.wysiwyg.client.wiki.EntityReference::getComponent(Ljava/lang/String;)('pageName');
        var file = entityReference.@org.xwiki.gwt.wysiwyg.client.wiki.EntityReference::getComponent(Ljava/lang/String;)('fileName');

        var nodeId, documentReference = new $wnd.XWiki.DocumentReference(wiki, space, page);
        if (file) {
            var attachmentReference = new $wnd.XWiki.AttachmentReference(file, documentReference);
            nodeId = 'attachment:' + $wnd.XWiki.Model.serialize(attachmentReference);
        } else if (anchor === 'Attachments') {
            nodeId = 'attachments:' + $wnd.XWiki.Model.serialize(documentReference);
        } else if (page) {
            nodeId = 'document:' + $wnd.XWiki.Model.serialize(documentReference);
        } else {
            var spaceReference = new $wnd.XWiki.SpaceReference(wiki, space);
            nodeId = 'addDocument:space:' + $wnd.XWiki.Model.serialize(spaceReference);
        }

        var element = this.@com.google.gwt.user.client.ui.UIObject::getElement()();
        var tree = $wnd.jQuery.jstree.reference(element);
        if (tree) {
            tree.deselect_all();
            tree.close_all();
            this.@org.xwiki.gwt.wysiwyg.client.widget.explorer.XWikiExplorer::openTo(Lcom/google/gwt/core/client/JavaScriptObject;Ljava/lang/String;)(tree, nodeId);
        } else {
            // The tree is not loaded yet. Save the node id and use it to select the node after the tree is loaded.
            this.@org.xwiki.gwt.wysiwyg.client.widget.explorer.XWikiExplorer::nodeToSelectOnLoad = nodeId;
        }
    }-*/;

    private native void openTo(JavaScriptObject tree, String nodeId)
    /*-{
        // Mark the tree as busy so that the functional tests can wait for it.
        tree.element.attr('aria-busy', true);

        var self = this;
        tree.openTo(nodeId, function(node) {
            // The last node in the path may be different than the requested node, e.g. when the target document or
            // attachment doesn't exist.
            if (node.id === nodeId && tree.select_node(node) !== false) {
                self.@org.xwiki.gwt.wysiwyg.client.widget.explorer.XWikiExplorer::scrollNodeIntoView(Lcom/google/gwt/core/client/JavaScriptObject;Ljava/lang/String;)(tree, nodeId);
            } else {
                // The requested node could not be found. Set the value of the finder.
                tree.element.prev('.xtree-finder').val(nodeId);
            }

            // Remove the busy marker.
            tree.element.attr('aria-busy', false);
        });
    }-*/;

    private native void scrollNodeIntoView(JavaScriptObject tree, String nodeId)
    /*-{
        // Scroll only the node label into view because the entire node may take a lot of vertical space due to its
        // descendants (when the node is expanded).
        tree.get_node(nodeId, true).children('.jstree-anchor')[0].scrollIntoView(false);
    }-*/;

    /**
     * @return {@code true} if a node is selected in the tree, {@code false} otherwise
     */
    public boolean hasSelectedNode()
    {
        return getSelectedNode() != null;
    }

    @Override
    public HandlerRegistration addDoubleClickNodeHandler(DoubleClickNodeHandler handler)
    {
        return addHandler(handler, DoubleClickNodeEvent.TYPE);
    }

    private void fireDoubleClickNodeEvent()
    {
        fireEvent(new DoubleClickNodeEvent());
    }

    /**
     * Reload the tree content next time it is displayed again. Call this method whenever new data is added to the tree
     * (e.g. new attachment, new page).
     */
    public void invalidateCache()
    {
        refreshOnLoad = true;
    }
}
