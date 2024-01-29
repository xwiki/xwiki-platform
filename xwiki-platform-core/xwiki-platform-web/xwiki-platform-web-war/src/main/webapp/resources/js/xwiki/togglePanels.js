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
require(['jquery'], function($) {
  let rightToggler = $("#rightPanelsToggler");
  let leftToggler = $("#leftPanelsToggler");

  /* The body update hides completely the panel column. We want to make sure the panel column is visible for the whole
  animation before hiding it. */
  let updateBody = function (isRight, newExpandedState) {
    let controlledBody = $('body.content');
    if (isRight) {
      if (newExpandedState) {
        // We remove the hideright class
        if (controlledBody.hasClass('hidelefthideright')) {
          controlledBody.removeClass('hidelefthideright');
          controlledBody.addClass('hideleft');
        } else {
          controlledBody.removeClass('hideright');
        }
      } else {
        // We add the hideright class
        if (controlledBody.hasClass('hideleft')) {
          controlledBody.addClass('hidelefthideright');
          controlledBody.removeClass('hideleft');
        } else {
          controlledBody.addClass('hideright');
        }
      }
    } else {
      if (newExpandedState) {
        // We remove the hideleft class
        if (controlledBody.hasClass('hidelefthideright')) {
          controlledBody.removeClass('hidelefthideright');
          controlledBody.addClass('hideright');
        } else {
          controlledBody.removeClass('hideleft');
        }
      } else {
        // We add the hideleft class
        if (controlledBody.hasClass('hideright')) {
          controlledBody.addClass('hidelefthideright');
          controlledBody.removeClass('hideright');
        } else {
          controlledBody.addClass('hideleft');
        }
      }
    }
  }
  var togglePanels = function (toggler, isRight) {
    let newExpandedState = toggler.attr('aria-expanded') === 'false';

    if (newExpandedState) {
      // If we want to expand the panel, we first show it, then we slide it in view.
      updateBody(isRight, newExpandedState);
      toggler.attr('aria-expanded', newExpandedState);
    } else {
      // If we want to collapse the panel, we first slide it out of view and then we hide it.
      toggler.attr('aria-expanded', newExpandedState);
      let panels;
      if (isRight) {
        panels = $('#rightPanels');
      } else {
        panels = $('#leftPanels');
      }
      panels.on('transitionend', function () {
        updateBody(isRight, newExpandedState);
        // Remove this listener after one call.
        panels.off('transitionend');
      });
    }
  }

  rightToggler.on('click', function(){togglePanels(rightToggler, true)});
  leftToggler.on('click', function(){togglePanels(leftToggler, false)});
});
