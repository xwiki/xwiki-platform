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
var XWiki = (function(XWiki) {
// Start XWiki augmentation.
var editors = XWiki.editors = XWiki.editors || {};
editors.XDataEditors = Class.create({

  // Maintain informations about actions performed on the editor.
  // Data abouts xobjects are map whom keys are xclass names and values are list of sorted xobjects ids
  editorStatus : {
    savedXObjects: {}, // already saved objects
    addedXObjects: {}, // objects added but not saved yet
    deletedXObjects: {} // objects deleted but not removed yet
  },

  initialize : function() {
    this.editedDocument = XWiki.currentDocument;

    $$('.xclass').each(function(item) {
      this.enhanceClassUX(item, true);
    }.bind(this));
    this.ajaxObjectAdd($('add_xobject'));
    this.ajaxPropertyAdd();
    this.makeSortable($('xwikiclassproperties'));
    this.ajaxRemoveDeprecatedProperties($("body"), ".syncAllProperties");

    // everytime the document is saved we restore the current state to be in sync with the server
    document.observe('xwiki:document:saved', function (event) {
      // Remove fields about deleted and added objects
      $$('input[name=deletedObjects]').each(function(item) {
        item.remove();
      });
      $$('input[name=addedObjects]').each(function(item) {
        item.remove();
      });

      // Show hidden links about edit and delete
      // Edit link is hidden on not-saved objects since it needs to be saved on the server.
      // Delete links are only shown for latest added object of an xclass to keep a good numbering.
      $$('.xobject-action.edit').invoke('show');
      $$('.xobject').each(function (item) {
        item.down('a.delete').show();
      });

      // We need to put all info about added objects in the saved objects collection.
      // We don't need to do that for deleted objects since we cannot revert a deletion in the UI except by canceling
      // the changes. So we directly manipulate the savedXObjects collection when performing a deletion.
      for (var xclassName in this.editorStatus.addedXObjects) {
        if (this.editorStatus.savedXObjects[xclassName] === undefined) {
          this.editorStatus.savedXObjects[xclassName] = this.editorStatus.addedXObjects[xclassName];
        } else {
          this.editorStatus.savedXObjects[xclassName].concat(this.editorStatus.addedXObjects[xclassName]);
          this.editorStatus.savedXObjects[xclassName].sort(this.numberSort);
        }
      }
      this.editorStatus.addedXObjects = {};
      this.editorStatus.deletedXObjects = {};
    }.bindAsEventListener(this));

    // in case of cancel we just clean everything so that we don't get any warnings for leaving the page without saving.
    document.observe('xwiki:actions:cancel', function (event) {
      // Remove fields about deleted and added objects
      $$('input[name=deletedObjects]').each(function(item) {
        item.remove();
      });
      $$('input[name=addedObjects]').each(function(item) {
        item.remove();
      });
      this.editorStatus.addedXObjects = {};
      this.editorStatus.deletedXObjects = {};
    }.bindAsEventListener(this));

    // We want to the user to be prevented if he tries to leave the editor before saving.
    window.addEventListener("beforeunload", function(event) {
      if (Object.keys(this.editorStatus.addedXObjects).length > 0
        || Object.keys(this.editorStatus.deletedXObjects).length > 0) {
        event.preventDefault();
        event.returnValue = "";
      } else {
        return;
      }
    }.bind(this));
  },
  /**
   * Sort function to allow sorting an array of integer.
   * Using [3, 0, 1].sort(numberSort) produces [0, 1, 3].
   */
  numberSort : function (a, b) {
    return a - b;
  },
  /**
   * Extract the xclass name from the XDOM id of the xclass.
   */
  getXClassNameFromXClassId : function (xclassId) {
    return xclassId.substring("xclass_".length);
  },
  /**
   * Extract the xclass name from the XDOM id of an xobject.
   */
  getXClassNameFromXObjectId : function (xobjectId) {
    return xobjectId.substring("xobject_".length, xobjectId.lastIndexOf('_'));
  },
  /**
   * Extract the xobject number from the XDOM id of an xobject.
   */
  getXObjectNumberFromXObjectId : function (xobjectId) {
    return xobjectId.substring(xobjectId.lastIndexOf('_') + 1);
  },
  /**
   * Returns true if an object exists with the given classname and number.
   */
  xObjectAlreadyExist : function (xclassName, objectNumber) {
    return (this.editorStatus.savedXObjects[xclassName] !== undefined
      && this.editorStatus.savedXObjects[xclassName].indexOf(objectNumber) !== -1)
      || (this.editorStatus.addedXObjects[xclassName] !== undefined
        && this.editorStatus.addedXObjects[xclassName].indexOf(objectNumber) !== -1);
  },
  /**
   * Returns the xobject DOM element for the given class and number.
   */
  getXObject : function (xclassName, objectNumber) {
    var expectedId = 'xobject_' + xclassName + '_' + objectNumber;
    var possibleObjects = $$('.xobject');
    for (var i = 0; i < possibleObjects.length; i++) {
      if (possibleObjects[i].id === expectedId) {
        return possibleObjects[i];
      }
    }
  },

  getDeletedXObject : function (xclassName, objectNumber) {
    var expectedId = 'deletedObject_' + xclassName + '_' + objectNumber;
    var possibleObjects = $$('input[name=deletedObjects]');
    for (var i = 0; i < possibleObjects.length; i++) {
      if (possibleObjects[i].id === expectedId) {
        return possibleObjects[i];
      }
    }
  },
  /**
   * Helper to remove an element from an array if and only if it was already in the array.
   */
  removeElementFromArray : function (array, element) {
    if (array.indexOf(element) !== -1) {
      array.splice(array.indexOf(element), 1);
    }
  },
  /**
   * Compute the new object number for a class name given the information we already have about savedXObjects and
   * addedXObjects.
   */
  getNewObjectNumber : function (xclassName) {
    var objectNumberVal;
    // if we already added xobjects of this type, the last number has to be taken there
    if (this.editorStatus.addedXObjects[xclassName] !== undefined) {
      objectNumberVal = Number(this.editorStatus.addedXObjects[xclassName].last()) + 1;
      // if we don't have added xobjects yet, but some are already saved, then we take the last number there
    } else if (this.editorStatus.savedXObjects[xclassName] !== undefined) {
      objectNumberVal = Number(this.editorStatus.savedXObjects[xclassName].last()) + 1;
    } else {
      objectNumberVal = 0;
    }
    objectNumberVal = objectNumberVal + "";

    // if an object with this number was already deleted, we remove the info from deleted objects.
    var deletedArray = this.editorStatus.deletedXObjects[xclassName];
    if (deletedArray !== undefined && deletedArray.indexOf(objectNumberVal) !== -1) {
      this.getDeletedXObject(xclassName, objectNumberVal).remove();
      this.removeElementFromArray(deletedArray, objectNumberVal);
      if (deletedArray.length === 0) {
        delete this.editorStatus.deletedXObjects[xclassName];
      }
    }
    return objectNumberVal;
  },
  /**
   * Enhance xclass for the JS behaviours and iterate over inner xobjects or xproperties to enhance them.
   *
   * @param xclass the xclass DOM element
   * @param init true if it's the call performed during the script initialization.
   */
  enhanceClassUX : function(xclass, init) {
    this.ajaxObjectAdd(xclass);
    this.expandCollapseClass(xclass);

    xclass.select('.xproperty').each(function(item) {
      this.expandCollapseMetaProperty(item);
      this.ajaxPropertyDeletion(item);
      this.makeDisableVisible(item);
    }.bind(this));

    // We always iterate on the xobjects of the xclass to enhance them.
    xclass.select('.xobject').each(function(item) {
      this.enhanceObjectUX(item, init);
    }.bind(this));
  },
  enhanceObjectUX : function(object, init) {
    var xclassName = this.getXClassNameFromXObjectId(object.id);
    var objectNumber = this.getXObjectNumberFromXObjectId(object.id);
    var listObjects;

    // Initialize the xobjects collections.
    if (init) {
      if (this.editorStatus.savedXObjects[xclassName] === undefined) {
        this.editorStatus.savedXObjects[xclassName] = [];
      }
      listObjects = this.editorStatus.savedXObjects[xclassName];
    } else {
      if (this.editorStatus.addedXObjects[xclassName] === undefined) {
        this.editorStatus.addedXObjects[xclassName] = [];
      }
      listObjects = this.editorStatus.addedXObjects[xclassName];
    }

    if (!this.xObjectAlreadyExist(xclassName, objectNumber)) {
      // we push the object number and we always sort the array.
      listObjects.push(objectNumber);
      listObjects.sort(this.numberSort);

      // We keep record of the added objects after init by adding a form input about them
      // The idea is to be able to submit to the server all the object added, even if they don't have any property
      // such as for AWM Content type.
      // We put those input directly with the xobject DOM element since we want it to be removed if the object is
      // removed later.
      if (!init) {
        var addedObject = new Element('input', {
          'type': 'hidden',
          'name': 'addedObjects',
          'id': 'addedObject_' + xclassName + '_' + objectNumber,
          'value': xclassName + '_' + objectNumber
        });
        object.appendChild(addedObject);
      }
      this.ajaxObjectDeletion(object);
      this.editButtonBehavior(object);
      this.expandCollapseObject(object);
      this.ajaxRemoveDeprecatedProperties(object, ".syncProperties");
    }
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
        var url, validClassName, classNameVal, objectNumberVal;
        if (item.href) {
          url = item.href.replace(/[?&]xredirect=[^&]*/, '');
          validClassName = true;

          // We compute the object number based on the information we have in our recorded objects array.
          classNameVal = this.getXClassNameFromXClassId(item.up('.xclass').id);
          url += "&objectNumber=" + this.getNewObjectNumber(classNameVal);
        } else {
          var classNameElt = element.down('select');
          if (classNameElt && classNameElt.selectedIndex >= 0) {
            classNameVal = classNameElt.options[classNameElt.selectedIndex].value;
          }
          validClassName = classNameVal && classNameVal != '-';
          url = this.editedDocument.getURL('edit', Object.toQueryString({
            xpage: 'editobject',
            xaction: 'addObject',
            classname: classNameVal,
            objectNumber: this.getNewObjectNumber(classNameVal),
            form_token: $$('input[name=form_token]')[0].value
          }));
        }
        if (!item.disabled && validClassName) {
          new Ajax.Request(
            /* Ajax request URL */
            url,
            /* Ajax request parameters */
            {
              onCreate : function() {
                item.disabled = true;
                item.notification = new XWiki.widgets.Notification("$services.localization.render('core.editors.object.add.inProgress')", "inprogress");
              },
              onSuccess : function(response) {
                var activator = item.up('.add_xobject');
                if(activator) {
                  var insertedElement;
                  // We don't use Prototype API here because we wan't to move the CSS/JavaScript includes to the page head.
                  var responseDocumentFragment = this._parseHTMLResponse(response.responseText);

                  if (activator.up('.xclass')) {
                    // Using plain JavaScript here because Prototype doesn't know how to insert a document fragment..
                    activator.up().insertBefore(responseDocumentFragment, activator);
                    insertedElement = activator.previous();
                  } else {
                    activator.up().insertBefore(responseDocumentFragment, activator.nextSibling);
                    insertedElement = activator.next();
                  }

                  if (insertedElement) {
                    // Notify the listeners that the DOM has been updated. This is needed in order to have pickers.
                    document.fire('xwiki:dom:updated', {elements: [insertedElement]});
                    var insertedObject;
                    if (insertedElement.hasClassName('xclass')) {
                      this.enhanceClassUX(insertedElement, false);
                      insertedObject = insertedElement.down('.xobject');
                    } else if (insertedElement.hasClassName('xobject')) {
                      var classId = insertedElement.id.replace(/^xobject_/, "xclass_").replace(/_\d+$/, "");

                      // clean up the deletion array if we add back a deleted object.
                      var xclassName = this.getXClassNameFromXObjectId(insertedElement.id);
                      if (this.editorStatus.deletedXObjects[xclassName] !== undefined) {
                        var deletionArray = this.editorStatus.deletedXObjects[xclassName];
                        // be sure to remove the requested object number from the array first.
                        if (deletionArray.indexOf(objectNumberVal) !== -1) {
                          this.removeElementFromArray(deletionArray, objectNumberVal);
                          this.getDeletedXObject(xclassName, objectNumberVal).remove();
                        }
                        if (deletionArray.length === 0) {
                          delete this.editorStatus.deletedXObjects[xclassName];
                        }
                      }
                      this.enhanceObjectUX(insertedElement, false);
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
                    // We don't display the edit link on newly added object until they have been saved.
                    insertedObject.down('.xobject-action.edit').hide();
                  }
                }
                item.notification.replace(new XWiki.widgets.Notification("$services.localization.render('core.editors.object.add.done')", "done"));
              }.bind(this),
              onFailure : function(response) {
                var failureReason = response.statusText || 'Server not responding';
                item.notification.replace(new XWiki.widgets.Notification("$services.localization.render('core.editors.object.add.failed')" + failureReason, "error"));
              },
              onComplete : function() {
                item.disabled = false;
                require(['xwiki-meta'], function (xm) {
                  xm.refreshVersion();
                });
                document.fire('xwiki:dom:refresh');
              },
              // 0 is returned for network failures.
              on0 : function(response) {
                response.request.options.onFailure(response);
              }
            }
          );
        }
      }.bindAsEventListener(this));
    }.bind(this));
  },
  /**
   * Parses the given HTML, moving the CSS/JavaScript includes in the page head.
   *
   * @param html the HTML to parse
   * @return the document fragment corresponding to the given HTML
   */
  _parseHTMLResponse : function(html) {
    // We don't use Element#update() because it doesn't move external scripts and sheets into HEAD and also because we
    // don't want to support in-line scripts in property displayers.
    var container = new Element('div');
    container.innerHTML = html;
    var head = document.body.previous('head');
    head && container.select('link').each(function(link) {
      head.insert(link);
    });
    head && container.select('script').each(function(script) {
      if (script.src) {
        // The script is not fetched if we simply move the script element.
        head.insert(new Element('script', {type: script.type, src: script.readAttribute('src')}));
      }
      script.remove();
    });
    return this._extractContents(container);
  },
  /**
   * Extracts the children of the given element into a document fragment.
   *
   * @param element a DOM element
   * @return a document fragment containing all the children of the given element, including text nodes
   */
  _extractContents : function(element) {
    var documentFragment = element.ownerDocument.createDocumentFragment();
    for(; element.firstChild; documentFragment.appendChild(element.firstChild)) {};
    return documentFragment;
  },
  // ------------------------------------
  // Ajax object deletion
  ajaxObjectDeletion : function(object) {
    var item = object.down('a.delete');
    var xclassName = this.getXClassNameFromXObjectId(object.id);
    var addedObjects = this.editorStatus.addedXObjects[xclassName];

    // if we have more than one object of the same type added, we always want the latest want to be deleted
    // so we hide the delete link for previous one.
    if (addedObjects !== undefined && addedObjects.length > 1) {
      var previousObjectNumber = addedObjects[addedObjects.length - 2];
      this.getXObject(xclassName, previousObjectNumber).down('a.delete').hide();
    }

    item.observe('click', function(event) {
      item.blur();
      event.stop();
      new XWiki.widgets.ConfirmationBox({
        onYes: function () {
          if (!item.disabled) {
            var form = item.up('form');
            var xobjectElement = item.up('.xobject');
            var xclassName = this.getXClassNameFromXObjectId(xobjectElement.id);
            var xObjectNumber = this.getXObjectNumberFromXObjectId(xobjectElement.id);
            var addedObjects = this.editorStatus.addedXObjects[xclassName];

            // if the object was already saved, then we need to add the right form information to delete it on server
            if (addedObjects === undefined || addedObjects.indexOf(xObjectNumber) === -1) {
              if (this.editorStatus.deletedXObjects[xclassName] === undefined) {
                this.editorStatus.deletedXObjects[xclassName] = [];
              }
              this.editorStatus.deletedXObjects[xclassName].push(xObjectNumber);
              this.editorStatus.deletedXObjects[xclassName].sort(this.numberSort);
              var deletedObject = new Element('input', {
                'type': 'hidden',
                'name': 'deletedObjects',
                'id': 'deletedObject_' + xclassName + '_' + xObjectNumber,
                'value': xclassName + '_' + xObjectNumber
              });
              form.appendChild(deletedObject);
              this.removeElementFromArray(this.editorStatus.savedXObjects[xclassName], xObjectNumber);
              if (this.editorStatus.savedXObjects[xclassName].length === 0) {
                delete this.editorStatus.savedXObjects[xclassName];
              }
              // if the object wasn't already saved, then we need to enable back the delete link on the previous added element
            } else {
              this.removeElementFromArray(addedObjects, xObjectNumber);
              if (addedObjects.length === 0) {
                delete this.editorStatus.addedXObjects[xclassName];
              } else {
                this.getXObject(xclassName, addedObjects.last()).down('a.delete').show();
              }
            }
            var xclassElement = xobjectElement.up('.xclass');
            xobjectElement.remove();

            this.updateXObjectCount(xclassElement);
          }
        }.bind(this),
      }, {
        confirmationText: "$services.localization.render('core.editors.object.delete.confirmJS')",
        // Allow the users to cancel the switch.
        showCancelButton: true
      });
    }.bindAsEventListener(this));
  },
  // -----------------------------------------------
  /* AJAX removal of deprecated properties */
  ajaxRemoveDeprecatedProperties : function(container, triggerSelector) {
    // Should never happen, but helpful for tests.
    if (!container) {
      return;
    }
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
                item.notification = new XWiki.widgets.Notification("$services.localization.render('core.editors.object.removeDeprecatedProperties.inProgress')", "inprogress");
              },
               onSuccess : function(response) {
                // Remove deprecated properties box
                container.select(".deprecatedProperties").invoke("remove");
                item.notification.replace(new XWiki.widgets.Notification("$services.localization.render('core.editors.object.removeDeprecatedProperties.done')", "done"));
              },
              onFailure : function(response) {
                var failureReason = response.statusText || 'Server not responding';
                item.notification.replace(new XWiki.widgets.Notification("$services.localization.render('core.editors.object.removeDeprecatedProperties.failed')" + failureReason, "error"));
              },
              onComplete : function() {
                item.disabled = false;
              },
              // 0 is returned for network failures.
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
      item.observe('click', function(event) {
        item.blur();
        event.stop();
        if (!item.disabled && item._x_propnameElt.value != '' && item._x_proptypeElt.selectedIndex >= 0) {
          var editURL = this.editedDocument.getURL('edit', Object.toQueryString({
            xpage: 'editclass',
            xaction: 'displayProperty',
            propName: item._x_propnameElt.value
          }));
          var ref = this.editedDocument.getURL('propadd', Object.toQueryString({
            propname: item._x_propnameElt.value,
            proptype: item._x_proptypeElt.options[item._x_proptypeElt.selectedIndex].value,
            xredirect: editURL,
            form_token: item._x_form_tokenElt.value
          }));
          new Ajax.Request(
            /* Ajax request URL */
            ref,
            /* Ajax request parameters */
            {
              onCreate : function() {
                item.disabled = true;
                item.notification = new XWiki.widgets.Notification("$services.localization.render('core.editors.class.addProperty.inProgress')", "inprogress");
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
                item.notification.replace(new XWiki.widgets.Notification("$services.localization.render('core.editors.class.addProperty.done')", "done"));
              }.bind(this),
              onFailure : function(response) {
                var failureReason = response.responseText;
                item.notification.replace(new XWiki.widgets.Notification("$services.localization.render('core.editors.class.addProperty.failed') " + failureReason, "error"));
              },
              onComplete : function() {
                item.disabled = false;
              },
              // 0 is returned for network failures.
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
            confirmationText: "$services.localization.render('core.editors.class.deleteProperty.confirm')",
            progressMessageText : "$services.localization.render('core.editors.class.deleteProperty.inProgress')",
            successMessageText : "$services.localization.render('core.editors.class.deleteProperty.done')",
            failureMessageText : "$services.localization.render('core.editors.class.deleteProperty.failed')"
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
    var xobjectCount = xclass.select('.xobject').length;
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
    object.addClassName('collapsable');
    var objectContent = object.down('.xobject-content');

    if (objectContent.childElementCount === 0) {
      object.addClassName('collapsed');
    }
    var objectToggle = object.down('.xobject-title h3 button');
    var xclassName = this.getXClassNameFromXObjectId(object.id);
    var xObjectNumber = this.getXObjectNumberFromXObjectId(object.id);
    objectToggle.observe('click', function() {
      var isAlreadyLoaded = objectContent.childElementCount > 0;
      if (!isAlreadyLoaded && !object.hasClassName('loading')) {
        object.addClassName('loading');
        var editURL = this.editedDocument.getURL('edit', Object.toQueryString({
          xpage: 'editobject',
          xaction: 'loadObject',
          classname: xclassName,
          objectNumber: xObjectNumber,
          form_token: $$('input[name=form_token]')[0].value
        }));
        new Ajax.Request(
          /* Ajax request URL */
          editURL,
          /* Ajax request parameters */
          {
            onCreate : function() {
              object.notification = new XWiki.widgets.Notification("$services.localization.render('core.editors.object.loadObject.inProgress')", "inprogress");
            },
            onSuccess : function(response) {
              // We don't use Prototype API here because we wan't to move the CSS/JavaScript includes to the page head.
              var responseDocumentFragment = this._parseHTMLResponse(response.responseText);
              // Using plain JavaScript here because Prototype doesn't know how to insert a document fragment.
              objectContent.insertBefore(responseDocumentFragment, null);

              // display the elements before firing the event to be sure they are visible.
              object.toggleClassName('collapsed');
              document.fire('xwiki:dom:updated', {elements: [objectContent]});
              object.removeClassName('loading');
              object.notification.replace(new XWiki.widgets.Notification("$services.localization.render('core.editors.object.loadObject.done')", "done"));
            }.bind(this),
            onFailure : function(response) {
              var failureReason = response.statusText || 'Server not responding';
              object.removeClassName('loading');
              object.notification.replace(new XWiki.widgets.Notification("$services.localization.render('core.editors.object.loadObject.failed') " + failureReason, "error"));
            },
            // 0 is returned for network failures.
            on0 : function(response) {
              response.request.options.onFailure(response);
            }
          }
        );
      } else {
        object.toggleClassName('collapsed');
      }
    }.bindAsEventListener(this));
  },
  // ------------------------------------
  //  Expand/collapse classes
  expandCollapseClass : function(xclass) {
    // Classes are expanded by default
    var xclassTitle = xclass.down('.xclass-title');
    if (!xclassTitle) {
      // No objects...
      return;
    }
    xclass.addClassName('collapsable');
    xclassTitle.observe('click', function(event) {
      xclassTitle.up().toggleClassName('collapsed');
    }.bindAsEventListener());
  },
  // ------------------------------------
  // Class editor: expand-collapse meta properties
  expandCollapseMetaProperty : function(property) {
    var propertyTitle = property.down('.xproperty-title');
    if (!propertyTitle) {
      // No such object...
      return;
    }
    property.addClassName('collapsable');
    property.addClassName('collapsed');
    propertyTitle.observe('click', function(event) {
      propertyTitle.up().toggleClassName('collapsed');
    }.bindAsEventListener());
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
      var movebutton = new Element('button', {
        'class': 'btn btn-secondary btn-xs tool move',
        title: $jsontool.serialize($services.localization.render('core.editors.class.moveProperty.handle.label'))
      }).update('<span class="sr-only">' +
        $jsontool.serialize($services.localization.render('core.editors.class.moveProperty.handle.label')) +
        '</span>' +
        $jsontool.serialize($services.icon.renderHTML('reposition')));
      item.makePositioned();
      item.appendChild(movebutton);
      movebutton.observe('click', function(event) {
        event.stop();
      }.bindAsEventListener());
      // Extend the Sortable items with arrow keys support
      movebutton.observe('keydown', function(event){
        // We only do something when the key is ArrowUp (keyCode: 38) or ArrowDown (keyCode: 40)
        if (![38, 40].includes(event.keyCode)) { return; }
        var sequence = Sortable.sequence($('xclassContent'));
        var sequenceLength = sequence.length;
        var item = event.target.parentElement.parentElement.parentElement;
        /* We need to recompute the name of the entry similarly to what's done in scriptaculous
          to make sure it matches the one we get in the sequence. 
          See https://github.com/madrobby/scriptaculous/blob/b0a8422f7f6f2e2e17f0d5ddfef1d9a6f5428472/src/dragdrop.js#L592 
          for the source of the format regex. */
        let format = /^[^_\-](?:[A-Za-z0-9\-\_]*)[_](.*)$/;
        let currentElementName = item.id.match(format) ? item.id.match(format)[1] : '';
        let currentElementIndex = sequence.indexOf(currentElementName);
        let swapWithIndex;
        if (event.keyCode === 38) { // UP ARROW
          swapWithIndex = (currentElementIndex + sequenceLength - 1) % sequenceLength;
        } else if (event.keyCode === 40) { // DOWN ARROW
          swapWithIndex = (currentElementIndex + sequenceLength + 1) % sequenceLength;
        }
        [ sequence[currentElementIndex], sequence[swapWithIndex] ] = [ sequence[swapWithIndex], sequence[currentElementIndex] ];
        /* We update the content of the sortable object.*/
        Sortable.setSequence($('xclassContent'), sequence);
        /* We make sure to trigger the listener that should be called with such an update. */
        Sortable.sortables['xclassContent'].onUpdate(Sortable.sortables['xclassContent'].containment);
        /* We refocus the element, so that the focus isn't lost when moving it. */
        event.target.focus();
      }.bindAsEventListener());
    });
    // Attach behavior to the move buttons
    Sortable.create($('xclassContent'), {
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
    for (var i = 0; i < children.length; ++i) {
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
  require(['scriptaculous/dragdrop'], function() {
    new editors.XDataEditors()
  });
  return true;
}

// When the document is loaded, create the Autosave control
(XWiki.domIsLoaded && init())
|| document.observe('xwiki:dom:loaded', init);

// End XWiki augmentation.
return XWiki;
}(XWiki || {}));

// Class Switcher
require(['jquery', 'xwiki-events-bridge'], function($) {
  $('#switch-xclass').on('change', function(event) {
    var selectedClass = $(event.target).val();
    if (selectedClass) {
      var selectedClassReference = XWiki.Model.resolve(selectedClass, XWiki.EntityType.DOCUMENT,
        XWiki.currentDocument.documentReference);
      var selectedClassURL = new XWiki.Document(selectedClassReference).getURL('edit', 'editor=class');
      var switchClass = function() {
        window.self.location = selectedClassURL;
      };
      new XWiki.widgets.ConfirmationBox({
        onYes: function() {
          // Save the current class before switching.
          $(document).trigger('xwiki:actions:save', {
            'continue': true,
            'form': $('#propupdate')[0]
          });
          $(document).on('xwiki:document:saved', switchClass);
        },
        // Switch without saving the current class.
        onNo: switchClass
      }, {
        confirmationText: "$services.localization.render('core.editors.class.switchClass.confirm')",
        // Allow the users to cancel the switch.
        showCancelButton: true
      });
    }
  });
});
