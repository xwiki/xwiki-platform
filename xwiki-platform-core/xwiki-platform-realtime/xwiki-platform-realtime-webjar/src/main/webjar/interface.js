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
// This file defines functions which are used by the real-time editors (Wiki / WYSIWYG) and address components of the
// user interface.
define('xwiki-realtime-interface', ['jquery', 'xwiki-l10n!xwiki-realtime-messages'], function($, Messages) {
  'use strict';

  class Interface {
    createAllowRealtimeCheckbox({checked, join, leave}) {
      let allowRealtimeCheckbox = this.getAllowRealtimeCheckbox();
      // Don't duplicate the checkbox if it already exists.
      if (!allowRealtimeCheckbox.length) {
        const wrapper = $(
          '<label class="realtime-allow-label text-nowrap">' +
            '<input type="checkbox" class="realtime-allow"/>' +
          '</label>'
        ).appendTo('.buttons');
        allowRealtimeCheckbox = wrapper.append(document.createTextNode(Messages.allowRealtime)).find('input');
      }

      return allowRealtimeCheckbox.off('change.realtime').on('change.realtime', () => {
        if (allowRealtimeCheckbox.prop('checked')) {
          // Disable the checkbox while we're trying to join the realtime editing session.
          allowRealtimeCheckbox.prop('disabled', true);
          join().catch(() => {
            // We failed to join to the realtime editing session.
            new XWiki.widgets.Notification(Messages['join.error'], 'error');
            allowRealtimeCheckbox.prop('checked', false);
          }).finally(() => {
            // Re-enable the checkbox so that the user can try again.
            allowRealtimeCheckbox.prop('disabled', false);
          });
        } else {
          leave();
        }
      // Disable the checkbox while we're trying to join the realtime editing session.
      }).prop('checked', checked).prop('disabled', checked);
    }

    getAllowRealtimeCheckbox() {
      return $('input.realtime-allow[type="checkbox"]');
    }
  }

  return new Interface();
});
