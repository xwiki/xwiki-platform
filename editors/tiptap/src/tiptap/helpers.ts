import { MarkType, NodeType } from "@tiptap/pm/model";
import { EditorState } from "@tiptap/pm/state";
import { findParentNode } from "@tiptap/vue-3";
import { Primitive } from "utility-types";

export function isNodeActive(
  type: NodeType,
  attrs?: Record<string, Primitive>,
): (state: EditorState) => boolean {
  return (state) => {
    let isActive = false;
    if (type) {
      const nodeAfter = state.selection.$from.nodeAfter;
      let node = nodeAfter?.type == type ? nodeAfter : undefined;
      if (!node) {
        const parent = findParentNode((node) => node.type === type)(
          state.selection,
        );
        node = parent?.node;
      }

      if (node) {
        isActive = node?.hasMarkup(type, { ...node.attrs, ...attrs });
      }
    }
    return isActive;
  };
}

export function isMarkActive(type: MarkType): (state: EditorState) => boolean {
  return (state) => {
    let isActive = false;
    if (type) {
      const selection = state.selection;
      const empty = selection.empty;
      if (empty) {
        isActive = !!type.isInSet(state.storedMarks || selection.$from.marks());
      } else {
        isActive = state.doc.rangeHasMark(selection.from, selection.to, type);
      }
    }
    return isActive;
  };
}
