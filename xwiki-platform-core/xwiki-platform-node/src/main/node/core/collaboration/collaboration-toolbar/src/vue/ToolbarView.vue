<!--
  See the NOTICE file distributed with this work for additional
  information regarding copyright ownership.

  This is free software; you can redistribute it and/or modify it
  under the terms of the GNU Lesser General Public License as
  published by the Free Software Foundation; either version 2.1 of
  the License, or (at your option) any later version.

  This software is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public
  License along with this software; if not, write to the Free
  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  02110-1301 USA, or see the FSF site: http://www.fsf.org.
-->
<script setup lang="ts">
import ChangeSummaryModal from "./ChangeSummaryModal.vue";
import LeaveModal from "./LeaveModal.vue";
import StatusLabel from "./StatusLabel.vue";
import UserAvatar from "./UserAvatar.vue";
import VersionModal from "./VersionModal.vue";
import WarningPopover from "./WarningPopover.vue";
import {
  useDocument,
  useEnvironment,
  useForm,
  useSession,
  useState,
  useToolbarElement,
} from "./context";
import { computed, ref } from "vue";
import { useI18n } from "vue-i18n";
import type {
  ChangeSummary,
  Changes,
  ToolbarUser,
  ToolbarVersion,
} from "./types";

const USER_LIMIT = 4;

const environment = useEnvironment();
const form = useForm();
const xwikiDocument = useDocument();
const session = useSession();
const state = useState();
const toolbar = useToolbarElement();
const { t } = useI18n();

// Read once because the edit form fields that determine the save options are not expected to change while editing.
const saveOptions = environment.getSaveOptions(form);

const selectedVersion = ref<ToolbarVersion>();
const selectedVersionContentURL = computed(
  () =>
    selectedVersion.value &&
    environment.getVersionContentURL(
      xwikiDocument,
      selectedVersion.value.number,
    ),
);
const selectedVersionDate = computed(
  () =>
    selectedVersion.value && environment.formatDate(selectedVersion.value.date),
);

// The users are plain links to their profile pages when the session doesn't handle the user selection.
const selectUser = session.selectUser
  ? (user: Partial<ToolbarUser>) => {
      if (user.id !== undefined) {
        session.selectUser!(user.id);
      }
    }
  : undefined;

// The Done button opens the change summary modal, instead of saving directly, when the change summary is mandatory.
const doneOpensModal = computed(
  () => saveOptions.editComment.enabled && saveOptions.editComment.mandatory,
);
const savingOrDisconnected = computed(
  () =>
    state.saveStatus === 1 ||
    (state.connectionStatus !== 2 && !session.canSaveWhileDisconnected),
);
const visibleUsers = computed(() => state.users.slice(0, USER_LIMIT));
const hiddenUserCount = computed(() => state.users.length - USER_LIMIT);

// The input of the change summary modal, set when the modal is opened.
const summaryContinueEditing = ref(false);
const minorEditAllowed = ref(false);
let reviewedVersion: string | undefined;

/**
 * Prepares the change summary modal before it is opened. This is called by the modal triggers before the Bootstrap
 * click handler that opens the modal.
 *
 * @param continueEditing - whether to remain in edit mode after saving or not
 */
function prepareChangeSummary(continueEditing: boolean) {
  summaryContinueEditing.value = continueEditing;
  // The minor change option is not available for new documents.
  minorEditAllowed.value = saveOptions.minorEdit && !xwikiDocument.isNew;
  reviewedVersion = state.lastReviewedVersion;
}

async function loadChanges(): Promise<Changes> {
  if (xwikiDocument.isNew) {
    return { status: "isNew" };
  }
  const wrapper = document.createElement("div");
  wrapper.innerHTML = await environment.fetchChanges(
    form,
    xwikiDocument,
    reviewedVersion,
    toolbar,
  );
  const diff = wrapper.querySelector(".diff-group")?.innerHTML;
  return diff === undefined ? { status: "empty" } : { status: "loaded", diff };
}

async function saveWithChangeSummary({ summary, minorEdit }: ChangeSummary) {
  environment.setChangeSummary(form, summary, minorEdit);
  const lastReviewedVersion = state.lastReviewedVersion;
  // The next version created is the one that includes the changes the user has reviewed.
  delete state.lastReviewedVersion;
  if (!(await save(summaryContinueEditing.value))) {
    state.lastReviewedVersion = lastReviewedVersion;
  }
}

async function done(event: MouseEvent) {
  if (doneOpensModal.value) {
    prepareChangeSummary(false);
    return;
  }
  event.preventDefault();
  // We can't rely on the save status to determine whether to save or cancel (e.g. to prevent creating a new version
  // when there are no changes and the document is not new) because the save status takes into account only the fields
  // that are synchronized in realtime and the document title, for instance, is not synchronized. So when someone
  // changes the title the others don't know that the document is dirty and thus has to be saved.
  await save();
}

/**
 * @param continueEditing - whether to remain in edit mode after saving or not
 * @returns whether the save succeeded
 */
async function save(continueEditing?: boolean): Promise<boolean> {
  try {
    await session.save(continueEditing);
    return true;
  } catch (error) {
    // The failure is already logged by the saver and reported to the user by the save notification. We only need to
    // catch it in order to avoid an unhandled promise rejection.
    console.debug("Failed to save.", error);
    return false;
  }
}
</script>

<template>
  <div class="realtime-edit-toolbar-left">
    <input type="hidden" name="preventEmptyRevision" value="true" />
    <div class="btn-group dropup">
      <button
        type="button"
        class="btn btn-primary realtime-action-done"
        :title="
          t(
            doneOpensModal
              ? 'realtime.summarizeAndDone.hint'
              : 'realtime.done.hint',
          )
        "
        :disabled="!doneOpensModal && savingOrDisconnected"
        v-bind="
          doneOpensModal
            ? {
                'data-continue': 'false',
                'data-toggle': 'modal',
                'data-target': '#realtime-changeSummaryModal',
              }
            : {}
        "
        @click="done"
      >
        <span v-html="environment.icons.check"></span>
        {{ t("realtime.done") }}
      </button>
      <button
        type="button"
        class="btn btn-primary dropdown-toggle"
        data-toggle="dropdown"
        aria-haspopup="true"
        aria-expanded="false"
      >
        <span class="caret"></span>
        <span class="sr-only">{{ t("core.menu.toggleDropdown") }}</span>
      </button>
      <ul class="dropdown-menu">
        <!-- When the change summary is mandatory the Done button opens the change summary modal. -->
        <li v-if="saveOptions.editComment.enabled && !doneOpensModal">
          <a
            href="#"
            class="realtime-action-summarize"
            :title="t('realtime.summarizeAndDone.hint')"
            data-continue="false"
            data-toggle="modal"
            data-target="#realtime-changeSummaryModal"
            @click="prepareChangeSummary(false)"
            >{{ t("realtime.summarizeAndDone") }}</a
          >
        </li>
        <li v-if="session.leave">
          <a
            href="#"
            class="realtime-action-leave"
            :title="t('realtime.leave.hint')"
            data-toggle="modal"
            data-target="#realtime-leave-modal"
            >{{ t("realtime.leave") }}</a
          >
        </li>
      </ul>
    </div>
    <StatusLabel
      class="realtime-connection-status"
      :status="state.connectionStatus"
      :labels="[t('realtime.disconnected'), t('realtime.connecting')]"
    />
    <ul class="realtime-users">
      <li v-for="user in visibleUsers" :key="user.id">
        <UserAvatar :user="user" compact @select="selectUser" />
      </li>
    </ul>
    <div
      class="btn-group dropup realtime-users-dropdown"
      :hidden="hiddenUserCount <= 0"
    >
      <button
        type="button"
        class="btn dropdown-toggle realtime-action-users badge"
        :title="t('realtime.users.hint')"
        data-toggle="dropdown"
        aria-haspopup="true"
        aria-expanded="false"
        :data-more="Math.max(hiddenUserCount, 0)"
      >
        <span class="sr-only">{{ t("realtime.users") }}</span>
      </button>
      <ul class="dropdown-menu">
        <li v-for="user in state.users" :key="user.id">
          <UserAvatar :user="user" @select="selectUser" />
        </li>
      </ul>
    </div>
    <!-- The warning is re-created when its content changes, in order to update and show again the popover. -->
    <WarningPopover
      v-for="warning in state.warnings"
      :key="JSON.stringify(warning)"
      :warning="warning"
    />
    <!-- Other modules can append elements here (e.g. the concurrent editing warning). -->
  </div>
  <div class="realtime-edit-toolbar-right">
    <StatusLabel
      class="realtime-save-status"
      :status="state.saveStatus"
      :labels="[
        t('realtime.saveStatus.unsaved'),
        t('realtime.saveStatus.saving'),
        t('realtime.saveStatus.saved'),
      ]"
    />
    <div class="btn-group dropup">
      <button
        type="button"
        class="btn btn-default dropdown-toggle realtime-action-history"
        :title="t('realtime.history.hint')"
        data-toggle="dropdown"
        aria-haspopup="true"
        aria-expanded="false"
      >
        <span v-html="environment.icons.clock"></span>
        <span class="sr-only">{{ t("realtime.history") }}</span>
      </button>
      <ul class="dropdown-menu dropdown-menu-right realtime-versions">
        <li class="dropdown-header">{{ t("realtime.history.hint") }}</li>
        <li v-for="version in state.versions" :key="version.number">
          <a
            :href="environment.getVersionURL(xwikiDocument, version.number)"
            class="realtime-version"
            data-toggle="modal"
            data-target="#realtime-version-modal"
            data-remote="false"
            :data-version="JSON.stringify(version)"
            @click.prevent="selectedVersion = version"
          >
            <UserAvatar
              v-if="
                typeof version.author === 'object' && version.author.reference
              "
              :user="{ ...version.author, reference: version.author.reference }"
              :link="false"
              compact
            />
            <span
              v-else
              class="realtime-version-icon"
              v-html="environment.icons.clock"
            ></span>
            <span class="realtime-version-info">
              <span class="realtime-version-number">{{ version.number }}</span>
              <span class="realtime-version-date">{{
                environment.formatDate(version.date)
              }}</span>
            </span>
          </a>
        </li>
        <li role="separator" class="divider"></li>
        <li v-if="saveOptions.editComment.enabled">
          <a
            href="#"
            class="realtime-action-summarize"
            :title="t('realtime.summarize.hint')"
            data-continue="true"
            data-toggle="modal"
            data-target="#realtime-changeSummaryModal"
            @click="prepareChangeSummary(true)"
            >{{ t("realtime.summarize") }}</a
          >
        </li>
      </ul>
    </div>
  </div>
  <Teleport to="body">
    <ChangeSummaryModal
      v-if="saveOptions.editComment.enabled"
      :continue-editing="summaryContinueEditing"
      :summary-required="saveOptions.editComment.mandatory"
      :minor-edit-allowed="minorEditAllowed"
      :disabled="savingOrDisconnected"
      :load-changes="loadChanges"
      @submit="saveWithChangeSummary"
    />
    <VersionModal
      :version="selectedVersion"
      :content-url="selectedVersionContentURL"
      :date="selectedVersionDate"
    />
    <LeaveModal v-if="session.leave" @submit="session.leave()" />
  </Teleport>
</template>

<style>
.realtime-edit-toolbar {
  display: flex;
  justify-content: space-between;
}

.realtime-edit-toolbar-left,
.realtime-edit-toolbar-right {
  display: flex;
  align-items: center;
}

.realtime-edit-toolbar .btn-group > .btn-primary:not(:first-child) {
  /* Fix the border that separates the primary button from the dropdown toggle. */
  border-left-color: color-mix(in srgb, var(--btn-primary-bg), black 5%);
}

/* Hide the user list and user dropdown when we are disconnected and while the connection is being established. */
.realtime-connection-status[data-status="dirty"] + .realtime-users,
.realtime-connection-status[data-status="dirty"] ~ .realtime-users-dropdown,
.realtime-connection-status[data-status="cleaning"] + .realtime-users,
.realtime-connection-status[data-status="cleaning"] ~ .realtime-users-dropdown,
.realtime-connection-status[data-status="clean"],
.realtime-users-dropdown[hidden] {
  display: none;
}

ul.realtime-users {
  list-style: none;
  display: flex;
  margin: 0;
  padding: 0;
}

.realtime-connection-status + .realtime-users {
  margin-left: 0.5em;
}

.realtime-users + .realtime-users-dropdown {
  margin: 4px;
}

.realtime-users-dropdown > .dropdown-toggle {
  padding: 4px 3px;
  font-size: 10px;
  font-family: monospace;
}

.realtime-users-dropdown > .dropdown-toggle:hover,
.realtime-users-dropdown > .dropdown-toggle:focus {
  color: #fff;
  background-color: color-mix(in srgb, var(--text-muted), black 30%);
}

.realtime-users-dropdown > .dropdown-toggle:focus {
  outline-offset: 3px;
  outline-style: auto;
  outline-width: thin;
  outline-color: color-mix(in srgb, var(--text-muted), black 30%);
}

.realtime-users-dropdown > .dropdown-toggle::after {
  content: "+" attr(data-more);
}

.dropdown-menu > li > a.realtime-version {
  display: flex;
  align-items: center;
}

.realtime-version > :first-child {
  flex-basis: 32px;
  flex-shrink: 0;
  text-align: center;
}

.realtime-version-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding: 0 20px 0 10px;
}

.realtime-version .realtime-version-number {
  font-weight: bolder;
}

.realtime-version .realtime-version-date {
  font-size: 85%;
  color: var(--text-muted);
}

/* Hide the divider when the summarize action is not available. */
ul.realtime-versions > li.divider:last-child {
  display: none;
}
</style>
