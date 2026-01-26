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

/**
 * The filterMixin is a vue mixin containing all the needed
 * props, computed values, methods, etc. for any custom filter:
 * `propertyId`, `index`, `filterEntry`, `config`, `applyFilter()`, ...
 * It should be included in every custom filter component
 */
export default {

  inject: ["logic"],

  props: {
    propertyId: String,
    index: Number,
  },

  data() {
    return {
      _applyFilterTimeoutId: undefined,
    };
  },

  // The computed values provide common data needed by filters
  computed: {
    // The filter group (the whole filter configuration) of `this.propertyId`
    filterGroup() {
      return this.logic.getQueryFilterGroup(this.propertyId) || {};
    },
    // The filter entry (the filter at `this.index`) of `this.propertyId`
    filterEntry() {
      return (this.filterGroup.constraints || [])[this.index] || {};
    },
    // The operator used, or default one if none specified
    operator() {
      return this.filterEntry.operator || this.logic.getFilterDefaultOperator(this.propertyId);
    },
    // The property descriptor of `this.propetyId`
    propertyDescriptor() {
      return this.logic.getPropertyDescriptor(this.propertyId);
    },
    // The configuration (aka filterDescriptor) of the filter
    config() {
      return this.logic.getFilterDescriptor(this.propertyId);
    },
    // The whole Livedata data object
    data() {
      return this.logic.data;
    },
  },

  methods: {
    // This method should be used to apply filter.
    // Since only the newValue has to be specified it is less error prone.
    /**
     * @param newValue the new filtering value
     * @param filterOperator the operator to apply, when undefined the default operator is used
     * @param skipFetch when true, the filter will be applied on the reactive variables, but will not trigger an
     * fetch. When undefined, the default value is false. This paramter is important in the case of asynchronous
     * methods where we need to have an instance feedback on the UI (e.g., between the advanced filtering panel and
     * the top filters in the table layout)
     */
    applyFilter: async function (newValue, filterOperator = undefined, skipFetch = false) {
      // Once a filter is applied, the filtering state is switched to true.
      // The filtering state is switched to false only once the filtering is finished.
      // The UI must not give visual clues when the fetching is not actually started.
      if (!skipFetch) {
        this.$emit("update:isFiltering", true);
      }
      try {
        this.logic.filter(this.propertyId, this.index, {value: newValue}, {filterOperator, skipFetch});
      } finally {
        // Whatever the filter promise result, the filtering state is switched to false.
        if (!skipFetch) {
          this.$emit("update:isFiltering", false);
        }
      }
    },

    removeFilter: function() {
      this.$emit("update:isFiltering", true);
      this.logic.removeFilter(this.propertyId, this.index)
        .finally(() => {
          // Whatever the removeFilter promise result, the filtering state is switched to false.
          this.$emit("update:isFiltering", false);
        });
    },

    // Call applyFilter method, but using a delay
    // This can be used when we want to call the applyFilter method inside an input event
    applyFilterWithDelay(newValue) {
      // Clear existing timeout
      // The filter is applied without delay, but skips updating the rows.
      // The update is only performed after the configured 250ms.
      this.applyFilter(newValue, undefined, true);
      clearTimeout(this._applyFilterTimeoutId);
      // Set a 250 milliseconds timeout before calling applyFilter method
      const timeoutDelay = 250;
      this._applyFilterTimeoutId = setTimeout(() => {
        this.logic.updateEntries();
      }, timeoutDelay);
    },

    // This method is automatically called by the widget when the operator change
    // It allow to decide what to do with the current value,
    // according to the new chosen operator
    // and the rules defined in the data function of the widget
    _operatorChangeHandler(oldOperator, newOperator) {
      if (!this.rules) {
        return;
      }
      // We reverse the rules so that the last ones take precedence over the first ones
      this.rules.slice().reverse().some(rule => {
        // Transform everything to array
        if (!(rule.from instanceof Array)) {
          rule.from = [rule.from];
        }
        if (!(rule.to instanceof Array)) {
          rule.to = [rule.to];
        }
        // Try to see if rule matches
        if (!rule.from.includes(oldOperator)) {
          return;
        }
        if (!rule.to.includes(newOperator)) {
          return;
        }
        // Rule matches the `from` and `to` operator criterias
        const newValue = rule.getValue({
          oldValue: this.filterEntry.value,
          oldOperator,
          newOperator,
        });
        this.applyFilter(newValue);
      });
    },
  },

  created() {
    // Whenever the filter operator changes
    // Update the filter value according to the rules defined in the filter widget
    this.logic.onEventWhere("filter", {
      type: "modify",
      oldEntry: { property: this.propertyId, index: this.index },
    }, e => {
      if (e.detail.oldEntry.operator === e.detail.newEntry.operator) {
        return;
      }
      // We don't want the other filter widget to call the hanlder the same value
      e.stopImmediatePropagation();
      this._operatorChangeHandler(e.detail.oldEntry.operator, e.detail.newEntry.operator);
    });
  },

};
