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
import { getMacroOutput } from "./macroMetadata";
import { EditorInstanceContext } from "../../contexts";
import { macroRendererName } from "../../services/macroRenderer";
import { useContext, useEffect } from "react";
import type { EditorInstance } from "../../editorInstance";
import type { MacroRenderer } from "../../services/macroRenderer";
import type { ReactNode } from "react";

/**
 * A server-rendered macro call of the edited content, identified as its output is kept in the metadata of the editor
 * instance (see `setMacroOutput`).
 *
 * @since 18.10.0RC1
 * @beta
 */
type MacroCallEntry = {
  /**
   * Identifies the macro call: the block id for a block macro, the `id` property for an inline macro or for a macro
   * nested in the parameters or content of another macro call.
   */
  id: string;

  /**
   * The macro call (JSON), as its output is associated with it.
   */
  call: string;
};

const MACRO_TYPES = new Set(["xwikiMacroBlock", "xwikiInlineMacro"]);

/**
 * Collects the server-rendered macro calls of the given content, in document order: the block and inline macro calls,
 * and the macro calls nested in their parameters or content (the parameters and the content that are editable
 * in-place are BlockNote fragments that can hold macro calls). The macro calls without an id are skipped because their
 * output can't be kept.
 *
 * @param content - the edited content (e.g. the editor document), or a fragment of it
 * @returns the server-rendered macro calls of the given content
 *
 * @since 18.10.0RC1
 * @beta
 */
function getMacroCalls(content: unknown): MacroCallEntry[] {
  const macroCalls: MacroCallEntry[] = [];
  collectMacroCalls(content, false, macroCalls);
  return macroCalls;
}

function collectMacroCalls(
  node: unknown,
  nested: boolean,
  macroCalls: MacroCallEntry[],
): void {
  if (Array.isArray(node)) {
    node.forEach((child) => collectMacroCalls(child, nested, macroCalls));
  } else if (node !== null && typeof node === "object") {
    const { type, id, props, ...rest } = node as {
      type?: unknown;
      id?: unknown;
      props?: Record<string, unknown>;
    };
    if (typeof type === "string" && MACRO_TYPES.has(type) && props) {
      collectMacroCall(type, id, props, nested, macroCalls);
    }
    // The props of the other blocks and inline content hold only primitive values. Visit the content and the children
    // (a macro block can have child blocks), whatever their shape (e.g. the rows and cells of a table).
    Object.values(rest).forEach((child) =>
      collectMacroCalls(child, nested, macroCalls),
    );
  }
}

function collectMacroCall(
  type: string,
  blockId: unknown,
  props: Record<string, unknown>,
  nested: boolean,
  macroCalls: MacroCallEntry[],
): void {
  // The call of a top-level macro is a JSON string (block and inline content properties hold only primitive values)
  // while the call of a nested macro is an object, part of the call of its parent.
  const call = nested ? props.call : parseMacroCall(props.call);
  const callJSON = nested ? JSON.stringify(call) : props.call;
  // BlockNote preserves the block id, but inline content and nested macros don't have one, so they use a property.
  const id = !nested && type === "xwikiMacroBlock" ? blockId : props.id;
  if (call !== null && typeof call === "object") {
    if (typeof id === "string" && id && typeof callJSON === "string") {
      macroCalls.push({ id, call: callJSON });
    }
    const { parameters, content } = call as {
      parameters?: Record<string, unknown>;
      content?: unknown;
    };
    collectMacroCalls(
      [...Object.values(parameters ?? {}), content],
      true,
      macroCalls,
    );
  }
}

/**
 * @param call - the call of a top-level macro (JSON)
 * @returns the macro call object, or undefined if the given value isn't a macro call
 */
function parseMacroCall(call: unknown): unknown {
  try {
    return typeof call === "string" ? JSON.parse(call) : undefined;
  } catch {
    return undefined;
  }
}

/**
 * Keeps the output of the macro calls of an editor instance up to date: asks the {@link MacroRenderer} to render the
 * edited content when the output of some macro calls is missing or out of date, whoever modified them (the current
 * user or another user editing the same content in realtime). A macro call that was rendered without producing an
 * output for it (e.g. because the rendering failed) isn't rendered again until it's modified.
 */
class MacroOutputUpdater {
  // The macro calls (id and call) that were already rendered, whether their output was kept or not.
  private readonly rendered = new Set<string>();

  private rendering = false;

  private renderAgain = false;

  private timeout?: ReturnType<typeof setTimeout>;

  private destroyed = false;

  /**
   * @param instance - the editor instance whose macro calls are kept up to date
   * @param renderer - renders the macro calls of the edited content
   * @param delay - how long to wait for the edited content to stop changing before rendering it, in milliseconds
   */
  constructor(
    private readonly instance: EditorInstance,
    private readonly renderer: MacroRenderer,
    private readonly delay = 300,
  ) {}

  /**
   * Checks, once the edited content stops changing, whether the macro calls need to be rendered.
   */
  public scheduleUpdate(): void {
    clearTimeout(this.timeout);
    this.timeout = setTimeout(() => this.update(), this.delay);
  }

  /**
   * Renders the edited content if the output of some macro calls is missing or out of date. When the edited content is
   * already being rendered, it's rendered again afterwards, because the current rendering is for the content as it was
   * when it started.
   */
  public update(): void {
    clearTimeout(this.timeout);
    if (this.destroyed) {
      return;
    }
    const outdatedMacroCalls = this.getOutdatedMacroCalls();
    if (!outdatedMacroCalls.length) {
      return;
    }
    if (this.rendering) {
      this.renderAgain = true;
    } else {
      void this.render(outdatedMacroCalls);
    }
  }

  /**
   * Stops updating the output of the macro calls.
   */
  public destroy(): void {
    this.destroyed = true;
    clearTimeout(this.timeout);
  }

  private async render(outdatedMacroCalls: string[]): Promise<void> {
    this.rendering = true;
    outdatedMacroCalls.forEach((macroCall) => this.rendered.add(macroCall));
    try {
      await this.renderer.render(this.instance);
    } catch (error) {
      console.error("Failed to render the macro calls.", error);
    } finally {
      this.rendering = false;
    }
    if (this.renderAgain) {
      this.renderAgain = false;
      this.update();
    }
  }

  /**
   * @returns the macro calls (id and call) whose output is missing or out of date, and that weren't rendered already
   */
  private getOutdatedMacroCalls(): string[] {
    return getMacroCalls(this.instance.editor.document)
      .filter(
        ({ id, call }) =>
          getMacroOutput(this.instance.metadata, id, call) === undefined,
      )
      .map(({ id, call }) => JSON.stringify([id, call]))
      .filter((macroCall) => !this.rendered.has(macroCall));
  }
}

/**
 * Keeps the output of the macro calls of the editor instance up to date, when a {@link MacroRenderer} is bound in its
 * service container: the content is rendered when the output of some macro calls is missing or out of date, e.g. when
 * a macro call is inserted or modified, or when joining a realtime collaboration session whose content isn't saved.
 */
function MacroOutputUpdate(): ReactNode {
  const instance = useContext(EditorInstanceContext);
  useEffect(() => {
    if (!instance?.serviceContainer.isBound(macroRendererName)) {
      return undefined;
    }
    const updater = new MacroOutputUpdater(
      instance,
      instance.serviceContainer.get<MacroRenderer>(macroRendererName),
    );
    // The changes made by the other users editing the same content in realtime are reported as well.
    const unsubscribe = instance.editor.onChange(() =>
      updater.scheduleUpdate(),
    );
    updater.scheduleUpdate();
    return () => {
      unsubscribe();
      updater.destroy();
    };
  }, [instance]);
  return null;
}

export { MacroOutputUpdate, MacroOutputUpdater, getMacroCalls };
export type { MacroCallEntry };
