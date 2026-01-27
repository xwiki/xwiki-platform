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
import { createCustomInlineContentSpec } from "../utils";

export const SubScript = createCustomInlineContentSpec({
  config: {
    type: "subscript",
    content: "styled",
    propSchema: {},
  },
  implementation: {
    render: ({ contentRef }) => {
      return <sub ref={contentRef} />;
    },
  },
  customToolbar: null,
  slashMenu: {
    default: () => ({
      type: "subscript" as const,
      // TODO: Currently required as the element would be invisible and un-editable if it was empty
      // Tracking issue: https://github.com/TypeCellOS/BlockNote/issues/2359
      content: [{ type: "text" as const, text: "subscript", styles: {} }],
    }),
    group: "Others",
    title: "SubScript",
    icon: "S",
  },
});
