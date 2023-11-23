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
/*!
#set ($paths = {
  'gadgetWizard': $xwiki.getSkinFile('uicomponents/dashboard/gadgetWizard.js', true)
})
#set ($l10nKeys = [
  'dashboard.actions.edit.differentsource.information',
  'dashboard.actions.edit.differentsource.warning',
  'dashboard.gadget.actions.drop',
  'dashboard.gadget.actions.delete.tooltip',
  'dashboard.gadget.actions.edit.tooltip',
  'dashboard.actions.add.tooltip',
  'dashboard.actions.add.button',
  'dashboard.actions.add.loading',
  'dashboard.actions.add.failed',
  'dashboard.gadget.actions.edit.error.notmacro',
  'dashboard.gadget.actions.edit.error.notmacro.title',
  'dashboard.gadget.actions.edit.loading',
  'dashboard.gadget.actions.edit.failed',
  'dashboard.gadget.actions.delete.confirm',
  'dashboard.gadget.actions.delete.inProgress',
  'dashboard.gadget.actions.delete.done',
  'dashboard.gadget.actions.delete.failed',
  'dashboard.actions.columns.add.tooltip',
  'dashboard.actions.columns.add.button',
  'dashboard.actions.edit.failed',
  'dashboard.actions.save.loading'
])
#set ($l10n = {})
#foreach ($key in $l10nKeys)
  #set ($discard = $l10n.put($key, $services.localization.render($key)))
#end
#set ($iconNames = ['pencil', 'cross', 'add'])
#set ($icons = {})
#foreach ($iconName in $iconNames)
  #set ($discard = $icons.put($iconName, $services.icon.renderHTML($iconName)))
#end
#[[*/
// Start JavaScript-only code.
(function(paths, l10n, icons) {
  "use strict";

require.config({paths});

window.XWiki = window.XWiki || {};
// Start XWiki augmentation.
XWiki.Dashboard = Class.create( {
  initialize : function(element) {
    this.element = element;  
    //the class of the gadget objects 
    this.gadgetsClass = "XWiki.GadgetClass";
    // read the metadata of the dashboard, the edit, add, remove URLs and the source of the dashboard
    this.readMetadata();
    // display the warning if we're editing a dashboard configured in a different document
    this.displayWarning();
    // flag to know if the dashboard was edited or not, to know if requests should be sent on save or not
    this.edited = false;
    // the list of removed gadgets, to really remove when the inline form is submitted
    this.removed = new Array();
    // add an extra class to this element, to know that it's editing, for css that needs to be special on edit
    this.element.addClassName("dashboard-edit");
    // find out all the gadget-containers in element and add them ids
    this.containers = element.select(".gadget-container");
    this.createDragAndDrops();
    this.addGadgetsHandlers();``
    // Create the section to contain add buttons
    var sectionAddButtons = new Element('section', {
      'class': 'containeradd'
    })
    this.element.insert({'top' : sectionAddButtons});
    this.addNewGadgetHandler();
    this.addNewContainerHandler();

    // Save the dashboard before the edit form is submitted.
    document.observe("xwiki:actions:beforeSave", this.saveChanges.bindAsEventListener(this));
  },

  /**
   * Reads the dashboard metadata from the HTML of the dashboard.
   */
  readMetadata : function() {
    // FIXME: check if all these elements are there or not, if not default on current document
    this.editURL = this.element.down('.metadata .editurl').readAttribute('href');
    this.removeURL = this.element.down('.metadata .removeurl').readAttribute('href');
    this.addURL = this.element.down('.metadata .addurl').readAttribute('href');

    this.sourcePage = this.element.down('.metadata .sourcepage').innerHTML;
    // Note: Starting with XWiki 7.2RC1, "sourcespace" is the full space reference and can contain more than 1 space
    // in the case of Nested Spaces (e.g. "space1.space2").
    this.sourceSpace = this.element.down('.metadata .sourcespace').innerHTML;
    this.sourceWiki = this.element.down('.metadata .sourcewiki').innerHTML;
    this.sourceURL = this.element.down('.metadata .sourceurl').readAttribute('href');
  },

  /**
   * Displays a warning at the top of the dashboard, if the source of the dashboard is not the current document, 
   * telling the users that they're editing something else that might impact other dashboards as well.
   */
  displayWarning : function() {
    if (XWiki.currentDocument.page != this.sourcePage || XWiki.currentDocument.space != this.sourceSpace 
        || XWiki.currentDocument.wiki != this.sourceWiki) {
      // by default styled by the colibri skin
      var warningElt = new Element('div', {'class' : 'box warningmessage differentsource'});
      // FIXME: I don't like the way these messages are used, should be able to insert the link in the translation
      var information = l10n['dashboard.actions.edit.differentsource.information'];
      var warning = l10n['dashboard.actions.edit.differentsource.warning'];
      var link = new Element('a', {'href' : this.sourceURL});
      link.update(this.sourceWiki + ':' + this.sourceSpace + '.' + this.sourcePage);
      warningElt.insert(information);
      warningElt.insert(link);
      warningElt.insert(warning);

      this.element.insert({'top' : warningElt});
    }
  },

  /**
   * @param container the container to get the id of 
   * @return the original container id, parsed from the HTML id of this container. 
   *         This is used to be able to have unique ids of containers in the HTML, and at the same time be able to match 
   *         containers ids to the model.
   * FIXME: this will cause issues with multiple dashboards, add the name of the dashboard to the ids.
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
   * FIXME: this will cause issues with multiple dashboards, add the name of the dashboard to the ids.
   */
  _getGadgetId : function(gadget) {
    // gadget ids are of the form gadget_<id>
    return gadget.readAttribute('id').substring(7);
  },

  /**
   * Returns a macro call XHTML comment from the passed macro call, to use as the content of a field in annotated 
   * XHTML syntax.
   * @param macroCall the macro call to wrap in a comment.
   * FIXME: check if there is escaping to do in the macro call to prevent nested comments.
   */
  _getMacroCallComment : function(macroCall) {
    return "<!--" + macroCall + "--><!--stopmacro-->";
  },

  /**
   * Parses a macro call XHTML comment and returns the macro call from it.
   * @param macroCallComment the XHTML comment that annotates the macro call
   * @return the macro call, parsed from the comment string
   */
  _parseMacroCallComment : function(macroCallComment) {
    var endPos = macroCallComment.indexOf("-->");
    if (macroCallComment.startsWith("<!--startmacro:") && endPos > 0) {
      return macroCallComment.substr(4, endPos - 4);
    } else {
      return null;
    }
  },

  /*
   * Drag & drop decorators functions, to display nice placeholders when dragging & dropping.
   */
  _insertPlaceholder: function (container) {
    if ( container.down('.gadget') || container.down('.gadget-placeholder')) {
      return;
    }
    var placeholder = new Element('div', {'class': 'gadget-placeholder'}).update(l10n['dashboard.gadget.actions.drop']);
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
      this.makeSortable(containerId, containerIds, this.onMoveGadget.bind(this));
    }.bind(this));

    // Add the decorators to the gadgets on drag & drop.
    Draggables.addObserver({
      onStart: this._doOnStartDrag.bind(this),
      onEnd: this._doOnEndDrag.bind(this)
    });
  },

  /**
   * Makes the container identified by containerId sortable, that is accepting drags and drops.
   * 
   * @param containerId the id of the container to make sortable
   * @param containerIds the list of ids of all containers that are sortable
   * @param onMove move callback, to be called when an item is dragged from a container to another
   */
  makeSortable : function(containerId, containerIds, onMove) {
    Sortable.create(containerId, {
      tag: 'div',
      only: 'gadget',
      handle: 'gadget-title',
      overlap: 'vertical',
      scroll: window,
      containment: containerIds,
      dropOnEmpty: true,
      constraint: false,
      ghosting: false,
      hoverclass: 'gadget-container-hover-highlight',
      onUpdate: onMove
    });
  },

  /**
   * Adds handlers to the gadgets on the dashboard: the edit and remove gadget actions.
   */
  addGadgetsHandlers : function() {
    // Iterate through all the gadgets and add handlers.
    this.element.select('.gadget').each(this.addGadgetHandlers.bind(this));
  },

  /**
   * Adds gadget action buttons on the gadget title bar, visible when hovering the gadget.
   *
   * @param gadget the gadget to add the action buttons to
   */
  addGadgetHandlers : function(gadget) {
    var removeIcon = new Element('span', {
      'class': 'remove action',
      'title': l10n['dashboard.gadget.actions.delete.tooltip']
    });
    removeIcon.observe('click', this.onRemoveGadget.bindAsEventListener(this));
    removeIcon.update(icons.cross);

    var editIcon = new Element('span', {
      'class': 'edit action',
      'title': l10n['dashboard.gadget.actions.edit.tooltip']
    });
    editIcon.observe('click', this.onEditGadgetClick.bindAsEventListener(this));
    editIcon.update(icons.pencil);

    var actionsContainer = new Element('div', {'class' : 'gadget-actions'})
    actionsContainer.insert(editIcon);
    actionsContainer.insert(removeIcon);
    gadget.insert(actionsContainer);
  },

  /**
   * Adds the handler to add the new gadget on the dashboard, the "Add" button at the top.
   */
  addNewGadgetHandler : function() {
    // create the button
    var addButton = new Element('div', {
      'class': 'btn btn-success addgadget',
      'title': l10n['dashboard.actions.add.tooltip']
    });
    addButton.update(icons.add + l10n['dashboard.actions.add.button']);
    addButton.observe('click', this.onAddGadgetClick.bindAsEventListener(this));
    // check if the warning is there, if it is, put the button under it
    var warning = this.element.down('.differentsource');
    if (warning) {
      warning.insert({'after' : addButton});
    } else {
      this.element.down('.containeradd').insert(addButton);
    }
  },

  /**
   * Handles the click on the add gadget button, starts the add gadget wizard.
   * 
   * @param event the click event on the add gadget button
   */
  onAddGadgetClick : function(event) {
    this.runGadgetWizard($(event.target), null, this.onAddGadgetComplete.bind(this));
  },

  runGadgetWizard: function(button, gadget, callback) {
    if (button.hasClassName('loading')) {
      return;
    }
    button.addClassName('loading');
    require(['gadgetWizard'], function(gadgetWizard) {
      gadgetWizard(gadget).then(callback).finally(() => {
        button.removeClassName('loading');
      });
    });
  },

  /**
   * Handles the command to actually add the gadget, after the user has filled in the wizard
   *
   * @param gadgetConfig the object representing the gadget instance, with the following fields: 'title', the title of 
   *                     the gadget, and 'content' the macroCall, to wrap in an XHTML comment and set as the value of 
   *                     the wysiwyg edited gadget content field. 
   */
  onAddGadgetComplete : function (gadgetConfig) {
    // compose the parameters for the object add action
    // compose the html comment for the contentField
    var contentField = this._getMacroCallComment(gadgetConfig.content);
    // to generate the position of the newly added gadget, on the last position in the last column
    var lastColumn = this.containers.length;
    var lastIndex = this.containers.last().select('.gadget').length + 1;
    // prepare parameters
    var addParameters = new Hash();
    // class
    addParameters.set('classname', this.gadgetsClass);
    // title
    addParameters.set(this.gadgetsClass + '_title', gadgetConfig.title);
    // content
    addParameters.set(this.gadgetsClass + '_content', contentField);
    addParameters.set('RequiresHTMLConversion', this.gadgetsClass + '_content');
    addParameters.set(this.gadgetsClass + '_content_syntax', "xwiki/2.0");
    // position
    addParameters.set(this.gadgetsClass + '_position', lastColumn + ', ' + lastIndex);
    // steal the form token of the edit form around the dashboard and send it with the form
    var formToken = this.getFormToken();
    addParameters.set('form_token', formToken);
    // aaaand send the request
    this._x_notification = new XWiki.widgets.Notification(l10n['dashboard.actions.add.loading'], "inprogress");
    new Ajax.Request(
      this.addURL,
      {
        parameters : addParameters,
        onSuccess : function (response) {
          // don't hide the notification here, leave it loading, will be removed by the reload
          // FIXME: reload only that widget
          window.location.reload();
        }.bind(this),
        onFailure : function(response) {
          var failureReason = response.statusText || 'Server not responding';
          this._x_notification.replace(new XWiki.widgets.Notification(
            l10n['dashboard.actions.add.failed'] + failureReason, "error", {timeout: 5}));
        }.bind(this),
        on0: function (response) {
          response.request.options.onFailure(response);
        }.bind(this)
      }
    );
  },

  /**
   * Handles the click on the edit gadget button: parses gadget metadata and starts the edit gadget wizard.
   *
   * @param event the click event on the edit gadget button 
   */
  onEditGadgetClick : function(event) {
    var gadget = event.element().up('.gadget');

    /**
     * Finds the first direct child element of the given element with the specified class name.
     *
     * @param {Element} element - the element to search for the child in
     * @param {string} className - the class name of the child element to find
     * @returns {Element|undefined} the direct child element with the specified class name, or undefined if not found
     */
    function findDirectChildWithClass(element, className) {
      return element.immediateDescendants().filter((e) => e.hasClassName(className)).first();
    }

    if (gadget) {
      // check if it is a macro
      const gadgetMetadata = findDirectChildWithClass(gadget, 'metadata');
      if (!gadgetMetadata) {
        return;
      }
      const macroMetadata = findDirectChildWithClass(gadgetMetadata, 'isMacro');
      if (macroMetadata && macroMetadata.innerHTML == 'true') {
        // it's a macro, edit it
        // get the gadget id
        var gadgetId = this._getGadgetId(gadget);
        var title, macroCall;
        // get the gadget metadata, start the wizard
        const titleMetadata = findDirectChildWithClass(gadgetMetadata, 'title');
        if (titleMetadata) {
          title = titleMetadata.innerHTML;
        }
        const macroCommentMetadata = findDirectChildWithClass(gadgetMetadata, 'content');
        if (macroCommentMetadata) {
          var macroComment = macroCommentMetadata.innerHTML;
          macroCall = this._parseMacroCallComment(macroComment);
        }

        this.runGadgetWizard($(event.target), {
          title: title,
          content: macroCall
        }, function(gadgetConfig) {
          this.onEditGadgetComplete(gadgetId, gadgetConfig);
        }.bind(this));
      } else {
        // this is not a macro, cannot be edited with wysiwyg macro dialog. Display a message, pointing the user to 
        // object editor.
        var dialog = new XWiki.widgets.ModalPopup(
          l10n['dashboard.gadget.actions.edit.error.notmacro'],
          {}, 
          {title: l10n['dashboard.gadget.actions.edit.error.notmacro.title']}
        );
        dialog.showDialog();
      }
    }
  },

  /**
   * Handles the command to actually edit the gadget's parameters, sends the ajax request to the edit url, after the 
   * user has completed the wizard.
   *
   * @param gadgetId the id of the gadget to edit
   * @param gadgetConfig the object representing the gadget instance, with the following fields: 'title', the title of 
   *                     the gadget, and 'content' the macroCall, to wrap in an XHTML comment and set as the value of 
   *                     the wysiwyg edited gadget content field.
   */
  onEditGadgetComplete : function(gadgetId, gadgetConfig) {
    // compose the parameters for the object edit action
    // compose the html comment for the contentField
    var contentField = this._getMacroCallComment(gadgetConfig.content);
    // prepare parameters
    var editParameters = new Hash();
    // title
    editParameters.set(this.gadgetsClass + '_' + gadgetId + '_title', gadgetConfig.title);
    // content
    editParameters.set(this.gadgetsClass + '_' + gadgetId + '_content', contentField);
    editParameters.set('RequiresHTMLConversion', this.gadgetsClass + '_' + gadgetId + '_content');
    editParameters.set(this.gadgetsClass + '_' + gadgetId + '_content_syntax', "xwiki/2.0");
    editParameters.set('ajax', '1');
    // steal form token parameter to be able to submit a valid form on the server
    var formToken = this.getFormToken();
    editParameters.set('form_token', formToken);

    // TODO: since I will not save position at this point, there will be an issue if a gadget is moved and then its 
    // parameters are edited, since it will not preserve position    

    // aaaand send the request
    this._x_notification = new XWiki.widgets.Notification(l10n['dashboard.gadget.actions.edit.loading'], "inprogress");
    new Ajax.Request(
      this.editURL,
      {
        parameters : editParameters,
        onSuccess : function (response) {
          // don't hide the notification here, leave it loading, will be removed by the reload
          // FIXME: reload only that widget
          window.location.reload();
        }.bind(this),
        onFailure : function(response) {
          var failureReason = response.statusText || 'Server not responding';
          this._x_notification.replace(new XWiki.widgets.Notification(l10n['dashboard.gadget.actions.edit.failed'] +
            failureReason, "error", {timeout: 5}));
        }.bind(this),
        on0: function (response) {
          response.request.options.onFailure(response);
        }.bind(this)
      }
    );
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
    new XWiki.widgets.ConfirmedAjaxRequest(
      this.removeURL,
      {
        parameters : {
          'classname' : encodeURIComponent(this.gadgetsClass),
          'classid' : encodeURIComponent(gadgetId),
          'ajax' : '1',
          'form_token' : this.getFormToken()
        },
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
         confirmationText: l10n['dashboard.gadget.actions.delete.confirm'],
         progressMessageText: l10n['dashboard.gadget.actions.delete.inProgress'],
         successMessageText: l10n['dashboard.gadget.actions.delete.done'],
         failureMessageText: l10n['dashboard.gadget.actions.delete.failed']
      }
    );
  },

  /**
   * Creates the button to add a new container in this dashboard, and inserts it after the add gadget button.
   * FIXME: this is columns layout specific, because it uses translations keys specific to columns
   */
  addNewContainerHandler : function() {
    // create the button
    var addButton = new Element('div', {
      'class': 'btn btn-success addcontainer',
      'title': l10n['dashboard.actions.columns.add.tooltip']
    });
    addButton.update(icons.add + l10n['dashboard.actions.columns.add.button']);
    addButton.observe('click', this.onAddColumn.bindAsEventListener(this));
    this.element.down('.containeradd').insert(addButton);
  },

  /**
   * Adds a column in this dashboard.
   * FIXME: this is columns layout specific, need to find a way to generate this on the server
   *
   * @param event the click event, on the 'add column' button.
   */
  onAddColumn : function(event) {
    // get the currently last container, to add after it
    var lastContainer = this.containers.last();
    var currentlyLastContainerId = this._getContainerId(lastContainer);
    var newIdNumber = 1 + parseInt(currentlyLastContainerId, 10);
    var newId = 'gadgetcontainer_' + newIdNumber;
    // create the container
    var newContainer = new Element('div', {'class' : lastContainer.readAttribute('class'),
      'id' : newId});
    // update the other containers - this is now the last column
    lastContainer.removeClassName('last-column');

    // add it in the DOM and in the containers list
    lastContainer.insert({'after' : newContainer});
    this.containers.push(newContainer);

    // Update the columns container to display the columns in the right way.
    // Compute the previous class name regarding the number of columns that we have currently and the new class name
    var oldColumnsContainerClass = 'container-columns-' + (this.containers.length - 1);
    var newColumnsContainerClass = 'container-columns-' + this.containers.length;
    var containersContainer = lastContainer.parentNode;
    containersContainer.addClassName(newColumnsContainerClass);
    containersContainer.removeClassName(oldColumnsContainerClass);

    // make it draggable
    var containerIds = new Array();
    this.containers.each(function(container) {
      containerIds.push(container.readAttribute('id'));
    });
    // recreate the drag & drops, to take into account the new added container as well. Re-create all because they all
    // need to take into account the new one
    this.createDragAndDrops();
  },

  /**
   * Actually performs the gadget edits, calling the onComplete callback when the edit is done.
   *
   * @param onComplete callback to notify when all the requests have finished
   */
  doEditGadgets : function(onComplete) {
    var editParameters = this.prepareEditParameters();
    // add the ajax parameter to the edit, to not get redirected after the call
    editParameters.set('ajax', '1');
    // steal the form token from the edit form to be able to submit valid form data on the server
    var formToken = this.getFormToken();
    editParameters.set('form_token', formToken);
    // send the ajax request to do the edit
    new Ajax.Request(
      this.editURL,
      {
        parameters : editParameters,
        onSuccess : function(response) {
          this.edited = false;
          if (response.responseJSON && response.responseJSON.newVersion) {
            // update the version
            require(['xwiki-meta'], function (xm) {
              xm.setVersion(response.responseJSON.newVersion);
              if (onComplete) {
                onComplete();
              }
            });
          } else {
            if (onComplete) {
              onComplete();
            }
          }
        }.bind(this),
        onFailure: function(response) {
          var failureReason = response.statusText || 'Server not responding';
          // show the error message at the bottom
          this._x_notification = new XWiki.widgets.Notification(l10n['dashboard.actions.edit.failed'] + failureReason,
            "error", {timeout: 5});
          if (onComplete) {
            onComplete();
          }
        }.bind(this),
        on0: function (response) {
          response.request.options.onFailure(response);
        }.bind(this)
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
    // if there are no changes, don't do anything
    if (!this.edited) {
      return;
    }

    // if there are changes, stop the save event, send an ajax request to the source of the dashboard to save it,
    // and then, when it's done, refire the save event

    // stop the event, so that it doesn't actually send the request just yet, we'll send it when we're done with saving
    event.stop();

    // get the element of the event
    var eventElt = event.element();

    // start to submit the edit, notify
    this._x_edit_notification = new XWiki.widgets.Notification(l10n['dashboard.actions.save.loading'], "inprogress");

    // save the edit
    this.doEditGadgets(function() {
      // and re-fire the save event, only if the changes were saved fine (edited is canceled)
      if (!this.edited) {
        // resume the form submit
        eventElt.click();
      }
      // and remove the notification
      if (this._x_edit_notification) {
        this._x_edit_notification.hide();
      }
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
    this.containers.each(function(container) {
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
  },

  /**
   * Gets the form token from the underlying xwiki edit inline form, to be able to submit a valid form on the server.
   * @return the form token of the form where the dashboard element appears in (the inline edit form), or empty string
   *         if such form does not exist
   */
  getFormToken : function() {
    var editForm = this.element.up('form');
    if (editForm) {
      var formTokenElt = editForm['form_token'];
      if (formTokenElt) {
        return formTokenElt.value;
      }
    }
    return '';
  }
});

function init() {
  // editable dashboard only in inline edit mode
  if (XWiki.contextaction == 'inline' || (XWiki.contextaction == 'edit' && $('inline'))) {
    // edit first dashboard FIXME: to create a dashboard editor for all dashboards
    var dashboardRootElt = $$('.dashboard')[0];
    if (dashboardRootElt) {
      require(['scriptaculous/dragdrop'], function() {
        new XWiki.Dashboard(dashboardRootElt);
      });
    }
  }
  return true;
}

// When the document is loaded, enable the dashboard editor in inline mode.
(XWiki.domIsLoaded && init())
|| document.observe("xwiki:dom:loaded", init);

// End JavaScript-only code.
}).apply(']]#', $jsontool.serialize([$paths, $l10n, $icons]));
