/**
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
import { Factory } from "./services/Factory";
import { container } from "./services/container";
import "./services/inplace";

const factory = new Factory();

define("xwiki-blocknote", [], () => factory);

/**
 * The edited content is inside the form, so the form fields that macros output in it (e.g. Live Data filters) would be
 * submitted with the form. A disabled field set disables all the form fields it contains.
 *
 * @param host - the element that hosts the BlockNote instance
 */
function disableFormFieldsInContent(host) {
  const fieldSet = host.querySelector(".xwiki-blocknote-content");
  if (fieldSet) {
    fieldSet.disabled = true;
    // The form data is collected right after these events, whether the form is submitted or not.
    setTimeout(() => {
      fieldSet.disabled = false;
    }, 0);
  }
}

// The buttons in the edited content (e.g. output by macros) would submit the whole form, i.e. save the page.
document.addEventListener("submit", (event) => {
  const fieldSet = event.submitter?.closest(".xwiki-blocknote-content");
  if (fieldSet && fieldSet.form === event.target) {
    event.preventDefault();
  }
});

function init(event, data) {
  const containers = data?.elements || [document];
  containers
    .flatMap((container) => [
      ...container.querySelectorAll(".xwiki-blocknote-wrapper"),
    ])
    .forEach((host) => factory.create(host));
}

requirejs(["jquery", "xwiki-meta", "xwiki-events-bridge"], ($, xwikiMeta) => {
  // Expose the xwiki-meta RequireJS module as a component.
  container.bind("XWikiMeta").toConstantValue(xwikiMeta);

  $(document).on(
    "xwiki:actions:beforePreview xwiki:actions:beforeSave",
    (event, data) => {
      // Some code fires these events without the form (e.g. when switching editors), or on the whole document.
      const form = data?.form || $(event.target).closest("form")[0];
      factory.getAll().forEach((blockNote) => {
        // Make sure that all BlockNote instances update their data before the form is submitted.
        blockNote.data();
        if (!form || blockNote.host.closest("form") === form) {
          disableFormFieldsInContent(blockNote.host);
        }
      });
    },
  );

  $(document).on("xwiki:dom:updated", init);
  $(init);
});

export { factory };
