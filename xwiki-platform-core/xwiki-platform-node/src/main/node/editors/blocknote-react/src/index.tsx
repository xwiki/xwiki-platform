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
import { App } from "./App";
import { createRoot } from "react-dom/client";
import type { BlockNoteViewWrapperProps } from "./components/BlockNoteViewWrapper";

/**
 * Mount a BlockNote editor inside a DOM container
 *
 * @param containerEl - The container to put BlockNote in (must be empty and be a block type component, e.g. `<div>`)
 * @param props - Properties to setup the editor with
 *
 * @returns - An unmount function to properly dispose of the editor
 *
 * @since 18.0.0RC1
 * @beta
 */
function mountBlockNote(
  containerEl: HTMLElement,
  props: BlockNoteViewWrapperProps,
): { unmount: () => void } {
  const root = createRoot(containerEl);

  root.render(<App {...props} />);

  let unmounted = false;

  return {
    unmount() {
      if (unmounted) {
        throw new Error("BlockNote is already unmounted!");
      }

      unmounted = true;
      root.unmount();
    },
  };
}

export { mountBlockNote };
export type { BlockNoteViewWrapperProps };

export type {
  BlockOfType,
  BlockType,
  EditorBlockSchema,
  EditorInlineContentSchema,
  EditorLanguage,
  EditorLink,
  EditorSchema,
  EditorStyleSchema,
  EditorStyledText,
  EditorType,
  InlineContentType,
} from "./blocknote";

export type { DefaultBlockNoteEditorOptions } from "./components/BlockNoteViewWrapper";

export type {
  ImageEditionOverrideFn,
  ImageUpdateResult,
} from "./components/images/CustomImageToolbar";

export type { LinkData } from "./components/links/LinkEditor";

export type { LinkEditionHooks } from "./components/links/linkEditionHooks";

export {
  MACRO_NAME_PREFIX,
  buildMacroRawContent,
  extractMacroRawContent,
} from "./blocknote/utils";

export type {
  ContextForMacros,
  InlineMacroInvocation,
  MacroBlockInvocation,
  MacroCall,
  MacroCallParams,
  MacroInsertionEditorPrefillData,
} from "./blocknote/utils";
