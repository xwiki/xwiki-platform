<!--
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
-->

<template>
  <div class="footnotes">
    <div v-for="footnote in footnotes.list()"
         :key="`footnote-${footnote.prefix}-${footnote.translationKey}`"
         class="box infomessage footnote">
      (<small>{{ footnote.symbol }}</small>) {{ $t(footnote.translationKey) }}
    </div>
  </div>
</template>

<script>
export default {
  name: "LivedataFootnotes",
  inject: ["logic"],
  data() {
    return {
      footnotes: this.logic.footnotes
    }
  }
}
</script>

<style scoped>
.footnotes .footnote {
  padding: 0.1em 1em;
}
</style>