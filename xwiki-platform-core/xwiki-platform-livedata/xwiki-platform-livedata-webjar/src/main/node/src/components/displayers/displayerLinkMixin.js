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
 * The displayerLinkMixin is a vue mixin containing the computed values required for the
 * DisplayerLink component or other components reusing it (for instance, DisplayerDocTitle).
 */
export default {
  computed: {
    htmlValue() {
      const container = document.createElement("div");
      container[this.config.html ? "innerHTML" : "textContent"] = this.value;
      // Remove the interactive content because it isn't allowed inside an anchor element.
      // See https://developer.mozilla.org/en-US/docs/Web/HTML/Element/a#properties
      // See
      // https://developer.mozilla.org/en-US/docs/Web/Guide/HTML/Content_categories#interactive_content
      const interactiveContent = "a, button, details, embed, iframe, keygen, label, select, textarea, audio[controls],"
        + "img[usemap], input, menu[type=toolbar], object[usemap], video[controls]";
      [...container.querySelectorAll(interactiveContent)].forEach(
        node => node.parentNode.removeChild(node));
      return container.innerHTML.trim();
    },
  },
};
