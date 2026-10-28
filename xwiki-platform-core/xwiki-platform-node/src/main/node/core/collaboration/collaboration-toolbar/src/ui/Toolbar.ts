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

import ToolbarView from "./ToolbarView.vue";
import { onModalEvent } from "./bootstrap";
import {
  documentKey,
  environmentKey,
  formKey,
  sessionKey,
  stateKey,
  toolbarKey,
} from "./context";
import { createApp, reactive } from "vue";
import type {
  ToolbarOptions,
  ToolbarState,
  ToolbarStatus,
  ToolbarUser,
  ToolbarVersion,
  ToolbarWarning,
} from "./types";
import type { App } from "vue";

/**
 * The standard edit toolbar, inside the edit form, that the realtime toolbar replaces while editing in realtime.
 */
const STANDARD_TOOLBAR_SELECTOR = [
  ".bottombuttons.sticky-buttons > .buttons",
  // The standard edit toolbar is moved when editing in fullscreen mode with the standalone editor.
  ".cke_maximized > .buttons",
  ".inplace-editing-buttons.sticky-buttons > .buttons",
].join(", ");

/**
 * The maximum number of versions listed in the history dropdown.
 */
const VERSION_LIMIT = 5;

/**
 * The modals displayed by the realtime toolbar, outside the toolbar element.
 */
const MODAL_SELECTOR = [
  "#realtime-changeSummaryModal",
  "#realtime-version-modal",
  "#realtime-leave-modal",
].join(", ");

function findOldToolbar(form: HTMLElement): HTMLElement {
  const oldToolbar = form.querySelector<HTMLElement>(STANDARD_TOOLBAR_SELECTOR);
  if (!oldToolbar) {
    throw new Error("Failed to find the edit toolbar to replace.");
  }
  return oldToolbar;
}

async function waitUntilHidden(modal: HTMLElement): Promise<void> {
  let onHidden: () => void;
  const hidden = new Promise<void>((resolve) => (onHidden = resolve));
  const removeListener = await onModalEvent(modal, "hidden", () => onHidden());
  // The listener is registered asynchronously so the modal may have been hidden in the meantime (e.g. when there's no
  // fade effect).
  if (modal.classList.contains("in")) {
    await hidden;
  }
  removeListener();
}

function createToolbarElement(oldToolbar: HTMLElement): HTMLElement {
  const toolbar = document.createElement("div");
  // Inherit some styles from the standard edit toolbar.
  toolbar.classList.add("realtime-edit-toolbar", ...oldToolbar.classList);
  // The user identifier is set once the user joins the realtime session.
  toolbar.dataset.userId = "";
  return toolbar;
}

/**
 * The toolbar displayed while editing in realtime, in place of the standard edit toolbar. It shows the connection
 * status, the users editing in realtime, the save status and the versions created during the realtime session, and it
 * provides the actions to save, summarize the changes and leave the realtime session.
 *
 * @since 18.9.0RC1
 * @beta
 */
class Toolbar {
  private readonly form: HTMLElement;

  private readonly oldToolbar: HTMLElement;

  private readonly toolbar: HTMLElement;

  private readonly state: ToolbarState;

  private readonly app: App;

  private userListSignature?: string;

  /**
   * Creates the realtime toolbar and displays it in place of the standard edit toolbar of the given edit form.
   *
   * @param options - the edit form and the document the toolbar is for, the realtime session and what the toolbar
   *   needs from the page
   */
  constructor(options: ToolbarOptions) {
    const { form, session, environment, i18n } = options;
    this.form = form;
    this.oldToolbar = findOldToolbar(form);
    this.toolbar = createToolbarElement(this.oldToolbar);
    this.oldToolbar.before(this.toolbar);
    this.oldToolbar.hidden = true;

    this.state = reactive<ToolbarState>({
      connectionStatus: 1,
      users: [],
      versions: [],
      warnings: [],
    });

    this.app = createApp(ToolbarView)
      .use(i18n)
      .provide(environmentKey, environment)
      .provide(formKey, form)
      .provide(documentKey, options.document)
      .provide(sessionKey, session)
      .provide(stateKey, this.state)
      .provide(toolbarKey, this.toolbar);
    this.app.mount(this.toolbar);
  }

  /**
   * Updates the displayed save status. Saving is not possible while the document is being saved.
   *
   * @param status - the new save status
   */
  onSaveStatusChange(status: ToolbarStatus): void {
    this.state.saveStatus = status;
  }

  /**
   * Updates the displayed connection status. The user list is cleared while not connected, and saving is not
   * possible, unless the session can save while disconnected.
   *
   * @param status - the new connection status
   * @param userId - the identifier of the current user in the realtime session, once known
   */
  onConnectionStatusChange(status: ToolbarStatus, userId?: string): void {
    this.state.connectionStatus = status;
    if (status !== 2) {
      this.onUserListChange([]);
    }
    if (userId) {
      this.toolbar.dataset.userId = userId;
    }
  }

  /**
   * Updates the list of users participating in the realtime session.
   *
   * @param users - the users currently participating in the realtime session
   */
  onUserListChange(users: ToolbarUser[]): void {
    // The user data we receive may also hold information that is not displayed on the toolbar (e.g. the caret position
    // of each user, which changes very often). Skip the update when nothing we display has changed, in order to avoid
    // needless re-rendering.
    const toolbarUsers = users.map(({ id, reference, name, avatar }) => ({
      id,
      reference,
      name,
      avatar,
    }));
    const signature = JSON.stringify(toolbarUsers);
    if (signature !== this.userListSignature) {
      this.userListSignature = signature;
      this.state.users = toolbarUsers;
    }
  }

  /**
   * Adds a version to the history dropdown, which shows only the most recent versions.
   *
   * @param version - a document version created during the realtime session
   */
  onCreateVersion(version: ToolbarVersion): void {
    this.state.lastReviewedVersion ??= version.number;
    // Make sure we don't add the same version twice. This can happen if some of the previous versions were deleted and
    // their version numbers are being reused.
    const versions = this.state.versions.filter(
      (existingVersion) => existingVersion.number !== version.number,
    );
    if (versions.length >= VERSION_LIMIT) {
      versions.shift();
    }
    versions.push(version);
    this.state.versions = versions;
  }

  /**
   * Displays a warning on the toolbar, replacing the warning with the same identifier, if any. The warning message is
   * shown once, right after the warning is displayed, and then each time the user clicks the warning icon.
   *
   * @param warning - the warning to display
   * @since 18.9.0
   */
  addWarning(warning: ToolbarWarning): void {
    this.state.warnings = [
      ...this.state.warnings.filter(({ id }) => id !== warning.id),
      warning,
    ];
  }

  /**
   * Removes a warning from the toolbar.
   *
   * @param id - the identifier of the warning to remove
   * @since 18.9.0
   */
  removeWarning(id: string): void {
    this.state.warnings = this.state.warnings.filter(
      (warning) => warning.id !== id,
    );
  }

  /**
   * Gets the edit form the toolbar belongs to.
   *
   * @returns the form that holds the edited document fields
   */
  getForm(): HTMLElement {
    return this.form;
  }

  /**
   * Removes the realtime toolbar and shows back the standard edit toolbar.
   */
  destroy(): void {
    this.oldToolbar.hidden = false;
    this.toolbar.remove();

    // Unmounting the toolbar removes the modals, so we have to wait for them to be closed (e.g. leaving the realtime
    // session is done from a modal), otherwise the modal backdrop remains.
    const openModals = [
      ...document.querySelectorAll<HTMLElement>(MODAL_SELECTOR),
    ].filter((modal) => modal.classList.contains("in"));
    if (!openModals.length) {
      this.app.unmount();
      return;
    }
    void (async () => {
      try {
        await Promise.all(openModals.map(waitUntilHidden));
      } catch (error) {
        console.error(error);
      } finally {
        this.app.unmount();
      }
    })();
  }
}

export { STANDARD_TOOLBAR_SELECTOR, Toolbar };
