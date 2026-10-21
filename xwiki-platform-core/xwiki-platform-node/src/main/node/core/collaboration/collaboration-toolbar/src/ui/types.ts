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

import type { XWikiDocument } from "@xwiki/platform-document-xwiki";
import type { I18n } from "vue-i18n";

/**
 * The status of the connection to the realtime collaboration session, or of the save:
 *
 * - for the connection: 0 = disconnected, 1 = connecting, 2 = connected (the values of `ConnectionStatus` from
 *   `@xwiki/platform-collaboration-api`)
 *
 * - for the save: 0 = unsaved changes, 1 = saving, 2 = saved (the values of `SaveStatus` from
 *   `@xwiki/platform-autosave-api`)
 *
 * @since 18.9.0RC1
 * @beta
 */
type ToolbarStatus = 0 | 1 | 2;

/**
 * A user participating in the realtime collaboration session.
 *
 * @since 18.9.0RC1
 * @beta
 */
type ToolbarUser = {
  /**
   * Identifies the user in the realtime collaboration session. The same user can join multiple times (e.g. from
   * different browser tabs) so this is not the user reference.
   */
  id: string;

  /**
   * The user reference, used to link to the user profile page.
   */
  reference: string;

  /**
   * The user name to display.
   */
  name: string;

  /**
   * The URL of the user avatar. The default avatar is used when missing.
   */
  avatar?: string;
};

/**
 * A document version created during the realtime collaboration session. Unlike the versions reported by the save
 * target (`DocumentVersion` from `@xwiki/platform-autosave-xwiki`), whose author can be a client identifier left for
 * the caller to resolve, the author of a toolbar version is meant to be displayed as is: the realtime editor resolves
 * it against the users of the collaboration session before passing the version to the toolbar.
 *
 * @since 18.9.0RC1
 * @beta
 */
type ToolbarVersion = {
  /**
   * The version number, e.g. `2.1`.
   */
  number: string;

  /**
   * The version creation date, as a timestamp.
   */
  date: number;

  /**
   * The user that created the version, or an identifier when the user couldn't be determined (e.g. the realtime
   * session identifier of a user that left the session in the meantime).
   */
  author: Partial<Omit<ToolbarUser, "id">> | string;
};

/**
 * A warning displayed on the realtime toolbar (e.g. when the realtime session can't share the changes anymore), as an
 * icon that shows the warning message in a popover.
 *
 * @since 18.9.0
 * @beta
 */
type ToolbarWarning = {
  /**
   * Identifies the warning, in order to replace or remove it.
   */
  id: string;

  /**
   * The warning title, displayed as plain text.
   */
  title: string;

  /**
   * The warning message, displayed as plain text.
   */
  message: string;
};

/**
 * The state displayed by the realtime toolbar.
 */
type ToolbarState = {
  /**
   * Whether we are disconnected, connecting or connected to the realtime collaboration session.
   */
  connectionStatus: ToolbarStatus;
  /**
   * Whether the form data is unsaved, being saved or saved. Undefined until the first save status update.
   */
  saveStatus?: ToolbarStatus;
  /**
   * The list of users connected to the realtime collaboration session.
   */
  users: ToolbarUser[];
  /**
   * The warnings to display.
   */
  warnings: ToolbarWarning[];
  /**
   * The most recent versions (revisions) of the edited document, created during the realtime collaboration session.
   */
  versions: ToolbarVersion[];
  /**
   * The version the user has seen the changes of, used as reference when showing the changes in the change summary
   * modal.
   */
  lastReviewedVersion?: string;
};

/**
 * The changes to summarize, shown in the change summary modal.
 */
type Changes =
  /**
   * The document is new, so there's no previous version to compare with.
   */
  | { status: "isNew" }
  /**
   * There are no changes since the reviewed version.
   */
  | { status: "empty" }
  /**
   * The changes were loaded.
   */
  | {
      status: "loaded";
      /**
       * The HTML that shows the changes.
       */
      diff: string;
    };

/**
 * The output of the change summary modal.
 */
type ChangeSummary = {
  /**
   * A text summarizing the recent changes (since the last reviewed revision).
   */
  summary: string;
  /**
   * Whether the changes are minor, undefined when the user can't choose.
   */
  minorEdit?: boolean;
};

/**
 * The actions of the realtime collaboration session that the realtime toolbar triggers.
 *
 * @since 18.9.0RC1
 * @beta
 */
type ToolbarSession = {
  /**
   * Saves the edited document.
   *
   * @param continueEditing - whether to remain in edit mode after saving or not
   */
  save(continueEditing?: boolean): Promise<void> | void;

  /**
   * Whether the session can save while disconnected from the collaboration server, when saving doesn't depend on the
   * connection. The save actions are disabled while disconnected when missing or false.
   *
   * @since 18.9.0
   */
  canSaveWhileDisconnected?: boolean;

  /**
   * Leaves the realtime collaboration session. The "Leave Collaboration" action is not available when missing.
   */
  leave?(): void;

  /**
   * Called when the user clicks on one of the users participating in the realtime collaboration session. It should
   * scroll the page to the form field that is currently edited by the clicked user. If that field is a rich text box,
   * it should scroll the edited content to the place where the clicked user is typing.
   *
   * @param userId - the identifier of the selected user in the realtime collaboration session
   */
  selectUser?(userId: string): void;
};

/**
 * What the user can specify when saving, besides the edited data.
 *
 * @since 18.9.0RC1
 * @beta
 */
type ToolbarSaveOptions = {
  /**
   * Whether the user can enter a change summary (version comment) when saving, and whether it is mandatory.
   */
  editComment: { enabled: boolean; mandatory: boolean };
  /**
   * Whether the user can mark the changes as minor.
   */
  minorEdit: boolean;
};

/**
 * Everything the realtime toolbar needs from the page it is displayed on: configuration, URLs, access to the edit form,
 * etc. It doesn't depend on the edited document, field nor on the realtime synchronization, so it is shared by all
 * the realtime toolbars of the page.
 *
 * @since 18.9.0RC1
 * @beta
 */
type ToolbarEnvironment = {
  /**
   * The URL of the avatar displayed for the users that don't have one.
   */
  defaultAvatarURL: string;
  /**
   * The HTML of the icons used by the toolbar.
   */
  icons: { check: string; clock: string; warning: string };
  /**
   * @param form - the edit form
   * @returns what the user can specify when saving with the given edit form
   */
  getSaveOptions(form: HTMLElement): ToolbarSaveOptions;
  /**
   * @param date - a timestamp
   * @returns the given date formatted for display
   */
  formatDate(date: number): string;
  /**
   * @param reference - a user reference
   * @returns the URL of the user profile page
   */
  getUserURL(reference: string): string;
  /**
   * @param document - the edited document
   * @param version - a version of the edited document
   * @returns the URL to view the given document version
   */
  getVersionURL(document: XWikiDocument, version: string): string;
  /**
   * @param document - the edited document
   * @param version - a version of the edited document
   * @returns the URL to get the rendered content of the given document version
   */
  getVersionContentURL(document: XWikiDocument, version: string): string;
  /**
   * @param form - the edit form, whose data holds the unsaved changes
   * @param document - the edited document
   * @param previousVersion - the version to compare with
   * @param toolbar - the toolbar element, inside the edit form, used to notify the others that the form is about to
   *   be submitted
   * @returns the HTML that shows the changes made since the given version
   */
  fetchChanges(
    form: HTMLElement,
    document: XWikiDocument,
    previousVersion: string | undefined,
    toolbar: HTMLElement,
  ): Promise<string>;
  /**
   * Puts the change summary and the minor edit flag in the edit form, before saving.
   *
   * @param form - the edit form
   * @param summary - the change summary
   * @param minorEdit - whether the change is minor, undefined when the user can't choose
   */
  setChangeSummary(
    form: HTMLElement,
    summary: string,
    minorEdit?: boolean,
  ): void;
};

/**
 * What the realtime toolbar is created with.
 *
 * @since 18.9.0RC1
 * @beta
 */
type ToolbarOptions = {
  /**
   * The edit form whose standard edit toolbar is replaced by the realtime toolbar. It is also the form that the
   * realtime toolbar fetches the changes from and puts the change summary in.
   */
  form: HTMLElement;

  /**
   * The document edited with the edit form, used to show its versions and its changes. The caller is responsible for
   * keeping it up to date (e.g. it is not new anymore after the first save).
   */
  document: XWikiDocument;

  /**
   * The actions of the realtime collaboration session that the toolbar triggers.
   */
  session: ToolbarSession;

  /**
   * What the toolbar needs from the page, see `createXWikiToolbarEnvironment`.
   */
  environment: ToolbarEnvironment;

  /**
   * The translations of the toolbar, see `loadXWikiToolbarTranslations`.
   */
  i18n: I18n;
};

export type {
  ChangeSummary,
  Changes,
  ToolbarEnvironment,
  ToolbarOptions,
  ToolbarSaveOptions,
  ToolbarSession,
  ToolbarState,
  ToolbarStatus,
  ToolbarUser,
  ToolbarVersion,
  ToolbarWarning,
};
