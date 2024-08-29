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

<!--
  LayoutCardsNewCard is a component for the Card layout that can be used
  to add new entries to the Livedata
-->
<template>

  <!--
    The component is formatted like a normal card
    but only contains an action to add a new one
  -->
  <div class="layout-cards-new-card">

    <a
      href="#"
      @click.prevent="logic.addEntry()"
    >
      <XWikiIcon :icon-descriptor="{name: 'add'}"/>
      <span class="add-entry-text">
        {{ $t('livedata.action.addEntry') }}
      </span>
    </a>

  </div>

</template>


<script>
import XWikiIcon from "../../utilities/XWikiIcon";
export default {

  name: "LayoutCardsNewCard",

  components: {XWikiIcon},

  inject: ["logic"],

  computed: {
    data () { return this.logic.data; },
  },

};
</script>


<style>

.layout-cards-new-card {
  display: inline-block;
  vertical-align: top;
  margin: 1rem;
  padding: 1rem;
  border: 1px solid lightgray;
  border-radius: 1rem;
}

.layout-cards-new-card a {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  text-decoration: none !important;
}

.layout-cards-new-card a .fa {
  display: inline-block;
  margin: 1rem 2rem;
}

.layout-cards-new-card a:hover .add-entry-text {
  text-decoration: underline;
}

/* for not IE11 */
@supports (display: grid) {

  .layout-cards-new-card {
    margin: 0;
  }

}

</style>
