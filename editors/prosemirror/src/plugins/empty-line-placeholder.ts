import { Plugin, Selection } from "prosemirror-state";
import { Decoration, DecorationSet } from "prosemirror-view";
import { Node, ResolvedPos } from "prosemirror-model";

const findParentParagraph = (
  $pos: ResolvedPos,
): { pos: number; node: Node } | undefined => {
  for (let i = $pos.depth; i > 0; i--) {
    const node = $pos.node(i);
    if (node.type.name === "paragraph") {
      return {
        pos: i > 0 ? $pos.before(i) : 0,
        node,
      };
    }
  }

  return undefined;
};

export const emptyLinePlaceholder = new Plugin({
  props: {
    decorations(state) {
      const doc = state.doc;
      if (
        doc.childCount == 1 &&
        doc.firstChild?.isTextblock &&
        doc.firstChild?.content.size == 0
      ) {
        const parent = (({ $from }: Selection) => findParentParagraph($from))(
          state.selection,
        );

        const decorations = [];
        if (parent) {
          const decoration = Decoration.node(
            parent.pos,
            parent.pos + parent.node.nodeSize,
            {
              class: "placeholder",
              "data-empty-text": "Type here...",
            },
          );
          decorations.push(decoration);
        }
        return DecorationSet.create(doc, decorations);
      }
    },
  },
});
