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
import type { BlockNoteIterator } from "./BlockNoteIterator";
import type { BlockType } from "@xwiki/platform-editors-blocknote-react";

/**
 * The macro parameter used to send the id of a macro call (see `setMacroOutput`) to the server, when the edited content
 * is rendered, so that the output of each macro call can be associated with it. Macros ignore the parameters they don't
 * support, and the server preserves the parameters of the macro calls in the rendered content. The name is made only of
 * letters so that it's valid in every syntax the parameters and the content editable in-place can be rendered to.
 */
const MACRO_ID_PARAMETER = "xwikiBlockNoteMacroId";

/**
 * @param node - the node to test
 * @returns `true` if the given node is a macro node (block or inline macro). The inline macro type is compared as a
 *   string because it is not part of the (statically-typed) inline content schema: it is registered through an index
 *   signature on the editor side so that it doesn't tighten the schema, so its literal type is not available here.
 */
function isMacroNode(node: object): boolean {
  const type = (node as { type?: unknown }).type;
  return type === "xwikiMacroBlock" || type === "xwikiInlineMacro";
}

/**
 * Calls the given function for each macro node found in the parameters or the content of the given macro call (the
 * parameters and the content that are editable in-place are BlockNote fragments that can contain macro calls). The
 * macros nested in the found macros are not visited.
 *
 * @param iterator - used to iterate the fragments
 * @param call - the macro call object
 * @param visit - the function to call for each nested macro node
 */
function visitNestedMacros(
  iterator: BlockNoteIterator,
  call: unknown,
  visit: (macroNode: Record<string, unknown>) => void,
): void {
  if (call === null || typeof call !== "object") {
    return;
  }
  const { parameters, content } = call as {
    parameters?: Record<string, unknown>;
    content?: unknown;
  };
  [...Object.values(parameters ?? {}), content]
    .filter((value) => value !== null && typeof value === "object")
    .forEach((fragment) => {
      const nodes = Array.isArray(fragment) ? fragment : [fragment];
      // A fragment can also hold inline content, which the iterator handles as well.
      iterator.iterate(nodes as BlockType[], {
        visit: (node) => {
          if (typeof node === "object" && isMacroNode(node)) {
            visit(node as Record<string, unknown>);
            return true;
          }
          return false;
        },
      });
    });
}

export { MACRO_ID_PARAMETER, isMacroNode, visitNestedMacros };
