import ImageInsertView from "./vue/ImageInsertView.vue";
import { Node, mergeAttributes } from "@tiptap/core";
import { VueNodeViewRenderer } from "@tiptap/vue-3";

const ImageInsertNode = Node.create({
  name: "imageInsert",
  group: "block",
  parseHTML() {
    return [{ tag: "image-insert-components" }];
  },
  renderHTML({ HTMLAttributes }) {
    return ["image-insert-components", mergeAttributes(HTMLAttributes)];
  },
  addNodeView() {
    return VueNodeViewRenderer(ImageInsertView);
  },
  addStorage() {
    return {
      markdown: {
        serialize: () => {
          // This element is only a temporary placeholder to pick and image, it is not ment to part of the persisted
          // syntax.
        },
      },
    };
  },
});

export { ImageInsertNode };
