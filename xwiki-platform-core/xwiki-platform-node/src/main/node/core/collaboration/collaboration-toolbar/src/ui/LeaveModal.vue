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
import { useModalEvent } from "./bootstrap";
import { useTemplateRef } from "vue";
import { useI18n } from "vue-i18n";

const emit = defineEmits<{
  /**
   * The user has confirmed leaving the realtime session. The modal is closed.
   */
  submit: [];
}>();

const { t } = useI18n();

const modal = useTemplateRef("modal");
const leaveButton = useTemplateRef("leaveButton");

// The autofocus HTML attribute has no effect in Bootstrap modals.
useModalEvent(modal, "shown", () => leaveButton.value!.focus());
</script>

<template>
  <div
    id="realtime-leave-modal"
    ref="modal"
    class="modal fade"
    tabindex="-1"
    role="dialog"
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
          <div class="modal-title">{{ t("realtime.leave") }}</div>
        </div>
        <div class="modal-body">{{ t("realtime.leave.confirmation") }}</div>
        <div class="modal-footer">
          <button
            ref="leaveButton"
            type="button"
            class="btn btn-primary"
            data-dismiss="modal"
            @click="emit('submit')"
          >
            {{ t("realtime.leave.accept") }}
          </button>
          <button type="button" class="btn btn-default" data-dismiss="modal">
            {{ t("cancel") }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
