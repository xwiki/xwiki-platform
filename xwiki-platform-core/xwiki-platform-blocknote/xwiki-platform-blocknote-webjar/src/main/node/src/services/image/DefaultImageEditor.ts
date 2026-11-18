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
import {
  canUploadFiles,
  imageEditorName,
  uploadFile,
} from "@xwiki/platform-editors-blocknote-react";
import { toXWikiEntityReference } from "@xwiki/platform-model-xwiki";
import { AttachmentReference } from "@xwiki/platform-xwiki-model-api";
import { loadById } from "@xwiki/platform-xwiki-utils";
import { Container, inject, injectable } from "inversify";
import type {
  EditorInstance,
  ImageEditionData,
  ImageEditor,
} from "@xwiki/platform-editors-blocknote-react";
import type {
  ResourceReference,
  ResourceReferenceParser,
} from "@xwiki/platform-rendering-api";
import type { EntityReference as XWikiEntityReference } from "@xwiki/platform-xwiki-model-api";
import type { LegacyXWikiDocument } from "@xwiki/platform-xwiki-utils";

// The image alignment options supported by the Image Wizard.
type ImageAlignment = "none" | "start" | "center" | "end";

/**
 * The input / output for the Image Wizard provided by xwiki-platform-wysiwyg-webjar.
 */
type ImageData = {
  alignment: ImageAlignment;
  alt: string;
  border: boolean;
  hasCaption: boolean;
  height: string;
  imageStyle: string;
  isLocked: boolean;
  resourceReference: ResourceReference;
  src: string;
  textWrap: boolean;
  width: string;
};

// Maps BlockNote image alignment to Image Wizard alignment.
const editAlignment: Record<string, ImageAlignment> = {
  left: "start",
  center: "center",
  right: "end",
  justify: "none",
};

// Maps Image Wizard alignment to BlockNote image alignment.
const saveAlignment: Record<
  ImageAlignment,
  "left" | "center" | "right" | undefined
> = {
  start: "left",
  center: "center",
  end: "right",
  none: undefined,
};

/**
 * Used to notify the Image Wizard about the status of the image upload.
 */
type UploadCallback = {
  onSuccess(entityReference: XWikiEntityReference): void;
  onError(error: Error): void;
  onAbort(): void;
};

/**
 * The signature of the Image Wizard provided by xwiki-platform-wysiwyg-webjar.
 */
type XWikiWYSIWYGImageWizard = (options: {
  captionAllowed: boolean;
  currentDocument: XWikiEntityReference;
  getImageResourceURL: (
    resourceReference: ResourceReference,
    extraParams: Record<string, string>,
  ) => string;
  imageData: Partial<ImageData>;
  isHTML5: boolean;
  isInsert: boolean;
  upload: (file: File, callback: UploadCallback) => Promise<void>;
}) => Promise<ImageData>;

/**
 * Default ImageEditor implementation, using the Bootstrap-based Image Wizard provided by xwiki-platform-wysiwyg-webjar:
 * the images are uploaded to the source document of the edited content, and their references are relative to it.
 *
 * @since 18.10.0RC1
 */
@injectable("Singleton")
export class DefaultImageEditor implements ImageEditor {
  public static bind(container: Container): void {
    container
      .bind<ImageEditor>(imageEditorName)
      .to(DefaultImageEditor)
      .inSingletonScope();
  }

  constructor(
    @inject("ResourceReferenceParser")
    private readonly resourceReferenceParser: ResourceReferenceParser,
  ) {}

  public insert(
    instance: EditorInstance,
  ): Promise<ImageEditionData | undefined> {
    return this.showWizard(instance);
  }

  public edit(
    instance: EditorInstance,
    image: ImageEditionData,
  ): Promise<ImageEditionData | undefined> {
    return this.showWizard(instance, image);
  }

  /**
   * @param instance - the editor instance where the image is inserted or edited
   * @param image - the edited image, undefined when inserting an image
   * @returns the image chosen by the user, or undefined if the user canceled
   */
  private async showWizard(
    instance: EditorInstance,
    image?: ImageEditionData,
  ): Promise<ImageEditionData | undefined> {
    // The image references are relative to the source document, so the image previews are computed for it, and the
    // images are uploaded to it.
    const { sourceDocumentReference } = instance.options;
    const documentReference = sourceDocumentReference
      ? toXWikiEntityReference(sourceDocumentReference)
      : XWiki.currentDocument.documentReference;
    const sourceDocument = sourceDocumentReference
      ? new XWiki.Document(documentReference)
      : XWiki.currentDocument;
    let imageData: ImageData;
    try {
      const imageWizard = await loadById<XWikiWYSIWYGImageWizard>(
        "xwiki-wysiwyg-image-wizard",
      );
      imageData = await imageWizard({
        captionAllowed: true,
        currentDocument: documentReference,
        getImageResourceURL: (resourceReference, extraParams) =>
          this.getResourceURL(resourceReference, extraParams, sourceDocument),
        imageData: image ? this.getImageData(image) : {},
        isHTML5: true,
        isInsert: !image,
        upload: (file, uploadCallback) =>
          this.upload(instance, file, uploadCallback, documentReference),
      });
    } catch {
      // The Image Wizard rejects when the user closes it.
      return undefined;
    }
    return this.getImageProperties(imageData, image?.reference);
  }

  private getResourceURL(
    resourceReference: ResourceReference,
    extraParams: Record<string, string>,
    sourceDocument: LegacyXWikiDocument,
  ): string {
    if (["url", "path", "unc", "unknown"].includes(resourceReference.type)) {
      return resourceReference.reference;
    } else if (["mailto", "data"].includes(resourceReference.type)) {
      return resourceReference.type + ":" + resourceReference.reference;
    } else {
      const language = document.documentElement.getAttribute("lang") || "";
      const parameters = new URLSearchParams({
        sheet: "XWiki.WYSIWYG.ResourceDispatcher",
        outputSyntax: "plain",
        language,
        ...extraParams,
      });
      parameters.set("type", resourceReference.type);
      parameters.set("reference", resourceReference.reference);
      // The resource reference is resolved relative to the document the URL points to.
      return sourceDocument.getURL("get", parameters);
    }
  }

  /**
   * Convert BlockNote image properties to Image Wizard image data.
   *
   * @param image - image properties supported by BlockNote
   * @returns image data expected by the Image Wizard
   */
  private getImageData(image: ImageEditionData): Partial<ImageData> {
    return {
      alignment: image.textAlignment && editAlignment[image.textAlignment],
      hasCaption: !!image.caption,
      resourceReference:
        image.reference ?? this.getImageResourceReference(image),
      src: image.url,
      width: image.previewWidth === undefined ? "" : `${image.previewWidth}px`,
    };
  }

  private getImageResourceReference(
    image: ImageEditionData,
  ): ResourceReference {
    return this.resourceReferenceParser.parse(image.url ?? "", {
      type: "attach",
    });
  }

  /**
   * Convert Image Wizard image data to BlockNote image properties.
   *
   * @param imageData - image data returned by the Image Wizard
   * @returns image properties supported by BlockNote
   */
  private getImageProperties(
    imageData: ImageData,
    oldReference?: ResourceReference,
  ): ImageEditionData {
    const newReference = imageData.resourceReference;
    return {
      // The width is empty when the user doesn't set it (or removes it), in which case the image is displayed with its
      // natural width.
      previewWidth: imageData.width
        ? Number.parseInt(imageData.width)
        : undefined,
      textAlignment: saveAlignment[imageData.alignment],
      url: imageData.src,
      // Preserve the old reference (with its parameters) when the image itself doesn't change, otherwise report
      // the new reference so that the edited image points to the newly selected resource.
      reference: this.isSameResourceReference(oldReference, newReference)
        ? oldReference
        : newReference,
    };
  }

  private isSameResourceReference(
    alice?: ResourceReference,
    bob?: ResourceReference,
  ): boolean {
    return alice?.type === bob?.type && alice?.reference === bob?.reference;
  }

  /**
   * Uploads the given image file with the file uploader of the editor instance (which attaches it to the edited
   * document) and notifies the Image Wizard about the upload status.
   *
   * @param instance - the editor instance where the image is inserted or edited
   * @param file - the image file to upload
   * @param callback - used to notify the Image Wizard about the upload progress
   * @param documentReference - the reference of the edited document
   */
  private async upload(
    instance: EditorInstance,
    file: File,
    callback: UploadCallback,
    documentReference: XWikiEntityReference,
  ): Promise<void> {
    if (!canUploadFiles(instance)) {
      // The user can't upload files.
      callback.onAbort();
      return;
    }
    try {
      const { name } = await uploadFile(instance, file);
      callback.onSuccess(new AttachmentReference(name, documentReference));
    } catch (error) {
      callback.onError(
        error instanceof Error ? error : new Error(String(error)),
      );
    }
  }
}
