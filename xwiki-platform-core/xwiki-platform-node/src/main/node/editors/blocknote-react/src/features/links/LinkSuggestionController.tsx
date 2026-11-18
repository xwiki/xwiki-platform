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
import { useLinkEditing } from "./linkEditing";
import { LINK_SUGGESTION_TRIGGER } from "./linkSuggestionMenu";
import { EditorInstanceContext } from "../../contexts";
import { canUploadFiles } from "../../files/fileUploader";
import { pickAndUploadFile, uploadedFileLink } from "../../files/uploadedFile";
import { suggestedLinkData } from "../../misc/suggestedLink";
import { insertLink } from "../../services/linkEditor";
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
import type { SuggestedLink } from "../../misc/suggestedLink";
import type { LinkEditionData } from "../../services/linkEditor";
import type { DefaultReactSuggestionItem } from "@blocknote/react";
import type {
  Link,
  LinkSuggestService,
  LinkSuggestServiceProvider,
} from "@xwiki/platform-link-suggest-api";

/**
 * Inserts a link selected by the user.
 *
 * @param linkData - the link to insert
 */
type InsertLink = (linkData: LinkEditionData) => void;

export type LinkSuggestionControllerProps = {
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
 * uploading a file to link to and opening the link editor (when bound) for any other link.
 */
export function LinkSuggestionController({
  maxSuggestions,
  pageSuggestionsShare,
}: LinkSuggestionControllerProps) {
  const { serviceContainer } = useContext(EditorInstanceContext)!;

  const linkSuggestService = serviceContainer.get<LinkSuggestServiceProvider>(
    "LinkSuggestServiceProvider",
  ).get!();

  const { insertSuggested, openLinkEditor, uploadFile } = useLinkInsertion();

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
          // Offered only when there's a link editor.
          ...(openLinkEditor
            ? [
                {
                  title: t("blocknote.linkSuggestion.linkTo"),
                  icon: <RiLink size={18} />,
                  onItemClick: openLinkEditor,
                },
              ]
            : []),
          // Offered only when the user can upload files.
          ...(uploadFile
            ? [
                {
                  title: t("blocknote.linkSuggestion.upload"),
                  icon: <RiUpload2Line size={18} />,
                  onItemClick: uploadFile,
                },
              ]
            : []),
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
function useLinkInsertion() {
  const instance = useContext(EditorInstanceContext)!;
  const linkEditing = useLinkEditing();

  const insert: InsertLink = useCallback(
    (linkData) => insertLink(instance, linkData),
    [instance],
  );

  const openLinkEditor = useMemo(
    () =>
      linkEditing &&
      (async () => {
        await linkEditing.insert({}, insert);
        instance.editor.focus();
      }),
    [instance, linkEditing, insert],
  );

  const insertSuggested = useCallback(
    (link: SuggestedLink) => insert(suggestedLinkData(instance, link)),
    [instance, insert],
  );

  const uploadFile = useMemo(
    () =>
      canUploadFiles(instance) &&
      (async () => {
        const uploadedFile = await pickAndUploadFile(
          instance,
          undefined,
          "Failed to upload the selected file.",
        );
        if (uploadedFile) {
          insert(uploadedFileLink(instance, uploadedFile));
        }
      }),
    [instance, insert],
  );

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
