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
  //
  // Collapsible diff summary.
  //

  var enhanceDiffSummaryItem = function() {
    var details = $(this).next('ul');
    if (details.size() > 0) {
      details.hide();
      $(this).find('a').click(function(event) {
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

  var enhanceHTMLDiff = function(container) {
    // Show ellipsis before and after diff blocks.
    container.find('.html-diff [data-xwiki-html-diff-block]').each(function() {
      maybeMarkEllipsis(getPreviousElement(this), true);
      maybeMarkEllipsis(getNextElement(this), false);
    });

    // Show the hidden content when the ellipsis is clicked.
    container.find('.html-diff').on('click', '[data-xwiki-html-diff-hidden="ellipsis"]', function() {
      $(this).attr('data-xwiki-html-diff-hidden', 'false');
      if ($(this).attr('data-xwiki-html-diff-expand') === 'up') {
        maybeMarkEllipsis(getPreviousElement(this), true);
      } else {
        maybeMarkEllipsis(getNextElement(this), false);
      }
      $(this).removeAttr('data-xwiki-html-diff-expand');
    });
  };

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

    container.find('input[name=conflict_id]').each(function () { bindConflictDecision($(this).val()); });
  };

  //
  // Initialization
  //

  var init = function(container) {
    enhanceDiffSummaryItems(container);
    enhanceHTMLDiff(container);
    enhanceConflictDecision(container);
  };

  $(document).on('xwiki:dom:updated', function(event, data) {
    init($(data.elements));
  });

  init($(document.body));
});
