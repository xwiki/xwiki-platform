import { SuggestionProps } from "@tiptap/suggestion";
import { Editor, Range } from "@tiptap/core";

export interface CommandParams {
  editor: Editor;
  range: Range;
  props: SuggestionProps<unknown>;
}
