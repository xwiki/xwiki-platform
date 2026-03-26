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
import { SearchBox } from "../components/SearchBox";
import { MantineProvider } from "@mantine/core";

export type SearchBoxForTestProps = {
  onSelect?: (url: string) => void;
  onSubmit?: (url: string) => void;
};

export const SearchBoxForTest: React.FC<SearchBoxForTestProps> = ({
  onSelect,
  onSubmit,
}) => {
  return (
    <MantineProvider>
      <SearchBox
        initialValue="Some great initial value"
        placeholder="Some super placeholder"
        linkEditionCtx={null}
        getSuggestions={async (query) => [
          {
            type: 1,
            reference: "",
            segments: [],
            title: "Some great suggestion title starting with " + query,
            url: "https://picsum.photos/150",
          },
          {
            type: 1,
            reference: "",
            segments: [],
            title: "Another great suggestion title starting with " + query,
            url: "https://picsum.photos/300",
          },
        ]}
        renderSuggestion={(suggestion) => (
          <span>Suggestion title: {suggestion.title}</span>
        )}
        onSelect={(url) => onSelect?.(url)}
        onSubmit={(url) => onSubmit?.(url)}
      />
    </MantineProvider>
  );
};
