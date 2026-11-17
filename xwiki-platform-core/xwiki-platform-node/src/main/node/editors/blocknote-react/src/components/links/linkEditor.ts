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
import { LinkToolbarExtension } from "@blocknote/core/extensions";
import { useContext } from "react";
import type { EditorInstance } from "../../editorInstance";
import type { ResourceReference } from "@xwiki/platform-rendering-api";

/**
 * The name under which the {@link LinkEditor} is bound in the service container.
 *
 * @since 18.10.0RC1
 * @beta
 */
const linkEditorName = "LinkEditor";

/**
 * The name under which the {@link LinkMetadataHandler} is bound in the service container.
 *
 * @since 18.10.0RC1
 * @beta
 */
const linkMetadataHandlerName = "LinkMetadataHandler";

/**
 * The data describing a link being created or edited.
 *
 * @since 18.7.0RC1
 * @beta
 */
type LinkEditionData = {
  /**
   * The link label.
   */
  title: string;

  /**
   * Whether the link label is generated from the link target rather than chosen by the user, i.e. the link has no
   * label of its own (e.g. in XWiki syntax, `[[doc:Space.Page]]` rather than `[[label>>doc:Space.Page]]`). The link
   * editor doesn't show a generated label as the text of the link, so that the label follows the link target, and it
   * sets this when the user leaves the link text empty. It's up to the integration to tell which labels are generated
   * (see {@link LinkMetadataHandler}).
   *
   * @since 18.10.0RC1
   */
  generatedTitle?: boolean;

  /**
   * The link URL, as written into the edited content.
   */
  url: string;

  /**
   * The reference of the linked resource, when the integration provides one. The URL is a rendering
   * of this reference, so the reference is the authoritative link target.
   */
  reference?: ResourceReference;
};

/**
 * What the {@link LinkEditor} is asked to edit.
 *
 * @since 18.10.0RC1
 * @beta
 */
type LinkEditionRequest = {
  /**
   * The link to edit, or the values to prefill the link editor with when creating a new link (e.g. the selected text,
   * as the link label).
   */
  current: LinkEditionData;

  /**
   * Whether a new link is created, or an existing one is edited.
   */
  mode: "createNew" | "editExisting";
};

/**
 * The UI (e.g. a modal) letting the user choose the target and the label of a link. The actions to create and edit
 * links with it are available only when it is bound in the service container (see {@link linkEditorName}).
 *
 * @since 18.10.0RC1
 * @beta
 */
interface LinkEditor {
  /**
   * Lets the user create or edit a link.
   *
   * @param instance - the editor instance holding the link
   * @param request - the link to edit, and whether it's a new link
   * @returns the link to write into the content, or undefined if the user canceled
   */
  edit(
    instance: EditorInstance,
    request: LinkEditionRequest,
  ): Promise<LinkEditionData | undefined>;
}

/**
 * Keeps the data about the links that the content can't hold (e.g. their resource reference, or whether their label is
 * generated), when it is bound in the service container (see {@link linkMetadataHandlerName}).
 *
 * @since 18.10.0RC1
 * @beta
 */
interface LinkMetadataHandler {
  /**
   * Called before the {@link LinkEditor} is opened on an existing link.
   *
   * @param instance - the editor instance holding the link
   * @param link - the link, as stored in the content
   * @returns the link to edit, e.g. with the resource reference of the link target
   */
  beforeEdit(instance: EditorInstance, link: LinkEditionData): LinkEditionData;

  /**
   * Called right before a link is written into the content, whatever the UI used (link suggestions, upload, or the
   * {@link LinkEditor}). Throwing cancels the write.
   *
   * @param instance - the editor instance holding the link
   * @param link - the link about to be written
   * @param previous - the edited link, exactly as it is stored in the content, i.e. *not* transformed by
   *   {@link LinkMetadataHandler.beforeEdit}; undefined when creating a new link. This lets the handler tell an edit
   *   (which should preserve the metadata bound to the existing link) apart from a creation.
   * @returns the link to write
   */
  beforeUpdate(
    instance: EditorInstance,
    link: LinkEditionData,
    previous?: LinkEditionData,
  ): LinkEditionData;
}

/**
 * The link editor of an editor instance, as used by the editor UI: the link returned by the link editor is applied,
 * and the failures are logged. The caller decides where the link is written.
 */
type LinkEditing = {
  /**
   * @param request - the link to edit, and whether it's a new link
   * @param apply - writes the link returned by the link editor
   * @returns a promise resolved once the link editor is closed, whatever the outcome
   */
  edit(
    request: LinkEditionRequest,
    apply: (link: LinkEditionData) => void,
  ): Promise<void>;
};

/**
 * @param instance - the editor instance
 * @returns the link metadata handler bound in the service container of the given editor instance, if any
 */
function getLinkMetadataHandler(
  instance: EditorInstance,
): LinkMetadataHandler | undefined {
  return instance.serviceContainer.isBound(linkMetadataHandlerName)
    ? instance.serviceContainer.get<LinkMetadataHandler>(
        linkMetadataHandlerName,
      )
    : undefined;
}

/**
 * @param instance - the editor instance holding the link
 * @param link - the link, as stored in the content
 * @returns the link to edit with the link editor, as provided by the link metadata handler, when bound
 */
function getEditedLink(
  instance: EditorInstance,
  link: LinkEditionData,
): LinkEditionData {
  return getLinkMetadataHandler(instance)?.beforeEdit(instance, link) ?? link;
}

/**
 * Writes a new link into the content, at the selection of the given editor instance, once the link metadata handler
 * had its say, then focuses the editor back, so that the user can keep typing after the link.
 *
 * @param instance - the editor instance to insert the link into
 * @param link - the link to insert
 * @param selectedText - the text selected when the link was configured: it is linked in place, without being replaced
 *   (which would strip the inline formatting it doesn't share), as long as the link label is that same text. When not
 *   specified, or when the label differs (the user changed the display text, or emptied it to have the label generated
 *   from the target), the selection is replaced with the link label.
 */
function insertLink(
  instance: EditorInstance,
  link: LinkEditionData,
  selectedText?: string,
): void {
  const { url, title } =
    getLinkMetadataHandler(instance)?.beforeUpdate(instance, link) ?? link;
  instance.editor.createLink(url, title === selectedText ? undefined : title);
  instance.editor.focus();
}

/**
 * Updates a link of the content of the given editor instance, once the link metadata handler had its say.
 *
 * @param instance - the editor instance holding the link
 * @param link - the updated link
 * @param previous - the edited link, as stored in the content
 * @param position - a position inside the edited link
 */
function updateLink(
  instance: EditorInstance,
  link: LinkEditionData,
  previous: LinkEditionData,
  position: number,
): void {
  const { url, title } =
    getLinkMetadataHandler(instance)?.beforeUpdate(instance, link, previous) ??
    link;
  instance.editor
    .getExtension(LinkToolbarExtension)
    ?.editLink(url, title, position);
}

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
    edit: async (request, apply) => {
      try {
        const link = await linkEditor.edit(instance, request);
        if (link) {
          apply(link);
        }
      } catch (error) {
        console.error("Failed to edit the link.", error);
      }
    },
  };
}

/**
 * @returns the link editing of the editor instance the component is part of, or undefined if not available
 */
function useLinkEditing(): LinkEditing | undefined {
  return getLinkEditing(useContext(EditorInstanceContext));
}

export {
  getEditedLink,
  getLinkEditing,
  insertLink,
  linkEditorName,
  linkMetadataHandlerName,
  updateLink,
  useLinkEditing,
};
export type {
  LinkEditing,
  LinkEditionData,
  LinkEditionRequest,
  LinkEditor,
  LinkMetadataHandler,
};
