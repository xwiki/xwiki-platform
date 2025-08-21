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
import { LinkType } from "@xwiki/cristal-link-suggest-api";
import {
  AttachmentReference,
  DocumentReference,
} from "@xwiki/cristal-model-api";
import { useCallback, useEffect, useRef } from "react";
import { useTranslation } from "react-i18next";
import { RiAttachmentLine } from "react-icons/ri";
import type {
  LinkEditionContext,
  LinkSuggestion,
} from "../../misc/linkSuggest";

export type ImageSelectorProps = {
  linkEditionCtx: LinkEditionContext;
  onSelected: (url: string) => void;
};

export const ImageSelector: React.FC<ImageSelectorProps> = ({
  linkEditionCtx,
  onSelected,
}) => {
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
        const parser =
          linkEditionCtx.modelReferenceParser?.parse(currentPageName);

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

      const suggestions = results.map((link): LinkSuggestion => {
        const attachmentReference = linkEditionCtx.modelReferenceParser?.parse(
          link.reference,
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

      return suggestions;
    },
    [linkEditionCtx],
  );

  // Start a first empty search on the first load, to not let the results empty.
  useEffect(() => {
    searchAttachments("");
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
    </Box>
  );
};
