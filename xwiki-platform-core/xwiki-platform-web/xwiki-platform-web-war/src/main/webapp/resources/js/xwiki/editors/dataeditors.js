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
  'jquery-ui': $services.webjars.url('jquery-ui', "jquery-ui#if ($services.debug.minify).min#{end}.js")
})
#set ($icons = {'reposition': $services.icon.renderHTML('reposition')})
#[[*/
// Start JavaScript-only code.

(function(icons, paths) {
  "use strict";
  require.config({
    paths
  });
  define('dataeditors-translations', {
    prefix: 'core.editors.',
    keys: [
      'object.add.inProgress',
      'object.add.done',
      'object.add.failed',
      'object.delete.confirmJS',
      'object.removeDeprecatedProperties.inProgress',
      'object.removeDeprecatedProperties.done',
      'object.removeDeprecatedProperties.failed',
      'class.addProperty.inProgress',
      'class.addProperty.done',
      'class.addProperty.failed',
      'class.deleteProperty.confirm',
      'class.deleteProperty.inProgress',
      'class.deleteProperty.done',
      'class.deleteProperty.failed',
      'object.loadObject.inProgress',
      'object.loadObject.done',
      'object.loadObject.failed',
      'class.moveProperty.handle.label',
      'class.switchClass.confirm'
    ]
  });
  require(['jquery','xwiki-meta','xwiki-l10n!dataeditors-translations','xwiki-events-bridge','jquery-ui'], function ($, xm, l10n) {
    class XDataEditors {
      constructor() {
        let self = this;
        // Maintain informations about actions performed on the editor.
        // Data abouts xobjects are map whom keys are xclass names and values are list of sorted xobjects ids
        this.editorStatus = {
          savedXObjects: {}, // already saved objects
          addedXObjects: {}, // objects added but not saved yet
          deletedXObjects: {} // objects deleted but not removed yet
        };
        this.editedDocument = XWiki.currentDocument;
        this.unsavedChanges = false;

        $('.xclass').each(function() {
          self.enhanceClassUX($(this), true);
        });
        this.ajaxObjectAdd($('#add_xobject'));
        this.ajaxPropertyAdd();
        this.makeSortable($('#xwikiclassproperties'));
        this.ajaxRemoveDeprecatedProperties($("body"), ".syncAllProperties");

        // everytime the document is saved we restore the current state to be in sync with the server
        $(document).on('xwiki:document:saved', function () {
          // Remove fields about deleted and added objects
          $('input[name=deletedObjects]').each(function() {
            $(this).remove();
          });
          $('input[name=addedObjects]').each(function() {
            $(this).remove();
          });

          // Show hidden links about edit and delete
          // Edit link is hidden on not-saved objects since it needs to be saved on the server.
          // Delete links are only shown for latest added object of an xclass to keep a good numbering.
          $('.xobject-action.edit').show();
          $('.xobject a.delete').show();

          // We need to put all info about added objects in the saved objects collection.
          // We don't need to do that for deleted objects since we cannot revert a deletion in the UI except by canceling
          // the changes. So we directly manipulate the savedXObjects collection when performing a deletion.
          for (let xclassName in self.editorStatus.addedXObjects) {
            if (self.editorStatus.savedXObjects[xclassName] === undefined) {
              self.editorStatus.savedXObjects[xclassName] = self.editorStatus.addedXObjects[xclassName];
            } else {
              self.editorStatus.savedXObjects[xclassName].concat(self.editorStatus.addedXObjects[xclassName]);
              self.editorStatus.savedXObjects[xclassName].sort(self.numberSort);
            }
          }
          self.editorStatus.addedXObjects = {};
          self.editorStatus.deletedXObjects = {};
          self.unsavedChanges = false;
        });

        // in case of cancel we just clean everything so that we don't get any warnings for leaving the page without saving.
        $(document).on('xwiki:actions:cancel', function () {
          // Remove fields about deleted and added objects
          $('input[name=deletedObjects]').remove();
          $('input[name=addedObjects]').remove();
          self.editorStatus.addedXObjects = {};
          self.editorStatus.deletedXObjects = {};
          self.unsavedChanges = false;
        });
        // We want to listen on inputs related to an xclass or an xobject, but not the actual inputs allowing
        // to create a property or an object.
        let filterInputs = function () {
            return $(this).parents('#add_xproperty,#add_xobject').length === 0
                && $(this).parents('.xclass').length > 0;
        };
        $('input,textarea').filter(filterInputs).on('change', function(e) {
          self.unsavedChanges = true;
        });
        $(document).on('xwiki:dom:updated', function (event, data) {
          $(data.elements).find('input,textarea').filter(filterInputs).on('change', function (e) {
            self.unsavedChanges = true;
          });
        });

        // We want to the user to be prevented if he tries to leave the editor before saving.
        // We cannot use jQuery style to listen on event for this one as apparently it's not working
        // See: https://stackoverflow.com/questions/4376596/jquery-unload-or-beforeunload
        window.onbeforeunload = function(event) {
          if (Object.keys(self.editorStatus.addedXObjects).length > 0
            || Object.keys(self.editorStatus.deletedXObjects).length > 0
            || self.unsavedChanges) {
            event.preventDefault();
            event.returnValue = "";
          } else {
            return;
          }
        };
      }

      /**
       * Sort function to allow sorting an array of integer.
       * Using [3, 0, 1].sort(numberSort) produces [0, 1, 3].
       */
      numberSort(a, b) {
        return a - b;
      }

      /**
       * Extract the xclass name from the XDOM id of the xclass.
       */
      getXClassNameFromXClassId(xclassId) {
        return xclassId.substring("xclass_".length);
      }

      /**
       * Extract the xclass name from the XDOM id of an xobject.
       */
      getXClassNameFromXObjectId(xobjectId) {
        return xobjectId.substring("xobject_".length, xobjectId.lastIndexOf('_'));
      }

      /**
       * Extract the xobject number from the XDOM id of an xobject.
       */
      getXObjectNumberFromXObjectId(xobjectId) {
        return xobjectId.substring(xobjectId.lastIndexOf('_') + 1);
      }

      /**
       * Returns true if an object exists with the given classname and number.
       */
      xObjectAlreadyExist(xclassName, objectNumber) {
        return (this.editorStatus.savedXObjects[xclassName] !== undefined
          && this.editorStatus.savedXObjects[xclassName].indexOf(objectNumber) !== -1)
          || (this.editorStatus.addedXObjects[xclassName] !== undefined
            && this.editorStatus.addedXObjects[xclassName].indexOf(objectNumber) !== -1);
      }

      /**
       * Returns the xobject DOM element for the given class and number.
       */
      getXObject(xclassName, objectNumber) {
        let expectedId = CSS.escape('xobject_' + xclassName + '_' + objectNumber);
        return $('#' + expectedId);
      }

      getDeletedXObject(xclassName, objectNumber) {
        let expectedId = CSS.escape('deletedObject_' + xclassName + '_' + objectNumber);
        return $('input[name=deletedObjects]#' + expectedId);
      }

      /**
       * Helper to remove an element from an array if and only if it was already in the array.
       */
      removeElementFromArray(array, element) {
        if (array.indexOf(element) !== -1) {
          array.splice(array.indexOf(element), 1);
        }
      }

      /**
       * Compute the new object number for a class name given the information we already have about savedXObjects and
       * addedXObjects.
       */
      getNewObjectNumber(xclassName) {
        let objectNumberVal;
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
        let deletedArray = this.editorStatus.deletedXObjects[xclassName];
        if (deletedArray !== undefined && deletedArray.indexOf(objectNumberVal) !== -1) {
          this.getDeletedXObject(xclassName, objectNumberVal).remove();
          this.removeElementFromArray(deletedArray, objectNumberVal);
          if (deletedArray.length === 0) {
            delete this.editorStatus.deletedXObjects[xclassName];
          }
        }
        return objectNumberVal;
      }

      /**
       * Enhance xclass for the JS behaviours and iterate over inner xobjects or xproperties to enhance them.
       *
       * @param xclass the xclass DOM element
       * @param init true if it's the call performed during the script initialization.
       */
      enhanceClassUX(xclass, init) {
        this.ajaxObjectAdd(xclass);
        this.expandCollapseClass(xclass);

        let self = this;
        xclass.find('.xproperty').each(function() {
          let item = $(this);
          self.expandCollapseMetaProperty(item);
          self.ajaxPropertyDeletion(item);
          self.makeDisableVisible(item);
        });


        // We always iterate on the xobjects of the xclass to enhance them.
        xclass.find('.xobject').each(function() {
          self.enhanceObjectUX($(this), init);
        });
      }

      enhanceObjectUX(object, init) {
        let xclassName = this.getXClassNameFromXObjectId(object.attr('id'));
        let objectNumber = this.getXObjectNumberFromXObjectId(object.attr('id'));
        let listObjects;

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
            let addedObject = $('<input/>', {
              'type': 'hidden',
              'name': 'addedObjects',
              'id': 'addedObject_' + xclassName + '_' + objectNumber,
              'value': xclassName + '_' + objectNumber
            });
            object.append(addedObject);
          }
          this.ajaxObjectDeletion(object);
          this.editButtonBehavior(object);
          this.expandCollapseObject(object);
          this.ajaxRemoveDeprecatedProperties(object, ".syncProperties");
        }
      }

      // -----------------------------------------------
      /* AJAX object add */
      ajaxObjectAdd(element) {
        if (!element) {
          return;
        }
        let self = this;
        element.find('.xobject-add-control').each(function() {
          let item = $(this);
          item.on('click', function(event) {
            item.blur();
            event.preventDefault();
            let url, validClassName, classNameVal, objectNumberVal;
            if (item.attr('href')) {
              url = item.attr('href').replace(/[?&]xredirect=[^&]*/, '');
              validClassName = true;

              // We compute the object number based on the information we have in our recorded objects array.
              classNameVal = self.getXClassNameFromXClassId(item.parents('.xclass').attr('id'));
              url += "&objectNumber=" + self.getNewObjectNumber(classNameVal);
            } else {
              let classNameElt = element.find('select');
              classNameVal = classNameElt.val();
              validClassName = classNameVal && classNameVal !== '-';
              url = self.editedDocument.getURL(xm.action, Object.toQueryString({
                xpage: 'editobject',
                xaction: 'addObject',
                classname: classNameVal,
                objectNumber: self.getNewObjectNumber(classNameVal),
                form_token: xm.form_token
              }));
            }
            let notification;
            if (!item.prop('disabled') && validClassName) {
              item.prop('disabled', true);
              notification = new XWiki.widgets.Notification(l10n['object.add.inProgress'], "inprogress");
              $.post(url).done(function(data) {
                let activator = item.parents('.add_xobject');
                if (activator.length > 0) {
                  let insertedElement = $(data);
                  if (activator.parents('.xclass').length > 0) {
                    activator.before(insertedElement);
                  } else {
                    activator.after(insertedElement);
                  }

                  if (insertedElement) {
                    // Notify the listeners that the DOM has been updated. This is needed in order to have pickers.
                    // insertedElement might contain text node which are not properly interpreted by listeners
                    // that wait to specific html nodes
                    let updatedElements = $(insertedElement).filter(function() { return this.nodeType !== Node.TEXT_NODE}).toArray();
                    $(document).trigger('xwiki:dom:updated', {elements: updatedElements});
                    let insertedObject;
                    if (insertedElement.hasClass('xclass')) {
                      self.enhanceClassUX(insertedElement, false);
                      insertedObject = insertedElement.find('.xobject');
                    } else if (insertedElement.hasClass('xobject')) {
                      // clean up the deletion array if we add back a deleted object.
                      insertedObject = insertedElement.filter('.xobject');
                      // clean up the deletion array if we add back a deleted object.
                      let xclassName = self.getXClassNameFromXObjectId(insertedObject.attr('id'));
                      if (self.editorStatus.deletedXObjects[xclassName] !== undefined) {
                        let deletionArray = self.editorStatus.deletedXObjects[xclassName];
                        // be sure to remove the requested object number from the array first.
                        if (deletionArray.indexOf(objectNumberVal) !== -1) {
                          self.removeElementFromArray(deletionArray, objectNumberVal);
                          self.getDeletedXObject(xclassName, objectNumberVal).remove();
                        }
                        if (deletionArray.length === 0) {
                          delete self.editorStatus.deletedXObjects[xclassName];
                        }
                      }
                      self.enhanceObjectUX(insertedObject, false);
                      let classId = insertedObject.attr('id').replace(/^xobject_/, "xclass_").replace(/_\d+$/, "");
                      let xclass = $('#' + CSS.escape(classId));
                      if (xclass.length > 0) {
                        xclass.find('.add_xobject').before(insertedObject);
                        self.updateXObjectCount(xclass);
                      }
                      insertedObject = insertedElement;
                    }
                    // Expand the newly inserted object, since the user will probably want to edit it once it was added
                    insertedObject.removeClass('collapsed');
                    insertedObject.parents('.xclass').removeClass('collapsed');
                    // // We don't display the edit link on newly added object until they have been saved.
                    insertedObject.find('.xobject-action.edit').hide();
                  }
                }
                notification.replace(new XWiki.widgets.Notification(l10n['object.add.done'], "done"));
                $(document).trigger('xwiki:dom:refresh');
              }).fail(function (error) {
                let failureReason = error.responseText || 'Server not responding';
                notification.replace(new XWiki.widgets.Notification(l10n['object.add.failed'] + failureReason, "error"));
              }).always(function () {
                item.prop('disabled',false);
              });
            }
          });
        });
      }

      // ------------------------------------
      // Ajax object deletion
      ajaxObjectDeletion(object) {
        let item = object.find('a.delete');
        let xclassName = this.getXClassNameFromXObjectId(object.attr('id'));
        let addedObjects = this.editorStatus.addedXObjects[xclassName];

        // if we have more than one object of the same type added, we always want the latest want to be deleted
        // so we hide the delete link for previous one.
        if (addedObjects !== undefined && addedObjects.length > 1) {
          let previousObjectNumber = addedObjects[addedObjects.length - 2];
          this.getXObject(xclassName, previousObjectNumber).find('a.delete').hide();
        }

        let self = this;
        item.on('click', function(event) {
          item.blur();
          event.preventDefault();
          new XWiki.widgets.ConfirmationBox({
            onYes: function () {
              if (!item.disabled) {
                let form = item.parents('form');
                let xobjectElement = item.parents('.xobject');
                let xclassName = self.getXClassNameFromXObjectId(xobjectElement.attr('id'));
                let xObjectNumber = self.getXObjectNumberFromXObjectId(xobjectElement.attr('id'));
                let addedObjects = self.editorStatus.addedXObjects[xclassName];

                // if the object was already saved, then we need to add the right form information to delete it on server
                if (addedObjects === undefined || addedObjects.indexOf(xObjectNumber) === -1) {
                  if (self.editorStatus.deletedXObjects[xclassName] === undefined) {
                    self.editorStatus.deletedXObjects[xclassName] = [];
                  }
                  self.editorStatus.deletedXObjects[xclassName].push(xObjectNumber);
                  self.editorStatus.deletedXObjects[xclassName].sort(self.numberSort);
                  let deletedObject = $('<input/>', {
                    'type': 'hidden',
                    'name': 'deletedObjects',
                    'id': 'deletedObject_' + xclassName + '_' + xObjectNumber,
                    'value': xclassName + '_' + xObjectNumber
                  });
                  form.append(deletedObject);
                  self.removeElementFromArray(self.editorStatus.savedXObjects[xclassName], xObjectNumber);
                  if (self.editorStatus.savedXObjects[xclassName].length === 0) {
                    delete self.editorStatus.savedXObjects[xclassName];
                  }
                  // if the object wasn't already saved, then we need to enable back the delete link on the previous added element
                } else {
                  self.removeElementFromArray(addedObjects, xObjectNumber);
                  if (addedObjects.length === 0) {
                    delete self.editorStatus.addedXObjects[xclassName];
                  } else {
                    self.getXObject(xclassName, addedObjects.last()).find('a.delete').show();
                  }
                }
                let xclassElement = xobjectElement.parents('.xclass');
                xobjectElement.remove();

                self.updateXObjectCount(xclassElement);
              }
            },
          }, {
            confirmationText: l10n['object.delete.confirmJS'],
            // Allow the users to cancel the switch.
            showCancelButton: true
          });
        });
      }

      // -----------------------------------------------
      /* AJAX removal of deprecated properties */
      ajaxRemoveDeprecatedProperties(container, triggerSelector) {
        // Should never happen, but helpful for tests.
        if (!container) {
          return;
        }
        container.find(triggerSelector).each(function() {
          let item = $(this);
          item.on("click", function(event) {
            item.blur();
            event.stopPropagation();
            if (!item.disabled) {
              item.prop('disabled', true);
              let notification = new XWiki.widgets.Notification(l10n['object.removeDeprecatedProperties.inProgress'], "inprogress");
              $.post(item.href).done(function(data) {
                // Remove deprecated properties box
                container.find(".deprecatedProperties").remove();
                notification.replace(new XWiki.widgets.Notification(l10n['object.removeDeprecatedProperties.done'], "done"));
              }).fail(function (error) {
                let failureReason = error.responseText || 'Server not responding';
                notification.replace(new XWiki.widgets.Notification(l10n['object.removeDeprecatedProperties.failed'] + failureReason, "error"));
              }).always(function () {
                item.prop('disabled', false);
              });
            }
          });
        });
      }

      // -----------------------------------------------
      /* AJAX property add */
      ajaxPropertyAdd() {
        let self = this;
        $('input[name=action_propadd]').each(function(){
          let item = $(this);
          item.on('click', function(event) {
            let propname = $('#propname').val();
            let proptype = $('#proptype').val();
            item.blur();
            event.stopPropagation();
            if (!item.prop('disabled') && propname !== '' && proptype !== '') {
              let editURL = self.editedDocument.getURL(xm.action, Object.toQueryString({
                xpage: 'editclass',
                xaction: 'displayProperty',
                propName: propname
              }));
              let ref = self.editedDocument.getURL('propadd', Object.toQueryString({
                propname: propname,
                proptype: proptype,
                xredirect: editURL,
                form_token: xm.form_token
              }));
              item.prop('disabled', true);
              let notification = new XWiki.widgets.Notification(l10n['class.addProperty.inProgress'], "inprogress");
              $.post(ref).done(function(data) {
                $('#xclassContent').append(data);
                let insertedPropertyElt = $('#xclassContent > div.xproperty:last-child');
                // Expand the newly inserted property, since the user will probably want to edit it once it was added
                self.expandCollapseMetaProperty(insertedPropertyElt);
                // Make teh newly added property sortable
                self.makeSortable(insertedPropertyElt);
                self.ajaxPropertyDeletion(insertedPropertyElt);
                self.makeDisableVisible(insertedPropertyElt);
                notification.replace(new XWiki.widgets.Notification(l10n['class.addProperty.done'], "done"));
              }).fail(function (error) {
                // FIXME: we should change translation value for action.addClassProperty.error.invalidName
                let failureReason = error.responseText.replaceAll("&#60;br/&#62;", "<br/>") || 'Server not responding';
                notification.replace(new XWiki.widgets.Notification(l10n['class.addProperty.failed'] + failureReason, "error"));
              }).always(function () {
                item.prop('disabled', false);
              });
            }
          });
        });
      }

      // ------------------------------------
      // Ajax property deletion
      ajaxPropertyDeletion(property) {
        let item = property.find('a.delete');
        item.on('click', function(event) {
          item.blur();
          event.preventDefault();
          if (!item.disabled) {
            new XWiki.widgets.ConfirmedAjaxRequest(
              /* Ajax request URL */
              item.attr('href'),
              /* Ajax request parameters */
              {
                onCreate : function() {
                  item.prop('disabled', true);
                },
                onSuccess : function() {
                  property.remove();
                },
                onComplete : function() {
                  item.prop('disabled', false);
                }
              },
              /* Interaction parameters */
              {
                confirmationText: l10n['class.deleteProperty.confirm'],
                progressMessageText : l10n['class.deleteProperty.inProgress'],
                successMessageText : l10n['class.deleteProperty.done'],
                failureMessageText : l10n['class.deleteProperty.failed']
              }
            );
          }
        });
      }

      // ------------------------------------
      //
      makeDisableVisible(property) {
        property.find('.disabletool input').on("click", function() {
          property.toggleClass('disabled');
        })
      }

      // ------------------------------------
      // Edit button behavior
      // Prevent from collapsing the object subtree when clicking on edit
      editButtonBehavior(object) {
        let item = object.find('a.edit');
        if (!item) {
          return;
        }
        item.on('click', function(event) {
          item.blur();
          event.stopPropagation();
          window.location = item.href;
        });
      }

      // Update the number of objects displayed in the class group title, when objects are added or deleted
      updateXObjectCount(xclass) {
        let xobjectCount = xclass.find('.xobject').length;
        if (xobjectCount == 0) {
          xclass.remove();
        } else {
          let xobjectCountElement = xclass.find('.xclass_xobject_nb');
          if (typeof(xobjectCountElement) != 'undefined') {
            xobjectCountElement.text('(' + xobjectCount + ')');
          }
        }
      }

      // ------------------------------------
      // Expand/collapse objects and object properties
      expandCollapseObject(object) {
        let self = this;
        object.addClass('collapsable');
        let objectContent = object.find('.xobject-content');

        if (objectContent.children().length === 0) {
          object.addClass('collapsed');
        }
        let objectTitle = object.find('.xobject-title');
        let xclassName = this.getXClassNameFromXObjectId(object.attr('id'));
        let xObjectNumber = this.getXObjectNumberFromXObjectId(object.attr('id'));
        objectTitle.on('click', function(event) {
          let isAlreadyLoaded = objectContent.children().length > 0;
          if (!isAlreadyLoaded && !object.hasClass('loading')) {
            object.addClass('loading');
            let editURL = self.editedDocument.getURL(xm.action, Object.toQueryString({
              xpage: 'editobject',
              xaction: 'loadObject',
              classname: xclassName,
              objectNumber: xObjectNumber,
              form_token: xm.form_token
            }));
            let notification = new XWiki.widgets.Notification(l10n['object.loadObject.inProgress'], "inprogress");
            $.post(editURL).done(function(data) {
              objectContent.append(data);
              // display the elements before firing the event to be sure they are visible.
              object.toggleClass('collapsed');
              $(document).trigger('xwiki:dom:updated', {elements: objectContent.toArray()});
              notification.replace(new XWiki.widgets.Notification(l10n['object.loadObject.done'], "done"));
            }).fail(function (error) {
              let failureReason = error.responseText || 'Server not responding';
              notification.replace(new XWiki.widgets.Notification(l10n['object.loadObject.failed'] + failureReason, "error"));
            }).always(function () {
              object.removeClass('loading');
            });
          } else {
            object.toggleClass('collapsed');
          }
        });
      }

      // ------------------------------------
      //  Expand/collapse classes
      expandCollapseClass(xclass) {
        // Classes are expanded by default
        let xclassTitle = xclass.find('.xclass-title');
        if (!xclassTitle) {
          // No objects...
          return;
        }
        xclass.addClass('collapsable');
        xclassTitle.on('click', function() {
          xclassTitle.parent().toggleClass('collapsed');
        });
      }

      // ------------------------------------
      // Class editor: expand-collapse meta properties
      expandCollapseMetaProperty(property) {
        let propertyTitle = property.find('.xproperty-title');
        if (!propertyTitle) {
          // No such object...
          return;
        }
        property.addClass('collapsable');
        property.addClass('collapsed');
        propertyTitle.on('click', function() {
          propertyTitle.parent().toggleClass('collapsed');
        });
      }

      //---------------------------------------------------
      /* Class editor: xproperty ordering */
      makeSortable(element) {
        if (element.length > 0) {
          // Hide the property number, as ordering can be done by drag and drop
          element.find('.xproperty-content').each(function () {
            let item = $(this);
            item.find("input").each(function () {
              let input = $(this);
              if (input.attr('id') && input.attr('id').endsWith("_number")) {
                item.data('numberProperty', input);
                input.parent().hide();
                if (input.parent().prev('dt')) {
                  input.parent().prev('dt').hide();
                }
              }
            });
          });
          // Create and insert move button
          element.find('.xproperty-title .tools').each(function () {
            let item = $(this);
            let movebutton = $('<span>', {
              'class': 'tool move',
              title: l10n['class.moveProperty.handle.label']
            }).html(icons['reposition']);
            item.css('position', 'relative');
            item.append(movebutton);
            movebutton.on('click', function (event) {
              event.stopPropagation();
            });
          });
          let self = this;
          $('#xclassContent').sortable({
            cursor: 'move',
            items: '.xproperty',
            start: self.startDrag,
            stop: self.endDrag,
            update: self.updateOrder
          });
        }
      }

      updateOrder() {
        let i = 1;
        $(this).find(".xproperty-content").each(function () {
          let item = $(this);
          // the numberProperty data is actually a reference to an input.
          item.data('numberProperty').val(i++);
        });
      }

      startDrag() {
        $(this).addClass('dragged');
        $('#xclassContent').children().each(function() {
          let item = $(this);
          item.data('_expandedBeforeDrag', !item.hasClass('collapsed'));
          item.addClass('collapsed');
        });
      }

      endDrag() {
        $(this).removeClass('dragged');
        $('#xclassContent').children().each(function() {
          let item = $(this);
          if (item.data('_expandedBeforeDrag')) {
            item.removeClass('collapsed');
          }
        });
      }
    }

    let initSwitchClassListener = function () {
      $('#switch-xclass').on('change', function (event) {
        let selectedClass = $(event.target).val();
        if (selectedClass) {
          let selectedClassReference = XWiki.Model.resolve(selectedClass, XWiki.EntityType.DOCUMENT,
              XWiki.currentDocument.documentReference);
          let selectedClassURL = new XWiki.Document(selectedClassReference).getURL(xm.action, 'editor=class');
          let switchClass = function () {
            window.self.location = selectedClassURL;
          };
          new XWiki.widgets.ConfirmationBox({
            onYes: function () {
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
            confirmationText: l10n['class.switchClass.confirm'],
            // Allow the users to cancel the switch.
            showCancelButton: true
          });
        }
      });
    }

    function init() {
      XWiki = window.XWiki || {};
      XWiki.editors = XWiki.editors || {};
      XWiki.editors.XDataEditors = new XDataEditors();
      initSwitchClassListener();
    }
    $(init);
  });

// End JavaScript-only code.
}).apply(']]#', $jsontool.serialize([$icons, $paths]));