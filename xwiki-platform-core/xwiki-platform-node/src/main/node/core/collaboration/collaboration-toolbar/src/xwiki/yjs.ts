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

import { createXWikiToolbarEnvironment } from "./environment";
import { loadXWikiToolbarTranslations } from "./translations";
import { Toolbar } from "../ui/Toolbar";
import { watch } from "vue";
import type {
  ToolbarStatus,
  ToolbarUser,
  ToolbarVersion,
  ToolbarWarning,
} from "../ui/types";
import type { Saver } from "@xwiki/platform-autosave-api";
import type {
  DocumentVersion,
  XWikiFormSaveContext,
} from "@xwiki/platform-autosave-xwiki";
import type {
  Collaboration,
  CollaborationWarning,
  Collaborator,
} from "@xwiki/platform-collaboration-api";
import type { XWikiDocument } from "@xwiki/platform-document-xwiki";

/**
 * What the realtime toolbar of a Yjs collaboration session is created with.
 *
 * @since 18.9.0RC1
 * @beta
 */
type YjsToolbarOptions = {
  /**
   * The edit form the toolbar is created for: the realtime toolbar replaces the standard edit toolbar found in it, reads
   * from it whether the version summary and the minor edit options are available, and fills the version summary
   * before a save.
   */
  form: HTMLElement;

  /**
   * The edited document, used to link the versions listed by the toolbar (and their content) and to compute the changes
   * made since the previous version when the user summarizes them. The save target keeps it up to date.
   */
  document: XWikiDocument;

  /**
   * The collaboration session, providing the connection status and the list of coeditors displayed by the toolbar. The
   * coeditors are also used to identify the authors of the versions, which the save target reports by client id.
   */
  collaboration: Collaboration;

  /**
   * The auto-saver of the collaboration session, used to save when the user clicks the toolbar save actions, and
   * providing the save status displayed by the toolbar.
   */
  saver: Saver<XWikiFormSaveContext>;

  /**
   * The versions reported by the save target before the toolbar was created, oldest first. The toolbar is created
   * asynchronously, after the auto-saver (it has to load its code, translations and environment), and meanwhile the save
   * target can already report versions: the version the editing session starts from, and the versions saved by the
   * other collaborators.
   */
  versions?: DocumentVersion[];
};

/**
 * The realtime toolbar of a Yjs collaboration session.
 *
 * @since 18.9.0RC1
 * @beta
 */
type YjsToolbar = {
  /**
   * Adds a version, reported by the save target, to the history dropdown.
   *
   * @param version - a version of the edited document
   */
  onCreateVersion(version: DocumentVersion): void;

  /**
   * Stops listening to the collaboration session and shows back the standard edit toolbar.
   */
  destroy(): void;
};

/**
 * The edit forms that have a realtime toolbar created by this module.
 */
const formsWithToolbar = new WeakSet<HTMLElement>();

function toToolbarUser({ id, user }: Collaborator): ToolbarUser {
  return {
    id: id ?? "",
    reference: user.name,
    name: user.username ?? user.name,
    avatar: user.avatar,
  };
}

/**
 * @param version - a version reported by the save target
 * @param collaborators - the users currently participating in the collaboration session
 * @returns the version to display, with its author resolved, when the author is still in the collaboration session
 */
function toToolbarVersion(
  { number, date, author }: DocumentVersion,
  collaborators: Collaborator[],
): ToolbarVersion {
  if (typeof author === "string") {
    // The save target reports the client identifier of the author, which is the identifier of the collaborator.
    const collaborator = collaborators.find(({ id }) => id === author);
    return {
      number,
      date,
      author: collaborator ? toToolbarUser(collaborator) : author,
    };
  }
  return { number, date, author: author ?? "" };
}

/**
 * Translates a translation key of the realtime toolbar.
 */
type Translate = (key: string) => string;

/**
 * @param warning - a warning reported by the collaboration session
 * @param translate - used to translate the warning title and message
 * @returns the warning to display on the toolbar
 */
function toToolbarWarning(
  warning: CollaborationWarning,
  translate: Translate,
): ToolbarWarning {
  const key = `realtime.${warning}Warning`;
  return {
    id: warning,
    title: translate(key),
    message: translate(`${key}.message`),
  };
}

/**
 * Creates the toolbar, making sure the edit form doesn't have one already.
 */
async function createToolbar(
  options: YjsToolbarOptions,
): Promise<[Toolbar, Translate]> {
  const { form } = options;
  if (formsWithToolbar.has(form)) {
    throw new Error("The edit form already has a realtime toolbar.");
  }
  formsWithToolbar.add(form);
  try {
    return await loadToolbar(options);
  } catch (error) {
    formsWithToolbar.delete(form);
    throw error;
  }
}

async function loadToolbar(
  options: YjsToolbarOptions,
): Promise<[Toolbar, Translate]> {
  const [environment, i18n] = await Promise.all([
    createXWikiToolbarEnvironment(),
    loadXWikiToolbarTranslations(),
  ]);
  const toolbar = new Toolbar({
    form: options.form,
    document: options.document,
    session: {
      save: (continueEditing) =>
        options.saver.save({
          action: continueEditing ? "saveAndContinue" : "saveAndView",
        }),
      // The document is saved over HTTP, independently of the collaboration server, and the saver is elected locally
      // when we're alone (which is the case while disconnected).
      canSaveWhileDisconnected: true,
    },
    environment,
    i18n,
  });
  // The translation function has a different signature depending on the vue-i18n mode (legacy or composition), but we
  // only use the common subset.
  const translate = i18n.global.t as Translate;
  return [toolbar, (key) => translate(key)];
}

/**
 * Keeps the toolbar in sync with the collaboration session and the auto-saver.
 *
 * @returns a function that stops the synchronization
 */
function connectToolbar(
  toolbar: Toolbar,
  translate: Translate,
  { collaboration, saver }: YjsToolbarOptions,
): () => void {
  const stopWatchers = [
    watch(
      collaboration.connectionStatus,
      (status) =>
        toolbar.onConnectionStatusChange(
          status as ToolbarStatus,
          collaboration.collaborator.id,
        ),
      { immediate: true },
    ),
    watch(
      collaboration.collaborators,
      (collaborators) =>
        toolbar.onUserListChange(collaborators.map(toToolbarUser)),
      { immediate: true },
    ),
    watch(
      () => collaboration.warnings?.value ?? [],
      (warnings, previousWarnings = []) => {
        previousWarnings
          .filter((warning) => !warnings.includes(warning))
          .forEach((warning) => toolbar.removeWarning(warning));
        warnings
          .filter((warning) => !previousWarnings.includes(warning))
          .forEach((warning) =>
            toolbar.addWarning(toToolbarWarning(warning, translate)),
          );
      },
      { immediate: true },
    ),
  ];

  // The save status values match the toolbar status values (unsaved, saving, saved).
  const onSaveStatusChange = (status: number) =>
    toolbar.onSaveStatusChange(status as ToolbarStatus);
  const saveStatus = saver.getStatus();
  if (saveStatus !== undefined) {
    onSaveStatusChange(saveStatus);
  }
  const removeSaveStatusListener = saver.addStatusListener(onSaveStatusChange);

  return () => {
    stopWatchers.forEach((stop) => stop());
    removeSaveStatusListener();
  };
}

/**
 * Creates the realtime toolbar of a Yjs collaboration session, in place of the standard edit toolbar of the given edit
 * form, and keeps it in sync with the session. The toolbar doesn't allow leaving the session nor selecting a user.
 *
 * @param options - the edit form, the edited document and the collaboration session
 * @returns the created toolbar
 * @since 18.9.0RC1
 * @beta
 */
async function createYjsToolbar(
  options: YjsToolbarOptions,
): Promise<YjsToolbar> {
  const { form, collaboration } = options;
  const [toolbar, translate] = await createToolbar(options);
  const disconnect = connectToolbar(toolbar, translate, options);

  const onCreateVersion = (version: DocumentVersion) =>
    toolbar.onCreateVersion(
      toToolbarVersion(version, collaboration.collaborators.value),
    );
  options.versions?.forEach(onCreateVersion);

  return {
    onCreateVersion,
    destroy() {
      disconnect();
      toolbar.destroy();
      formsWithToolbar.delete(form);
    },
  };
}

export { createYjsToolbar };
export type { YjsToolbar, YjsToolbarOptions };
