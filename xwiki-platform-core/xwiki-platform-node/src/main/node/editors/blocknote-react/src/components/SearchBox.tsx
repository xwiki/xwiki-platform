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
import { Combobox, InputBase, Paper, useCombobox } from "@mantine/core";
import { t } from "i18next";
import { debounce } from "lodash-es";
import { useCallback, useEffect, useState } from "react";
import { RiLink } from "react-icons/ri";
import type { LinkEditionContext, LinkSuggestion } from "../misc/linkSuggest";
import type { KeyboardEvent, ReactElement } from "react";

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
   * Link edition context, required for validate raw entity references on submit
   *
   * @since 0.22
   */
  linkEditionCtx: LinkEditionContext | null;

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
   */
  onSelect: (url: string) => void;

  /**
   * Triggered when a result is submitted by the user
   *
   * e.g. when the user uses the `<Enter>` key on an URL
   */
  onSubmit: (url: string) => void;
};

/**
 * This component provides a search (text) field with a dropdown for the results
 *
 * Raw URLs input is supported, as well as raw entity references.
 *
 * @see SearchBoxProps
 */
export const SearchBox: React.FC<SearchBoxProps> = ({
  initialValue,
  placeholder,
  linkEditionCtx,
  getSuggestions,
  renderSuggestion,
  onSelect,
  onSubmit,
}) => {
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
    async (e: KeyboardEvent<HTMLInputElement>, value: string) => {
      if (isUrl(value)) {
        onSubmit(value);
        return;
      }

      if (!linkEditionCtx) {
        e.preventDefault();
        return;
      }

      const reference = await linkEditionCtx.modelReferenceParser
        .parseAsync(value)
        .catch(() => null);

      if (!reference) {
        e.preventDefault();
        return;
      }

      const url = linkEditionCtx.remoteURLSerializer.serialize(reference);

      if (url === undefined) {
        e.preventDefault();
        throw new Error("Failed to serialize entity reference: " + value);
      }

      onSubmit(url);
    },
    [onSubmit, linkEditionCtx],
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
        onSelect(url);
      }}
    >
      <Combobox.Target>
        <InputBase
          leftSection={<RiLink />}
          rightSection=" "
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
          onKeyDown={(e) =>
            e.key === "Enter" && submitRawValue(e, e.currentTarget.value)
          }
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
