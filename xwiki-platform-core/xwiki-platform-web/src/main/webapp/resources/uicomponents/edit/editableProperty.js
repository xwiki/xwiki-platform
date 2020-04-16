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
define('editableProperty', ['jquery', 'xwiki-meta'], function($, xcontext) {
  $.fn.editableProperty = function() {
    return this.each(init);
  };

  // Initialization

  var init = function() {
    // Maybe add the action icons.
    if ($(this).find('.editableProperty-edit').length === 0) {
      var editIcon = $('<a href="#editProperty" class="editableProperty-edit"></a>');
      editIcon.attr('title', $jsontool.serialize($services.localization.render('edit')));
      editIcon.html($jsontool.serialize($services.icon.renderHTML('pencil')));

      var cancelIcon = $('<a href="#cancelProperty" class="editableProperty-cancel"></a>');
      cancelIcon.attr('title', $jsontool.serialize($services.localization.render('cancel')));
      cancelIcon.hide().html($jsontool.serialize($services.icon.renderHTML('cross')));

      var saveIcon = $('<a href="#saveProperty" class="editableProperty-save"></a>');
      saveIcon.attr('title', $jsontool.serialize($services.localization.render('save')));
      saveIcon.hide().html($jsontool.serialize($services.icon.renderHTML('check')));

      // Insert the action icons right after the label because it may be followed by a hint on the next line.
      $(this).find('label').after(editIcon, cancelIcon, saveIcon);
    }

    // Mark the viewer.
    var viewer = $(this).next('dd').addClass('editableProperty-viewer');

    // Prepare the editor.
    $('<dd class="editableProperty-editor"/>').hide().insertAfter(viewer);
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
    return $.get(XWiki.currentDocument.getURL('get'), {
      xpage: 'display',
      mode: 'edit',
      property: editableProperty.data('property'),
      type: editableProperty.data('propertyType'),
      language: xcontext.locale
    }).done(function(html) {
      // Replace the edit action with the save and cancel actions.
      editIcon.hide();
      editableProperty.find('.editableProperty-save, .editableProperty-cancel').show();
      // Update the editor.
      var editor = editableProperty.next('.editableProperty-viewer').hide().next('.editableProperty-editor');
      editor.html(html).show();
      // Allow others to enhance the editor.
      $(document).trigger('xwiki:dom:updated', {'elements': editor.toArray()});
      // Focus the first visible input.
      editor.find(':input').filter(':visible').focus();
    }).fail(function() {
      new XWiki.widgets.Notification(
        $jsontool.serialize($services.localization.render('Failed to edit property.')),
        'error'
      );
    }).always(function() {
      // Re-enable the edit action (even if hidden).
      editIcon.removeClass('disabled');
    });
  };

  var cancel = function(editableProperty) {
    editableProperty.next('.editableProperty-viewer').show().next('.editableProperty-editor').hide();
    editableProperty.find('.editableProperty-save, .editableProperty-cancel').hide();
    editableProperty.find('.editableProperty-edit').show();
  };

  var save = function(editableProperty) {
    // Disable the save and cancel actions while the property is being saved.
    editableProperty.find('.editableProperty-save, .editableProperty-cancel').addClass('disabled');
    // Notify others that we're about to save.
    $(document).trigger('xwiki:actions:beforeSave');
    // Collect the submit data.
    var editor = editableProperty.next('.editableProperty-viewer').next('.editableProperty-editor');
    var data = editor.find(':input').serializeArray();
    data.push({name: 'language', value: xcontext.locale});
    data.push({name: 'comment', value: 'Update property ' + editableProperty.data('property')});
    data.push({name: 'minorEdit', value: true});
    data.push({name: 'form_token', value: xcontext.form_token});
    data.push({name: 'ajax', value: true});
    var notification = new XWiki.widgets.Notification(
      $jsontool.serialize($services.localization.render('core.editors.saveandcontinue.notification.inprogress')),
      'inprogress'
    );
    return $.post(XWiki.currentDocument.getURL('save'), data).done(function() {
      notification.replace(new XWiki.widgets.Notification(
        $jsontool.serialize($services.localization.render('core.editors.saveandcontinue.notification.done')),
        'done'
      ));
    }).fail(function(response) {
      notification.replace(new XWiki.widgets.Notification(
        $jsontool.serialize($services.localization.render('core.editors.saveandcontinue.notification.error',
          ['<span id="saveFailureReason"/>'])),
        'error'
      ));
      $('#saveFailureReason').text(response.statusText);
    }).always(function() {
      // Re-enable the save and cancel actions.
      editableProperty.find('.editableProperty-save, .editableProperty-cancel').removeClass('disabled');
    });
  };

  var view = function(editableProperty) {
    return $.get(XWiki.currentDocument.getURL('get'), {
      xpage: 'display',
      property: editableProperty.data('property'),
      type: editableProperty.data('propertyType'),
      language: xcontext.locale
    }).done(function(html) {
      // Cancel the edit if needed.
      cancel(editableProperty);
      // Update the viewer.
      var viewer = editableProperty.next('.editableProperty-viewer').html(html);
      // Allow others to enhance the viewer.
      $(document).trigger('xwiki:dom:updated', {'elements': viewer.toArray()});
    }).fail(function() {
      new XWiki.widgets.Notification(
        $jsontool.serialize($services.localization.render('Failed to view property.')),
        'error'
      );
    });
  };

  var saveAndView = function(editableProperty) {
    return save(editableProperty).then($.proxy(view, null, editableProperty));
  };
});

require(['jquery', 'editableProperty', 'xwiki-events-bridge'], function($) {
  var init = function(event, data) {
    var container = $((data && data.elements) || document);
    container.find('dt.editableProperty').editableProperty();
  };

  $(document).on('xwiki:dom:updated', init);
  XWiki.domIsLoaded && init();
});
