var XWiki = (function(XWiki) {
// Start XWiki augmentation.
var editors = XWiki.editors = XWiki.editors || {};
editors.XDataEditors = Class.create({
  initialize : function() {
    this.classDocument = $$('meta[name=document]')[0].content;
    this.classDocumentName = $$('meta[name=page]')[0].content;
    this.classDocumentSpace = $$('meta[name=space]')[0].content;
    // URL template generated from velocity
    this.urlEditTemplate = "$xwiki.getURL('__space__.__page__', 'edit')".replace('__space__', this.classDocumentSpace).replace('__page__', this.classDocumentName);
    this.urlAddPropertyTemplate = "$xwiki.getURL('__space__.__page__', 'propadd')".replace('__space__', this.classDocumentSpace).replace('__page__', this.classDocumentName);

    $$('.xclass').each(function(item) {
      this.enhanceClassUX(item);
    }.bind(this));
    this.ajaxObjectAdd($('add_xobject'));
    this.ajaxPropertyAdd();
    this.makeSortable($('xwikiclassproperties'));
    this.classSwitcherBehavior();
    this.ajaxRemoveDeprecatedProperties($("body"), ".syncAllProperties");
  },
  enhanceClassUX : function(xclass) {
    this.expandCollapseClass(xclass);
    xclass.select('.xobject').each(function(item) {
      this.enhanceObjectUX(item);
    }.bind(this));
    xclass.select('.xproperty').each(function(item) {
      this.expandCollapseMetaProperty(item);
      this.ajaxPropertyDeletion(item);
      this.makeDisableVisible(item);
    }.bind(this));
    this.ajaxObjectAdd(xclass);
  },
  enhanceObjectUX : function(object) {
    this.ajaxObjectDeletion(object);
    this.editButtonBehavior(object);
    this.expandCollapseObject(object);
    this.ajaxRemoveDeprecatedProperties(object, ".syncProperties");
  },
  // -----------------------------------------------
  /* AJAX object add */
  ajaxObjectAdd : function(element) {
    if (!element) {
      return;
    }
    element.select('.xobject-add-control').each(function(item) {
      item.observe('click', function(event) {
        item.blur();
        event.stop();
        var url, validClassName, classNameVal;
        if (item.href) {
          url = item.href.replace(/[?&]xredirect=[^&]*/, '');
          validClassName = true;
        } else {
          var classNameElt = element.down('select');
          if (classNameElt && classNameElt.selectedIndex >= 0) {
            classNameVal = classNameElt.options[classNameElt.selectedIndex].value;
          }
          validClassName = classNameVal && classNameVal != '-';
          url = this.urlEditTemplate + "?xpage=editobject&xaction=addObject&className=" + classNameVal;
        }
        if (!item.disabled && validClassName) {
          new Ajax.Request(
            /* Ajax request URL */
            url,
            /* Ajax request parameters */
            {
              onCreate : function() {
                item.disabled = true;
                item.notification = new XWiki.widgets.Notification("$msg.get('core.editors.object.add.inProgress')", "inprogress");
              },
              onSuccess : function(response) {
                var activator = item.up('.add_xobject');
                if(activator) {
                  var insertedElement;
                  if (activator.up('.xclass')) {
                    activator.insert({before: response.responseText});
                    insertedElement = activator.previous();
                  } else {
                    activator.insert({after: response.responseText});
                    insertedElement = activator.next();
                  }
                  if (insertedElement) {
                    var insertedObject;
                    if(insertedElement.hasClassName('xclass')) {
                      this.enhanceClassUX(insertedElement);
                      insertedObject = insertedElement.down('.xobject');
                    } else if(insertedElement.hasClassName('xobject')) {
                      this.enhanceObjectUX(insertedElement);
                      var classId = insertedElement.id.replace(/^xobject_/, "xclass_").replace(/_\d+$/, "");
                      var xclass = $(classId);
                      if(xclass) {
                        xclass.down('.add_xobject').insert({before: insertedElement});
                        this.updateXObjectCount(xclass);
                      }
                      insertedObject = insertedElement;
                    }
                    // Expand the newly inserted object, since the user will probably want to edit it once it was added
                    insertedObject.removeClassName('collapsed');
                    insertedObject.up('.xclass').removeClassName('collapsed');
                  }
                }
                item.notification.replace(new XWiki.widgets.Notification("$msg.get('core.editors.object.add.done')", "done"));
              }.bind(this),
              onFailure : function(response) {
                var failureReason = response.statusText;
                if (response.statusText == '' /* No response */ || response.status == 12031 /* In IE */) {
                  failureReason = 'Server not responding';
                }
                item.notification.replace(new XWiki.widgets.Notification("$msg.get('core.editors.object.add.failed')" + failureReason, "error"));
              },
              onComplete : function() {
                item.disabled = false;
              },
              // IE converts 204 status code into 1223...
              on1223 : function(response) {
                response.request.options.onSuccess(response);
              },
              // 0 is returned for network failures, except on IE where a strange large number (12031) is returned.
              on0 : function(response) {
                response.request.options.onFailure(response);
              }
            }
          );
        }
      }.bindAsEventListener(this));
    }.bind(this));
  },
  // ------------------------------------
  // Ajax object deletion
  ajaxObjectDeletion : function(object) {
    var item = object.down('a.delete');
    item.observe('click', function(event) {
      item.blur();
      event.stop();
      if (!item.disabled) {
              new XWiki.widgets.ConfirmedAjaxRequest(
                /* Ajax request URL */
                item.href,
                /* Ajax request parameters */
                {
                  onCreate : function() {
                    item.disabled = true;
                  },
                  onSuccess : function() {
                    var xobjectElement = item.up('.xobject');
                    var xclassElement = xobjectElement.up('.xclass');
                    xobjectElement.remove();
                    this.updateXObjectCount(xclassElement);
                  }.bind(this),
                  onComplete : function() {
                    item.disabled = false;
                  }
                },
                /* Interaction parameters */
                {
                  confirmationText: "$msg.get('core.editors.object.delete.confirm')",
                  progressMessageText : "$msg.get('core.editors.object.delete.inProgress')",
                  successMessageText : "$msg.get('core.editors.object.delete.done')",
                  failureMessageText : "$msg.get('core.editors.object.delete.failed')"
                }
              );
      }
    }.bindAsEventListener(this));
  },
  // -----------------------------------------------
  /* AJAX removal of deprecated properties */
  ajaxRemoveDeprecatedProperties : function(container, triggerSelector) {
    container.select(triggerSelector).each(function(item) {
      item.observe("click", function(event) {
        item.blur();
        event.stop();
        if (!item.disabled) {
          new Ajax.Request(
            /* Ajax request URL */
            item.href,
            /* Ajax request parameters */
            {
              onCreate : function() {
                item.disabled = true;
                item.notification = new XWiki.widgets.Notification("$msg.get('core.editors.object.removeDeprecatedProperties.inProgress')", "inprogress");
              },
               onSuccess : function(response) {
                // Remove deprecated properties box
                container.select(".deprecatedProperties").invoke("remove");
                item.notification.replace(new XWiki.widgets.Notification("$msg.get('core.editors.object.removeDeprecatedProperties.done')", "done"));
              },
              onFailure : function(response) {
                var failureReason = response.statusText;
                if (response.statusText == '' /* No response */ || response.status == 12031 /* In IE */) {
                  failureReason = 'Server not responding';
                }
                item.notification.replace(new XWiki.widgets.Notification("$msg.get('core.editors.object.removeDeprecatedProperties.failed')" + failureReason, "error"));
              },
              onComplete : function() {
                item.disabled = false;
              },
              // IE converts 204 status code into 1223...
              on1223 : function(response) {
                response.request.options.onSuccess(response);
              },
              // 0 is returned for network failures, except on IE where a strange large number (12031) is returned.
              on0 : function(response) {
                response.request.options.onFailure(response);
              }
            }
          );
        }
      });
    });
  },
  // -----------------------------------------------
  /* AJAX property add */
  ajaxPropertyAdd : function() {
    $$('input[name=action_propadd]').each(function(item){
      item._x_propnameElt = $('propname');
      item._x_proptypeElt = $('proptype');
      item._x_form_tokenElt = $('form_token');
      var token = item._x_form_tokenElt ? item._x_form_tokenElt.value : "";
      item.observe('click', function(event) {
        item.blur();
        event.stop();
        if (!item.disabled && item._x_propnameElt.value != '' && item._x_proptypeElt.selectedIndex >= 0) {
          var ref = this.urlAddPropertyTemplate + "?propname=" + item._x_propnameElt.value + "&proptype=" + item._x_proptypeElt.options[item._x_proptypeElt.selectedIndex].value + "&xredirect=" + encodeURIComponent(this.urlEditTemplate + "?xpage=editclass&xaction=displayProperty&propName=" + item._x_propnameElt.value) + "&form_token=" + item._x_form_tokenElt.value;
          new Ajax.Request(
            /* Ajax request URL */
            ref,
            /* Ajax request parameters */
            {
              onCreate : function() {
                item.disabled = true;
                item.notification = new XWiki.widgets.Notification("$msg.get('core.editors.class.addProperty.inProgress')", "inprogress");
              },
              onSuccess : function(response) {
                $('xclassContent').insert({bottom : response.responseText});
                var insertedPropertyElt = $('xclassContent').lastChild;
                // Expand the newly inserted property, since the user will probably want to edit it once it was added
                this.expandCollapseMetaProperty(insertedPropertyElt);
                // Make teh newly added property sortable
                this.makeSortable(insertedPropertyElt);
                this.ajaxPropertyDeletion(insertedPropertyElt);
                this.makeDisableVisible(insertedPropertyElt);
                item.notification.replace(new XWiki.widgets.Notification("$msg.get('core.editors.class.addProperty.done')", "done"));
              }.bind(this),
              onFailure : function(response) {
                var failureReason = response.statusText;
                if (response.statusText == '' /* No response */ || response.status == 12031 /* In IE */) {
                  failureReason = 'Server not responding';
                }
                item.notification.replace(new XWiki.widgets.Notification("$msg.get('core.editors.class.addProperty.failed')" + failureReason, "error"));
              },
              onComplete : function() {
                item.disabled = false;
              },
              // IE converts 204 status code into 1223...
              on1223 : function(response) {
                response.request.options.onSuccess(response);
              },
              // 0 is returned for network failures, except on IE where a strange large number (12031) is returned.
              on0 : function(response) {
                response.request.options.onFailure(response);
              }
            }
          );
        }
      }.bindAsEventListener(this));
    }.bind(this));
  },
  // ------------------------------------
  // Ajax property deletion
  ajaxPropertyDeletion : function(property) {
    var item = property.down('a.delete');
    item.observe('click', function(event) {
      item.blur();
      event.stop();
      if (!item.disabled) {
        new XWiki.widgets.ConfirmedAjaxRequest(
          /* Ajax request URL */
          item.href,
          /* Ajax request parameters */
          {
            onCreate : function() {
              item.disabled = true;
            },
            onSuccess : function() {
              property.remove();
            },
            onComplete : function() {
              item.disabled = false;
            }
          },
          /* Interaction parameters */
          {
            confirmationText: "$msg.get('core.editors.class.deleteProperty.confirm')",
            progressMessageText : "$msg.get('core.editors.class.deleteProperty.inProgress')",
            successMessageText : "$msg.get('core.editors.class.deleteProperty.done')",
            failureMessageText : "$msg.get('core.editors.class.deleteProperty.failed')"
          }
        );
      }
    });
  },
  // ------------------------------------
  //
  makeDisableVisible : function(property) {
    property.down('.disabletool input').observe("click", function(event) {
      property.toggleClassName('disabled');
    })
  },
  // ------------------------------------
  // Edit button behavior
  // Prevent from collapsing the object subtree when clicking on edit
  editButtonBehavior : function(object) {
    var item = object.down('a.edit');
    if (!item) {
      return;
    }
    item.observe('click', function(event) {
      item.blur();
      event.stop();
      window.location = item.href;
    }.bindAsEventListener());
  },
  // Update the number of objects displayed in the class group title, when objects are added or deleted
  updateXObjectCount: function(xclass) {
    var xobjectCount = xclass.select('.xobject').size();
    if (xobjectCount == 0) {
      xclass.remove();
    } else {
      var xobjectCountElement = xclass.down('.xclass_xobject_nb');
      if (typeof(xobjectCountElement) != 'undefined') {
        xobjectCountElement.update('(' + xobjectCount + ')');
      }
    }
  },
  // ------------------------------------
  // Expand/collapse objects and object properties
  expandCollapseObject : function(object) {
    totalItems = $$('#xwikiobjects .xobject').size();
    object.addClassName('collapsable');
    if (totalItems > 1) {
      object.toggleClassName('collapsed');
    }
    var objectTitle = object.down('.xobject-title');
    objectTitle.observe('click', function(event) {
      objectTitle.up().toggleClassName('collapsed');
    }.bindAsEventListener());
    object.select('.xobject-content dt').each(function(item) {
      if(! item.down('input[type=checkbox]')) {
        item.addClassName('collapsable');
        var collapser = new Element('span', {'class' : 'collapser'});
        collapser.observe('click', function(event) {
          this.up('dt').next('dd').toggle();
          this.toggleClassName('collapsed');
        }.bindAsEventListener(collapser));
        item.insert({top: collapser});
      } else {
        item.addClassName('uncollapsable');
      }
    });
    object.select('.xobject-content dt label').each(function(item) {
      item.observe('click', function(event) {
        if(item.up('dt').down('span').hasClassName('collapsed')) {
          item.up('dt').next('dd').toggle();
          item.up('dt').down('span').toggleClassName('collapsed');
        }
      }.bindAsEventListener());
    });
  },
  // ------------------------------------
  //  Expand/collapse classes
  expandCollapseClass : function(xclass) {
    // Classes are expanded by default
    xclass.addClassName('collapsable');
    var xclassTitle = xclass.down('.xclass-title');
    xclassTitle.observe('click', function(event) {
      xclassTitle.up().toggleClassName('collapsed');
    }.bindAsEventListener());
  },
  // ------------------------------------
  // Class editor: expand-collapse meta properties
  expandCollapseMetaProperty : function(property) {
    property.addClassName('collapsable');
    property.addClassName('collapsed');
    var propertyTitle = property.down('.xproperty-title');
    propertyTitle.observe('click', function(event) {
      propertyTitle.up().toggleClassName('collapsed');
    }.bindAsEventListener());
    property.select('.xproperty-content dt').each(function(item) {
      if(! item.down('input[type=checkbox]')) {
        item.addClassName('collapsable');
        var collapser = new Element('span', {'class' : 'collapser'});
        collapser.observe('click', function(event) {
          this.up('dt').next('dd').toggle();
          this.toggleClassName('collapsed');
        }.bindAsEventListener(collapser));
        item.insert({top: collapser});
      } else {
        item.addClassName('uncollapsable');
      }
    });
    property.select('.xproperty-content dt label').each(function(item) {
      item.observe('click', function(event) {
        if(item.up('dt').down('span').hasClassName('collapsed')) {
          item.up('dt').next('dd').toggle();
          item.up('dt').down('span').toggleClassName('collapsed');
        }
      }.bindAsEventListener());
    });
  },
  //---------------------------------------------------
  // Class switcher: no submit
  classSwitcherBehavior : function() {
    var switcher = $$('#switch-xclass #classname');
    if(switcher.size() > 0) {
      switcher = switcher[0];
      switcher.observe('change', function() {
        var value = this.options[this.selectedIndex].value;
        if (value != '-') {
          new XWiki.widgets.ConfirmationBox(
            {
              onYes : function() {
                  document.fire('xwiki:actions:save', {'continue' : true, 'form' : $('propupdate')});
                  document.observe('xwiki:document:saved', function () {
                    window.self.location = value;
                  });
              },
              onNo : function() {
                  window.self.location = value;
              }
            },
            /* Interaction parameters */
            { confirmationText: "$msg.get('core.editors.class.switchClass.confirm')" }
          );
        }
      }.bindAsEventListener(switcher));
      switcher.up('form').down("input[type='submit']").hide();
      switcher.up('form').down(".warningmessage").hide();
    }
  },
  //---------------------------------------------------
  /* Class editor: xproperty ordering */
  makeSortable : function(element) {
    if (!element) {
      return;
    }
    // Hide the property number, as ordering can be done by drag and drop
    element.select('.xproperty-content').each(function(item) {
      item.select("input").each(function(input) {
        if (input.id.endsWith("_number")) {
          item.numberProperty = input;
          input.up().hide();
          if (input.up().previous('dt')) {
            input.up().previous('dt').hide();
          }
        }
      });
    });
    // Create and insert move button
    element.select('.xproperty-title .tools').each(function(item) {
      var movebutton = new Element('span', {
        'class': 'tool move',
        title: 'Drag and drop to change the order'
      }).update('move');
      item.makePositioned();
      item.appendChild(movebutton);
      movebutton.observe('click', function(event) {
        event.stop();
      }.bindAsEventListener());
    });
    // Attach behavior to the move buttons
    Sortable.create('xclassContent', {
      tag : 'div',
      only : 'xproperty',
      handle : 'move',
      starteffect : this.startDrag.bind(this),
      endeffect : this.endDrag.bind(this),
      onUpdate : this.updateOrder.bind(this)
    });
  },
  updateOrder : function(container) {
    var children = container.childElements();
    for (var i = 0; i < children.size(); ++i) {
      var child = children[i].down(".xproperty-content");
      child.numberProperty.value = i+1;
    }
  },
  startDrag : function(dragged) {
    dragged.addClassName('dragged');
    $('xclassContent').childElements().each(function(item) {
      item._expandedBeforeDrag = !item.hasClassName('collapsed');
      item.addClassName('collapsed');
    });
  },
  endDrag : function(dragged) {
    dragged.removeClassName('dragged');
    $('xclassContent').childElements().each(function(item) {
      if (item._expandedBeforeDrag) {
        item.removeClassName('collapsed');
      }
    });
  }
});

function init() {
  return new editors.XDataEditors();
}

// When the document is loaded, create the Autosave control
(XWiki.domIsLoaded && init())
|| document.observe('xwiki:dom:loaded', init);

// End XWiki augmentation.
return XWiki;
}(XWiki || {}));
