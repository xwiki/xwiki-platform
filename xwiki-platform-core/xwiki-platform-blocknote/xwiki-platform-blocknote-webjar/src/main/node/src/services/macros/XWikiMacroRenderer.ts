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
import { MACRO_ID_PARAMETER, isMacroNode } from "../blocknote/macroNodes";
import { BLOCKNOTE_SYNTAX } from "../blocknote/syntax";
import {
  BlockNoteDocument,
  blockNoteIteratorName,
  blockNoteProcessorName,
  getMacroCalls,
  macroRendererName,
} from "@xwiki/platform-editors-blocknote-react";
import { toXWikiEntityReference } from "@xwiki/platform-model-xwiki";
import { inject, injectable, named } from "inversify";
import type {
  BlockNoteIterator,
  BlockNoteProcessor,
  BlockType,
  EditorInstance,
  MacroFeatureOptions,
  MacroRenderer,
} from "@xwiki/platform-editors-blocknote-react";
import type { XWikiMeta } from "@xwiki/platform-xwiki-utils";
import type { Container } from "inversify";

type MacroOutputs = NonNullable<MacroFeatureOptions["outputs"]>;

/**
 * Renders the macro calls of the edited content on the XWiki server: the edited content is sent, with the id of each
 * macro call as a macro parameter, and the server sends back the content with the output of each macro call. The
 * content is rendered like when it's viewed: for its source document, in its syntax, restricted or not (see
 * `EditorOptions.restricted`). When the content is edited in a realtime collaboration session, the server renders it
 * with the rights of the session participant that has the least script rights among those that changed the content,
 * so that the current user doesn't execute, with their own rights, the scripts inserted by the other participants.
 *
 * @since 18.10.0
 */
@injectable("Singleton")
export class XWikiMacroRenderer implements MacroRenderer {
  public static bind(container: Container): void {
    container
      .bind<MacroRenderer>(macroRendererName)
      .to(XWikiMacroRenderer)
      .inSingletonScope();
  }

  constructor(
    @inject(blockNoteProcessorName)
    @named("XWiki")
    private readonly processor: BlockNoteProcessor,
    @inject(blockNoteIteratorName) private readonly iterator: BlockNoteIterator,
    @inject("XWikiMeta") private readonly xwikiMeta: XWikiMeta,
  ) {}

  public async render({
    editor,
    options,
  }: EditorInstance): Promise<MacroOutputs> {
    const content = editor.document;
    // The macro calls as they are when the rendering starts: the output is associated with them, so that it is ignored
    // for the macro calls that are modified (or added) in the meantime.
    const macroCalls = new Map(
      getMacroCalls(content, this.iterator).map(({ id, call }) => [id, call]),
    );
    if (!macroCalls.size) {
      return {};
    }

    const source = this.processor.save(new BlockNoteDocument(content), {
      macroIds: true,
    });
    const renderedContent = await this.fetchRenderedContent(source, options);
    return this.getMacroOutputs(renderedContent, macroCalls);
  }

  private async fetchRenderedContent(
    source: string,
    options: EditorInstance["options"],
  ): Promise<BlockType[]> {
    const sourceDocumentReference = options.sourceDocumentReference;
    // The content is rendered for its source document, which is the context of the macro calls, like when it's viewed.
    const url = new XWiki.Document(
      sourceDocumentReference
        ? toXWikiEntityReference(sourceDocumentReference)
        : XWiki.currentDocument.documentReference,
    ).getURL(
      "get",
      new URLSearchParams({
        xpage: "wysiwyg/refresh",
        language: sourceDocumentReference?.locale ?? "",
      }),
    );
    const parameters = new URLSearchParams({
      content: source,
      editorSyntax: BLOCKNOTE_SYNTAX,
      sourceSyntax: options.syntax,
      restricted: String(options.restricted === true),
      form_token: this.xwikiMeta.form_token,
    });
    if (options.collaboration?.id) {
      // Tells the server to render the content with the rights of the collaboration session participants.
      parameters.set("collaboration", options.collaboration.id);
    }
    const response = await fetch(url, { method: "POST", body: parameters });
    if (!response.ok) {
      throw new Error(
        `Failed to render the macro calls: ${response.status} ${response.statusText}`,
      );
    }
    return JSON.parse(await response.text()) as BlockType[];
  }

  /**
   * @param renderedContent - the rendered content, where each macro call has its output and its id (as a parameter)
   * @param macroCalls - the macro calls that were rendered, by id
   * @returns the output of the rendered macro calls, by id, for the macro calls they were rendered for
   */
  private getMacroOutputs(
    renderedContent: BlockType[],
    macroCalls: Map<string, string>,
  ): MacroOutputs {
    const outputs: MacroOutputs = {};
    this.iterator.iterate(renderedContent, {
      visit: (node) => {
        if (typeof node === "object" && isMacroNode(node)) {
          const props = ((node as { props?: unknown }).props ?? {}) as Record<
            string,
            unknown
          >;
          const id = this.removeMacroIdParameter(props.call);
          const call = id ? macroCalls.get(id) : undefined;
          if (id && call !== undefined) {
            const output = Array.isArray(props.output) ? props.output : [];
            outputs[id] = { call, output };
          }
        }
        // Enter the macro calls, to get the output of the nested macro calls.
        return false;
      },
    });
    return outputs;
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
