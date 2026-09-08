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

/**
 * The parts of the Records macro dialog that react to the data type.
 *
 * Everything here is plain DOM work kept apart from the RequireJS and jQuery wiring in the entry point, so that it
 * can be tested. The dialog's shape is not ours: {@link https://github.com/xwiki/xwiki-platform} builds it from the
 * macro descriptor, and the two things this module needs from it are stable — the tab strip and its panes are
 * `.macro-tabs` and `.tab-content` siblings of the mandatory fields, and every parameter field is an input named
 * after its parameter.
 */

import { DATA_TYPE_PARAMETER } from "./fieldPicker";

/**
 * The parameters whose value is derived from the data type.
 *
 * Their candidates are that data type's fields, so none of them survives a change of data type: a column, a filter
 * or a sort naming a field the new type does not have would render a table the author cannot explain.
 */
const DERIVED_PARAMETERS: readonly string[] = ["properties", "filters", "sort"];

/**
 * A Tom Select instance, as the suggest widget exposes it on the enhanced element.
 */
interface Enhanced {
  selectize?: {
    clear: (silent?: boolean) => void;
    clearOptions: () => void;
  };
}

/**
 * Returns the form the dialog's fields live in, which scopes every lookup to one dialog.
 *
 * @param element - any element inside the dialog
 * @returns the enclosing form, or the document when the field is not in one
 */
function findScope(element: Element): ParentNode {
  return element.closest("form") ?? element.ownerDocument;
}

/**
 * Returns the data type input of a dialog.
 *
 * @param scope - the dialog's form
 * @returns the input, or null when the dialog has none
 */
function findDataTypeInput(
  scope: ParentNode,
): HTMLInputElement | HTMLSelectElement | null {
  const field = scope.querySelector(`[name="${DATA_TYPE_PARAMETER}"]`);
  return field instanceof HTMLInputElement || field instanceof HTMLSelectElement
    ? field
    : null;
}

/**
 * Shows or hides the tab strip and its panes.
 *
 * Hiding rather than disabling is deliberate: the macro editor has no disabled state for a group, so the choice is
 * between a tab that is reachable but has nothing to offer and no tab at all. Until a data type is picked, every
 * tab is derived from it, so there is nothing to show.
 *
 * @param scope - the dialog's form
 * @param visible - whether the tabs should be shown
 */
function setTabsVisible(scope: ParentNode, visible: boolean): void {
  scope.querySelectorAll(".macro-tabs, .tab-content").forEach((element) => {
    if (element instanceof HTMLElement) {
      element.hidden = !visible;
    }
  });
}

/**
 * Clears the parameters derived from the data type, back to their defaults.
 *
 * The default of all three is empty: every field, no filter, no sort. An enhanced widget is cleared through its own
 * API rather than by resetting the underlying element, so that what the author sees matches what will be saved.
 *
 * @param scope - the dialog's form
 * @returns the names of the parameters that actually held a value
 */
function resetDerivedParameters(scope: ParentNode): string[] {
  const reset: string[] = [];
  DERIVED_PARAMETERS.forEach((name) => {
    scope.querySelectorAll(`[name="${name}"]`).forEach((element) => {
      if (resetField(element)) {
        reset.push(name);
      }
    });
  });
  return [...new Set(reset)];
}

/**
 * Clears one parameter field.
 *
 * @param element - the field to clear
 * @returns whether it held a value before being cleared
 */
function resetField(element: Element): boolean {
  const held = hasValue(element);
  const enhanced = (element as unknown as Enhanced).selectize;
  if (enhanced) {
    enhanced.clear(true);
    enhanced.clearOptions();
  } else if (
    element instanceof HTMLInputElement ||
    element instanceof HTMLTextAreaElement
  ) {
    element.value = "";
  } else if (element instanceof HTMLSelectElement) {
    element.selectedIndex = -1;
  }
  return held;
}

/**
 * @param element - a parameter field
 * @returns whether the field holds anything
 */
function hasValue(element: Element): boolean {
  if (element instanceof HTMLSelectElement) {
    return Array.from(element.selectedOptions).some(
      (option) => option.value !== "",
    );
  }
  if (
    element instanceof HTMLInputElement ||
    element instanceof HTMLTextAreaElement
  ) {
    return element.value !== "";
  }
  return false;
}

export {
  DERIVED_PARAMETERS,
  findDataTypeInput,
  findScope,
  hasValue,
  resetDerivedParameters,
  setTabsVisible,
};
