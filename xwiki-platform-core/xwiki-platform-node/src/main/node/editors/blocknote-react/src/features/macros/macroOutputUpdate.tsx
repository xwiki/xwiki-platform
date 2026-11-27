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
import { EditorInstanceContext } from "../../contexts";
import { blockNoteIteratorName } from "../../services/blockNoteIterator";
import { macroRendererName } from "../../services/macroRenderer";
import { useContext, useEffect } from "react";
import type { BlockType } from "../../blocknote";
import type { EditorInstance } from "../../editorInstance";
import type {
  BlockNoteIterator,
  NodeType,
} from "../../services/blockNoteIterator";
import type { MacroRenderer } from "../../services/macroRenderer";
import type { MacroOutputCache } from "./macroOutputCache";

/**
 * A macro call of the edited content that is rendered server-side. Its output is kept (see
 * `MacroFeatureOptions.outputs`) under its {@link MacroCallEntry.id}, together with its {@link MacroCallEntry.call}.
 *
 * @since 18.10.0
 * @beta
 */
type MacroCallEntry = {
  /**
   * Identifies the macro call: the block id for a block macro, the `id` property for an inline macro or for a macro
   * nested in the parameters or content of another macro call.
   */
  id: string;

  /**
   * The macro call, serialized as JSON. The output kept for the macro call is displayed only as long as the macro call
   * is the same as the one the output was rendered for.
   */
  call: string;
};

const MACRO_TYPES = new Set(["xwikiMacroBlock", "xwikiInlineMacro"]);

/**
 * Collects the server-rendered macro calls of the given content, in document order. The macro calls nested in the
 * parameters or content of other macro calls (the parameters and the content that are editable in-place are BlockNote
 * fragments that can hold macro calls) are collected only if the given iterator enters the macro calls. The macro
 * calls without an id are skipped because their output can't be kept.
 *
 * @param content - the edited content (e.g. the editor document)
 * @param iterator - used to iterate the given content
 * @returns the server-rendered macro calls of the given content
 *
 * @since 18.10.0
 * @beta
 */
function getMacroCalls(
  content: BlockType[],
  iterator: BlockNoteIterator,
): MacroCallEntry[] {
  const macroCalls: MacroCallEntry[] = [];
  iterator.iterate(content, {
    visit: (node, macro) => {
      const macroCall = getMacroCall(node, macro !== undefined);
      if (macroCall) {
        macroCalls.push(macroCall);
      }
      // Enter the macro calls (if the iterator supports it), to collect the nested macro calls.
      return false;
    },
  });
  return macroCalls;
}

/**
 * @param node - a node of the edited content
 * @param nested - whether the given node is in the parameters or content of a macro call
 * @returns the macro call of the given node, or undefined if the node isn't a macro node with an id
 */
function getMacroCall(
  node: NodeType,
  nested: boolean,
): MacroCallEntry | undefined {
  if (typeof node !== "object" || !MACRO_TYPES.has(node.type as string)) {
    return undefined;
  }
  const {
    type,
    id: blockId,
    props,
  } = node as {
    type: string;
    id?: unknown;
    props?: { id?: unknown; call?: unknown };
  };
  // The call of a top-level macro is a JSON string (block and inline content properties hold only primitive values)
  // while the call of a nested macro is an object, part of the call of its parent.
  const call = nested ? JSON.stringify(props?.call) : props?.call;
  // BlockNote preserves the block id, but inline content and nested macros don't have one, so they use a property.
  const id = !nested && type === "xwikiMacroBlock" ? blockId : props?.id;
  return typeof id === "string" && id && isMacroCall(call)
    ? { id, call }
    : undefined;
}

/**
 * @param call - the JSON serialization of a macro call, or something else
 * @returns whether the given value is the JSON serialization of a macro call (object)
 */
function isMacroCall(call: unknown): call is string {
  try {
    const value = typeof call === "string" ? JSON.parse(call) : undefined;
    return value !== null && typeof value === "object";
  } catch {
    return false;
  }
}

/**
 * Keeps the output of the macro calls of an editor instance up to date: asks the {@link MacroRenderer} to render the
 * edited content when the output of some macro calls is missing or out of date, whoever modified them (the current
 * user or another user editing the same content in realtime). A macro call whose rendering didn't produce an output for
 * it (e.g. because the rendering failed) isn't rendered again until it's modified.
 */
class MacroOutputUpdater {
  /**
   * The macro calls whose rendering didn't produce an output for them, since the editor instance was created, each
   * serialized as `JSON.stringify([id, call])`. They are not rendered again, to avoid rendering the edited content in
   * a loop. A macro call is removed from here once it gets an output, so that it is rendered again when it comes back
   * after being modified (e.g. on undo), its output being replaced in the meantime by the output of the modified call.
   */
  private readonly failed = new Set<string>();

  /**
   * Whether the edited content is being rendered. Only one rendering runs at a time.
   */
  private rendering = false;

  /**
   * Whether the edited content has to be rendered again once the current rendering ends, because it changed in the
   * meantime (the current rendering is for the content as it was when it started).
   */
  private renderAgain = false;

  /**
   * The pending update, scheduled to run once the edited content stops changing.
   */
  private timeout?: ReturnType<typeof setTimeout>;

  /**
   * Whether the editor instance was destroyed, in which case the macro calls are not rendered anymore.
   */
  private destroyed = false;

  /**
   * @param instance - the editor instance whose macro calls are kept up to date
   * @param renderer - renders the macro calls of the edited content
   * @param iterator - used to find the macro calls of the edited content
   * @param outputs - where the output of the macro calls of the editor instance is kept
   * @param delay - how long to wait for the edited content to stop changing before rendering it, in milliseconds
   */
  constructor(
    private readonly instance: EditorInstance,
    private readonly renderer: MacroRenderer,
    private readonly iterator: BlockNoteIterator,
    private readonly outputs: MacroOutputCache,
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

  /**
   * Renders the edited content and keeps the output of its macro calls.
   *
   * @param outdatedMacroCalls - the macro calls (see {@link MacroOutputUpdater.failed}) whose output is missing or out
   *   of date, which this rendering is expected to produce
   */
  private async render(outdatedMacroCalls: string[]): Promise<void> {
    this.rendering = true;
    // Considered failed until the rendering produces their output.
    outdatedMacroCalls.forEach((macroCall) => this.failed.add(macroCall));
    try {
      const outputs = await this.renderer.render(this.instance);
      Object.entries(outputs).forEach(([id, { call, output }]) => {
        this.failed.delete(JSON.stringify([id, call]));
        this.outputs.set(id, call, output);
      });
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
   * @returns the macro calls (see {@link MacroOutputUpdater.failed}) whose output is missing or out of date, and whose
   *   rendering didn't fail already
   */
  private getOutdatedMacroCalls(): string[] {
    return (
      getMacroCalls(this.instance.editor.document, this.iterator)
        // Filter macro calls whose output is missing or out of date.
        .filter(({ id, call }) => this.outputs.get(id, call) === undefined)
        .map(({ id, call }) => JSON.stringify([id, call]))
        // Filter macro calls whose rendering failed already.
        .filter((macroCall) => !this.failed.has(macroCall))
    );
  }
}

/**
 * Keeps the output of the macro calls of the editor instance up to date, when a {@link MacroRenderer} is bound in its
 * service container: the content is rendered when the output of some macro calls is missing or out of date, e.g. when
 * a macro call is inserted or modified, or when joining a realtime collaboration session whose content isn't saved.
 */
function MacroOutputUpdate({
  outputs,
}: {
  /**
   * Where the output of the macro calls of the editor instance is kept.
   */
  readonly outputs: MacroOutputCache;
}): null {
  const instance = useContext(EditorInstanceContext);
  useEffect(() => {
    if (!instance?.serviceContainer.isBound(macroRendererName)) {
      return undefined;
    }
    const updater = new MacroOutputUpdater(
      instance,
      instance.serviceContainer.get<MacroRenderer>(macroRendererName),
      instance.serviceContainer.get<BlockNoteIterator>(blockNoteIteratorName),
      outputs,
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
  }, [instance, outputs]);
  return null;
}

export { MacroOutputUpdate, MacroOutputUpdater, getMacroCalls };
export type { MacroCallEntry };
