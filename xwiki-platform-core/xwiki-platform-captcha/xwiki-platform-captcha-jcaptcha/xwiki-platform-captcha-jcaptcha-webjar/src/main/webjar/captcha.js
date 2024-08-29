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
  var registerListeners = function(container) {
    // If no container is specified, using the entire document as default.
    if (!container) {
      container = document;
    }

    // Click listener for the audio CAPTCHA play/listen button.
    $(container).find('.captcha-play').on('click', function() {
      $(this).siblings('audio.captcha-challenge')[0].play();
    });

    // Click listener for the CAPTCHA refresh/reload button.
    $(container).find('.captcha-refresh').on('click', function() {
      // We simply reset the 'src' attribute's value to make the CAPTCHA display a new challenge.
      var currentSrc = $(this).siblings('.captcha-challenge').prop('src');
      // Make sure to append a cache-buster to the src, since some elements might not reload if the src is the same
      // (e.g. image does not reload, but iframe reloads).
      $(this).siblings('.captcha-challenge')
        .prop('src', currentSrc.replace(/cache-buster=[0-9]+/gm, '') + 'cache-buster=' + new Date().getTime());
    });
  };

  // On load, register listeners for CAPTCHAs we can find.
  registerListeners(document);
  // Whenever a CAPTCHA is reloaded via AJAX, we need to re-register the listeners.
  $(document).on('xwiki:captcha:reloaded', function(event, data) {
    registerListeners(event.target);
  });

  // Does nothing but break Firefox's bfcache (back-forward cache) to make sure the CAPTCHA is reloaded when pressing
  // back and not showing a stale CAPTCHA.
  $(window).on('unload', function() {});
});