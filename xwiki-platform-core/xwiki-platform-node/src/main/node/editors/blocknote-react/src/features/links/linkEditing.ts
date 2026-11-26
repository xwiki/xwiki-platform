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
import { EditorInstanceContext } from "../../contexts";
import { linkEditorName } from "../../services/linkEditor";
import { useContext } from "react";
import type { EditorInstance } from "../../editorInstance";
import type { LinkEditionData, LinkEditor } from "../../services/linkEditor";

/**
 * The link editor of an editor instance, as used by the editor UI: the link returned by the link editor is applied,
 * and the failures are logged. The caller decides where the link is written.
 */
type LinkEditing = {
  /**
   * @param prefill - the values to prefill the link editor with
   * @param apply - inserts the link returned by the link editor
   * @returns a promise resolved once the link editor is closed, whatever the outcome
   */
  insert(
    prefill: Partial<LinkEditionData>,
    apply: (link: LinkEditionData) => void,
  ): Promise<void>;

  /**
   * @param link - the edited link
   * @param apply - writes the link returned by the link editor
   * @returns a promise resolved once the link editor is closed, whatever the outcome
   */
  edit(
    link: LinkEditionData,
    apply: (link: LinkEditionData) => void,
  ): Promise<void>;
};

/**
 * @param instance - the editor instance
 * @returns the link editing of the given editor instance, or undefined if no link editor is bound in its service
 *   container
 */
function getLinkEditing(
  instance: EditorInstance | null,
): LinkEditing | undefined {
  if (!instance?.serviceContainer.isBound(linkEditorName)) {
    return undefined;
  }
  const linkEditor = instance.serviceContainer.get<LinkEditor>(linkEditorName);
  return {
    insert: (prefill, apply) =>
      run(
        () => linkEditor.insert(instance, prefill),
        apply,
        "Failed to insert the link.",
      ),
    edit: (link, apply) =>
      run(
        () => linkEditor.edit(instance, link),
        apply,
        "Failed to edit the link.",
      ),
  };
}

/**
 * @param showLinkEditor - shows the link editor
 * @param apply - writes the link returned by the link editor, unless the user canceled
 * @param errorMessage - the message logged when the link editor fails
 */
async function run(
  showLinkEditor: () => Promise<LinkEditionData | undefined>,
  apply: (link: LinkEditionData) => void,
  errorMessage: string,
): Promise<void> {
  try {
    const link = await showLinkEditor();
    if (link) {
      apply(link);
    }
  } catch (error) {
    console.error(errorMessage, error);
  }
}

/**
 * @returns the link editing of the editor instance the component is part of, or undefined if not available
 */
function useLinkEditing(): LinkEditing | undefined {
  return getLinkEditing(useContext(EditorInstanceContext));
}

export { getLinkEditing, useLinkEditing };
export type { LinkEditing };
