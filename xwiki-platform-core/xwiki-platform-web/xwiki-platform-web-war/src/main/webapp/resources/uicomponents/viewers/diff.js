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
define('xwiki-diff', ['jquery', 'xwiki-events-bridge'], function($) {
  //
  // Toggle between raw and rendered changes.
  //

  var enhanceDiffTabs = function(container) {
    container.find('.changes-body a[data-toggle="pill"]').on('show.bs.tab', function (event) {
      var tabPanel = $($(event.target).attr('href'));
      if (!tabPanel.hasClass('loading') && tabPanel.html().trim() == '') {
        var queryString = window.location.search.substring(1).replace('viewer', 'xpage');
        var url = XWiki.currentDocument.getURL('get', queryString);
        var fragmentId = tabPanel.attr('id');
        tabPanel.addClass('loading').load(url + ' #' + fragmentId, {'include': fragmentId}, function() {
          // Remove the duplicated tab pane div because the loaded tab pane doesn't replace the existing one.
          tabPanel.removeClass('loading').children('.tab-pane').children().unwrap();
          $(document).trigger('xwiki:dom:updated', {'elements': tabPanel.toArray()});
        });
      }
      // Toggle the corresponding actions.
      var hint = $(event.target).data('hint');
      $(event.target).closest('.changes-body-header').find('.changes-actions').removeClass('active').filter('.' + hint)
        .addClass('active');
    });
  };

  //
  // Collapsible diff summary.
  //

  var enhanceDiffSummaryItem = function() {
    var details = $(this).next('ul');
    if (details.length) {
      details.hide();
      $(this).find('a').on('click', function(event) {
        event.preventDefault();
        details.toggle();
      });
    }
  };

  var enhanceDiffSummaryItems = function(container) {
    container.find('div.diff-summary-item').each(enhanceDiffSummaryItem);
  };

  //
  // Expandable HTML Diff Context
  //

  var getPreviousElement = function(element) {
    var previous = $(element).prev();
    if (previous.length > 0) {
      return previous[0];
    } else {
      var parent = $(element).parent();
      if (parent.length > 0) {
        return getPreviousElement(parent[0]);
      }
    }
  };

  var getNextElement = function(element) {
    var next = $(element).next();
    if (next.length > 0) {
      return next[0];
    } else {
      var parent = $(element).parent();
      if (parent.length > 0) {
        return getNextElement(parent[0]);
      }
    }
  };

  var maybeMarkEllipsis = function(element, expandUp) {
    if ($(element).attr('data-xwiki-html-diff-hidden') === 'true') {
      $(element).attr({
        'data-xwiki-html-diff-hidden': 'ellipsis',
        'data-xwiki-html-diff-expand': expandUp ? 'up' : 'down',
      });
    }
  };

  var hideHTMLDiffContext = function(htmlDiff) {
    // Hide all context nodes.
    htmlDiff.find('[data-xwiki-html-diff-hidden]').attr('data-xwiki-html-diff-hidden', true);
    // Show ellipsis before and after diff blocks.
    htmlDiff.find('[data-xwiki-html-diff-block]').each(function() {
      maybeMarkEllipsis(getPreviousElement(this), true);
      maybeMarkEllipsis(getNextElement(this), false);
    });
  };

  var enhanceHTMLDiff = function(container) {
    container.find('.html-diff-context-toggle').on('click', function(event) {
      event.preventDefault();
      var toggle = $(this);
      var htmlDiff = toggle.closest('.changes-body').find('.html-diff');
      if (toggle.hasClass('html-diff-context-toggle-show')) {
        // Show all context nodes.
        htmlDiff.find('[data-xwiki-html-diff-hidden]').attr('data-xwiki-html-diff-hidden', false);
        toggle.removeClass('html-diff-context-toggle-show').addClass('html-diff-context-toggle-hide');
      } else {
        hideHTMLDiffContext(htmlDiff);
        toggle.removeClass('html-diff-context-toggle-hide').addClass('html-diff-context-toggle-show');
      }
    });

    var htmlDiff = (container.hasClass('html-diff') && container) || container.find('.html-diff');
    hideHTMLDiffContext(htmlDiff);

    // Enable the show / hide context toggle only if there are context nodes.
    htmlDiff.each(function() {
      var contextNodeCount = $(this).find('[data-xwiki-html-diff-hidden]').length;
      $(this).closest('.changes-body').find('.html-diff-context-toggle').toggleClass('hidden', contextNodeCount === 0);
    });

    // Show the hidden content when the ellipsis is clicked.
    htmlDiff.off('click.showDiffContext').on('click.showDiffContext', '[data-xwiki-html-diff-hidden="ellipsis"]',
    function() {
      $(this).attr('data-xwiki-html-diff-hidden', 'false');
      if ($(this).attr('data-xwiki-html-diff-expand') === 'up') {
        maybeMarkEllipsis(getPreviousElement(this), true);
      } else {
        maybeMarkEllipsis(getNextElement(this), false);
      }
      $(this).removeAttr('data-xwiki-html-diff-expand');
    });
  };

  //
  // Conflict Decision
  //

  var enhanceConflictDecision = function (container) {
    var changeConflictDecision = function (conflictId) {
      var selectValue = $('#conflict_decision_select_' + conflictId).val();
      ['custom', 'previous', 'current', 'next'].forEach(function (item) {
        if (item !== selectValue) {
          $('#conflict_decision_value_' + item + '_' + conflictId).hide();
        } else {
          $('#conflict_decision_value_' + item + '_' + conflictId).show();
        }
      });
    };

    var bindConflictDecision = function (conflictId) {
      $('#conflict_decision_select_' + conflictId).on('change', function () { changeConflictDecision(conflictId) });
    };

    var onlyDisplayCurrent = function (conflictId) {
      ['custom', 'previous', 'current', 'next'].forEach(function (item) {
        if (item !== 'current') {
          $('#conflict_decision_value_' + item + '_' + conflictId).hide();
        } else {
          $('#conflict_decision_value_' + item + '_' + conflictId).show();
        }
      });
    };

    container.find('input[name=conflict_id]').each(function () {
      bindConflictDecision($(this).val());
      onlyDisplayCurrent($(this).val());
    });
  };

  //
  // Initialization
  //

  var init = function(container) {
    enhanceDiffTabs(container);
    enhanceDiffSummaryItems(container);
    enhanceHTMLDiff(container);
    enhanceConflictDecision(container);
  };

  $(document).on('xwiki:dom:updated', function(event, data) {
    init($(data.elements));
  });

  init($(document.body));
});

// Execute the code when this file is loaded with $xwiki.jsfx.use(). This is not needed if the file is loaded with
// RequireJS but it shoudn't hurt either. We do this in order to ensure that the module code is executed only once.
require(['xwiki-diff'], function() {});
