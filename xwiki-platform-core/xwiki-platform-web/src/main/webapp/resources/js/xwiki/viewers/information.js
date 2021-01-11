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
  'core.viewers.information.pageReference.globalButton',
  'core.viewers.information.pageReference.localButton',
  'core.viewers.information.pageReference.copied'
])
#set ($l10n = {})
#foreach ($key in $l10nKeys)
  #set ($discard = $l10n.put($key, $services.localization.render($key)))
#end
#[[*/
// Start JavaScript-only code.
(function(l10n) {
  "use strict";

require(['jquery', 'xwiki-meta'], function($, xm) {
  // Select the reference value on focus.
  $(document).on('focus', '#reference-value', function() {
    $(this).select();
  });

  // Toggle between local and global reference.
  $(document).on('click', '#button-inout', function() {
    var button = $('#button-inout');
    var referenceValue= $('#reference-value');
    var localReference = xm.documentReference.relativeTo(xm.documentReference.getRoot());
    var globalReference = xm.documentReference;

    if (button.hasClass('btn-info')) {
      referenceValue.val(localReference);
      button.removeClass('btn-info').addClass('btn-default');
      button.attr('title', l10n['core.viewers.information.pageReference.globalButton']);
    } else {
      referenceValue.val(globalReference);
      button.removeClass('btn-default').addClass('btn-info');
      button.attr('title', l10n['core.viewers.information.pageReference.localButton']);
    }
  });

  // Copy the reference value to clipboard.
  $(document).on('click', '#button-paste', function() {
    $('#reference-value').select();
    document.execCommand("copy");
    new XWiki.widgets.Notification(l10n['core.viewers.information.pageReference.copied'], 'info');
  });
});

// End JavaScript-only code.
}).apply(']]#', $jsontool.serialize([$l10n]));
