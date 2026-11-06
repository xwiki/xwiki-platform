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

import { DefaultLogger } from "@xwiki/platform-api";
import { Saver } from "@xwiki/platform-autosave-api";
import { XWikiFormSaveTarget } from "@xwiki/platform-autosave-xwiki";
import { YjsAwarenessSaveTransport } from "@xwiki/platform-collaboration-api";
import { toCristalEntityReference } from "@xwiki/platform-model-xwiki";
import type {
  DocumentVersion,
  XWikiFormSaveContext,
} from "@xwiki/platform-autosave-xwiki";
import type {
  Collaboration,
  CollaborationManager,
} from "@xwiki/platform-collaboration-api";
import type { YjsToolbar } from "@xwiki/platform-collaboration-toolbar";
import type { XWikiDocument } from "@xwiki/platform-document-xwiki";
import type { DocumentReference } from "@xwiki/platform-model-api";

/**
 * The maximum number of versions kept for the realtime toolbar, which displays only the most recent ones.
 */
const VERSION_LIMIT = 5;

/**
 * What is needed to edit an XWiki document in realtime, using an XWiki edit form.
 *
 * @since 18.9.0RC1
 * @beta
 */
type FormCollaborationOptions = {
  /**
   * Used to join and leave the collaboration session.
   */
  collaborationManager: CollaborationManager;

  /**
   * The edited document, which identifies the collaboration session (document reference and locale). It is kept up to
   * date with the versions the collaborators create so that the next save doesn't run into a merge conflict.
   */
  document: XWikiDocument;

  /**
   * The identifier of the edit form to submit when saving. Defaults to `edit`, the form of the standalone edit mode.
   */
  formId?: string;

  /**
   * The version summary to record for an auto-save, already translated.
   */
  autoSaveVersionSummary?: string;
};

/**
 * A realtime collaboration session joined to edit an XWiki document using an XWiki edit form.
 *
 * @since 18.9.0RC1
 * @beta
 */
type FormCollaboration = {
  /**
   * The joined collaboration session.
   */
  collaboration: Collaboration;

  /**
   * Starts the auto-save of the collaboration session and shows the realtime toolbar in place of the standard edit
   * toolbar of the edit form. Only the first call for a given collaboration session has an effect, because the
   * auto-saver and the toolbar are shared by everyone editing in that session from the current page. Call it once the
   * editor is ready, because the first client joining a session loads the initial content into the shared document,
   * which is a local change that would otherwise mark the session as dirty.
   */
  startAutoSave(): void;

  /**
   * Leaves the collaboration session. The realtime toolbar is removed and the auto-save is stopped when the last
   * editor leaves.
   */
  leave(): void;
};

/**
 * What the editors taking part in the same collaboration session, from the current page, share.
 */
type SessionState = {
  /**
   * How many editors have joined the collaboration session and haven't left it yet.
   */
  usageCount: number;

  /**
   * The most recent versions reported by the save target, oldest first, replayed when the toolbar is created.
   */
  versions: DocumentVersion[];

  /**
   * The realtime toolbar, once created.
   */
  toolbar?: YjsToolbar;
};

const sessions = new WeakMap<Collaboration, SessionState>();

/**
 * @param document - the edited document
 * @returns the reference identifying the collaboration session, i.e. the document reference with the document locale,
 *   or undefined to use the current document
 */
function getCollaborationReference(
  document: XWikiDocument,
): DocumentReference | undefined {
  const reference = toCristalEntityReference(document.documentReference) as
    | DocumentReference
    | undefined;
  if (reference) {
    reference.locale = document.language;
  }
  return reference;
}

function createAutoSaver(
  options: FormCollaborationOptions,
  collaboration: Collaboration,
  session: SessionState,
): Saver<XWikiFormSaveContext> {
  // Instantiate the logger rather than resolving the Logger role from the component manager, because the default
  // implementation is bound as a singleton and setting the module name on it would relabel the messages of every
  // other component as coming from the auto-save.
  const logger = new DefaultLogger();
  logger.setModule("collaboration.autosave");

  const onCreateVersion = (version: DocumentVersion) => {
    session.versions.push(version);
    if (session.versions.length > VERSION_LIMIT) {
      session.versions.shift();
    }
    session.toolbar?.onCreateVersion(version);
  };

  return new Saver<XWikiFormSaveContext>(
    { logger },
    (saver) => new YjsAwarenessSaveTransport(saver, { collaboration, logger }),
    (saver) =>
      new XWikiFormSaveTarget(saver, {
        document: options.document,
        formId: options.formId,
        autoSaveVersionSummary: options.autoSaveVersionSummary,
        onCreateVersion,
        logger,
      }),
  );
}

async function createToolbar(
  options: FormCollaborationOptions,
  collaboration: Collaboration,
  session: SessionState,
  saver: Saver<XWikiFormSaveContext>,
): Promise<void> {
  const form = document.getElementById(options.formId ?? "edit");
  if (!form) {
    throw new Error("Failed to find the edit form.");
  }
  // The toolbar is loaded on demand, through the import map, so that it is shared with the other realtime editors.
  const { createYjsToolbar } = await import(
    "@xwiki/platform-collaboration-toolbar"
  );
  const toolbar = await createYjsToolbar({
    form,
    document: options.document,
    collaboration,
    saver,
    versions: session.versions,
  });
  if (session.usageCount > 0) {
    session.toolbar = toolbar;
  } else {
    // Everyone left the collaboration session while the toolbar was loading.
    toolbar.destroy();
  }
}

function startAutoSave(
  options: FormCollaborationOptions,
  collaboration: Collaboration,
  session: SessionState,
): void {
  if (collaboration.saver || session.usageCount <= 0) {
    return;
  }
  // Nothing is awaited between the test above and this assignment, which is what makes it safe against a second
  // editor starting the auto-save at the same time.
  const saver = createAutoSaver(options, collaboration, session);
  collaboration.saver = saver;
  void (async () => {
    try {
      await saver.toBeReady();
    } catch (error) {
      console.error("Failed to start the auto-save.", error);
    }
  })();
  void (async () => {
    try {
      await createToolbar(options, collaboration, session, saver);
    } catch (error) {
      console.error("Failed to create the realtime toolbar.", error);
    }
  })();
}

function leave(
  options: FormCollaborationOptions,
  reference: DocumentReference | undefined,
  session: SessionState,
): void {
  session.usageCount--;
  if (session.usageCount === 0) {
    // Remove the toolbar before the collaboration manager stops the auto-saver, because the toolbar listens to it.
    session.toolbar?.destroy();
    delete session.toolbar;
  }
  // The collaboration manager stops the auto-saver when the last editor leaves the session.
  options.collaborationManager.leave(reference);
}

/**
 * Joins the realtime collaboration session for editing the given XWiki document using an XWiki edit form. The
 * collaboration session is shared by all the editors that join it from the current page.
 *
 * @param options - the edited document, the edit form and the collaboration manager
 * @returns the joined collaboration session, once the connection is established and the initial synchronization is
 *   done, or once the collaboration manager gives up waiting for the connection (see
 *   {@link CollaborationManager.join})
 * @since 18.9.0RC1
 * @beta
 */
async function joinFormCollaboration(
  options: FormCollaborationOptions,
): Promise<FormCollaboration> {
  const reference = getCollaborationReference(options.document);
  const collaboration = await options.collaborationManager.join(reference);
  const session = sessions.get(collaboration) ?? {
    usageCount: 0,
    versions: [],
  };
  sessions.set(collaboration, session);
  session.usageCount++;

  let left = false;
  return {
    collaboration,
    startAutoSave: () => startAutoSave(options, collaboration, session),
    leave() {
      if (!left) {
        left = true;
        leave(options, reference, session);
      }
    },
  };
}

export { joinFormCollaboration };
export type { FormCollaboration, FormCollaborationOptions };
