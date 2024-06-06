import Link from "@tiptap/extension-link";
import { type Mark } from "prosemirror-model";

/**
 * Extends the default tiptap extension link with custom markdown serializion
 * rules to handle internal links.
 */
export default Link.extend({
  addStorage() {
    return {
      markdown: {
        serialize: {
          open(state: unknown, mark: Mark) {
            return mark.attrs.class?.includes("internal-link")
              ? "[["
              : // TODO: replace with a call to the default spec.
                "[";
          },
          close: function (state: unknown, mark: Mark) {
            if (mark.attrs.class?.includes("internal-link")) {
              return `|${mark.attrs.href}]]`;
            } else {
              // TODO: replace with a call to the default spec.
              return `](${mark.attrs.href.replace(/[()"]/g, "\\$&")}${
                mark.attrs.title
                  ? ` "${mark.attrs.title.replace(/"/g, '\\"')}"`
                  : ""
              })`;
            }
          },
          mixable: true,
        },
      },
    };
  },
});
