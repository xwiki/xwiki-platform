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
  IMAGE_SUGGESTION_UPLOAD_BTN_TITLE_PLACEHOLDER,
  ImageSuggestionMenu,
} from "./ImageSuggestionMenu";
import { DepsContainerContext } from "../../contexts";
import { useEditor } from "../../hooks";
import { useImageUploader } from "../../misc/fileUpload";
import { insertOrUpdateBlockForSlashMenu } from "@blocknote/core";
import { SuggestionMenuController } from "@blocknote/react";
import { LinkType } from "@xwiki/platform-link-suggest-api";
import { useCallback, useContext } from "react";
import { useTranslation } from "react-i18next";
import type { EditorType } from "../../blocknote";
import type { DefaultReactSuggestionItem } from "@blocknote/react";
import type {
  LinkSuggestService,
  LinkSuggestServiceProvider,
} from "@xwiki/platform-link-suggest-api";
import type { AttachmentReference } from "@xwiki/platform-model-api";
import type {
  ModelReferenceParser,
  ModelReferenceParserProvider,
} from "@xwiki/platform-model-reference-api";

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

  const modelReferenceParser = depsContainer
    .get<ModelReferenceParserProvider>("ModelReferenceParserProvider")
    .get()!;

  const editor = useEditor();
  const uploadImage = useImageUploader(editor, depsContainer);

  const { t } = useTranslation();

  const searchImages = useCallback(
    async (query: string) => {
      const suggestions = await fetchImageSuggestions(
        query,
        linkSuggestService,
        modelReferenceParser,
        editor,
        t,
      );

      // HACK: placeholder for the upload button (see the `ImageSuggestionMenu` component for more info.)
      suggestions.unshift({
        title: IMAGE_SUGGESTION_UPLOAD_BTN_TITLE_PLACEHOLDER,
        onItemClick: uploadImage,
      });

      return maxSuggestions
        ? suggestions.slice(0, maxSuggestions + 1) // account for the placeholder item
        : suggestions;
    },
    [t, linkSuggestService, modelReferenceParser, maxSuggestions],
  );

  return (
    <SuggestionMenuController
      triggerCharacter={":"} /* TODO: "img::" */
      getItems={searchImages}
      suggestionMenuComponent={ImageSuggestionMenu}
    />
  );
}

async function fetchImageSuggestions(
  query: string,
  linkSuggestService: LinkSuggestService | undefined,
  modelReferenceParser: ModelReferenceParser,
  editor: EditorType,
  t: ReturnType<typeof useTranslation>["t"],
): Promise<DefaultReactSuggestionItem[]> {
  if (!linkSuggestService) {
    return [
      {
        title: t("blocknote.combobox.backendSearchUnsupported"),
        onItemClick() {},
      },
    ];
  }

  const results = await linkSuggestService.getLinks(
    query,
    LinkType.ATTACHMENT,
    "image/*",
  );

  return results.map((link): DefaultReactSuggestionItem => {
    const attachmentReference = modelReferenceParser?.parse(link.reference, {
      relative: false,
    }) as AttachmentReference;

    const documentReference = attachmentReference.document;
    const segments = documentReference.space?.names.slice(0) ?? [];

    if (documentReference.terminal) {
      segments.push(documentReference.name);
    }

    return {
      title: link.label,
      subtext: link.url,
      onItemClick() {
        insertOrUpdateBlockForSlashMenu(editor, {
          type: "image",
          props: { url: link.url },
        });
      },
    };
  });
}
