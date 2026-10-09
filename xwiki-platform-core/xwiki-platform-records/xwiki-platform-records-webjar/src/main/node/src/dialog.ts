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
  const enhanced = element.selectize;
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

/**
 * The CSS class marking the message box this module puts under a field, so that it is replaced rather than stacked.
 */
const MESSAGE_CLASS = "records-picker-message";

/**
 * The style of a message box: `error` for something that failed, `warning` for something the author should fix.
 */
type MessageKind = "error" | "warning";

/**
 * A button offered in a message box.
 */
interface MessageAction {
  label: string;
  run: () => void;
}

/**
 * Returns the name the author knows the selected data type under.
 *
 * The class picker is a select whose options are labelled with the class names, so the selected option's text is
 * that name; a plain input only has the reference.
 *
 * @param dataTypeInput - the data type field
 * @returns the label of the selected data type, or its reference when there is no label
 */
function findDataTypeLabel(
  dataTypeInput: HTMLInputElement | HTMLSelectElement,
): string {
  const label =
    dataTypeInput instanceof HTMLSelectElement
      ? dataTypeInput.selectedOptions[0]?.text.trim()
      : undefined;
  return label || dataTypeInput.value;
}

/**
 * Shows a message right after a field, replacing the one already there.
 *
 * The message is plain text, since it carries names the author or the wiki chose. The box is an alert so that a
 * screen reader announces a failure the author did not trigger directly, such as one of the loads a dialog makes
 * when it opens.
 *
 * @param anchor - the element to show the message after: the enhanced widget, or the field itself
 * @param kind - the box style, `error` for something that failed and `warning` for something the author should fix
 * @param text - the message, or several, shown as a list
 * @param action - the button to offer next to the message, if any
 */
function showMessage(
  anchor: Element,
  kind: MessageKind,
  text: string | string[],
  action?: MessageAction,
): void {
  clearMessage(anchor);
  const box = anchor.ownerDocument.createElement("div");
  box.className = `box ${kind}message ${MESSAGE_CLASS}`;
  box.setAttribute("role", "alert");
  // Keep the box apart from the field it follows.
  box.style.marginTop = "0.75em";
  box.append(createMessageText(anchor.ownerDocument, text));
  if (action) {
    box.append(" ", createActionButton(anchor, action));
  }
  anchor.after(box);
}

/**
 * @param document - the document the message is shown in
 * @param text - the message, or several
 * @returns the message as text, or the messages as a list
 */
function createMessageText(
  document: Document,
  text: string | string[],
): Element {
  if (!Array.isArray(text)) {
    const message = document.createElement("span");
    message.textContent = text;
    return message;
  }
  const list = document.createElement("ul");
  list.append(
    ...text.map((line) => {
      const item = document.createElement("li");
      item.textContent = line;
      return item;
    }),
  );
  return list;
}

/**
 * @param anchor - the element the message is shown after
 * @param action - the button to create
 * @returns a button that removes the message, then runs the action
 */
function createActionButton(
  anchor: Element,
  action: MessageAction,
): HTMLButtonElement {
  const button = anchor.ownerDocument.createElement("button");
  // Not a submit button: the box sits inside the dialog's form.
  button.type = "button";
  button.className = "btn btn-default btn-xs";
  button.textContent = action.label;
  button.addEventListener("click", () => {
    clearMessage(anchor);
    action.run();
  });
  return button;
}

/**
 * Removes the message shown after a field, if any.
 *
 * @param anchor - the element the message was shown after
 * @param kind - the only style of message to remove, when not any
 */
function clearMessage(anchor: Element, kind?: MessageKind): void {
  const next = findMessage(anchor, kind);
  next?.remove();
}

/**
 * @param anchor - the element a message is shown after
 * @param kind - the only style of message to find, when not any
 * @returns the message shown after the element, if any
 */
function findMessage(anchor: Element, kind?: MessageKind): Element | null {
  const next = anchor.nextElementSibling;
  return next?.classList.contains(MESSAGE_CLASS) &&
    (kind === undefined || next.classList.contains(`${kind}message`))
    ? next
    : null;
}

/**
 * Points at the selected items of a picker that the table ignores, and says why under the picker.
 *
 * An item is struck through, with the reason as its tooltip, and the reasons are listed in a warning box, which is
 * what a screen reader announces. The box is left as it is when it already says the same thing, so that it is not
 * announced again on every keystroke, and an error box is left alone, since it says the items could not be checked.
 *
 * @param wrapper - the element the widget is rendered in
 * @param problemOf - why the table ignores the item holding a value, if it does
 */
function showProblems(
  wrapper: Element,
  problemOf: (value: string) => string | undefined,
): void {
  const problems = new Set<string>();
  wrapper.querySelectorAll(".item[data-value]").forEach((item) => {
    if (!(item instanceof HTMLElement)) {
      return;
    }
    const problem = problemOf(item.dataset.value ?? "");
    // The label only: the item also holds its remove button.
    const label =
      item.querySelector<HTMLElement>(".xwiki-selectize-option-label") ?? item;
    label.style.textDecoration = problem === undefined ? "" : "line-through";
    if (problem !== undefined) {
      item.title = problem;
      problems.add(problem);
    }
  });
  if (findMessage(wrapper, "error") !== null) {
    return;
  }
  const lines = [...problems];
  if (lines.length === 0) {
    clearMessage(wrapper, "warning");
  } else if (findMessage(wrapper, "warning")?.textContent !== lines.join("")) {
    showMessage(wrapper, "warning", lines);
  }
}

/**
 * Removes every message shown in a dialog, which all concern the data type that was selected when they appeared.
 *
 * @param scope - the dialog's form
 */
function clearMessages(scope: ParentNode): void {
  scope.querySelectorAll(`.${MESSAGE_CLASS}`).forEach((box) => box.remove());
}

export {
  DERIVED_PARAMETERS,
  clearMessage,
  clearMessages,
  findDataTypeInput,
  findDataTypeLabel,
  findScope,
  hasValue,
  resetDerivedParameters,
  setTabsVisible,
  showMessage,
  showProblems,
};
export type { MessageAction, MessageKind };
