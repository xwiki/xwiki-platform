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
require([ 'jquery' ], function($) {
  $('.log-item').each(function(index, element) {
    var stacktrace = $(this).find('.stacktrace');
    if (stacktrace) {
      // Hide the stacktrace by default.
      stacktrace.toggle();
      // Show the stacktrace when the log message is clicked.
      var logMessage = $(this).find('div');
      logMessage.css({
        "cursor" : "pointer"
      });
      logMessage.click(function() {
        stacktrace.toggle();
      });
    }
  });
});
