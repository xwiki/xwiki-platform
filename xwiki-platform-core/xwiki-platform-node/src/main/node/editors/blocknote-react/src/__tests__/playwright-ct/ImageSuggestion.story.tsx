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
import { FULL_SYNTAX } from "./syntax.mock";
import { BlockNoteViewWrapper } from "../../components/BlockNoteViewWrapper";
import { useMemo } from "react";
// NOTE: `LinkType` is intentionally not imported here (not even type-only): the module also
// transitively exports classes using decorators, which the Playwright Node-side transform (unlike
// the Vite pipeline used to bundle this story) can't parse.
import type { LinkSuggestServiceProvider } from "@xwiki/platform-link-suggest-api";

const LINK_TYPE_ATTACHMENT = 1; // LinkType.ATTACHMENT

export type ImageSuggestionForTestProps = {
  images?: Array<{ label: string; url: string }>;
  // When set, the LinkSuggestService is unavailable, as with a backend that doesn't support search.
  linkSuggestServiceUnavailable?: boolean;
};

/**
 * Renders the editor with a mocked LinkSuggestService feeding the image quick action's suggestions.
 *
 * This is a dedicated story (rather than a `BlockNoteForTest` prop) because it builds its own
 * `vitest-mock-extended` container: test-prop functions get proxied back to Node when called from the
 * mounted component, which strips any mock methods off objects passed through them (see
 * `LinkEdition.story.tsx` for the same constraint). Building the container here, where the code
 * actually runs in the browser, keeps the mock intact.
 */
export const ImageSuggestionForTest: React.FC<ImageSuggestionForTestProps> = ({
  images,
  linkSuggestServiceUnavailable,
}) => {
  const depsContainer = useMemo(() => {
    const container = depsContainerMock();

    container.get.calledWith("LinkSuggestServiceProvider").mockReturnValue(
      linkSuggestServiceUnavailable
        ? { get: () => undefined }
        : ({
            get: () => ({
              async getLinks() {
                return (images ?? []).map((image, index) => ({
                  id: `image-${index}`,
                  hint: "",
                  label: image.label,
                  reference: "some attachment reference",
                  type: LINK_TYPE_ATTACHMENT,
                  url: image.url,
                }));
              },
            }),
          } satisfies LinkSuggestServiceProvider),
    );

    return container;
  }, []);

  return (
    <BlockNoteViewWrapper
      lang="en"
      label="Some Label"
      depsContainer={depsContainer}
      macros={false}
      content={[]}
      syntax={FULL_SYNTAX}
      linkEditionHandler={() => {
        throw new Error("Link editor should not open for this test");
      }}
    />
  );
};
