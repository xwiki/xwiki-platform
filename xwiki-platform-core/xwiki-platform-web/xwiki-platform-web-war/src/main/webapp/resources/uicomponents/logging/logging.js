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
/**
 * Enhances the behaviour a log display.
 */
require(['jquery', 'xwiki-events-bridge'], function($) {
  var enhanceStackTrace = function(container) {
    container.find('.log-item').each(function() {
      var stacktrace = $(this).find('.stacktrace');
      if (stacktrace) {
        // Hide the stacktrace by default.
        stacktrace.toggle();
        // Show the stacktrace when the log message is clicked.
        var logMessage = $(this).find('div');
        logMessage.css({
          "cursor" : "pointer"
        });
        logMessage.on('click', function() {
          stacktrace.toggle();
        });
      }
    });
  };

  var autoScrollLog = function(container) {
    // Scroll the log to the end if it has a loading item.
    // TODO: Preserve the scroll position if the user scrolls through the log.
    container.find('.log').has('.log-item-loading').each(function() {
      this.scrollTop = this.scrollHeight;
    });
  };

  var getTarget = function(trigger) {
    try {
      return document.evaluate(trigger.getAttribute('data-target-xpath'), trigger, null,
        XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
    } catch (e) {
      console && console.log(e);
      return null;
    }
  };

  var collapsibleLog = function(container) {
    // Initial state.
    container.find('.collapse-toggle.collapsed').each(function() {
      $(getTarget(this)).addClass('hidden');
    });
    // Expand/Collapse on click.
    container.find('.collapse-toggle').on('click', function() {
      $(this).toggleClass('collapsed');
      $(getTarget(this)).toggleClass('hidden');
    });
  };

  var enhanceLog = function(container) {
    enhanceStackTrace(container);
    autoScrollLog(container);
    collapsibleLog(container);
  }

  $(document).on('xwiki:dom:updated', function(event, data) {
    enhanceLog($(data.elements));
  });
  enhanceLog($('body'));
});
