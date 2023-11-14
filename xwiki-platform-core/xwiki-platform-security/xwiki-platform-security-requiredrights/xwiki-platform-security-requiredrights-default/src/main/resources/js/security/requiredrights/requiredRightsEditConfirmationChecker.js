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
require(['jquery'], function ($) {
  const hiddenClass = "hidden";
  const eventKey = 'click.required-rights-results';

  function init(root) {
    const results = root.find(".required-rights-results");
    const advancedToggle = root.find(".required-rights-advanced-toggle");
    const advancedExpand = advancedToggle.find(".toggle-expand");
    const advancedCollapse = advancedToggle.find(".toggle-collapse");
    advancedToggle
      .off(eventKey)
      .on(eventKey, () => {
        const isHidden = results.hasClass(hiddenClass);
        if (isHidden) {
          results.removeClass(hiddenClass);
          advancedCollapse.removeClass(hiddenClass);
          advancedExpand.addClass(hiddenClass);
        } else {
          results.addClass(hiddenClass);
          advancedCollapse.addClass(hiddenClass);
          advancedExpand.removeClass(hiddenClass);
        }
        advancedToggle.attr("aria-expanded", isHidden);
        results.attr("aria-expanded", isHidden);
      })
  }

  const modal = $(document);
  init($(modal));
  modal.on('show.bs.modal', function () {
    init($(this));
  })
});