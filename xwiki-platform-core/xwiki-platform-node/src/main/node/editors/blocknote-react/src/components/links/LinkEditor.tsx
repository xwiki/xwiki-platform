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
import { DepsContainerContext } from "../../contexts";
import { createLinkSuggestor } from "../../misc/linkSuggest";
import { SearchBox } from "../SearchBox";
import {
  Breadcrumbs,
  Button,
  Input,
  Stack,
  Text,
  useCombobox,
} from "@mantine/core";
import { tryFallible } from "@xwiki/platform-fn-utils";
import { LinkType } from "@xwiki/platform-link-suggest-api";
import { useCallback, useContext, useState } from "react";
import { useTranslation } from "react-i18next";
import { RiFileLine, RiText } from "react-icons/ri";
import type { ModelReferenceSerializerProvider } from "@xwiki/platform-model-reference-api";
import type { RemoteURLParserProvider } from "@xwiki/platform-model-remote-url-api";
import type { ResourceReference } from "@xwiki/platform-rendering-api";
import type { Container } from "inversify";

/**
 * The data describing a link being created or edited.
 *
 * @since 18.6.0RC1
 * @beta
 */
type LinkData = {
  title: string;
  url: string;
  /**
   * The XWiki resource reference the link points to, built from the user's selection (a raw URL, a
   * suggestion or a typed entity reference). It rides alongside the URL so the integration can persist
   * it without having to parse the URL back into a reference.
   */
  reference?: ResourceReference;
};

type LinkEditorProps = {
  current: LinkData | null;
  updateLink: (linkData: LinkData) => void;
  creationMode?: boolean;
};

export const LinkEditor: React.FC<LinkEditorProps> = ({
  current,
  updateLink,
  creationMode,
}) => {
  const { t } = useTranslation();
  const depsContainer = useContext(DepsContainerContext)!;
  const linkSuggestor = createLinkSuggestor(depsContainer);

  const [title, setTitle] = useState(current?.title ?? "");
  const [url, setUrl] = useState(current?.url ?? "");
  // Initialized from the incoming data so a title-only edit preserves the reference injected by the
  // beforeEdit hook; updated whenever the user picks a new target in the search box.
  const [reference, setReference] = useState(current?.reference);

  const submit = useCallback(
    (overrides?: {
      title?: string;
      url?: string;
      reference?: ResourceReference;
    }) => {
      updateLink({
        title: overrides?.title ?? title,
        url: overrides?.url ?? url,
        reference: overrides?.reference ?? reference,
      });
    },
    [updateLink, title, url, reference],
  );

  const suggestLinks = useCallback(
    async (query: string) => {
      if (linkSuggestor === null) {
        return false;
      }

      const suggestions = await linkSuggestor({ query });

      return suggestions.filter(
        (suggestion) => suggestion.type === LinkType.PAGE,
      );
    },
    [linkSuggestor],
  );

  const combobox = useCombobox({
    onDropdownClose: () => combobox.resetSelectedOption(),
  });

  return (
    <Stack>
      {!creationMode && (
        <Input
          leftSection={<RiText />}
          data-test="linkTitle"
          value={title}
          onChange={(e) => setTitle(e.currentTarget.value)}
          onKeyDown={(e) => {
            if (current && e.key === "Enter") {
              // Prevent the default editing action of the Enter key: the submit handler moves the
              // focus back to the editor synchronously, in which case the browser would apply the
              // default action to the editor's restored selection, deleting its content.
              e.preventDefault();
              submit({ title: e.currentTarget.value });
            }
          }}
        />
      )}

      <SearchBox
        placeholder={t("blocknote.linkEditor.placeholder")}
        initialValue={
          current?.url ? getSerializedReference(current.url, depsContainer) : ""
        }
        getSuggestions={suggestLinks}
        renderSuggestion={(link) => (
          <Stack justify="center">
            <Text>
              <RiFileLine /> {link.title}
              <Breadcrumbs c="gray">
                {link.segments.map((segment, i) => (
                  <Text key={`${i}${segment}`} fz="md">
                    {segment}
                  </Text>
                ))}
              </Breadcrumbs>
            </Text>
          </Stack>
        )}
        onSelect={(url, reference) => {
          if (creationMode) {
            submit({ url, reference });
          } else {
            setUrl(url);
            setReference(reference);
          }
        }}
        onSubmit={(url, reference) => submit({ url, reference })}
      />

      {!creationMode && (
        <Button fullWidth type="submit" onClick={() => submit()}>
          {t("blocknote.linkEditor.submit")}
        </Button>
      )}
    </Stack>
  );
};

function getSerializedReference(url: string, depsContainer: Container): string {
  const remoteURLParser = depsContainer
    .get<RemoteURLParserProvider>("RemoteURLParserProvider")
    .get()!;

  const modelReferenceSerializer = depsContainer
    .get<ModelReferenceSerializerProvider>("ModelReferenceSerializerProvider")
    .get()!;

  const reference = tryFallible(() => remoteURLParser.parse(url));
  return reference ? modelReferenceSerializer.serialize(reference)! : url;
}

export type { LinkData, LinkEditorProps };
