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
require(['jquery', 'xwiki-meta'], function($, xm) {

  $(document).on('click', '#button-inout', function() {
    var button = $('#button-inout');
    var referenceValue= $('#reference-value');
    var localReference = xm.documentReference.relativeTo(xm.documentReference.getRoot());
    var globalReference = xm.documentReference;

    if (button.hasClass('btn-info')) {
      referenceValue.text(localReference);
      button.removeClass('btn-info');
      button.attr('title',"$services.localization.render('core.viewers.information.pageReference.globalButton')");
    } else {
      referenceValue.text(globalReference);
      button.addClass('btn-info');
      button.attr('title',"$services.localization.render('core.viewers.information.pageReference.localButton')");
    }
  });

  var copyToClipboard = function(element) {
    var $temp = $("<input>");
    $("body").append($temp);
    var value = $(element).text();
    $temp.val(value).select();
    document.execCommand("copy");
    $temp.remove();
    return value;
  };

  $(document).on('click', '#button-paste', function() {
    copyToClipboard('#reference-value');
    new XWiki.widgets.Notification("$services.localization.render('core.viewers.information.pageReference.copied')",
      'info');
  });
});