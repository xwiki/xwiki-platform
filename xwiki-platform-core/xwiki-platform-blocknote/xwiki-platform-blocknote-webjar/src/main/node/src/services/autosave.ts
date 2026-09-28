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
import type { XWikiFormSaveContext } from "@xwiki/platform-autosave-xwiki";
import type { Collaboration } from "@xwiki/platform-collaboration-api";
import type { XWikiDocument } from "@xwiki/platform-document-xwiki";

/**
 * What the auto-saver needs to know about the content being edited.
 */
type AutoSaveConfig = {
  /**
   * The collaboration session whose clients share this auto-saver.
   */
  collaboration: Collaboration;

  /**
   * The document being edited, kept up to date with the versions the other clients create so that the next save
   * doesn't run into a merge conflict.
   */
  document: XWikiDocument;

  /**
   * The identifier of the edit form to submit. Left unset in standalone edit mode, where the save target falls back
   * to the standard edit form.
   */
  formId?: string;

  /**
   * The version summary to record for an auto-save, already translated.
   */
  autoSaveVersionSummary?: string;
};

/**
 * Create the auto-saver of a realtime collaboration session: the saver states travel over the Yjs awareness of the
 * session, and the content is saved by submitting the XWiki edit form, like for any other editor.
 *
 * Note that this is deliberately synchronous, because the caller stores the result on the collaboration session and
 * must be able to check that no other editor created one without an await in between. Wait for
 * {@link Saver.toBeReady} afterwards.
 *
 * @param config - what is being edited
 * @returns the created auto-saver, not ready yet
 */
function createAutoSaver(config: AutoSaveConfig): Saver<XWikiFormSaveContext> {
  // Instantiate the logger rather than resolving the Logger role from the component manager, because the default
  // implementation is bound as a singleton and setting the module name on it would relabel the messages of every
  // other component as coming from the auto-save.
  const logger = new DefaultLogger();
  logger.setModule("blocknote.autosave");

  return new Saver<XWikiFormSaveContext>(
    { logger },
    (saver) =>
      new YjsAwarenessSaveTransport(saver, {
        collaboration: config.collaboration,
        logger,
      }),
    (saver) =>
      new XWikiFormSaveTarget(saver, {
        document: config.document,
        formId: config.formId,
        autoSaveVersionSummary: config.autoSaveVersionSummary,
        logger,
      }),
  );
}

export { createAutoSaver };
export type { AutoSaveConfig };
