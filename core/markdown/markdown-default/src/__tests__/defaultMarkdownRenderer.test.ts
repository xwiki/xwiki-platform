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
import { DefaultMarkdownRenderer } from "../defaultMarkdownRenderer";
import {
  AttachmentReference,
  DocumentReference,
  SpaceReference,
} from "@xwiki/cristal-model-api";
import { describe, expect, it } from "vitest";
import { mock } from "vitest-mock-extended";
import type {
  ModelReferenceParser,
  ModelReferenceParserProvider,
} from "@xwiki/cristal-model-reference-api";
import type {
  RemoteURLSerializer,
  RemoteURLSerializerProvider,
} from "@xwiki/cristal-model-remote-url-api";
import type { MockProxy } from "vitest-mock-extended";

function addReferenceMocks(
  modelReferenceParser: MockProxy<ModelReferenceParser> & ModelReferenceParser,
  remoteURLSerializer: MockProxy<RemoteURLSerializer> & RemoteURLSerializer,
) {
  const documentReference = new DocumentReference(
    "WebHome",
    new SpaceReference(undefined, "Main"),
  );
  const attachmentReference = new AttachmentReference(
    "image.png",
    documentReference,
  );

  modelReferenceParser.parse
    .calledWith("Main.WebHome")
    .mockReturnValue(documentReference);
  remoteURLSerializer.serialize
    .calledWith(documentReference)
    .mockReturnValue("https://cristal.xwiki.org");

  modelReferenceParser.parse
    .calledWith("Main.WebHome@image.png")
    .mockReturnValue(attachmentReference);
  remoteURLSerializer.serialize
    .calledWith(attachmentReference)
    .mockReturnValue("https://cristal.xwiki.org/image.png");
}

function initComponent() {
  const modelReferenceParserProvider = mock<ModelReferenceParserProvider>();
  const modelReferenceParser = mock<ModelReferenceParser>();
  modelReferenceParserProvider.get.mockReturnValue(modelReferenceParser);
  const remoteURLSerializerProvider = mock<RemoteURLSerializerProvider>();
  const remoteURLSerializer = mock<RemoteURLSerializer>();
  remoteURLSerializerProvider.get.mockReturnValue(remoteURLSerializer);

  addReferenceMocks(modelReferenceParser, remoteURLSerializer);

  return new DefaultMarkdownRenderer(
    modelReferenceParserProvider,
    remoteURLSerializerProvider,
  );
}

// eslint-disable-next-line max-statements
describe("DefaultMarkdownRenderer", () => {
  it("renders content without markup unchanged", () => {
    const defaultMarkdownRenderer = initComponent();
    expect(defaultMarkdownRenderer.render("abcd")).toBe("<p>abcd</p>\n");
  });

  it("renders bold content", () => {
    const defaultMarkdownRenderer = initComponent();
    expect(defaultMarkdownRenderer.render("a**b**cd")).toBe(
      "<p>a<strong>b</strong>cd</p>\n",
    );
  });

  it("renders external links", () => {
    const defaultMarkdownRenderer = initComponent();
    expect(
      defaultMarkdownRenderer.render("[Cristal](https://cristal.xwiki.org/)"),
    ).toBe('<p><a href="https://cristal.xwiki.org/">Cristal</a></p>\n');
  });

  it("renders internal links", () => {
    const defaultMarkdownRenderer = initComponent();
    expect(defaultMarkdownRenderer.render("[[Cristal|Main.WebHome]]")).toBe(
      '<p><a href="https://cristal.xwiki.org" class="internal-link">Cristal</a></p>\n',
    );
  });

  it("renders internal links in a sentence", () => {
    const defaultMarkdownRenderer = initComponent();
    expect(
      defaultMarkdownRenderer.render("Hello [[Cristal|Main.WebHome]] World"),
    ).toBe(
      '<p>Hello <a href="https://cristal.xwiki.org" class="internal-link">Cristal</a> World</p>\n',
    );
  });

  it("renders internal links in a sentence, in a paragraph", () => {
    const defaultMarkdownRenderer = initComponent();
    expect(
      defaultMarkdownRenderer.render(
        `Line 1

Hello [[Cristal|Main.WebHome]] World

Line 3`,
      ),
    ).toBe(
      `<p>Line 1</p>
<p>Hello <a href="https://cristal.xwiki.org" class="internal-link">Cristal</a> World</p>
<p>Line 3</p>
`,
    );
  });

  it("renders internal links no text", () => {
    const defaultMarkdownRenderer = initComponent();
    expect(defaultMarkdownRenderer.render("[[Main.WebHome]]")).toBe(
      '<p><a href="https://cristal.xwiki.org" class="internal-link">Main.WebHome</a></p>\n',
    );
  });

  it("renders external images", () => {
    const defaultMarkdownRenderer = initComponent();
    expect(
      defaultMarkdownRenderer.render(
        "![Cristal](https://cristal.xwiki.org/image.png)",
      ),
    ).toBe(
      '<p><img src="https://cristal.xwiki.org/image.png" alt="Cristal"></p>\n',
    );
  });

  it("renders internal image", () => {
    const defaultMarkdownRenderer = initComponent();
    expect(
      defaultMarkdownRenderer.render("![[Cristal|Main.WebHome@image.png]]"),
    ).toBe(
      '<p><img src="https://cristal.xwiki.org/image.png" alt="Cristal"></p>\n',
    );
  });

  it("renders internal image no text", () => {
    const defaultMarkdownRenderer = initComponent();
    expect(defaultMarkdownRenderer.render("![[Main.WebHome@image.png]]")).toBe(
      '<p><img src="https://cristal.xwiki.org/image.png" alt=""></p>\n',
    );
  });

  it("renders bolded links", () => {
    const defaultMarkdownRenderer = initComponent();
    expect(
      defaultMarkdownRenderer.render(
        "**before [[Main.WebHome@image.png]] after**",
      ),
    ).toBe(
      "<p><strong>before " +
        '<a href="https://cristal.xwiki.org/image.png" class="internal-link">Main.WebHome@image.png</a> ' +
        "after</strong></p>\n",
    );
  });

  it("renders bolded images", () => {
    const defaultMarkdownRenderer = initComponent();
    expect(
      defaultMarkdownRenderer.render(
        "**before ![[Main.WebHome@image.png]] after**",
      ),
    ).toBe(
      '<p><strong>before <img src="https://cristal.xwiki.org/image.png" alt=""> after</strong></p>\n',
    );
  });

  it("renders link with italic", () => {
    const defaultMarkdownRenderer = initComponent();
    expect(defaultMarkdownRenderer.render(`[[*italic*|Main.WebHome]]`)).toBe(
      '<p><a href="https://cristal.xwiki.org" class="internal-link"><em>italic</em></a></p>\n',
    );
  });

  it("renders link with bold outside link", () => {
    const defaultMarkdownRenderer = initComponent();
    expect(
      defaultMarkdownRenderer.render(`[[*italic*|Main.WebHome]] **bold**`),
    ).toBe(
      '<p><a href="https://cristal.xwiki.org" class="internal-link"><em>italic</em></a> <strong>bold</strong></p>\n',
    );
  });

  it("renders link with double closing brackets inside link", () => {
    const defaultMarkdownRenderer = initComponent();
    expect(
      defaultMarkdownRenderer.render(`[[some content \`]]\`|Main.WebHome]]`),
    ).toBe(
      '<p><a href="https://cristal.xwiki.org" class="internal-link">some content <code>]]</code></a></p>\n',
    );
  });
});
