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

<!--
  LayoutCardsCover is the cover displayed at the top of a card, when a cover property is selected.
  It displays the image held by the cover property, or, when there is no image, the text of a property,
  enlarged. The cover is read-only.
-->
<template>
  <div class="card-cover" :class="{ 'card-cover-text': !image }">
    <!-- The image is displayed once the size of the cover is known, to fetch it at the right size. -->
    <img
      v-if="image && isMeasured"
      :src="imageSource"
      :alt="image.alt"
      loading="lazy"
      @error="$emit('imageError')"
    />
    <div v-else-if="textPropertyId" class="card-cover-text-value">
      <LivedataDisplayer :property-id="textPropertyId" :entry="entry" />
    </div>
  </div>
</template>

<script>
import { getResizedImageSource } from "./cardsLayout.js";
import LivedataDisplayer from "../../displayers/LivedataDisplayer.vue";
import { createReadOnlyLogic } from "../readOnlyLogic.js";

export default {
  name: "LayoutCardsCover",

  components: {
    LivedataDisplayer,
  },

  inject: ["logic"],

  // Provide a read-only logic to the displayer, so that it doesn't offer to edit the cover.
  provide() {
    return {
      logic: createReadOnlyLogic(this.logic),
    };
  },

  props: {
    entry: {
      type: Object,
      required: true,
    },
    // The image to display (with its `src` and `alt`), undefined to display the text of a property instead.
    image: {
      type: Object,
      default: undefined,
    },
    // The id of the property whose text is displayed when there is no image.
    textPropertyId: {
      type: String,
      default: undefined,
    },
  },

  emits: ["imageError"],

  data() {
    return {
      isMeasured: false,
      // The width of the cover in device pixels, undefined when it can't be measured.
      width: undefined,
    };
  },

  computed: {
    // The image fetched at the size of the cover, when the server can resize it.
    imageSource() {
      return getResizedImageSource(this.image.src, this.width);
    },
  },

  mounted() {
    // The cards all have the same width, so it is measured once, when the cover is displayed.
    const width = this.$el.clientWidth * (window.devicePixelRatio || 1);
    this.width = width > 0 ? width : undefined;
    this.isMeasured = true;
  },
};
</script>

<style>
/* The cover has a fixed ratio, so that the cards keep the same size, with or without an image. */
.layout-cards .card-cover {
  aspect-ratio: 16 / 9;
  overflow: hidden;
  border-radius: var(--border-radius-base) var(--border-radius-base) 0 0;
}

.layout-cards .card-cover img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.layout-cards .card-cover-text {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  background-color: color-mix(in srgb, currentColor 6%, transparent);
}

/* Display the text enlarged, on at most 3 lines. */
.layout-cards .card-cover-text-value {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
  line-clamp: 3;
  overflow: hidden;
  font-size: 1.75em;
  font-weight: var(--font-weight-semibold);
  line-height: 1.25;
  text-align: center;
  word-break: break-word;
}

/* Display the value inline, instead of filling its container like in a table cell. */
.layout-cards .card-cover-text-value .livedata-displayer,
.layout-cards .card-cover-text-value .livedata-displayer > .view,
.layout-cards .card-cover-text-value .livedata-displayer > .view > div {
  display: inline;
  width: auto;
  height: auto;
  text-align: inherit;
}
</style>
