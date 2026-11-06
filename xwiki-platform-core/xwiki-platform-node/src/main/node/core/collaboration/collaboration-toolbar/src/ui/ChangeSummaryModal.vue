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
import LoadingSpinner from "./LoadingSpinner.vue";
import { hideModal, useModalEvent } from "./bootstrap";
import { nextTick, ref, useTemplateRef } from "vue";
import { useI18n } from "vue-i18n";
import type { ChangeSummary, Changes } from "./types";

const props = defineProps<{
  /**
   * Whether the user wants to remain in edit mode after saving or not.
   */
  continueEditing: boolean;
  /**
   * Whether the change summary is mandatory when saving (creating a new revision).
   */
  summaryRequired: boolean;
  /**
   * Whether the user can mark the changes as minor.
   */
  minorEditAllowed: boolean;
  /**
   * Whether submitting the modal is disabled (e.g. while saving or while disconnected).
   */
  disabled: boolean;
  /**
   * Loads the changes to summarize, when the user selects the changes tab.
   */
  loadChanges: () => Promise<Changes>;
}>();

const emit = defineEmits<{
  /**
   * The user has provided a valid change summary. The modal is closed.
   */
  submit: [changeSummary: ChangeSummary];
}>();

type Tab = "summary" | "changes";
type ChangesState = "" | "loading" | Changes["status"] | "error";

const { t } = useI18n();

const modal = useTemplateRef("modal");
const summaryTextArea = useTemplateRef("summaryTextArea");
const minorChangeCheckbox = useTemplateRef("minorChangeCheckbox");

const activeTab = ref<Tab>("summary");
const changesState = ref<ChangesState>("");
const diff = ref("");

function selectTab(tab: Tab) {
  activeTab.value = tab;
  if (tab === "changes" && !changesState.value) {
    showChanges();
  }
}

async function showChanges() {
  changesState.value = "loading";
  try {
    const changes = await props.loadChanges();
    diff.value = changes.status === "loaded" ? changes.diff : "";
    changesState.value = changes.status;
  } catch (error) {
    changesState.value = "error";
    console.error(error);
  }
}

async function submit() {
  // Select the summary tab in order to be able to show validation errors, e.g. when the summary is mandatory.
  activeTab.value = "summary";
  await nextTick();
  if (!summaryTextArea.value!.reportValidity()) {
    // The provided change summary is not valid.
    return;
  }

  await hideModal(modal.value!);
  // We read the values from the form fields, rather than using v-model, because the fields can be modified without
  // firing input events (e.g. by the functional tests).
  emit("submit", {
    summary: summaryTextArea.value!.value,
    minorEdit: props.minorEditAllowed
      ? minorChangeCheckbox.value!.checked
      : undefined,
  });
}

useModalEvent(modal, "show", () => {
  activeTab.value = "summary";
  // Force a reload next time the user activates the changes tab.
  changesState.value = "";
});

useModalEvent(modal, "shown", () => {
  summaryTextArea.value!.focus();
  summaryTextArea.value!.select();
});
</script>

<template>
  <div
    id="realtime-changeSummaryModal"
    ref="modal"
    class="modal fade"
    tabindex="-1"
    role="dialog"
    :data-continue="continueEditing"
  >
    <div class="modal-dialog" role="document">
      <div class="modal-content">
        <div class="modal-header">
          <button
            type="button"
            class="close"
            data-dismiss="modal"
            :aria-label="t('realtime.close')"
          >
            <span aria-hidden="true">&times;</span>
          </button>
          <div class="modal-title">{{ t("realtime.summarize") }}</div>
        </div>
        <div class="modal-body xform">
          <ul class="nav nav-tabs" role="tablist">
            <li
              role="presentation"
              :class="{ active: activeTab === 'summary' }"
            >
              <a
                href="#realtime-changeSummaryModal-summaryTab"
                aria-controls="realtime-changeSummaryModal-summaryTab"
                role="tab"
                @click.prevent="selectTab('summary')"
                >{{ t("realtime.summary") }}</a
              >
            </li>
            <li
              role="presentation"
              :class="{ active: activeTab === 'changes' }"
            >
              <a
                href="#realtime-changeSummaryModal-changesTab"
                aria-controls="realtime-changeSummaryModal-changesTab"
                role="tab"
                @click.prevent="selectTab('changes')"
                >{{ t("realtime.changes") }}</a
              >
            </li>
          </ul>
          <div class="tab-content">
            <div
              id="realtime-changeSummaryModal-summaryTab"
              role="tabpanel"
              class="tab-pane"
              :class="{ active: activeTab === 'summary' }"
            >
              <dl>
                <dt>
                  <!-- The label is redundant with the tab name. -->
                  <label for="realtime-changeSummaryModal-summary" hidden>{{
                    t("realtime.summary")
                  }}</label>
                  <span class="xHint">
                    {{ t("core.comment.tooltip") }}
                    {{ t("core.validation.required") }}
                  </span>
                </dt>
                <dd>
                  <textarea
                    id="realtime-changeSummaryModal-summary"
                    ref="summaryTextArea"
                    name="summary"
                    maxlength="1023"
                    :required="summaryRequired"
                    rows="10"
                    :title="t('realtime.summary')"
                  ></textarea>
                </dd>
                <!-- Save & Continue always creates a minor change so we show the minor change checkbox only when the
                  action is Save & View. -->
                <template v-if="minorEditAllowed">
                  <dt
                    id="realtime-changeSummaryModal-minorChange"
                    data-continue="false"
                  >
                    <label>
                      <input
                        ref="minorChangeCheckbox"
                        type="checkbox"
                        name="minorChange"
                      />
                      {{ t("realtime.minorChange") }}
                    </label>
                    <span class="xHint">{{
                      t("realtime.minorChange.hint")
                    }}</span>
                  </dt>
                  <dd></dd>
                </template>
              </dl>
            </div>
            <div
              id="realtime-changeSummaryModal-changesTab"
              role="tabpanel"
              class="tab-pane"
              :class="{ active: activeTab === 'changes' }"
              :data-state="changesState"
            >
              <LoadingSpinner />
              <div class="diff-group" v-html="diff"></div>
              <div class="box infomessage noChange">
                {{ t("realtime.changes.empty") }}
              </div>
              <div class="box infomessage isNew">
                {{ t("realtime.changes.isnew") }}
              </div>
              <div class="box errormessage">
                {{ t("realtime.changes.error") }}
              </div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button
            type="button"
            class="btn btn-primary"
            :disabled="disabled"
            @click="submit"
          >
            {{ continueEditing ? t("save") : t("realtime.done") }}
          </button>
          <button type="button" class="btn btn-default" data-dismiss="modal">
            {{ t("cancel") }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style>
#realtime-changeSummaryModal .modal-body > .nav-tabs {
  margin-bottom: 1em;
}

#realtime-changeSummaryModal dl {
  margin-bottom: 0;
}

#realtime-changeSummaryModal[data-continue="false"] [data-continue="true"],
#realtime-changeSummaryModal[data-continue="true"] [data-continue="false"] {
  display: none;
}

#realtime-changeSummaryModal-changesTab > .realtime-spinner,
#realtime-changeSummaryModal-changesTab > .diff-group,
#realtime-changeSummaryModal-changesTab > .infomessage,
#realtime-changeSummaryModal-changesTab > .errormessage {
  display: none;
}

#realtime-changeSummaryModal-changesTab[data-state="loading"]
  > .realtime-spinner,
#realtime-changeSummaryModal-changesTab[data-state="loaded"] > .diff-group,
#realtime-changeSummaryModal-changesTab[data-state="empty"]
  > .infomessage.noChange,
#realtime-changeSummaryModal-changesTab[data-state="isNew"]
  > .infomessage.isNew,
#realtime-changeSummaryModal-changesTab[data-state="error"] > .errormessage {
  display: block;
}
</style>
