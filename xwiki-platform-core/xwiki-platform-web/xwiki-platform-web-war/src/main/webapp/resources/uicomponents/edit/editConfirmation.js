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
/*global define */

define('xwiki-edit-confirmation-l10n-keys', {
  prefix: '',
  keys: [
    'platform.core.editConfirmation.close'
  ]
});

define('xwiki-edit-confirmation-icon-keys', {
  'icons': ['cross']
});

/**
 * Reusable edit confirmation modal. The server (e.g. display.vm or the inplace editor sheet) runs the edit confirmation
 * checkers and, when a confirmation is required, returns its result as JSON with a 423 (Locked) status. This module
 * displays that result in a Bootstrap modal and lets the caller know whether the user confirmed (so that it can
 * re-issue the request with force=1) or dismissed the confirmation.
 *
 * The confirmation JSON has the shape produced by the #getEditConfirmation Velocity macro:
 *   {title: String, message: String (HTML), reject: String (label), confirm: String (label, only when forceable)}
 */
define('xwiki-edit-confirmation', [
  'jquery',
  'xwiki-l10n!xwiki-edit-confirmation-l10n-keys',
  'xwiki-icon!xwiki-edit-confirmation-icon-keys',
  'bootstrap'
], function($, l10n, icons) {
  "use strict";

  /**
   * Signals that the user explicitly dismissed the edit confirmation dialog without confirming.
   * Callers should use {@link isConfirmationDismissed} rather than instanceof checks.
   */
  class EditConfirmationDismissed extends Error {
    constructor() {
      super('Edit confirmation dismissed by user.');
      this.name = 'EditConfirmationDismissed';
    }
  }

  /**
   * @param {*} error
   * @return {boolean} true if the error represents a user-initiated dismissal of the confirmation dialog
   */
  function isConfirmationDismissed(error) {
    return error instanceof EditConfirmationDismissed;
  }

  function createForceLockModal() {
    const modal = $([
      '<div class="modal fade force-edit-lock-modal" tabindex="-1" role="dialog">',
        '<div class="modal-dialog" role="document">',
          '<div class="modal-content">',
            '<div class="modal-header">',
              '<button type="button" class="close" data-dismiss="modal"></button>',
              '<h4 class="modal-title"></h4>',
            '</div>',
            '<div class="modal-body"></div>',
            '<div class="modal-footer">',
              '<button type="button" class="btn btn-default" data-dismiss="modal"></button>',
              '<button type="button" class="btn btn-warning"></button>',
            '</div>',
          '</div>',
        '</div>',
      '</div>'
    ].join(''));

    modal.find('.close').attr('aria-label', l10n['platform.core.editConfirmation.close'])
      .append(icons.cross.render());

    modal.find('.modal-footer .btn-warning').on('click', function() {
      // The user has confirmed they want to proceed.
      modal.data('deferred').shouldResolve = true;
      modal.modal('hide');
    });

    modal.on('hidden.bs.modal', function() {
      // Resolve or reject the promise only once the modal has been hidden to avoid race conditions between this
      // modal hiding and another modal potentially already showing.
      const deferred = modal.data('deferred');
      if (deferred) {
        if (deferred.shouldResolve) {
          deferred.resolve();
        } else {
          deferred.reject(new EditConfirmationDismissed());
        }
      }
    });

    return modal.appendTo('body').modal({show: false});
  }

  function getForceLockModal() {
    let modal = $('.force-edit-lock-modal');
    if (!modal.length) {
      modal = createForceLockModal();
    }
    return modal;
  }

  /**
   * Displays the edit confirmation modal and returns a promise that is settled once the user reacts to it.
   *
   * @param {object} confirmation the confirmation produced by the #getEditConfirmation macro: {title, message (HTML),
   *   reject (label), confirm (label, only present when the warnings can be forced)}
   * @return {Promise<void>} resolves when the user confirms (clicks the warning button), rejects with an
   *   {@link EditConfirmationDismissed} error when the user dismisses the modal
   */
  function showConfirmationModal({title, message, reject, confirm} = {}) {
    let deferred;
    const promise = new Promise((resolve, rejectPromise) => {
      deferred = {resolve, reject: rejectPromise};
    });

    const modal = getForceLockModal();
    modal.data('deferred', deferred);

    modal.find('.modal-title').text(title);
    modal.find('.modal-body').html(message);
    modal.find('.modal-footer .btn-default').text(reject);
    if (confirm) {
      modal.find('.modal-footer .btn-warning').show().text(confirm);
    } else {
      modal.find('.modal-footer .btn-warning').hide();
    }

    modal.modal('show');

    return promise;
  }

  /**
   * Extracts the edit confirmation result from a failed field request. The server returns the confirmation as JSON with
   * a 423 (Locked) status; any other failure is re-thrown so that the caller can handle it as a regular error.
   *
   * @param {object} jqXHR the jQuery XHR object from the failed request
   * @return {object} the parsed confirmation data ({title, message, reject, confirm})
   * @throws the original {@code jqXHR} when it's not a 423 edit confirmation response
   */
  function parseConfirmationResponse(jqXHR) {
    if (jqXHR?.status === 423) {
      // Depending on the request's dataType, jQuery may have already parsed the JSON body.
      if (jqXHR.responseJSON) {
        return jqXHR.responseJSON;
      }
      try {
        return JSON.parse(jqXHR.responseText);
      } catch (e) {
        console.error('Failed to parse edit confirmation response: ', e);
        // Fall through to re-throw the original response.
      }
    }
    throw jqXHR;
  }

  return {
    showConfirmationModal,
    parseConfirmationResponse,
    isConfirmationDismissed
  };
});
