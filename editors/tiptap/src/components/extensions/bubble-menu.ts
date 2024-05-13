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
