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
import { METADATA_COLUMNS } from "./fields";
import type { Problem } from "./fieldUse";
import type { FieldOption } from "./fields";

/**
 * The module holding the translation keys the dialog needs, which the `xwiki-l10n` loader plugin resolves into the
 * translated messages. The keys are the ones of the macro's translation bundle, without the prefix.
 */
const TRANSLATION_KEYS_MODULE = "xwiki-records-translation-keys";

/**
 * The prefix the translation keys of the macro's bundle share.
 */
const TRANSLATION_PREFIX = "rendering.macro.records.";

/**
 * The translated messages, as the `xwiki-l10n` loader plugin hands them over.
 */
interface Messages {
  get: (key: string, ...args: string[]) => string | null;
}

/**
 * The messages the dialog displays. Set when the module is initialized, which is before anything reads it.
 */
let messages: Messages = { get: () => null };

/**
 * Declares the translation keys module, so that the `xwiki-l10n` loader plugin can resolve it.
 */
function defineTranslationKeys(): void {
  // An empty dependency list and a factory returning the value is the typed form of a plain value module.
  define(TRANSLATION_KEYS_MODULE, [], () => ({
    prefix: TRANSLATION_PREFIX,
    keys: [
      "picker.group.fields",
      "picker.group.metadata",
      ...METADATA_COLUMNS.map((id) => `picker.metadata.${id}`),
      "picker.sort.ascending",
      "picker.sort.descending",
      "picker.sort.default",
      "picker.resetWarning",
      "picker.loadFailed",
      "picker.retry",
      "picker.dataTypeMissing",
      "picker.problem.fieldMissing",
      "picker.problem.notSortable",
      "picker.problem.notFilterable",
      "picker.problem.valueDoesNotFit",
      "picker.problem.unreadable",
    ],
  }));
}

/**
 * @param translations - the translated messages the loader plugin resolved
 */
function setMessages(translations: Messages): void {
  messages = translations;
}

/**
 * @param key - a translation key, without the prefix
 * @param args - the values of the message parameters
 * @returns the translated message, or the key when the bundle does not have it, so that a missing translation shows
 *   up as an identifier rather than as an empty label
 */
function translate(key: string, ...args: string[]): string {
  return messages.get(key, ...args) ?? key;
}

/**
 * @returns the translated labels the page metadata is offered under, by identifier
 */
function metadataLabels(): Record<string, string> {
  return Object.fromEntries(
    METADATA_COLUMNS.map((id) => [id, translate(`picker.metadata.${id}`)]),
  );
}

/**
 * @param option - the option showing a stored value back
 * @param problem - why the table ignores that value, if it does
 * @returns the option, carrying the translated problem
 */
function withProblem(
  option: FieldOption,
  problem: Problem | null,
): FieldOption {
  return problem === null
    ? option
    : { ...option, problem: translate(problem.key, ...problem.args) };
}

export {
  TRANSLATION_KEYS_MODULE,
  defineTranslationKeys,
  metadataLabels,
  setMessages,
  translate,
  withProblem,
};
export type { Messages };
