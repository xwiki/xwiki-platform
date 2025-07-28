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
  $(document).on("xwiki:actions:beforePreview xwiki:actions:beforeSave", () => {
    // Make sure that all BlockNote instances update their data before the form is submitted.
    factory.getAll().forEach((blockNote) => blockNote.data);
  });

  $(document).on("xwiki:dom:updated", init);
  $(init);
});

export { factory };
