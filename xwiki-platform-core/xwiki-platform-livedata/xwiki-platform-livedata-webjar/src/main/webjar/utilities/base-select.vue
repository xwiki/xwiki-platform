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
          :selected="selected"
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
        :key="option"
      >
        <a href="#"
          @click.prevent.stop="toggleSelect(option)">
          <slot
            name="option"
            :value="option"
            :checked="isSelected(option)"
          >
            {{ option }}
          </slot>
        </a>
      </li>

    </ul>

  </div>
</template>


<script>
/*
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
define([
  "Vue",
], function (
  Vue,
  filterMixin
) {

  Vue.component("base-select", {

    name: "base-select",

    template: template,

    model: {
      prop: "selected",
      event: "change",
    },

    props: {
      options: {
        type: Array,
        default: function () { return []; },
      },
      selected: {
        type: Array,
        default: function () { return []; },
      },
      multiple: {
        type: Boolean,
        default: false,
      },
      sort: {
        type: Boolean,
        default: false,
      }
    },

    methods: {
      isSelected: function (option) {
        return this.selected.indexOf(option) !== -1;
      },

      select: function (option) {
        if (this.options.indexOf(option) === -1) { return; }
        if (this.isSelected(option)) { return; }
        var _selected = this.selected.slice()
        _selected.push(option);
        this.formatSelected(_selected);
        this.$emit("change", _selected);
      },

      deselect: function (option) {
        if (this.options.indexOf(option) === -1) { return; }
        var index = this.selected.indexOf(option);
        if (index === -1) { return; }
        var _selected = this.selected.slice();
        _selected.splice(index, 1);
        this.formatSelected(_selected);
        this.$emit("change", _selected);
      },

      toggleSelect: function (option) {
        if (this.isSelected(option)) {
          this.deselect(option);
        } else {
          this.select(option);
        }
      },

      selectAll: function () {
        var self = this;
        this.$emit("change", this.options.slice());
      },

      selectNone: function () {
        this.$emit("change", []);
      },

      formatSelected: function (selected) {
        var self = this;
        if (this.sort) {
          selected.sort(function (a, b) {
            return self.options.indexOf(a) - self.options.indexOf(b);
          });
        }
        return selected;
      },

    },


  });

});
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
