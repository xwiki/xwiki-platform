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
import {
  XWIKI_GENERATED_LABEL,
  XWIKI_REFERENCE,
  getLinkMetadata,
  linkMetadataHandlerName,
  setLinkMetadata,
  stripLinkMetadata,
} from "@xwiki/platform-editors-blocknote-react";
import { toXWikiEntityReference } from "@xwiki/platform-model-xwiki";
import { Container, injectable } from "inversify";
import type {
  EditorInstance,
  LinkEditionData,
  LinkMetadataHandler,
} from "@xwiki/platform-editors-blocknote-react";
import type { ResourceReference } from "@xwiki/platform-rendering-api";

/**
 * Default LinkMetadataHandler implementation, binding the links to their XWiki metadata. The metadata of a link (its
 * resource reference, its parameters, its freestanding flag and its generated label) is kept in the link URL (see
 * `setLinkMetadata`), because the BlockNote links can't have properties. Being part of the edited content, it is
 * shared with the other users editing in realtime, and it is restored by undo / redo along with the link.
 *
 * @since 18.10.0RC1
 */
@injectable("Singleton")
export class DefaultLinkMetadataHandler implements LinkMetadataHandler {
  public static bind(container: Container): void {
    container
      .bind<LinkMetadataHandler>(linkMetadataHandlerName)
      .to(DefaultLinkMetadataHandler)
      .inSingletonScope();
  }

  // Called before the link editor is opened on an existing link. We read the metadata of the link from its URL to
  // pre-fill the link editor, and give the link editor the URL without the metadata.
  public beforeEdit(
    _instance: EditorInstance,
    linkData: LinkEditionData,
  ): LinkEditionData {
    const metadata = getLinkMetadata(linkData.url);
    if (!Object.keys(metadata).length) {
      // Leave the URL exactly as the user entered it (handling the metadata normalizes the URL, e.g. adding a trailing
      // slash).
      return linkData;
    }
    const generatedLabel = metadata[XWIKI_GENERATED_LABEL.name];
    return {
      ...linkData,
      url: stripLinkMetadata(linkData.url),
      reference: metadata[XWIKI_REFERENCE.name] as
        | ResourceReference
        | undefined,
      // The label of the link is generated as long as it's the one generated for the link target: the user may have
      // changed it in the edited content since.
      generatedTitle:
        typeof generatedLabel === "string" && generatedLabel === linkData.title,
    };
  }

  // Called right before the link is written into the content. We put the (possibly updated) metadata of the link in its
  // URL. When editing, we start from the metadata of the edited link (taken from the previous link data), so that what
  // the link editor doesn't edit (the parameters, the freestanding flag) is preserved, even when the target changes.
  public beforeUpdate(
    instance: EditorInstance,
    linkData: LinkEditionData,
    previous?: LinkEditionData,
  ): LinkEditionData {
    const metadata = getLinkMetadata(previous?.url ?? "");
    // Preserve what the link editor doesn't edit when the link still points to the same resource, otherwise report the
    // new reference, so that the edited link points to the newly selected resource. Without a reference, the link is
    // saved from its URL.
    const { sourceDocumentReference } = instance.options;
    metadata[XWIKI_REFERENCE.name] = linkData.reference
      ? editedLinkResourceReference(
          metadata[XWIKI_REFERENCE.name] as ResourceReference | undefined,
          linkData.reference,
          sourceDocumentReference
            ? toXWikiEntityReference(sourceDocumentReference)
            : undefined,
        )
      : undefined;
    // A generated label is not saved, so that it's generated again (server-side) from the link target. A label chosen
    // by the user is saved, even when it's the same as the generated one.
    metadata[XWIKI_GENERATED_LABEL.name] = linkData.generatedTitle
      ? linkData.title
      : undefined;
    return {
      ...linkData,
      url: setLinkMetadata(stripLinkMetadata(linkData.url), metadata),
    };
  }
}
