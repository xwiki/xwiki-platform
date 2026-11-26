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
import { serviceContainerMock } from "./serviceContainer.mock";
import { BlockNoteViewWrapper } from "../../components/BlockNoteViewWrapper";
import { macroEditorName } from "../../services/macroEditor";
import { useMemo, useState } from "react";
import type { BlockType } from "../../blocknote";
import type { EditorOptions } from "../../editorOptions";
import type {
  InlineMacroInvocation,
  MacroBlockInvocation,
  MacroEditor,
} from "../../services/macroEditor";
import type { SyntaxConfig } from "@xwiki/platform-syntaxes-config";
import type { Container } from "inversify";

export type BlockNoteForTestProps = {
  content: BlockType[];
  // The options that the tests need to set, the others being the same for all the tests. The configuration of the
  // syntax of the edited content is registered in the container, the editor taking only its id.
  options: Omit<
    EditorOptions,
    "lang" | "label" | "serviceContainer" | "syntax"
  > & {
    syntax: SyntaxConfig;
  };
  // Binds the services that the test needs in the service container. Only for the stories wrapping this one, since a
  // function passed from the test runs outside of the browser.
  bind?: (container: Container) => void;
  // Binds a macro editor returning the given macro call (or nothing, as when the user cancels). The calls it gets are
  // rendered, so that the tests can check them.
  macroEditor?: { result?: MacroBlockInvocation | InlineMacroInvocation };
};

export const BlockNoteForTest: React.FC<BlockNoteForTestProps> = ({
  content,
  options,
  bind,
  macroEditor,
}) => {
  const [macroEditorCalls, setMacroEditorCalls] = useState<unknown[]>([]);

  const serviceContainer = useMemo(() => {
    const container = serviceContainerMock([options.syntax]);
    bind?.(container);
    if (macroEditor) {
      container.bind<MacroEditor>(macroEditorName).toConstantValue({
        async insert(instance, prefill) {
          setMacroEditorCalls((calls) => [
            ...calls,
            { action: "insert", syntax: instance.options.syntax, prefill },
          ]);
          return macroEditor.result;
        },
        async edit(instance, invocation) {
          setMacroEditorCalls((calls) => [
            ...calls,
            { action: "edit", syntax: instance.options.syntax, invocation },
          ]);
          return macroEditor.result;
        },
      });
    }
    return container;
    // The services are registered once, when the container is created.
  }, []);

  return (
    <>
      <BlockNoteViewWrapper
        options={{
          lang: "en",
          label: "Some Label",
          serviceContainer,
          ...options,
          syntax: options.syntax.id,
        }}
        content={content}
      />
      {/* Rendered only once the macro editor is called, so that it doesn't change the text of the component checked
        by the other tests. */}
      {macroEditorCalls.length > 0 && (
        <output data-test="macroEditorCalls">
          {JSON.stringify(macroEditorCalls)}
        </output>
      )}
    </>
  );
};
