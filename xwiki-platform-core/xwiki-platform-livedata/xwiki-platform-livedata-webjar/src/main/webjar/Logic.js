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
  "xwiki-livedata",
  //"polyfills"
], function (
  Vue,
  XWikiLivedata
) {



  /**
   * ---------------------------------------------------------------
   * LAYOUT
   */


  class LogicLayout {

    constructor (logic) {
      this.logic = logic;
      this.currentId = undefined;
      this.change(this.logic.data.meta.defaultLayout);
    }


    /**
     * Return the list of layout ids
     * @returns {Array}
     */
    getIds () {
      return this.logic.data.meta.layouts.map(layoutDescriptor => layoutDescriptor.id);
    }


    /**
     * Return the layout descriptor corresponding to a layout id
     * @param {String} propertyId
     * @returns {Object}
     */
    getDescriptor (layoutId) {
      return this.logic.data.meta.layouts
        .find(layoutDescriptor => layoutDescriptor.id === layoutId);
    }


    /**
     * Load a layout, or default layout if none specified
     * @param {String} layoutId The id of the layout to load with requireJS
     * @returns {Promise}
     */
    change (layoutId) {
      console.log("LAYOUT.CHANGE", this);

      // bad layout
      if (!this.getDescriptor(layoutId)) {
        console.error("Layout of id `" + layoutId + "` does not have a descriptor");
        return;
      }
      // set layout
      const previousLayoutId = this.currentId;
      this.currentId = layoutId;
      // dispatch events
      this.logic.event.trigger("layoutChange", {
        layoutId: layoutId,
        previousLayoutId: previousLayoutId,
      });
    }

  };




    /**
     * ---------------------------------------------------------------
     * EVENTS
     */


    class LogicEvent {

      constructor (logic) {
        this.logic = logic;
      }

      /**
       * Send custom events
       * @param {String} eventName The name of the event, without the prefix "xwiki:livedata"
       * @param {Object} eventData The data associated with the event.
       *  The livedata object reference is automatically added
       */
      trigger (eventName, eventData) {
        // configure event
        const defaultData = {
          livedata: this.logic,
        };
        eventName = "xwiki:livedata:" + eventName;
        eventData = {
          bubbles: true,
          detail: Object.assign(defaultData, eventData),
        };
        const event = new CustomEvent(eventName, eventData);
        // dispatch event
        this.logic.element.dispatchEvent(event);
      }


      /**
       * Listen for custom events
       * @param {String} eventName The name of the event, without the prefix "xwiki:livedata"
       * @param {Function} callback Function to call we the event is triggered
       */
      on (eventName, callback) {
        eventName = "xwiki:livedata:" + eventName;
        this.logic.element.addEventListener(eventName, function (e) {
          callback(e.detail);
        });
      }


      /**
       * Listen for custom events, mathching certain conditions
       * @param {String} eventName The name of the event, without the prefix "xwiki:livedata"
       * @param {Object|Function} condition The condition to execute the callback
       *  if Object, values of object properties must match e.detail properties values
       *  if Function, the function must return true. e.detail is passed as argument
       * @param {Function} callback Function to call we the event is triggered
       */
      onWhere (eventName, condition, callback) {
        eventName = "xwiki:livedata:" + eventName;
        this.logic.element.addEventListener(eventName, function (e) {
          // Object check
          if (typeof condition === "object") {
            const every = Object.keys(condition).every(key => condition[key] === e.detail[key]);
            if (!every) { return; }
          }
          // Function check
          if (typeof condition === "function") {
            if (!condition(e.detail)) { return; }
          }
          // call callback
          callback(e.detail);
        });
      }
    };





    /**
     * ---------------------------------------------------------------
     * ENTRIES
     */

    class LogicEntries {

      constructor (logic) {
        this.logic = logic;
      }


      /**
       * Return the id of the given entry
       * @param {Object} entry
       * @returns {String}
       */
      getId (entry) {
        const idProperty = this.logic.data.meta.entryDescriptor.idProperty || "id";
        if (entry[idProperty] === undefined) {
          console.warn("Entry has no id (at property [" + idProperty + "]", entry);
          return;
        }
        return entry[idProperty];
      }


      fetch () {
        return new Promise(function (resolve, reject) {
          const err = new Error("Error while fetching entries");
          // TODO: FETCH ENTRIES FROM HERE
          reject(err);
        });
      }


      update () {
        return this.fetch()
          .then(entries => this.data.data.entries = entries)
          .catch(err => console.error(err));
      }


      add () {
        const mockNewUrl = () => this.logic.entries.getId(this.logic.data.data.entries.slice(-1)[0]) + "0";
        // TODO: CALL FUNCTION TO CREATE NEW DATA HERE
        Promise.resolve({ /* MOCK DATA */
          "doc_url": mockNewUrl(),
          "doc_name": undefined,
          "doc_date": "2020/03/27 13:23",
          "doc_title": undefined,
          "doc_author": "Author 1",
          "doc_creationDate": "2020/03/27 13:21",
          "doc_creator": "Creator 1",
          "age": undefined,
          "tags": undefined,
          "country": undefined,
          "other": undefined,
        })
        .then(newEntry => {
          this.logic.data.data.entries.push(newEntry);
          this.logic.data.data.count++; // TODO: remove when merging with backend
        });
      }

    };



    /**
     * ---------------------------------------------------------------
     * PAGINATION
     */

    class LogicPagination {

      constructor (logic) {
        this.logic = logic;
      }

      /**
       * Get total number of pages
       * @returns {Number}
       */
      getPageCount () {
        return Math.ceil(this.logic.data.data.count / this.logic.data.query.limit);
      }


      /**
       * Get the page corresponding to the specified entry (0-based index)
       * @param {Number} entryIndex The index of the entry. Uses current entry if undefined.
       * @returns {Number}
       */
      getPageIndex (entryIndex) {
        if (entryIndex === undefined) {
          entryIndex = this.logic.data.query.offset;
        }
        return Math.floor(entryIndex / this.logic.data.query.limit);
      }


      /**
       * Set page index (0-based index), then fetch new data
       * @param {Number} pageIndex
       * @returns {Promise}
       */
      setPageIndex (pageIndex) {
        return new Promise ((resolve, reject) => {
          if (pageIndex < 0 || pageIndex >= this.getPageCount()) { return void reject(); }
          const previousPageIndex = this.getPageIndex();
          this.logic.data.query.offset = this.getFirstIndexOfPage(pageIndex);
          this.logic.event.trigger("pageChange", {
            pageIndex: pageIndex,
            previousPageIndex: previousPageIndex,
          });
          // TODO: CALL FUNCTION TO FETCH NEW DATA HERE
          resolve();
        });
      }


      /**
       * Get the first entry index of the given page index
       * @param {Number} pageIndex The page index. Uses current page if undefined.
       * @returns {Number}
       */
      getFirstIndexOfPage (pageIndex) {
        if (pageIndex === undefined) {
          pageIndex = this.getPageIndex();
        }
        if (0 <= pageIndex && pageIndex < this.getPageCount()) {
          return pageIndex * this.logic.data.query.limit;
        } else {
          return -1;
        }
      }


      /**
       * Get the last entry index of the given page index
       * @param {Number} pageIndex The page index. Uses current page if undefined.
       * @returns {Number}
       */
      getLastIndexOfPage (pageIndex) {
        if (pageIndex === undefined) {
          pageIndex = this.getPageIndex();
        }
        if (0 <= pageIndex && pageIndex < this.getPageCount()) {
          return Math.min(this.getFirstIndexOfPage(pageIndex) + this.logic.data.query.limit, this.logic.data.data.count) - 1;
        } else {
          return -1;
        }
      }


      /**
       * Set the pagination page size, then fetch new data
       * @param {Number} pageSize
       * @returns {Promise}
       */
      setPageSize (pageSize) {
        return new Promise ((resolve, reject) => {
          if (pageSize < 0) { return void reject(); }
          const previousPageSize = this.logic.data.query.limit;
          if (pageSize === previousPageSize) { return void resolve(); }
          this.logic.data.query.limit = pageSize;
          this.logic.event.trigger("pageSizeChange", {
            pageSize: pageSize,
            previousPageSize: previousPageSize,
          });
          // TODO: CALL FUNCTION TO FETCH NEW DATA HERE
          resolve();
        });
      }

    };




    /**
     * ---------------------------------------------------------------
     * PROPERTIES
     */

    class LogicProperties {

      constructor (logic) {
        this.logic = logic;
      }

      /**
       * Return whether the specified property id is valid (i.e. the property has a descriptor)
       * @param {String} propertyId
       */
      isIdValid (propertyId) {
        return this.logic.data.query.properties.includes(propertyId);
      }


      /**
       * Return whether the specified property type is valid (i.e. there is a type descriptor in meta)
       * @param {String} propertyType
       */
      isTypeValid (propertyType) {
        return this.logic.data.meta.propertyTypes
          .find(propertyTypeDescriptor => propertyTypeDescriptor.id === propertyType);
      }



      /**
       * Returns the property descriptors of displayable properties
       * @returns {Array}
       */
      getDescriptors () {
        return this.logic.data.query.properties
          .map(propertyId => this.logic.properties.getPropertyDescriptor(propertyId));
      }


      /**
       * Return the property descriptor corresponding to a property id
       * @param {String} propertyId
       * @returns {Object}
       */
      getPropertyDescriptor (propertyId) {
        const propertyDescriptor = this.logic.data.meta.propertyDescriptors
          .find(propertyDescriptor => propertyDescriptor.id === propertyId);
        if (!propertyDescriptor) {
          console.error("Property descriptor of property `" + propertyId + "` does not exists");
        }
        return propertyDescriptor;
      }


      /**
       * Return the type descriptor corresponding to a property type
       * @param {String} propertyType
       * @returns {Object}
       */
      getTypeDescriptor (propertyType) {
        const typeDescriptor = this.logic.data.meta.propertyTypes
          .find(typeDescriptor => typeDescriptor.id === propertyType);
        if (!typeDescriptor) {
          console.error("Type descriptor of `" + propertyType + "` does not exists");
        }
        return typeDescriptor;
      }


      /**
       * Return the property type descriptor corresponding to a property id
       * @param {String} propertyId
       * @returns {Object}
       */
      getPropertyTypeDescriptor (propertyId) {
        const propertyDescriptor = this.getPropertyDescriptor(propertyId);
        if (!propertyDescriptor) { return; }
        return this.logic.data.meta.propertyTypes
          .find(typeDescriptor => typeDescriptor.id === propertyDescriptor.type);
      }



      /**
       * Returns whether a certain property is visible
       * @param {String} propertyId
       * @returns {Boolean}
       */
      isVisible (propertyId) {
        const propertyDescriptor = this.getPropertyDescriptor(propertyId);
        return propertyDescriptor.visible;
      }


      /**
       * Set whether the given property should be visible
       * @param {String} propertyId
       * @param {Boolean} visible
       */
      setVisibility (propertyId, visible) {
        const propertyDescriptor = this.getPropertyDescriptor(propertyId);
        propertyDescriptor.visible = visible;
      }


      /**
       * Move a property to a certain index in the property order list
       * @param {String|Number} from The id or index of the property to move
       * @param {Number} toIndex
       */
      reorder (from, toIndex) {
        let fromIndex;
        if (typeof from === "number") {
          fromIndex = from;
        } else if (typeof from === "string") {
          if (!this.isIdValid(from)) { return; }
          fromIndex = this.logic.data.query.properties.indexOf(from);
        } else {
          return;
        }
        if (fromIndex <= -1 || toIndex <= -1) { return; }
        this.logic.data.query.properties.splice(toIndex, 0, this.logic.data.query.properties.splice(fromIndex, 1)[0]);
      }



      setType (propertyId, type) {
        const propertyDescriptor = this.getPropertyDescriptor(propertyId);
        propertyDescriptor.type = type;
      }


      setDisplayer (propertyId, displayerId) {
        if (displayerId) {
          Vue.set(this.getPropertyDescriptor(propertyId), "displayer", {
            id: displayerId,
          });
        }
        else {
          Vue.delete(this.getPropertyDescriptor(propertyId), "displayer");
        }
      }


      setFilter (propertyId, filterId) {
        if (filterId) {
          Vue.set(this.getPropertyDescriptor(propertyId), "filter", {
            id: filterId,
          });
        }
        else {
          Vue.delete(this.getPropertyDescriptor(propertyId), "filter");
        }
      }

     };




    /**
     * ---------------------------------------------------------------
     * SELECTION
     */

    class LogicSelection {

      constructor (logic) {
        this.logic = logic;

        // If the `isGlobal` property is set to true, all properties are considered selected
        this.isGlobal = false;
        // selected properties
        this.selected = [];
        // If the `isGlobal` property is set to true,  we track deselected entries
        // using the `deselected` array property
        this.deselected = [];
      }

      /**
       * Return whether the entry is currently selected
       * @param {Object} entry
       * @returns {Boolean}
       */
      isSelected (entry) {
        const entryId = this.logic.entries.getId(entry);
        if (this.isGlobal) {
          return !this.logic.uniqueArrayHas(this.deselected, entryId);
        } else {
          return this.logic.uniqueArrayHas(this.selected, entryId);
        }
      }


      /**
       * Select the specified entries
       * @param {Object|Array} entries
       */
      select (entries) {
        const entryArray = (entries instanceof Array) ? entries : [entries];
        entryArray.forEach(entry => {
          const entryId = this.logic.entries.getId(entry);
          if (this.isGlobal) {
            this.logic.uniqueArrayRemove(this.deselected, entryId);
          }
          else {
            this.logic.uniqueArrayAdd(this.selected, entryId);
          }
          this.logic.event.trigger("select", {
            entry: entry,
          });
        });
      }


      /**
       * Deselect the specified entries
       * @param {Object|Array} entries
       */
      deselect (entries) {
        const entryArray = (entries instanceof Array) ? entries : [entries];
        entryArray.forEach(entry => {
          const entryId = this.logic.entries.getId(entry);
          if (this.isGlobal) {
            this.logic.uniqueArrayAdd(this.deselected, entryId);
          }
          else {
            this.logic.uniqueArrayRemove(this.selected, entryId);
          }
          this.logic.event.trigger("deselect", {
            entry: entry,
          });
        });
      }


      /**
       * Toggle the selection of the specified entries
       * @param {Object|Array} entries
       * @param {Boolean} select Whether to select or not the entries. Undefined toggle current state
       */
      toggleSelect (entries, select) {
        const entryArray = (entries instanceof Array) ? entries : [entries];
        entryArray.forEach(entry => {
          if (select === undefined) {
            select = !this.isSelected(entry);
          }
          if (select) {
            this.select(entry);
          } else {
            this.deselect(entry);
          }
        });
      }


      /**
       * Get number of selected entries
       * @returns {Number}
       */
      getCount () {
        if (this.isGlobal) {
          return this.logic.data.data.count - this.deselected.length;
        } else {
          return this.selected.length;
        }
      }


      /**
       * Set the entry selection globally accross pages
       * @param {Boolean} global
       */
      setGlobal (global) {
        this.isGlobal = global;
        this.selected.splice(0);
        this.deselected.splice(0);
        this.logic.event.trigger("selectGlobal", {
          state: global,
        });
      }

    };






    /**
     * ---------------------------------------------------------------
     * SORT
     */

    class LogicSort {

      constructor (logic) {
        this.logic = logic;
      }


      /**
       * Returns whether a certain property is sortable or not
       * @param {String} propertyId
       * @returns {Boolean}
       */
      isSortable (propertyId) {
        const propertyDescriptor = this.logic.properties.getPropertyDescriptor(propertyId);
        const propertyTypeDescriptor = this.logic.properties.getPropertyTypeDescriptor(propertyId);
        return propertyDescriptor.sortable !== undefined ?
        propertyDescriptor.sortable :
        propertyTypeDescriptor.sortable;
      }


      /**
       * Returns the property descriptors of sortable properties
       * @returns {Array}
       */
      getSortablePropertyDescriptors () {
        return this.logic.data.meta.propertyDescriptors
          .filter(propertyDescriptor => this.isSortable(propertyDescriptor.id));
      }


      /**
       * Get the sort query associated to a property id
       * @param {String} propertyId
       */
      getQuerySort (propertyId) {
        if (!this.logic.properties.isIdValid(propertyId)) { return; }
        return this.logic.data.query.sort.find(sort => sort.property === propertyId);
      }


      /**
       * Update sort configuration based on parameters, then fetch new data
       * @param {String} property The property to sort according to
       * @param {String} level The sort level for the property (0 is the highest).
       *   Undefined means keep current. Negative value removes property sort.
       * @param {String} descending Specify whether the sort should be descending or not.
       *   Undefined means toggle current direction
       * @returns {Promise}
       */
      sort (property, level, descending) {
        const err = new Error("Property `" + property + "` is not sortable");
        return new Promise ((resolve, reject) => {
          if (!this.logic.properties.isIdValid(property)) { return void reject(err); }
          if (!this.isSortable(property)) { return void reject(err); }
          // find property current sort level
          const currentLevel = this.logic.data.query.sort.findIndex(sortObject => sortObject.property === property);
          // default level
          if (level === undefined) {
            level = (currentLevel !== -1) ? currentLevel : 0;
          } else if (level < 0) {
            level = -1;
          }
          // default descending
          if (descending === undefined) {
            descending = (currentLevel !== -1) ? !this.logic.data.query.sort[currentLevel].descending : false;
          }
          // create sort object
          const sortObject = {
            property: property,
            descending: descending,
          };
          // apply sort
          if (level !== -1) {
            this.logic.data.query.sort.splice(level, 1, sortObject);
          }
          if (currentLevel !== -1 && currentLevel !== level) {
            this.logic.data.query.sort.splice(currentLevel, 1);
          }
          // dispatch events
          this.logic.event.trigger("sort", {
            property: property,
            level: level,
            descending: descending,
          });

          // TODO: CALL FUNCTION TO FETCH NEW DATA HERE
          resolve();
        });
      }


      /**
       * Add new sort entry, shorthand of sort:
       * If the property is already sorting, does nothing
       * @param {String} property The property to add to the sort
       * @param {String} descending Specify whether the sort should be descending or not.
       *   Undefined means toggle current direction
       * @returns {Promise}
       */
      add (property, descending) {
        const err = new Error("Property `" + property + "` is already sorting");
        const propertyQuerySort = this.logic.data.query.sort.find(sortObject => sortObject.property === property);
        if (propertyQuerySort) { return Promise.reject(err); }
        return this.sort(property, this.logic.data.query.sort.length, descending);
      }


      /**
       * Remove a sort entry, shorthand of sort:
       * @param {String} property The property to remove to the sort
       * @returns {Promise}
       */
      remove (property) {
        return this.sort(property, -1);
      }


      /**
       * Move a sort entry to a certain index in the query sort list
       * @param {String} property The property to reorder the sort
       * @param {Number} toIndex
       */
      reorder (propertyId, toIndex) {
        const err = new Error("Property `" + propertyId + "` is not sortable");
        return new Promise ((resolve, reject) => {
          if (!this.logic.properties.isIdValid(propertyId)) { return void reject(err); }
          const fromIndex = this.logic.data.query.sort.findIndex(querySort => querySort.property === propertyId);
          if (fromIndex <= -1 || toIndex <= -1) { return void reject(err); }
          this.logic.data.query.sort.splice(toIndex, 0, this.logic.data.query.sort.splice(fromIndex, 1)[0]);

          // dispatch events
          this.logic.event.trigger("sort", {
            type: "move",
            property: propertyId,
            level: toIndex,
          });

          // TODO: CALL FUNCTION TO FETCH NEW DATA HERE
          resolve();
        });
      }

    };





    /**
     * ---------------------------------------------------------------
     * FILTER
     */

    class LogicFilters {

      constructor (logic) {
        this.logic = logic;
      }


      /**
       * Returns whether a certain property is filterable or not
       * @param {String} propertyId
       * @returns {Boolean}
       */
      isFilterable (propertyId) {
        const propertyDescriptor = this.logic.properties.getPropertyDescriptor(propertyId);
        const propertyTypeDescriptor = this.logic.properties.getPropertyTypeDescriptor(propertyId);
        return propertyDescriptor.filterable !== undefined ?
          propertyDescriptor.filterable :
          propertyTypeDescriptor.filterable;
      }


      /**
       * Returns the property descriptors of filterable properties
       * @returns {Array}
       */
      getFilterablePropertyDescriptors () {
        return this.logic.data.meta.propertyDescriptors
          .filter(propertyDescriptor => this.isFilterable(propertyDescriptor.id));
      }



      /**
       * Get the filter descriptor associated to a property id
       * @param {String} propertyId
       * @returns {Object}
       */
      getDescriptorFromProperty (propertyId) {
        if (!this.logic.properties.isIdValid(propertyId)) { return; }
        // property descriptor config
        const propertyDescriptor = this.logic.properties.getPropertyDescriptor(propertyId) || {};
        const propertyDescriptorFilter = propertyDescriptor.filter || {};
        // property type descriptor config
        const typeDescriptor = this.logic.properties.getPropertyTypeDescriptor(propertyId) || {};
        const typeDescriptorFilter = typeDescriptor.filter || {};
        // merge property and/or type filter descriptors
        let mergedFilterDescriptor;
        if (!propertyDescriptorFilter.id || propertyDescriptorFilter.id === typeDescriptorFilter.id) {
          mergedFilterDescriptor = Object.assign({}, typeDescriptorFilter, propertyDescriptorFilter);
        } else {
          mergedFilterDescriptor = Object.assign({}, propertyDescriptorFilter);
        }
        // resolve filter
        return this._resolveFilterDescriptor(mergedFilterDescriptor);
      }


      /**
       * Get the filter descriptor associated to a property type
       * @param {String} propertyType
       * @returns {Object}
       */
      getDescriptorFromType (propertyType) {
        if (!this.logic.properties.isTypeValid(propertyType)) { return; }
        // property type descriptor config
        const typeDescriptor = this.logic.properties.getTypeDescriptor(propertyType) || {};
        const typeDescriptorDisplayer = typeDescriptor.displayer || {};
        // resolve filter
        return this._resolveFilterDescriptor(typeDescriptorDisplayer);
      }


      /**
       * Inherit or default given filter descriptor
       * @param {Object} filterDescriptor A filter descriptor, from the property descriptor or type descriptor
       * @returns {Object}
       */
      _resolveFilterDescriptor (filterDescriptor) {
        const filterId = filterDescriptor.id;
        const filter = this.logic.data.meta.filters.find(filter => filter.id === filterId);
        // merge filters
        if (filterDescriptor.id) {
          return Object.assign({}, filter, filterDescriptor);
        } else {
          // default filter
          return { id: this.logic.data.meta.defaultFilter };
        }
      }



      /**
       * Get the filter object in the query data object associated to a property id
       * @param {String} propertyId
       * @returns {Object}
       */
      getQueryFilterGroup (propertyId) {
        if (!this.logic.properties.isIdValid(propertyId)) { return; }
        return this.logic.data.query.filters.find(filter => filter.property === propertyId);
      }


      /**
       * Get the filters in the query data object associated to a property id
       * @param {String} propertyId
       * @returns {Array} The constrains array of the filter group, or empty array if it does not exist
       */
      getQueryFilters (propertyId) {
        if (!this.logic.properties.isIdValid(propertyId)) { return; }
        const queryFilterGroup = this.getQueryFilterGroup(propertyId);
        return queryFilterGroup && queryFilterGroup.constrains || [];
      }


      /**
       * Get the default filter operator associated to a property id
       * @param {String} propertyId
       * @returns {String}
       */
      getDefaultOperator (propertyId) {
        // get valid operator descriptor
        const filterDescriptor = this.getDescriptorFromProperty(propertyId);
        if (!filterDescriptor) { return; }
        const filterOperators = filterDescriptor.operators;
        if (!(filterOperators instanceof Array)) { return; }
        if (filterOperators.length === 0) { return; }
        // get default operator
        const defaultOperator = filterDescriptor.defaultOperator;
        const isDefaultOperatorValid = !!filterOperators.find(operator => operator.id === defaultOperator);
        if (defaultOperator && isDefaultOperatorValid) {
          return defaultOperator;
        } else {
          return filterOperators[0].id;
        }
      }


      /**
       * Return an object containing the new and old filter entries corresponding to parameters
       *  oldEntry: the filter entry to be modified
       *  newEntry: what this.logic entry should be modified to
       * @param {String} property The property to filter according to
       * @param {String} index The index of the filter entry
       * @param {String} filterEntry The filter data used to update the filter configuration
       *  (see Logic.prototype.filter for more)
       * @returns {Object} {oldEntry, newEntry}
       *  with oldEntry / newEntry being {property, index, operator, value}
       */
      _computeFilterEntries (property, index, filterEntry) {
        if (!this.logic.properties.isIdValid(property)) { return; }
        if (!this.isFilterable(property)) { return; }
        // default indexes
        index = index || 0;
        if (index < 0) { index = -1; }
        if (filterEntry.index < 0) { filterEntry.index = -1; }
        // old entry
        let oldEntry = {
          property: property,
          index: index,
        };
        const queryFilters = this.getQueryFilters(property);
        const currentEntry = queryFilters[index] || {};
        oldEntry = Object.assign({}, currentEntry, oldEntry);
        // new entry
        let newEntry = filterEntry || {};
        const self = this;
        const defaultEntry = {
          property: property,
          value: "",
          get operator () { return self.getDefaultOperator(this.property); },
          index: 0,
        };
        newEntry = Object.assign({}, defaultEntry, oldEntry, newEntry);
        return {
          oldEntry: oldEntry,
          newEntry: newEntry,
        };
      }


      /**
       * Return the filtering type, based on oldEntry and newEntry
       * @param {Object} oldEntry
       * @param {Oject} newEntry
       * @returns {String} "add" | "remove" | "move" | "modify"
       */
      _getFilteringType (oldEntry, newEntry) {
        const queryFilter = this.getQueryFilterGroup(oldEntry.property);
        if (queryFilter && oldEntry.index === -1) {
          return "add";
        }
        if (newEntry.index === -1) {
          return "remove";
        }
        if (oldEntry.index !== newEntry.index) {
          return "move";
        }
        return "modify";
      }


      /**
       * Update filter configuration based on parameters, then fetch new data
       * @param {String} property The property to filter according to
       * @param {String} index The index of the filter entry
       * @param {String} filterEntry The filter data used to update the filter configuration
       *  filterEntry = {property, operator, value}
       *  undefined values are defaulted to current values, then to default values.
       * @param {String} filterEntry.property The new property to filter according to
       * @param {String} filterEntry.index The new index the filter should go. -1 delete filter
       * @param {String} filterEntry.operator The operator of the filter.
       *  Should match the filter descriptor of the filter property
       * @param {String} filterEntry.value Value for the new filter entry
       * @returns {Promise}
       */
      filter (property, index, filterEntry) {
        const err = new Error("Property `" + property + "` is not filterable");
        return new Promise ((resolve, reject) => {
          const filterEntries = this._computeFilterEntries(property, index, filterEntry);
          if (!filterEntries) { return void reject(err); }
          const oldEntry = filterEntries.oldEntry;
          const newEntry = filterEntries.newEntry;
          const filteringType = this._getFilteringType(oldEntry, newEntry);
          // remove filter at current property and index
          if (oldEntry.index !== -1) {
            this.getQueryFilters(oldEntry.property).splice(index, 1);
          }
          // add filter at new property and index
          if (newEntry.index !== -1) {
            // create filterGroup if not exists
            if (!this.getQueryFilterGroup(newEntry.property)) {
              this.logic.data.query.filters.push({
                property: newEntry.property,
                matchAll: true,
                constrains: [],
              });
            }
            // add entry
            this.getQueryFilterGroup(newEntry.property).constrains.splice(newEntry.index, 0, {
              operator: newEntry.operator,
              value: newEntry.value,
            });
          }
          // remove filter group if empty
          if (this.getQueryFilters(oldEntry.property).length === 0) {
            this.removeAll(oldEntry.property);
          }
          // dispatch events
          this.logic.event.trigger("filter", {
            type: filteringType,
            oldEntry: oldEntry,
            newEntry: newEntry,
          });

          // TODO: CALL FUNCTION TO FETCH NEW DATA HERE
          resolve();
        });
      }


      /**
       * Add new filter entry, shorthand of filter:
       * @param {String} property Which property to add the filter to
       * @param {String} operator The operator of the filter. Should match the filter descriptor of the property
       * @param {String} value Default value for the new filter entry
       * @param {Number} index Index of new filter entry. Undefined means last
       * @returns {Promise}
       */
      add (property, operator, value, index) {
        if (index === undefined) {
          index = ((this.getQueryFilterGroup(property) || {}).constrains || []).length;
        }
        return this.filter(property, -1, {
          property: property,
          operator: operator,
          value: value,
          index: index,
        });
      }


      /**
       * Remove a filter entry in the configuration, then fetch new data
       * @param {String} property Property to remove the filter to
       * @param {String} index The index of the filter to remove. Undefined means last.
       * @returns {Promise}
       */
      remove (property, index) {
        return this.filter(property, index, {index: -1});
      }


      /**
       * Remove all the filters associated to a property
       * @param {String} property Property to remove the filters to
       * @returns {Promise}
       */
      removeAll (property) {
        return new Promise ((resolve, reject) => {
          if (!this.logic.properties.isIdValid(property)) { return; }
          const filterIndex = this.logic.data.query.filters
            .findIndex(filterGroup => filterGroup.property === property);
          if (filterIndex === -1) { return void reject(); }
          const removedFilterGroups = this.logic.data.query.filters.splice(filterIndex, 1);
          // dispatch events
          this.logic.event.trigger("filter", {
            type: "removeAll",
            property: property,
            removedFilters: removedFilterGroups[0].constrains,
          });
          // TODO: CALL FUNCTION TO FETCH NEW DATA HERE
          resolve();
        });
      }

   };


   class LogicDisplayers {

    constructor (logic) {
      this.logic = logic;
    }


    /**
     * Get the displayer descriptor associated to a property id
     * @param {String} propertyId
     * @returns {Object}
     */
    getDescriptorFromProperty (propertyId) {
      if (!this.logic.properties.isIdValid(propertyId)) { return; }
      // property descriptor config
      const propertyDescriptor = this.logic.properties.getPropertyDescriptor(propertyId) || {};
      const propertyDescriptorDisplayer = propertyDescriptor.displayer || {};
      // property type descriptor config
      const typeDescriptor = this.logic.properties.getPropertyTypeDescriptor(propertyId) || {};
      const typeDescriptorDisplayer = typeDescriptor.displayer || {};
      // merge property and/or type displayer descriptors
      let mergedDisplayerDescriptor;
      if (!propertyDescriptorDisplayer.id || propertyDescriptorDisplayer.id === typeDescriptorDisplayer.id) {
        mergedDisplayerDescriptor = Object.assign({}, typeDescriptorDisplayer, propertyDescriptorDisplayer);
      } else {
        mergedDisplayerDescriptor = Object.assign({}, propertyDescriptorDisplayer);
      }
      // resolve displayer
      return this._resolveDisplayerDescriptor(mergedDisplayerDescriptor);
    }


    /**
     * Get the displayer descriptor associated to a property type
     * @param {String} propertyType
     * @returns {Object}
     */
    getDescriptorFromType (propertyType) {
      if (!this.logic.properties.isTypeValid(propertyType)) { return; }
      // property type descriptor config
      const typeDescriptor = this.logic.properties.getTypeDescriptor(propertyType) || {};
      const typeDescriptorDisplayer = typeDescriptor.displayer || {};
      // resolve displayer
      return this._resolveDisplayerDescriptor(typeDescriptorDisplayer);
    }


    /**
     * Inherit or default given displayer descriptor
     * @param {Object} displayerDescriptor A displayer descriptor, from the property descriptor or type descriptor
     * @returns {Object}
     */
    _resolveDisplayerDescriptor (displayerDescriptor) {
      const displayerId = displayerDescriptor.id;
      const displayer = this.logic.data.meta.displayers.find(displayer => displayer.id === displayerId);
      // merge displayers
      if (displayerDescriptor.id) {
        return Object.assign({}, displayer, displayerDescriptor);
      } else {
        // default displayer
        return { id: this.logic.data.meta.defaultDisplayer };
      }
    }

   };





    /**
     * ---------------------------------------------------------------
     * TEMPORARY CONFIG
     */

    class LogicTemporaryConfig {

      constructor (logic) {
        this.logic = logic;
        // The different saves used to store Livedata states
        // Some states examples are:
        // - The initial state (config the user gets from the server)
        // - The pre-design state (modified config before the user switch to design mode)
        this.dataSaves = {};
        this.save("initial");
      }


      /**
       * Return temporary config at given key if config is a string
       * or return directly the given config if it is valid
       * @param {String|Object} config The key of an already save config, or a config object
       * @returns
       */
      _parse (config) {
        if (typeof config === "string") {
          // Type String: return config saved at given key
          const configObject = this.dataSaves[config];
          if (!configObject) {
            console.error("Error: temporary config at key `" + config + "` can't be found");
          }
          return configObject;
        } else if (typeof config === "object") {
          // Type Object: return config as it is
          return config;
        }
        // Other type: return undefined and show error
        console.error("Error: given config is invalid");
      }


      /**
       * Format a config object to only keep query and meta properties
       * JSON.parse(JSON.stringify()) is used to ensure that
       * the saved config is not reactive to further changes
       * @param {Object} configObject The object to format
       */
      _format (configObject) {
        return {
          query: JSON.parse(JSON.stringify(configObject.query)),
          meta: JSON.parse(JSON.stringify(configObject.meta)),
        };
      }

      /**
       * Save current or given Livedata configuration to a temporary state
       * This state could be reloaded later using the temporaryConfig.load method
       * @param {String} configName The key used to store the config
       * @param {Object} config The config to save. Uses current if undefined
       */
      save (configName, config) {
        if (config === undefined) {
          config = config || this.logic.data;
        }
        const configToSave = this._format(config);
        this.dataSaves[configName] = configToSave;
      }


      /**
       * Replace current config with the given one
       * @param {String|Object} config The key of an already save config, or a config object
       */
      load (config) {
        const configToLoad = this._parse(config);
        if (!configToLoad) { return; }
        this.logic.data.query = configToLoad.query;
        this.logic.data.meta = configToLoad.meta;
      }


      /**
       * Return whether the given config is equal (deeply) to the current config
       * @param {String|Object} config The key of an already save config, or a config object
       * @returns {Boolean}
       */
      equals (config) {
        const configToCompare = this._parse(config);
        if (!configToCompare) { return; }
        return this.logic.isDeepEqual(
          this._format(configToCompare),
          this._format(this.logic.data)
        );
      }


    };






    /**
     * ---------------------------------------------------------------
     * DESIGN MODE
     */


     class LogicDesignMode {

      constructor (logic) {
        this.logic = logic;
        // Whether the Livedata is in design mode or not
        this.activated = false;
      }


      /**
       * Toggle design mode
       * When toggling ON: save temporary config to "edit" save
       * When toggling OFF: load temporary config from "edit" save
       *
       * In order to keep design state when leaving design mode
       * the design config has to be saved in "initial" and "edit" saves
       * (This is done automatically in the LivedataDesignModeBar component)
       * @param {Boolean} activated true to toggle design mode ON, false for OFF
       * If undefined, toggle current state
       * @returns {Promise}
       */
      toggle (activated) {
        if (activated === undefined) {
          activated = !this.activated;
        }
        if (activated) {
          this.activated = true;
          this.temporaryConfig.save("edit");
        } else {
          this.activated = false;
          this.temporaryConfig.load("edit");
        }
      }

    };







  /**
   * Map the element to its data object
   * So that each instance of the livedata on the page handle there own data
   */
  const instancesMap = new WeakMap();



  /**
   * The init function of the logic script
   * For each livedata element on the page, returns its corresponding data / API
   * If the data does not exists yet, create it from the element
   * @param {HTMLElement} element The HTML Element corresponding to the Livedata component
   */
  const init = function (element) {

    if (!instancesMap.has(element)) {
      // create a new logic object associated to the element
      const logic = new Logic(element);
      instancesMap.set(element, logic);
    }

    return instancesMap.get(element);
  };



  class Logic {

    constructor (element) {
      this.element = element;

      // The element where the Livedata vue component is mounted
      this.element = element;

      // A helper object to track opened configuration panels
      this.openedPanels = [];

      // The Livedata configuration object
      this.data = JSON.parse(element.getAttribute("data-data") || "{}");
      element.removeAttribute("data-data");

      this.event = new LogicEvent(this);
      this.temporaryConfig = new LogicTemporaryConfig(this);
      this.layout = new LogicLayout(this);
      this.entries = new LogicEntries(this);
      this.pagination = new LogicPagination(this);
      this.properties = new LogicProperties(this);
      this.selection = new LogicSelection(this);
      this.sort = new LogicSort(this);
      this.filters = new LogicFilters(this);
      this.displayers = new LogicDisplayers(this);
      this.designMode = new LogicDesignMode(this);

      // Create Livedata instance
      new Vue({
        el: this.element,
        components: {
          "XWikiLivedata": XWikiLivedata,
        },
        template: "<XWikiLivedata :logic='logic'></XWikiLivedata>",
        data: {
          logic: this,
        },
      });

    }


    /**
     * ---------------------------------------------------------------
     * UTILS
     */



    /**
     * Compare two objects deeply, using SameValue comparison
     * @param {Any} a A first object to compare
     * @param {Any} b A second object to compare
     */
    isDeepEqual (a, b) {
      // Check direct equality (using sameValue comparison)
      if (Object.is(a, b)) { return true; }
      // If properties are not equal and not object, return false
      if (typeof a !== "object" || typeof b !== "object") { return false; }
      // Check class equality
      if (a.constructor !== b.constructor) { return false; }
      // check if objects  contain the same entries
      const aKeys = Object.keys(a);
      const bKeys = Object.keys(b);
      if (aKeys.length !== bKeys.length) { return false; }
      return aKeys.every(key => this.isDeepEqual(a[key], b[key]));
    }


    /*
      As Sets are not reactive in Vue 2.x, if we want to create
      a reactive collection of unique objects, we have to use arrays.
      So here are some handy functions to do what Sets do, but with arrays
    */

    /**
     * Return whether the array has the given item
     * @param {Array} uniqueArray An array of unique items
     * @param {Any} item
     */
    uniqueArrayHas (uniqueArray, item) {
      return uniqueArray.includes(item);
    }


    /**
     * Add the given item if not present in the array, or does nothing
     * @param {Array} uniqueArray An array of unique items
     * @param {Any} item
     */
    uniqueArrayAdd (uniqueArray, item) {
      if (this.uniqueArrayHas(uniqueArray, item)) { return; }
      uniqueArray.push(item);
    }


    /**
     * Remove the given item from the array if present, or does nothing
     * @param {Array} uniqueArray An array of unique items
     * @param {Any} item
     */
    uniqueArrayRemove (uniqueArray, item) {
      const index = uniqueArray.indexOf(item);
      if (index === -1) { return; }
      uniqueArray.splice(index, 1);
    }


    /**
     * Toggle the given item from the array, ensuring its uniqueness
     * @param {Array} uniqueArray An array of unique items
     * @param {Any} item
     * @param {Boolean} force Optional: true force add / false force remove
     */
    uniqueArrayToggle (uniqueArray, item, force) {
      if (force === undefined) {
        force = !this.uniqueArrayHas(uniqueArray, item);
      }
      if (force) {
        this.uniqueArrayAdd(uniqueArray, item);
      } else {
        this.uniqueArrayRemove(uniqueArray, item);
      }
    }

  };






  // return the init function to be used in the layouts
  return init;

});



