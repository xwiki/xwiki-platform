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
define('requiredRightsSelectorTranslationKeys', {
  prefix: '',
  keys: [
    'web.widgets.requiredRights.contentUpdate.inProgress',
    'web.widgets.requiredRights.contentUpdate.done',
    'web.widgets.requiredRights.contentUpdate.failed'
  ]
});

require(['jquery', 'xwiki-l10n!requiredRightsSelectorTranslationKeys'], function ($, translations) {
  function bind(select) {
    select.on('change', function (event, target) {
      var data = $(target).find(':selected').data();

      var lowerRightsBlock = $(target).parent('.editRequiredRights').find('.lowerrights');
      $(target).data('updatedPriority', data.priority);
      if (data.priority < $(target).data().priority) {
        lowerRightsBlock.removeClass('hidden');
      } else {
        lowerRightsBlock.addClass('hidden');
      }

      var hiddenParams = $(target).parent('.editRequiredRights').find('.hidden.parameters');
      hiddenParams.empty();
      hiddenParams.append($('<input/>')
        .attr('type', 'hidden')
        .attr('name', 'activateRequiredRights')
        .val(data.activated ? "1" : "0")
      );
      hiddenParams.append($('<input/>')
        .attr('type', 'hidden')
        .attr('name', 'updateRequiredRights')
        .val('1'));
      hiddenParams.append($('<input/>')
        .attr('type', 'hidden')
        .attr('name', 'requiredRights')
        .val(data.right)
      );
      if (data.right === 'programming') {
        hiddenParams.append($('<input/>')
          .attr('type', 'hidden')
          .attr('name', 'requiredRights')
          .val('script')
        );
      }
      $(select).trigger('requiredRightsPickerUpdated');
    });
  }

  var render = function () {
    var data = {
      // Get only the document content and title (without the header, footer, panels, etc.)
      xpage: 'get',
      // The displayed document title can depend on the rendered document content.
      outputTitle: true
    };
    return $.get(XWiki.currentDocument.getURL('view'), data).then(function (html) {
      // Extract the rendered title and content.
      var container = $('<div/>').html(html);
      return {
        renderedTitle: container.find('#document-title h1').html(),
        renderedContent: container.find('#xwikicontent').html()
      };
    });
  };

  $(document).on('xwiki:document:saved', function () {
    var inProgressMessage = translations.get('web.widgets.requiredRights.contentUpdate.inProgress');
    var notification = new XWiki.widgets.Notification(inProgressMessage, 'inprogress');
    var contentWrapper = $('#xwikicontent').not('[contenteditable]');
    render().done(function (output) {
      // Update the displayed document title and content.
      $('#document-title h1').html(output.renderedTitle);
      contentWrapper.html(output.renderedContent);
      // Let others know that the DOM has been updated, in order to enhance it.
      $(document).trigger('xwiki:dom:updated', {'elements': contentWrapper.toArray()});
      var doneMessage = translations.get('web.widgets.requiredRights.contentUpdate.done');
      notification.replace(new XWiki.widgets.Notification(doneMessage, 'done'));
    }).fail(function () {
      var failedMessage = translations.get('web.widgets.requiredRights.contentUpdate.failed');
      notification.replace(new XWiki.widgets.Notification(failedMessage, 'error'));
    });
  });

  var init = function (event, data) {
    var container = $((data && data.elements) || document);
    container.find('select[data-type="requiredRights"]').each((idx, select) => bind(select));
  };

  $(document).on('xwiki:dom:updated', init);
  return XWiki.domIsLoaded && init();
});