/**
 * See the LICENSE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 * This file is part of the Cristal Wiki software prototype
 * @copyright  Copyright (c) 2023 XWiki SAS
 * @license    http://opensource.org/licenses/AGPL-3.0 AGPL-3.0
 *
 **/

import { isMarkActive, isNodeActive } from "../../tiptap/helpers";
import { Editor } from "@tiptap/vue-3";
import { EditorState } from "@tiptap/pm/state";
import { Range } from "@tiptap/core";
import { Level } from "@tiptap/extension-heading";

// TODO: also add condition, for instance some actions shouldn't be proposed on
// code.
/**
 * Bubble menu action descriptor.
 *
 * @sincer 0.8
 */
export interface BubbleMenuAction {
  title: string;
  command: (params: { editor: Editor; range: Range }) => void;
  isActive: (state: EditorState) => boolean;
  icon: string;
}

function getBoldAction(editor: Editor): BubbleMenuAction {
  return {
    title: "Bold",
    icon: "type-bold",
    command({ editor }) {
      editor.commands.toggleBold();
    },
    isActive: isMarkActive(editor.schema.marks.strong),
  };
}

function getItalic(editor: Editor): BubbleMenuAction {
  return {
    title: "Italic",
    icon: "type-italic",
    command({ editor }) {
      editor.commands.toggleItalic();
    },
    isActive: isMarkActive(editor.schema.marks.strong),
  };
}

function getCode(editor: Editor): BubbleMenuAction {
  return {
    title: "Code",
    icon: "code",
    command({ editor }) {
      editor.commands.toggleCode();
    },
    isActive: isMarkActive(editor.schema.marks.code),
  };
}

function getHeadingAction(level: Level, editor: Editor): BubbleMenuAction {
  return {
    title: `H${level}`,
    icon: `type-h${level}`,
    command({ editor }) {
      editor.chain().toggleHeading({ level }).run();
    },
    isActive: isNodeActive(editor.schema.nodes.heading, { level: level }),
  };
}

export default function getMenuActions(editor: Editor): BubbleMenuAction[] {
  const numbers: Level[] = [1, 2, 3, 4, 5, 6];
  const headings: BubbleMenuAction[] = numbers.map((level) =>
    getHeadingAction(level, editor),
  );
  return [
    ...headings,
    getBoldAction(editor),
    getItalic(editor),
    getCode(editor),
  ];
}
