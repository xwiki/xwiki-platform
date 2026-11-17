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
import { editedLinkResourceReference } from "./editedLinkResourceReference";
import { extractLinkId, injectLinkId, stripLinkId } from "./linkId";
import { uuidv4 } from "lib0/random";
import type { BlockNoteDocument } from "./BlockNoteProcessor";
import type { LinkEditionHooks } from "@xwiki/platform-editors-blocknote-react";
import type { ResourceReference } from "@xwiki/platform-rendering-api";
import type { EntityReference as XWikiEntityReference } from "@xwiki/platform-xwiki-model-api";

/**
 * Creates the hooks that bind the links edited with the link editor to their XWiki metadata. The metadata of a link
 * (its resource reference, its parameters, its freestanding flag and its generated label) is kept outside the BlockNote
 * schema, mapped to a synthetic id that the link carries in its URL (see `linkId`).
 *
 * @param getBlockNoteDocument - provides the edited BlockNote document, which holds the link metadata
 * @param editedDocumentReference - the reference of the edited document, which the references of the linked pages and
 *   attachments are relative to
 * @returns the link edition hooks
 */
function createLinkEditionHooks(
  getBlockNoteDocument: () => BlockNoteDocument | undefined,
  editedDocumentReference?: XWikiEntityReference,
): LinkEditionHooks {
  return {
    // Runs before the link editor is opened on an existing link. We resolve the metadata of the link and inject it into
    // the link data to pre-fill the link editor. The synthetic id is stripped from the url passed to the link editor;
    // beforeUpdate recovers it from the previous link data.
    beforeEdit: (linkData) => {
      const id = extractLinkId(linkData.url);
      if (!id) {
        // The link doesn't have associated metadata (e.g. a resource reference) so we return the link data as-is.
        // Calling stripLinkId when the ID is missing is safe but the URL gets normalized by the URL constructor (e.g. a
        // trailing slash is added). We prefer to avoid that in order to keep the link URL exactly as the user entered
        // it.
        return linkData;
      }
      const metadata = getBlockNoteDocument()?.getMetadata(id);
      return {
        ...linkData,
        url: stripLinkId(linkData.url),
        reference: metadata?.xwikiReference as ResourceReference | undefined,
        // The label of the link is generated as long as it's the one generated for the link target: the user may have
        // changed it in the edited content since.
        generatedTitle:
          typeof metadata?.xwikiGeneratedLabel === "string" &&
          metadata.xwikiGeneratedLabel === linkData.title,
      };
    },

    // Runs right before the link is written into the content (i.e. before createLink / editLink). We store the
    // (possibly updated) metadata of the link, mapped to a synthetic id that we carry in the link url. When editing, we
    // reuse the id of the edited link (taken from the previous link data) so that the other link metadata (parameters,
    // freestanding flag) is preserved even when the target changes; when creating, we mint a new id.
    beforeUpdate: (linkData, previous) => {
      if (!linkData.reference && !linkData.generatedTitle) {
        // Without a reference the link is saved from its url, so leave that url exactly as the link editor produced
        // it: storing a synthetic id in it would round-trip it through the URL constructor, normalizing it (e.g.
        // adding a trailing slash).
        return linkData;
      }
      const id = extractLinkId(previous?.url ?? "") ?? uuidv4();
      const metadata = getBlockNoteDocument()!.getMetadata(id, true)!;
      if (linkData.reference) {
        // Preserve what the link editor doesn't edit when the link still points to the same resource, otherwise report
        // the new reference, so that the edited link points to the newly selected resource.
        metadata.xwikiReference = editedLinkResourceReference(
          metadata.xwikiReference as ResourceReference | undefined,
          linkData.reference,
          editedDocumentReference,
        );
      }
      // A generated label is not saved, so that it's generated again (server-side) from the link target. A label
      // chosen by the user is saved, even when it's the same as the generated one.
      if (linkData.generatedTitle) {
        metadata.xwikiGeneratedLabel = linkData.title;
      } else {
        delete metadata.xwikiGeneratedLabel;
      }
      return { ...linkData, url: injectLinkId(stripLinkId(linkData.url), id) };
    },
  };
}

export { createLinkEditionHooks };
