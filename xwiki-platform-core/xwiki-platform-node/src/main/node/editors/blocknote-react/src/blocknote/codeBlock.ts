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

import { codeBlockOptions } from "@blocknote/code-block";
import { createCodeBlockSpec } from "@blocknote/core";

/**
 * The language of the code blocks that don't specify one. We don't use the default language from `codeBlockOptions`
 * ("javascript") because code blocks without a language would then be saved as JavaScript.
 */
const DEFAULT_LANGUAGE = "text";

/**
 * Maps the id and the aliases of each language supported by the syntax highlighter to the language id (e.g. "js" is
 * mapped to "javascript").
 */
const HIGHLIGHTED_LANGUAGES = new Map(
  Object.entries(codeBlockOptions.supportedLanguages).flatMap(
    ([id, { aliases }]) =>
      [id, ...(aliases ?? [])].map((name) => [name.toLowerCase(), id]),
  ),
);

type CodeBlockSpec = ReturnType<typeof createCodeBlockSpec>;
type CodeBlockRender = CodeBlockSpec["implementation"]["render"];

/**
 * @returns true if the language selector supports the given language, false otherwise
 */
function isSelectable(language: string): boolean {
  return language in codeBlockOptions.supportedLanguages;
}

/**
 * Adds the given language to the language selector of a code block, and selects it.
 */
function selectLanguage(select: HTMLSelectElement, language: string) {
  const option = document.createElement("option");
  option.value = language;
  option.text = language;
  select.appendChild(option);
  select.value = language;
}

/**
 * Adds the given CSS classes to the `pre` element of a rendered code block. The `pre` element holds the code so it
 * matches the element that wraps the code snippets in view mode, while the language selector is positioned over its
 * top padding.
 */
function addClassName(
  nodeView: ReturnType<CodeBlockRender>,
  className: string | undefined,
): ReturnType<CodeBlockRender> {
  const classes = className?.split(/\s+/).filter((name) => name.length > 0);
  if (classes?.length) {
    nodeView.dom.querySelector("pre")?.classList.add(...classes);
  }
  return nodeView;
}

/**
 * Renders a code block whose language is not supported by the language selector (which throws an exception in this
 * case). The block is rendered as if it had the default language, and its actual language is added to the language
 * selector. The block itself is not modified, so its language is preserved on save. Syntax highlighting falls back to
 * plain text (see the `highlight` meta property of the code block spec).
 */
function renderUnsupportedLanguage(
  this: ThisParameterType<CodeBlockRender>,
  render: CodeBlockRender,
  ...[block, editor]: Parameters<CodeBlockRender>
): ReturnType<CodeBlockRender> {
  const language = block.props.language;
  const nodeView = render.call(
    this,
    { ...block, props: { ...block.props, language: DEFAULT_LANGUAGE } },
    editor,
  );

  const select = nodeView.dom.querySelector("select");
  if (select) {
    selectLanguage(select, language);
  }

  // The block props are exposed as data attributes only when they don't have the default value, so the language
  // attribute is missing because we rendered the block with the default language.
  if (nodeView.dom instanceof HTMLElement) {
    nodeView.dom.setAttribute("data-language", language);
  }

  return nodeView;
}

/**
 * Renders a code block, even if its language is not supported by the language selector.
 */
function renderCodeBlock(
  this: ThisParameterType<CodeBlockRender>,
  render: CodeBlockRender,
  ...[block, editor]: Parameters<CodeBlockRender>
): ReturnType<CodeBlockRender> {
  const language = block.props.language;
  if (isSelectable(language)) {
    try {
      return render.call(this, block, editor);
    } catch (e) {
      // Catch only the exception thrown by the language selector for unsupported languages.
      if (!(e instanceof Error && e.message.includes("is not supported"))) {
        throw e;
      }
      console.warn(
        `Failed to render the language selector of the code block with language [${language}].`,
        e,
      );
    }
  }
  return renderUnsupportedLanguage.call(this, render, block, editor);
}

/**
 * Creates the code block spec, with a language selector and syntax highlighting support, that doesn't fail when the
 * code block language is not supported.
 *
 * @param options - the code block options
 * @returns the code block spec
 */
function XWikiCodeBlock(options?: {
  /**
   * The CSS classes (space separated) to add to the code block, e.g. to style it like the code snippets displayed in
   * view mode.
   */
  className?: string;
}): CodeBlockSpec {
  const spec = createCodeBlockSpec({
    ...codeBlockOptions,
    defaultLanguage: DEFAULT_LANGUAGE,
  });
  spec.implementation.meta = {
    ...spec.implementation.meta,
    // The syntax highlighter throws an exception (that is only logged) when asked to highlight a language that is not
    // included in its bundle, so we highlight only the supported languages. The others are displayed as plain text.
    highlight: (block) =>
      HIGHLIGHTED_LANGUAGES.get(String(block.props.language).toLowerCase()),
  };

  const render = spec.implementation.render;
  spec.implementation.render = function (block, editor) {
    return addClassName(
      renderCodeBlock.call(this, render, block, editor),
      options?.className,
    );
  };

  return spec;
}

export { XWikiCodeBlock };
