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

if (typeof XWiki == "undefined") {
    alert("ERROR: xwikiexplorer.js depends on xwiki.js");
}

/*
 * REST constants.
 */
XWiki.constants.rest = {

    /**
     * Base URI for XWiki REST service.
     */
    baseRestURI : XWiki.contextPath + "/rest/",

    /**
     * REST children relationship.
     */
    restChildrenRel: "http://www.xwiki.org/rel/children",

    /**
     * REST children relationship.
     */
    restParentRel: "http://www.xwiki.org/rel/parent",

    /**
     * REST attachments relationship.
     */
    restAttachmentsRel: "http://www.xwiki.org/rel/attachments",

    /**
     * REST home relationship.
     */
    restHomeRel: "http://www.xwiki.org/rel/home"
}

/**
 * Class extending isc.ResultTree to allow:
 */
isc.ClassFactory.defineClass("XWEResultTree", isc.ResultTree);

isc.XWEResultTree.addClassProperties({
    constants : {
        addNodeSuffix : "..new",
        pageHint : "$msg.get('xwikiexplorer.page.hint')",
        attachmentsTitle : "$msg.get('xwikiexplorer.attachments.title')",
        attachmentsHint : "$msg.get('xwikiexplorer.attachments.hint')",
        attachmentHint : "$msg.get('xwikiexplorer.attachment.hint')",
        addPageTitle : "$msg.get('xwikiexplorer.addpage.title')",
        addPageHint : "$msg.get('xwikiexplorer.addpage.hint')",
        addAttachmentTitle : "$msg.get('xwikiexplorer.addattachment.title')",
        addAttachmentHint : "$msg.get('xwikiexplorer.addattachment.hint')"
    }
});

isc.XWEResultTree.addClassMethods({
    /**
     * Joins the given arguments using the path separator.
     */
    formatPath : function() {
        return Array.prototype.slice.call(arguments, 0).join(' \u00BB ');
    },

    /**
     * Generates a string that can be used as the label of a tree node.
     */
    formatTitle : function(title, hint, url) {
        title = (title || '').escapeHTML();
        // The tool tip is very useful when writing functional tests. It is difficult to locate tree nodes without the hint.
        hint = (hint || '').escapeHTML();
        if (url) {
            return '<a href="' + url + '" title="' + hint + '">' + title + '</a>';
        } else {
            return '<span title="' + hint + '">' + title + '</span>';
        }
    }
});

isc.XWEResultTree.addProperties({

    multiDSTree : true, // Use multiple datasources.

    /**
     * Callbacks container.
     */
    callbacks : {
        // XWEResultTree.dataArrived
        dataArrived : new Array()
    },

    /**
     * Local cache of the parent/child relationships between pages.
     * This cache is not used by the tree itself but by the suggest mecanism.
     */
    parentMap : {},

    /*
     * XWiki Explorer (XWE) default options, can be overriden at creation.
     */
    displayLinks: true, // Display links in nodes of the tree.
    displayAddPage: false, // Display a "Add page" node in spaces.
    displayAddPageOnTop: true, // Display the "Add page" node on top.
    displayAttachments: true, // Display attachments of pages.
    displayAttachmentsOnTop: false, // Display attachments before children pages.
    displayAttachmentsWhenEmpty: false, // Display attachments meta-node even if there's no attachments.
    displayAddAttachment: false, // Display a "Add Attachment" node in each Attachments meta-node.
    displayAddAttachmentOnTop: true, // Display the "Add Attachment" node on top.
    displayBlacklistedSpaces: false // Don't display blacklisted spaces.
});

isc.XWEResultTree.addMethods({

    /**
     * Override isc.ResultTree.getChildDataSource
     */
    getChildDataSource : function (node, nodeDS) {

        // look for explicitly specified child type
        var childDSName = node[this.childTypeProperty];
        if (childDSName != null) {
            return isc.DS.get(childDSName);
        }
        // see if there is a mapping from this parent's type to its child type
        var nodeDS = nodeDS || this.getNodeDataSource(node);
        // - if this is a single DS tree, use the one and only DataSource
        // - if we're at root (which is the only node with no DS), use the root DataSource
        if (nodeDS == null || !this.isMultiDSTree()) {
            return this.getRootDataSource();
        }
        // otherwise try to find a relation from this node's DS to some other DS
        // - see if there's an explicitly declared tree relation
        var treeRelations = this.treeRelations, childDataSources = nodeDS.getChildDataSources();
        // manage XWiki dynamic DS instantiation
        // it can't be managed with getChildDataSources since it needs the node to be created.
        var resource = XWiki.resource.get(node.id);
        node["resource"] = resource;
        if (nodeDS.Class == "XWEDataSource") {
            childDSName = isc.XWEWikiDataSource.getOrCreate(resource.wiki).getID();
        } else if (nodeDS.Class == "XWEWikiDataSource") {
            childDSName = isc.XWESpaceDataSource.getOrCreate(resource.wiki, resource.space).getID();
        } else if (nodeDS.Class == "XWESpaceDataSource") {
            if (node["isXWikiAttachment"] == null) {
                childDSName = isc.XWESpaceDataSource.getOrCreate(resource.wiki, resource.space).getID();
            } else {
                childDSName =
                    isc.XWEAttachmentsDataSource.getOrCreate(resource.wiki, resource.space, resource.name).getID();
            }
        } else if (treeRelations) {
            // Default multi DataSource behavior
            childDSName = treeRelations[nodeDS.ID];
        }
        if (childDSName != null) {
            return isc.DS.get(childDSName);
        }
        // otherwise take the first relationship to any other DataSource
        if (childDataSources != null) {
            return childDataSources[0];
        }
    },

    /**
     * Implementation of the optional isc.ResultTree.dataArrived callback.
     */
    dataArrived : function(parentNode) {

        var parentDS = this.getNodeDataSource(parentNode);
        var childrenDSClass = "";
        // getNodeDataSource returns null when parentNode is the root node.
        if (parentDS == null) {
          parentDS = this.getDataSource();
        }

        // Remove blacklisted spaces.
        if (parentDS.Class == "XWEWikiDataSource" && this.displayBlacklistedSpaces == false) {
            this.filterNodesByName(this.getChildren(), XWiki.blacklistedSpaces);
        }

        // Various transformations on children nodes.
        // Redo getChildren to avoid getting previously removed nodes (which have been nullified).
        var children = this.getChildren(parentNode);
        for (var i = 0; i < children.length; i++) {
            var currentDS = this.getNodeDataSource(children[i]);
            // Display the title for entities that have a title (e.g. wiki pages).
            var title = children[i].title || children[i].name;
            var hint = currentDS.getHint(children[i]);
            var enabled = true;

            // Transform title to a link if showLinks is activated.
            if (this.displayLinks == true && children[i].xwikiRelativeUrl != null) {
                title = isc.XWEResultTree.formatTitle(title, hint, children[i].xwikiRelativeUrl);
            } else {
                title = isc.XWEResultTree.formatTitle(title, hint);
            }

            // Overwrite node properties.
            isc.addProperties(children[i], {
                // Overwrite children icon with the one defined in the XWiki DataSource.
                icon: currentDS.icon,
                plainTitle: children[i].title,
                title: title,
                isNewPage: false,
                isNewAttachment: false
            });

            if (i == 0) {
                // Store the children DS Class once
                childrenDSClass = currentDS.Class;
            }
        }

        if (childrenDSClass == "XWESpaceDataSource" && parentDS.Class == "XWEWikiDataSource"
            && this.displayAddPage == true) {
            this.addAddPageNode(parentNode);
        }

        // Add an attachments child to the parentNode if the parentNode is a page and showAttachments is activated.
        if (parentDS.Class == "XWESpaceDataSource" && this.displayAttachments == true
               && !parentNode.isXWikiAttachment) {
            this.addAttachmentsNode(parentNode);
        }

        // Add an attachments child to the parentNode if the parentNode is a page and showAttachments is activated.
        if (parentNode.isXWikiAttachment && this.displayAddAttachment == true) {
            this.addAddAttachmentsNode(parentNode);
        }

        // XWiki dataArrived callback handler.
        if (this.callbacks.dataArrived.length > 0) {
            var daCallback = this.callbacks.dataArrived.shift();
            daCallback.callback();
        }
    },

    /**
     * Override isc.Tree.isFolder to determine if a node has children from XWiki specific information.
     */
    isFolder : function (node) {
        var nodeDS = this.getNodeDataSource(node);
        if (nodeDS != null) {
            if (nodeDS.Class == "XWEDataSource") {
                // If the node is a wiki it necessarily has children.
                return true;
            } else if (nodeDS.Class == "XWEWikiDataSource") {
                // If the node is a space it necessarily has children (a space exist only if some page declares
                // to be located within).
                return true;
            } else if (nodeDS.Class == "XWESpaceDataSource") {
                // If the node is an attachment container it necessarily has children.
                if (node.isXWikiAttachment == true) {
                    return true;
                }
                // If the node is page, see if it points to children or attachments. If so it's a folder.
                var links = (node.link != null) ? node.link : new Array();
                for (var i = 0; i < links.length; i++) {
                    if (links[i].rel == XWiki.constants.rest.restChildrenRel
                            || links[i].rel == XWiki.constants.rest.restAttachmentsRel
                            || this.displayAttachmentsWhenEmpty
                            || this.displayAddAttachment) {
                        return true;
                    }
                }
                return false;
            } else if (nodeDS.Class == "XWEAttachmentsDataSource") {
                // If the node is an attachment it can't have children.
                return false;
            }
        }
        return true;
    },

    /**
     * Get children node matching the given name.
     */
    getChildNodeByName : function(parent, name) {
        var children = this.getChildren(parent);
        if (children != null) {
            for (var i = 0; i < children.length; i++) {
                if (children[i].name == name) {
                    return children[i];
                }
            }
        }
        return null;
    },

    /**
     * This method loops over nodes and removes those which titles match an entry of the list of titles to filter.
     *
     * @param nodes Nodes to filter.
     * @param namesToFilter Array of names to filter.
     */
    filterNodesByName : function(nodes, namesToFilter) {
        for (var i = 0; i < nodes.length; i++) {
            if (XWiki.blacklistedSpaces.indexOf(nodes[i].name) != -1) {
                this.remove(nodes[i]);
            }
        }
    },

    addAddPageNode : function(node) {
        var hint = isc.XWEResultTree.constants.addPageHint + ' '
            + isc.XWEResultTree.formatPath(node.resource.wiki, node.resource.space);
        var newNode = {
            id: node.id + isc.XWEResultTree.constants.addNodeSuffix,
            wiki: node.wiki,
            space: node.space,
            title: isc.XWEResultTree.formatTitle(isc.XWEResultTree.constants.addPageTitle, hint),
            parentId: node.id,
            icon: "$xwiki.getSkinFile('icons/silk/bullet_add.png')",
            resource: node.resource,
            isNewPage: true,
            isNewAttachment: false,
            clickCallback: function(viewer, node, recordNum) {
                node.resource = XWiki.resource.get(node.resource.prefixedSpace);
                viewer.input.value = "";
              }
        };

        // Determine node position in the children list.
        var index;
        // Get position from DataSource options.
        if (this.displayAddPageOnTop == true) {
            index = 0;
        } else {
            index = null;
        }

        // Add the node to the parent node.
        this.add(newNode, node, index);
    },

    addAddAttachmentsNode : function(node) {
        var hint = isc.XWEResultTree.constants.addAttachmentHint + ' '
            + isc.XWEResultTree.formatPath(node.resource.wiki, node.resource.space, node.resource.name);
        var newNode = {
            id: node.id + isc.XWEResultTree.constants.addNodeSuffix,
            wiki: node.wiki,
            space: node.space,
            title: isc.XWEResultTree.formatTitle(isc.XWEResultTree.constants.addAttachmentTitle, hint),
            parentId: node.id,
            icon: "$xwiki.getSkinFile('icons/silk/bullet_add.png')",
            resource: node.resource,
            isNewPage: false,
            isNewAttachment: true,
            clickCallback: function(viewer, node, recordNum) { viewer.input.value = '' }
        };

        // Determine node position in the children list.
        var index;
        // Get position from DataSource options.
        if (this.displayAddAttachmentsOnTop == true) {
            index = 0;
        } else {
            index = null;
        }

        // Add the node to the parent node.
        this.add(newNode, node, index);
    },

    /**
     * Add an attachments node (meta-node) to a page node if :
     *  - it contains a link representing an attachments relation,
     *  - or if displayAttachmentsWhenEmpty is set to true.
     * See dataArrived() bellow.
     */
    addAttachmentsNode : function(node) {
        var hasAttachments = false;
        var nodeDS = this.getNodeDataSource(node);

        // Determine if the attachments node must be displayed.
        if (this.displayAttachmentsWhenEmpty == true || this.displayAddAttachment) {
            hasAttachments = true;
        } else {
            // Loop over <link> to find an attachments relationship.
            var links = (node.link != null) ? node.link : new Array();
            var hasAttachments = false;
            for (var i = 0; i < links.length; i++) {
                if (links[i].rel == XWiki.constants.rest.restAttachmentsRel) {
                    hasAttachments = true;
                    break;
                }
            }
        }

        // If the attachments node must be displayed.
        if (hasAttachments == true) {

            // Create attachments container node title.
            var hint = isc.XWEResultTree.constants.attachmentsHint + ' '
                + isc.XWEResultTree.formatPath(node.resource.wiki, node.resource.space, node.resource.name);
            var plainTitle = isc.XWEResultTree.constants.attachmentsTitle + ' (' + node.plainTitle + ')';
            if (this.displayLinks == true) {
                var title = isc.XWEResultTree.formatTitle(plainTitle, hint, node.xwikiRelativeUrl
                    + XWiki.constants.anchorSeparator + XWiki.constants.docextraAttachmentsAnchor);
            } else {
                var title = isc.XWEResultTree.formatTitle(plainTitle, hint);
            }

            // Create the node itself.
            var attachNode = {
                id: node.id + XWiki.constants.anchorSeparator + XWiki.constants.docextraAttachmentsAnchor,
                fullName: node.fullName + XWiki.constants.anchorSeparator + XWiki.constants.docextraAttachmentsAnchor,
                wiki: node.wiki,
                space: node.space,
                title: title,
                name: node.name,
                parentId: node.id,
                xwikiRelativeURL: node.xwikiRelativeURL + XWiki.constants.anchorSeparator +
                                  XWiki.constants.docextraAttachmentsAnchor,
                icon: "$xwiki.getSkinFile('icons/silk/page_white_zip.png')",
                resource: XWiki.resource.get(node.id + XWiki.constants.anchorSeparator +
                                            XWiki.constants.docextraAttachmentsAnchor),
                isXWikiAttachment: true,
                isNewPage: false,
                isNewAttachment: false
            };

            // Determine attachments node position in the children list.
            var index;
            // Get position from DataSource options.
            if (this.displayAttachmentsOnTop == true) {
                index = 0;
            } else {
                index = null;
            }

            // Add the attachments node to the node children list.
            this.add(attachNode, node, index);
        }
    }
});

isc.ClassFactory.defineClass("XWEDataSource", isc.DataSource);

isc.XWEDataSource.addClassProperties({
    sep : "_"
});

isc.XWEDataSource.addProperties({

    /*
     * Isomorphic DataSource generic options.
     */
    dataFormat : "xml", // Format we get from REST calls.
    xmlNamespaces : { xwiki : "http://www.xwiki.org" }, // XML namespaces of our resources.
    resultTreeClass : "XWEResultTree", // Class to use to manage results.
    // We override DataSource.transformResponse to inject XWiki.callbacks.transformResponse callback handler.
    transformResponse : function (dsResponse, dsRequest, data) {
        if (this.callbacks.transformResponse.length > 0) {
            var trCallback = this.callbacks.transformResponse.shift();
            trCallback.callback(dsResponse, dsRequest, data);
        }
        return dsResponse;
    },

    /*
     * Isomorphic DataSource per-DataSource type (will be overriden) options.
     */
    dataURL : XWiki.constants.rest.baseRestURI + "wikis/", // Default (farm) REST URL.
    recordXPath : "/xwiki:wikis/xwiki:wiki", // Default (farm) XPATH for our resources.
    fields : [ // Default fields (farm) in the resource.
        { name:"id", required: true, type: "text", primaryKey:true },
        { name:"name", type: "text" },
        { name:"title", type: "text" },
        { name:"xwikiRelativeUrl", type: "text" }
    ],

    /*
     * XWiki Explorer (XWE) per-DataSource type (will be overriden) options.
     */
    icon : "$xwiki.getSkinFile('icons/silk/database.png')",

    /**
     * Properties passed to the RPCManager when request are performed.
     */
    requestProperties : {
        promptStyle: "cursor",
        // Prevent the RPCManager from displaying a warning message every time a request fails. This is especially
        // annoying when we try to open the tree to a page that doesn't exist.
        // See http://www.smartclient.com/smartgwtee/javadoc/com/smartgwt/client/rpc/RPCRequest.html#getWillHandleError()
        willHandleError: true
    },

    /**
     * Callbacks container.
     */
    callbacks : {
        // XWEDataSource.transformResponse
        transformResponse : new Array()
    }
});

/**
 * Implement optional transformRequest callback to add a random parameter to each request to overcome IE cache.
 */
isc.XWEDataSource.addMethods({
    transformRequest : function(dsRequest) {
        // We override ISC default Accept headers because we are hitting this bug in Restlet
        // JAX-RS extension : http://restlet.tigris.org/issues/show_bug.cgi?id=730
        // Precisely, the default ISC DataSource AJAX request sends the following accept header :
        // text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8,application/rdf+xml;q=0.93,text/rdf+n3;q=0.5
        // And Restlet chokes with "No message body writer found" while it has all the info necessary to use the XML writer.
        // FIXME Remove this when the issue is fixed upstream in restlet JAX-RS or when we switch to another
        // JAX-RS implementation.
        dsRequest.httpHeaders = { "Accept" : "application/xml" };

        if (dsRequest.originalData) {
            dsRequest.originalData.r = "" + Math.floor(Math.random() * 1000000);
        }
        return dsRequest.data;
    },

    /**
     * @return the hint for the specified item obtained from this data source
     */
    getHint : function(item) {
        // No hint by default. Should be overwritten by specific data sources.
        return '';
    }
});

/*
 * Wiki DataSource
 */
isc.ClassFactory.defineClass("XWEWikiDataSource", isc.XWEDataSource);

isc.XWEWikiDataSource.addClassMethods({
    getOrCreate : function(wiki) {
        var id = "XWEWikiDataSource_" + wiki;
        var ds = this.get(id);
        if (ds == null) {
            ds = this.create({ ID : id, wiki : wiki });
        }
        return ds;
    }
});

isc.XWEWikiDataSource.addProperties({
    wiki: XWiki.currentWiki,
    recordXPath : "/xwiki:spaces/xwiki:space",
    fields : [
        { name:"id", required: true, type: "text", primaryKey:true },
        { name:"name", required: true, type: "text" },
        { name:"title", type: "text" },
        { name:"xwikiRelativeUrl", type: "text" }
    ],
    icon : "$xwiki.getSkinFile('icons/silk/folder.png')"
});

isc.XWEWikiDataSource.addMethods({
    init : function() {
        this.dataURL = XWiki.constants.rest.baseRestURI + "wikis/" + this.wiki + "/spaces";
        this.Super("init", arguments);
    },

    /**
     * @return the hint for the specified item obtained from this data source
     */
    getHint : function(item) {
        return isc.XWEResultTree.constants.pageHint + ' ' + isc.XWEResultTree.formatPath(item.resource.wiki, item.name);
    }
});

/*
 * Space DataSource
 */
isc.ClassFactory.defineClass("XWESpaceDataSource", isc.XWEDataSource);

isc.XWESpaceDataSource.addClassMethods({
    getOrCreate : function(wiki, space) {
        var id = "XWESpaceDataSource_" + wiki + isc.XWEDataSource.sep + space;
        var ds = this.get(id);
        if (ds == null) {
            ds = this.create({ ID : id, wiki : wiki, space : space });
        }
        return ds;
    }
});

isc.XWESpaceDataSource.addProperties({
    wiki : "xwiki",
    space : "Main",
    recordXPath : "/xwiki:pages/xwiki:pageSummary",
    fields : [
        { name:"id", required: true, type: "text", primaryKey:true },
        { name:"fullName", required: true, type: "text" },
        { name:"wiki", required: true, type: "text" },
        { name:"space", required: true, type: "text" },
        { name:"name", required: true, type: "text" },
        { name:"title", required: true, type: "text" },
        { name:"parentId", required: true, type: "text", foreignKey: "id" },
        { name:"parent", required: true, type: "text" },
        { name:"xwikiRelativeUrl", type: "text" },
        { name:"link", propertiesOnly: true }
    ],
    icon : "$xwiki.getSkinFile('icons/silk/page_white_text.png')"
});

isc.XWESpaceDataSource.addMethods({
    init : function() {
        this.dataURL = XWiki.constants.rest.baseRestURI + "wikis/" + this.wiki + "/spaces/"
                + this.space + "/pages";
        // Override transformRequest method to allow the insertion of a fake initial parent when
        // parent property is null. This fake initial parent is a regex that allow to retrieve only
        // pages without parent or with a parent outside of the current space.
        this.transformRequest = function (dsRequest) {
            var prefixedSpace = this.wiki + XWiki.constants.wikiSpaceSeparator + this.space;
            if (dsRequest.originalData.parentId == prefixedSpace || dsRequest.originalData.parentId == null) {
                dsRequest.originalData.parentId = "^(?!" + prefixedSpace + "\.).*$";
            }
            return this.Super("transformRequest", arguments);
        };
        this.Super("init", arguments);
    },

    /**
     * @return the hint for the specified item obtained from this data source
     */
    getHint : function(item) {
        return isc.XWEResultTree.constants.pageHint + ' ' + isc.XWEResultTree.formatPath(item.wiki, item.space, item.name);
    }
});

/*
 * Page DataSource
 */
isc.ClassFactory.defineClass("XWEPageDataSource", isc.XWEDataSource);

isc.XWEPageDataSource.addClassMethods({
    getOrCreate : function(wiki, space, page) {
        var id = "XWEPageDataSource_" + wiki + isc.XWEDataSource.sep + space + isc.XWEDataSource.sep + page;
        var ds = this.get(id);
        if (ds == null) {
            ds = this.create({ ID : id, wiki : wiki, space : space, page : page });
        }
        return ds;
    }
});

isc.XWEPageDataSource.addProperties({
    wiki: "xwiki",
    space: "Main",
    page: "WebHome",
    recordXPath : "/xwiki:page",
    fields : [
        { name:"id", required: true, type: "text", primaryKey:true },
        { name:"wiki", required: true, type: "text" },
        { name:"space", required: true, type: "text" },
        { name:"name", required: true, type: "text" },
        { name:"parentId", required: true, type: "text" },
        { name:"parent", required: true, type: "text" },
        { name:"link", propertiesOnly: true }
    ],
    icon : "$xwiki.getSkinFile('icons/silk/page_white_text.png')"
});

isc.XWEPageDataSource.addMethods({
    init : function() {
        this.dataURL = XWiki.constants.rest.baseRestURI + "wikis/" + this.wiki + "/spaces/" + this.space
                + "/pages/" + this.page;
        this.Super("init", arguments);
    }
});

/*
 * Attachments DataSource
 */
isc.ClassFactory.defineClass("XWEAttachmentsDataSource", isc.XWEDataSource);

isc.XWEAttachmentsDataSource.addClassMethods({
    getOrCreate : function(wiki, space, page) {
        var id = "XWEAttachmentsDataSource_" + wiki + isc.XWEDataSource.sep + space + isc.XWEDataSource.sep + page;
        var ds = this.get(id);
        if (ds == null) {
            ds = this.create({ ID : id, wiki : wiki, space : space, page : page });
        }
        return ds;
    }
});

isc.XWEAttachmentsDataSource.addProperties({
    wiki: "xwiki",
    space: "Main",
    page: "WebHome",
    recordXPath : "/xwiki:attachments/xwiki:attachment",
    fields : [
        { name:"id", required: true, type: "text", primaryKey:true },
        { name:"name", required: true, type: "text" },
        { name:"title", type: "text" },
        { name:"xwikiRelativeUrl", type: "text" }
    ],
    icon : "$xwiki.getSkinFile('icons/silk/attach.png')"
});

isc.XWEAttachmentsDataSource.addMethods({
    init : function() {
        this.dataURL = XWiki.constants.rest.baseRestURI + "wikis/" + this.wiki + "/spaces/"
                + this.space + "/pages/"
                + this.page + "/attachments";
        this.Super("init", arguments);
    },

    /**
     * @return the hint for the specified item obtained from this data source
     */
    getHint : function(item) {
        return isc.XWEResultTree.constants.attachmentHint + ' ' + isc.XWEResultTree.formatPath(item.resource.wiki,
            item.resource.space, item.resource.name);
    }
});

/*
 * XWikiExplorer TreeGrid Widget.
 */
isc.ClassFactory.defineClass("XWETreeGrid", isc.TreeGrid);

isc.XWETreeGrid.addProperties({

    /*
     * Override isc.TreeGrid default values.
     */
    // Disabling automatic drawing of the widget, draw() must be called after creation.
    autoDraw: false,
    // Data:
    autoFetchData : true, // Load data automatically on node opening.
    // Callbacks:
    nodeClick : function(viewer, node, recordNum) {
        this.nodeClickCallback(viewer, node, recordNum)
    },
    // Style:
    showHeader: false, // Hide the sort header.
    folderIcon : "$xwiki.getSkinFile('icons/silk/database.png')", // Icon to use, will be overriden by datasources.
    position : "relative", // CSS position.
    dropIconSuffix : "", // Keep the same icon for all states (opened, closed, etc).
    openIconSuffix : "", // Keep the same icon for all states (opened, closed, etc).
    closedIconSuffix : "", // Keep the same icon for all states (opened, closed, etc).
    animateFolders : false, // Turn off animation on node open/close.

    /*
     * XWiki variables.
     */
    wiki : XWiki.currentWiki,
    space : null,
    displaySuggest : true,
    defaultValue : "Main.WebHome",

    /**
     * This variable is used to store the value of the input when openNodesFromInput is called.
     */
    inputValueCache : ""
});

isc.XWETreeGrid.addMethods({

    /**
     * Draw the tree.
     */
    draw : function() {
        // Draw tree.
        if (this.Super("draw", arguments) != null) {
            return this;
        }

        // Create suggest input.
        if (typeof this.input == "undefined" && this.input == null) {
            this.drawInput();
        }

        // Propagate XWE ResultTree display options (displayLinks, etc) to the ResultTree.
        for (member in this) {
            if (typeof member != "function" && member.startsWith("display")) {
                this.data[member] = this[member];
            }
        }

        // We have to print some CSS (eww) to override toucan table styles.
        var css = "div.listGrid td, div.listGrid table {margin:0;padding:0;} " +
                  "div.listGrid td {border:0;color:#333;} #actionmenu {z-index: 999999;}";
        var pa= document.getElementsByTagName('head')[0];
        var el = document.createElement('style');
        el.type = 'text/css';
        el.media = 'screen';
        if (el.styleSheet) {
            // IE
            el.styleSheet.cssText = css;
        } else {
            el.appendChild(document.createTextNode(css));
        }
        pa.appendChild(el);
    },

    /**
     * Invalidate Cache needs to be overwritten to also invalidate the input cached value.
     */
    invalidateCache : function() {
        this.Super("invalidateCache", arguments);
        this.inputValueCache = "";
    },

    /**
     * Open a node in the tree.
     *
     * @param rt XWEResultTree.
     * @param nodeId id of the node to open.
     * @param fireCallback if true this method will call inputObserver back after loading completion.
     */
    openNode : function (rt, nodeId, fireCallback) {
        // Callback the calls xwikiExplorerObserver again when a node has been opened.
        var daCallback = {
            treeId : this.getID(),
            callback : function() {
                window[this.treeId].openNodesFromInput();
            }
        };

        var node = rt.findById(nodeId);
        if (node != null) {

            // Open node if it is a folder and it is not opened.
            // If fireCallback is true, register a callback to continue node opening.
            if (rt.isFolder(node) && !rt.isOpen(node)) {
                if (fireCallback == true) {
                    this.getData().callbacks.dataArrived.push(daCallback);
                }
                rt.openFolder(node);
                return null;
            }

            // Select node
            this.selectNodeAndScroll(node);

            // Return the node object if it is already opened
            return node;
        }

        return null;
    },


    selectNodeAndScroll : function(node) {
        this.deselectAllRecords();
        this.selectRecord(node);
        // Scroll to the newly selected node
        nodeYPos = this.getFocusRow() * this.getRowHeight();
        this.body.scrollTo(this.body.getScrollLeft(), nodeYPos);
    },


    /**
     * Open the parent of the given resource.
     * This method is recalled until it founds a parent that exist in the tree.
     *
     * @param rt XWEResultTree.
     * @param resource Object representing a XWiki resource (see XWiki.resource.get()).
     */
    openParent : function(rt, resource) {
        if (resource.name != "") {
            // Look for the parent/child relationship in the cache.
            if (rt.parentMap[resource.prefixedFullName] == null) {
                // Usage of a DataSource to retrieve page REST resource.
                // This is the only place where this kind of DataSource is used.
                var pageDS = isc.XWEPageDataSource.getOrCreate(resource.wiki, resource.space, resource.name);
                // Prepare fetchData callback. Once the page REST resource has been loaded it retrieves
                // page parent from the xmlDoc response. If the parent has already been loaded, it opens it, if not
                // it calls the openParent method again, this time with the parent of the resource.
                var fetchCallback = function(xmlDoc, xmlText, rpcResponse, rpcRequest) {
                    if (xmlDoc.httpResponseCode == 200) {
                        // XWiki.resource.get(reference) resolves the passed reference relative to the current document reference.
                        var parentRes = xmlDoc.data[0].parent ? XWiki.resource.get(xmlDoc.data[0].parent) : { prefixedFullName: '', name: '' };
                        // Store the parent / child relationship in the cache to avoid the need of another request if this
                        // relationship is searched again.
                        rt.parentMap[resource.prefixedFullName] = parentRes;
                        if (rt.findById(parentRes.prefixedFullName) != null) {
                            // The node exists, open it.
                            this.openNode(rt, parentRes.prefixedFullName, true);
                        } else {
                            // The node does not exist, call the openParent method again with the parent we've found.
                            this.openParent(rt, parentRes);
                        }
                    } else {
                        if (this.displayAddPage == true) {
                            var addPageNode = this.getData().findById(resource.prefixedSpace
                                    + isc.XWEResultTree.constants.addNodeSuffix);
                            addPageNode.resource = resource;
                            this.selectNodeAndScroll(addPageNode);
                        }
                    }
                }.bind(this);
                // FetchData call, this method will load the REST resource we've defined above. Note the fetchCallback.
                pageDS.transformRequest = function(dsRequest) {
                  // Work around bug in Restlet JAX-RS extension. See XWEDataSource#transformRequest for more details.
                  dsRequest.httpHeaders = { 'Accept' : 'application/xml' };
                };
                pageDS.fetchData(null, fetchCallback, null);
            } else {
                // Get the parent/child relationship from the cache and open the parent.
                var parentRes = rt.parentMap[resource.prefixedFullName];
                if (rt.findById(parentRes.prefixedFullName) != null) {
                    // The node exists, open it.
                    this.openNode(rt, parentRes.prefixedFullName, true);
                } else {
                    // The node does not exist, call the openParent method again with the parent we've found.
                    this.openParent(rt, parentRes);
                }
            }
        }
    },

    /**
     * Method called when "keyup" event is fired by the input (text input).
     * This method also calls itself back with a callback passed to smartClient (kind of recursively).
     * It uses XWiki.resource.get to parse the fullName within the input,
     * then it checks if the nodes corresponding to resource parts (wiki, space, etc) are opened, one after another.
     * The first node not opened is opened and a callback is registered to call the method again if the resource part
     * as at least one child.
     *
     * With the value "xwiki:Main.RecentChanges@lquo.gif", nodes "xwiki", "Main", "WebHome" and "lquo.gif" will be opened.
     */
    openNodesFromInput : function() {
        // Build resource, selectedResource and get XWEResultTree.
        var currentDSClass = this.getDataSource().Class;
        var resource = XWiki.resource.get(this.input.value);
        var selectedRes = XWiki.resource.get("");
        var rt = this.getData();

        // Get selectedResource from the selected node if any.
        if (this.getSelectedRecord() != null) {
            selectedRes = this.getSelectedRecord().resource;
        }

        // Open wiki node if the tree is displaying multiple wikis.
        if (currentDSClass == "XWEDataSource") {
            var wikiNode = this.openNode(rt, resource.wiki, true);
            if (wikiNode == null) {
                return;
            }
        }

        if (currentDSClass == "XWEDataSource" || currentDSClass == "XWEWikiDataSource") {
            // Unselect previously selected node if space differs.
            if (resource.space != selectedRes.space) {
                this.deselectRecord(this.getSelectedRecord());
            }

            // Open space node.
            var spaceNode = this.openNode(rt, resource.prefixedSpace, true);
            if (spaceNode == null) {
               return;
            }
        }

        // Open page node.
        var pageNode = this.openNode(rt, resource.prefixedFullName, true);
        if (pageNode == null) {
            // Pages are a bit complex since their representation is flat within a space but they are
            // organized in a tree here (parent/child relationships). We must manualy try to get the page
            // REST representation and if that page exist we must climb the tree until we find an already
            // loaded node. When this node is found we must go down the tree and open all the node we've
            // found in between.
            this.openParent(rt, resource);
        }

        // Open attachment node.
        if (selectedRes.attachment != resource.attachment ||
                (selectedRes.anchor != resource.anchor && resource.anchor == XWiki.constants.docextraAttachmentsAnchor)) {
            var attachmentsNodeName = resource.prefixedFullName + XWiki.constants.anchorSeparator +
                                      XWiki.constants.docextraAttachmentsAnchor;
            var attachmentsNode = this.openNode(rt, attachmentsNodeName, true);
            if (attachmentsNode == null) {
                return;
            }

            // Open attachment node, attachments are leafs of our tree so this opening doesn't have to trigger a callback.
            var attachmentNodeName = resource.prefixedFullName + XWiki.constants.pageAttachmentSeparator + resource.attachment;
            var attachmentNode = this.openNode(rt, attachmentNodeName, false);
        }

        return;
    },

    /**
     * This method is called every 2s to check if the user has modified the input.
     * If so we try to open the corresponding nodes with openNodesFromInput().
     */
    inputObserver : function() {
        var inputValue = this.input.value;
        // If the value of the input has changed during the last 2s.
        if (inputValue != "" && inputValue != this.inputValueCache) {
            // Open nodes.
            this.openNodesFromInput();
            // Set the cache to the new value.
            this.inputValueCache = inputValue;
        }
        // Indefinitely recall this method every 2s.
        setTimeout(this.inputObserver.bind(this), 2000);
    },

    /**
     * Create a text input that will be used as a suggest for the tree.
     * The modifications on this input will be observed and the resource entered in the input,
     * like Main.Dashboard for example, will be opened in the tree.
     */
    drawInput : function() {

        // Add input to the document
        var type = (this.displaySuggest == false) ? "hidden" : "text";

        var widthWithoutBorders = this.width - 6;
        var input = document.createElement("input");
        input.setAttribute("id", this.getID() + "_Input");
        input.setAttribute("name", this.getID() + "_Input");
        input.setAttribute("style", "width:" + widthWithoutBorders + "px;clear:both");
        input.setAttribute("type", type);
        if (this.defaultValue) {
            input.setAttribute("value", this.defaultValue);
        }
        this.htmlElement.appendChild(input);
        this.input = input;


        // Prepare suggest feature.
        if (this.displaySuggest) {
            var inputFocus = function() {
                var suggest = new XWiki.widgets.Suggest(this, {
                    script: '/xwiki/rest/wikis/' + XWiki.currentWiki + '/search?scope=name&',
                    varname:'q'
                });
                // We override XWiki's ajax suggest setSuggestions method to adapt it to XWiki REST search results.
                suggest.setSuggestions = function (req) {
                    this.aSuggestions = [];
                    var xml = req.responseXML;
                    var results = xml.getElementsByTagName('searchResult');
                    for (var i = 0; i < results.length; i++) {
                        var id = results[i].getElementsByTagName('id')[0].firstChild.nodeValue;
                        var space = results[i].getElementsByTagName('space')[0].firstChild.nodeValue;
                        var pageName = results[i].getElementsByTagName('pageName')[0].firstChild.nodeValue;
                        if (results[i].hasChildNodes()) {
                            this.aSuggestions.push({
                                "id": id,
                                "value": space + XWiki.constants.spacePageSeparator + pageName,
                                "info": ""
                            });
                        }
                    }
                    this.idAs = "as_" + this.fld.id;
                    this.createList(this.aSuggestions);
                };
            };
            Event.observe(input, "focus", inputFocus);

            // Call inputObserver for the first time when the first set of data arrives.
            // The method will be called every 2s after that.
            var daCallback = {
                treeId : this.getID(),
                callback : function() {
                    window[this.treeId].inputObserver();
                }
            };

            this.getData().callbacks.dataArrived.push(daCallback);
        } else {
            // If the suggest is not displayed, call openNodesFromInput once to take defaultValue into account
            // by scrolling to the correct node in the tree.
            var daCallback = {
                treeId : this.getID(),
                callback : function() {
                    window[this.treeId].openNodesFromInput();
                }
            };

            this.getData().callbacks.dataArrived.push(daCallback);
        }
    },

    /**
     * Callback modifying the input value with the id (prefixedFullName) of the clicked node.
     */
    nodeClickCallback : function(viewer, node, recordNum) {
        if (node.clickCallback == null) {
            var resId = node.id;
            // If the resource is a wiki, add :Main.WebHome to the resource id.
            if (!resId.include(XWiki.constants.wikiSpaceSeparator)
                    && this.getData().getNodeDataSource(node).Class == "XWEDataSource") {
                resId = resId + XWiki.constants.wikiSpaceSeparator + "Main"
                          + XWiki.constants.spacePageSeparator + "WebHome";
            }
            // If the resource is a space, add .WebHome to the resource id.
            if (!resId.include(XWiki.constants.spacePageSeparator)
                    && this.getData().getNodeDataSource(node).Class == "XWEWikiDataSource") {
                resId = resId + XWiki.constants.spacePageSeparator + "WebHome";
            }
            // If there's only the current wiki in the tree, remove the wiki prefix (ex: "xwiki:").
            if (this.getDataSource().Class != "XWEDataSource") {
                resId = resId.substring(resId.indexOf(XWiki.constants.wikiSpaceSeparator) + 1, resId.length);
            }
            // If the resource is located in the current space, remove the space prefix (ex: "Main.")
            if (node.resource["space"] == XWiki.currentSpace) {
                resId = resId.substring(resId.indexOf(XWiki.constants.spacePageSeparator) + 1, resId.length);
            }
            // Set the input value to the clean ID of the clicked node.
            this.input.value = resId;
            // Set the cache to the value we've set to prevent the tree from jumping to the WebHome node afterwards.
            this.inputValueCache = resId;
        } else {
            node.clickCallback(viewer, node, recordNum);
        }
    },

    /**
     * Set the value of the explorer input.
     */
    setValue : function(value) {
        this.input.value = value;
    },

    /**
     * Get the fullName of the resource selected in the tree.
     */
    getValue : function() {
        return this.input.value;
    },

    /**
     * Selects the tree node corresponding to the given resource.
     *
     * @param resource the resource to be selected
     */
    selectResource : function(resource) {
        this.setValue(XWiki.resource.serialize(resource));
    },

    /**
     * Get a property from the selected resource (ex: wiki, space, page, etc).
     */
    getSelectedResourceProperty : function(propertyName) {
        var value = this.getValue();
        var selectedRecord = this.getSelectedRecord();
        if (value.length > 0) {
            return XWiki.resource.get(value)[propertyName];
        } else if (selectedRecord != null) {
            return selectedRecord.isNewPage && propertyName == 'name' ? null : selectedRecord.resource[propertyName];
        }
        return null;
    },

    /**
     * Is the selected resource a new page.
     */
    isNewPage : function() {
        // There's no record selected when we target a new page in a new space since we can't select a space that does
        // not exist in the tree, but the input value must not be empty in this case.
        var selectedRecord = this.getSelectedRecord();
        return selectedRecord == null ? this.getValue().length > 0 : selectedRecord.isNewPage;
    },

    /**
     * Is the selected resource a new attachment.
     */
    isNewAttachment : function() {
        return this.getSelectedRecord().isNewAttachment;
    }
});
