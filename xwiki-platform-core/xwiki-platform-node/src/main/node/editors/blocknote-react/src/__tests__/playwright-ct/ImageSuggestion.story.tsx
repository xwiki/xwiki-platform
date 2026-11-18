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
import { serviceContainerMock } from "./serviceContainer.mock";
import { FULL_SYNTAX } from "./syntax.mock";
import { BlockNoteViewWrapper } from "../../components/BlockNoteViewWrapper";
import { createBlockMetadataStore } from "../../misc/blockMetadata";
import {
  imageEditorName,
  imageMetadataHandlerName,
} from "../../services/imageEditor";
import { DocumentReference, SpaceReference } from "@xwiki/platform-model-api";
import { useMemo, useState } from "react";
// NOTE: `LinkType` is intentionally not imported here (not even type-only): the module also
// transitively exports classes using decorators, which the Playwright Node-side transform (unlike
// the Vite pipeline used to bundle this story) can't parse.
import type { BlockNoteViewWrapperProps } from "../../components/BlockNoteViewWrapper";
import type {
  ImageEditionData,
  ImageEditor,
  ImageMetadataHandler,
} from "../../services/imageEditor";
import type { LinkSuggestServiceProvider } from "@xwiki/platform-link-suggest-api";
import type { ResourceReference } from "@xwiki/platform-rendering-api";
import type { SyntaxConfig } from "@xwiki/platform-syntaxes-config";

const LINK_TYPE_ATTACHMENT = 1; // LinkType.ATTACHMENT

export type ImageSuggestionForTestProps = {
  // The hint (i.e. the location of the image) defaults to an empty string, and the reference to an attachment of
  // "Some.Page" named after the position of the image.
  images?: Array<{
    label: string;
    url: string;
    hint?: string;
    reference?: string;
  }>;
  // When set, the LinkSuggestService is unavailable, as with a backend that doesn't support search.
  linkSuggestServiceUnavailable?: boolean;
  // When set, searching for images fails, as when the backend can't be reached.
  linkSuggestServiceFailing?: boolean;
  // The URL of the uploaded image, whatever the file the user picks (uploading fails when not set).
  uploadedImageURL?: string;
  // The name of the uploaded image, as reported by the attachments service ("picture.png" when not set).
  uploadedImageName?: string;
  // The reference of the edited document (the current document, "Some.Page", is edited when not set).
  documentReference?: string;
  // When set, an image editor is bound, returning the given result right away (undefined when the user cancels). The
  // image selector of the editor is used otherwise.
  imageEditor?: { result?: ImageEditionData };
  // The resource references of the images of the initial content, by block id, as kept by the image metadata handler.
  imageReferences?: Record<string, ResourceReference>;
  // The editor's initial content (an empty paragraph when not set).
  content?: BlockNoteViewWrapperProps["content"];
  // The syntax of the edited content (supporting everything when not set).
  syntax?: SyntaxConfig;
  // The upload option of the editor.
  upload?: boolean;
};

/**
 * Renders the editor with a mocked LinkSuggestService feeding the image quick action's suggestions.
 *
 * This is a dedicated story (rather than a `BlockNoteForTest` prop) because it builds its own container of mocked
 * services: test-prop functions get proxied back to Node when called from the mounted component, which strips any
 * methods off objects passed through them (see `LinkEdition.story.tsx` for the same constraint). Building the
 * container here, where the code actually runs in the browser, keeps the mocked services intact.
 */
export const ImageSuggestionForTest: React.FC<ImageSuggestionForTestProps> = ({
  images,
  linkSuggestServiceUnavailable,
  linkSuggestServiceFailing,
  uploadedImageURL,
  uploadedImageName,
  documentReference,
  imageEditor,
  imageReferences,
  content,
  syntax,
  upload,
}) => {
  // The images selected by the user, as reported to the image metadata handler.
  const [selectedImages, setSelectedImages] = useState<unknown[]>([]);
  // The calls to the image editor.
  const [imageEditorCalls, setImageEditorCalls] = useState<unknown[]>([]);

  // Holds the references of the images, like the store of a real integration.
  const metadata = useMemo(() => {
    const store = createBlockMetadataStore();
    Object.entries(imageReferences ?? {}).forEach(([id, reference]) =>
      store.set(id, "reference", reference),
    );
    return store;
  }, []);

  const serviceContainer = useMemo(() => {
    const container = serviceContainerMock([syntax ?? FULL_SYNTAX]);

    container
      .bind<ImageMetadataHandler>(imageMetadataHandlerName)
      .toConstantValue({
        beforeEdit: (instance, block) => ({
          ...block.props,
          reference: instance.metadata.get(block.id)?.reference as
            | ResourceReference
            | undefined,
        }),
        afterUpdate: (instance, block, reference) => {
          instance.metadata.set(block.id, "reference", reference);
          setSelectedImages((images) => [
            ...images,
            { blockId: block.id, url: block.props.url, reference },
          ]);
        },
      });

    if (imageEditor) {
      container.bind<ImageEditor>(imageEditorName).toConstantValue({
        async insert() {
          setImageEditorCalls((calls) => [...calls, { action: "insert" }]);
          return imageEditor.result;
        },
        async edit(_instance, image) {
          setImageEditorCalls((calls) => [...calls, { action: "edit", image }]);
          return imageEditor.result;
        },
      });
    }

    container.rebind("LinkSuggestServiceProvider").toConstantValue(
      linkSuggestServiceUnavailable
        ? { get: () => undefined }
        : ({
            get: () => ({
              async getLinks() {
                if (linkSuggestServiceFailing) {
                  throw new Error("Failed to reach the backend");
                }

                return (images ?? []).map((image, index) => ({
                  id: `image-${index}`,
                  hint: image.hint ?? "",
                  label: image.label,
                  reference: image.reference ?? `Some.Page@image-${index}.png`,
                  type: LINK_TYPE_ATTACHMENT,
                  url: image.url,
                }));
              },
            }),
          } satisfies LinkSuggestServiceProvider),
    );

    // A page reference that the model reference parser of the mocked container understands.
    container.rebind("DocumentService").toConstantValue({
      getCurrentDocumentReferenceString: () => ({ value: "Some.Page" }),
    });
    container.rebind("AttachmentsService").toConstantValue({
      async upload() {
        if (!uploadedImageURL) {
          throw new Error("Failed to upload the file");
        }
        return [
          {
            id: "uploaded image",
            name: uploadedImageName ?? "picture.png",
            mimetype: "image/png",
            href: uploadedImageURL,
            date: new Date(),
            size: 12,
            author: undefined,
          },
        ];
      },
    });
    // Parses and serializes the page and attachment references like XWiki does, in order to check that the uploaded
    // images are inserted with the reference of the uploaded attachment, on the page it was uploaded to.
    container.rebind("ModelReferenceParserProvider").toConstantValue({
      get: () => ({
        parse: (page: string) => ({ name: page }),
        parseAsync: async (page: string) => ({ name: page }),
      }),
    });
    container.rebind("ModelReferenceSerializerProvider").toConstantValue({
      get: () => ({
        serialize: (reference: {
          name: string;
          document?: { name: string };
        }) =>
          reference.document
            ? `attach:${reference.document.name}@${reference.name}`
            : reference.name,
      }),
    });

    return container;
  }, []);

  return (
    <>
      <BlockNoteViewWrapper
        options={{
          lang: "en",
          label: "Some Label",
          serviceContainer,
          sourceDocumentReference:
            documentReference === undefined
              ? undefined
              : // The mocked model reference serializer serializes a page reference as its name.
                new DocumentReference(documentReference, new SpaceReference()),
          syntax: (syntax ?? FULL_SYNTAX).id,
          metadata,
          upload,
        }}
        content={content ?? []}
      />
      <output data-testid="selectedImages">
        {JSON.stringify(selectedImages)}
      </output>
      <output data-testid="imageEditorCalls">
        {JSON.stringify(imageEditorCalls)}
      </output>
    </>
  );
};
