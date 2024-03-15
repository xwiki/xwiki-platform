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
define('modalTranslationKeys', [], [
  'ok',
  'cancel',
  'close'
]);

define('modal', ['jquery', 'l10n!modal', 'bootstrap'], function($, translations) {
  'use strict';
  var modalTemplate = 
    '<div class="modal" tabindex="-1" role="dialog" data-backdrop="static">' +
      '<div class="modal-dialog" role="document">' +
        '<div class="modal-content">' +
          '<div class="modal-header">' +
            '<button type="button" class="close" data-dismiss="modal" aria-label="Close">' +
              '<span aria-hidden="true">&times;</span>' +
            '</button>' +
            '<h4 class="modal-title"></h4>' +
          '</div>' +
          '<div class="modal-body"></div>' +
          '<div class="modal-footer">' +
            '<button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>' +
            '<button type="button" class="btn btn-primary" disabled="disabled">OK</button>' +
          '</div>' +
        '</div>' +
      '</div>' +
    '</div>',

  createModal = function(definition) {
    definition = $.extend({
      title: '',
      content: '',
      acceptLabel: translations.get('ok'),
      dismissLabel: translations.get('cancel'),
      form: false
    }, definition);
    var modal = $(modalTemplate).addClass(definition['class']).appendTo(document.body);
    modal.find('.close').attr({
      title: translations.get('close'),
      'aria-label': translations.get('close')
    });
    modal.find('.modal-title').text(definition.title);
    modal.find('.modal-body').html('').append(definition.content);
    const submitButton = modal.find('.modal-footer .btn-primary');
    submitButton.text(definition.acceptLabel);
    modal.find('.modal-footer .btn[data-dismiss="modal"]').text(definition.dismissLabel);
    if(definition.form) {
      // We want the submit button to actually submit the form
      submitButton.removeAttr('type');
      // This modal should act and look like a form. We replace its content node by a form, so we can benefit from
      // native form utilities, such as implicit form validation (pressing Enter on a text field)
      const modalContent= modal.find('.modal-content');
      const modalContentInnerHTML = modalContent.html();
      const formModalContent = $('<form class="modal-content">');
      formModalContent.html(modalContentInnerHTML);
      modalContent.replaceWith(formModalContent);
    }
    return modal;
  },

  createModalStep = function(definition) {
    var modal;
    return function(input) {
      if (!modal) {
        modal = createModal(definition);
        if (definition && typeof definition.onLoad === 'function') {
          definition.onLoad.call(modal);
        }
        modal.on('hidden.bs.modal', function(event) {
          var deferred = modal.data('deferred');
          var output = modal.data('output');
          if (typeof output === 'undefined') {
            deferred.reject();
          } else {
            deferred.resolve(output);
          }
        });
      }
      var deferred = $.Deferred();
      if (modal.is(':hidden')) {
        modal.data({
          deferred: deferred,
          input: input
        }).removeData('output').modal();
      } else {
        deferred.reject();
      }
      return deferred.promise();
    };
  };

  // TODO: This method must be removed once the parent version is upgraded to  13.4.3+, 12.10.9+, or 13.7-rc-1+ 
  // (see https://jira.xwiki.org/browse/XWIKI-18895)
  $.fn.loadRequiredSkinExtensions = function(requiredSkinExtensions) {
    return this.each(function() {
      // 'this' can be an element, the window or the document itself.
      var ownerDocument = this.ownerDocument || this.document || this;
      var head = $(ownerDocument).find('head');
      var existingSkinExtensions;
      var getExistingSkinExtensions = function() {
        return head.find('link, script').map(function() {
          return $(this).attr('href') || $(this).attr('src');
        }).get();
      };
      $('<div></div>').html(requiredSkinExtensions).find('link, script').filter(function() {
        if (!existingSkinExtensions) {
          existingSkinExtensions = getExistingSkinExtensions();
        }
        var url = $(this).attr('href') || $(this).attr('src');
        return existingSkinExtensions.indexOf(url) < 0;
      }).appendTo(head);
    });
  };
  
  return {
    createModal: createModal,
    createModalStep: createModalStep
  };
});