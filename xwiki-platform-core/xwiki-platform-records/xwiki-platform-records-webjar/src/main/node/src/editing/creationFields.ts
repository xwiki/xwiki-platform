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

import { clearMessage, findMessage, showMessage } from "../common/dialog";
import { METADATA_PREFIX } from "../common/fields";
import { metadataLabels, translate } from "../common/translations";

/**
 * The fields of the Records macro dialog that decide whether readers can create entries from the table.
 *
 * Creating entries needs the edit mode, so the creation strategy is disabled while the edit mode is not allowed, and
 * the location is disabled while there is no strategy. A strategy is also disabled when the table does not display
 * the columns it names the new pages after, with the reason in a warning below the dropdown. The macro editor does not
 * save a disabled field, so turning the edit mode off drops the strategy and the location.
 */

const EDIT_MODE_PARAMETER = "allowEditMode";

const STRATEGY_PARAMETER = "creationStrategy";

const LOCATION_PARAMETER = "newEntriesLocation";

const COLUMNS_PARAMETER = "properties";

/**
 * The column holding the page title.
 */
const TITLE_COLUMN = "doc.title";

/**
 * Registers a change listener. The suggest widget notifies its changes through jQuery, which a native listener
 * would miss, so the caller decides how to listen.
 */
type ChangeListener = (element: Element, handler: () => void) => void;

/**
 * @param scope - the dialog's form
 * @returns the edit mode checkbox, or null when the dialog has none
 */
function findEditModeInput(scope: ParentNode): HTMLInputElement | null {
  // The checkbox is followed by a hidden input of the same name, which posts false when it is not checked.
  return scope.querySelector<HTMLInputElement>(
    `input[type="checkbox"][name="${EDIT_MODE_PARAMETER}"]`,
  );
}

/**
 * @param scope - the dialog's form
 * @returns the creation strategy dropdown, or null when the dialog has none
 */
function findStrategySelect(scope: ParentNode): HTMLSelectElement | null {
  return scope.querySelector<HTMLSelectElement>(
    `select[name="${STRATEGY_PARAMETER}"]`,
  );
}

/**
 * @param scope - the dialog's form
 * @returns the columns displayed, or null when the author picked none, in which case the table displays the title and
 *   every field
 */
function getDisplayedColumns(scope: ParentNode): string[] | null {
  const field = scope.querySelector(`[name="${COLUMNS_PARAMETER}"]`);
  let values: string[] = [];
  if (field instanceof HTMLSelectElement) {
    values = Array.from(field.selectedOptions, (option) => option.value);
  } else if (field instanceof HTMLInputElement) {
    values = field.value.split(",");
  }
  const columns = values.map((value) => value.trim()).filter(Boolean);
  return columns.length > 0 ? columns : null;
}

/**
 * @param option - a creation strategy option
 * @param displayed - the columns displayed, or null when the author picked none
 * @returns the columns the strategy needs that the table does not display
 */
function getMissingColumns(
  option: HTMLOptionElement,
  displayed: string[] | null,
): string[] {
  return (option.dataset.requiredColumns ?? "")
    .split(",")
    .filter(
      (column) =>
        column !== "" &&
        !(displayed?.includes(column) ?? isDisplayedByDefault(column)),
    );
}

/**
 * @param column - a column
 * @returns whether the table displays the column when the author picked none: the title and every field do
 */
function isDisplayedByDefault(column: string): boolean {
  return column === TITLE_COLUMN || !column.startsWith(METADATA_PREFIX);
}

/**
 * Enables the location picker only when it is used.
 *
 * @param scope - the dialog's form
 * @param enabled - whether the picker is enabled
 */
function setLocationEnabled(scope: ParentNode, enabled: boolean): void {
  scope.querySelectorAll(`[name="${LOCATION_PARAMETER}"]`).forEach((field) => {
    if (field.selectize) {
      if (enabled) {
        field.selectize.enable();
      } else {
        field.selectize.disable();
      }
    }
    if (
      field instanceof HTMLInputElement ||
      field instanceof HTMLSelectElement
    ) {
      field.disabled = !enabled;
    }
    // The button opening the tree next to the picker.
    field
      .closest(".location-picker-compact")
      ?.querySelectorAll("button")
      .forEach((button) => {
        button.disabled = !enabled;
      });
  });
}

/**
 * Disables the strategies needing a column the table does not display.
 *
 * @param strategy - the creation strategy dropdown
 * @param displayed - the columns displayed, or null when the author picked none
 * @returns why strategies are disabled, one line per missing column so that translations do not depend on their count
 */
function updateStrategyOptions(
  strategy: HTMLSelectElement,
  displayed: string[] | null,
): string[] {
  const labels = metadataLabels();
  return Array.from(strategy.options).flatMap((option) => {
    const missing = getMissingColumns(option, displayed);
    option.disabled = missing.length > 0;
    return missing.map((column) =>
      translate(
        "picker.creation.columnsMissing",
        option.text,
        labels[column] ?? column,
      ),
    );
  });
}

/**
 * Brings the creation fields in line with the edit mode and the columns.
 *
 * @param scope - the dialog's form
 * @param reset - whether to turn creation off when the selected strategy needs a column that is no longer displayed;
 *   false when the dialog opens, so that reopening it does not silently change what was saved
 */
function updateCreationFields(scope: ParentNode, reset: boolean): void {
  const strategy = findStrategySelect(scope);
  if (strategy === null) {
    return;
  }
  const problems = updateStrategyOptions(strategy, getDisplayedColumns(scope));
  if (reset && strategy.selectedOptions[0]?.disabled) {
    strategy.value = "";
  }
  // The macro editor does not save a disabled option.
  strategy.selectedOptions[0]?.removeAttribute("disabled");
  strategy.disabled = findEditModeInput(scope)?.checked === false;
  setLocationEnabled(scope, !strategy.disabled && strategy.value !== "");
  showStrategyProblems(strategy, problems);
}

/**
 * Shows below the dropdown why strategies are disabled, while it can be used.
 *
 * @param strategy - the creation strategy dropdown
 * @param problems - why strategies are disabled
 */
function showStrategyProblems(
  strategy: HTMLSelectElement,
  problems: string[],
): void {
  if (strategy.disabled || problems.length === 0) {
    clearMessage(strategy, "warning");
  } else if (
    findMessage(strategy, "warning")?.textContent !== problems.join("")
  ) {
    // Only when the text changes, so that screen readers do not announce it again.
    showMessage(strategy, "warning", problems);
  }
}

/**
 * Keeps the creation fields of a dialog in line with the edit mode and the columns.
 *
 * @param scope - the dialog's form
 * @param listen - registers a change listener on a field
 */
function wireCreationFields(scope: ParentNode, listen: ChangeListener): void {
  updateCreationFields(scope, false);
  [EDIT_MODE_PARAMETER, STRATEGY_PARAMETER, COLUMNS_PARAMETER].forEach(
    (name) => {
      scope.querySelectorAll(`[name="${name}"]`).forEach((field) => {
        listen(field, () => updateCreationFields(scope, true));
      });
    },
  );
}

export { updateCreationFields, wireCreationFields };
export type { ChangeListener };
