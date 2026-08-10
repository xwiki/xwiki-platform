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
import { createReactBlockSpec } from "@blocknote/react";

function XWikiRawContent({
  block,
}: {
  readonly block: {
    readonly props: { readonly syntax: string; readonly text: string };
  };
}) {
  const { syntax, text } = block.props;
  if (syntax.startsWith("html/")) {
    return (
      <div className="xwiki-raw" dangerouslySetInnerHTML={{ __html: text }} />
    );
  }
  return <pre className="xwiki-raw">{text}</pre>;
}

/**
 * BlockNote block spec for a raw content fragment emitted by the server renderer.
 * Carries a `syntax` id (e.g. `"html/5.0"`) and a `text` value. HTML syntaxes are
 * injected verbatim; other syntaxes are rendered as plain text in a `<pre>` element.
 * Not editable and not exposed in the slash menu; it appears only inside macro output.
 *
 * @since 18.6.0RC1
 * @beta
 */
const XWikiRawBlock = createReactBlockSpec(
  {
    type: "xwikiRaw",
    propSchema: {
      syntax: { default: "" },
      text: { default: "" },
    },
    content: "none",
  },
  {
    render: XWikiRawContent,
    toExternalHTML: XWikiRawContent,
  },
);

export { XWikiRawBlock };
