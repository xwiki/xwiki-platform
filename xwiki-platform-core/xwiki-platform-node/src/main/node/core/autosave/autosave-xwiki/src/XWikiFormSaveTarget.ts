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

import { SaveTarget } from "@xwiki/platform-autosave-api";
import { EntityType, Model } from "@xwiki/platform-xwiki-model-api";
import { loadById } from "@xwiki/platform-xwiki-utils";
import type {
  DocumentVersion,
  XWikiFormSaveAction,
  XWikiFormSaveContext,
} from "./types";
import type { Logger } from "@xwiki/platform-api";
import type {
  SaveResult,
  Saver,
  SaverState,
} from "@xwiki/platform-autosave-api";
import type { XWikiDocument } from "@xwiki/platform-document-xwiki";

/**
 * How long to wait for the result of a save request before giving up. This is a safety net for the case where
 * neither the save success nor the save failure event is fired, which would otherwise block the auto-save forever.
 * The value is well above any realistic save round-trip because on timeout the content remains dirty and is saved
 * again, so a shorter value would risk saving twice (creating an extra version) when the request is only slow.
 */
const SUBMIT_TIMEOUT = 120000;

/**
 * Calls the given function as if the given edit mode was still in use.
 *
 * @param editor - the edit mode to use during the call
 * @param callback - the function to call
 * @returns what the function returns
 */
function inEditMode<T>(editor: string | undefined, callback: () => T): T {
  const currentEditor = XWiki.editor;
  XWiki.editor = editor;
  try {
    return callback();
  } finally {
    // Restore the edit mode only if the callback didn't change it.
    if (XWiki.editor === editor) {
      XWiki.editor = currentEditor;
    }
  }
}

/**
 * The priority of each save in the save election. The manual saves come first. Save & View comes before Save &
 * Continue because it leaves the edit mode, so we want to make sure we don't lose unsaved changes, while the latter
 * keeps the user in the edit mode where the auto-save continues.
 */
const SAVE_PRIORITY: Record<XWikiFormSaveAction, number> = {
  saveAndView: 3,
  saveAndContinue: 2,
  autoSave: 1,
};

/**
 * How to save an XWiki document by submitting its edit form.
 *
 * @since 18.8.0RC1
 * @beta
 */
type XWikiFormSaveTargetConfig = {
  /**
   * The document being edited, kept up to date with the versions the other clients create so that the next save
   * doesn't run into a merge conflict.
   */
  document: XWikiDocument;

  /**
   * The identifier of the edit form to submit. Defaults to `edit`.
   */
  formId?: string;

  /**
   * The version summary to record for an auto-save. The caller supplies it already translated, which keeps this
   * module free of any localization dependency.
   */
  autoSaveVersionSummary?: string;

  /**
   * Called when a new version of the document exists, whoever created it.
   */
  onCreateVersion?: (version: DocumentVersion) => void;

  /**
   * Where to report what the target is doing. The caller passes the logger it resolved from the component manager,
   * with the module name it wants the messages attributed to.
   */
  logger?: Logger;
};

/**
 * Saves the content edited with an XWiki edit form, by submitting that form. It works with any transport, so the
 * same target serves both the ChainPad and the Yjs real-time sessions.
 *
 * The save interval can be overridden per edit form, by setting the number of seconds between two consecutive
 * saves as the `data-auto-save-interval` attribute of that form.
 *
 * @since 18.8.0RC1
 * @beta
 */
class XWikiFormSaveTarget extends SaveTarget<XWikiFormSaveContext> {
  private readonly document: XWikiDocument;

  private readonly formId: string;

  private readonly autoSaveVersionSummary?: string;

  private readonly onCreateVersion: (version: DocumentVersion) => void;

  private readonly logger?: Logger;

  private readonly revertList: (() => void)[] = [];

  /**
   * Set by {@link XWikiFormSaveTarget.initialize}, before any other method can run.
   */
  private $!: JQueryStatic;

  /**
   * @param saver - the saver using this target
   * @param config - how to save the edited document
   */
  public constructor(
    saver: Saver<XWikiFormSaveContext>,
    config: XWikiFormSaveTargetConfig,
  ) {
    super(saver);

    this.document = config.document;
    this.formId = config.formId ?? "edit";
    this.autoSaveVersionSummary = config.autoSaveVersionSummary;
    this.onCreateVersion = config.onCreateVersion ?? ((): void => {});
    this.logger = config.logger;
  }

  public override async initialize(): Promise<void> {
    // jQuery is needed only by this save target, so it's loaded on demand rather than being a dependency of the
    // entire module.
    this.$ = await loadById<JQueryStatic>("jquery");

    this.hidePreviewButton();
    this.overwriteAjaxSaveAndContinue();
    this.interceptSaveRequests();
    this.notifyInitialVersion();
  }

  /**
   * There's a very small chance that the preview button might cause problems, so let's just get rid of it.
   */
  private hidePreviewButton(): void {
    const $previewButton = this.$(this.getForm()).find(
      'input[name="action_preview"]',
    );
    if ($previewButton.is(":visible")) {
      $previewButton.hide();
      this.revertList.push(() => {
        $previewButton.show();
      });
    }
  }

  /**
   * Route the saves the user asks for through the saver, so that they take part in the save election.
   */
  private interceptSaveRequests(): void {
    const form = this.getForm();
    const beforeSaveHandler = async (
      event: JQuery.TriggeredEvent,
    ): Promise<void> => {
      if (!this.saver.isSaving()) {
        event.preventDefault();
        event.stopImmediatePropagation();
        const button = event.target as HTMLInputElement;
        try {
          await this.saver.save({
            action:
              button.name === "action_saveandcontinue"
                ? "saveAndContinue"
                : "saveAndView",
          });
        } catch {
          // The save failure is already logged by the saver and reported to the user by the save notification.
        }
      }
    };
    this.$(form).on(
      "xwiki:actions:beforeSave.realtime-saver",
      beforeSaveHandler,
    );
    this.revertList.push(() => {
      this.$(form).off(
        "xwiki:actions:beforeSave.realtime-saver",
        beforeSaveHandler,
      );
    });
  }

  public override getAutoSaveContext(): XWikiFormSaveContext {
    return { action: "autoSave" };
  }

  public override getSavePriority({ action }: XWikiFormSaveContext): number {
    return SAVE_PRIORITY[action];
  }

  public override async submit({
    action,
  }: XWikiFormSaveContext): Promise<SaveResult> {
    const saveButton = this.resolveSaveButton(action);
    const form = this.getForm();
    const removeListeners: (() => void)[] = [];
    const submitResultPromise = this.getSubmitResult(
      form,
      removeListeners,
      SUBMIT_TIMEOUT,
    );

    if (this.clickSaveButton(saveButton, action === "autoSave")) {
      // The save is prevented if the form has invalid data (e.g. missing mandatory title). In this case the
      // xwiki:document:saved and xwiki:document:saveFailed events are not triggered, so we need to remove the
      // corresponding event listeners and reject the save.
      removeListeners.forEach((removeListener) => removeListener());
      throw new Error("Save prevented. Verify that the form has valid data.");
    }

    return this.afterSave(await submitResultPromise);
  }

  /**
   * Refuse the save when it cannot go through, and pick the button performing it otherwise.
   *
   * @param action - what the save does
   * @returns the button to click
   */
  private resolveSaveButton(action: XWikiFormSaveAction): HTMLInputElement {
    // The merge conflict modal is already displayed (from a previous save attempt). Clicking the save button again
    // would reopen the same modal and reset the fields the user did not submit yet. We don't want that.
    if (this.$("#previewDiffModal").is(":visible")) {
      throw new Error("Merge conflict prevents save.");
    }

    // An auto-save keeps the user in the edit mode, so it goes through the save and continue button.
    const saveButton = this.getSaveButton(action !== "saveAndView");
    if (!saveButton?.matches(":enabled")) {
      throw new Error("The save button is disabled or missing.");
    }
    return saveButton;
  }

  public override onStatesChanged(
    states: Record<string, SaverState>,
    localClientId: string,
  ): void {
    const { latestVersion, savedBy } = this.getLatestVersion(states);
    if (savedBy && this.isNewVersion(latestVersion)) {
      this.document.update({
        version: latestVersion,
        modified: Date.now(),
        isNew: false,
      });
      if (savedBy !== localClientId) {
        this.onCreateVersion({
          number: latestVersion,
          date: this.document.modified!,
          author: savedBy,
        });
      }
    }
  }

  /**
   * @param states - the saver states of the clients
   * @returns the latest version saved by the clients, and the client that saved it (undefined when no client saved)
   */
  private getLatestVersion(states: Record<string, SaverState>): {
    latestVersion: string;
    savedBy?: string;
  } {
    let latestVersion = "0.0";
    let savedBy: string | undefined;
    for (const [clientId, state] of Object.entries(states)) {
      if (this.compareVersions(state.version ?? "0.0", latestVersion) > 0) {
        latestVersion = state.version!;
        savedBy = clientId;
      }
    }
    return { latestVersion, savedBy };
  }

  /**
   * @param version - a version saved by one of the clients
   * @returns whether the given version is a version of the edited document that we don't know about
   */
  private isNewVersion(version: string): boolean {
    const comparison = this.compareVersions(version, this.document.version!);
    // A new document already has the version its first save creates (1.1), so for a new document the same version
    // means that it was created.
    return comparison > 0 || (comparison === 0 && this.document.isNew === true);
  }

  public override dispose(): void {
    // Remove the event listeners and restore the action buttons behaviour.
    this.revertList.forEach((revert) => revert());
  }

  /**
   * @param continueEditing - whether the save should keep the user in the edit mode
   * @returns the button that performs that save
   */
  private getSaveButton(continueEditing: boolean): HTMLInputElement | null {
    return this.getForm().querySelector(
      `input[name="action_save${continueEditing ? "andcontinue" : ""}"]`,
    );
  }

  public override getSaveInterval(): number | undefined {
    // Read the value on each call rather than caching it, so that it can be set after the editor has loaded.
    const seconds = Number(this.getForm()?.dataset.autoSaveInterval);
    // Fall back on the default interval when the attribute is missing, empty, not a number or not positive.
    return Number.isFinite(seconds) && seconds > 0 ? seconds * 1000 : undefined;
  }

  private getForm(): HTMLFormElement {
    return document.getElementById(this.formId) as HTMLFormElement;
  }

  /**
   * Retrieve information about the initial version, when joining the editing session, without blocking the saver
   * ready state.
   */
  private notifyInitialVersion(): void {
    if (this.document.isNew) {
      return;
    }
    void this.loadInitialVersion();
  }

  private async loadInitialVersion(): Promise<void> {
    try {
      const revision = await this.document.getRevision(this.document.version!);
      this.onCreateVersion({
        number: revision.version,
        date: new Date(revision.modified).getTime(),
        author: {
          reference: this.getAbsoluteUserReference(revision.author),
          name: revision.authorName,
        },
      });
    } catch (error: unknown) {
      this.logger?.debug(
        "Failed to retrieve information about the initial version.",
        error,
      );
    }
  }

  /**
   * Prevent the save buttons from leaving the page, so that the editing session survives a save.
   */
  private overwriteAjaxSaveAndContinue(): void {
    const saver = this.saver;
    const logger = this.logger;
    const prototype = XWiki.actionButtons?.AjaxSaveAndContinue.prototype;
    if (!prototype) {
      return;
    }
    // Keep a reference to the methods we override, in order to call and later restore them.
    const original = {
      reloadEditor: prototype.reloadEditor,
      maybeRedirect: prototype.maybeRedirect,
    };
    const replacement = {
      // Prevent the save buttons from reloading the page. Instead, reset the editor's content.
      // FIXME: The in-place editor is also overriding reloadEditor, before this code is executed, so here we're
      // actually overwriting in-place editor's behavior.
      reloadEditor: (): void => {
        void this.document.reload();
        // HACK: Replicate the behavior from the in-place editor.
        setTimeout(() => {
          this.$(this.getForm()).trigger("xwiki:actions:reload");
        }, 0);
      },
      // Redirect only after we have confirmation that the saver state has been propagated to all clients.
      maybeRedirect: function (
        this: unknown,
        continueEditing: boolean,
      ): boolean {
        if (continueEditing) {
          // eslint-disable-next-line prefer-rest-params
          return original.maybeRedirect.apply(this, arguments as never);
        } else {
          // eslint-disable-next-line prefer-rest-params
          const args = arguments;
          // The in-place editor overrides this method in order to stay on the page, but only while it is in edit mode,
          // which it leaves right after the save, without waiting for the saver state to settle. Let it decide based on
          // the edit mode this method is called in, otherwise it would reload the page it already switched to view.
          const editor = XWiki.editor;
          void (async () => {
            try {
              await saver.whenSettled();
              inEditMode(editor, () =>
                original.maybeRedirect.apply(this, args as never),
              );
            } catch (error) {
              logger?.debug(
                "Failed to wait for the saver state to settle.",
                error,
              );
            }
          })();
          return true;
        }
      },
    };
    Object.assign(prototype, replacement);
    this.revertList.push(() => {
      // Revert only if the method has not been overridden by another script.
      for (const [methodName, method] of Object.entries(replacement)) {
        const key = methodName as keyof typeof replacement;
        if (prototype[key] === method) {
          Object.assign(prototype, { [key]: original[key] });
        }
      }
    });
  }

  /**
   * Click the save button, reporting whether the save was prevented, e.g. because the form has invalid data.
   */
  private clickSaveButton(
    button: HTMLInputElement,
    isAutoSave: boolean,
  ): boolean {
    let savePrevented = true;
    this.$(button).on("xwiki:actions:save.realtime-saver", (event) => {
      savePrevented = event.isDefaultPrevented();
    });

    const restoreVersionSummary =
      this.maybeSetAutoSaveVersionSummary(isAutoSave);
    button.click();
    this.$(button).off("xwiki:actions:save.realtime-saver");
    restoreVersionSummary?.();

    return savePrevented;
  }

  /**
   * @returns a function restoring the version summary the user had typed, when it was replaced
   */
  private maybeSetAutoSaveVersionSummary(
    isAutoSave: boolean,
  ): (() => void) | undefined {
    const commentInput = this.getForm().querySelector<HTMLInputElement>(
      'input[name="comment"]',
    );
    if (commentInput && isAutoSave && this.autoSaveVersionSummary) {
      // Backup the version summary before setting the auto-save value.
      const versionSummary = commentInput.value;
      commentInput.value = this.autoSaveVersionSummary;
      return () => {
        // Restore the version summary after the auto-save was triggered.
        commentInput.value = versionSummary;
      };
    }
    return undefined;
  }

  /**
   * @param form - the edit form that is being submitted
   * @param removeListeners - the list of functions to call in order to stop waiting for the result
   * @param timeout - how long to wait for the save result before rejecting; when not specified we wait indefinitely
   *   (e.g. while the user is dealing with the merge conflict modal)
   * @returns a promise that resolves with the save result or rejects if the save fails
   */
  private getSubmitResult(
    form: HTMLFormElement,
    removeListeners: (() => void)[],
    timeout?: number,
  ): Promise<{ newVersion?: string }> {
    return new Promise((resolve, reject) => {
      if (timeout) {
        const timer = setTimeout(() => {
          // Stop waiting for the save result, including for the events that are part of the same group.
          removeListeners.forEach((removeListener) => removeListener());
          reject(new Error("Timeout while waiting for the save result."));
        }, timeout);
        // Disarm the timer as soon as we receive the save result.
        removeListeners.push(() => clearTimeout(timer));
      }
      this.once(
        form,
        removeListeners,
        "xwiki:document:saved.realtime-saver",
        (event: JQuery.TriggeredEvent, data: unknown) => {
          resolve(data as { newVersion?: string });
        },
      );
      this.once(
        form,
        removeListeners,
        "xwiki:document:saveFailed.realtime-saver",
        (event: JQuery.TriggeredEvent, data: unknown) => {
          if (
            (data as { response: { status: number } }).response.status === 409
          ) {
            this.logger?.debug("Save blocked by merge conflict");
            // Keep the saving flag while the user deals with the merge conflict modal (i.e. we don't want the merge
            // conflict to be handled by multiple users because this leads to more merge conflicts).
            // The other listeners of the group are removed at this point, so the merge conflict resolution is the
            // only thing left that can settle the save result.
            resolve(this.waitForMergeConflictResolution(form));
          } else {
            reject(new Error("Failed to save."));
          }
        },
      );
    });
  }

  private async waitForMergeConflictResolution(
    form: HTMLFormElement,
  ): Promise<{ newVersion?: string }> {
    // There are multiple events that signal the merge conflict resolution. We want to wait for which one comes
    // first and then remove the other listeners. For this, we collect all the remove listener functions.
    const removeListeners: (() => void)[] = [];
    return Promise.race([
      // Wait for the document to be saved (after the merge conflict is resolved) or for the save to fail (which is
      // triggered also when the merge conflict modal fails to be fetched from the server).
      this.getSubmitResult(form, removeListeners),
      // ... or for the user to give up on the merge conflict resolution.
      this.waitForMergeConflictAbort(form, removeListeners),
    ]);
  }

  /**
   * Wait for the user to give up on the merge conflict resolution. The returned promise never resolves.
   *
   * @param form - the edit form
   * @param removeListeners - the listeners of the event group this wait takes part in
   */
  private async waitForMergeConflictAbort(
    form: HTMLFormElement,
    removeListeners: (() => void)[],
  ): Promise<never> {
    return new Promise((_resolve, reject) => {
      // Wait for the editor to be reloaded, if the user decides to discard the local changes.
      this.once(form, removeListeners, "xwiki:actions:reload", () => {
        reject(new Error("Discarding local changes by reloading the editor."));
      });
      // ... or for the merge conflict modal to be closed without resolving the conflict.
      this.once(
        document,
        removeListeners,
        "hide.bs.modal.realtime-saver",
        "#previewDiffModal",
        () => {
          if (this.$("#previewDiffModal").data("action") === "cancel") {
            reject(new Error("Save canceled."));
          } else {
            // The modal was closed but not canceled so we still need to wait for a save (successful or not) or
            // reload event. Keep the other event listeners in the group.
            return true;
          }
          return undefined;
        },
      );
    });
  }

  /**
   * Do something when any of the events from a group is triggered for the first time (once).
   *
   * @param target - the target element on which the event listener is registered
   * @param removeListeners - the list of event listeners to remove after an event from the group is triggered
   * @param args - the arguments passed when registering the event listener
   */
  private once(
    target: HTMLElement | Document,
    removeListeners: (() => void)[],
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    ...args: any[]
  ): void {
    // Wrap the original handler so that we can remove all the event listeners in the group after one of them is
    // triggered.
    const originalHandler = args.at(-1);
    args[args.length - 1] = (...params: unknown[]): unknown => {
      const result = originalHandler(...params);
      if (result !== true) {
        // Cleanup.
        removeListeners.forEach((removeListener) => removeListener());
      }
      return result;
    };
    const $target = this.$(target);
    $target.one(...(args as [string, () => void]));
    removeListeners.push(() => {
      $target.off(...(args as [string, () => void]));
    });
  }

  private afterSave({ newVersion }: { newVersion?: string }): SaveResult {
    if (newVersion === this.document.version && !this.document.isNew) {
      // The version didn't change because the document hasn't been modified. Note that a new document already has the
      // version its first save creates (1.1).
      return {};
    } else if (newVersion === "1.1") {
      this.logger?.debug("Created document version 1.1");
    } else {
      this.logger?.debug(
        `Version bumped from ${this.document.version} to ${newVersion}.`,
      );
    }
    this.onCreateVersion({
      number: newVersion!,
      date: Date.now(),
      author: this.saver.getClientId(),
    });
    return { version: newVersion };
  }

  private getAbsoluteUserReference(userReference: string): string {
    const usersSpaceReference = Model.resolve(
      "XWiki",
      EntityType.SPACE,
      this.document.documentReference,
    );
    return Model.serialize(
      Model.resolve(userReference, EntityType.DOCUMENT, usersSpaceReference),
    );
  }

  private compareVersions(a: string, b: string): number {
    const [aMajor, aMinor] = `${a}`.split(".").map(Number);
    const [bMajor, bMinor] = `${b}`.split(".").map(Number);
    return aMajor - bMajor || aMinor - bMinor;
  }
}

export { XWikiFormSaveTarget };
export type { XWikiFormSaveTargetConfig };
