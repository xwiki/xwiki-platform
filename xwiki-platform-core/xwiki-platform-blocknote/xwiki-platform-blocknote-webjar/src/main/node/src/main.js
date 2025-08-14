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
import { Factory } from "./services/Factory";
import "./services/inplace";

const factory = new Factory();

define("xwiki-blocknote", [], () => factory);

function init(event, data) {
  const containers = data?.elements || [document];
  containers
    .flatMap((container) => [...container.querySelectorAll(".xwiki-blocknote-wrapper")])
    .forEach((host) => factory.create(host));
}

// eslint-disable-next-line @typescript-eslint/no-require-imports
require(["jquery", "xwiki-events-bridge"], ($) => {
  $(document).on("xwiki:actions:beforePreview xwiki:actions:beforeSave", async (event) => {
    // Make sure that all BlockNote instances update their data before the form is submitted.
    const dataAsynchronousSerializationKey = 'xwiki-blocknote-asynchronous-serialization';
    const target = $(event.target);
    const firstEvent = target.data(dataAsynchronousSerializationKey) !== 'true';
    // On the first click, prevent the event, let the editors serialize their content asynchronously, then resubmit.
    // The second programmatically triggered click will just remove the data marker and pass through to let the content
    // be saved.
    if (firstEvent) {
      event.preventDefault();
      event.stopImmediatePropagation();
      target.data(dataAsynchronousSerializationKey, 'true')
      await Promise.all(factory.getAll().map((blockNote) => blockNote.data()));
      event.target.click();
    } else {
      // Clear the data to let the user click again on save?
      target.removeData(dataAsynchronousSerializationKey)
    }
  });

  $(document).on("xwiki:dom:updated", init);
  $(init);
});

export { factory };
