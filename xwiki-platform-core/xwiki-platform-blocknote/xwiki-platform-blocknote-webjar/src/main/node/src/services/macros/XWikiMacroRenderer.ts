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
import { BlockNoteDocument } from "../blocknote/BlockNoteProcessor";
import {
  MACRO_ID_PARAMETER,
  isMacroNode,
  visitNestedMacros,
} from "../blocknote/macroNodes";
import {
  getMacroCalls,
  setMacroOutput,
} from "@xwiki/platform-editors-blocknote-react";
import { toXWikiEntityReference } from "@xwiki/platform-model-xwiki";
import type { BlockNoteIterator } from "../blocknote/BlockNoteIterator";
import type { BlockNoteProcessor } from "../blocknote/BlockNoteProcessor";
import type {
  BlockMetadataStore,
  BlockType,
  EditorInstance,
  MacroRenderer,
} from "@xwiki/platform-editors-blocknote-react";

/**
 * What the server needs to know, besides the edited content, to render the macro calls like they are rendered when the
 * edited content is saved and viewed.
 *
 * @since 18.10.0RC1
 */
type XWikiMacroRendererOptions = {
  /**
   * Whether the edited content is rendered in a restricted context (e.g. a comment), where some macros are not
   * executed.
   */
  restricted: boolean;

  /**
   * The locale of the edited document translation.
   */
  locale?: string;

  /**
   * Identifies the realtime collaboration session where the content is edited, if any. The server renders the content
   * with the rights of the session participant that has the least script rights among those that changed the content,
   * so that the current user doesn't execute, with their own rights, the scripts inserted by the other participants.
   */
  collaboration?: string;
};

/**
 * Renders the macro calls of the edited content on the XWiki server: the edited content is sent, with the id of each
 * macro call as a macro parameter, and the server sends back the content with the output of each macro call.
 *
 * @since 18.10.0RC1
 */
export class XWikiMacroRenderer implements MacroRenderer {
  constructor(
    private readonly processor: BlockNoteProcessor,
    private readonly iterator: BlockNoteIterator,
    private readonly options: XWikiMacroRendererOptions,
  ) {}

  public async render({
    editor,
    metadata,
    options,
  }: EditorInstance): Promise<void> {
    const content = editor.document;
    // The macro calls as they are when the rendering starts: the output is kept for them, and ignored for the macro
    // calls that are modified (or added) in the meantime, since it doesn't match them.
    const macroCalls = new Map(
      getMacroCalls(content).map(({ id, call }) => [id, call]),
    );
    if (!macroCalls.size) {
      return;
    }

    const source = this.processor.save(
      new BlockNoteDocument(content, metadata),
      { macroIds: true },
    );
    const renderedContent = await this.fetchRenderedContent(
      source,
      options.syntax,
      options.sourceDocumentReference
        ? toXWikiEntityReference(options.sourceDocumentReference)
        : XWiki.currentDocument.documentReference,
    );
    this.keepMacroOutput(renderedContent, macroCalls, metadata);
  }

  private async fetchRenderedContent(
    source: string,
    syntax: string | undefined,
    sourceDocumentReference: ReturnType<typeof toXWikiEntityReference>,
  ): Promise<BlockType[]> {
    // The content is rendered for its source document, which is the context of the macro calls, like when it's viewed.
    const url = new XWiki.Document(sourceDocumentReference).getURL(
      "get",
      new URLSearchParams({
        xpage: "blocknote/render",
        language: this.options.locale ?? "",
      }),
    );
    const parameters = new URLSearchParams({
      content: source,
      restricted: String(this.options.restricted),
      form_token: document.documentElement.dataset.xwikiFormToken ?? "",
    });
    if (syntax) {
      parameters.set("syntax", syntax);
    }
    if (this.options.collaboration) {
      parameters.set("collaboration", this.options.collaboration);
    }
    const response = await fetch(url, { method: "POST", body: parameters });
    if (!response.ok) {
      throw new Error(
        `Failed to render the macro calls: ${response.status} ${response.statusText}`,
      );
    }
    return (await response.json()) as BlockType[];
  }

  /**
   * Keeps the output of the rendered macro calls, for the macro calls they were rendered for.
   *
   * @param renderedContent - the rendered content, where each macro call has its output and its id (as a parameter)
   * @param macroCalls - the macro calls that were rendered, by id
   * @param metadata - where to keep the output of the macro calls
   */
  private keepMacroOutput(
    renderedContent: BlockType[],
    macroCalls: Map<string, string>,
    metadata: BlockMetadataStore,
  ): void {
    const visit = (macroNode: Record<string, unknown>) => {
      const props = (macroNode.props ?? {}) as Record<string, unknown>;
      const id = this.removeMacroIdParameter(props.call);
      const call = id ? macroCalls.get(id) : undefined;
      if (id && call !== undefined) {
        const output = Array.isArray(props.output) ? props.output : [];
        setMacroOutput(metadata, id, call, output);
      }
      visitNestedMacros(this.iterator, props.call, visit);
    };
    this.iterator.iterate(renderedContent, {
      visit: (node) => {
        if (typeof node === "object" && isMacroNode(node)) {
          visit(node as Record<string, unknown>);
          return true;
        }
        return false;
      },
    });
  }

  /**
   * @param call - a rendered macro call
   * @returns the id of the given macro call, removed from its parameters, or undefined if it doesn't have one
   */
  private removeMacroIdParameter(call: unknown): string | undefined {
    const parameters = (call as { parameters?: Record<string, unknown> })
      ?.parameters;
    // The parameter names are case-insensitive.
    const name = Object.keys(parameters ?? {}).find(
      (key) => key.toLowerCase() === MACRO_ID_PARAMETER.toLowerCase(),
    );
    if (!parameters || !name) {
      return undefined;
    }
    const id = parameters[name];
    delete parameters[name];
    return typeof id === "string" ? id : undefined;
  }
}

export type { XWikiMacroRendererOptions };
