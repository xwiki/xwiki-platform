/**
 * See the LICENSE file distributed with this work for additional
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

import { createCustomBlockSpec } from "../utils";

function createCustomHeading(level: 4 | 5 | 6) {
  return createCustomBlockSpec({
    config: {
      type: `Heading${level}`,
      content: "inline",
      propSchema: {},
    },
    implementation: {
      render({ contentRef }) {
        return {
          [4]: <h4 ref={contentRef}></h4>,
          [5]: <h5 ref={contentRef}></h5>,
          [6]: <h6 ref={contentRef}></h6>,
        }[level];
      },
    },
    slashMenu: {
      title: `Heading ${level}`,
      group: "Headings",
      icon: <>H</>,
      aliases: [],
      default: () => ({
        type: `Heading${level}`,
      }),
    },
    customToolbar: null,
  });
}

/**
 * Level 4 heading
 *
 * Required as BlockNote does not implement heading levels greater than 3
 *
 * @since 0.16
 * @beta
 */
const Heading4 = createCustomHeading(4);

/**
 * Level 5 heading
 *
 * Required as BlockNote does not implement heading levels greater than 3
 *
 * @since 0.16
 * @beta
 */
const Heading5 = createCustomHeading(5);

/**
 * Level 6 heading
 *
 * Required as BlockNote does not implement heading levels greater than 3
 *
 * @since 0.16
 * @beta
 */

const Heading6 = createCustomHeading(6);

export { Heading4, Heading5, Heading6 };
