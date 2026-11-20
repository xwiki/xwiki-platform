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
import { toRelativeAttachmentResourceReference } from "../blocknote/attachmentResourceReference";
import {
  METADATA_PROP,
  XWIKI_REFERENCE,
  getBlockMetadata,
  imageMetadataHandlerName,
  setBlockMetadata,
} from "@xwiki/platform-editors-blocknote-react";
import { toXWikiEntityReference } from "@xwiki/platform-model-xwiki";
import { Container, injectable } from "inversify";
import type {
  BlockOfType,
  EditorInstance,
  ImageEditionData,
  ImageMetadataHandler,
} from "@xwiki/platform-editors-blocknote-react";
import type { ResourceReference } from "@xwiki/platform-rendering-api";

/**
 * Default ImageMetadataHandler implementation: the XWiki resource reference of an image is kept in the metadata of the
 * image block (see `setBlockMetadata`), so that the image is saved with the reference provided by the server rather
 * than with its URL, relative to the source document of the edited content. Being part of the edited content, it is
 * shared with the other users editing in realtime, and it is restored by undo / redo along with the image.
 *
 * @since 18.10.0RC1
 */
@injectable("Singleton")
export class DefaultImageMetadataHandler implements ImageMetadataHandler {
  public static bind(container: Container): void {
    container
      .bind<ImageMetadataHandler>(imageMetadataHandlerName)
      .to(DefaultImageMetadataHandler)
      .inSingletonScope();
  }

  public beforeEdit(
    _instance: EditorInstance,
    block: BlockOfType<"image">,
  ): ImageEditionData {
    // The image editor gets the image properties without the metadata, which it doesn't edit, and the reference of the
    // image, because the user may select a different image.
    const props: ImageEditionData = { ...block.props };
    delete (props as Record<string, unknown>)[METADATA_PROP];
    return {
      ...props,
      reference: getBlockMetadata(block)[XWIKI_REFERENCE.name] as
        | ResourceReference
        | undefined,
    };
  }

  public afterUpdate(
    instance: EditorInstance,
    block: BlockOfType<"image">,
    reference?: ResourceReference,
  ): void {
    // An image known only by its URL is saved with that URL, so we drop the reference of the previous image source.
    setBlockMetadata(
      instance.editor,
      block,
      XWIKI_REFERENCE,
      reference?.type === "attach"
        ? this.toImageAttachmentReference(instance, reference)
        : reference,
    );
  }

  private toImageAttachmentReference(
    instance: EditorInstance,
    reference: ResourceReference,
  ): ResourceReference {
    const { sourceDocumentReference } = instance.options;
    return {
      ...(sourceDocumentReference
        ? toRelativeAttachmentResourceReference(
            reference,
            toXWikiEntityReference(sourceDocumentReference),
          )
        : reference),
      // Untyped, like the image references written by the WYSIWYG editors (an image points to an attachment by
      // default).
      typed: false,
    };
  }
}
