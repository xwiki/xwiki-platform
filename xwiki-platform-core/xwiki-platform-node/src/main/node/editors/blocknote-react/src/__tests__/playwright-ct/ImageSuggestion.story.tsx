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
import { depsContainerMock } from "./depsContainer.mock";
import { FULL_SYNTAX } from "./syntax.mock";
import { BlockNoteViewWrapper } from "../../components/BlockNoteViewWrapper";
import { useMemo, useState } from "react";
// NOTE: `LinkType` is intentionally not imported here (not even type-only): the module also
// transitively exports classes using decorators, which the Playwright Node-side transform (unlike
// the Vite pipeline used to bundle this story) can't parse.
import type { BlockNoteViewWrapperProps } from "../../components/BlockNoteViewWrapper";
import type { ImageEditionHooks } from "../../components/images/imageEditionHooks";
import type {
  ImageInsertionOverrideFn,
  ImageInsertionResult,
} from "../../components/images/imageInsertionOverride";
import type { LinkSuggestServiceProvider } from "@xwiki/platform-link-suggest-api";

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
  // When set, the image insertion is overridden, the override returning this result right away (the default image
  // selector is used otherwise).
  imageInsertionResult?: ImageInsertionResult;
  // The editor's initial content (an empty paragraph when not set).
  content?: BlockNoteViewWrapperProps["content"];
  // The syntax of the edited content (supporting everything when not set).
  syntax?: BlockNoteViewWrapperProps["syntax"];
};

/**
 * Renders the editor with a mocked LinkSuggestService feeding the image quick action's suggestions.
 *
 * This is a dedicated story (rather than a `BlockNoteForTest` prop) because it builds its own
 * `vitest-mock-extended` container: test-prop functions get proxied back to Node when called from the
 * mounted component, which strips any mock methods off objects passed through them (see
 * `LinkEdition.story.tsx` for the same constraint). Building the container here, where the code
 * actually runs in the browser, keeps the mock intact.
 */
export const ImageSuggestionForTest: React.FC<ImageSuggestionForTestProps> = ({
  images,
  linkSuggestServiceUnavailable,
  linkSuggestServiceFailing,
  uploadedImageURL,
  uploadedImageName,
  documentReference,
  imageInsertionResult,
  content,
  syntax,
}) => {
  // The images selected by the user, as reported by the image edition hooks.
  const [selectedImages, setSelectedImages] = useState<unknown[]>([]);
  // Stable, like the overrides of a real integration.
  const imageInsertion = useMemo<ImageInsertionOverrideFn | undefined>(
    () => imageInsertionResult && ((insert) => insert(imageInsertionResult)),
    [],
  );

  // Stable, like the hooks of a real integration, so that the editor doesn't rebuild its menus on each selection.
  const imageEditionHooks = useMemo<ImageEditionHooks>(
    () => ({
      afterUpdate: (block, reference) =>
        setSelectedImages((images) => [
          ...images,
          { blockId: block.id, url: block.props.url, reference },
        ]),
    }),
    [],
  );

  const depsContainer = useMemo(() => {
    const container = depsContainerMock();

    container.get.calledWith("LinkSuggestServiceProvider").mockReturnValue(
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
    container.get.calledWith("DocumentService").mockReturnValue({
      getCurrentDocumentReferenceString: () => ({ value: "Some.Page" }),
    });
    container.get.calledWith("AttachmentsService").mockReturnValue({
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
    container.get.calledWith("ModelReferenceParserProvider").mockReturnValue({
      get: () => ({
        parse: (page: string) => ({ name: page }),
        parseAsync: async (page: string) => ({ name: page }),
      }),
    });
    container.get
      .calledWith("ModelReferenceSerializerProvider")
      .mockReturnValue({
        get: () => ({
          serialize: (reference: {
            name: string;
            document: { name: string };
          }) => `attach:${reference.document.name}@${reference.name}`,
        }),
      });

    return container;
  }, []);

  return (
    <>
      <BlockNoteViewWrapper
        lang="en"
        label="Some Label"
        depsContainer={depsContainer}
        documentReference={documentReference}
        macros={false}
        content={content ?? []}
        syntax={syntax ?? FULL_SYNTAX}
        linkEditionHandler={() => {
          throw new Error("Link editor should not open for this test");
        }}
        overrides={{ imageEditionHooks, imageInsertion }}
      />
      <output data-testid="selectedImages">
        {JSON.stringify(selectedImages)}
      </output>
    </>
  );
};
