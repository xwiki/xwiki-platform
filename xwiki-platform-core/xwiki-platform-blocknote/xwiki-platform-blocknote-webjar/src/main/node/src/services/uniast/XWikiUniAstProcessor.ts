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
import { UniAstProcessor } from "./UniAstProcessor";
import { MacroInvocation, UniAst } from "@xwiki/platform-uniast-api";
import { Container, inject, injectable } from "inversify";
import type { UniAstIterator, UniAstNode } from "./UniAstIterator";

/**
 * XWik specific implementation of a UniAstProcessor.
 *
 * @beta
 */
@injectable("Singleton")
export class XWikiUniAstProcessor implements UniAstProcessor {
  public static bind(container: Container): void {
    container
      .bind("UniAstProcessor")
      .to(XWikiUniAstProcessor)
      .inSingletonScope()
      .whenNamed("XWiki");
  }

  constructor(
    @inject("UniAstIterator") private readonly iterator: UniAstIterator,
  ) {}

  public load(uniAstJSON: string): UniAst {
    const uniAst = uniAstJSON ? JSON.parse(uniAstJSON) : { blocks: [] };
    this.iterator.iterate(uniAst, {
      visit: (node) => this.loadNode(node),
    });
    return uniAst;
  }

  private loadNode(node: UniAstNode): boolean {
    if (node.type === "macroBlock") {
      this.loadMacro(node, "xwikiMacroBlock");
    } else if (node.type === "inlineMacro") {
      this.loadMacro(node, "xwikiInlineMacro");
    }
    return false;
  }

  private loadMacro(
    macroNode: {
      type: "macroBlock" | "inlineMacro";
      call: MacroInvocation;
      output?: unknown;
    },
    id: string,
  ): void {
    const output = JSON.stringify(macroNode.output);
    const call = JSON.stringify(macroNode.call);
    delete macroNode.output;
    macroNode.call = {
      id,
      params: {
        call,
        output,
      },
      body: { type: "none" },
    };
  }

  public save(uniAst: UniAst): string {
    this.iterator.iterate(uniAst, {
      visit: (node) => this.saveNode(node),
    });
    return JSON.stringify(uniAst);
  }

  private saveNode(node: UniAstNode): boolean {
    if (node.type === "macroBlock" || node.type === "inlineMacro") {
      this.saveMacro(node);
    }
    return false;
  }

  private saveMacro(macroNode: {
    type: "macroBlock" | "inlineMacro";
    call: MacroInvocation;
    output?: unknown;
  }): void {
    if (
      macroNode.call.id === "xwikiMacroBlock" ||
      macroNode.call.id === "xwikiInlineMacro"
    ) {
      const call = JSON.parse(macroNode.call.params.call as string);
      let output = macroNode.call.params.output;
      if (typeof output === "string") {
        output = JSON.parse(output as string);
      }
      macroNode.call = call;
      macroNode.output = output;
    }
  }
}
