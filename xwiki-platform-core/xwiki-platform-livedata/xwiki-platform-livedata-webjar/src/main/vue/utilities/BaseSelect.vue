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
  <div class="base-select dropdown">

    <button
      class="btn btn-default dropdown-toggle"
      type="button"
      data-toggle="dropdown" aria-haspopup="true" aria-expanded="true"
    >
      <span class="title">
        <slot
          name="title"
          :selected-values="selectedValues"
        >
          Select value
        </slot>
      </span>
      <span class="caret"></span>
    </button>

    <ul class="dropdown-menu">

      <template v-if="multiple">
        <li>
          <a href="#" @click.prevent.stop="selectAll">
            Select All
          </a>
          <a href="#" @click.prevent.stop="selectNone">
            Select None
          </a>
        </li>

        <li role="separator" class="divider"></li>
      </template>

      <li
        v-for="option in options"
        :key="option.value"
      >
        <a href="#"
          @click.prevent.stop="toggleSelect(option)">
          <slot
            name="option"
            :value="option.value"
            :label="option.label"
            :checked="isSelected(option)"
            :toggle="toggleSelect"
          >
            {{ option.label }}
          </slot>
        </a>
      </li>

    </ul>

  </div>
</template>


<script>
export default {

  name: "BaseSelect",

  model: {
    prop: "selected",
    event: "change",
  },

  props: {
    options: {
      type: Array,
      default () { return []; },
    },
    selectedValues: {
      type: Array,
      default () { return []; },
    },
    multiple: {
      type: Boolean,
      default: false,
    },
    sort: {
      type: Boolean,
      default: false,
    },
  },

  computed: {
    optionsValues () {
      return this.options.map(option => option.value);
    },

    selected () {
      return this.selectedValues.map(value => this.options.find(option => option.value === value));
    },
  },

  methods: {
    isValidOption (option) {
      return this.optionsValues.includes(option.value);
    },

    isSelected (option) {
      return this.selectedValues.includes(option.value);
    },

    select (option) {
      if (!this.isValidOption(option)) { return; }
      if (this.isSelected(option)) { return; }
      const _selected = this.multiple ? this.selectedValues.slice() : [];
      _selected.push(option.value);
      this.sortSelected(_selected);
      this.$emit("change", _selected);
    },

    deselect (option) {
      const index = this.selectedValues.indexOf(option.value);
      if (index === -1) { return; }
      const _selected = this.selectedValues.slice();
      _selected.splice(index, 1);
      this.sortSelected(_selected);
      this.$emit("change", _selected);
    },

    toggleSelect (option) {
      if (this.isSelected(option)) {
        this.deselect(option);
      } else {
        this.select(option);
      }
    },

    selectAll () {
      this.$emit("change", this.optionsValues);
    },

    selectNone () {
      this.$emit("change", []);
    },

    sortSelected (selected) {
      if (this.sort) {
        selected.sort((a, b) => this.optionsValues.indexOf(a) - this.optionsValues.indexOf(b));
      }
      return selected;
    },

  },


};
</script>


<style>

.base-select > .btn.btn-default,
.base-select > .btn.btn-default:active,
.base-select > .btn.btn-default:focus,
.base-select > .btn.btn-default:hover {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  margin: 0;

  color: #555555;
  background-image: unset;
  background-color: #fff;
  border: 1px solid #ccc;
  border-radius: 4px;
}

.base-select > .btn .caret {
  margin-left: 1rem;
}

.base-select .title {
  width: 100%;
  text-align: left;
}

.base-select ul.dropdown-menu {
  overflow: auto;
  min-width: 100%;
  max-height: 30rem;
}
.base-select li a {
  display: flex;
  flex-direction: row;
  justify-content: flex-start;
  align-items: center;
}

</style>
