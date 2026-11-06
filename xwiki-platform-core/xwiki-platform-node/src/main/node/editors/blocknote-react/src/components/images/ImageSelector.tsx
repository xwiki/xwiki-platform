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
import { DepsContainerContext, EditedDocumentContext } from "../../contexts";
import { triggerUserImageUpload } from "../../misc/fileUpload";
import { SearchBox } from "../SearchBox";
import {
  Box,
  Breadcrumbs,
  Button,
  Flex,
  Space,
  Stack,
  Text,
} from "@mantine/core";
import { tryFallible } from "@xwiki/platform-fn-utils";
import { LinkType } from "@xwiki/platform-link-suggest-api";
import {
  AttachmentReference,
  DocumentReference,
  EntityType,
} from "@xwiki/platform-model-api";
import { useCallback, useContext, useMemo } from "react";
import { useTranslation } from "react-i18next";
import { RiAttachmentLine } from "react-icons/ri";
import type { LinkSuggestion } from "../SearchBox";
import type { LinkSuggestServiceProvider } from "@xwiki/platform-link-suggest-api";
import type { ModelReferenceHandlerProvider } from "@xwiki/platform-model-reference-api";
import type { RemoteURLParserProvider } from "@xwiki/platform-model-remote-url-api";
import type { ResourceReference } from "@xwiki/platform-rendering-api";

export type ImageSelectorProps = {
  currentSelection?: string;
  /**
   * Called when the user selects an image.
   *
   * @param url - the URL of the selected image
   * @param reference - the resource reference of the selected image, when known (e.g. for a suggested or an uploaded
   *   image)
   */
  onSelected: (url: string, reference?: ResourceReference) => void;
};

export function ImageSelector({
  currentSelection,
  onSelected,
}: ImageSelectorProps) {
  const depsContainer = useContext(DepsContainerContext)!;

  const remoteURLParser = depsContainer
    .get<RemoteURLParserProvider>("RemoteURLParserProvider")
    .get()!;

  const modelReferenceHandler = depsContainer
    .get<ModelReferenceHandlerProvider>("ModelReferenceHandlerProvider")
    .get()!;

  const linkSuggestService = depsContainer.get<LinkSuggestServiceProvider>(
    "LinkSuggestServiceProvider",
  ).get!();

  const [initialQuery, selectedEntityPath] = useMemo(() => {
    if (!currentSelection) {
      return ["", null];
    }

    const entityRef = tryFallible(() =>
      remoteURLParser.parse(currentSelection),
    );

    if (!entityRef || entityRef instanceof Error) {
      return [currentSelection, null];
    }

    const documentReference =
      entityRef?.type == EntityType.ATTACHMENT
        ? (entityRef as AttachmentReference).document
        : (entityRef as DocumentReference);

    const segments = documentReference.space?.names.slice(0) ?? [];

    return ["", segments.concat([modelReferenceHandler.getTitle(entityRef)!])];
  }, [currentSelection]);

  const { t } = useTranslation();

  const searchAttachments = useCallback(
    async (query: string) => {
      if (!linkSuggestService) {
        return false;
      }

      const results = await linkSuggestService.getLinks(
        query,
        LinkType.ATTACHMENT,
        "image/*",
      );

      return results.map(
        (link): LinkSuggestion => ({
          type: link.type,
          title: link.label,
          reference: link.reference,
          url: link.url,
          hint: link.hint,
        }),
      );
    },
    [linkSuggestService],
  );

  const editedDocumentReference = useContext(EditedDocumentContext);

  const uploadImage = useCallback(async () => {
    const image = await triggerUserImageUpload(
      depsContainer,
      editedDocumentReference,
    );

    if (image) {
      onSelected(image.url, image.reference);
    }
  }, [depsContainer, editedDocumentReference, onSelected]);

  return (
    <Box>
      <Button variant="default" onClick={uploadImage}>
        {t("blocknote.imageSelector.uploadButton")}
      </Button>

      <Space h="sm" />

      <SearchBox
        placeholder={t("blocknote.imageSelector.placeholder")}
        initialValue={initialQuery}
        getSuggestions={searchAttachments}
        renderSuggestion={(suggestion) => (
          <Flex gap="sm">
            <img
              src={suggestion.url}
              style={{
                maxWidth: "100px",
                maxHeight: "100px",
                objectFit: "contain",
              }}
            />
            <Stack justify="center">
              <Text>
                <RiAttachmentLine /> {suggestion.title}
              </Text>
              {suggestion.hint && <Text c="gray">{suggestion.hint}</Text>}
            </Stack>
          </Flex>
        )}
        onSelect={onSelected}
        // The search box builds a page reference from an entered entity reference, so only keep the URL of an entered
        // value.
        onSubmit={(url) => onSelected(url)}
      />

      {selectedEntityPath && (
        <Breadcrumbs c="gray" pt="md" separatorMargin="0.33rem">
          {selectedEntityPath.map((segment, i) => (
            <Text fz="0.9rem" key={`${i}${segment}`}>
              {segment}
            </Text>
          ))}
        </Breadcrumbs>
      )}
    </Box>
  );
}
