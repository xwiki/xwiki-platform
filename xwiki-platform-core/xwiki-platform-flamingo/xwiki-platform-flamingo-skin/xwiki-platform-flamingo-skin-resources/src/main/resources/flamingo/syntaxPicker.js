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
  'web.widgets.syntaxPicker.conversionConfirmation.title',
  'web.widgets.syntaxPicker.conversionUnsupported.acknowledge',
  'no',
  'yes',
  [
    'web.widgets.syntaxPicker.conversionConfirmation.message',
    '<strong class="previousSyntax"></strong>',
    '<strong class="nextSyntax"></strong>'
  ],
  [
    'web.widgets.syntaxPicker.conversionUnsupported.message',
    '<strong class="previousSyntax"></strong>',
    '<strong class="nextSyntax"></strong>'
  ],
  'web.widgets.syntaxPicker.conversion.inProgress',
  'web.widgets.syntaxPicker.conversion.done',
  'web.widgets.syntaxPicker.conversion.failed',
  'web.widgets.syntaxPicker.contentUpdate.inProgress',
  'web.widgets.syntaxPicker.contentUpdate.done',
  'web.widgets.syntaxPicker.contentUpdate.failed'
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
#set ($iconNames = ['cross'])
#set ($icons = {})
#foreach ($iconName in $iconNames)
  #set ($discard = $icons.put($iconName, $services.icon.renderHTML($iconName)))
#end
#[[*/
// Start JavaScript-only code.
(function(l10n, icons) {
  "use strict";

/**
 * The syntax picker.
 */
require(['jquery', 'xwiki-syntax-converter', 'bootstrap'], function($, syntaxConverter) {
  var syntaxPickerSelector = '#xwikidocsyntaxinput2';

  $(document).on('change.xwikiDocumentSyntaxPicker', syntaxPickerSelector, function() {
    var syntaxPicker = $(this);
    var previousSyntax = syntaxPicker.data('previousSyntax');
    var nextSyntax = getSelectedSyntax(syntaxPicker);
    maybeAskForSyntaxConversionConfirmation(previousSyntax, nextSyntax).then(data => {
      // Give the focus back to the syntax picker.
      syntaxPicker.focus();
      // Trigger the syntax change event.
      triggerSyntaxChange(syntaxPicker, {
        previousSyntax: previousSyntax,
        syntax: nextSyntax,
        convertSyntax: data.convertSyntax
      });
    });
  });

  var getSelectedSyntax = function(syntaxPicker) {
    var selectedOption = syntaxPicker.find('option').filter(':selected');
    var selectedSyntax = $.extend(selectedOption.data('syntax'), {
      id: selectedOption.val(),
      label: selectedOption.text(),
      parser: selectedOption.data('parser'),
      renderer: selectedOption.data('renderer')
    });
    return selectedSyntax;
  };

  var triggerSyntaxChange = function(syntaxPicker, data) {
    // Save the answer to be used by the save action and get the target document reference.
    var documentReference = syntaxPicker.next('input[name="convertSyntax"]').val(data.convertSyntax)
      .data('documentReference');
    // Notify the others that the document syntax has changed. We disable the syntax picker while the UI is updated as
    // a result of the document syntax change.
    data = $.extend(data, {
      // Used by the syntax change listeners to schedule asynchronous tasks when the syntax changes:
      //   data.promise = data.promise.then(() => {...});
      promise: Promise.resolve(),
      savedSyntax: syntaxPicker.data('savedSyntax'),
      syntaxConverter: syntaxConverter,
      documentReference: XWiki.Model.resolve(documentReference, XWiki.EntityType.DOCUMENT,
        XWiki.currentDocument.documentReference)
    });
    syntaxPicker.prop('disabled', true).trigger('xwiki:document:syntaxChange', data);
    // Re-enable the syntax picker after the UI has been updated.
    data.promise.finally(syntaxPicker.prop.bind(syntaxPicker, 'disabled', false));
    // Update the previous syntax.
    syntaxPicker.data('previousSyntax', data.syntax);
  };

  var confirmationModal = $(`
    <div id="syntaxConversionConfirmation" class="modal" tabindex="-1" role="dialog">
      <div class="modal-dialog" role="document">
        <div class="modal-content">
          <div class="modal-header">
            <button type="button" class="close" data-dismiss="modal" aria-label="Close">` +
              icons.cross +
            `</button>
            <h4 class="modal-title"></h4>
          </div>
          <div class="modal-body"></div>
          <div class="modal-footer">
            <button type="button" class="btn btn-default dontConvertSyntax" data-dismiss="modal"></button>
            <button type="button" class="btn btn-primary convertSyntax" data-dismiss="modal"></button>
            <button type="button" class="btn btn-primary acknowledge" data-dismiss="modal"></button>
          </div>
        </div>
      </div>
    </div>
  `).on('shown.bs.modal', function() {
    confirmationModal.find('.btn-primary').filter(':visible').focus();
  }).on('hidden.bs.modal', function() {
    confirmationModal.data('deferred').resolve(confirmationModal.data('data'));
  }).on('click', 'button.convertSyntax', function() {
    confirmationModal.data('data').convertSyntax = true;
  }).appendTo('body').modal({show: false});

  confirmationModal.find('.modal-title').text(l10n['web.widgets.syntaxPicker.conversionConfirmation.title']);
  confirmationModal.find('button.dontConvertSyntax').text(l10n.no);
  confirmationModal.find('button.convertSyntax').text(l10n.yes);
  confirmationModal.find('button.acknowledge').text(l10n['web.widgets.syntaxPicker.conversionUnsupported.acknowledge']);

  var maybeAskForSyntaxConversionConfirmation = function(previousSyntax, nextSyntax) {
    var message;
    // In order to perform a syntax conversion we need to parse from the previous syntax and render to the next syntax.
    var canConvertSyntax = previousSyntax.parser && nextSyntax.renderer;
    if (canConvertSyntax) {
      // Ask for confirmation to convert the syntax.
      message = l10n['web.widgets.syntaxPicker.conversionConfirmation.message'];
    } else {
      // Let the user know that the automatic syntax conversion is not possible.
      message = l10n['web.widgets.syntaxPicker.conversionUnsupported.message'];
    }
    confirmationModal.find('.modal-body').html(message);
    confirmationModal.find('.previousSyntax').text(previousSyntax.label);
    confirmationModal.find('.nextSyntax').text(nextSyntax.label);
    confirmationModal.find('button.dontConvertSyntax, button.convertSyntax').toggle(canConvertSyntax);
    confirmationModal.find('button.acknowledge').toggle(!canConvertSyntax);
    var deferred, promise = new Promise((resolve, reject) => {
      deferred = {resolve, reject};
    });
    confirmationModal.data('deferred', deferred).data('data', {convertSyntax: false}).modal('show');
    return promise;
  };

  $(document).on('xwiki:actions:cancel.xwikiDocumentSyntaxPicker', function(event) {
    // See if there is a syntax picker in the canceled form. Note that by doing this we trigger the syntax change
    // (revert) event only if the cancel event doesn't target the entire page.
    var form = $(event.target).closest('form, .form');
    var syntaxPicker = form.find(syntaxPickerSelector);
    if (syntaxPicker.length) {
      var previousSyntax = getSelectedSyntax(syntaxPicker);
      var nextSyntax = syntaxPicker.data('savedSyntax');
      if (previousSyntax.id !== nextSyntax.id) {
        triggerSyntaxChange(syntaxPicker, {
          previousSyntax: previousSyntax,
          syntax: nextSyntax,
          convertSyntax: syntaxPicker.next('input[name="convertSyntax"]').val() === 'true',
          reverting: true
        });
      }
    }
  });

  $(document).on('xwiki:document:saved.xwikiDocumentSyntaxPicker', function(event) {
    // See if there is a syntax picker in the saved form.
    var form = $(event.target).closest('form, .form');
    var syntaxPicker = form.find(syntaxPickerSelector);
    if (syntaxPicker.length) {
      // Update the saved syntax.
      syntaxPicker.data('savedSyntax', getSelectedSyntax(syntaxPicker));
    }
  });

  var maybeInitSyntaxPicker = function(syntaxPicker) {
    if (!syntaxPicker.data('savedSyntax')) {
      var selectedSyntax = getSelectedSyntax(syntaxPicker);
      // We want to know the saved syntax in order to be able to revert the change on cancel.
      syntaxPicker.data('savedSyntax', selectedSyntax);
      // We want to know the previous syntax on change.
      syntaxPicker.data('previousSyntax', selectedSyntax);
    }
  };

  var init = function(event, data) {
    var container = $((data && data.elements) || document);
    container.find(syntaxPickerSelector).each(function() {
      maybeInitSyntaxPicker($(this));
    });
  };

  $(document).on('xwiki:dom:updated.xwikiDocumentSyntaxPicker', init);
  $(init);
});

/**
 * The syntax converter.
 */
define('xwiki-syntax-converter', ['jquery', 'xwiki-meta'], function($, xcontext) {
  return {
    /**
     * Convert the document content, or the given content, from the input syntax to the output syntax. This is useful if
     * you want to preview a syntax change with syntax conversion.
     */
    convert: function(outputSyntax, inputSyntax, content) {
      var data = {
        // Preview action requires the CSRF token (in order to prevent anyone from executing arbitrary code on behalf
        // of the current user by forging a preview URL).
        /* jshint camelcase:false */
        form_token: xcontext.form_token,
        // Get only the document content (without the header, footer, panels, etc.)
        xpage: 'get',
        // Don't apply any sheet and don't execute any rendering transformation. Just convert the syntax.
        contentTransformed: false,
        // The output syntax
        outputSyntax: outputSyntax.type.id,
        outputSyntaxVersion: outputSyntax.version
      };
      if (inputSyntax) {
        data.syntaxId = inputSyntax.id;
      }
      if (content) {
        data.content = content;
      }
      return $.post(XWiki.currentDocument.getURL('preview'), data);
    },

    /**
     * Render the content and the title of the current document as if the document syntax is the given syntax. This is
     * useful if you want to preview a syntax change without the syntax conversion.
     */
    render: function(inputSyntax, content) {
      var data = {
        // Preview action requires the CSRF token (in order to prevent anyone from executing arbitrary code on behalf
        // of the current user by forging a preview URL).
        /* jshint camelcase:false */
        form_token: xcontext.form_token,
        // Get only the document content and title (without the header, footer, panels, etc.)
        xpage: 'get',
        // The displayed document title can depend on the rendered document content.
        outputTitle: true,
        // Render the document content as if the document syntax is the given syntax.
        syntaxId: inputSyntax.id
      };
      if (content) {
        data.content = content;
      }
      return $.post(XWiki.currentDocument.getURL('preview'), data).then(function(html) {
        // Extract the rendered title and content.
        var container = $('<div></div>').html(html);
        return {
          renderedTitle: container.find('#document-title h1').html(),
          renderedContent: container.find('#xwikicontent').html()
        };
      });
    }
  };
});

/**
 * Syntax conversion for the wiki editor.
 */
require(['jquery'], function($) {
  // Maybe convert the document content on syntax change.
  $(document).on('xwiki:document:syntaxChange.wikiEditor', function(event, data) {
    var contentField = $('#content');
    if (contentField.data('syntax') !== data.previousSyntax.id ||
        !XWiki.currentDocument.documentReference.equals(data.documentReference)) {
      // Either the source content is not edited directly but through an WYSIWYG editor, most probably, or the document
      // for which the syntax has changed is not the current document.
      return;
    // Convert the syntax if the content is not empty and the form was not canceled.
    } else if (data.convertSyntax && contentField.val() && !data.reverting) {
      // Convert the content to the new syntax.
      var notification;
      data.promise = data.promise.then(() => {
        notification = new XWiki.widgets.Notification(l10n['web.widgets.syntaxPicker.conversion.inProgress'],
          'inprogress');
        // Pass the content since it may have unsaved changes.
        return data.syntaxConverter.convert(data.syntax, data.previousSyntax, contentField.val());
      }).then(newContent => {
        // Update the content and the syntax. We trigger the change event in case the content field is enhanced with
        // syntax highlighting.
        contentField.val(newContent).data('syntax', data.syntax.id).trigger('change');
        notification.replace(new XWiki.widgets.Notification(l10n['web.widgets.syntaxPicker.conversion.done'], 'done'));
      }).catch(() => {
        notification.replace(new XWiki.widgets.Notification(l10n['web.widgets.syntaxPicker.conversion.failed'],
          'error'));
      });
    } else {
      // Just update the syntax. The user can change the syntax multiple times during an editing session without saving.
      contentField.data('syntax', data.syntax.id);
    }
  });
});

/**
 * Preview syntax conversion in view mode.
 */
require(['jquery'], function($) {
  // The rendered document content (what the user sees in view mode) depends on the document syntax so we should
  // re-render the document content whenever the document syntax is changed. This allows the user to preview the syntax
  // change before saving.
  $(document).on('xwiki:document:syntaxChange.viewMode', function(event, data) {
    // Check if we are viewing the document content. Also make sure that the syntax change targets the current document.
    var contentWrapper = $('#xwikicontent').not('[contenteditable]');
    if (contentWrapper.length && XWiki.currentDocument.documentReference.equals(data.documentReference)) {
      var notification;
      data.promise = data.promise.then(() => {
        notification = new XWiki.widgets.Notification(l10n['web.widgets.syntaxPicker.contentUpdate.inProgress'],
          'inprogress');
        return maybeConvertAndRender(data);
      }).then(output => {
        // Update the displayed document title and content.
        $('#document-title h1').html(output.renderedTitle);
        contentWrapper.html(output.renderedContent);
        // Let others know that the DOM has been updated, in order to enhance it.
        $(document).trigger('xwiki:dom:updated', {'elements': contentWrapper.toArray()});
        notification.replace(new XWiki.widgets.Notification(l10n['web.widgets.syntaxPicker.contentUpdate.done'],
          'done'));
      }).catch(() => {
        notification.replace(new XWiki.widgets.Notification(l10n['web.widgets.syntaxPicker.contentUpdate.failed'],
          'error'));
      });
    }
  });

  var maybeConvertAndRender = function(data) {
    // Convert to the new syntax only if it's different than the currently saved syntax. This check is useful because
    // the user can change the syntax multiple times before saving. Note that this check works as long as the document
    // content doesn't have unsaved changes, which is true because we are only viewing the content in this case.
    if (data.convertSyntax && data.syntax.id !== data.savedSyntax.id) {
      return data.syntaxConverter.convert(data.syntax)
        .then(data.syntaxConverter.render.bind(data.syntaxConverter, data.syntax));
    } else {
      return data.syntaxConverter.render(data.syntax);
    }
  };
});

// End JavaScript-only code.
}).apply(']]#', $jsontool.serialize([$l10n, $icons]));
