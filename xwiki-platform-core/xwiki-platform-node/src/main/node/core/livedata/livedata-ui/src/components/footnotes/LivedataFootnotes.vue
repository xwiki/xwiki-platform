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
<script>
import XWikiIcon from "../utilities/XWikiIcon.vue";

export default {
  name: "LivedataFootnotes",
  components: { XWikiIcon },
  inject: ["logic"],
  data() {
    return {
      footnotes: this.logic.footnotes,
    };
  },
};
</script>

<template>
  <div class="footnotes">
    <div
      v-for="footnote in footnotes.list()"
      :key="`footnote-${footnote.prefix}-${footnote.translationKey}`"
      class="box infomessage footnote"
    >
      (<small>{{ footnote.symbol }}</small
      >) {{ $t(footnote.translationKey) }}
    </div>
    <div v-if="logic.isViewFrozen()" class="box infomessage footnote">
      {{ $t("livedata.footnotes.frozenEntries") }}
      <span
        tabindex="0"
        role="img"
        :title="$t('livedata.footnotes.frozenEntries.hint')"
        :aria-label="$t('livedata.footnotes.frozenEntries.hint')"
      >
        <XWikiIcon :icon-descriptor="{ name: 'info' }" />
      </span>
    </div>
  </div>
</template>

<style scoped>
.footnotes .footnote {
  padding: 0.1em 1em;
}
</style>
