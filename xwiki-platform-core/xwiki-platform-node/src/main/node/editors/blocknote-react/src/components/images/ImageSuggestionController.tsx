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
import { ImageSelector } from "./ImageSelector";
import { IMAGE_SUGGESTION_TRIGGER } from "./imageSuggestionMenu";
import { DepsContainerContext } from "../../contexts";
import { useEditor } from "../../hooks";
import { useImageUploader } from "../../misc/fileUpload";
import { insertOrUpdateBlockForSlashMenu } from "@blocknote/core";
import { PositionPopover, SuggestionMenuController } from "@blocknote/react";
import { Paper } from "@mantine/core";
import {
  debounceAsync,
  tryFalliblePromiseOrError,
} from "@xwiki/platform-fn-utils";
import { LinkType } from "@xwiki/platform-link-suggest-api";
import { useCallback, useContext, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { RiFolderImageLine, RiUpload2Line } from "react-icons/ri";
import type { EditorType } from "../../blocknote";
import type { DefaultReactSuggestionItem } from "@blocknote/react";
import type {
  LinkSuggestService,
  LinkSuggestServiceProvider,
} from "@xwiki/platform-link-suggest-api";

import "./ImageSuggestionController.css";

export type ImageSuggestionControllerProps = {
  maxSuggestions?: number;
};

export function ImageSuggestionController({
  maxSuggestions,
}: ImageSuggestionControllerProps) {
  const depsContainer = useContext(DepsContainerContext)!;

  const linkSuggestService = depsContainer.get<LinkSuggestServiceProvider>(
    "LinkSuggestServiceProvider",
  ).get!();

  const editor = useEditor();
  const uploadImage = useImageUploader(editor, depsContainer);

  const { t } = useTranslation();

  const [selectorPosition, setSelectorPosition] = useState<{
    from: number;
    to: number;
  }>();

  // Opens the full image selector popover where the image suggestion menu was (the menu itself
  // is already closed, and its query removed, by the time an item is clicked).
  const openImageSelector = useCallback(() => {
    const pos = editor.transact((tr) => tr.selection.from);
    setSelectorPosition({ from: pos, to: pos });
  }, [editor]);

  const searchImages = useMemo(
    () =>
      debounceAsync(async (query: string) => {
        const suggestions = await fetchImageSuggestions(
          query,
          linkSuggestService,
          editor,
          t,
        );

        const actions: DefaultReactSuggestionItem[] = [
          {
            title: t("blocknote.imageSuggestion.browse"),
            icon: <RiFolderImageLine size={18} />,
            onItemClick: openImageSelector,
          },
          {
            title: t("blocknote.imageSuggestion.upload"),
            icon: <RiUpload2Line size={18} />,
            onItemClick: uploadImage,
          },
        ];

        return [
          ...actions,
          ...(maxSuggestions
            ? suggestions.slice(0, maxSuggestions)
            : suggestions),
        ];
      }),
    [
      t,
      linkSuggestService,
      editor,
      maxSuggestions,
      openImageSelector,
      uploadImage,
    ],
  );

  return (
    <>
      <SuggestionMenuController
        triggerCharacter={IMAGE_SUGGESTION_TRIGGER}
        // A typed trigger shows the menu only once followed by a query, so that writing "image: "
        // in a sentence doesn't open it (and let Enter pick an action instead of breaking the line).
        minQueryLength={1}
        getItems={searchImages}
      />

      <PositionPopover
        position={selectorPosition}
        useFloatingOptions={{
          open: selectorPosition !== undefined,
          onOpenChange: (open) => {
            if (!open) {
              setSelectorPosition(undefined);
            }
          },
          placement: "bottom-start",
        }}
      >
        <Paper shadow="md" p="sm">
          <ImageSelector
            onSelected={(url) => {
              insertOrUpdateBlockForSlashMenu(editor, {
                type: "image",
                props: { url },
              });
              setSelectorPosition(undefined);
            }}
          />
        </Paper>
      </PositionPopover>
    </>
  );
}

async function fetchImageSuggestions(
  query: string,
  linkSuggestService: LinkSuggestService | undefined,
  editor: EditorType,
  t: ReturnType<typeof useTranslation>["t"],
): Promise<DefaultReactSuggestionItem[]> {
  if (!linkSuggestService) {
    // Only the browse and upload actions are offered: the image selector opened by the browse action
    // tells that search isn't supported, without the message being mistaken for a menu item.
    return [];
  }

  const results = await tryFalliblePromiseOrError(() =>
    linkSuggestService.getLinks(query, LinkType.ATTACHMENT, "image/*"),
  );

  if (results instanceof Error) {
    // Don't fail the whole menu: the browse and upload actions remain available.
    console.warn(
      `Failed to get the image suggestions for [${query}].`,
      results,
    );
    return [];
  }

  // The group's label separates the image suggestions from the browse and upload actions above.
  const group = t("blocknote.imageSuggestion.imagesGroup");

  return results.map(
    (link): DefaultReactSuggestionItem => ({
      title: link.label,
      // The location of the image (shown under its name, like in the image selector), to distinguish
      // images with the same name.
      subtext: link.hint || undefined,
      group,
      // The image is described by the title right next to it, hence the empty alternative text.
      icon: (
        <img
          className="xwiki-image-suggestion-thumbnail"
          src={link.url}
          alt=""
        />
      ),
      onItemClick() {
        insertOrUpdateBlockForSlashMenu(editor, {
          type: "image",
          props: { url: link.url },
        });
      },
    }),
  );
}
