var XWiki = (function (XWiki) {
// Start XWiki augmentation.
XWiki.Dashboard = Class.create( {
  initialize : function(element) {
    //the class of the gadget objects 
    this.gadgetsClass = "XWiki.GadgetClass";
    // the source of the dashboard, the document where the objects are stored and which needs to be updated
    // TODO: put me here, but it's a bit tough because we don't know if as URL or as fullname or space, page, wiki vars
    // flag to know if the dashboard was edited or not, to know if requests should be sent on save or not
    this.edited = false;
    // the list of removed gadgets, to really remove when the inline form is submitted
    this.removed = new Array();
    this.element = element;
    // add an extra class to this element, to know that it's editing, for css that needs to be special on edit
    this.element.addClassName("dashboard-edit");
    // find out all the gadget-containers in element and add them ids
    this.containers = element.select(".gadget-container");
    this.createDragAndDrops();
    this.addGadgetsHandlers();
    
    // add save listener, to save the dashboard before submit of the form
    document.observe("xwiki:actions:save", this.saveChanges.bindAsEventListener(this));
  },
  
  /**
   * @param container the container to get the id of 
   * @return the original container id, parsed from the HTML id of this container. 
   *         This is used to be able to have unique ids of containers in the HTML, and at the same time be able to match 
   *         containers ids to the model.
   * FIXME: this will cause issues with multiple dashboards, add the name of the dashboard to the ids         
   */
  _getContainerId : function (container) {
    // the gadget container id is of the form gadgetcontainer_<containerId>, so parse it back
    return container.readAttribute('id').substring(16);
  },
  
  /**
   * @param gadget the gadget to get the id of 
   * @return the original gadget id, parsed from the HTML id of this gadget. 
   *         This is used to be able to have unique ids of gadgets in the HTML, and at the same time be able to match 
   *         gadgets ids to the model.
   * FIXME: this will cause issues with multiple dashboards, add the name of the dashboard to the ids         
   */  
  _getGadgetId : function(gadget) {
    // gadget ids are of the form gadget_<id>
    return gadget.readAttribute('id').substring(7);    
  },
  
  /*
   * Drag & drop decorators functions, to display nice placeholders when dragging & dropping.
   */
  _insertPlaceholder: function (container) {
    if ( container.down('.gadget') || container.down('.gadget-placeholder')) {
      return;
    }
    var placeholder = new Element('div', {'class' : 'gadget-placeholder'})
      .update('$msg.get("dashboard.gadget.actions.drop")');
    container.insert(placeholder);
  },

  _removePlaceholder: function (container) {
    var placeholders = container.select('.gadget-placeholder');
    placeholders.each(function (el) { 
      el.remove(); 
    });
  },

  _doOnStartDrag: function() {
    this.containers.each(this._insertPlaceholder);
  },
  _doOnEndDrag: function() {
    this.containers.each(this._removePlaceholder);
  },  

  /**
   * Creates drag and drops for the gadgets inside the gadget containers.
   */
  createDragAndDrops : function() {
    // put all the container ids in a list, to be able to pass it to the sortables
    var containerIds = new Array();
    this.containers.each(function(container) {
      containerIds.push(container.readAttribute('id'));
    });    
    
    // create a sortable for each gadget container
    containerIds.each(function(containerId) {
      Sortable.create(containerId, {
        tag:'div', 
        only:'gadget', 
        handle:'gadget-title', 
        overlap: 'vertical', 
        scroll: window, 
        containment: containerIds, 
        dropOnEmpty: true, 
        constraint: false,
        ghosting:false,
        hoverclass: 'gadget-container-hover-highlight',
        onUpdate: this.onMoveGadget.bind(this)
      });
    }.bind(this));
  },
  
  /**
   * Adds handlers to the gadgets on the dashboard, the remove.
   */
  addGadgetsHandlers : function() {
    // iterate through all the gadgets and add settings handlers
    this.element.select('.gadget').each(function(gadget){
      // create a settings menu button and add it to the gadget-title
      var itemMenu = new Element('div', {'class' : 'settings', 'title' : '$msg.get("dashboard.gadget.actions.tooltip")'});
      var gadgetTitle = gadget.down('.gadget-title');
      if (!gadgetTitle) {
        return;
      }
      // create a remove button in the settings menu
      var removeLink = new Element('div', {'class' : 'remove', 'title' : '$msg.get("dashboard.gadget.action.delete.tooltip")'});
      removeLink.observe('click', this.onRemoveGadget.bindAsEventListener(this));
      var actionsContainer = new Element('div', {'class' : 'settings-menu'})
      actionsContainer.hide();
      actionsContainer.insert(removeLink);
      itemMenu.hide();
      gadgetTitle.insert(itemMenu);
      gadgetTitle.insert(actionsContainer);
      // and listen to the click to open the menu
      itemMenu.observe('click', function(event){
        // toggle actions container
        actionsContainer.toggle();
        // and add a class to the item menu, to be able to color it properly
        itemMenu.toggleClassName('settings-open');
      });
      
      // display the link remove link only when the gadget is hovered
      gadget.observe('mouseover', function() {
        itemMenu.show();
      });
      gadget.observe('mouseout', function(event) {
        var relatedTarget = event.relatedTarget || event.toElement;
        if ((event.element() == gadget || event.element().up('.gadget')) && (relatedTarget == null || relatedTarget.up('.gadget') == null)) {
          // enough propagation
          event.stop();
          itemMenu.hide();
          actionsContainer.hide();
          itemMenu.removeClassName('settings-open');
        }
      });
    }.bind(this));

    // add the decorators to the gadgets on drag & drop
    var doOnStartDrag = this._doOnStartDrag.bind(this);
    var doOnEndDrag = this._doOnEndDrag.bind(this);
    Draggables.addObserver({
        onStart: doOnStartDrag,
        onEnd: doOnEndDrag
    });
  },
  
  /**
   * Removes the gadget passed by its id.
   * 
   * @param event the click event on the remove button for a gadget  
   */
  onRemoveGadget : function(event) {
    // get the clicked button
    var item = event.element();
    // get the gadget to remove
    var gadget = item.up(".gadget");
    if (!gadget) {
      return;
    }
    var gadgetId = this._getGadgetId(gadget);
    this.removed.push(gadgetId);
    // send a request to the REST url to remove the object corresponding to this gadget    
    // TODO: this should be the URI to the dashboard source, not to the current page
    var gadgetObjectURL = XWiki.contextPath + '/rest/wikis/' + encodeURIComponent(XWiki.currentWiki) + '/spaces/' + 
                          encodeURIComponent(XWiki.currentSpace) + '/pages/' + encodeURIComponent(XWiki.currentPage) + 
                          '/objects/' + encodeURIComponent(this.gadgetsClass) + '/' + gadgetId + '?media=json&method=DELETE';
    new XWiki.widgets.ConfirmedAjaxRequest(
      gadgetObjectURL,
      {  
        onCreate : function() {
          // Disable the button, to avoid a cascade of clicks from impatient users
          item.disabled = true;
        },
        onSuccess : function(response) {
          // remove the gadget from the page
          gadget.remove();
        }.bind(this)
      },
      /* Interaction parameters */
      {
         confirmationText: "$msg.get('dashboard.gadget.action.delete.confirm')",
         progressMessageText : "$msg.get('dashboard.gadget.action.delete.inProgress')",
         successMessageText : "$msg.get('dashboard.gadget.action.delete.done')",
         failureMessageText : "$msg.get('dashboard.gadget.action.delete.failed')"
      }      
    );
  },
  
  /**
   * Function called when a gadget has been moved from a container to another.
   * 
   * @param container the source and target container for the move, depending on the particular call of this function
   */
  onMoveGadget : function(container) {
    // just flag that the dashboard was edited, actual changes were performed when the save button will be clicked
    this.edited = true;
  },
  
  /**
   * Saves the changes on the dashboard in this document: perform all removes and injects the object edit fields in the 
   * inline edit form.  
   */
  saveChanges : function(event) {
    if (!this.edited) {
      return;
    }

    var editFields = this.prepareEditParameters();
    // get the edit form
    var editForm = event.memo.form;
    // put the fields in a dashboard layout fieldset, to be able to update them on subsequent saves 
    // (in case of save & continue)
    // FIXME: add the name of the dashboard in here, will cause issues when there are multiple dashboards
    var dashboardFieldSet = editForm.down('.dashboard-layout-fieldset');
    if (dashboardFieldSet) {
      // empty the dashboard field set, to fill in with the new fields after
      dashboardFieldSet.update('');
    } else {
      // create the dashboard field set, to fill in with the new fields
      dashboardFieldSet = new Element('fieldset', {'class' : 'dashboard-layout-fieldset'});
      editForm.insert(dashboardFieldSet);      
    }
    editFields.each(function(editField) {
      // create a hidden input for the update of the position of the gadget object and append it to the form
      var fieldInput = new Element('input', {'name' : editField.key, 'value' : editField.value, 'type' : 'hidden'});
      dashboardFieldSet.insert(fieldInput);
    }.bind(this));
  },

  /**
   * Prepares a hashmap of parameters that would update the positions of the gadget fields.
   * @return the hash of parameters to submit to the object edit URL, to update the gadget object positions in this 
   *         dashboard
   */
  prepareEditParameters : function() {
    var parameters = new Hash();
    // for each gadget in the containers, put it in the map, along with its new position
    this.element.select('.gadget-container').each(function(container) {
      // get the id of the container
      var containerId = this._getContainerId(container);
      // foreach of its gadget children, get the position and compose the position update field
      container.select('.gadget').each(function(gadget, index) {
        // get the id of the current gadget -> object number, actually
        var gadgetId = this._getGadgetId(gadget);
        // the position field name as in the inline form edit XWiki.GadgetClass_0_position
        var positionFieldName = this.gadgetsClass + '_' + gadgetId + '_' + 'position';
        // compose the position field value as container, index (1 based, though)        
        var positionFieldValue = containerId + ', ' + (index + 1);
        // and put these in the prepared hash
        parameters.set(positionFieldName, positionFieldValue);
      }, this);
    }, this);
    
    return parameters;
  }  
});
//End XWiki augmentation.
return XWiki;
}(XWiki || {}));

document.observe('xwiki:dom:loaded', function(event) {
  // editable dashboard only in inline mode
  if (XWiki.contextaction == 'inline') {
    // edit first dashboard FIXME: to create a dashboard editor for all dashboards
    var dashboardRootElt = $$('.dashboard')[0];
    if (dashboardRootElt) {
      var dashboard = new XWiki.Dashboard(dashboardRootElt);
    }
  }
});