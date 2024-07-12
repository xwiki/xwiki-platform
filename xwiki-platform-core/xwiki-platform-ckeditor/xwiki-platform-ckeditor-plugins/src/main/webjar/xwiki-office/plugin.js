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
 define('officeImporterModal', ['jquery', 'modal'], function($, $modal) {
  'use strict';
  return $modal.createModalStep({
    'class': 'office-importer-modal',
    onLoad: function() {
      var modal = this;
      var modalBody = modal.find('.modal-body');
      var submitButton = modal.find('.modal-footer .btn-primary');
      modal.on('show.bs.modal', function(event) {
        if (modalBody.is(':empty') && !modalBody.hasClass('loading')) {
          modalBody.addClass('loading');
          var l10n = modal.data('input').localization;
          submitButton.text(l10n.get('xwiki-office.importer.import'));
          modal.find('.modal-title').text(l10n.get('xwiki-office.importer.title'));
          var formURL = modal.data('input').formURL;
          $.get(formURL).done(function(html) {
            modalBody.html(html);
          }).fail(function() {
            var errorMessage = $('<div class="box errormessage"></div>')
              .text(l10n.get('xwiki-office.importer.failedToLoadForm'));
            modalBody.empty().append(errorMessage);
          }).always(function() {
            modalBody.removeClass('loading');
          });
        }
      });
      modal.on('change', 'input[name="filePath"]', function(event) {
        submitButton.prop('disabled', !$(event.target).val());
      });
      submitButton.on('click', function(event) {
        var output = {
          files: modalBody.find('input[name="filePath"]')[0].files,
          filterStyles: modalBody.find('input[name="filterStyles"]').prop('checked'),
          useOfficeViewer: modalBody.find('input[name="useOfficeViewer"]').prop('checked')
        };
        if (output.files && output.files.length > 0) {
          modal.data('output', output).modal('hide');
        }
      });
    }
  });
});

(function() {
  'use strict';
  var $ = jQuery;

  // Declare the configuration namespace.
  CKEDITOR.config['xwiki-office'] = CKEDITOR.config['xwiki-office'] || {
    __namespace: true
  };

  CKEDITOR.plugins.add('xwiki-office', {
    requires: 'uploadwidget,notification,xwiki-localization,xwiki-macro,xwiki-dialog',

    init : function(editor) {
      // Fill missing configuration with default values.
      editor.config['xwiki-office'] = $.extend({
        enabled: false,
        importer: editor.config.sourceDocument.getURL('get', $.param({
          sheet: 'CKEditor.OfficeImporter',
          language: editor.getContentLocale()
        }))
      }, editor.config['xwiki-office']);

      this.initOfficeImporter(editor);

      if (editor.config.applyPasteFilterAfterPasteFromWord) {
        this.applyPasteFilterAfterPasteFromWord(editor);
      }
    },

    // The paste filter is not applied by default for content pasted from Word.
    // https://dev.ckeditor.com/ticket/13093
    // https://stackoverflow.com/questions/45501341/ckeditor-pastefromword-ignores-pastefilter
    applyPasteFilterAfterPasteFromWord: function(editor) {
      editor.on('afterPasteFromWord', function(event) {
        var filter = editor.pasteFilter;
        if (!filter) {
          return;
        }
        var fragment = CKEDITOR.htmlParser.fragment.fromHtml(event.data.dataValue),
          writer = new CKEDITOR.htmlParser.basicWriter();
        filter.applyTo(fragment);
        fragment.writeHtml(writer);
        event.data.dataValue = writer.getHtml();
      });
    },

    initOfficeImporter: function(editor) {
      var thisPlugin = this;
      var officeConfig = editor.config['xwiki-office'];

      editor.ui.addButton('officeImporter', {
        label: editor.localization.get('xwiki-office.importer.title'),
        icon: 'pastefromword',
        command: 'officeImporter',
        toolbar: 'insert,50'
      });

      editor.addCommand('officeImporter', {
        async: true,
        contextSensitive: false,
        startDisabled: !officeConfig.enabled,
        exec: function(editor) {
          var command = this;
          require(['officeImporterModal'], function(officeImporterModal) {
            officeImporterModal({
              formURL: officeConfig.importer,
              localization: editor.localization
            }).done(function(formData) {
              thisPlugin.importOfficeFile(editor, formData);
            }).always(function() {
              editor.fire('afterCommandExec', {name: command.name, command: command});
            });
          });
        }
      });

      CKEDITOR.fileTools.addUploadWidget(editor, 'uploadOfficeFile', {
        onUploaded: function(upload) {
          var notification = editor.showNotification(editor.localization.get('xwiki-office.importer.inProgress'),
            'progress');
          var widget = this;
          $.get(officeConfig.importer, {
            fileName: upload.fileName,
            filterStyles: upload.file.filterStyles,
            useOfficeViewer: upload.file.useOfficeViewer,
            outputSyntax: 'plain'
          }).done(function(html, textStatus, jqXHR) {
            // Load the required skin extensions reusing the function defined by the Macro Wizard. This is needed for
            // instance when importing a presentation using the Office Viewer macro which requires the gallery widget so
            // we need to load the gallery CSS and JavaScript resources (skin extensions).
            var requiredSkinExtensions = jqXHR.getResponseHeader('X-XWIKI-HTML-HEAD');
            require(['macroWizard'], function() {
              $(editor.document.$).loadRequiredSkinExtensions(requiredSkinExtensions);
            });
            widget.replaceWith(html);
            notification.update({
              message: editor.localization.get('xwiki-office.importer.done'),
              type: 'success'
            });
          }).fail(function() {
            notification.update({
              message: editor.localization.get('xwiki-office.importer.fail'),
              type: 'warning'
            });
            editor.widgets.del(widget);
          });
        }
      });
    },

    importOfficeFile: function(editor, formData) {
      var fileTools = CKEDITOR.fileTools;
      var uploadURL = fileTools.getUploadUrl(editor.config);
      if (!uploadURL) {
        editor.showNotification(editor.localization.get('xwiki-office.importer.noUploadURL'), 'warning');
        return;
      }
      var file = formData.files[0];
      // Save the rest of the import parameters on the file to be used later.
      file.filterStyles = formData.filterStyles;
      file.useOfficeViewer = formData.useOfficeViewer;
      var loader = editor.uploadRepository.create(file);
      var placeholder = editor.document.createElement('div');
      placeholder.appendText(editor.localization.get('xwiki-office.importer.inProgress'));
      fileTools.markElement(placeholder, 'uploadOfficeFile', loader.id);
      fileTools.bindNotifications(editor, loader);
      editor.insertElement(placeholder);
      editor.widgets.initOn(placeholder);
      loader.upload(uploadURL);
    }
  });
})();
