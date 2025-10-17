/**
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
import { App } from "./App";
import { MACRO_NAME_PREFIX } from "./blocknote/utils";
import { createRoot } from "react-dom/client";
import type { ContextForMacros } from "./blocknote/utils";
import type { BlockNoteViewWrapperProps } from "./components/BlockNoteViewWrapper";
import type { LinkEditionContext } from "./misc/linkSuggest";

/**
 * Mount a BlockNote editor inside a DOM container
 *
 * @param containerEl - The container to put BlockNote in (must be empty and be a block type component, e.g. `<div>`)
 * @param props - Properties to setup the editor with
 *
 * @returns - An unmount function to properly dispose of the editor
 *
 * @since 0.19
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

export { MACRO_NAME_PREFIX, mountBlockNote };

export type { BlockNoteViewWrapperProps, ContextForMacros, LinkEditionContext };

export type {
  BlockNoteConcreteMacro,
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
} from "./blocknote";

export {
  createBlockNoteSchema,
  createDictionary,
  querySuggestionsMenuItems,
} from "./blocknote";

export type {
  createCustomBlockSpec,
  createCustomInlineContentSpec,
} from "./blocknote/utils";
