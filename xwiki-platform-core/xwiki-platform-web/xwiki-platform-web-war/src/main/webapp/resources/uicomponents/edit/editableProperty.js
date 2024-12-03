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
#set ($l10nKeys = [
  'edit',
  'cancel',
  'save',
  'core.editors.saveandcontinue.notification.inprogress',
  'core.editors.saveandcontinue.notification.done',
  ['core.editors.saveandcontinue.notification.error', '<span id="saveFailureReason"></span>'],
  'web.editableProperty.editFailed',
  'web.editableProperty.viewFailed'
])
#set ($l10n = {})
#foreach ($key in $l10nKeys)
  #set ($params = $key.subList(1, $key.size()))
  #if ($params)
    #set ($discard = $l10n.put($key[0], $services.localization.render($key[0], $params)))
  #else
    #set ($discard = $l10n.put($key, $services.localization.render($key)))
  #end
#end
#set ($iconNames = ['pencil', 'cross', 'check'])
#set ($icons = {})
#foreach ($iconName in $iconNames)
  #set ($discard = $icons.put($iconName, $services.icon.renderHTML($iconName)))
#end
#[[*/
// Start JavaScript-only code.
(function(l10n, icons) {
  "use strict";

define('editableProperty', ['jquery', 'xwiki-meta'], function($, xcontext) {
  $.fn.editableProperty = function() {
    return this.each(init);
  };

  // Initialization

  var init = function() {
    // Maybe add the action icons.
    if ($(this).find('.editableProperty-edit').length === 0) {
      var editIcon = $('<a href="#editProperty" class="editableProperty-edit"></a>');
      editIcon.attr('title', l10n.edit);
      editIcon.html(icons.pencil);

      var cancelIcon = $('<a href="#cancelProperty" class="editableProperty-cancel"></a>');
      cancelIcon.attr('title', l10n.cancel);
      cancelIcon.hide().html(icons.cross);

      var saveIcon = $('<a href="#saveProperty" class="editableProperty-save"></a>');
      saveIcon.attr('title', l10n.save);
      saveIcon.hide().html(icons.check);

      // Insert the action icons right after the label because it may be followed by a hint on the next line.
      $(this).find('label').after(editIcon, cancelIcon, saveIcon);
    }

    // Mark the viewer.
    var viewer = $(this).next('dd').addClass('editableProperty-viewer');

    // Prepare the editor. The 'form' CSS class is important because it ensures the action events (e.g.
    // xwiki:actions:beforeSave or xwiki:actions:cancel) don't have effects outside of this element. For instance,
    // saving or canceling one editable property shouldn't affect the other properties or the other forms on the page.
    $('<dd class="editableProperty-editor form"></dd>').hide().insertAfter(viewer);
  };

  // Event listeners

  $(document).on('click.editProperty', '.editableProperty-edit', function(event) {
    event.preventDefault();
    var editIcon = $(this);
    if (!editIcon.hasClass('disabled')) {
      edit(editIcon.closest('.editableProperty'));
    }
  });

  $(document).on('click.saveProperty', '.editableProperty-save', function(event) {
    event.preventDefault();
    var saveIcon = $(this);
    if (!saveIcon.hasClass('disabled')) {
      saveAndView(saveIcon.closest('.editableProperty'));
    }
  });

  $(document).on('click.cancelProperty', '.editableProperty-cancel', function(event) {
    event.preventDefault();
    var cancelIcon = $(this);
    if (!cancelIcon.hasClass('disabled')) {
      cancel(cancelIcon.closest('.editableProperty'));
    }
  });

  // Actions

  var edit = function(editableProperty) {
    // Disable the edit action while we load the editor.
    var editIcon = editableProperty.find('.editableProperty-edit').addClass('disabled');
    return Promise.resolve($.get(XWiki.currentDocument.getURL('get'), {
      xpage: 'display',
      mode: 'edit',
      property: editableProperty.data('property'),
      type: editableProperty.data('propertyType'),
      objectPolicy: editableProperty.data('objectPolicy'),
      language: xcontext.locale
    })).then(html => {
      // Replace the edit action with the save and cancel actions.
      editIcon.hide();
      editableProperty.find('.editableProperty-save, .editableProperty-cancel').show();
      // Update the editor.
      var editor = editableProperty.next('.editableProperty-viewer').hide().next('.editableProperty-editor');
      editor.html(html).show();
      // Allow others to enhance the editor.
      $(document).trigger('xwiki:dom:updated', {'elements': editor.toArray()});
      // Focus the first visible input.
      var editInput = editor.find(':input').filter(':visible');
      // If we cannot find any kind of editInput, then we're in a weird edge case
      // and we don't want to apply any of the following changes.
      if(!editInput) { return; }
      editInput.focus();
      // Make sure the edit input has an ID, and use the name of the input as a fallback
      if (!editInput.attr('id')) {
        editInput.attr('id', editInput.attr('name'));
      }
      // Bind the label to the newly generated edit input
      editableProperty.find('label').attr('for',editInput.attr('id'));
    }).catch(() => {
      new XWiki.widgets.Notification(l10n['web.editableProperty.editFailed'], 'error');
      return Promise.reject();
    }).finally(() => {
      // Re-enable the edit action (even if hidden).
      editIcon.removeClass('disabled');
    });
  };

  var cancel = function(editableProperty) {
    editableProperty.next('.editableProperty-viewer').show()
      // Notify the widgets used to edit the property that the user has canceled the edit. This allows the widgets to
      // perform some cleanup before the editor is destroyed. We trigger the event only if the editor is not already
      // destroyed.
      .next('.editableProperty-editor').filter(':visible').trigger('xwiki:actions:cancel').hide();
    editableProperty.find('.editableProperty-save, .editableProperty-cancel').hide();
    editableProperty.find('.editableProperty-edit').show();
    // Remove the for attribute from the label, resetting it to its default state
    editableProperty.find('label').removeAttr('for');
  };

  var save = function(editableProperty) {
    // Disable the save and cancel actions while the property is being saved.
    editableProperty.find('.editableProperty-save, .editableProperty-cancel').addClass('disabled');
    // Remove the for attribute from the label, resetting it to its default state
    editableProperty.find('label').removeAttr('for');
    // Show progress notification message.
    var notification = new XWiki.widgets.Notification(l10n['core.editors.saveandcontinue.notification.inprogress'],
      'inprogress');
    // Collect the submit data.
    var editor = editableProperty.next('.editableProperty-viewer').next('.editableProperty-editor');
    // Notify the others that we're about to save so that they have the chance to update the submit data.
    editor.trigger('xwiki:actions:beforeSave');
    var data = editor.find(':input').serializeArray();
    data.push({name: 'language', value: xcontext.locale});
    data.push({name: 'comment', value: 'Update property ' + editableProperty.data('property')});
    data.push({name: 'minorEdit', value: true});
    data.push({name: 'form_token', value: xcontext.form_token});
    data.push({name: 'ajax', value: true});
    data.push({name: 'objectPolicy', value: editableProperty.data('objectPolicy')});
    // Make the request to save the property.
    return Promise.resolve($.post(XWiki.currentDocument.getURL('save'), data)).then(() => {
      editor.trigger('xwiki:document:saved');
      notification.replace(new XWiki.widgets.Notification(l10n['core.editors.saveandcontinue.notification.done'],
        'done'));
    }).catch(response => {
      editor.trigger('xwiki:document:saveFailed');
      notification.replace(new XWiki.widgets.Notification(l10n['core.editors.saveandcontinue.notification.error'],
        'error'));
      $('#saveFailureReason').text(response.statusText);
      return Promise.reject();
    }).finally(() => {
      // Re-enable the save and cancel actions.
      editableProperty.find('.editableProperty-save, .editableProperty-cancel').removeClass('disabled');
    });
  };

  var view = function(editableProperty) {
    return Promise.resolve($.get(XWiki.currentDocument.getURL('get'), {
      xpage: 'display',
      property: editableProperty.data('property'),
      type: editableProperty.data('propertyType'),
      objectPolicy: editableProperty.data('objectPolicy'),
      language: xcontext.locale
    })).then(html => {
      // Update the viewer.
      var viewer = editableProperty.next('.editableProperty-viewer').html(html);
      // Cancel the edit if needed.
      cancel(editableProperty);
      // Allow others to enhance the viewer.
      $(document).trigger('xwiki:dom:updated', {'elements': viewer.toArray()});
    }).catch(() => {
      new XWiki.widgets.Notification(l10n['web.editableProperty.viewFailed'], 'error');
      return Promise.reject();
    });
  };

  var saveAndView = function(editableProperty) {
    return save(editableProperty).then(view.bind(null, editableProperty));
  };
});

require(['jquery', 'editableProperty', 'xwiki-events-bridge'], function($) {
  var init = function(event, data) {
    var container = $((data && data.elements) || document);
    container.find('dt.editableProperty').editableProperty();
  };

  $(document).on('xwiki:dom:updated', init);
  $(init);
});

// End JavaScript-only code.
}).apply(']]#', $jsontool.serialize([$l10n, $icons]));
