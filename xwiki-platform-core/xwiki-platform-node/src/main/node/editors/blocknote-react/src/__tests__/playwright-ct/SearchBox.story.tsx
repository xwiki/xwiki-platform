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
import { depsContainerMock } from "./depsContainer.mock";
import { SearchBox } from "../../components/SearchBox";
import { DepsContainerContext } from "../../contexts";
import { MantineProvider } from "@mantine/core";
import { useCallback, useMemo, useState } from "react";

export type SearchBoxForTestProps = {
  onSelect?: (url: string) => void;
  onSubmit?: (url: string) => void;
};

export const SearchBoxForTest: React.FC<SearchBoxForTestProps> = ({
  onSelect,
  onSubmit,
}) => {
  const depsContainer = useMemo(depsContainerMock, []);

  // Every query searched so far, rendered for the tests to check which searches were performed.
  const [searchedQueries, setSearchedQueries] = useState<string[]>([]);

  // Stable, like the suggestion providers of the actual consumers, so that only query changes
  // trigger a search.
  const getSuggestions = useCallback(async (query: string) => {
    setSearchedQueries((queries) => [...queries, query]);

    return [
      {
        type: 1,
        reference: "",
        hint: "",
        title: "Some great suggestion title starting with " + query,
        url: "https://picsum.photos/150",
      },
      {
        type: 1,
        reference: "",
        hint: "",
        title: "Another great suggestion title starting with " + query,
        url: "https://picsum.photos/300",
      },
    ];
  }, []);

  return (
    <MantineProvider>
      <DepsContainerContext value={depsContainer}>
        <SearchBox
          initialValue="Some great initial value"
          placeholder="Some super placeholder"
          getSuggestions={getSuggestions}
          renderSuggestion={(suggestion) => (
            <span>Suggestion title: {suggestion.title}</span>
          )}
          onSelect={(url) => onSelect?.(url)}
          onSubmit={(url) => onSubmit?.(url)}
        />
        <output data-test="searchedQueries">
          {JSON.stringify(searchedQueries)}
        </output>
      </DepsContainerContext>
    </MantineProvider>
  );
};
