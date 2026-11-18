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
import {
  linkEditorName,
  linkMetadataHandlerName,
} from "../../components/links/linkEditor";
import {
  AttachmentReference,
  DocumentReference,
  EntityType,
  SpaceReference,
} from "@xwiki/platform-model-api";
import { useMemo, useState } from "react";
// NOTE: `LinkType` is intentionally not imported here (not even type-only): the module also
// transitively exports classes using decorators, which the Playwright Node-side transform (unlike
// the Vite pipeline used to bundle this story) can't parse.
import type { BlockNoteViewWrapperProps } from "../../components/BlockNoteViewWrapper";
import type {
  LinkEditionData,
  LinkEditor,
  LinkMetadataHandler,
} from "../../components/links/linkEditor";
import type { LinkSuggestServiceProvider } from "@xwiki/platform-link-suggest-api";
import type {
  ModelReferenceParserProvider,
  ModelReferenceSerializerProvider,
} from "@xwiki/platform-model-reference-api";
import type { SyntaxConfig } from "@xwiki/platform-syntaxes-config";
import type { Container } from "inversify";

const LINK_TYPE_PAGE = 0; // LinkType.PAGE
const LINK_TYPE_ATTACHMENT = 1; // LinkType.ATTACHMENT

/**
 * A suggested page or attachment. The hint (i.e. its location) defaults to an empty string, and the reference to a
 * page or an attachment named after its position.
 */
type SuggestedLink = {
  label: string;
  url: string;
  hint?: string;
  reference?: string;
};

export type LinkSuggestionForTestProps = {
  pages?: SuggestedLink[];
  attachments?: SuggestedLink[];
  // When set, the LinkSuggestService is unavailable, as with a backend that doesn't support search.
  linkSuggestServiceUnavailable?: boolean;
  // When set, searching for the links of that kind fails, as when the backend can't be reached.
  failingSearch?: "pages" | "attachments";
  // The URL of the uploaded file, whatever the file the user picks (uploading fails when not set).
  uploadedFileURL?: string;
  // The name of the uploaded attachment, as reported by the attachments service ("document.pdf" when not set).
  uploadedFileName?: string;
  // The link data submitted by the scripted link editor (which is cancelled when not set).
  linkEditorSubmit?: LinkEditionData;
  // When set, no link editor is bound.
  noLinkEditor?: boolean;
  // The editor's initial content (an empty paragraph when not set).
  content?: BlockNoteViewWrapperProps["content"];
  // The syntax of the edited content (supporting everything when not set).
  syntax?: SyntaxConfig;
  // The upload option of the editor.
  upload?: boolean;
};

/**
 * Binds a link metadata handler reporting the inserted links (and rewriting their URL, like the XWiki integration does,
 * to check that the rewritten URL is the one inserted), and a scripted link editor, when specified.
 */
function bindLinkServices(
  container: Container,
  {
    onInsert,
    linkEditor,
  }: {
    onInsert: (link: LinkEditionData) => void;
    linkEditor?: {
      onEdit: (input: unknown) => void;
      submit?: LinkEditionData;
    };
  },
): void {
  container.bind<LinkMetadataHandler>(linkMetadataHandlerName).toConstantValue({
    beforeEdit: (_instance, link) => link,
    beforeUpdate: (_instance, link) => {
      onInsert(link);
      return { ...link, url: `${link.url}#inserted` };
    },
  });

  if (linkEditor) {
    container.bind<LinkEditor>(linkEditorName).toConstantValue({
      async insert(_instance, prefill) {
        linkEditor.onEdit({ action: "insert", prefill });
        return linkEditor.submit;
      },
      async edit(_instance, link) {
        linkEditor.onEdit({ action: "edit", link });
        return linkEditor.submit;
      },
    });
  }
}

/**
 * Renders the editor with a mocked LinkSuggestService feeding the link quick action's suggestions, a scripted link
 * editor and a link metadata handler reporting the inserted links.
 *
 * This is a dedicated story (rather than a `BlockNoteForTest` prop) because it builds its own container of mocked
 * services: test-prop functions get proxied back to Node when called from the mounted component, which strips any
 * methods off objects passed through them, and breaks the synchronous return value that the link metadata handler
 * relies on. Building them here, where the code actually runs in the browser,
 * keeps them intact.
 */
export const LinkSuggestionForTest: React.FC<LinkSuggestionForTestProps> = ({
  pages,
  attachments,
  linkSuggestServiceUnavailable,
  failingSearch,
  uploadedFileURL,
  uploadedFileName,
  linkEditorSubmit,
  noLinkEditor,
  content,
  syntax,
  upload,
}) => {
  // The links received by the link metadata handler before being inserted, and the link data the link editor was
  // opened with, rendered so that the tests can assert on them.
  const [insertedLinks, setInsertedLinks] = useState<LinkEditionData[]>([]);
  const [linkEditorInput, setLinkEditorInput] = useState<string>();

  const serviceContainer = useMemo(() => {
    const container = serviceContainerMock([syntax ?? FULL_SYNTAX]);
    bindLinkServices(container, {
      onInsert: (link) => setInsertedLinks((links) => [...links, link]),
      linkEditor: noLinkEditor
        ? undefined
        : {
            onEdit: (input) => setLinkEditorInput(JSON.stringify(input)),
            submit: linkEditorSubmit,
          },
    });

    const toLinks = (links: SuggestedLink[] | undefined, type: number) =>
      (links ?? []).map((link, index) => ({
        id: `link-${type}-${index}`,
        hint: link.hint ?? "",
        label: link.label,
        reference:
          link.reference ??
          (type === LINK_TYPE_PAGE
            ? `Some.Page${index}`
            : `Some.Page@file-${index}.pdf`),
        type,
        url: link.url,
      }));

    container.rebind("LinkSuggestServiceProvider").toConstantValue(
      linkSuggestServiceUnavailable
        ? { get: () => undefined }
        : ({
            get: () => ({
              async getLinks(_query, type) {
                const isPage = type === LINK_TYPE_PAGE;
                if (failingSearch === (isPage ? "pages" : "attachments")) {
                  throw new Error("Failed to reach the backend");
                }
                return isPage
                  ? toLinks(pages, LINK_TYPE_PAGE)
                  : toLinks(attachments, LINK_TYPE_ATTACHMENT);
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
        if (!uploadedFileURL) {
          throw new Error("Failed to upload the file");
        }
        return [
          {
            id: "uploaded file",
            name: uploadedFileName ?? "document.pdf",
            mimetype: "application/pdf",
            href: uploadedFileURL,
            date: new Date(),
            size: 12,
            author: undefined,
          },
        ];
      },
    });
    // Parses and serializes the page ("Space.Page") and attachment ("Space.Page@file.pdf") references like XWiki
    // does, in order to check that the links are saved with the reference of the linked page or attachment, relative to
    // the edited page.
    const parse = (reference: string, options?: { type?: EntityType }) => {
      const separator = reference.lastIndexOf("@");
      return options?.type === EntityType.ATTACHMENT
        ? new AttachmentReference(
            reference.substring(separator + 1),
            new DocumentReference(
              reference.substring(0, separator),
              new SpaceReference(),
            ),
          )
        : new DocumentReference(reference, new SpaceReference());
    };
    container.rebind("ModelReferenceParserProvider").toConstantValue({
      get: () => ({ parse, parseAsync: async (...args) => parse(...args) }),
    } satisfies ModelReferenceParserProvider);
    container.rebind("ModelReferenceSerializerProvider").toConstantValue({
      get: () => ({
        serialize(reference, base) {
          if (reference instanceof AttachmentReference) {
            // The page is omitted when it's the base page.
            return reference.document.name === base?.name
              ? reference.name
              : `${reference.document.name}@${reference.name}`;
          }
          return reference?.name;
        },
      }),
    } satisfies ModelReferenceSerializerProvider);

    return container;
  }, []);

  return (
    <>
      <BlockNoteViewWrapper
        options={{
          lang: "en",
          label: "Some Label",
          serviceContainer,
          syntax: (syntax ?? FULL_SYNTAX).id,
          upload,
        }}
        content={content ?? []}
      />
      <output data-testid="insertedLinks">
        {JSON.stringify(insertedLinks)}
      </output>
      <output data-testid="linkEditorInput">{linkEditorInput}</output>
    </>
  );
};
