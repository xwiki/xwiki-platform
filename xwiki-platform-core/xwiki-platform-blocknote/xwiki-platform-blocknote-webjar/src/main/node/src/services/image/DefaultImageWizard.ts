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
import { Container, inject, injectable } from "inversify";
import type { ImageWizard, ImageWizardCallback } from "./ImageWizard";
import type {
  ResourceReference,
  ResourceReferenceParser,
} from "../model/reference/ResourceReferenceParser";
import type { XWikiEntityReference } from "../model/reference/XWikiEntityReference";
import type { StorageProvider } from "@xwiki/platform-backend-api";
import type { BlockOfType } from "@xwiki/platform-editors-blocknote-react";

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
 * Default ImageWizard implementation, using the Bootstrap-based Image Wizard provided by xwiki-platform-wysiwyg-webjar.
 *
 * @since 18.3.0RC1
 */
@injectable("Singleton")
export class DefaultImageWizard implements ImageWizard {
  public static bind(container: Container): void {
    container.bind("ImageWizard").to(DefaultImageWizard).inSingletonScope();
  }

  constructor(
    @inject("ResourceReferenceParser")
    private readonly resourceReferenceParser: ResourceReferenceParser,
    @inject("StorageProvider")
    private readonly storageProvider: StorageProvider,
  ) {}

  public insert(callback: ImageWizardCallback): void {
    this.showWizard(callback);
  }

  public edit(
    image: BlockOfType<"image">["props"],
    callback: ImageWizardCallback,
  ): void {
    this.showWizard(callback, image);
  }

  private showWizard(
    callback: ImageWizardCallback,
    image?: BlockOfType<"image">["props"],
  ): void {
    requirejs(["xwiki-wysiwyg-image-wizard"], (imageWizard) => {
      (imageWizard as XWikiWYSIWYGImageWizard)({
        captionAllowed: true,
        currentDocument: XWiki.currentDocument.documentReference,
        getImageResourceURL: this.getResourceURL.bind(this),
        imageData: image ? this.getImageData(image) : {},
        isHTML5: true,
        isInsert: !image,
        upload: this.upload.bind(this),
      })
        .then(this.getImageProperties.bind(this))
        .then(callback.submit)
        .catch(callback.cancel);
    });
  }

  private getResourceURL(
    resourceReference: ResourceReference,
    extraParams: Record<string, string>,
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
      return XWiki.currentDocument.getURL("get", parameters);
    }
  }

  /**
   * Convert BlockNote image properties to Image Wizard image data.
   *
   * @param image - image properties supported by BlockNote
   * @returns image data expected by the Image Wizard
   */
  private getImageData(
    image: BlockOfType<"image">["props"],
  ): Partial<ImageData> {
    return {
      alignment: editAlignment[image.textAlignment],
      hasCaption: !!image.caption,
      resourceReference: this.getImageResourceReference(image),
      src: image.url,
      width: image.previewWidth === undefined ? "" : `${image.previewWidth}px`,
    };
  }

  private getImageResourceReference(
    image: BlockOfType<"image">["props"],
  ): ResourceReference {
    return this.resourceReferenceParser.parse(image.url, { type: "attach" });
  }

  /**
   * Convert Image Wizard image data to BlockNote image properties.
   *
   * @param imageData - image data returned by the Image Wizard
   * @returns image properties supported by BlockNote
   */
  private getImageProperties(
    imageData: ImageData,
  ): Partial<BlockOfType<"image">["props"]> {
    return {
      previewWidth: Number.parseInt(imageData.width),
      textAlignment: saveAlignment[imageData.alignment],
      url: `${imageData.resourceReference.type}:${imageData.resourceReference.reference}`,
    };
  }

  /**
   * Uploads the given image file as an attachment of the edited document and notifies the Image Wizard about the upload
   * status.
   *
   * @param file - the image file to upload
   * @param callback - used to notify the Image Wizard about the upload progress
   */
  private async upload(file: File, callback: UploadCallback): Promise<void> {
    // For now, we assume the current document is being edited.
    const currentDocumentReference = XWiki.Model.serialize(
      XWiki.currentDocument.documentReference,
    );
    try {
      const result = await this.storageProvider
        .get()
        .saveAttachments(currentDocumentReference, [file]);
      if (result?.[0]) {
        callback.onSuccess(
          XWiki.Model.resolve(result[0], XWiki.EntityType.ATTACHMENT),
        );
      } else {
        callback.onAbort();
      }
    } catch (error) {
      callback.onError(
        error instanceof Error ? error : new Error(String(error)),
      );
    }
  }
}
