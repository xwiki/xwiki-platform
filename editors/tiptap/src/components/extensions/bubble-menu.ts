/*
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

import CTiptapLinkEdit from "../../vue/c-tiptap-link-edit.vue";
import { Primitive } from "@tiptap/vue-3";
import { type Editor } from "@tiptap/core";
import { EditorState } from "@tiptap/pm/state";
import { Range } from "@tiptap/core";
import { Level } from "@tiptap/extension-heading";
import { type Component } from "vue";

// TODO: also add condition, for instance some actions shouldn't be proposed on
// code.
/**
 * Bubble menu action descriptor.
 *
 * @since 0.8
 */
export interface BubbleMenuAction {
  title: string;
  additionalComponent?: Component;
  command: (
    params: { editor: Editor; range: Range },
    props?: Record<string, Primitive>,
  ) => void;
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
    isActive: () => editor.schema.marks.bold !== undefined,
  };
}

function getItalic(editor: Editor): BubbleMenuAction {
  return {
    title: "Italic",
    icon: "type-italic",
    command({ editor }) {
      editor.commands.toggleItalic();
    },
    isActive: () => editor.schema.marks.italic != undefined,
  };
}

function getCode(editor: Editor): BubbleMenuAction {
  return {
    title: "Code",
    icon: "code",
    command({ editor }) {
      editor.commands.toggleCode();
    },
    isActive: () => editor.schema.marks.code != undefined,
  };
}

function getHeadingAction(level: Level, editor: Editor): BubbleMenuAction {
  return {
    title: `H${level}`,
    icon: `type-h${level}`,
    command({ editor }) {
      editor.chain().toggleHeading({ level }).run();
    },
    isActive: () => editor.schema.nodes.heading != undefined,
  };
}

export function getLinkAction(editor: Editor): BubbleMenuAction {
  return {
    title: "Link",
    icon: "link-45deg",
    additionalComponent: CTiptapLinkEdit,
    // @ts-expect-error TODO the types needs to be refined
    command(
      { editor },
      { linkValue, removeLink }: { linkValue?: string; removeLink?: boolean },
    ) {
      if (linkValue) {
        editor
          .chain()
          .focus()
          .extendMarkRange("link")
          .setLink({ href: linkValue })
          .run();
      }

      if (removeLink) {
        editor.commands.unsetLink();
      }
    },
    isActive: () => editor.schema.marks.link != undefined,
  };
}

export default function getMenuActions(editor: Editor): BubbleMenuAction[] {
  const numbers: Level[] = [1, 2, 3, 4, 5, 6];
  const headings: BubbleMenuAction[] = numbers.map((level) =>
    getHeadingAction(level, editor),
  );
  const action = [
    ...headings,
    getBoldAction(editor),
    getItalic(editor),
    getCode(editor),
    getLinkAction(editor),
  ];
  // Keep only actions at can be activated in the current context:
  // Actions with marks and nodes that are not available should not be available
  // Some actions can also be deactivated when the current selection does not
  // allow it (e.g., not possible to define bold text inside a code block).
  return action.filter((action) => {
    return action.isActive(editor.state);
  });
}
