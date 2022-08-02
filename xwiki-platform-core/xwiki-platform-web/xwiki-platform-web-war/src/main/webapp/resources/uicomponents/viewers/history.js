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
require(['jquery', 'xwiki-events-bridge'], function($) {
  var getRev = function () {
    var rev1 = $("input[type='radio'][name='rev1']:checked").val() || '';
    var rev2 = $("input[type='radio'][name='rev2']:checked").val() || '';

    return {
      'rev1': rev1,
      'rev2': rev2
    }
  };

  var getForm = function () {
    return $('#historyform');
  };

  var submitForm = function (form) {
    getForm().submit();
  };

  var displayCompareFunc = function(e) {
    e.preventDefault();

    var revs = getRev();
    var docVariant = XWiki.docvariant;
    var newUrl = XWiki.currentDocument.getURL('view', "viewer=changes&rev1=__rev1__&rev2=__rev2__&" + docVariant)
      .replace('__rev1__', revs.rev1)
      .replace('__rev2__', revs.rev2);
    getForm().attr('action', newUrl);
    submitForm();
  };

  var deleteVersionFunc = function(e) {
    e.preventDefault();

    var revs = getRev();
    var confirmText = "$services.localization.render('core.viewers.history.confirmDeleteRange')"
      .replace('__rev1__', revs.rev1)
      .replace('__rev2__', revs.rev2);

    if (!confirm(confirmText)) {
      return false;
    }
    var newUrl = XWiki.currentDocument.getURL('deleteversions', 'confirm=1');

    getForm().attr('action', newUrl);
    submitForm();
  };

  var viewMinorVersionFunc = function(e) {
    e.preventDefault();
    var docVariant = XWiki.docvariant;
    var newUrl = XWiki.currentDocument.getURL('view', "viewer=history&showminor=true&" + docVariant);

    getForm().attr('action', newUrl);
    submitForm();
  };

  var hideMinorVersionFunc = function(e) {
    e.preventDefault();
    var docVariant = XWiki.docvariant;
    var newUrl = XWiki.currentDocument.getURL('view', "viewer=history&" + docVariant);

    getForm().attr('action', newUrl);
    submitForm();
  };

  var init = function () {
    var displayCompareButton = $("input[type='submit'][name='displayCompare']");
    displayCompareButton.on("click", displayCompareFunc);

    var deleteVersionButton = $("input[type='submit'][name='deleteVersions']");
    deleteVersionButton.on("click", deleteVersionFunc);

    var viewMinorVersionsButton = $("input[type='submit'][name='viewMinorVersions']");
    viewMinorVersionsButton.on("click", viewMinorVersionFunc);

    var hideMinorVersionButton = $("input[type='submit'][name='hideMinorVersions']");
    hideMinorVersionButton.on("click", hideMinorVersionFunc);
  };

  $(document).on('xwiki:docextra:loaded', init);

  init();
});