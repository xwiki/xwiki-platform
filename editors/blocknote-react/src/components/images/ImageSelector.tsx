/**
 * See the LICENSE file distributed with this work for additional
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
import { SearchBox } from "../SearchBox";
import {
  Box,
  Breadcrumbs,
  Button,
  FileInput,
  Flex,
  Space,
  Stack,
  Text,
  VisuallyHidden,
} from "@mantine/core";
import { tryFallible } from "@xwiki/cristal-fn-utils";
import { LinkType } from "@xwiki/cristal-link-suggest-api";
import {
  AttachmentReference,
  DocumentReference,
  EntityType,
} from "@xwiki/cristal-model-api";
import { useCallback, useEffect, useMemo, useRef } from "react";
import { useTranslation } from "react-i18next";
import { RiAttachmentLine } from "react-icons/ri";
import type {
  LinkEditionContext,
  LinkSuggestion,
} from "../../misc/linkSuggest";

export type ImageSelectorProps = {
  linkEditionCtx: LinkEditionContext;
  currentSelection?: string;
  onSelected: (url: string) => void;
};

export const ImageSelector: React.FC<ImageSelectorProps> = ({
  linkEditionCtx,
  currentSelection,
  onSelected,
}) => {
  const [initialQuery, selectedEntityPath] = useMemo(() => {
    if (!currentSelection) {
      return ["", null];
    }

    const entityRef = tryFallible(() =>
      linkEditionCtx.remoteURLParser.parse(currentSelection),
    );

    if (!entityRef || entityRef instanceof Error) {
      return [currentSelection, null];
    }

    const documentReference =
      entityRef?.type == EntityType.ATTACHMENT
        ? (entityRef as AttachmentReference).document
        : (entityRef as DocumentReference);

    const segments = documentReference.space?.names.slice(0) ?? [];

    return [
      "",
      segments.concat([
        linkEditionCtx.modelReferenceHandler.getTitle(entityRef)!,
      ]),
    ];
  }, [currentSelection]);

  const { t } = useTranslation();

  const fileUploadRef = useRef<HTMLButtonElement>(null);

  const triggerUpload = useCallback(() => {
    fileUploadRef?.current?.click();
  }, [fileUploadRef]);

  const fileSelected = useCallback(
    async (file: File) => {
      const currentPageName =
        linkEditionCtx.documentService.getCurrentDocumentReferenceString()
          .value ?? "";

      const uploadedFilesUrls = await linkEditionCtx.attachmentsService.upload(
        currentPageName,
        [file],
      );

      let url: string | undefined;
      if (uploadedFilesUrls && uploadedFilesUrls[0]) {
        url = uploadedFilesUrls[0];
      } else {
        const parser = linkEditionCtx.modelReferenceParser?.parse(
          currentPageName,
          { relative: false },
        );

        url = linkEditionCtx.remoteURLSerializer?.serialize(
          new AttachmentReference(file.name, parser as DocumentReference),
        );
      }

      if (url) {
        onSelected(url);
      }
    },
    [onSelected],
  );

  const searchAttachments = useCallback(
    async (query: string) => {
      const results = await linkEditionCtx.linkSuggestService.getLinks(
        query,
        LinkType.ATTACHMENT,
        "image/*",
      );

      return results.map((link): LinkSuggestion => {
        const attachmentReference = linkEditionCtx.modelReferenceParser?.parse(
          link.reference,
          { relative: false },
        ) as AttachmentReference;

        const documentReference = attachmentReference.document;
        const segments = documentReference.space?.names.slice(0) ?? [];

        if (documentReference.terminal) {
          segments.push(documentReference.name);
        }

        return {
          type: link.type,
          title: link.label,
          reference: link.reference,
          url: link.url,
          segments,
        };
      });
    },
    [linkEditionCtx],
  );

  // Start a first empty search on the first load, to not let the results empty.
  useEffect(() => {
    searchAttachments(initialQuery);
  }, []);

  return (
    <Box>
      <Button variant="default" onClick={triggerUpload}>
        {t("blocknote.imageSelector.uploadButton")}
      </Button>

      <Space h="sm" />

      <VisuallyHidden>
        <FileInput
          ref={fileUploadRef}
          accept="image/*"
          onChange={(file) => file && fileSelected(file)}
        />
      </VisuallyHidden>

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
              <Breadcrumbs c="gray">
                {suggestion.segments.map((segment, i) => (
                  <Text key={`${i}${segment}`}>{segment}</Text>
                ))}
              </Breadcrumbs>
            </Stack>
          </Flex>
        )}
        onSelect={onSelected}
        onSubmit={onSelected}
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
};
