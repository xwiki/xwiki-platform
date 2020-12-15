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
  "liveDataSource"
], function (
  Vue,
  XWikiLivedata,
  liveDataSource
) {

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


  /**
   * Class for a logic element
   * Contains the Livedata data object and methods to mutate it
   * Can be used in the layouts to display the data, and call its API
   * @param {HTMLElement} element The HTML Element corresponding to the Livedata
   */
  const Logic = function (element) {
    this.element = element;
    this.data = JSON.parse(element.getAttribute("data-config") || "{}");
    this.currentLayoutId = "";
    this.changeLayout(this.data.meta.defaultLayout);
    this.entrySelection = {
      selected: [],
      deselected: [],
      isGlobal: false,
    };
    this.openedPanels = [];

    element.removeAttribute("data-config");
    // create Vuejs instance
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

    // Fetch the data if we don't have any.
    if (!this.data.data.entries.length) {
      this.updateEntries();
    }
  };




  /**
   * THE LOGIC API
   */
  Logic.prototype = {


    /**
     * ---------------------------------------------------------------
     * EVENTS
     */


    /**
     * Send custom events
     * @param {String} eventName The name of the event, without the prefix "xwiki:livedata"
     * @param {Object} eventData The data associated with the event.
     *  The livedata object reference is automatically added
     */
    triggerEvent (eventName, eventData) {
      // configure event
      const defaultData = {
        livedata: this,
      };
      eventName = "xwiki:livedata:" + eventName;
      eventData = {
        bubbles: true,
        detail: Object.assign(defaultData, eventData),
      };
      const event = new CustomEvent(eventName, eventData);
      // dispatch event
      this.element.dispatchEvent(event);
    },

    /**
     * Listen for custom events
     * @param {String} eventName The name of the event, without the prefix "xwiki:livedata"
     * @param {Function} callback Function to call we the event is triggered
     */
    onEvent (eventName, callback) {
      eventName = "xwiki:livedata:" + eventName;
      this.element.addEventListener(eventName, function (e) {
        callback(e.detail);
      });
    },


    /**
     * Listen for custom events, mathching certain conditions
     * @param {String} eventName The name of the event, without the prefix "xwiki:livedata"
     * @param {Object|Function} condition The condition to execute the callback
     *  if Object, values of object properties must match e.detail properties values
     *  if Function, the function must return true. e.detail is passed as argument
     * @param {Function} callback Function to call we the event is triggered
     */
    onEventWhere (eventName, condition, callback) {
      eventName = "xwiki:livedata:" + eventName;
      this.element.addEventListener(eventName, function (e) {
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
    },




    /**
     * ---------------------------------------------------------------
     * UTILS
     */


    /**
     * Return the list of layout ids
     * @returns {Array}
     */
    getLayoutIds () {
      return this.data.meta.layouts.map(layoutDescriptor => layoutDescriptor.id);
    },


    /**
     * Return whether the specified property id is valid (i.e. the property has a descriptor)
     * @param {String} propertyId
     */
    isValidPropertyId (propertyId) {
      return this.data.query.properties.includes(propertyId);
    },


    /**
     * Return the id of the given entry
     * @param {Object} entry
     * @returns {String}
     */
    getEntryId (entry) {
      const idProperty = this.data.meta.entryDescriptor.idProperty || "id";
      if (entry[idProperty] === undefined) {
        console.warn("Entry has no id (at property [" + idProperty + "]", entry);
        return;
      }
      return entry[idProperty];
    },


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
    },


    /**
     * Add the given item if not present in the array, or does nothing
     * @param {Array} uniqueArray An array of unique items
     * @param {Any} item
     */
    uniqueArrayAdd (uniqueArray, item) {
      if (this.uniqueArrayHas(uniqueArray, item)) { return; }
      uniqueArray.push(item);
    },


    /**
     * Remove the given item from the array if present, or does nothing
     * @param {Array} uniqueArray An array of unique items
     * @param {Any} item
     */
    uniqueArrayRemove (uniqueArray, item) {
      const index = uniqueArray.indexOf(item);
      if (index === -1) { return; }
      uniqueArray.splice(index, 1);
    },


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
    },




    /**
     * ---------------------------------------------------------------
     * DESCRIPTORS
     */


    /**
     * Returns the property descriptors of displayable properties
     * @returns {Array}
     */
    getPropertyDescriptors () {
      return this.data.query.properties.map(propertyId => this.getPropertyDescriptor(propertyId));
    },


    /**
     * Return the property descriptor corresponding to a property id
     * @param {String} propertyId
     * @returns {Object}
     */
    getPropertyDescriptor (propertyId) {
      const propertyDescriptor = this.data.meta.propertyDescriptors
        .find(propertyDescriptor => propertyDescriptor.id === propertyId);
      if (!propertyDescriptor) {
        console.error("Property descriptor of property `" + propertyId + "` does not exists");
      }
      return propertyDescriptor;
    },


    /**
     * Return the property type descriptor corresponding to a property id
     * @param {String} propertyId
     * @returns {Object}
     */
    getPropertyTypeDescriptor (propertyId) {
      const propertyDescriptor = this.getPropertyDescriptor(propertyId);
      if (!propertyDescriptor) { return; }
      return this.data.meta.propertyTypes
        .find(typeDescriptor => typeDescriptor.id === propertyDescriptor.type);
    },


    /**
     * Return the layout descriptor corresponding to a layout id
     * @param {String} propertyId
     * @returns {Object}
     */
    getLayoutDescriptor (layoutId) {
      return this.data.meta.layouts
        .find(layoutDescriptor => layoutDescriptor.id === layoutId);
    },


    /**
     * Get the displayer descriptor associated to a property id
     * @param {String} propertyId
     * @returns {Object}
     */
    getDisplayerDescriptor (propertyId) {
      if (!this.isValidPropertyId(propertyId)) { return; }
      // property descriptor config
      const propertyDescriptor = this.getPropertyDescriptor(propertyId) || {};
      const propertyDescriptorDisplayer = propertyDescriptor.displayer || {};
      // property type descriptor config
      const typeDescriptor = this.getPropertyTypeDescriptor(propertyId) || {};
      const typeDescriptorDisplayer = typeDescriptor.displayer || {};
      // merge property and/or type displayer descriptors
      let highLevelDisplayer;
      if (!propertyDescriptorDisplayer.id || propertyDescriptorDisplayer.id === typeDescriptorDisplayer.id) {
        highLevelDisplayer = Object.assign({}, typeDescriptorDisplayer, propertyDescriptorDisplayer);
      } else {
        highLevelDisplayer = Object.assign({}, propertyDescriptorDisplayer);
      }
      // displayer config
      const displayerId = highLevelDisplayer.id;
      const displayer = this.data.meta.displayers.find(displayer => displayer.id === displayerId);
      // merge displayers
      if (highLevelDisplayer.id) {
        return Object.assign({}, displayer, highLevelDisplayer);
      } else {
        // default displayer
        return { id: this.data.meta.defaultDisplayer };
      }
    },


    /**
     * Get the filter descriptor associated to a property id
     * @param {String} propertyId
     * @returns {Object}
     */
    getFilterDescriptor (propertyId) {
      if (!this.isValidPropertyId(propertyId)) { return; }
      // property descriptor config
      const propertyDescriptor = this.getPropertyDescriptor(propertyId) || {};
      const propertyDescriptorFilter = propertyDescriptor.filter || {};
      // property type descriptor config
      const typeDescriptor = this.getPropertyTypeDescriptor(propertyId) || {};
      const typeDescriptorFilter = typeDescriptor.filter || {};
      // merge property and/or type filter descriptors
      let highLevelFilter;
      if (!propertyDescriptorFilter.id || propertyDescriptorFilter.id === typeDescriptorFilter.id) {
        highLevelFilter = Object.assign({}, typeDescriptorFilter, propertyDescriptorFilter);
      } else {
        highLevelFilter = Object.assign({}, propertyDescriptorFilter);
      }
      // filter filter config
      const filterId = highLevelFilter.id;
      const filter = this.data.meta.filters.find(filter => filter.id === filterId);
      // merge filters
      if (highLevelFilter.id) {
        return Object.assign({}, filter, highLevelFilter);
      } else {
        // default filter
        return { id: this.data.meta.defaultFilter };
      }
    },




    /**
     * ---------------------------------------------------------------
     * LAYOUT
     */


    fetchEntries () {
      return liveDataSource.getEntries(this.data.query);
    },


    updateEntries () {
      return this.fetchEntries()
        .then(data => this.data.data = data)
        .catch(err => console.error(err));
    },


    /**
     * Return whether the Livedata is editable or not
     * if entry given, return whether it is editable
     * if property given, return whether it is editable (for any entries)
     * If entry and property given, return whether specific value is editable
     * @param {Object} [parameters]
     * @param {Object} [parameters.entry] The entry object
     * @param {Number} [parameters.propertyId] The property id of the entry
     */
    isEditable ({ entry, propertyId } = {}) {
      // TODO: Ensure entry is valid (need other current PR)
      // TODO: Ensure property is valid (need other current PR)

      // Check if the edit entry action is available.
      if (!this.data.meta.actions.find(action => action.id === 'editEntry')) {
        return false;
      }

      // Check if we are allowed to edit the given entry.
      if (entry && !isEntryEditable(entry)) {
        return false;
      }

      // Check if the specified property is editable.
      return !propertyId || isPropertyEditable(propertyId);
    },

    /**
     * Returns whether the given entry is editable or not.
     *
     * @param {Object} entry
     * @returns {Boolean}
     */    
    isEntryEditable (entry) {
      const allowEditProperty = this.data.meta.entryDescriptor.allowEditProperty || "allowEdit";
      return entry[allowEditProperty];
    },

    /**
     * Returns whether a certain property is editable or not.
     *
     * @param {String} propertyId
     * @returns {Boolean}
     */
    isPropertyEditable (propertyId) {
      const propertyDescriptor = this.getPropertyDescriptor(propertyId);
      const propertyTypeDescriptor = this.getPropertyTypeDescriptor(propertyId);
      return propertyDescriptor.editable !== undefined ?
      propertyDescriptor.editable :
      propertyTypeDescriptor.editable;
    },

    /**
     * Set the value of the given entry property
     * @param {Object} parameters
     * @param {Object} parameters.entry The entry we want to modify
     * @param {number} parameters.propertyId The property id we want to modify in the entry
     * @param {string} parameters.value The new value of entry property
     */
    setValue ({ entry, propertyId, value }) {
      // TODO: Ensure entry is valid (need other current PR)
      // TODO: Ensure property is valid (need other current PR)
      if (!this.isEditable({ entry, propertyId })) { return; }
      entry[propertyId] = value;
      // TODO: push value to server
    },


    /**
     * Return whether adding new entries is enabled.
     */
    canAddEntry () {
      // Check if the add entry action is available.
      return this.data.meta.actions.find(action => action.id === 'addEntry');
    },

    addEntry () {
      if (!this.canAddEntry()) { return; }
      const mockNewUrl = () => this.getEntryId(this.data.data.entries.slice(-1)[0]) + "0";
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
        this.data.data.entries.push(newEntry);
        this.data.data.count++; // TODO: remove when merging with backend
      });
    },




    /**
     * ---------------------------------------------------------------
     * LAYOUT
     */


    /**
     * Load a layout, or default layout if none specified
     * @param {String} layoutId The id of the layout to load with requireJS
     * @returns {Promise}
     */
    changeLayout (layoutId) {
      // bad layout
      if (!this.getLayoutDescriptor(layoutId)) {
        console.error("Layout of id `" + layoutId + "` does not have a descriptor");
        return;
      }
      // set layout
      const previousLayoutId = this.currentLayoutId;
      this.currentLayoutId = layoutId;
      // dispatch events
      this.triggerEvent("layoutChange", {
        layoutId: layoutId,
        previousLayoutId: previousLayoutId,
      });
    },




    /**
     * ---------------------------------------------------------------
     * PAGINATION
     */


    /**
     * Get total number of pages
     * @returns {Number}
     */
    getPageCount () {
      return Math.ceil(this.data.data.count / this.data.query.limit);
    },


    /**
     * Get the page corresponding to the specified entry (0-based index)
     * @param {Number} entryIndex The index of the entry. Uses current entry if undefined.
     * @returns {Number}
     */
    getPageIndex (entryIndex) {
      if (entryIndex === undefined) {
        entryIndex = this.data.query.offset;
      }
      return Math.floor(entryIndex / this.data.query.limit);
    },


    /**
     * Set page index (0-based index), then fetch new data
     * @param {Number} pageIndex
     * @returns {Promise}
     */
    setPageIndex (pageIndex) {
      return new Promise ((resolve, reject) => {
        if (pageIndex < 0 || pageIndex >= this.getPageCount()) { return void reject(); }
        const previousPageIndex = this.getPageIndex();
        this.data.query.offset = this.getFirstIndexOfPage(pageIndex);
        this.triggerEvent("pageChange", {
          pageIndex: pageIndex,
          previousPageIndex: previousPageIndex,
        });
        this.updateEntries().then(resolve, reject);
      });
    },


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
        return pageIndex * this.data.query.limit;
      } else {
        return -1;
      }
    },


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
        return Math.min(this.getFirstIndexOfPage(pageIndex) + this.data.query.limit, this.data.data.count) - 1;
      } else {
        return -1;
      }
    },


    /**
     * Set the pagination page size, then fetch new data
     * @param {Number} pageSize
     * @returns {Promise}
     */
    setPageSize (pageSize) {
      return new Promise ((resolve, reject) => {
        if (pageSize < 0) { return void reject(); }
        const previousPageSize = this.data.query.limit;
        if (pageSize === previousPageSize) { return void resolve(); }
        this.data.query.limit = pageSize;
        // Reset the offset whenever the page size changes.
        this.data.query.offset = 0;
        this.triggerEvent("pageSizeChange", {
          pageSize: pageSize,
          previousPageSize: previousPageSize,
        });
        this.updateEntries().then(resolve, reject);
      });
    },




    /**
     * ---------------------------------------------------------------
     * DISPLAY
     */


    /**
     * Returns whether a certain property is visible
     * @param {String} propertyId
     * @returns {Boolean}
     */
    isPropertyVisible (propertyId) {
      const propertyDescriptor = this.getPropertyDescriptor(propertyId);
      return propertyDescriptor.visible;
    },


    /**
     * Set whether the given property should be visible
     * @param {String} propertyId
     * @param {Boolean} visible
     */
    setPropertyVisible (propertyId, visible) {
      const propertyDescriptor = this.getPropertyDescriptor(propertyId);
      propertyDescriptor.visible = visible;
    },


    /**
     * Move a property to a certain index in the property order list
     * @param {String|Number} from The id or index of the property to move
     * @param {Number} toIndex
     */
    reorderProperty (from, toIndex) {
      let fromIndex;
      if (typeof from === "number") {
        fromIndex = from;
      } else if (typeof from === "string") {
        if (!this.isValidPropertyId(from)) { return; }
        fromIndex = this.data.query.properties.indexOf(from);
      } else {
        return;
      }
      if (fromIndex <= -1 || toIndex <= -1) { return; }
      this.data.query.properties.splice(toIndex, 0, this.data.query.properties.splice(fromIndex, 1)[0]);
    },




    /**
     * ---------------------------------------------------------------
     * ACTIONS
     */


    /**
     * Return whether selecting entries is enabled. If an entry is given, return whether that entry can be selected.
     *
     * @param {Object} [parameters]
     * @param {Object} [parameters.entry]
     */
    isSelectionEnabled ({ entry } = {}) {
      // An entry is selectable if it has an id specified.
      return this.data.meta.selection.enabled && (!entry || this.getEntryId(entry));
    },


    /**
     * Return whether the entry is currently selected
     * @param {Object} entry
     * @returns {Boolean}
     */
    isEntrySelected (entry) {
      const entryId = this.getEntryId(entry);
      if (this.entrySelection.isGlobal) {
        return !this.uniqueArrayHas(this.entrySelection.deselected, entryId);
      } else {
        return this.uniqueArrayHas(this.entrySelection.selected, entryId);
      }
    },


    /**
     * Select the specified entries
     * @param {Object|Array} entries
     */
    selectEntries (entries) {
      if (!this.isSelectionEnabled()) { return; }
      const entryArray = (entries instanceof Array) ? entries : [entries];
      entryArray.forEach(entry => {
        if (!this.isSelectionEnabled({ entry })) { return; }
        const entryId = this.getEntryId(entry);
        if (this.entrySelection.isGlobal) {
          this.uniqueArrayRemove(this.entrySelection.deselected, entryId);
        }
        else {
          this.uniqueArrayAdd(this.entrySelection.selected, entryId);
        }
        this.triggerEvent("select", {
          entry: entry,
        });
      });
    },


    /**
     * Deselect the specified entries
     * @param {Object|Array} entries
     */
    deselectEntries (entries) {
      if (!this.isSelectionEnabled()) { return; }
      const entryArray = (entries instanceof Array) ? entries : [entries];
      entryArray.forEach(entry => {
        if (!this.isSelectionEnabled({ entry })) { return; }
        const entryId = this.getEntryId(entry);
        if (this.entrySelection.isGlobal) {
          this.uniqueArrayAdd(this.entrySelection.deselected, entryId);
        }
        else {
          this.uniqueArrayRemove(this.entrySelection.selected, entryId);
        }
        this.triggerEvent("deselect", {
          entry: entry,
        });
      });
    },


    /**
     * Toggle the selection of the specified entries
     * @param {Object|Array} entries
     * @param {Boolean} select Whether to select or not the entries. Undefined toggle current state
     */
    toggleSelectEntries (entries, select) {
      if (!this.isSelectionEnabled()) { return; }
      const entryArray = (entries instanceof Array) ? entries : [entries];
      entryArray.forEach(entry => {
        if (!this.isSelectionEnabled({ entry })) { return; }
        if (select === undefined) {
          select = !this.isEntrySelected(entry);
        }
        if (select) {
          this.selectEntries(entry);
        } else {
          this.deselectEntries(entry);
        }
      });
    },


    /**
     * Get number of selectable entries in page
     * @returns {Number}
     */
    selectableCountInPage () {
      if (!this.isSelectionEnabled()) { return 0; }
      return this.data.data.entries
        .filter(entry => this.isSelectionEnabled({ entry }))
        .length;
    },


    /**
     * Set the entry selection globally accross pages
     * @param {Boolean} global
     */
    setEntrySelectGlobal (global) {
      if (!this.isSelectionEnabled()) { return; }
      this.entrySelection.isGlobal = global;
      this.entrySelection.selected.splice(0);
      this.entrySelection.deselected.splice(0);
      this.triggerEvent("selectGlobal", {
        state: global,
      });
    },




    /**
     * ---------------------------------------------------------------
     * SORT
     */


    /**
     * Returns whether a certain property is sortable or not
     * @param {String} propertyId
     * @returns {Boolean}
     */
    isPropertySortable (propertyId) {
      const propertyDescriptor = this.getPropertyDescriptor(propertyId);
      const propertyTypeDescriptor = this.getPropertyTypeDescriptor(propertyId);
      return propertyDescriptor.sortable !== undefined ?
      propertyDescriptor.sortable :
      propertyTypeDescriptor.sortable;
    },


    /**
     * Returns the property descriptors of sortable properties
     * @returns {Array}
     */
    getSortablePropertyDescriptors () {
      return this.data.meta.propertyDescriptors
        .filter(propertyDescriptor => this.isPropertySortable(propertyDescriptor.id));
    },


    /**
     * Get the sort query associated to a property id
     * @param {String} propertyId
     */
    getQuerySort (propertyId) {
      if (!this.isValidPropertyId(propertyId)) { return; }
      return this.data.query.sort.find(sort => sort.property === propertyId);
    },


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
        if (!this.isValidPropertyId(property)) { return void reject(err); }
        if (!this.isPropertySortable(property)) { return void reject(err); }
        // find property current sort level
        const currentLevel = this.data.query.sort.findIndex(sortObject => sortObject.property === property);
        // default level
        if (level === undefined) {
          level = (currentLevel !== -1) ? currentLevel : 0;
        } else if (level < 0) {
          level = -1;
        }
        // default descending
        if (descending === undefined) {
          descending = (currentLevel !== -1) ? !this.data.query.sort[currentLevel].descending : false;
        }
        // create sort object
        const sortObject = {
          property: property,
          descending: descending,
        };
        // apply sort
        if (level !== -1) {
          this.data.query.sort.splice(level, 1, sortObject);
        }
        if (currentLevel !== -1 && currentLevel !== level) {
          this.data.query.sort.splice(currentLevel, 1);
        }
        // dispatch events
        this.triggerEvent("sort", {
          property: property,
          level: level,
          descending: descending,
        });

        this.updateEntries().then(resolve, reject);
      });
    },


    /**
     * Add new sort entry, shorthand of sort:
     * If the property is already sorting, does nothing
     * @param {String} property The property to add to the sort
     * @param {String} descending Specify whether the sort should be descending or not.
     *   Undefined means toggle current direction
     * @returns {Promise}
     */
    addSort (property, descending) {
      const err = new Error("Property `" + property + "` is already sorting");
      const propertyQuerySort = this.data.query.sort.find(sortObject => sortObject.property === property);
      if (propertyQuerySort) { return Promise.reject(err); }
      return this.sort(property, this.data.query.sort.length, descending);
    },


    /**
     * Remove a sort entry, shorthand of sort:
     * @param {String} property The property to remove to the sort
     * @returns {Promise}
     */
    removeSort (property) {
      return this.sort(property, -1);
    },


    /**
     * Move a sort entry to a certain index in the query sort list
     * @param {String} property The property to reorder the sort
     * @param {Number} toIndex
     */
    reorderSort (propertyId, toIndex) {
      const err = new Error("Property `" + propertyId + "` is not sortable");
      return new Promise ((resolve, reject) => {
        if (!this.isValidPropertyId(propertyId)) { return void reject(err); }
        const fromIndex = this.data.query.sort.findIndex(querySort => querySort.property === propertyId);
        if (fromIndex <= -1 || toIndex <= -1) { return void reject(err); }
        this.data.query.sort.splice(toIndex, 0, this.data.query.sort.splice(fromIndex, 1)[0]);

        // dispatch events
        this.triggerEvent("sort", {
          type: "move",
          property: propertyId,
          level: toIndex,
        });

        this.updateEntries().then(resolve, reject);
      });
    },




    /**
     * ---------------------------------------------------------------
     * FILTER
     */


    /**
     * Returns whether a certain property is filterable or not
     * @param {String} propertyId
     * @returns {Boolean}
     */
    isPropertyFilterable (propertyId) {
      const propertyDescriptor = this.getPropertyDescriptor(propertyId);
      const propertyTypeDescriptor = this.getPropertyTypeDescriptor(propertyId);
      return propertyDescriptor.filterable !== undefined ?
        propertyDescriptor.filterable :
        propertyTypeDescriptor.filterable;
    },


    /**
     * Returns the property descriptors of filterable properties
     * @returns {Array}
     */
    getFilterablePropertyDescriptors () {
      return this.data.meta.propertyDescriptors
        .filter(propertyDescriptor => this.isPropertyFilterable(propertyDescriptor.id));
    },


    /**
     * Get the filter in the query data object associated to a property id
     * @param {String} propertyId
     * @returns {Object}
     */
    getQueryFilterGroup (propertyId) {
      if (!this.isValidPropertyId(propertyId)) { return; }
      return this.data.query.filters.find(filter => filter.property === propertyId);
    },


    /**
     * Get the filters in the query data object associated to a property id
     * @param {String} propertyId
     * @returns {Array} The constraints array of the filter group, or empty array if it does not exist
     */
    getQueryFilters (propertyId) {
      if (!this.isValidPropertyId(propertyId)) { return; }
      const queryFilterGroup = this.getQueryFilterGroup(propertyId);
      return queryFilterGroup && queryFilterGroup.constraints || [];
    },


    /**
     * Get the default filter operator associated to a property id
     * @param {String} propertyId
     * @returns {String}
     */
    getFilterDefaultOperator (propertyId) {
      // get valid operator descriptor
      const filterDescriptor = this.getFilterDescriptor(propertyId);
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
    },


    /**
     * Return an object containing the new and old filter entries corresponding to parameters
     *  oldEntry: the filter entry to be modified
     *  newEntry: what this entry should be modified to
     * @param {String} property The property to filter according to
     * @param {String} index The index of the filter entry
     * @param {String} filterEntry The filter data used to update the filter configuration
     *  (see Logic.prototype.filter for more)
     * @returns {Object} {oldEntry, newEntry}
     *  with oldEntry / newEntry being {property, index, operator, value}
     */
    _computeFilterEntries (property, index, filterEntry) {
      if (!this.isValidPropertyId(property)) { return; }
      if (!this.isPropertyFilterable(property)) { return; }
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
      // new entry (copy properties that are not undefined from filterEntry)
      let newEntry = Object.fromEntries(Object.entries(filterEntry || {})
        .filter(entry => entry[1] !== undefined));
      const self = this;
      const defaultEntry = {
        property: property,
        value: "",
        operator: self.getFilterDefaultOperator(property),
        index: 0,
      };
      newEntry = Object.assign({}, defaultEntry, oldEntry, newEntry);
      // check newEntry operator
      const newEntryValidOperator = this.getFilterDescriptor(newEntry.property).operators
        .some(operator => operator.id === newEntry.operator);
      if (!newEntryValidOperator) {
        newEntry.operator = self.getFilterDefaultOperator(newEntry.property);
      }
      return {
        oldEntry: oldEntry,
        newEntry: newEntry,
      };
    },


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
    },


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
            this.data.query.filters.push({
              property: newEntry.property,
              matchAll: true,
              constraints: [],
            });
          }
          // add entry
          this.getQueryFilterGroup(newEntry.property).constraints.splice(newEntry.index, 0, {
            operator: newEntry.operator,
            value: newEntry.value,
          });
        }
        // remove filter group if empty
        if (this.getQueryFilters(oldEntry.property).length === 0) {
          this.removeAllFilters(oldEntry.property);
        }
        // Reset the offset whenever the filters are updated.
        this.data.query.offset = 0;
        // dispatch events
        this.triggerEvent("filter", {
          type: filteringType,
          oldEntry: oldEntry,
          newEntry: newEntry,
        });

        this.updateEntries().then(resolve, reject);
      });
    },


    /**
     * Add new filter entry, shorthand of filter:
     * @param {String} property Which property to add the filter to
     * @param {String} operator The operator of the filter. Should match the filter descriptor of the property
     * @param {String} value Default value for the new filter entry
     * @param {Number} index Index of new filter entry. Undefined means last
     * @returns {Promise}
     */
    addFilter (property, operator, value, index) {
      if (index === undefined) {
        index = ((this.getQueryFilterGroup(property) || {}).constraints || []).length;
      }
      return this.filter(property, -1, {
        property: property,
        operator: operator,
        value: value,
        index: index,
      });
    },


    /**
     * Remove a filter entry in the configuration, then fetch new data
     * @param {String} property Property to remove the filter to
     * @param {String} index The index of the filter to remove. Undefined means last.
     * @returns {Promise}
     */
    removeFilter (property, index) {
      return this.filter(property, index, {index: -1});
    },


    /**
     * Remove all the filters associated to a property
     * @param {String} property Property to remove the filters to
     * @returns {Promise}
     */
    removeAllFilters (property) {
      return new Promise ((resolve, reject) => {
        if (!this.isValidPropertyId(property)) { return; }
        const filterIndex = this.data.query.filters
          .findIndex(filterGroup => filterGroup.property === property);
        if (filterIndex === -1) { return void reject(); }
        const removedFilterGroups = this.data.query.filters.splice(filterIndex, 1);
        // Reset the offset whenever the filters are updated.
        this.data.query.offset = 0;
        // dispatch events
        this.triggerEvent("filter", {
          type: "removeAll",
          property: property,
          removedFilters: removedFilterGroups[0].constraints,
        });
        this.updateEntries().then(resolve, reject);
      });
    },




  };





  // return the init function to be used in the layouts
  return init;

});



