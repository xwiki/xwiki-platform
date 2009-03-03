/**
 * XWiki Explorer widget for smartClient.
 */

if (typeof XWiki == "undefined") {
  alert("ERROR: xwikiexplorer.js depends on xwiki.js");
}
 
XWiki.xwikiExplorer = {
  	  	
  /**
   * Base URI for XWiki REST service.
   */
  baseRestURI : XWiki.constants.contextPath + "/rest/",
    
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
  restHomeRel: "http://www.xwiki.org/rel/home",
  
  /**
   * A set of callbacks fired by several smartClient classes (callback handlers are injected by XWiki.smartClient).
   */  
  callbacks : {
    // XWikiExplorerResultTree.dataArrived
  	dataArrived : new Array(),
  	// DataSource.transformResponse
  	transformResponse : new Array()
  },

  /** 
   * Create a SmartClient class extending isc.ResultTree to allow:
   * Dynamic DS instantiation.
   * Custom icon per entry type without having any icon field in the REST response.
   */
  createXWikiExplorerResultTreeClass : function() {
  	
    /*
     * XWikiExplorerResultTree class overriding default ResultTree class.
     * Used by xwikiExplorer DataSources. 
     */
    isc.ClassFactory.defineClass("XWikiExplorerResultTree", isc.ResultTree);
    isc.XWikiExplorerResultTree.addMethods({
 
      /** 
       * Override isc.ResultTree.getChildDataSource     
       */ 
      getChildDataSource : function (node, nodeDS) {              
        // look for explicitly specified child type 
        var childDSName = node[this.childTypeProperty];
        if (childDSName != null) 
          return isc.DS.get(childDSName);
        // see if there is a mapping from this parent's type to its child type
        var nodeDS = nodeDS || this.getNodeDataSource(node);
        // - if this is a single DS tree, use the one and only DataSource
        // - if we're at root (which is the only node with no DS), use the root DataSource
        if (nodeDS == null || !this.isMultiDSTree()) 
          return this.getRootDataSource();      
        // otherwise try to find a relation from this node's DS to some other DS
        // - see if there's an explicitly declared tree relation
        var treeRelations = this.treeRelations,
          childDataSources = nodeDS.getChildDataSources(); 
        // manage XWiki dynamic DS instantiation
        // it can't be managed with getChildDataSources since it needs the node to be created.
        var xwikiRecordsType = nodeDS.xwikiRecordsType;
        if (xwikiRecordsType != null && xwikiRecordsType != "") {           
          if (xwikiRecordsType == "wiki") {
            childDSName = XWiki.xwikiExplorer.getDS(node["name"]);
          } else if (xwikiRecordsType == "space") {
            childDSName = XWiki.xwikiExplorer.getDS(node["wiki"], node["name"]);
          } else if (xwikiRecordsType == "page") {
          	if (node["isXWikiAttachment"] == null) {
          	  childDSName = XWiki.xwikiExplorer.getDS(node["wiki"], node["space"]);
          	} else {
              childDSName = XWiki.xwikiExplorer.getDS(node["wiki"], node["space"], node["name"],
                              XWiki.constants.docextraAttachmentsAnchor);
          	}
          }          
        } else if (treeRelations) {      
          // Default multi DataSource behavior                              
          childDSName = treeRelations[nodeDS.ID];
        }        
        if (childDSName != null) 
          return isc.DS.get(childDSName);
        // otherwise take the first relationship to any other DataSource
        if (childDataSources != null) 
          return childDataSources[0];
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
       * This method looks for space nodes in the list and remove blacklisted space from the ResultTree. 
       * See dataArrived() bellow.
       */
      removeBlacklistedSpaces : function(nodes) {
        for (var i = 0; i < nodes.length; i++) {      	      	      	  
          var currentDS = this.getNodeDataSource(nodes[i]);                    
          if (currentDS.xwikiRecordsType == "space") {          	
            if (xwikiExplorerTree.blacklistedSpaces != "" && xwikiExplorerTree.blacklistedSpaces[nodes[i].name] == true) {
              this.remove(nodes[i]);	
            }
          }  
        }                             		
      },
      
      /**
       * Add an attachments node (meta-node) to a page node if :
       *  - it contains a link representing an attachments relation,
       *  - or if xwikiExplorerTree.displayAttachmentsWhenEmpty is set to true.
       * See dataArrived() bellow.
       */
      addAttachmentsNode : function(node) {
      	var hasAttachments = false;

        // Determine if the attachments node must be displayed.
      	if (xwikiExplorerTree.displayAttachmentsWhenEmpty == true) {
      	  // xwikiExplorerTree.displayAttachmentsWhenEmpty is set to true.
      	  hasAttachments = true;
      	} else {
      	  // Loop over <link> to find an attachments relationship.
          var links = (node.link != null) ? node.link : new Array();          
          var hasAttachments = false;
          for (var i = 0; i < links.length; i++) {
            if (links[i].rel == XWiki.xwikiExplorer.restAttachmentsRel) {
              hasAttachments = true;  
              break;        	    	              
            }
          }
      	}
          
        // If the attachments node must be displayed.
        if (hasAttachments == true) {        
        	                	
          // Create attachments container node title.    	
          var title = "Attachments (" + node.name + ")";
          if (xwikiExplorerTree.displayLinks == true) {
            title = "<a href='" + node.xwikiUrl + XWiki.constants.docextraAttachmentsAnchor + "'>" 
                      + title + "</a>"
          }
          	
          // Create the node itself.
          var attachNode = {
              id: node.id + XWiki.constants.docextraAttachmentsAnchor,
              fullName: node.fullName + XWiki.constants.docextraAttachmentsAnchor,
              wiki: node.wiki,
              space: node.space,              
              title: title,	
              name: node.name,
              parentId: node.id,
              parent: node.fullName,
              xwikiURL: node.xwikiURL + XWiki.constants.docextraAttachmentsAnchor,
              icon: "$xwiki.getSkinFile('icons/silk/page_white_zip.gif')",
              resource: XWiki.getResource(node.id + XWiki.constants.docextraAttachmentsAnchor),
              isXWikiAttachment: true
            };
 
          // Determine attachments node position in the children list.
          var index;
          // Get position from xwikiExplorerTree options.
          if (xwikiExplorerTree.displayAttachmentsAtTop == true) {
          	index = 0;
          } else {
          	index = null;
          }
                        
          // Add the attachments node to the node children list.
          this.add(attachNode, node, index);
        }
      },
      
 
      /**
       * Implementation of the optional isc.ResultTree.dataArrived callback.
       */
      dataArrived : function(parentNode) {       
      	            	
        // Various transformations on children nodes.
        // Redo getChildren to avoid getting previously removed nodes (which have been nullified).
      	var children = this.getChildren(parentNode);
        for (var i = 0; i < children.length; i++) {
          var currentDS = this.getNodeDataSource(children[i]);                         
          var title = children[i].name;    
                                                  
          // Transform title to a link if showLinks is activated.
          if (xwikiExplorerTree.displayLinks == true && children[i].xwikiUrl != null) {          
            title = "<a href='" + children[i].xwikiUrl + "'>" + title + "</a>";
          }
            
          // Overwrite node properties.
          isc.addProperties(children[i], { 
              // Overwrite children icon with the one defined in the XWiki DataSource.
              icon: currentDS.xwikiIcon,
              canDrag: currentDS.xwikiCanDragNodes,
              canAcceptDrop: currentDS.xwikiCanAcceptDropNodes,
              resource: XWiki.getResource(children[i].id),
          	  title: title
            });            	  
          	
          // Open current wiki node.	
          if (currentDS.xwikiRecordsType == "wiki" && children[i].name == XWiki.constants.currentWiki) {
            this.openFolder(children[i]);	
          }          
        }                     
        
        // We don't try to make spaces/pages transformation on the root node.
      	if (!this.isRoot(parentNode)) {
          var parentDS = this.getNodeDataSource(parentNode);          
        
          // Remove blacklisted spaces.
          if (parentDS.xwikiRecordsType == "wiki") {        
            this.removeBlacklistedSpaces(children);
          }
          
          // Add an attachments child to the parentNode if the parentNode is a page and showAttachments is activated.
          if (parentDS.xwikiRecordsType == "page" && xwikiExplorerTree.displayAttachments == true
                && !parentNode.isXWikiAttachment) {          
            this.addAttachmentsNode(parentNode);
          }
      	}              
                  
        // XWiki dataArrived callback handler.          
        if (XWiki.xwikiExplorer.callbacks.dataArrived.length > 0) {
          var xwikiCallback = XWiki.xwikiExplorer.callbacks.dataArrived.shift();    		
          xwikiCallback.params.parentNode = parentNode;
          xwikiCallback.callback(xwikiCallback.params);
        }      	
      },
      
      /**
       * Override isc.Tree.isFolder to determine if a node has children from XWiki specific information.
       */
      isFolder : function (node) {
      	var nodeDS = this.getNodeDataSource(node);
        if (nodeDS != null) {
          var xwikiRecordsType = nodeDS.xwikiRecordsType;        
          if (xwikiRecordsType != null && xwikiRecordsType != "") {           
            if (xwikiRecordsType == "wiki") {
              // If the node is a wiki it necessarily has children.
              return true;
            } else if (xwikiRecordsType == "space") {
              // If the node is a space it necessarily has children (a space exist only if some page declares                           
              // to be located within). 
              return true;
            } else if (xwikiRecordsType == "page") {
              // If the node is an attachment container it necessarily has children.
              if (node.isXWikiAttachment == true) {
              	return true;
              }	
              // If the node is page, see if it points to children or attachments. If so it's a folder.
              var links = (node.link != null) ? node.link : new Array();                        
              for (var i = 0; i < links.length; i++) {              	
                if (links[i].rel == XWiki.xwikiExplorer.restChildrenRel 
                      || links[i].rel == XWiki.xwikiExplorer.restAttachmentsRel) {
                  return true;
                }               
              }
              return false;
            } else if (xwikiRecordsType == "attachment") {
              // If the node is an attachment it can't have children.
              return false;
            }
          }
        }         
      	return true;
      }     
    });
  }, 
  
  /**
   * Get an object containing XWiki DataSource default parameters.
   */
  getDSDefaultParams : function() {
  	return {
        id : "root__DS",
        dataFormat : "xml",
        dataURL : this.baseRestURI + "wikis/",
        xmlNamespaces : { xwiki : "http://www.xwiki.org" },
        recordXPath : "/xwiki:wikis/xwiki:wiki",
        resultTreeClass : "XWikiExplorerResultTree",
        fields : [ 
            { name:"id", required: true, type: "text", primaryKey:true },
            { name:"name", required: true, type: "text" },
            { name:"title", type: "text" },
            { name:"xwikiUrl", type: "text" }
          ],
        showPrompt: false,  
        
        // XWiki specific
        xwikiRecordsType :  "wiki",
        xwikiIcon : "$xwiki.getSkinFile('icons/silk/database.gif')",
        xwikiCanDragNodes : false,
        xwikiCanAcceptDropNodes : false
      };
  },
     
  /**
   * Get DataSource id for the given XWiki resource, creates the DataSource if it doesn't exist. 
   * If all the arguments are empty the root DataSource (id: rootDS) will be returned.
   *
   * @param wiki Wiki name (optional)
   * @param space Space name (optional)
   * @param page Page name (optional)
   * @return DataSouce ID (String)
   */
  getDS : function(wiki, space, page) {
    // Create XWikiExplorerResultTree on first call
    if (isc.XWikiExplorerResultTree == null) {
      this.createXWikiExplorerResultTreeClass();
    }

    // Default values (root node)
    var dsParams = XWiki.xwikiExplorer.getDSDefaultParams();
      
    // Override DataSource.transformResponse to inject XWiki.callbacks.transformResponse callback handler.
    if (dsParams.transformResponse == null) {
      dsParams.transformResponse = function (dsResponse, dsRequest, data) {
          if (XWiki.xwikiExplorer.callbacks.transformResponse.length > 0) {
            var xwikiCallback = XWiki.xwikiExplorer.callbacks.transformResponse.shift();    		
         	xwikiCallback.callback(xwikiCallback.params, dsResponse, dsRequest, data);
          }
          return dsResponse;
        };
    }  
      
    // Switch on resource type (space, page, etc) and overwrite DataSource creation parameters.
    // TODO: refactor this part.   
    switch(arguments.length) {
      case 0:    
        break;      
      case 1:
        // List of space within the given wiki
        dsParams.id = wiki + "__DS";
        dsParams.dataURL = this.baseRestURI + "wikis/" + wiki + "/spaces";
        dsParams.recordXPath = "/xwiki:spaces/xwiki:space";
        dsParams.fields = [   	 
            { name:"id", required: true, type: "text", primaryKey:true },
            { name:"name", required: true, type: "text" },
            { name:"title", type: "text" },
            { name:"xwikiUrl", type: "text" }
          ];        
        dsParams.xwikiRecordsType = "space";
        dsParams.xwikiIcon = "$xwiki.getSkinFile('icons/silk/folder.gif')";
        break;      
      case 2:
        // List of pages within the given space
        dsParams.id = wiki + "__" + space + "__DS";
        dsParams.dataURL = this.baseRestURI + "wikis/"  + wiki + "/spaces/" + space + "/pages";
        dsParams.recordXPath = "/xwiki:pages/xwiki:pageSummary";
        dsParams.fields = [   	 
            { name:"id", required: true, type: "text", primaryKey:true },
            { name:"fullName", required: true, type: "text" },
            { name:"wiki", required: true, type: "text" },
            { name:"space", required: true, type: "text" },
            { name:"name", required: true, type: "text" },
            { name:"title", required: true, type: "text" },
            { name:"xwikiUrl", type: "text" },
            { name:"link", propertiesOnly: true },
            { name:"parent", required: true, type: "text", foreignKey: this.id + ".fullName" }            
          ];
        dsParams.xwikiRecordsType = "page";
        dsParams.xwikiIcon = "$xwiki.getSkinFile('icons/silk/page_white_text.gif')";
        dsParams.xwikiCanDragNodes = true;
        dsParams.xwikiCanAcceptDropNodes = true;
        // Override transformRequest method to allow the insertion of a fake initial parent when
        // parent property is null. This fake initial parent is a regex that allow to retrieve only
        // pages without parent or with a parent outside of the current space.
        dsParams.transformRequest = function (dsRequest) {
            if (dsRequest.originalData.parent == null) {
              dsRequest.originalData.parent = "^(?!" + space + "\.).*$";
            }
            return dsRequest.data;
          };        
        break;      
      case 3:
        // Resource for the given page
        dsParams.id = wiki + "__" + space + "__" + page + "__DS";
        dsParams.dataURL = this.baseRestURI + "wikis/"  + wiki + "/spaces/" + space + "/pages/" + page;
        dsParams.recordXPath = "/xwiki:page";
        dsParams.fields = [   	 
            { name:"id", required: true, type: "text", primaryKey:true },
            { name:"wiki", required: true, type: "text" },
            { name:"space", required: true, type: "text" },
            { name:"parent", required: true, type: "text" },
            { name:"link", propertiesOnly: true },
          ];         
        dsParams.xwikiRecordsType = "page";  
        dsParams.xwikiIcon = "$xwiki.getSkinFile('icons/silk/page_white_text.gif')"
        break;
      case 4:
        // Attachments for the given page
        dsParams.id = wiki + "__" + space + "__" + page + "__Attachments__DS";
        dsParams.dataURL = this.baseRestURI + "wikis/"  + wiki + "/spaces/" + space + "/pages/" + page + "/attachments";
        dsParams.recordXPath = "/xwiki:attachments/xwiki:attachment";
        dsParams.fields = [   	 
            { name:"id", required: true, type: "text", primaryKey:true },
            { name:"name", required: true, type: "text" },
            { name:"title", type: "text" },
            { name:"xwikiUrl", type: "text" }
          ];         
        dsParams.xwikiRecordsType = "attachment";  
        dsParams.xwikiIcon = "$xwiki.getSkinFile('icons/silk/attach.gif')"
        break; 
    }           
    
    // Create DataSource
    isc.DataSource.create(dsParams); 

    return dsParams.id;    
  },
  
  /**
   * Open a node in the tree.
   *
   * @param rt XWikiExplorerResultTree.
   * @param nodeId id of the node to open.
   * @param fireCallback if true this method will call inputObserver back after loading completion.
   */
  openNode : function (rt, nodeId, fireCallback) {
    // Callback the calls xwikiExplorerObserver again when a node has been opened.
    var callback = { 
  	    callback : function(params) {
            XWiki.xwikiExplorer.openNodesFromInput();
          },
        params : { }
      };
                    
    var node = rt.findById(nodeId);
    if (node != null) {    	      
            	
      // Open node if it is a folder and it is not opened.
      // If fireCallback is true, register a callback to continue node opening.
      if (rt.isFolder(node) && !rt.isOpen(node)) {
        if (fireCallback == true) {
          XWiki.xwikiExplorer.callbacks.dataArrived.push(callback);	 
        }
        rt.openFolder(node);
        return null;
      }

      // Select node
      xwikiExplorerTree.deselectAllRecords();
      xwikiExplorerTree.selectRecord(node);
      // Scroll to the newly selected node
      nodeYPos = xwikiExplorerTree.getFocusRow() * xwikiExplorerTree.getRowHeight();
      xwikiExplorerTree.body.scrollTo(xwikiExplorerTree.body.getScrollLeft(), nodeYPos);
      
      // Return the node object if it is already opened
      return node;
    }
    
    return null;    
  },
    
  /**
   * Local cache of the parent/child relationships between pages.
   * This cache is not used by the tree itself but by the suggest mecanism.
   */
  parentMap : {},  
    
  /**
   * Open the parent of the given resource.
   * This method is recalled until it founds a parent that exist in the tree.
   *
   * @param rt XWikiExplorerResultTree.
   * @param resource Object representing a XWiki resource (see XWiki.getResource()).
   */
  openParent : function(rt, resource) {
    if (resource.name != "") {
      // Look for the parent/child relationship in the cache.
      if (XWiki.xwikiExplorer.parentMap[resource.prefixedFullName] == null) {
        // Usage of a DataSource to retrieve page REST resource.
        // This is the only place where this kind of DataSource is used.      
        var pageDSName = XWiki.xwikiExplorer.getDS(resource.wiki, resource.space, resource.name);
        var pageDS = isc.DataSource.get(pageDSName);
        // Prepare fetchData callback. Once the page REST resource has been loaded it retrieves
        // page parent from the xmlDoc response. If the parent has already been loaded, it opens it, if not
        // it calls the openParent method again, this time with the parent of the resource.
        var fetchCallback = function(xmlDoc, xmlText, rpcResponse, rpcRequest) {
          if (xmlDoc.httpResponseCode == 200) {    
            var parentRes = XWiki.getResource(xmlDoc.data[0].parent);      	  
        	var parentNode = xwikiExplorerTree.data.findById(parentRes.fullName);
      	    // Store the parent / child relationship in the cache to avoid the need of another request if this
      	    // relationship is searched again.
      	    XWiki.xwikiExplorer.parentMap[resource.prefixedFullName] = parentRes;
      	    if (rt.findById(parentRes.prefixedFullName) != null) {
      	  	  // The node exists, open it.
      	  	  XWiki.xwikiExplorer.openNode(rt, parentRes.prefixedFullName, true);
      	    } else {
      	      // The node does not exist, call the openParent method again with the parent we've found.
      	      XWiki.xwikiExplorer.openParent(rt, parentRes);
      	    }
      	  }
        }
        // FetchData call, this method will load the REST resource we've defined above. Note the fetchCallback.      	
        pageDS.fetchData(null, fetchCallback, null);      
      } else {
      	// Get the parent/child relationship from the cache and open the parent.
      	var parentRes = XWiki.xwikiExplorer.parentMap[resource.prefixedFullName];
      	if (rt.findById(parentRes.prefixedFullName) != null) {
      	  // The node exists, open it.
      	  XWiki.xwikiExplorer.openNode(rt, parentRes.prefixedFullName, true);
      	} else {
      	  // The node does not exist, call the openParent method again with the parent we've found.
      	  XWiki.xwikiExplorer.openParent(rt, parentRes);
      	}
      }
    }	
  },
      
  /**
   * Method called when "keyup" event is fired by xwikiExplorerInput (text input).
   * This method also calls itself back with a callback passed to smartClient (kind of recursively).
   * It uses XWiki.getResource to parse the fullName within xwikiExplorerInput,
   * then it checks if the nodes corresponding to resource parts (wiki, space, etc) are opened, one after another.
   * The first node not opened is opened and a callback is registered to call the method again if the resource part
   * as at least one child.
   * 
   * With the value "xwiki:Main.RecentChanges@lquo.gif", nodes "xwiki", "Main", "WebHome" and "lquo.gif" will be opened.
   */
  openNodesFromInput : function() {
    
    // Build resource, selectedResource and get XWikiExplorerResultTree.
  	var resource = XWiki.getResource($("xwikiExplorerInput").value);
    var selectedRes = XWiki.getResource("");
    var rt = xwikiExplorerTree.getData();       
        
    // Get selectedResource from the selected node if any.    
    if (xwikiExplorerTree.getSelectedRecord() != null) {
      selectedRes = xwikiExplorerTree.getSelectedRecord().resource; 
    }
        
    // Open wiki node.
    //if (resource.wiki == "") { return; }
    if (selectedRes.wiki != resource.wiki) {
      var wikiNode = XWiki.xwikiExplorer.openNode(rt, resource.wiki, true);
      if (wikiNode == null) { return; } 
    }

    // Open space node.
    //if (resource.space == "") { return; }
    if (selectedRes.prefixedSpace != resource.prefixedSpace) {
      var spaceNode = XWiki.xwikiExplorer.openNode(rt, resource.prefixedSpace, true);
      if (spaceNode == null) { return; }
    }
    
    // Open page node.
    //if (resource.name == "") { return; }
    if (selectedRes.prefixedFullName != resource.prefixedFullName) {
      var pageNode = XWiki.xwikiExplorer.openNode(rt, resource.prefixedFullName, true);
      if (pageNode == null) {  
        // Pages are a bit complex since their representation is flat within a space but they are
        // organized in a tree here (parent/child relationships). We must manualy try to get the page
        // REST representation and if that page exist we must climb the tree until we find an already
        // loaded node. When this node is found we must go down the tree and open all the node we've
        // found in between.
        XWiki.xwikiExplorer.openParent(rt, resource);
      }
    }
        
    // Open attachment node.
    if (selectedRes.attachment != resource.attachment) {
      var attachmentsNodeName = resource.prefixedFullName + XWiki.constants.docextraAttachmentsAnchor;
      var attachmentsNode = XWiki.xwikiExplorer.openNode(rt, attachmentsNodeName, true);     
      if (attachmentsNode == null) { return; }
      
      // Open attachment node, attachments are leafs of our tree so this opening doesn't have to trigger a callback.    
      var attachmentNodeName = resource.prefixedFullName + XWiki.constants.pageAttachmentSeparator + resource.attachment;
      var attachmentNode = XWiki.xwikiExplorer.openNode(rt, attachmentNodeName, false);    
    }
                                   
    return;
  },    
  
  /**
   * This variable is used to store the value of xwikiExplorerInput when openNodesFromInput is called.
   */
  inputValueCache : "",
  
  /**
   * This method is called every 2s to check if the user has modified xwikiExplorerInput.
   * If so we try to open the corresponding nodes with openNodesFromInput().
   */   
  inputObserver : function() {
  	var inputValue = $("xwikiExplorerInput").value;
  	// If the value of xwikiExplorerInput has changed during the last 2s.
  	if (inputValue != "" && inputValue != XWiki.xwikiExplorer.inputValueCache) {
  	  // Open nodes.
  	  XWiki.xwikiExplorer.openNodesFromInput();
  	  // Set the cache to the new value.
  	  XWiki.xwikiExplorer.inputValueCache = inputValue;  	  
  	}
  	// Indefinitely recall this method every 2s.
  	setTimeout(XWiki.xwikiExplorer.inputObserver, 2000);
  },
  
  /**
   * Callback modifying xwikiExplorerInput value with the id (prefixedFullName) of the clicked node.
   */
  nodeClickCallback : function(viewer,node,recordNum) { 
    $("xwikiExplorerInput").value = node.id; 
  },
  
  /**
   * Callback changing page parent on drag and drop.
   */
  folderDropCallback : function(moveList, newParent, index, dragTarget) {
  	// Loop over dragged nodes.
  	for (var i = 0; i < moveList.length; i++) {
      var currentNode = moveList[i];
      var url = XWiki.xwikiExplorer.baseRestURI + "wikis/"  + currentNode.resource.wiki + "/spaces/" 
                  + currentNode.resource.space + "/pages/" + currentNode.resource.name
                  + "?method=PUT";
      // Modify nodes parent.            
      // Note that the use of xml will be replaced by post paramaters when their use will 
      // be available in xwiki-rest.
      //var xmlStart = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>';
      var xmlStart = "";
      new Ajax.Request(url, {
          method: "put",
          contentType: "application/xml",
          postBody: xmlStart + '<page xmlns="http://www.xwiki.org"><parent>' 
                      + newParent.resource.fullName + "</parent></page>"
        });
  	}
  	// console.log(dragTarget); invalidate
    this.Super("folderDrop", arguments);
  },
  
  /**
   * Create a text input that will be used as a suggest for the tree.
   * The modifications on this input will be observed and the resource entered in the input,
   * like Main.Dashboard for example, will be opened in the tree.
   */
  createSuggest : function(displaySuggest, width) {  		
    // Add xwikiExplorer input to the document if not present.
    if ($("xwikiExplorerInput") == null) {
      document.write("<input id='xwikiExplorerInput' name='xwikiExplorerInput' type='hidden' style='width:"
        + (width - 6) + "px;border-size:3px;'/>");
    }
    // Prepare suggest feature.
    if(displaySuggest) {      
      $("xwikiExplorerInput").type = "text";
      $("xwikiExplorerInput").observe("focus", function() { new ajaxSuggest(this, { script: '/xwiki/bin/view/Main/Tree?xpage=plain&', varname:'input' }); });  
      // Call inputObserver for the first time, it will be called every 2s after that.
      XWiki.xwikiExplorer.inputObserver();
    }
  },
 
  /**
   * Create XWikiExplorer widget.
   *
   * @param id The id of the widget
   * @param width Width in pixels
   * @param height Height in pixels
   * @param displaySuggest true to display an input allowing to navigate through the tree
   */
  create : function(params) {  
   
    // Options initialization.
  	var width = params.width == null ? 500 : params.width;
   	var height = params.height == null ? 500 : params.height;
  	var displaySuggest = (params.displaySuggest == null) ? true : params.displaySuggest;
  	var displayAttachments = (params.displayAttachments == null) ? false : params.displayAttachments;
  	var displayAttachmentsAtTop = (params.displayAttachmentsAtTop == null) ? true : params.displayAttachmentsAtTop;
  	var displayAttachmentsWhenEmpty = 
  	      (params.displayAttachmentsWhenEmpty == null) ? false : params.displayAttachmentsWhenEmpty;
  	var displayLinks = (params.displayLinks == null) ? false : params.displayLinks;
  	var blacklistedSpaces = (params.blacklistedSpaces == null) ? "" : params.blacklistedSpaces;  
                
    // Create xwikiExplorer widget.
    var tree = isc.TreeGrid.create({
    	ID : "xwikiExplorerTree",        
    	
    	// Data management.
        dataSource : XWiki.xwikiExplorer.getDS(),
        autoFetchData : true,
        multiDSTree : true,
        
        // Callbacks.
        nodeClick : XWiki.xwikiExplorer.nodeClickCallback,        
        folderDrop : XWiki.xwikiExplorer.folderDropCallback,
        
        // Look and feel.
        width : width,
        height : height,
        position : "relative",
        dropIconSuffix : "",
        openIconSuffix : "",
        closedIconSuffix : "",
        folderIcon : "$xwiki.getSkinFile('icons/silk/database.gif')",
        animateFolders: false,        
        
        // Drag and drop.
        // Those options are effective on Page nodes since they are the only ones to be draggable.
        // They will be uncommented when the feature will be ready.
        // canAcceptDroppedRecords: true,
        // canDropOnLeaves: true,
        // canReparentNodes: true,
        
        // XWiki options.
        displayAttachments: displayAttachments,
        displayLinks: displayLinks,
        // TODO: build the list of blacklisted spaces in this method and provide a 
        // displayBlacklistedSpaces option.
        blacklistedSpaces: blacklistedSpaces
      });           
      
    // Create suggest input. 
    XWiki.xwikiExplorer.createSuggest(displaySuggest, width);                   
         
    return tree;
  }  
};
