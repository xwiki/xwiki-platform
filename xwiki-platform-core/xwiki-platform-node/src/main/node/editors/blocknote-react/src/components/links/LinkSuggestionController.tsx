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
import { insertLink } from "./insertLink";
import { LINK_SUGGESTION_TRIGGER } from "./linkSuggestionMenu";
import { DepsContainerContext, EditedDocumentContext } from "../../contexts";
import { useEditor } from "../../hooks";
import { triggerUserAttachmentUpload } from "../../misc/fileUpload";
import { toLinkResourceReference } from "../../misc/linkResourceReference";
import { SuggestionMenuController } from "@blocknote/react";
import {
  debounceAsync,
  tryFalliblePromiseOrError,
} from "@xwiki/platform-fn-utils";
import { LinkType } from "@xwiki/platform-link-suggest-api";
import { useCallback, useContext, useMemo } from "react";
import { useTranslation } from "react-i18next";
import {
  RiAttachment2,
  RiFileTextLine,
  RiLink,
  RiUpload2Line,
} from "react-icons/ri";
import type { EditorType } from "../../blocknote";
import type { LinkEditionData, LinkEditionHandler } from "./linkEdition";
import type { LinkEditionHooks } from "./linkEditionHooks";
import type { DefaultReactSuggestionItem } from "@blocknote/react";
import type {
  Link,
  LinkSuggestService,
  LinkSuggestServiceProvider,
} from "@xwiki/platform-link-suggest-api";
import type { Container } from "inversify";

/**
 * Inserts a link selected by the user.
 *
 * @param linkData - the link to insert
 */
type InsertLink = (linkData: LinkEditionData) => void;

/**
 * A suggested or uploaded page or attachment to link to.
 */
type SuggestedLink = Pick<Link, "label" | "url" | "type"> & {
  /** The absolute serialized reference of the page or attachment, when known. */
  reference?: string;
};

export type LinkSuggestionControllerProps = {
  /** Opens the link editor, for the links that the suggestions don't cover. */
  linkEditionHandler: LinkEditionHandler;
  /** Intercepts the inserted links. */
  linkEditionHooks?: LinkEditionHooks;
  /** The maximum number of suggestions shown, pages and attachments together. */
  maxSuggestions: number;
  /**
   * The number of pages shown when there are enough matching pages and attachments, the remaining slots going to the
   * attachments. Either kind fills the slots that the other kind can't.
   */
  pageSuggestionsShare: number;
};

/**
 * Suggests the pages and attachments to link to, as the user types after the link suggestion trigger, in addition to
 * uploading a file to link to and opening the link editor for any other link.
 */
export function LinkSuggestionController({
  linkEditionHandler,
  linkEditionHooks,
  maxSuggestions,
  pageSuggestionsShare,
}: LinkSuggestionControllerProps) {
  const depsContainer = useContext(DepsContainerContext)!;

  const linkSuggestService = depsContainer.get<LinkSuggestServiceProvider>(
    "LinkSuggestServiceProvider",
  ).get!();

  const editor = useEditor();
  const { insertSuggested, openLinkEditor, uploadFile } = useLinkInsertion(
    editor,
    depsContainer,
    linkEditionHandler,
    linkEditionHooks,
  );

  const { t } = useTranslation();

  const searchLinks = useMemo(
    () =>
      debounceAsync(async (query: string) => {
        // Pages and attachments are searched separately, so that each kind has its own share of the menu.
        const [pages, attachments] = shareSuggestions(
          await Promise.all([
            fetchLinks(query, LinkType.PAGE, linkSuggestService),
            fetchLinks(query, LinkType.ATTACHMENT, linkSuggestService),
          ]),
          maxSuggestions,
          pageSuggestionsShare,
        );

        const actions: DefaultReactSuggestionItem[] = [
          {
            title: t("blocknote.linkSuggestion.linkTo"),
            icon: <RiLink size={18} />,
            onItemClick: openLinkEditor,
          },
          {
            title: t("blocknote.linkSuggestion.upload"),
            icon: <RiUpload2Line size={18} />,
            onItemClick: uploadFile,
          },
        ];

        return [
          ...actions,
          ...toSuggestions(
            pages,
            t("blocknote.linkSuggestion.pagesGroup"),
            <RiFileTextLine size={18} />,
            insertSuggested,
          ),
          ...toSuggestions(
            attachments,
            t("blocknote.linkSuggestion.attachmentsGroup"),
            <RiAttachment2 size={18} />,
            insertSuggested,
          ),
        ];
      }),
    [
      t,
      linkSuggestService,
      insertSuggested,
      openLinkEditor,
      uploadFile,
      maxSuggestions,
      pageSuggestionsShare,
    ],
  );

  return (
    <SuggestionMenuController
      triggerCharacter={LINK_SUGGESTION_TRIGGER}
      // A typed trigger shows the menu only once followed by a query, so that typing "[[" doesn't open it (and let
      // Enter pick an action instead of breaking the line).
      minQueryLength={1}
      getItems={searchLinks}
    />
  );
}

/**
 * @returns the functions inserting a link selected by the user, with the link editor, from an uploaded file, or
 *   directly; the link is inserted at the editor's selection, where the link suggestion menu was (the menu itself is
 *   already closed, and its query removed, by the time an item is clicked)
 */
function useLinkInsertion(
  editor: EditorType,
  depsContainer: Container,
  linkEditionHandler: LinkEditionHandler,
  linkEditionHooks?: LinkEditionHooks,
) {
  const insert: InsertLink = useCallback(
    (linkData) => insertLink(editor, linkData, linkEditionHooks),
    [editor, linkEditionHooks],
  );

  const openLinkEditor = useCallback(
    () =>
      linkEditionHandler({
        mode: "createNew",
        current: { title: "", url: "" },
        onSubmit: insert,
      }),
    [linkEditionHandler, insert],
  );

  const editedDocumentReference = useContext(EditedDocumentContext);

  // The label of a link to a suggested or uploaded page or attachment is generated from its target (the title of the
  // page, the name of the attachment), which is the label of the suggestion.
  const insertSuggested = useCallback(
    (link: SuggestedLink) =>
      insert({
        title: link.label,
        generatedTitle: true,
        url: link.url,
        // Without a reference, the link is saved from its URL.
        reference:
          link.reference === undefined
            ? undefined
            : toLinkResourceReference(
                depsContainer,
                link.reference,
                link.type,
                editedDocumentReference,
              ),
      }),
    [depsContainer, editedDocumentReference, insert],
  );

  const uploadFile = useCallback(async () => {
    const attachment = await triggerUserAttachmentUpload(
      depsContainer,
      editedDocumentReference,
    );

    if (attachment) {
      insertSuggested({
        label: attachment.name,
        url: attachment.url,
        reference: attachment.reference?.reference,
        type: LinkType.ATTACHMENT,
      });
    }
  }, [depsContainer, editedDocumentReference, insertSuggested]);

  return { insertSuggested, openLinkEditor, uploadFile };
}

/**
 * @returns the links of the given type matching the given query, none when the backend doesn't support search or when
 *   the search fails (the actions of the menu remain available)
 */
async function fetchLinks(
  query: string,
  type: LinkType,
  linkSuggestService: LinkSuggestService | undefined,
): Promise<Link[]> {
  if (!linkSuggestService) {
    return [];
  }

  const results = await tryFalliblePromiseOrError(() =>
    linkSuggestService.getLinks(query, type),
  );

  if (results instanceof Error) {
    console.warn(
      `Failed to get the ${type === LinkType.PAGE ? "page" : "attachment"} suggestions for [${query}].`,
      results,
    );
    return [];
  }

  return results;
}

/**
 * Shares the menu between the pages and the attachments: each kind gets its share when there are enough matches of
 * both kinds, and the slots that one kind can't fill go to the other kind, so that no slot is left empty while there
 * are matches left (e.g. with 8 slots, 5 of them for pages: 6 pages and 2 attachments when only 2 attachments match,
 * 3 pages and 5 attachments when only 3 pages match).
 *
 * @param matches - the matching pages and attachments, the best matches first
 * @param maxSuggestions - the number of slots of the menu
 * @param pageSuggestionsShare - the number of slots for pages when there are enough matches of both kinds
 * @returns the pages and attachments to show
 */
function shareSuggestions(
  [pages, attachments]: [Link[], Link[]],
  maxSuggestions: number,
  pageSuggestionsShare: number,
): [Link[], Link[]] {
  const pageCount = Math.min(
    pages.length,
    Math.max(pageSuggestionsShare, maxSuggestions - attachments.length),
  );
  return [
    pages.slice(0, pageCount),
    attachments.slice(0, maxSuggestions - pageCount),
  ];
}

/**
 * @param links - the suggested links
 * @param group - the label of the group of the suggestions, separating them from the other kinds of suggestions
 * @param icon - the icon of the suggestions, telling their kind
 * @param insertSuggested - inserts the link to the selected suggestion
 * @returns the menu items inserting the given links
 */
function toSuggestions(
  links: Link[],
  group: string,
  icon: React.JSX.Element,
  insertSuggested: (link: Link) => void,
): DefaultReactSuggestionItem[] {
  return links.map((link) => ({
    title: link.label,
    // The location of the linked page or attachment, to distinguish the links with the same label.
    subtext: link.hint || undefined,
    group,
    icon,
    onItemClick: () => insertSuggested(link),
  }));
}
