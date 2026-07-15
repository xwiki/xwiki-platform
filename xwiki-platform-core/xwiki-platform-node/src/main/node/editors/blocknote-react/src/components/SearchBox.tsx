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
import { DepsContainerContext } from "../contexts";
import { Combobox, InputBase, Paper, useCombobox } from "@mantine/core";
import { LinkType } from "@xwiki/platform-link-suggest-api";
import { ResourceType } from "@xwiki/platform-rendering-api";
import { t } from "i18next";
import { debounce } from "lodash-es";
import { useCallback, useContext, useEffect, useState } from "react";
import { RiLink } from "react-icons/ri";
import type { LinkSuggestion } from "../misc/linkSuggest";
import type { ModelReferenceParserProvider } from "@xwiki/platform-model-reference-api";
import type { RemoteURLSerializerProvider } from "@xwiki/platform-model-remote-url-api";
import type {
  ResourceReference,
  ResourceReferenceParser,
} from "@xwiki/platform-rendering-api";
import type { ReactElement } from "react";

/**
 * Map a link suggestion type to the XWiki resource type used to build its resource reference.
 */
function linkTypeToResourceType(type: LinkType): string {
  return type === LinkType.ATTACHMENT
    ? ResourceType.ATTACHMENT
    : ResourceType.DOCUMENT;
}

export type SearchBoxProps = {
  /**
   * The search box's initial value
   */
  initialValue?: string;

  /**
   * The search box's placeholder (when empty)
   */
  placeholder: string;

  /**
   * Perform a search
   *
   * @param query - A user query (text)
   *
   * @returns Suggestions matching the provided query
   */
  getSuggestions: (query: string) => Promise<LinkSuggestion[] | false>;

  /**
   * Render a single suggestion
   *
   * @param suggestion -
   *
   * @returns A JSX element
   */
  renderSuggestion: (suggestion: LinkSuggestion) => ReactElement;

  /**
   * Triggered when a result is selected in the list of suggestions
   *
   * @param url - the URL of the selected result
   * @param reference - the XWiki resource reference of the selected result
   */
  onSelect: (url: string, reference: ResourceReference) => void;

  /**
   * Triggered when a result is submitted by the user
   *
   * e.g. when the user uses the `<Enter>` key on an URL
   *
   * @param url - the submitted URL
   * @param reference - the XWiki resource reference matching the submitted value
   */
  onSubmit: (url: string, reference: ResourceReference) => void;
};

/**
 * This component provides a search (text) field with a dropdown for the results
 *
 * Raw URLs input is supported, as well as raw entity references.
 *
 * @see SearchBoxProps
 */
// eslint-disable-next-line max-statements
export const SearchBox: React.FC<SearchBoxProps> = ({
  initialValue,
  placeholder,
  getSuggestions,
  renderSuggestion,
  onSelect,
  onSubmit,
}) => {
  const depsContainer = useContext(DepsContainerContext);

  if (!depsContainer) {
    throw new Error("Missing dependencies container in React context");
  }

  const modelReferenceParser = depsContainer
    .get<ModelReferenceParserProvider>("ModelReferenceParserProvider")
    .get()!;

  const remoteURLSerializer = depsContainer
    .get<RemoteURLSerializerProvider>("RemoteURLSerializerProvider")
    .get()!;

  const resourceReferenceParser = depsContainer.get<ResourceReferenceParser>(
    "ResourceReferenceParser",
  );

  const combobox = useCombobox({
    onDropdownClose: () => combobox.resetSelectedOption(),
  });

  const [query, setQuery] = useState(initialValue ?? "");
  const [suggestions, setSuggestions] = useState<
    | { status: "loading" }
    | {
        status: "resolved";
        suggestions: LinkSuggestion[];
      }
    | {
        status: "backendSearchUnsupported";
      }
  >({ status: "resolved", suggestions: [] });

  const isUrl = (value: string) =>
    value.startsWith("http://") || value.startsWith("https://");

  const performSearch = useCallback(
    debounce((search: string) => {
      if (isUrl(search)) {
        setSuggestions({ status: "resolved", suggestions: [] });
        return;
      }

      setSuggestions({ status: "loading" });

      // eslint-disable-next-line promise/catch-or-return
      getSuggestions(search).then((suggestions) => {
        setSuggestions(
          // eslint-disable-next-line promise/always-return
          suggestions !== false
            ? { status: "resolved", suggestions }
            : { status: "backendSearchUnsupported" },
        );
      });
    }),
    [setSuggestions, getSuggestions],
  );

  const submitRawValue = useCallback(
    // eslint-disable-next-line max-statements
    async (value: string) => {
      if (isUrl(value)) {
        onSubmit(value, {
          type: ResourceType.URL,
          typed: false,
          reference: value,
          parameters: {},
        });
        return;
      }

      if (!modelReferenceParser || !remoteURLSerializer) {
        return;
      }

      const reference = await modelReferenceParser
        .parseAsync(value)
        .catch(() => null);

      if (!reference) {
        return;
      }

      const url = remoteURLSerializer.serialize(reference);

      if (url === undefined) {
        throw new Error("Failed to serialize entity reference: " + value);
      }

      // Build the resource reference directly from the typed entity reference, rather than parsing it
      // back from the URL we just serialized.
      onSubmit(
        url,
        resourceReferenceParser.parse(value, { type: ResourceType.DOCUMENT }),
      );
    },
    [
      onSubmit,
      modelReferenceParser,
      remoteURLSerializer,
      resourceReferenceParser,
    ],
  );

  // Automatically perform a search when the query changes
  useEffect(() => performSearch(query), [query, performSearch]);

  // Perform a search at the opening
  useEffect(() => performSearch(""), []);

  return (
    <Combobox
      store={combobox}
      // We don't use a portal as BlockNote's toolbar closes on interaction with an element that isn't part of it in the DOM
      withinPortal={false}
      onOptionSubmit={(url) => {
        if (
          suggestions.status === "loading" ||
          suggestions.status === "backendSearchUnsupported"
        ) {
          return;
        }

        const result = suggestions.suggestions.find((s) => s.url === url);

        if (!result) {
          return;
        }

        combobox.closeDropdown();
        setQuery(result.title);
        // Build the resource reference from the suggestion's entity reference (with the right default
        // type), rather than parsing it back from the URL.
        onSelect(
          url,
          resourceReferenceParser.parse(result.reference, {
            type: linkTypeToResourceType(result.type),
          }),
        );
      }}
    >
      <Combobox.Target>
        <InputBase
          leftSection={<RiLink />}
          rightSection=" "
          data-test="searchBoxInput"
          placeholder={placeholder}
          value={query}
          onChange={(event) => {
            combobox.openDropdown();
            combobox.updateSelectedOptionIndex();
            setQuery(event.currentTarget.value);
          }}
          onClick={() => combobox.openDropdown()}
          onFocus={() => combobox.openDropdown()}
          onBlur={() => {
            combobox.closeDropdown();
          }}
          onKeyDown={(e) => {
            if (e.key === "Enter") {
              // Prevent the default editing action of the Enter key: the submit handlers can move
              // the focus back to the editor synchronously, in which case the browser would apply
              // the default action to the editor's restored selection, deleting its content.
              e.preventDefault();
              submitRawValue(e.currentTarget.value);
            }
          }}
        />
      </Combobox.Target>

      {
        // Don't display results for URLs
        !isUrl(query) &&
          // Don't display results if the query is empty and we have no results
          (query.length > 0 ||
            suggestions.status === "loading" ||
            (suggestions.status === "resolved" &&
              suggestions.suggestions.length > 0)) && (
            <Combobox.Dropdown
              style={{
                zIndex: 10000,
              }}
            >
              <Paper shadow="md" p="sm">
                <Combobox.Options>
                  {suggestions.status === "loading" ? (
                    <Combobox.Empty>
                      {t("blocknote.combobox.loadingSuggestions")}
                    </Combobox.Empty>
                  ) : suggestions.status === "backendSearchUnsupported" ? (
                    <Combobox.Empty>
                      {t("blocknote.combobox.backendSearchUnsupported")}
                    </Combobox.Empty>
                  ) : suggestions.suggestions.length > 0 ? (
                    suggestions.suggestions.map((suggestion) => (
                      <Combobox.Option
                        value={suggestion.url}
                        key={suggestion.url}
                      >
                        {renderSuggestion(suggestion)}
                      </Combobox.Option>
                    ))
                  ) : (
                    <Combobox.Empty>
                      {t("blocknote.combobox.noResultFound")}
                    </Combobox.Empty>
                  )}
                </Combobox.Options>
              </Paper>
            </Combobox.Dropdown>
          )
      }
    </Combobox>
  );
};
