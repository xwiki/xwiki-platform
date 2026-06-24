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

import { createStyleSpec } from "@blocknote/core";

/**
 * BlockNote style spec for subscript text.
 * Renders as a `<sub>` HTML element.
 *
 * @since 18.6.0RC1
 * @beta
 */
const SubscriptStyle = createStyleSpec(
  { type: "subscript", propSchema: "boolean" },
  {
    render: () => {
      const sub = document.createElement("sub");
      return { dom: sub, contentDOM: sub };
    },
    parse: (element) => (element.tagName === "SUB" ? true : undefined),
  },
);

/**
 * BlockNote style spec for superscript text.
 * Renders as a `<sup>` HTML element.
 *
 * @since 18.6.0RC1
 * @beta
 */
const SuperscriptStyle = createStyleSpec(
  { type: "superscript", propSchema: "boolean" },
  {
    render: () => {
      const sup = document.createElement("sup");
      return { dom: sup, contentDOM: sup };
    },
    parse: (element) => (element.tagName === "SUP" ? true : undefined),
  },
);

export { SubscriptStyle, SuperscriptStyle };
