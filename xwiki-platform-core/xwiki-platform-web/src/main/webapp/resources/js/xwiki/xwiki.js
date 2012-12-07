var XWiki = (function(XWiki) {
XWiki.widgets = XWiki.widgets || {};
/**
 * XWiki namespace.
 * TODO: move everything in it.
 *
 * @type object
 */

Object.extend(XWiki, {

  constants: {
    /** URL Anchor separator. */
    anchorSeparator: "#",

    /** URL Anchor for page comments. */
    docextraCommentsAnchor: "Comments",

    /** URL Anchor for page attachments. */
    docextraAttachmentsAnchor: "Attachments",

    /** URL Anchor for page history. */
    docextraHistoryAnchor: "History",

    /** URL Anchor for page information. */
    docextraInformationAnchor: "Information"
  },

  resource: {
    /**
     * Build a resource object from a wiki resource name (aka fullName). Example with "Main.WebHome":
     * {
     *   wiki: "xwiki",
     *   space: "Main",
     *   prefixedSpace: "xwiki:Main",
     *   fullName: "Main.WebHome",
     *   prefixedFullName: "xwiki:Main.WebHome",
     *   name: "WebHome",
     *   attachment: ""
     *  }
     *
     * @param name name of the resource to create (examples: xwiki:Main.WebHome, xwiki:Main.WebHome@Archive.tgz).
     * @param entityType the type of entity specified by the given name; this parameter is optional but very useful when
     *                   the passed string is a relative entity reference; e.g. XWiki.EntityType.SPACE for 'xwiki:Main'
     *                   and XWiki.EntityType.ATTACHMENT for 'Page@file.txt'
     * @return the newly created resource object.
     */
    get: function(name, entityType) {
        // First, extract the anchor if it is specified, because entity references don't support anchors. We use a list
        // of known anchors to reduce the possibility of a conflict with an entity whose name contains the anchor
        // separator. Of couse this meas we don't support entity names that end with known anchors.
        var anchor = '', knownAnchors = ['Attachments'];
        // In case the entity type is not specified determine it from the known anchor.
        var entityTypeForAnchor = [XWiki.EntityType.DOCUMENT];
        for (var i = 0; i < knownAnchors.length; i++) {
            if (name.endsWith(XWiki.constants.anchorSeparator + knownAnchors[i])) {
                anchor = knownAnchors[i];
                name = name.substr(0, name.length - (anchor.length + 1));
                entityType = entityType || entityTypeForAnchor[i];
                break;
            }
        }

        var reference;
        if (entityType) {
            reference = XWiki.Model.resolve(name, entityType);
        } else {
            // NOTE: The given name can be an attachment reference, a document reference, a space reference or a wiki
            // reference. Since we don't know the entity type we try to deduce it. This can fail if the given name is a
            // relative reference.
            reference = XWiki.Model.resolve(name, XWiki.EntityType.ATTACHMENT);
            if (!reference.parent) {
                // Attachment references must be prefixed with at least the document name.
                reference = XWiki.Model.resolve(name, XWiki.EntityType.DOCUMENT);
                if (!reference.parent) {
                    // Check if it's not a space reference.
                    var spaceReference = XWiki.Model.resolve(name, XWiki.EntityType.SPACE);
                    if (spaceReference.parent) {
                        // The wiki is specified so we assume it is a space reference.
                        reference = spaceReference;
                    } else {
                        // The given name is either a wiki name, a space name or a document name. We can't really
                        // distinguish between them so we assume it's the name of a document from the current space.
                    }
                }
            }
        }

        return this.fromEntityReference(reference, anchor);
    },

    fromEntityReference : function(reference, anchor) {
        var wiki = reference.extractReference(XWiki.EntityType.WIKI);
        wiki = (wiki && wiki.name) || XWiki.currentWiki;

        var space = reference.extractReference(XWiki.EntityType.SPACE);
        space = (space && space.name) || XWiki.currentSpace;

        var page = reference.extractReference(XWiki.EntityType.DOCUMENT);
        page = (page && page.name) || XWiki.currentPage;

        var attachment = reference.extractReference(XWiki.EntityType.ATTACHMENT);
        attachment = (attachment && attachment.name) || '';

        var documentReference = new XWiki.DocumentReference(wiki, space, page);
        var fullName = XWiki.Model.serialize(documentReference.relativeTo(new XWiki.WikiReference(wiki)));
        var prefixedSpace = XWiki.Model.serialize(documentReference.parent);
        var prefixedFullName = XWiki.Model.serialize(documentReference);

        return {
            wiki: wiki,
            space: space,
            prefixedSpace: prefixedSpace,
            fullName: fullName,
            prefixedFullName: prefixedFullName,
            name: page,
            attachment: attachment,
            anchor: anchor
        };
    },

    asEntityReference : function(resource) {
        var reference;
        var components = [resource.wiki, resource.space, resource.name, resource.attachment];
        for (var i = 0; i < components.length; i++) {
            if (components[i]) {
                reference = new XWiki.EntityReference(components[i], i, reference);
            }
        }
        return reference;
    },

    /**
     * Serializes the given resource.
     *
     * @param resource the resource to be serialized
     * @return a string that can be passed to the #get(String) method to reconstruct the resource
     */
    serialize: function(resource) {
        var name = XWiki.Model.serialize(this.asEntityReference(resource));
        if (resource.anchor) {
            if (name.length > 0) {
                name += XWiki.constants.anchorSeparator;
            }
            name += resource.anchor;
        }
        return name;
    }
  },

  /**
   * Deprecated. See XWiki.resource.get(String).
   */
  getResource : function(fullName) {
    return this.resource.get(fullName);
  },

  /**
   * Method used by docextra.vm to emulate tabbed panes.
   *
   * @param extraID Id of the pane to show.
   * @param extraTemplate Velocity template to retrieve and display in the pane.
   * @param scrollToAnchor Jump to the pane anchor.
   * @return
   */
   displayDocExtra: function (extraID, extraTemplate, scrollToAnchor) {
     // Nested function: hides the previously displayed extra pane (window.activeDocExtraPane)
     // and display the one that is passed as an argument (extraID).
     // Fires an event to notify that the pane has changed.
     var dhtmlSwitch = function(extraID) {
        var tab = document.getElementById(extraID + "tab");
        var pane = document.getElementById(extraID + "pane");
        if (window.activeDocExtraTab != null) {
            window.activeDocExtraTab.className="";
            window.activeDocExtraPane.className="hidden";
        }
        window.activeDocExtraTab = tab;
        window.activeDocExtraPane = pane;
        window.activeDocExtraTab.className="active";
        window.activeDocExtraPane.className="";
        tab.blur();

        document.fire("xwiki:docextra:activated", {"id": extraID});
     };

     // Use Ajax.Updater to display the requested pane (extraID) : comments, attachments, etc.
     // On complete :
     //   1. Call dhtmlSwitch()
     //   2. If the function call has been triggered by an event : reset location.href to #extraID
     //      (because when the link has been first clicked the anchor was not loaded)
     if ($(extraID + "pane").className.indexOf("empty") != -1) {
        if (window.activeDocExtraPane != null) {
            window.activeDocExtraPane.className="invisible";
        }
        $("docextrapanes").className="loading";
        new Ajax.Updater(
                extraID + "pane",
                window.docgeturl + '?xpage=xpart&vm=' + extraTemplate,
                {
                    method: 'post',
                    evalScripts: true,
                    onComplete: function(transport){
                      $("docextrapanes").className="";

                      // Let other know new content has been loaded
                      document.fire("xwiki:docextra:loaded", {
                        "id" : extraID,
                        "element": $(extraID + "pane")
                      });

                      // switch tab
                      dhtmlSwitch(extraID);

                      if (scrollToAnchor) {
                        // Yes, this is a POJW (Plain Old JavaScript Ha^Wworkaround) which
                        // prevents the anchor 'jump' after a click event but enable it
                        // when the user is arriving from a direct /Space/Page#Section URL
                        $(extraID + 'anchor').id = extraID;
                        location.href='#' + extraID;
                        $(extraID).id = extraID + 'anchor';
                      }
                    }
                });
     } else {
        dhtmlSwitch(extraID);
        if (scrollToAnchor) {
            $(extraID + 'anchor').id = extraID;
            location.href='#' + extraID;
            $(extraID).id = extraID + 'anchor';
        }
     }
  },

  /**
   * Add click listeners on all rendereing error messages to let the user read the detailed error description.
   * If a content is passed, add click listener for errors reported in this content (usefull for AJAX requests response)
   * Otherwise make all the document's body errors expandable.
   */
  makeRenderingErrorsExpandable: function(content) {
    if (typeof content == "undefined") {
      content = document.body;
    }
    $(content).select(".xwikirenderingerror").each(function(error) {
        if(error.next().innerHTML !== "" && error.next().hasClassName("xwikirenderingerrordescription")) {
            error.style.cursor="pointer";
            error.title = "$msg.get('platform.core.rendering.error.readTechnicalInformation')";
            Event.observe(error, "click", function(event){
                   event.element().next().toggleClassName("hidden");
            });
        }
    });
  },

  /**
   * Make links marked with rel="external" in an external window and sets the target attribute to any
   * rel attribute starting with "_". Note that We need to do this in Javascript
   * as opposed to using target="_blank" since the target attribute is not valid XHTML.
   * Apply this on links found in the passed content if any, or on the document's all body otherwise.
   */
  fixLinksTargetAttribute: function(content) {
    // apply this transformation only in the view mode, to not apply transformation on the content in edit mode to
    // avoid having it saved by the wysiwyg afterwards. Actually it should be anything different from edit or inline,
    // but like this is consistent with the next function, for section editing.
    if (XWiki.contextaction == "view" || XWiki.contextaction == "preview") {
      if (typeof content == "undefined") {
        content = document.body;
      }
      var anchors = content.select("a[rel]");
      for (var i = 0; i < anchors.length; i++) {
          var anchor = anchors[i];
          if (anchor.getAttribute("href") && anchor.getAttribute("rel")) {
              // Since the rel attribute can have other values we need to only take into account the ones
              // starting with "_"
              var values = anchor.getAttribute("rel").split(" ");
              for (var j = 0; j < values.length; j++) {
                  if (values[j].charAt(0) == "_") {
                      anchor.target = values[j].substring(1);
                      break;
                  } else if (values[j] == "external") {
                      anchor.target = "_blank";
                      break;
                  }
              }
          }
      }
    }
  },

  /**
   * Insert a link for editing sections.
   */
  insertSectionEditLinks: function() {
      // Insert links only in view mode and for documents not in xwiki/1.0 syntax
      if (XWiki.docsyntax != "xwiki/1.0" && XWiki.contextaction == "view" && XWiki.hasEdit) {

          // Section count starts at one, not zero.
          var sectioncount = 1;

          // We can't use element.select() since it does not keep the order of the elements in the flow.
          var nodes = $("xwikicontent");
          if (!nodes) {
            return;
          }
          nodes = nodes.childNodes;

          // Only allow section editing for the specified depth level (2 by default)
          var headerPattern = new RegExp("H[1-" + $xwiki.getSectionEditingDepth() + "]");

          // For all non-generated headers, add a SPAN and A element in order to be able to edit the section.
          for (var i = 0; i < nodes.length; i++) {

              var node = $(nodes[i]);

              if (headerPattern.test(node.nodeName) && node.className.include("wikigeneratedheader") == false) {
                  var editspan = document.createElement("SPAN");
                  var editlink = document.createElement("A");

                  editlink.href = window.docediturl + "?section=" + sectioncount;
                  editlink.style.textDecoration = "none";
                  editlink.innerHTML = "$msg.get('edit')";
                  editspan.className = "edit_section";

                  editspan.appendChild(editlink);
                  node.insert( { 'after': editspan } );
                  sectioncount++;
              }
          }
      }
  },

  /**
   * Display a modal box allowing to create the new document from a template when clicking on broken links.
   */
  insertCreatePageFromTemplateModalBoxes: function() {
      // Insert links only in view mode and for documents not in xwiki/1.0 syntax
      if (XWiki.docsyntax != "xwiki/1.0" && XWiki.contextaction == "view" && XWiki.hasEdit && XWiki.widgets.ModalPopup) {
          XWiki.widgets.CreatePagePopup = Class.create(XWiki.widgets.ModalPopup, {
              initialize : function($super, interactionParameters) {
                  var content =  new Element('div', {'class': 'modal-popup'});
                  content.insert(interactionParameters.content);
                  $super(
                          content,
                          {
                              "show"  : { method : this.showDialog,  keys : [] },
                              "close" : { method : this.closeDialog, keys : ['Esc'] }
                          },
                          {
                              displayCloseButton : true,
                              verticalPosition : "center",
                              backgroundColor : "#FFF"
                          }
                  );
                  this.showDialog();
                  this.setClass("createpage-modal-popup");
              }
          });

          var spans = document.body.select("span.wikicreatelink");
          for (var i = 0; i < spans.length; i++) {
              spans[i].down('a').observe('click', function(event) {
                  new Ajax.Request(event.findElement('a').href + '&xpage=createinline&ajax=1', {
                      method:'get',
                      onSuccess: function(transport) {
                          var redirect = transport.getHeader('redirect');
                          if (redirect) {
                            window.location = redirect;
                          } else {
                            new XWiki.widgets.CreatePagePopup({content: transport.responseText});
                          }
                      },
                      onFailure: function() {
                        new XWiki.widgets.Notification("$msg.get('core.create.ajax.error')", 'error', {inactive: true}).show();
                      }
                  });
                  event.stop();
              });
          }
      }
  },

  /**
   * Watchlist methods.
   */
  watchlist : {

    /**
     * Mapping between link IDs and associated actions.
     */
    actionsMap : {
        'tmWatchDocument' : 'adddocument',
        'tmUnwatchDocument' : 'removedocument',
        'tmWatchSpace' : 'addspace',
        'tmUnwatchSpace' : 'removespace',
        'tmWatchWiki' : 'addwiki',
        'tmUnwatchWiki' : 'removewiki'
    },

    /**
     * Mapping allowing to know which action to display when a previous action has been executed.
     */
    flowMap : {
        'tmWatchDocument' : 'tmUnwatchDocument',
        'tmUnwatchDocument' : 'tmWatchDocument',
        'tmWatchSpace' : 'tmUnwatchSpace',
        'tmUnwatchSpace' : 'tmWatchSpace',
        'tmWatchWiki' : 'tmUnwatchWiki',
        'tmUnwatchWiki' : 'tmWatchWiki'
    },

    /**
     * Execute a watchlist action (add or remove the given document/space/wiki from watchlist).
     *
     * @param element the element that fired the action.
     */
    executeAction : function(element) {
        var surl = window.docgeturl + "?xpage=watch&do=" + this.actionsMap[element.id];
        var myAjax = new Ajax.Request(
          surl,
          {
            method: 'get',
            onComplete: function() {
              if (element.nodeName == 'A') {
                element.up().toggleClassName('hidden');
                $(XWiki.watchlist.flowMap[element.id]).up().toggleClassName('hidden');
              } else {
                element.toggleClassName('hidden');
                $(XWiki.watchlist.flowMap[element.id]).toggleClassName('hidden');
              }
            }
          });
    },

    /**
     * Initialize watchlist UI.
     */
    initialize: function() {
        for (button in XWiki.watchlist.actionsMap) {
          if ($(button) != null) {
            var element = $(button);
            var self = this;

            if (element.nodeName != 'A') {
              element = $(button).down('A');
            }

            // unregister previously registered handler if any
            element.stopObserving('click');
            element.observe('click', function(event) {
                Event.stop(event);
                var element = event.element();
                while (element.id == '') {
                    element = element.up();
                }
                XWiki.watchlist.executeAction(element);
              });
          }
        }
    }
  },

  cookies: {
    /**
     * Create a cookie, with or without expiration date.
     *
     * @param name Name of the cookie.
     * @param value Value of the cookie.
     * @param days Days to keep the cookie (can be null).
     * @return
     */
    create: function(name,value,days) {
        if (days) {
            var date = new Date();
            date.setTime(date.getTime()+(days*24*60*60*1000));
            var expires = "; expires="+date.toGMTString();
        }
        else var expires = "";
        document.cookie = name+"="+value+expires+"; path=/";
    },

    /**
     * Read a cookie.
     *
     * @param name Name of the cookie.
     * @return Value for the given cookie.
     */
    read:function(name) {
        var nameEQ = name + "=";
        var ca = document.cookie.split(';');
        for(var i=0;i < ca.length;i++) {
            var c = ca[i];
            while (c.charAt(0)==' ') {
                c = c.substring(1,c.length);
            }
            if (c.indexOf(nameEQ) == 0) {
                return c.substring(nameEQ.length,c.length);
            }
        }
        return null;
    },

    /**
     * Erase a cookie.
     *
     * @param name Name of the cookie to erase.
     * @return
     */
    erase:function(name) {
        XWiki.cookies.create(name,"",-1);
    }

  },

  /**
   * Expand the given panel if collapsed, collapse if visible.
   *
   * @param form  {element} The panel element.
   */
  togglePanelVisibility: function(element){
    element = $(element);
    element.toggleClassName("collapsed");
  },

  registerPanelToggle: function() {
    $$('.panel .xwikipaneltitle').each(function(item) {
      item.observe('click', this.togglePanelVisibility.bind(this, item.up('.panel')));
    }.bind(this));
  },

  /**
   * Extracts the file name from the value of the specified file input.
   */
  extractFileName: function(fileInput) {
    fileInput = $(fileInput);
    if (fileInput.files && fileInput.files.length > 0) {
      // Modern browsers provide additional information about the selected file(s).
      return fileInput.files[0].name;
    } else if (fileInput.value.substr(0, 12) == 'C:\\fakepath\\') {
      // Most browsers hide the real path for security reasons.
      return fileInput.value.substr(12);
    } else {
      var lastPathSeparatorIndex = fileInput.value.lastIndexOf('/');
      if (lastPathSeparatorIndex >= 0) {
        // Unix-based path.
        return fileInput.value.substr(lastPathSeparatorIndex + 1);
      }
      lastPathSeparatorIndex = fileInput.value.lastIndexOf('\\');
      if (lastPathSeparatorIndex >= 0) {
        // Windows-based path.
        return fileInput.value.substr(lastPathSeparatorIndex + 1);
      }
      // The file input value is just the file name.
      return fileInput.value;
    }
  },

  /**
   * Initialize method for the XWiki object. This is to be called only once upon dom loading.
   * It makes rendering errors expandable and fixes external links on the body content.
   * Then it fires an custom event to signify the (modified) DOM is now loaded.
   */
  initialize: function() {
    // Extra security to make sure we do not get initalized twice.
    // It would fire the custom xwiki:dom:loaded event twice, which could make their observers misbehave.
    if (typeof this.isInitialized == "undefined" || this.isInitialized == false) {
      // This variable is set when the marker script is executed, which should always be the last script to execute.
      // In case the dom:loaded event was fired prematurely, delay the execution and ask instead the marker script to
      // re-invoke the initialization process.
      if (typeof XWiki.lastScriptLoaded == "undefined") {
        XWiki.failedInit = true;
        return;
      }

      // All ready, continue with the initialization.
      this.isInitialized = true;
      document.fire("xwiki:dom:loading");

      this.makeRenderingErrorsExpandable();
      this.fixLinksTargetAttribute();
      this.insertSectionEditLinks();
      this.insertCreatePageFromTemplateModalBoxes();
      this.watchlist.initialize();
      this.registerPanelToggle();

      this.domIsLoaded = true;
      document.fire("xwiki:dom:loaded");
    }
  }
});

return XWiki;

})(XWiki || {});

/**
 * Hook to initialize the XWiki object.
 * All other code should ideally observe "xwiki:dom:loaded" instead of Prototype's "dom:loaded"
 * in order to be sure they will benefit from the transformations on the DOM made by XWiki.
 */
document.observe("dom:loaded", XWiki.initialize.bind(XWiki));

// Passed this point, the methods are not XWiki-namespaced.
// They should be progressively cleaned, and we should not add any other of such.
// See http://jira.xwiki.org/jira/browse/XWIKI-3175

/**
 * Show items under the given entry in the top menu (menuview.vm).
 *
 * @param element The selected item
 * @return
 */
function showsubmenu(element){
    if(element.lastChild.tagName.toLowerCase() == "span"){
        if(window.hidetimer){
            if(window.hideelement == element.lastChild){
                clearTimeout(window.hidetimer);
                window.hidetimer = null;
                window.hideelement = null;
            }
            else{
                doHide();
            }
        }
        var coords = Element.positionedOffset(element);
        element.lastChild.style.left = (coords[0] - 10) + "px";
        element.lastChild.style.top = (coords[1] + element.offsetHeight) + "px";
        element.lastChild.className = element.lastChild.className.replace("hidden", "visible");
    }
}

/**
 * hide items under the given entry in the top menu (menuview.vm).
 *
 * @param element The selected item
 * @return
 */
function hidesubmenu(element){
    if(element.lastChild.tagName.toLowerCase() == "span"){
        window.hideelement = element.lastChild;
        window.hidetimer = setTimeout(doHide, 100);
    }
}

/**
 * Method doing the hide action on the element set by hidesubmenu() in the window object.
 *
 * @return
 */
function doHide(){
    window.hideelement.className = window.hideelement.className.replace("visible", "hidden");
    clearTimeout(window.hidetimer);
    window.hidetimer = null;
    window.hideelement = null;
}

/**
 * Toggle CSS class in the given element.
 *
 * @param o Element.
 * @param className CSS class.
 * @return
 */
function toggleClass(o, className){
    if(!eltHasClass(o,className)) {
        o.className += ' ' + className
    }
    else {
        rmClass(o, className);
    }
}

/**
 * Add a CSS class to an element.
 *
 * @param o Element.
 * @param className CSS class.
 * @return
 */
function addClass(o, className){
    if(!eltHasClass(o,className))
        o.className += ' ' + className
}

/**
 * Check if an element has a CSS class.
 *
 * @param o Element.
 * @param className CSS class.
 * @return True if the element has the class.
 */
function eltHasClass(o,className){
    if(!o.className)
        return false;
    return new RegExp('\\b' + className + '\\b').test(o.className)
}

/**
 * Remove a CSS class from an element.
 *
 * @param o Element.
 * @param className CSS class.
 * @return
 */
function rmClass(o, className){
    o.className = o.className.replace(new RegExp('\\s*\\b' + className + '\\b'),'')
}

/**
 * Open an URL in a pop-up.
 *
 * @param url URL to open.
 * @return
 */
function openURL(url) {
    win = open( url, "win", "titlebar=0,width=990,height=500,resizable,scrollbars");
    if( win ) {
        win.focus();
    }
}

/**
 * Open XWiki syntax documentation in a pop-up.
 *
 * @deprecated
 * @return
 */
function openHelp() {
    win = open( "http://platform.xwiki.org/xwiki/bin/view/Main/XWikiSyntax?xpage=print", "XWikiSyntax", "titlebar=0,width=750,height=480,resizable,scrollbars");
    if( win ) {
        win.focus();
    }
}

/**
 * Remove special characters from text inputs.
 *
 * @param field1 Text input
 * @param field2 Text input
 * @param removeclass
 * @return true if the text empty after the operation.
 */
function updateName(field1, field2, removeclass) {
    var name = field1.value;
    name = noaccent(name);
    if (removeclass!=false) {
        name = name.replace(/class$/gi,"");
    }
    if (field2 == null) {
        field1.value = name;
    } else {
        field2.value = name;
    }
    if (name=="") {
        return false;
    }
    return true;
}

/**
 * Replace accented chars by non-accented chars in a string.
 *
 * @param txt String to clean.
 * @return The cleaned string.
 */
function noaccent(txt) {
    temp = txt.replace(/[\u00c0\u00c1\u00c2\u00c3\u00c4\u00c5\u0100\u0102\u0104\u01cd\u01de\u01e0\u01fa\u0200\u0202\u0226]/g,"A");
    temp = temp.replace(/[\u00e0\u00e1\u00e2\u00e3\u00e4\u00e5\u0101\u0103\u0105\u01ce\u01df\u01e1\u01fb\u0201\u0203\u0227]/g,"a");
    temp = temp.replace(/[\u00c6\u01e2\u01fc]/g,"AE");
    temp = temp.replace(/[\u00e6\u01e3\u01fd]/g,"ae");
    temp = temp.replace(/[\u008c\u0152]/g,"OE");
    temp = temp.replace(/[\u009c\u0153]/g,"oe");
    temp = temp.replace(/[\u00c7\u0106\u0108\u010a\u010c]/g,"C");
    temp = temp.replace(/[\u00e7\u0107\u0109\u010b\u010d]/g,"c");
    temp = temp.replace(/[\u00d0\u010e\u0110]/g,"D");
    temp = temp.replace(/[\u00f0\u010f\u0111]/g,"d");
    temp = temp.replace(/[\u00c8\u00c9\u00ca\u00cb\u0112\u0114\u0116\u0118\u011a\u0204\u0206\u0228]/g,"E");
    temp = temp.replace(/[\u00e8\u00e9\u00ea\u00eb\u0113\u0115\u0117\u0119\u011b\u01dd\u0205\u0207\u0229]/g,"e");
    temp = temp.replace(/[\u011c\u011e\u0120\u0122\u01e4\u01e6\u01f4]/g,"G");
    temp = temp.replace(/[\u011d\u011f\u0121\u0123\u01e5\u01e7\u01f5]/g,"g");
    temp = temp.replace(/[\u0124\u0126\u021e]/g,"H");
    temp = temp.replace(/[\u0125\u0127\u021f]/g,"h");
    temp = temp.replace(/[\u00cc\u00cd\u00ce\u00cf\u0128\u012a\u012c\u012e\u0130\u01cf\u0208\u020a]/g,"I");
    temp = temp.replace(/[\u00ec\u00ed\u00ee\u00ef\u0129\u012b\u012d\u012f\u0131\u01d0\u0209\u020b]/g,"i");
    temp = temp.replace(/[\u0132]/g,"IJ");
    temp = temp.replace(/[\u0133]/g,"ij");
    temp = temp.replace(/[\u0134]/g,"J");
    temp = temp.replace(/[\u0135]/g,"j");
    temp = temp.replace(/[\u0136\u01e8]/g,"K");
    temp = temp.replace(/[\u0137\u0138\u01e9]/g,"k");
    temp = temp.replace(/[\u0139\u013b\u013d\u013f\u0141]/g,"L");
    temp = temp.replace(/[\u013a\u013c\u013e\u0140\u0142\u0234]/g,"l");
    temp = temp.replace(/[\u00d1\u0143\u0145\u0147\u014a\u01f8]/g,"N");
    temp = temp.replace(/[\u00f1\u0144\u0146\u0148\u0149\u014b\u01f9\u0235]/g,"n");
    temp = temp.replace(/[\u00d2\u00d3\u00d4\u00d5\u00d6\u00d8\u014c\u014e\u0150\u01d1\u01ea\u01ec\u01fe\u020c\u020e\u022a\u022c\u022e\u0230]/g,"O");
    temp = temp.replace(/[\u00f2\u00f3\u00f4\u00f5\u00f6\u00f8\u014d\u014f\u0151\u01d2\u01eb\u01ed\u01ff\u020d\u020f\u022b\u022d\u022f\u0231]/g,"o");
    temp = temp.replace(/[\u0156\u0158\u0210\u0212]/g,"R");
    temp = temp.replace(/[\u0157\u0159\u0211\u0213]/g,"r");
    temp = temp.replace(/[\u015a\u015c\u015e\u0160\u0218]/g,"S");
    temp = temp.replace(/[\u015b\u015d\u015f\u0161\u0219]/g,"s");
    temp = temp.replace(/[\u00de\u0162\u0164\u0166\u021a]/g,"T");
    temp = temp.replace(/[\u00fe\u0163\u0165\u0167\u021b\u0236]/g,"t");
    temp = temp.replace(/[\u00d9\u00da\u00db\u00dc\u0168\u016a\u016c\u016e\u0170\u0172\u01d3\u01d5\u01d7\u01d9\u01db\u0214\u0216]/g,"U");
    temp = temp.replace(/[\u00f9\u00fa\u00fb\u00fc\u0169\u016b\u016d\u016f\u0171\u0173\u01d4\u01d6\u01d8\u01da\u01dc\u0215\u0217]/g,"u");
    temp = temp.replace(/[\u0174]/g,"W");
    temp = temp.replace(/[\u0175]/g,"w");
    temp = temp.replace(/[\u00dd\u0176\u0178\u0232]/g,"Y");
    temp = temp.replace(/[\u00fd\u00ff\u0177\u0233]/g,"y");
    temp = temp.replace(/[\u0179\u017b\u017d]/g,"Z");
    temp = temp.replace(/[\u017a\u017c\u017e]/g,"z");
    temp = temp.replace(/[\u00df]/g,"SS");
    temp = temp.replace(/[^a-zA-Z0-9_]/g,"");
    return temp;
}

/**
 * Method used by register.vm to concatenate first name and last name to generate
 * the name of the profile page of the user who is registering.
 *
 * @param form The register form.
 * @return
 */
function prepareName(form) {
    var fname = form.register_first_name.value;
    var lname = form.register_last_name.value;
    var cxwikiname = form.xwikiname;
    if (fname != "") {
        fname = fname.substring(0,1).toUpperCase() + fname.substring(1);
        fname.replace(/ /g,"");
    }
    if (lname != "") {
        lname = lname.substring(0,1).toUpperCase() + lname.substring(1)
        lname.replace(/ /g,"");
    }
    if (cxwikiname.value == "") {
        cxwikiname.value = noaccent(fname + lname);
    }
}

/**
 * Method used by editmodes.vm to warn the user if he tries to go to the WYSIWYG editor
 * with HTML in his content.
 *
 * @param message Translated warning message.
 */
function checkAdvancedContent(message) {
    result = false;
    if (!document.forms.edit) {
        return true;
    }
    data = document.forms.edit.content.value;
    myRE = new RegExp("</?(html|body|img|a|i|b|embed|script|form|input|textarea|object|font|li|ul|ol|table|center|hr|br|p) ?([^>]*)>", "ig")
    results = data.match(myRE)
    if (results&&results.length>0)
        result = true;

    myRE2 = new RegExp("(#(set|include|if|end|for)|#(#) Advanced content|public class|/\* Advanced content \*/)", "ig")
    results = data.match(myRE2)
    if (results&&results.length>0)
        result = true;

    if (result==true)
        return confirm(message);

    return true;
}

/**
 * Keyboard Shortcuts.
 * Version: 2.01.A
 * URL: http://www.openjs.com/scripts/events/keyboard_shortcuts/
 * Author: Binny VA
 * License : BSD
 */
shortcut = {
        'all_shortcuts':{},//All the shortcuts are stored in this array
        'add': function(shortcut_combination,callback,opt) {
            //Provide a set of default options
            var default_options = {
                    'type':'keydown',
                    'propagate':false,
                    'disable_in_input':false,
                    'target':document,
                    'keycode':false
            }
            if(!opt) opt = default_options;
            else {
                for(var dfo in default_options) {
                    if(typeof opt[dfo] == 'undefined') opt[dfo] = default_options[dfo];
                }
            }

            var ele = opt.target
            if(typeof opt.target == 'string') ele = document.getElementById(opt.target);
            var ths = this;
            shortcut_combination = shortcut_combination.toLowerCase();

            //The function to be called at keypress
            var func = function(e) {
                e = e || window.event;

                if(opt['disable_in_input']) { //Don't enable shortcut keys in Input, Textarea fields
                    var element;
                    if(e.target) element=e.target;
                    else if(e.srcElement) element=e.srcElement;
                    if(element.nodeType==3) element=element.parentNode;
                    if(element.tagName == 'INPUT' || element.tagName == 'TEXTAREA' || element.tagName == 'SELECT') {
                        return;
                    }
                }

                //Find Which key is pressed
                var code = 0;
                if (e.keyCode) code = e.keyCode;
                else if (e.which) code = e.which;
                var character = String.fromCharCode(code).toLowerCase();

                if(code == 188) character=","; //If the user presses , when the type is onkeydown
                if(code == 190) character="."; //If the user presses , when the type is onkeydown

                var keys = shortcut_combination.split("+");
                //Key Pressed - counts the number of valid keypresses - if it is same as the number of keys, the shortcut function is invoked
                var kp = 0;

                //Work around for stupid Shift key bug created by using lowercase - as a result the shift+num combination was broken
                var shift_nums = {
                        "`":"~",
                        "1":"!",
                        "2":"@",
                        "3":"#",
                        "4":"$",
                        "5":"%",
                        "6":"^",
                        "7":"&",
                        "8":"*",
                        "9":"(",
                        "0":")",
                        "-":"_",
                        "=":"+",
                        ";":":",
                        "'":"\"",
                        ",":"<",
                        ".":">",
                        "/":"?",
                        "\\":"|"
                }
                //Special Keys - and their codes
                var special_keys = {
                        'esc':27,
                        'escape':27,
                        'tab':9,
                        'space':32,
                        'return':13,
                        'enter':13,
                        'backspace':8,

                        'scrolllock':145,
                        'scroll_lock':145,
                        'scroll':145,
                        'capslock':20,
                        'caps_lock':20,
                        'caps':20,
                        'numlock':144,
                        'num_lock':144,
                        'num':144,

                        'pause':19,
                        'break':19,

                        'insert':45,
                        'home':36,
                        'delete':46,
                        'end':35,

                        'pageup':33,
                        'page_up':33,
                        'pu':33,

                        'pagedown':34,
                        'page_down':34,
                        'pd':34,

                        'left':37,
                        'up':38,
                        'right':39,
                        'down':40,

                        'f1':112,
                        'f2':113,
                        'f3':114,
                        'f4':115,
                        'f5':116,
                        'f6':117,
                        'f7':118,
                        'f8':119,
                        'f9':120,
                        'f10':121,
                        'f11':122,
                        'f12':123
                }

                var modifiers = {
                        shift: { wanted:false, pressed:false},
                        ctrl : { wanted:false, pressed:false},
                        alt  : { wanted:false, pressed:false},
                        meta : { wanted:false, pressed:false}   //Meta is Mac specific
                };
                if(e.ctrlKey)   modifiers.ctrl.pressed = true;
                if(e.shiftKey)  modifiers.shift.pressed = true;
                if(e.altKey)    modifiers.alt.pressed = true;
                if(e.metaKey)   modifiers.meta.pressed = true;

                for(var i=0; k=keys[i],i<keys.length; i++) {
                    //Modifiers
                    if(k == 'ctrl' || k == 'control') {
                        kp++;
                        modifiers.ctrl.wanted = true;

                    } else if(k == 'shift') {
                        kp++;
                        modifiers.shift.wanted = true;

                    } else if(k == 'alt') {
                        kp++;
                        modifiers.alt.wanted = true;
                    } else if(k == 'meta') {
                        kp++;
                        modifiers.meta.wanted = true;
                    } else if(k.length > 1) { //If it is a special key
                        if(special_keys[k] == code) kp++;

                    } else if(opt['keycode']) {
                        if(opt['keycode'] == code) kp++;

                    } else { //The special keys did not match
                        if(character == k) kp++;
                        else {
                            if(shift_nums[character] && e.shiftKey) { //Stupid Shift key bug created by using lowercase
                                character = shift_nums[character];
                                if(character == k) kp++;
                            }
                        }
                    }
                }

                if(kp == keys.length &&
                        modifiers.ctrl.pressed == modifiers.ctrl.wanted &&
                        modifiers.shift.pressed == modifiers.shift.wanted &&
                        modifiers.alt.pressed == modifiers.alt.wanted &&
                        modifiers.meta.pressed == modifiers.meta.wanted) {
                    callback(e);

                    if(!opt['propagate']) { //Stop the event
                        //e.cancelBubble is supported by IE - this will kill the bubbling process.
                        e.cancelBubble = true;
                        e.returnValue = false;

                        // XWIKI : added to force Alt+key events not to be propagated to IE7
                        if (document.all && !window.opera && window.XMLHttpRequest) {
                            e.keyCode = 0;
                        }

                        //e.stopPropagation works in Firefox.
                        if (e.stopPropagation) {
                            e.stopPropagation();
                            e.preventDefault();
                        }
                        return false;
                    }
                }
            }
            this.all_shortcuts[shortcut_combination] = {
                    'callback':func,
                    'target':ele,
                    'event': opt['type']
            };
            //Attach the function with the event
            if(ele.addEventListener) ele.addEventListener(opt['type'], func, false);
            else if(ele.attachEvent) ele.attachEvent('on'+opt['type'], func);
            else ele['on'+opt['type']] = func;
        },

        //Remove the shortcut - just specify the shortcut and I will remove the binding
        'remove':function(shortcut_combination) {
            shortcut_combination = shortcut_combination.toLowerCase();
            var binding = this.all_shortcuts[shortcut_combination];
            delete(this.all_shortcuts[shortcut_combination])
            if(!binding) return;
            var type = binding['event'];
            var ele = binding['target'];
            var callback = binding['callback'];

            if(ele.detachEvent) ele.detachEvent('on'+type, callback);
            else if(ele.removeEventListener) ele.removeEventListener(type, callback, false);
            else ele['on'+type] = false;
        }
}

/**
 * Browser Detect
 * Version: 2.1.6
 * URL: http://dithered.chadlindstrom.ca/javascript/browser_detect/index.html
 * License: http://creativecommons.org/licenses/by/1.0/
 * Author: Chris Nott (chris[at]dithered[dot]com)
*/
function BrowserDetect() {
    var ua = navigator.userAgent.toLowerCase();

    // browser engine name
    this.isGecko       = (ua.indexOf('gecko') != -1 && ua.indexOf('safari') == -1);
    this.isAppleWebKit = (ua.indexOf('applewebkit') != -1);

    // browser name
    this.isKonqueror   = (ua.indexOf('konqueror') != -1);
    this.isSafari      = (ua.indexOf('safari') != - 1);
    this.isOmniweb     = (ua.indexOf('omniweb') != - 1);
    this.isOpera       = (ua.indexOf('opera') != -1);
    this.isIcab        = (ua.indexOf('icab') != -1);
    this.isAol         = (ua.indexOf('aol') != -1);
    this.isIE          = (ua.indexOf('msie') != -1 && !this.isOpera && (ua.indexOf('webtv') == -1) );
    this.isMozilla     = (this.isGecko && ua.indexOf('gecko/') + 14 == ua.length);
    this.isFirefox     = (ua.indexOf('firefox/') != -1 || ua.indexOf('firebird/') != -1);
    this.isNS          = ( (this.isGecko) ? (ua.indexOf('netscape') != -1) : ( (ua.indexOf('mozilla') != -1) && !this.isOpera && !this.isSafari && (ua.indexOf('spoofer') == -1) && (ua.indexOf('compatible') == -1) && (ua.indexOf('webtv') == -1) && (ua.indexOf('hotjava') == -1) ) );

    // spoofing and compatible browsers
    this.isIECompatible = ( (ua.indexOf('msie') != -1) && !this.isIE);
    this.isNSCompatible = ( (ua.indexOf('mozilla') != -1) && !this.isNS && !this.isMozilla);

    // rendering engine versions
    this.geckoVersion = ( (this.isGecko) ? ua.substring( (ua.lastIndexOf('gecko/') + 6), (ua.lastIndexOf('gecko/') + 14) ) : -1 );
    this.equivalentMozilla = ( (this.isGecko) ? parseFloat( ua.substring( ua.indexOf('rv:') + 3 ) ) : -1 );
    this.appleWebKitVersion = ( (this.isAppleWebKit) ? parseFloat( ua.substring( ua.indexOf('applewebkit/') + 12) ) : -1 );

    // browser version
    this.versionMinor = parseFloat(navigator.appVersion);

    // correct version number
    if (this.isGecko && !this.isMozilla) {
        this.versionMinor = parseFloat( ua.substring( ua.indexOf('/', ua.indexOf('gecko/') + 6) + 1 ) );
    }
    else if (this.isMozilla) {
        this.versionMinor = parseFloat( ua.substring( ua.indexOf('rv:') + 3 ) );
    }
    else if (this.isIE && this.versionMinor >= 4) {
        this.versionMinor = parseFloat( ua.substring( ua.indexOf('msie ') + 5 ) );
    }
    else if (this.isKonqueror) {
        this.versionMinor = parseFloat( ua.substring( ua.indexOf('konqueror/') + 10 ) );
    }
    else if (this.isSafari) {
        this.versionMinor = parseFloat( ua.substring( ua.lastIndexOf('safari/') + 7 ) );
    }
    else if (this.isOmniweb) {
        this.versionMinor = parseFloat( ua.substring( ua.lastIndexOf('omniweb/') + 8 ) );
    }
    else if (this.isOpera) {
        this.versionMinor = parseFloat( ua.substring( ua.indexOf('opera') + 6 ) );
    }
    else if (this.isIcab) {
        this.versionMinor = parseFloat( ua.substring( ua.indexOf('icab') + 5 ) );
    }

    this.versionMajor = parseInt(this.versionMinor);

    // dom support
    this.isDOM1 = (document.getElementById);
    this.isDOM2Event = (document.addEventListener && document.removeEventListener);

    // css compatibility mode
    this.mode = document.compatMode ? document.compatMode : 'BackCompat';

    // platform
    this.isWin    = (ua.indexOf('win') != -1);
    this.isWin32  = (this.isWin && ( ua.indexOf('95') != -1 || ua.indexOf('98') != -1 || ua.indexOf('nt') != -1 || ua.indexOf('win32') != -1 || ua.indexOf('32bit') != -1 || ua.indexOf('xp') != -1) );
    this.isMac    = (ua.indexOf('mac') != -1);
    this.isUnix   = (ua.indexOf('unix') != -1 || ua.indexOf('sunos') != -1 || ua.indexOf('bsd') != -1 || ua.indexOf('x11') != -1)
    this.isLinux  = (ua.indexOf('linux') != -1);

    // specific browser shortcuts
    this.isNS4x = (this.isNS && this.versionMajor == 4);
    this.isNS40x = (this.isNS4x && this.versionMinor < 4.5);
    this.isNS47x = (this.isNS4x && this.versionMinor >= 4.7);
    this.isNS4up = (this.isNS && this.versionMinor >= 4);
    this.isNS6x = (this.isNS && this.versionMajor == 6);
    this.isNS6up = (this.isNS && this.versionMajor >= 6);
    this.isNS7x = (this.isNS && this.versionMajor == 7);
    this.isNS7up = (this.isNS && this.versionMajor >= 7);

    this.isIE4x = (this.isIE && this.versionMajor == 4);
    this.isIE4up = (this.isIE && this.versionMajor >= 4);
    this.isIE5x = (this.isIE && this.versionMajor == 5);
    this.isIE55 = (this.isIE && this.versionMinor == 5.5);
    this.isIE5up = (this.isIE && this.versionMajor >= 5);
    this.isIE6x = (this.isIE && this.versionMajor == 6);
    this.isIE6up = (this.isIE && this.versionMajor >= 6);

    this.isIE4xMac = (this.isIE4x && this.isMac);
}
var browser = new BrowserDetect();

/**
 * XWiki Model access APIs.
 */
XWiki.Document = Class.create({
  /**
   * Constructor. All parameters are optional, and default to the current document location.
   */
  initialize : function(page, space, wiki) {
    this.page = page || XWiki.Document.currentPage;
    this.space = space || XWiki.Document.currentSpace;
    this.wiki = wiki || XWiki.Document.currentWiki;
  },
  /**
   * Gets an URL pointing to this document.
   */
  getURL : function(action, queryString, fragment) {
    action = action || 'view';
    var url = XWiki.Document.URLTemplate;
    url = url.replace("__space__", encodeURIComponent(this.space));
    url = url.replace("__page__", (this.page == 'WebHome') ? '' : encodeURIComponent(this.page));
    url = url.replace("__action__/", (action == 'view') ? '' : (encodeURIComponent(action) + "/"));
    if (queryString) {
      url += '?' + queryString;
    }
    if (fragment) {
      url += '#' + fragment;
    }
    return url;
  },
  /**
   * Gets an URL which points to the REST location for accessing this document.
   */
  getRestURL : function(entity, queryString) {
    entity = entity || '';
    var url = XWiki.Document.RestURLTemplate;
    url = url.replace("__wiki__", this.wiki);
    url = url.replace("__space__", this.space);
    url = url.replace("__page__", this.page);
    if (entity) {
      url += "/" + entity;
    }
    if (queryString) {
      url += '?' + queryString;
    }
    return url;
  }
});

/* Initialize the document URL factory, and create XWiki.currentDocument. */
document.observe('xwiki:dom:loading', function() {
  XWiki.Document.currentWiki = ($$("meta[name=wiki]").length > 0) ? $$("meta[name=wiki]")[0].content : (XWiki.currentWiki || "xwiki");
  XWiki.Document.currentSpace = ($$("meta[name=space]").length > 0) ? $$("meta[name=space]")[0].content : (XWiki.currentSpace || "Main");
  XWiki.Document.currentPage = ($$("meta[name=page]").length > 0) ? $$("meta[name=page]")[0].content : (XWiki.currentPage || "WebHome");
  XWiki.Document.URLTemplate = "$xwiki.getURL('__space__.__page__', '__action__')";
  XWiki.Document.RestURLTemplate = "${request.contextPath}/rest/wikis/__wiki__/spaces/__space__/pages/__page__";
  XWiki.Document.WikiSearchURLStub = "${request.contextPath}/rest/wikis/__wiki__/search";
  XWiki.Document.SpaceSearchURLStub = "${request.contextPath}/rest/wikis/__wiki__/spaces/__space__/search";
  XWiki.Document.getRestSearchURL = function(queryString, space, wiki) {
    wiki = wiki || XWiki.Document.currentWiki;
    var url;
    if (space) {
      url = XWiki.Document.SpaceSearchURLStub.replace("__wiki__", wiki).replace("__space__", space);
    } else {
      url = XWiki.Document.WikiSearchURLStub.replace("__wiki__", wiki);
    }
    if (queryString) {
      url += "?" + queryString;
    }
    return url;
  };
  XWiki.currentDocument = new XWiki.Document();
});

/**
 * Small JS improvement, which automatically hides and reinserts the default text for input fields, acting as a tip.
 *
 * To activate this behavior on an input element, set a "placeholder" attribute, or add the "withTip" classname to it,
 * or pass it as the 'element' value of the memo of a 'xwiki:addBehavior:withTip' event.
 */
(function(){
  var placeholderPolyfill;
  if ('placeholder' in document.createElement('input')) {
    // For browsers that do support the 'placeholder' attribute, we just add support for the older way of supporting this through the 'withTip' classname and the default input value.
    placeholderPolyfill = function(event) {
      var item = event.memo.element;
      if (item.placeholder === '') {
        item.placeholder = item.defaultValue;
        item.value = '';
      }
    }
  } else {
    // For browsers that don't support the 'placeholder' attribute, we simulate it with 'focus' and 'blur' event handlers.
    var onFocus = function() {
      if (this.value == this.defaultValue) {
        this.value = '';
      } else {
        this.select();
      }
    }
    var onBlur = function() {
      if (this.value == '') {
        this.value = this.defaultValue;
      }
    }
    placeholderPolyfill = function(event) {
      var item = event.memo.element;
      if (item.readAttribute('placeholder')) {
        item.defaultValue = item.readAttribute('placeholder');
      }
      item.observe('focus', onFocus.bindAsEventListener(item));
      item.observe('blur', onBlur.bindAsEventListener(item));
    }
  }
  document.observe('xwiki:addBehavior:withTip', placeholderPolyfill);
  document.observe('xwiki:dom:loaded', function() {
    $$("input.withTip", "textarea.withTip", "[placeholder]").each(function(item) {
      document.fire("xwiki:addBehavior:withTip", {'element' : item});
    });
  });
  document.observe('xwiki:dom:updated', function(event) {
    event.memo.elements.each(function(element) {
      element.select("input.withTip", "textarea.withTip", "[placeholder]").each(function(item) {
        document.fire("xwiki:addBehavior:withTip", {'element' : item});
      });
    });
  });
})();
/**
 * Small JS improvement, which suggests document names (doc.fullName) when typing in an input.
 *
 * To activate this behavior on an input elements, add one of the following classname to it :
 * <ul>
 * <li><tt>suggestDocuments</tt> to suggest from any available document</li>
 * <li><tt>suggestSpaces</tt> to suggest space names</li>
 * </ul>
 */
document.observe('xwiki:dom:loaded', function() {
    var suggestionsMapping = {
        "documents" : {
            script: XWiki.Document.getRestSearchURL("scope=name&number=10&media=json&"),
            varname: "q",
            icon: "$xwiki.getSkinFile('icons/silk/page_white_text.png')",
            noresults: "Document not found",
            json: true,
            resultsParameter : "searchResults",
            resultId : "id",
            resultValue : "pageFullName",
            resultInfo : "pageFullName"
        },
        "spaces" : {
            script: XWiki.Document.getRestSearchURL("scope=spaces&number=10&media=json&"),
            varname: "q",
            icon: "$xwiki.getSkinFile('icons/silk/folder.png')",
            noresults: "Space not found",
            json: true,
            resultsParameter : "searchResults",
            resultId : "id",
            resultValue : "space",
            resultInfo : "space"
        }
    };
    var addSuggests = function(elements) {
      if (typeof(XWiki.widgets.Suggest) != "undefined") {
        var keys = Object.keys(suggestionsMapping);
        for (var i=0;i<keys.length;i++) {
          var selector = 'input.suggest' + keys[i].capitalize();
          elements.each(function(element) {$(element).select(selector).each(function(item) {
            if (!item.hasClassName('initialized')) {
              var options = {
                timeout : 30000,
                parentContainer : item.up()
              };
              Object.extend(options, suggestionsMapping[keys[i]]);
              // Create the Suggest.
              var suggest = new XWiki.widgets.Suggest(item, options);
              item.addClassName('initialized');
            }
          })});
        }
      }
    };
    addSuggests([$(document.documentElement)]);
    document.observe('xwiki:dom:updated', function(event) {
      addSuggests(event.memo.elements);
    });
});

/**
 * Small JS improvement, which blocks normal browser autocomplete for fields which use an AJAX suggest,
 * and initializes the suggest object early instead of waiting for the field to be focused.
 *
 * To activate this behavior on an input elements, add the "suggested" classname to it.
 */
['xwiki:dom:loaded', 'xwiki:dom:updated'].each(function(eventName) {
  document.observe(eventName, function(event) {
    if (typeof(XWiki.widgets.Suggest) != "undefined") {
      var elements = event.memo && event.memo.elements || [document.documentElement];
      elements.each(function(element) {
        element.select(".suggested").each(function(item) {
          item.setAttribute("autocomplete", "off");
          if (typeof item.onfocus === "function") {
            item.onfocus();
            item.removeAttribute("onfocus");
          }
        });
      });
    }
  });
});

/*
 * AJAX improvements for setting the document parent.
 */
document.observe('xwiki:dom:loaded', function() {
  var hierarchyElement   = $('hierarchy');
  var breadcrumbsElement = $('breadcrumbs');
  var editParentTrigger  = $('editParentTrigger');
  var parentInputSection = $('parentinput');
  var parentInputField   = $('xwikidocparentinput');
  var titleInputField    = $('xwikidoctitleinput');

  /** Hides the parent input field when focusing out of the parent field. */
  function hideParentSection(event) {
    if (event) {
      event.stop();
    }
    parentInputSection.removeClassName('active');
    editParentTrigger.addClassName('edit-parent');
    editParentTrigger.removeClassName('hide-edit-parent');
  }
  /** Displays the parent input field when clicking on the "Edit parent" button. */
  function showParentSection(event) {
    if (event) {
      event.stop();
    }
    parentInputSection.addClassName('active');
    parentInputField.focus();
    editParentTrigger.removeClassName('edit-parent');
    editParentTrigger.addClassName('hide-edit-parent');
  }
  /** Toggles the visibility of the parent input field. */
  function toggleParentSectionVisibility (event) {
    event.stop();
    event.element().blur();
    if (editParentTrigger.hasClassName('edit-parent')) {
      showParentSection();
    } else {
      hideParentSection();
    }
  }

  if ($('hideEditParentTrigger')) {
    $('hideEditParentTrigger').style.display = 'none';
  }
  if (editParentTrigger) {
    editParentTrigger.observe('click', toggleParentSectionVisibility);
  }
  if (parentInputField) {
    if (hierarchyElement || breadcrumbsElement) {
      ['blur', 'change', 'xwiki:suggest:selected'].each(function(monitoredEvent) {
        parentInputField.observe(monitoredEvent, function () {
          var parameters = {
            xpage: 'xpart',
            vm: (hierarchyElement ? 'hierarchy.vm' : 'space.vm'),
            parent : parentInputField.value
          };
          if (titleInputField) {
            parameters.title = titleInputField.value;
          }
          new Ajax.Request(XWiki.currentDocument.getURL('edit'), {
            parameters: parameters,
            onSuccess : function(response) {
              if (hierarchyElement) {
                hierarchyElement.replace(response.responseText);
                hierarchyElement = $('hierarchy');
              } else {
                var tmp = new Element('div');
                tmp.update(response.responseText);
                breadcrumbsElement.replace(tmp.down('[id=breadcrumbs]'));
                breadcrumbsElement = $('breadcrumbs');
              }
            }
          });
        });
      });
    }
    $('body').observe('click', function (event) {
      if (!event.element().descendantOf(parentInputSection) && event.element() != parentInputSection && event.element() != editParentTrigger) {
        hideParentSection();
      }
    })
  }
});

/*
 * JS improvement for keeping the content menu visible on the screen when scrolling down.
 */
document.observe("xwiki:dom:loaded", function() {
  var menu = $('contentmenu') || $('editmenu'); // Both for view and edit
  var content = $('mainContentArea') || $('mainEditArea'); // Both for view and edit
  if (menu && content) {
    createGhost(menu);
    // Resize the fixed menu when the window width changes
    Event.observe(window, 'resize', function() {
      if (menu.style.position == 'fixed') {
        menu.style.width = content.getWidth() + 'px';
        if (typeof(menu.__fm_extra) != 'undefined') {
          if (menu.__fm_extra.getStyle('padding-left').replace(/[^a-z]/g, '') == 'px') {
            var boxExtra = menu.__fm_extra.getStyle('border-left-width').replace(/[^0-9.]/g, '') - 0;
            boxExtra += menu.__fm_extra.getStyle('padding-left').replace(/[^0-9.]/g, '') - 0;
            boxExtra += menu.__fm_extra.getStyle('padding-right').replace(/[^0-9.]/g, '') - 0;
            boxExtra += menu.__fm_extra.getStyle('border-right-width').replace(/[^0-9.]/g, '') - 0;
          } else {
            boxExtra = 50; // magic number 50 = standard left+right padding
          }
          menu.__fm_extra.style.width = (content.getWidth() - boxExtra) + 'px';
        }
      }
    });
    if (!browser.isIE6x) { // IE6 is too dumb to be supported
      Event.observe(window, 'scroll', handleScroll);
      // Make sure the annotations settings panel shows up in the right place
      document.observe('xwiki:annotations:settings:loaded', handleScroll);
    }
  }

  /**
   * Ensures that the content menu is always visible when scrolling down.
   */
  function handleScroll() {
    var menuExtras = $$('.annotationsettings');
    var extraHeight = 0;
    if (menuExtras && menuExtras.size() > 0) {
      menu.__fm_extra = menuExtras[0];
      createGhost(menu.__fm_extra);
      extraHeight = menu.__fm_extra.getHeight();
    }
    var menuHeight = menu.getHeight();
    var menuMinTop = content.cumulativeOffset().top - extraHeight;
    if (document.viewport.getScrollOffsets().top >= menuMinTop) {
      var menuWidth = content.getWidth();
      var menuLeft = content.cumulativeOffset().left;
      makeFixed(menu, 0, menuLeft, menuWidth);
      if (menu.__fm_extra) {
        makeFixed(menu.__fm_extra, menuHeight, menuLeft, (menuWidth -
          menu.__fm_extra.getStyle('border-left-width').replace(/[^0-9]/g,'') -
          menu.__fm_extra.getStyle('border-right-width').replace(/[^0-9]/g,'') -
          menu.__fm_extra.getStyle('padding-right').replace(/[^0-9]/g,'') -
          menu.__fm_extra.getStyle('padding-left').replace(/[^0-9]/g,'')));
      }
    } else {
      makeScrollable(menu);
      makeScrollable(menu.__fm_extra);
    }
  }

  /**
   * Creates a clone of the provided element, which has the same size and position.
   * This clone prevents layout changes when moving the element outside its parent.
   * The clone will be stored in the __fm_ghost property of the element and is inserted
   * after the element in the DOM. The clone is not visible initially.
   *
   * @param element the element whose position and dimesions should be cloned
   */
  function createGhost(element) {
    if (typeof(element.__fm_ghost) == 'undefined') {
      element.__fm_ghost = new Element('div');
      element.__fm_ghost.hide();
      element.insert({'after' : element.__fm_ghost});
    }
    element.__fm_ghost.clonePosition(element, {setWidth : false});
  }
  /**
   * Pins the provided element at a certain position inside the window. The element's clone is made
   * visible to prevent layout changes.
   *
   * @see #createGhost
   */
  function makeFixed(element, top, left, width) {
    if (element) {
      element.addClassName('floating-menu');
      element.style.position = 'fixed';
      element.style.top = top + 'px';
      element.style.left = left + 'px';
      element.style.width = width + 'px';
      element.__fm_ghost.show();
    }
  }
  /**
   * Restores the provided element to its original position in the document.
   */
  function makeScrollable(element) {
    if (element) {
      element.removeClassName('floating-menu');
      element.style.position = '';
      element.style.top = '';
      element.style.left = '';
      element.style.width = '';
      element.__fm_ghost.hide();
    }
  }
});
