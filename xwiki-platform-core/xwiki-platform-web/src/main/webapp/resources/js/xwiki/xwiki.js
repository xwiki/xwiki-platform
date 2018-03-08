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
    $(content || 'body').select(".xwikirenderingerror").each(function(error) {
        if(error.next().innerHTML !== "" && error.next().hasClassName("xwikirenderingerrordescription")) {
            error.style.cursor="pointer";
            error.title = "$escapetool.javascript($services.localization.render('platform.core.rendering.error.readTechnicalInformation'))";
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
      var anchors = $(content || 'body').select("a[rel]");
      for (var i = 0; i < anchors.length; i++) {
          var anchor = anchors[i];
          if (anchor.up('.xRichTextEditor')) {
              // Do not touch links inside a WYSIWYG editor since, if the content is saved, our modification would be saved
              // with it and we do not want to alter the content.
              // Note: The WYSIWYG editor is currently in a frame, so this code would not reach it anyway. This is more of
              // a precaution for the future, in case that changes.
              continue;
          }
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
              // If the link is opened in a new window, we are vulnerable to a phishing attack
              // see https://mathiasbynens.github.io/rel-noopener/ or https://dev.to/phishing
              // To avoid that, we just need to add "noopener" and "noreferrer" in the "rel" attribute of the link.
              // (_self, _parent and _top are the only values that are not concerned by this security issue)
              if (anchor.target && anchor.target != "_self" && anchor.target != "_parent" && anchor.target != "_top") {
                  var contains = function (array, value) {
                    for (var i = 0; i < array.length; ++i) {
                      if (array[i] == value) {
                        return true;
                      }
                    }
                    return false;
                  }
                  if (!contains(values, "noopener")) {
                    values.push("noopener");
                  }
                  if (!contains(values, "noreferrer")) {
                    values.push("noreferrer");
                  }
                  anchor.setAttribute("rel", values.join(" "));
              }
          }
      }
  },

  /**
   * Insert a link for editing sections.
   */
  insertSectionEditLinks: function(container) {
      // Insert links only if enabled, in view mode and for documents not in xwiki/1.0 syntax
      if ( $xwiki.hasSectionEdit() && XWiki.docsyntax != "xwiki/1.0" && XWiki.contextaction == "view" && XWiki.hasEdit) {

          // Section count starts at one, not zero.
          var sectioncount = 1;

          container = $(container || 'body');
          container = container.id == 'xwikicontent' ? container : container.down('#xwikicontent');
          if (!container) {
            return;
          }
          // We can't use element.select() since it does not keep the order of the elements in the flow.
          var nodes = container.childNodes;

          // Only allow section editing for the specified depth level (2 by default)
          var headerPattern = new RegExp("H[1-" + $xwiki.getSectionEditingDepth() + "]");

          // For all non-generated headers, add a SPAN and A element in order to be able to edit the section.
          for (var i = 0; i < nodes.length; i++) {

              var node = $(nodes[i]);

              if (headerPattern.test(node.nodeName) && node.className.include("wikigeneratedheader") == false) {
                  var editspan = document.createElement("SPAN");
                  editspan.className = "edit_section";
                  // Hide the section editing link if the section heading is hidden.
                  (!node.visible() || node.hasClassName('hidden')) && editspan.hide();

                  // If there's no Syntax Renderer for the current document's syntax then make sure the section edit
                  // button will be displayed inactive since editing a section requires a Syntax Renderer.
                  var editlink;
                  if (!XWiki.hasRenderer) {
                      editlink = document.createElement("SPAN");
                      editspan.className = editspan.className + " disabled";
                      editlink.title = "$escapetool.javascript($services.localization.render('platform.core.rendering.noRendererForSectionEdit'))";
                  } else {
                      editlink = document.createElement("A");
                      editlink.href = window.docediturl + "?section=" + sectioncount;
                      editlink.style.textDecoration = "none";
                      editlink.innerHTML = "$escapetool.javascript($services.localization.render('edit'))";
                  }

                  editspan.appendChild(editlink);
                  node.insert( { 'after': editspan } );
                  sectioncount++;
              }
          }
      }
  },

  /**
   * Display a modal box allowing to create the new document from a template when clicking on broken links.
   *
   * @param container where to look for broken links
   */
  insertCreatePageFromTemplateModalBoxes: function(container) {
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

          var spans = $(container || 'body').select("span.wikicreatelink");
          for (var i = 0; i < spans.length; i++) {
              spans[i].down('a').observe('click', function(event) {
                  // Remove the fragment identifier from the link URL.
                  new Ajax.Request(event.findElement('a').href.replace(/#.*$/, ''), {
                      method:'get',
                      parameters: {
                        xpage: 'createinline',
                        ajax: 1
                      },
                      onSuccess: function(transport) {
                          var redirect = transport.getHeader('redirect');
                          if (redirect) {
                            window.location = redirect;
                          } else {
                            // The create action actually loads some JS and CSS. This modal box needs them too, but we
                            // load them on demand.
                            // We display an notification while the browser fetch the resources/
                            var notification = new XWiki.widgets.Notification("$escapetool.javascript($services.localization.render('core.create.popup.loading'))", 'inprogress');
                            // Add the CSS
                            var newStyle = new Element('link', {'rel': 'stylesheet', 'type':'text/css', 
                              'href': '$xwiki.getSkinFile("uicomponents/widgets/select/select.css", true)'});
                            $(document.head).insert(newStyle);
                            // Add the JS
                            require(["$xwiki.getSkinFile('js/xwiki/create.js', true)",
                                     "$xwiki.getSkinFile('uicomponents/widgets/select/select.js', true)"],
                                    function($) {
                                       // We are sure that the JS have been loaded, so we finally display
                                       // the create popup
                                       new XWiki.widgets.CreatePagePopup({content: transport.responseText});
                                       notification.hide();
                                    });
                          }
                      },
                      onFailure: function() {
                        new XWiki.widgets.Notification("$escapetool.javascript($services.localization.render('core.create.ajax.error'))", 'error', {inactive: true}).show();
                      }
                  });
                  event.stop();
              });
          }
      }
  },

  /**
   * Watchlist methods.
   * 
   * @deprecated Since XWiki 7.4, the watchlist UI is implemented in a UI extension. This code is still there to not 
   * break the retro-compatibility but we can consider removing it.
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
    initialize: function(container) {
        container = $(container || 'body');
        for (button in XWiki.watchlist.actionsMap) {
          var element = container.down('#' + button);
          if (element) {
            var self = this;

            if (element.nodeName != 'A') {
              element = $(button).down('A');
            }
            
            if (!element) {
              // This is supposed to happen every time since the watchlist icons are implemented in the notifications
              // menu. The watchlist icons are now implemented as a UI extension, and the inputs are handled with a 
              // custom solution (bootstrap-switch).
              // For these reasons, we stop the initialization here.
              // We keep this function for old skins (like Colibri), that still have the old-fashioned watchlist icons.
              return;
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
        document.cookie = name+"="+encodeURIComponent(value)+expires+"; path=/";
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
                return decodeURIComponent(c.substring(nameEQ.length,c.length));
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

  registerPanelToggle: function(container) {
    $(container || 'body').select('.panel .xwikipaneltitle').each(function(item) {
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

      // Make sure we re-add the behaviour whenever a part of the DOM is updated.
      document.observe('xwiki:dom:updated', function(event) {
        event.memo.elements.each(this._addBehaviour.bind(this));
      }.bindAsEventListener(this));
      // Add behaviour to the entire DOM.
      this._addBehaviour();

      this.domIsLoaded = true;
      document.fire("xwiki:dom:loaded");
    }
  },

  /**
   * Enhances some of the common page elements with JavaScript behaviour.
   */
  _addBehaviour: function(container) {
    container = container || $('body');

    this.makeRenderingErrorsExpandable(container);
    this.fixLinksTargetAttribute(container);
    this.insertSectionEditLinks(container);
    this.insertCreatePageFromTemplateModalBoxes(container);
    this.watchlist.initialize(container);
    this.registerPanelToggle(container);
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
 * Manage the keyboards shortcuts.
 * This object interfaces with the bundled Keypress JS library.
 */
shortcut = new Object({

    /**
     * @returns {Array} of registered shortcuts
     */
    all_shortcuts: function() {
        var shortcuts = [];

        Object.values(this._listeners).forEach(function(group, index) {
            Object.values(group).forEach(function(listener, index2) {
                shortcuts = shortcuts.concat(listener.get_registered_combos());
            })
        });

        return shortcuts;
    },

    /**
     * Type of shortcut that can be registered.
     */
    type: new Object({
        SIMPLE: 'simple',
        COUNTING: 'counting',
        SEQUENCE: 'sequence'
    }),

    /**
     * Add a new shortcut.
     *
     * The opt parameter should be a map of optional parameters used while registering the shortcut.
     * <ul>
     *     <li>target: A DOM element in which the shortcut should be listened (defaults to the whole document)</li>
     *     <li>disable_in_input: If true, the shortcut will not be listened when an input
     *     or textarea field is selected (default: false)</li>
     *     <li>type: The type (shortcut.type) that should be used (default: SIMPLE)</li>
     * </ul>
     *
     * @param shortcut_combination {string} the shortcut that should trigger the callback
     * @param callback the function triggered by the shortcut
     * @param opt optional map defining parameters for registering the shortcut
     */
    add: function(shortcut_combination, callback, opt) {
        // Require Keypress JS to be fully loaded before registering the shortcut.
        require(["$services.webjars.url('org.webjars:Keypress', 'keypress.js')", 'jquery'], function(keypress) {

            // If no options are defined, create a blank map
            opt = (opt) ? opt : {};

            var shortcut_descriptor = {
                "keys": shortcut._format_shortcut_combination(shortcut_combination),
                "is_solitary": true
            };

            // CSS selector that should be used by the listener holding the shortcut
            var listener_target = ('target' in opt) ? opt['target'] : document;

            // Get the group in which we should store the listener
            var listener_group = ('disable_in_input' in opt && opt['disable_in_input'])
                ? shortcut._listeners.disabled_in_inputs : shortcut._listeners.enabled_in_inputs;

            var listener = shortcut._get_or_create_listener(listener_target, listener_group, keypress);

            if ('type' in opt && Object.values(shortcut.type).indexOf(opt['type']) > -1) {
                switch (opt['type']) {
                    case shortcut.type.COUNTING:
                        shortcut_descriptor["is_counting"] = true;
                        shortcut_descriptor["is_unordered"] = false;
                        break;
                    case shortcut.type.SEQUENCE:
                        shortcut_descriptor["is_sequence"] = true;
                        shortcut_descriptor["is_exclusive"] = true;
                        break;
                }

                shortcut_descriptor["on_keydown"] = shortcut._wrap_shortcut_call(callback, opt['type']);
                listener.register_combo(shortcut_descriptor);
            } else {
                if ('type' in opt) {
                    console.warn('The parameter [' + opt['type'] + '] for the shortcut [' + combination
                        + '] type deprecated.');
                }

                shortcut_descriptor["on_keydown"] = shortcut._wrap_shortcut_call(callback, shortcut.type.SIMPLE);
                listener.register_combo(shortcut_descriptor);
            }

            // Log deprecation warnings for opt parameters
            var allowedOptParameters = ['target', 'disable_in_input', 'type'];
            Object.keys(opt).forEach(function (key) {
                if (allowedOptParameters.indexOf(key) === -1) {
                    console.warn('The parameter [' + key + '] for the shortcut [' + combination + '] is deprecated.');
                }
            });
        });
    },

    /**
     * Remove the given shortcut combination from every known listener.
     *
     * @param shortcut_combination a string representing the shortcut combination to remove
     */
    remove: function(shortcut_combination) {
        var combination = shortcut._format_shortcut_combination(shortcut_combination);

        Object.values(this._listeners).forEach(function(group, index) {
            Object.values(group).forEach(function(listener, index2) {
                listener.unregister_combo(combination);
            });
        });
    },

    /**
     * Map of every Keypress JS shortcut listeners.
     *
     * @private
     */
    _listeners: {
        disabled_in_inputs: {},
        enabled_in_inputs: {}
    },

    /**
     * Determine if a sequence shortcut is being triggered.
     *
     * @private
     */
    _is_sequence_shortcut_triggered: false,

    /**
     * When a sequence shortcut is called and when a combo shortcut is defined with a single key as the last key of
     * the previous sequence, the two shortcuts are triggered.
     *
     * This method wraps the shortcut callback in order to prevent the combo shortcut from being triggered.
     * Please note that, in order to do so, we are assuming that no other combo shortcut should be triggered during
     * the next 500ms after having triggered the sequence shortcut.
     *
     * @param callback the callback used for the shortcut
     * @param shortcut_type the shortcut type
     * @retun {*} The new callback to be used for defining the shortcut against Keypress JS libray
     * @private
     */
    _wrap_shortcut_call: function(callback, shortcut_type) {
        if (shortcut_type === shortcut.type.SEQUENCE) {
            return function(args) {
                shortcut._is_sequence_shortcut_triggered = true;
                setTimeout(function(){ shortcut._is_sequence_shortcut_triggered = false; }, 500);
                callback.apply(this, args);
            };
        } else {
            return function(args) {
                if (!shortcut._is_sequence_shortcut_triggered) {
                    callback.apply(this, args);
                }
            };
        }
    },

    /**
     * Check if a Keypress JS listener exists for the given target in the given group, if not, create it.
     *
     * @param target the DOM element that should be targeted by the listener
     * @param group the group (either disabled_in_inputs or enabled_in_inputs) in which the listener belongs
     * @param keypress a reference to the Keypress JS API
     * @returns {*} The Keypress JS listener
     * @private
     */
    _get_or_create_listener: function(target, group, keypress) {
        if (target in group) {
            return group[target];
        } else {
            var newListener = new keypress.Listener(target);

            if (group === this._listeners.disabled_in_inputs) {
                // Disable the created listener when focus goes on an input or textarea field
                jQuery(document)
                    .on('focus', 'input, textarea',
                        function() { newListener.stop_listening(); })
                    .on('blur', 'input, textarea',
                        function() { newListener.listen(); });
            }

            group[target] = newListener;
            return newListener;
        }
    },

    /**
     * Format the given combination to match the format accepted by Keypress JS while allowing backward compatibility
     * with the previous OpenJS shortcut syntax.
     *
     * @returns {string} the formatted combination
     * @private
     */
    _format_shortcut_combination: function(combination) {
        return combination.toLowerCase().replace(/\+/g, ' ');
    }
});

/**
 * Browser Detect
 * Version: 2.1.6 (modified to add support for IE11+ detection)
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

    var trident = /trident\/(\d+)/.exec(ua);
    this.isIE11up = trident && parseInt(trident[1]) >= 7;
}
var browser = new BrowserDetect();

/**
 * XWiki Model access APIs.
 */
XWiki.Document = Class.create({
  /**
   * Constructor. All parameters are optional, and default to the current document location.
   *
   * Note: Starting with XWiki 7.2M1 the space field is deprecated and holds a space reference (i.e. one or
   * several spaces separated by dots, e.g. "space1.space2").
   *
   * The constructor which takes the 3 arguments is mostly for retro-compatibility. The best practice is to construct a 
   * new instance with a document reference.
   * e.g. new XWiki.Document(new XWiki.DocumentReference('xwiki', ['Space1', 'Space2'], 'Page'));
   */
  initialize : function(pageNameOrReference, space, wiki) {
    if (typeof pageNameOrReference === 'string' || typeof space === 'string' || typeof wiki === 'string') {
      this.initializeFromStrings(pageNameOrReference, space, wiki);
    } else {
      // The first argument is a document reference (or it is null).
      // We ignore the other arguments since all the needed information is on the reference
      this.initializeFromReference(pageNameOrReference);
    }
  },
  
  /**
   * Initialize the document with a document reference.
   */
  initializeFromReference : function(reference) {
    this.documentReference  = reference || XWiki.Document.currentDocumentReference;
    var wikiReference       = this.documentReference.extractReference(XWiki.EntityType.WIKI);
    var spaceReference      = this.documentReference.extractReference(XWiki.EntityType.SPACE);
    var localSpaceReference = spaceReference.relativeTo(wikiReference);
    this.page   = this.documentReference.extractReferenceValue(XWiki.EntityType.DOCUMENT);
    this.space  = XWiki.Model.serialize(localSpaceReference);
    this.spaces = localSpaceReference.getReversedReferenceChain().map(function(spaceRef) {
      return spaceRef.getName();
    });
    this.wiki   = this.documentReference.extractReferenceValue(XWiki.EntityType.WIKI);
  },
  
  /**
   * Initialize the document with some strings
   */
  initializeFromStrings : function(page, space, wiki) {
    this.page = page || XWiki.Document.currentPage;
    // Note: Starting with XWiki 7.2M1 the this.space variable is deprecated and holds a space reference (i.e. one or
    // several spaces separated by dots, e.g. "space1.space2").
    // It is recommended to use the new this.spaces variable which is an array of space Strings, representing the spaces
    // in which the document is located.
    this.space = space || XWiki.Document.currentSpace;
    var localSpaceReference = XWiki.Model.resolve(this.space, XWiki.EntityType.SPACE);
    this.spaces = localSpaceReference.getReversedReferenceChain().map(function(spaceRef) {
      return spaceRef.getName();
    });
    this.wiki = wiki || XWiki.Document.currentWiki;
    this.documentReference = new XWiki.DocumentReference(this.wiki, this.spaces, this.page);
  },
  
  /**
   * Gets an URL pointing to this document.
   */
  getURL : function(action, queryString, fragment) {
    action = action || 'view';
    var url = action == 'view' ? XWiki.Document.ViewURLTemplate : XWiki.Document.URLTemplate;
    var spaceSegments = this.spaces.map(function(spaceSegment) {
      return encodeURIComponent(spaceSegment);
    }).join('/');
    url = url.replace("__space__", spaceSegments);
    url = url.replace("__page__", (this.page == 'WebHome') ? '' : encodeURIComponent(this.page));
    url = url.replace("__action__/", encodeURIComponent(action) + "/");
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
    var spaceSegments = this.spaces.map(function(spaceSegment) {
      return encodeURIComponent(spaceSegment);
    }).join('/spaces/');
    url = url.replace("__space__", spaceSegments);
    url = url.replace("__page__", this.page);
    if (entity) {
      url += "/" + entity;
    }
    if (queryString) {
      url += '?' + queryString;
    }
    return url;
  },
  getDocumentReference : function() {
    return this.documentReference;
  }
});
/* Initialize the document URL factory, and create XWiki.currentDocument.
TODO: use the new API to get the document meta data (see: http://jira.xwiki.org/browse/XWIKI-11225).
Currently not done because the new API is asynchronous meanwhile this script must be loaded first/ */
var htmlElement = $(document.documentElement);
if (htmlElement.readAttribute('data-xwiki-reference') != null) {
  // Case 1: meta information are stored in the data- attributes of the <html> tag
  // (since Flamingo)
  var documentReference = XWiki.Model.resolve(
    htmlElement.readAttribute('data-xwiki-reference'), XWiki.EntityType.DOCUMENT);
  var spaceReference    = documentReference.extractReference(XWiki.EntityType.SPACE);
  var wikiReference     = documentReference.extractReference(XWiki.EntityType.WIKI);
  XWiki.Document.currentDocumentReference = documentReference;
  XWiki.Document.currentPage              = XWiki.Document.currentDocumentReference.getName();
  XWiki.Document.currentSpace             = XWiki.Model.serialize(spaceReference.relativeTo(wikiReference));
  XWiki.Document.currentWiki              = wikiReference.getName();
  XWiki.currentDocument = new XWiki.Document();
} else {
  // Case 2: meta information are stored in deprecated <meta> tags
  // (in colibri)
  XWiki.Document.currentWiki  = XWiki.currentWiki  || "xwiki";
  XWiki.Document.currentSpace = XWiki.currentSpace || "Main";
  XWiki.Document.currentPage  = XWiki.currentPage  || "WebHome";
  if ($$("meta[name=wiki]").length > 0) {
    XWiki.Document.currentWiki = $$("meta[name=wiki]")[0].content;
  }
  if ($$("meta[name=space]").length > 0) {
    XWiki.Document.currentSpace = $$("meta[name=space]")[0].content;
  }
  if ($$("meta[name=page]").length > 0) {
    XWiki.Document.currentPage = $$("meta[name=page]")[0].content;
  }
  XWiki.currentDocument = new XWiki.Document(XWiki.Document.currentPage, XWiki.Document.currentSpace,
    XWiki.Document.currentWiki);
  XWiki.Document.currentDocumentReference = XWiki.currentDocument.getDocumentReference();
}
XWiki.Document.URLTemplate = "$xwiki.getURL('__space__.__page__', '__action__')";
// We need a separate template for the view URL because the action name is missing when Short URLs are enabled.
XWiki.Document.ViewURLTemplate = "$xwiki.getURL('__space__.__page__')";
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
        if (item.hasClassName('useTitleAsTip')) {
          // The place-holder text is different than the initial (default) input value.
          item.placeholder = item.title;
        } else {
          // Use the initial (default) input value as place-holder.
          item.placeholder = item.defaultValue;
          item.value = '';
        }
      }
    }
  } else {
    // For browsers that don't support the 'placeholder' attribute, we simulate it with 'focus' and 'blur' event handlers.
    var onFocus = function() {
      var empty = this.hasClassName('empty');
      this.removeClassName('empty');
      if (empty) {
        this.value = '';
      } else {
        this.select();
      }
    }
    var onBlur = function() {
      if (this.value == '') {
        this.value = this.defaultValue;
        this.addClassName('empty');
      }
    }
    placeholderPolyfill = function(event) {
      var item = event.memo.element;
      // Backup the initial input value because IE resets it when the default value is set.
      var initialValue = item.value;
      if (item.readAttribute('placeholder')) {
        item.defaultValue = item.readAttribute('placeholder');
      } else if (item.hasClassName('useTitleAsTip')) {
        item.defaultValue = item.title;
      }
      // Restore the initial input value;
      item.value = initialValue;
      if (item.value == item.defaultValue) {
        // The 'empty' CSS class has two functions:
        // * display the placeholder value with a different color
        // * distinguish between the case when the user has left the input empty and the case when he typed exactly the
        //   default value (which should be valid).
        item.addClassName('empty');
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
            script: XWiki.Document.getRestSearchURL("scope=name&number=10&"),
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
            script: XWiki.Document.getRestSearchURL("scope=spaces&number=10&"),
            varname: "q",
            icon: "$xwiki.getSkinFile('icons/silk/folder.png')",
            noresults: "Space not found",
            json: true,
            resultsParameter : "searchResults",
            resultId : "id",
            resultValue : "space",
            resultInfo : "space"
        },
        "propertyValues" : {
            script: function(input) {
              return [
                '$request.contextPath', 'rest',
                'wikis', encodeURIComponent(XWiki.currentWiki),
                'classes', encodeURIComponent(input.getAttribute('data-className')),
                'properties', encodeURIComponent(input.getAttribute('data-propertyName')),
                'values'
              ].join('/');
            },
            varname: "fp",
            noresults: "Value not found",
            json: true,
            resultsParameter : "propertyValues",
            resultId : "value",
            resultValue : "value",
            resultInfo : "metaData.label",
            minchars: 0
        }
    };
    var addSuggests = function(elements) {
      if (typeof(XWiki.widgets.Suggest) != "undefined") {
        var keys = Object.keys(suggestionsMapping);
        for (var i=0;i<keys.length;i++) {
          var selector = 'input.suggest' + keys[i].substr(0, 1).toUpperCase() + keys[i].substr(1);
          elements.each(function(element) {$(element).select(selector).each(function(item) {
            if (!item.hasClassName('initialized')) {
              var options = {
                timeout : 30000
              };
              Object.extend(options, suggestionsMapping[keys[i]]);
              if (typeof options.script === 'function') {
                options.script = options.script(item);
              }
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
if ($services.parentchild.isParentChildMechanismEnabled()) {
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
      if (event.element().descendantOf && !event.element().descendantOf(parentInputSection) && event.element() != parentInputSection && event.element() != editParentTrigger) {
        hideParentSection();
      }
    })
  }
});
}

/*
 * JS improvement for keeping the content menu visible on the screen when scrolling down.
 */
document.observe("xwiki:dom:loaded", function() {
  // Do it only for colibri
  if (!$("body").hasClassName("skin-colibri")) {
    return;
  }
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
