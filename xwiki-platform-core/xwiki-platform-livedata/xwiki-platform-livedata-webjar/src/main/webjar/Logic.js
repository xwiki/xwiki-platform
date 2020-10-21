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
  XWikiLivedata,
) {




  /**
   * ---------------------------------------------------------------
   * UTILS
   */

  /**
   * The Utils class only contains static methods
   * The methods of Utils are ones that don't directly
   * have something to do with the Livedata
   */
  class Utils {

    /**
     * Compare two objects deeply, using SameValue comparison
     * @param {string} a A first object to compare
     * @param {string} b A second object to compare
     */
    static isDeepEqual (a, b) {
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
     * @param {string} item
     */
    static uniqueArrayHas (uniqueArray, item) {
      return uniqueArray.includes(item);
    }


    /**
     * Add the given item if not present in the array, or does nothing
     * @param {Array} uniqueArray An array of unique items
     * @param {string} item
     */
    static uniqueArrayAdd (uniqueArray, item) {
      if (Utils.uniqueArrayHas(uniqueArray, item)) { return; }
      uniqueArray.push(item);
    }


    /**
     * Remove the given item from the array if present, or does nothing
     * @param {Array} uniqueArray An array of unique items
     * @param {string} item
     */
    static uniqueArrayRemove (uniqueArray, item) {
      const index = uniqueArray.indexOf(item);
      if (index === -1) { return; }
      uniqueArray.splice(index, 1);
    }


    /**
     * Toggle the given item from the array, ensuring its uniqueness
     * @param {Array} uniqueArray An array of unique items
     * @param {string} item
     * @param {boolean} force Optional: true force add / false force remove
     */
    static uniqueArrayToggle (uniqueArray, item, force) {
      if (force === undefined) {
        force = !Utils.uniqueArrayHas(uniqueArray, item);
      }
      if (force) {
        Utils.uniqueArrayAdd(uniqueArray, item);
      } else {
        Utils.uniqueArrayRemove(uniqueArray, item);
      }
    }

  };





  /**
   * ---------------------------------------------------------------
   * LAYOUT
   */


  /**
   * Logic Layout Module
   */
  class LogicLayout {

    constructor (logic) {
      this.logic = logic;
      this.currentId = undefined;
    }


    /**
     * Return the list of available layout ids
     *
     * @returns {Object[]}
     */
    getIds () {
      return this.logic.config.meta.layouts.map(layoutDescriptor => layoutDescriptor.id);
    }


    /**
     * Return the layout descriptor corresponding to a layout id
     * @param {Object} [parameters]
     * @param {string} [parameters.propertyId] The id of the layout to get the descriptor of. If undefined use current layout.<s
     * @returns {(Object|undefined)}
     */
    getDescriptor ({ layoutId } = {}) {
      layoutId = layoutId || this.currentId;
      return this.logic.config.meta.layouts
        .find(layoutDescriptor => layoutDescriptor.id === layoutId);
    }


    /**
     * Load a layout, or default layout if none specified
     * @param {Object} parameters
     * @param {string} parameters.layoutId The id of the layout to load with requireJS
     * @returns {Promise}
     */
    change ({ layoutId }) {
      // bad layout
      if (!this.getDescriptor({ layoutId })) {
        console.error("Layout of id `" + layoutId + "` does not have a descriptor");
        return;
      }
      // set layout
      const previousLayoutId = this.currentId;
      this.currentId = layoutId;
      // dispatch events
      this.logic.event.trigger({
        name: "layoutChange",
        data: {
          layoutId: layoutId,
          previousLayoutId: previousLayoutId,
        },
      });
    }

  };




    /**
     * ---------------------------------------------------------------
     * EVENTS
     */


    /**
     * Logic Event Module
     */
    class LogicEvent {

      constructor (logic) {
        this.logic = logic;
      }


      /**
       * Send custom events
       * @param {Object} parameters
       * @param {string} parameters.name The name of the event, without the prefix "xwiki:livedata"
       * @param {Object} [parameters.data] The data associated with the event.
       *  The livedata object reference is automatically added
       */
      trigger ({ name, data }) {
        // configure event
        const defaultData = {
          livedata: this.logic,
        };
        name = "xwiki:livedata:" + name;
        data = {
          bubbles: true,
          detail: Object.assign(defaultData, data),
        };
        const event = new CustomEvent(name, data);
        // dispatch event
        this.logic.element.dispatchEvent(event);
      }


      /**
       * Listen for custom events
       * @param {Object} parameters
       * @param {string} parameters.name The name of the event, without the prefix "xwiki:livedata"
       * @param {Object|Function} [parameters.condition] The condition to execute the callback
       *  if Object, values of object properties must match e.detail properties values
       *  if Function, the function must return true. e.detail is passed as argument
       * @param {Function} parameters.callback Function to call we the event is triggered
       */
      on ({ name, condition, callback }) {
        name = "xwiki:livedata:" + name;
        this.logic.element.addEventListener(name, function (e) {

          // Verify condition if object
          if (typeof condition === "object") {
            const every = Object.keys(condition).every(key => condition[key] === e.detail[key]);
            if (!every) { return; }
          }

          // Verify condition if function
          if (typeof condition === "function") {
            if (!condition(e.detail)) { return; }
          }

          // Call callback
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
       * Need one of: entry | entryIndex
       * @param {Object} parameters
       * @param {Object} [parameters.entry]
       * @param {Object} [parameters.entryIndex] The index of the value. Negative value count from the end.
       * @returns {(string|undefined)}
       */
      getId ({ entry, entryIndex }) {
        if (!entry) {
          entry = this.logic.config.data.entries.slice(entryIndex)[0];
        }
        const idProperty = this.logic.config.meta.entryDescriptor.idProperty || "id";
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


      async update () {
        try {
          this.logic.config.data.entries = await this.fetch();
        } catch (err) {
          return console.error(err);
        }
      }


      add () {
        const mockNewUrl = () => this.logic.entries.getId({ entryIndex: -1 }) + "0";
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
          this.logic.config.data.entries.push(newEntry);
          this.logic.config.data.count++; // TODO: remove when merging with backend
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
       * @returns {number}
       */
      getPageCount () {
        return Math.ceil(this.logic.config.data.count / this.logic.config.query.limit);
      }


      /**
       * Get the page corresponding to the specified entry (0-based index)
       * @param {Object} [parameters]
       * @param {number} [parameters.entryIndex] The index of the entry. Uses current entry if undefined.
       * @returns {number}
       */
      getPageIndex ({ entryIndex } = {}) {
        if (entryIndex === undefined) {
          entryIndex = this.logic.config.query.offset;
        }
        return Math.floor(entryIndex / this.logic.config.query.limit);
      }


      /**
       * Set page index (0-based index), then fetch new data
       * @param {Object} parameters
       * @param {number} parameters.pageIndex
       * @returns {Promise}
       */
      setPageIndex ({ pageIndex }) {
        return new Promise ((resolve, reject) => {
          if (pageIndex < 0 || pageIndex >= this.getPageCount()) { return void reject(); }
          const previousPageIndex = this.getPageIndex();
          this.logic.config.query.offset = this.getFirstIndexOfPage({ pageIndex });
          this.logic.event.trigger({
            name: "pageChange",
            data: {
              pageIndex: pageIndex,
              previousPageIndex: previousPageIndex,
            },
          });
          // TODO: CALL FUNCTION TO FETCH NEW DATA HERE
          resolve();
        });
      }


      /**
       * Get the first entry index of the given page index
       * @param {Object} [parameters]
       * @param {number} [parameters.pageIndex] The page index. Uses current page if undefined.
       * @returns {number}
       */
      getFirstIndexOfPage ({ pageIndex } = {}) {
        if (pageIndex === undefined) {
          pageIndex = this.getPageIndex();
        }
        if (0 <= pageIndex && pageIndex < this.getPageCount()) {
          return pageIndex * this.logic.config.query.limit;
        } else {
          return -1;
        }
      }


      /**
       * Get the last entry index of the given page index
       * @param {Object} [parameters]
       * @param {number} [parameters.pageIndex] The page index. Uses current page if undefined.
       * @returns {number}
       */
      getLastIndexOfPage ({ pageIndex } = {}) {
        if (pageIndex === undefined) {
          pageIndex = this.getPageIndex();
        }
        if (0 <= pageIndex && pageIndex < this.getPageCount()) {
          return Math.min(this.getFirstIndexOfPage({ pageIndex }) + this.logic.config.query.limit, this.logic.config.data.count) - 1;
        } else {
          return -1;
        }
      }


      /**
       * Set the pagination page size, then fetch new data
       * @param {Object} parameters
       * @param {number} pageSize
       * @returns {Promise}
       */
      setPageSize ({ pageSize }) {
        return new Promise ((resolve, reject) => {
          if (pageSize < 0) { return void reject(); }
          const previousPageSize = this.logic.config.query.limit;
          if (pageSize === previousPageSize) { return void resolve(); }
          this.logic.config.query.limit = pageSize;
          this.logic.event.trigger({
            name: "pageSizeChange",
            data: {
              pageSize: pageSize,
              previousPageSize: previousPageSize,
            },
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
       * Return whether the specified property is valid
       * If given a property id, return whether the id exists in config
       * If given a property descriptor, return whether the given object is one of the config property descriptors
       *
       * @param {Object} parameters
       * Need one of: propertyId | propertyDescriptor
       * @param {string} [parameters.propertyId]
       * @param {Object} [parameters.propertyDescriptor]
       * @returns {boolean}
       */
      isValid ({ propertyId, propertyDescriptor }) {
        if (propertyId) {
          const isValid = this.logic.config.query.properties.includes(propertyId);
          if (!isValid) {
            // const err = new Error(`Error: property "${propertyId}" is not valid`);
            // console.err(err);
            return;
          }
        } else if (propertyDescriptor) {
          const isValid = this.logic.config.meta.propertyDescriptors.includes(propertyDescriptor);
          if (!isValid) {
            // const err = new Error(`Error: object "${propertyDescriptor}" is not a valid property descriptor`);
            // console.err(err);
            return;
          }
        } else {
            const err = new Error(`Error: no property where given to test`);
            return void console.error(err);
        }
        return true;
      }


      /**
       * Return whether the specified property type is valid
       * If given a property type, return whether the type exists in config
       * If given a type descriptor, return whether the given object is one of the config type descriptors
       *
       * @param {Object} parameters
       * Need one of: propertyId | typeDescriptor
       * @param {string} [parameters.propertyId]
       * @param {Object} [parameters.typeDescriptor]
       * @returns {boolean}
       */
      isTypeValid ({ propertyType, typeDescriptor }) {
        if (propertyType) {
          const isValid = this.logic.config.meta.propertyTypes
            .find(typeDescriptor => typeDescriptor.id === propertyType);
          if (!isValid) {
            // const err = new Error(`Error: property type "${propertyType}" is not valid`);
            // console.err(err);
            return;
          }
        } else if (typeDescriptor) {
          const isValid = this.logic.config.meta.propertyTypes.includes(typeDescriptor);
          if (!isValid) {
            // const err = new Error(`Error: object "${typeDescriptor}" is not a valid property descriptor`);
            // console.err(err);
            return;
          }
        } else {
            const err = new Error(`Error: no property type where given to test`);
            return void console.error(err);
        }
        return true;
      }


      /**
       * Return the property id corresponding to a property descriptor
       *
       * If propertyId is provided, returns it.
       * It is usefull when we need to get the propertyId,
       * whether we have the id or the descriptor:
       * `propertyId = getId({ propertyId, propertyDescriptor })`
       *
       * @param {Object} parameters
       * Need one of: propertyId | propertyDescriptor
       * @param {string} [parameters.propertyId]
       * @param {Object} [parameters.propertyDescriptor]
       * @returns {(string|undefined)}
       */
      getId ({ propertyId, propertyDescriptor }) {
        if (!propertyId && propertyDescriptor) {
          propertyId = propertyDescriptor.id;
        }
        if (this.isValid({ propertyId })){
          return propertyId;
        }
      }


      /**
       * Return the property descriptor corresponding to a property id
       *
       * If propertyDescriptor is provided, returns it.
       * It is usefull when we need to get the propertyDescriptor,
       * whether we have the id or the descriptor:
       * `propertyDescriptor = getDescriptor({ propertyId, propertyDescriptor })`
       *
       * @param {Object} parameters
       * Need one of: propertyId | propertyDescriptor
       * @param {string} [parameters.propertyId]
       * @param {Object} [parameters.propertyDescriptor]
       * @returns {(Object|undefined)}
       */
      getDescriptor ({ propertyId, propertyDescriptor }) {
        if (!propertyDescriptor) {
          propertyDescriptor = this.logic.config.meta.propertyDescriptors
          .find(propertyDescriptor => propertyDescriptor.id === propertyId);
        }
        if (this.isValid({ propertyDescriptor })) {
          return propertyDescriptor;
        }
      }


      /**
       * Return the type descriptor corresponding to a property type
       *
       * If typeDescriptor is provided, returns it.
       * It is usefull when we need to get the typeDescriptor,
       * whether we have the id or the descriptor:
       * `typeDescriptor = getTypeDescriptor({ propertyType, typeDescriptor })`
       *
       * @param {Object} parameters
       * Need of of: propertyId | propertyDescriptor | propertyType | typeDescriptor
       * @param {string} [parameters.propertyId]
       * @param {Object} [parameters.propertyDescriptor]
       * @param {string} [parameters.propertyType]
       * @param {string} [parameters.typeDescriptor]
       * @returns {(Object|undefined)}
       */
      getTypeDescriptor ({ propertyId, propertyDescriptor, propertyType, typeDescriptor }) {
        if (!typeDescriptor) {
          // If propertyType is undefined
          // get the type from the given propertyId or propertyDescriptor
          if (!propertyType) {
            propertyDescriptor = this.getDescriptor({ propertyId, propertyDescriptor }) || {};
            propertyType = propertyDescriptor.type;
          }
          // Find the typeDescriptor corresponding to the propertyType
          typeDescriptor = this.logic.config.meta.propertyTypes
            .find(typeDescriptor => typeDescriptor.id === propertyType);
        }
        if (this.isTypeValid({ typeDescriptor })) {
          return typeDescriptor;
        }
      }


      /**
       * Returns the property descriptors of displayable properties
       * @param {Object} [parameters]
       * @param {boolean} [parameters.filterable = false] Return only filterable properties
       * @param {boolean} [parameters.sortable = false] Return only sortable properties
       * @returns {Object[]}
       */
      getDescriptors ({ filterable = false, sortable = false } = {}) {
        let descriptors = this.logic.config.query.properties
          .map(propertyId => this.logic.properties.getDescriptor({ propertyId }));

        if (sortable) {
          descriptors = descriptors
            .filter(propertyDescriptor => this.isSortable({ propertyId: propertyDescriptor.id }));
        }
        if (filterable) {
          descriptors = descriptors
            .filter(propertyDescriptor => this.isFilterable({ propertyId: propertyDescriptor.id }));
        }

        return descriptors;
      }


      /**
       * Returns whether a certain property is visible
       * @param {Object} parameters
       * Need one of: propertyId | propertyDescriptor
       * @param {string} [parameters.propertyId]
       * @param {Object} [parameters.propertyDescriptor]
       * @returns {boolean}
       */
      isVisible ({ propertyId, propertyDescriptor }) {
        propertyDescriptor = this.getDescriptor({ propertyId, propertyDescriptor });
        return propertyDescriptor.visible;
      }


      /**
       * Toggle whether the given property should be visible
       * @param {Object} parameters
       * Need one of: propertyId | propertyDescriptor
       * @param {string} [parameters.propertyId]
       * @param {Object} [parameters.propertyDescriptor]
       * @param {boolean} [visible] Whether the property is visible. Undefined toggle current visibility.
       */
      toggleVisibility ({ propertyId, propertyDescriptor, visible }) {
        propertyDescriptor = this.getDescriptor({ propertyId, propertyDescriptor });
        if (visible === undefined) {
          visible = !propertyDescriptor.visible;
        }
        propertyDescriptor.visible = visible;
      }


      /**
       * Move a property to a certain index in the property order list
       * @param {Object} parameters
       * Need one of: propertyId | propertyDescriptor | fromIndex
       * @param {string} [parameters.propertyId]
       * @param {Object} [parameters.propertyDescriptor]
       * @param {number} [parameters.fromIndex] The index of the property to be moved
       * @param {number} toIndex
       */
      reorder ({ propertyId, propertyDescriptor, fromIndex, toIndex }) {
        if (fromIndex === undefined) {
          propertyId = this.getId({ propertyId, propertyDescriptor });
          fromIndex = this.logic.config.query.properties.indexOf(propertyId);
        }
        if (fromIndex === undefined || fromIndex <= -1 || toIndex <= -1) { return; }
        this.logic.config.query.properties.splice(toIndex, 0,
          this.logic.config.query.properties.splice(fromIndex, 1)[0]
        );
      }


      /**
       * Returns whether a certain property is sortable or not
       * @param {Object} parameters
       * Need of of: propertyId | propertyDescriptor
       * @param {string} [parameters.propertyId]
       * @param {Object} [parameters.propertyDescriptor]
       * @returns {boolean}
       */
      isSortable ({ propertyId, propertyDescriptor }) {
        propertyDescriptor = this.getDescriptor({ propertyId, propertyDescriptor });
        const typeDescriptor = this.getTypeDescriptor({ propertyId, propertyDescriptor });
        return propertyDescriptor.sortable !== undefined ?
          propertyDescriptor.sortable :
          typeDescriptor.sortable;
      }


      /**
       * Returns whether a certain property is filterable or not
       * @param {Object} parameters
       * Need of of: propertyId | propertyDescriptor
       * @param {string} [parameters.propertyId]
       * @param {Object} [parameters.propertyDescriptor]
       * @returns {boolean}
       */
      isFilterable ({ propertyId, propertyDescriptor }) {
        propertyDescriptor = this.getDescriptor({ propertyId, propertyDescriptor });
        const typeDescriptor = this.getTypeDescriptor({ propertyId, propertyDescriptor });
        return propertyDescriptor.filterable !== undefined ?
          propertyDescriptor.filterable :
          typeDescriptor.filterable;
      }


      /**
       * Set the type of the given property.
       * @param {Object} parameters
       * Need one of: propertyId | propertyDescriptor
       * @param {string} [parameters.propertyId]
       * @param {Object} [parameters.propertyDescriptor]
       * @param {string} type
       */
      setType ({ propertyId, propertyDescriptor, type }) {
        propertyDescriptor = this.getDescriptor({ propertyId, propertyDescriptor });
        if (!propertyDescriptor) {
          console.error("Could not set type of the given property");
        }
        propertyDescriptor.type = type;
      }


      /**
       * Set the displayer of the given property.
       * @param {Object} parameters
       * Need one of: propertyId | propertyDescriptor
       * @param {string} [parameters.propertyId]
       * @param {Object} [parameters.propertyDescriptor]
       * @param {(string|undefined)} [displayerId] The new displayerId. Takes default one if undefined.
       */
      setDisplayer ({ propertyId, propertyDescriptor, displayerId }) {
        propertyDescriptor = this.getDescriptor({ propertyId, propertyDescriptor });
        if (displayerId) {
          Vue.set(propertyDescriptor, "displayer", { id: displayerId });
        }
        else {
          Vue.delete(propertyDescriptor, "displayer");
        }
      }


      /**
       * Set the filter of the given property.
       * @param {Object} parameters
       * Need one of: propertyId | propertyDescriptor
       * @param {string} [parameters.propertyId]
       * @param {Object} [parameters.propertyDescriptor]
       * @param {(string|undefined)} [filterId] The new filterId. Takes default one if undefined.
       */
      setFilter ({ propertyId, propertyDescriptor, filterId }) {
        propertyDescriptor = this.getDescriptor({ propertyId, propertyDescriptor });
        if (filterId) {
          Vue.set(propertyDescriptor, "filter", { id: filterId });
        }
        else {
          Vue.delete(propertyDescriptor, "filter");
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
       * @param {Object} parameters
       * @param {Object} parameters.entry
       * @returns {boolean}
       */
      isSelected ({ entry }) {
        const entryId = this.logic.entries.getId({ entry });
        if (this.isGlobal) {
          return !Utils.uniqueArrayHas(this.deselected, entryId);
        } else {
          return Utils.uniqueArrayHas(this.selected, entryId);
        }
      }


      /**
       * Select the specified entries
       * @param {Object} parameters
       * Need one of: entry | entries
       * @param {(Object|Object[])} parameters.entry
       * @param {(Object|Object[])} parameters.entries
       */
      select ({ entry, entries }) {
        const entryArray = entries || [entry];
        entryArray.forEach(entry => {
          const entryId = this.logic.entries.getId({ entry });
          if (entryId === undefined) { return; }
          if (this.isGlobal) {
            Utils.uniqueArrayRemove(this.deselected, entryId);
          }
          else {
            Utils.uniqueArrayAdd(this.selected, entryId);
          }
          this.logic.event.trigger({
            name: "select",
            data: { entry },
          });
        });
      }


      /**
       * Deselect the specified entries
       * @param {Object} parameters
       * Need one of: entry | entries
       * @param {(Object|Object[])} parameters.entry
       * @param {(Object|Object[])} parameters.entries
       */
      deselect ({ entry, entries }) {
        const entryArray = entries || [entry];
        entryArray.forEach(entry => {
          const entryId = this.logic.entries.getId({ entry });
          if (this.isGlobal) {
            Utils.uniqueArrayAdd(this.deselected, entryId);
          }
          else {
            Utils.uniqueArrayRemove(this.selected, entryId);
          }
          this.logic.event.trigger({
            name: "deselect",
            data: { entry },
          });
        });
      }


      /**
       * Toggle the selection of the specified entries
       * @param {Object} parameters
       * Need one of: entry | entries
       * @param {(Object|Object[])} parameters.entry
       * @param {(Object|Object[])} parameters.entries
       * @param {boolean} [parameters.select] Whether to select or not the entries. Undefined toggle current state
       */
      toggleSelect ({ entry, entries, select }) {
        const entryArray = entries || [entry];
        entryArray.forEach(entry => {
          if (select === undefined) {
            select = !this.isSelected({ entry });
          }
          if (select) {
            this.select({ entry });
          } else {
            this.deselect({ entry });
          }
        });
      }


      /**
       * Get number of selected entries
       * @returns {number}
       */
      getCount () {
        if (this.isGlobal) {
          return this.logic.config.data.count - this.deselected.length;
        } else {
          return this.selected.length;
        }
      }


      /**
       * Set the entry selection globally accross pages
       * @param {Object} parameters
       * @param {boolean} parameter.global
       */
      setGlobal ({ global }) {
        this.isGlobal = global;
        this.selected.splice(0);
        this.deselected.splice(0);
        this.logic.event.trigger({
          name: "selectGlobal",
          data: { state: global },
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
       * Get the sort query associated to a property id
       * @param {Object} parameters
       * Need one of propertyId | propertyDescriptor
       * @param {string} [parameters.propertyId]
       * @param {string} [parameters.propertyDescriptor]
       */
      getConfig ({ propertyId, propertyDescriptor }) {
        propertyId = this.logic.properties.getId({ propertyId, propertyDescriptor });
        if (!propertyId) { return; }
        return this.logic.config.query.sort.find(sort => sort.property === propertyId);
      }


      /**
       * Update sort configuration based on parameters, then fetch new data
       * @param {Object} parameters
       * Need one of propertyId | propertyDescriptor
       * The property to sort according to
       * @param {string} [parameters.propertyId]
       * @param {string} [parameters.propertyDescriptor]
       *
       * @param {string} [parameters.level] The sort level for the property (0 is the highest).
       *   Undefined means keep current. Negative value removes property sort.
       * @param {string} [parameters.descending] Specify whether the sort should be descending or not.
       *   Undefined means toggle current direction
       * @returns {Promise}
       */
      sort ({ propertyId, propertyDescriptor, level, descending }) {
        const err = new Error(`Property "${propertyId}" is not sortable`);
        propertyId = this.logic.properties.getId({ propertyId, propertyDescriptor });
        if (!propertyId) { return Promise.reject(err); }
        if (!this.logic.properties.isSortable({ propertyId })) { return Promise.reject(err); }

        return new Promise ((resolve, reject) => {

          // find property current sort level
          const currentLevel = this.logic.config.query.sort
            .findIndex(sortObject => sortObject.property === propertyId);
          // default level
          if (level === undefined) {
            level = (currentLevel !== -1) ? currentLevel : 0;
          } else if (level < 0) {
            level = -1;
          }
          // default descending
          if (descending === undefined) {
            descending = (currentLevel !== -1) ?
              !this.logic.config.query.sort[currentLevel].descending :
              false;
          }

          // create sort object
          const sortObject = {
            property: propertyId,
            descending: descending,
          };

          // apply sort
          if (level !== -1) {
            this.logic.config.query.sort.splice(level, 1, sortObject);
          }
          if (currentLevel !== -1 && currentLevel !== level) {
            this.logic.config.query.sort.splice(currentLevel, 1);
          }

          // dispatch events
          this.logic.event.trigger({
            name: "sort",
            data: {
              propertyId,
              level,
              descending,
            },
          });

          // TODO: CALL FUNCTION TO FETCH NEW DATA HERE
          resolve();
        });
      }


      /**
       * Add new sort entry, shorthand of sort:
       * If the property is already sorting, does nothing
       * @param {Object} parameters
       * Need one of propertyId | propertyDescriptor
       * The property to sort according to
       * @param {string} [parameters.propertyId]
       * @param {string} [parameters.propertyDescriptor]
       * @param {string} [parameters.descending] Specify whether the sort should be descending or not.
       *   Undefined means toggle current direction
       * @returns {Promise}
       */
      add ({ propertyId, propertyDescriptor, descending }) {
        propertyId = this.logic.properties.getId({ propertyId, propertyDescriptor });
        const err = new Error(`Property "${propertyId}" is already sorting`);
        const propertyQuerySort = this.logic.config.query.sort.find(sortObject => sortObject.property === propertyId);
        if (propertyQuerySort) { return Promise.reject(err); }
        return this.sort({ propertyId, level: this.logic.config.query.sort.length, descending });
      }


      /**
       * Remove a sort entry, shorthand of sort:
       * @param {Object} parameters
       * Need one of propertyId | propertyDescriptor
       * The property to sort according to
       * @param {string} [parameters.propertyId]
       * @param {string} [parameters.propertyDescriptor]
       * @returns {Promise}
       */
      remove ({ propertyId, propertyDescriptor }) {
        return this.sort({ propertyId, propertyDescriptor, level: -1 });
      }


      /**
       * Move a sort entry to a certain index in the query sort list
       * @param {Object} parameters
       * Need one of propertyId | propertyDescriptor | fromIndex
       * The property to sort according to
       * @param {string} [parameters.propertyId]
       * @param {string} [parameters.propertyDescriptor]
       * @param {number} [parameters.fromIndex]
       * @param {number} [parameters.toIndex]
       */
      reorder ({ propertyId, propertyDescriptor, fromIndex, toIndex }) {
        propertyId = this.logic.properties.getId({ propertyId, propertyDescriptor });
        const err = new Error(`Property "${propertyId}" is not sortable`);
        if (!propertyId) { return Promise.reject(err); }
        fromIndex = this.logic.config.query.sort.findIndex(querySort => querySort.property === propertyId);
        if (fromIndex <= -1 || toIndex <= -1) { return Promise.reject(err); }

        return new Promise ((resolve, reject) => {
          this.logic.config.query.sort.splice(toIndex, 0, this.logic.config.query.sort.splice(fromIndex, 1)[0]);

          // dispatch events
          this.logic.event.trigger({
            name: "sort",
            data: {
              type: "move",
              propertyId,
              level: toIndex,
            },
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
       * Get the filter descriptor associated to a property, or a property type
       * If propertyId or propertyDescriptor is specified, returns the
       * filter descriptor inherited from the type one and the default one
       * If propertyType or typeDescriptor is specified, returns the
       * filter descriptor inherited from the default one
       * @param {Object} parameters
       * Need of of: propertyId | propertyDescriptor | propertyType | typeDescriptor
       * @param {string} [parameters.propertyId]
       * @param {Object} [parameters.propertyDescriptor]
       * @param {string} [parameters.propertyType]
       * @param {string} [parameters.typeDescriptor]
       * @returns {Object}
       */
      getDescriptor ({ propertyId, propertyDescriptor, propertyType, typeDescriptor }) {
        if (!propertyType && !typeDescriptor) {
          propertyDescriptor = this.logic.properties.getDescriptor({ propertyId, propertyDescriptor });
        }
        typeDescriptor = this.logic.properties.getTypeDescriptor({ propertyId, propertyDescriptor, propertyType, typeDescriptor });

        // Merge property filter with type filter (if their id correspond)
        const propertyFilterDescriptor = (propertyDescriptor || {}).filter || {};
        const typeFilterDescriptor = (typeDescriptor || {}).filter || {};
        let mergedFilterDescriptor;
        if (!propertyFilterDescriptor.id || propertyFilterDescriptor.id === typeFilterDescriptor.id) {
          mergedFilterDescriptor = Object.assign({}, typeFilterDescriptor, propertyFilterDescriptor);
        } else {
          mergedFilterDescriptor = Object.assign({}, propertyFilterDescriptor);
        }

        // Merge with default config
        const filterId = mergedFilterDescriptor.id;
        const filter = this.logic.config.meta.filters.find(filter => filter.id === filterId);
        // merge filters
        if (mergedFilterDescriptor.id) {
          return Object.assign({}, filter, mergedFilterDescriptor);
        } else {
          // default filter
          return { id: this.logic.config.meta.defaultFilter };
        }
      }


      /**
       * Get the filter object in the query config object associated to a property id
       * @param {Object} parameters
       * Need of of: propertyId | propertyDescriptor
       * @param {string} [parameters.propertyId]
       * @param {Object} [parameters.propertyDescriptor]
       * @param {Object} [parameters.constrainsOnly] Only return the constrains array instead of the whole object
       * @returns {Object}
       */
      getConfig ({ propertyId, propertyDescriptor, constrainsOnly = false }) {
        propertyId = this.logic.properties.getId({ propertyId, propertyDescriptor });
        if (!propertyId) { return; }
        const filterGroup = this.logic.config.query.filters
          .find(filter => filter.property === propertyId);
        if (constrainsOnly) {
          return (filterGroup || {}).constrains || [];
        }
        return filterGroup;
      }


      /**
       * Get the default filter operator associated to a property id
       * @param {Object} parameters
       * Need of of: propertyId | propertyDescriptor
       * @param {string} [parameters.propertyId]
       * @param {Object} [parameters.propertyDescriptor]
       * @returns {string}
       */
      getDefaultOperator ({ propertyId, propertyDescriptor}) {
        // get valid operator descriptor
        const filterDescriptor = this.getDescriptor({ propertyId, propertyDescriptor });
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
       * @param {Object} parameters
       * Need of of: propertyId | propertyDescriptor
       * @param {string} [parameters.propertyId]
       * @param {Object} [parameters.propertyDescriptor]
       * @param {string} [parameters.index] The index of the filter entry
       * @param {string} [parameters.filterEntry] The filter data used to update the filter configuration
       *  (see Logic.prototype.filter for more)
       * @returns {(Object|undefined)} {oldEntry, newEntry}
       *  with oldEntry / newEntry being {property, index, operator, value}
       * Returns undefined if the given parameters are invalid
       */
      _computeFilterEntries ({ propertyId, propertyDescriptor, index, filterEntry = {} }) {
        propertyId = this.logic.properties.getId({ propertyId, propertyDescriptor });
        if (!propertyId) { return; }
        if (!this.logic.properties.isFilterable({ propertyId })) { return; }
        // default indexes
        index = index || 0;
        if (index < 0) { index = -1; }
        if (filterEntry.index < 0) { filterEntry.index = -1; }
        // old entry
        let oldEntry = {
          propertyId,
          index,
        };
        const configConstrains = this.getConfig({ propertyId, constrainsOnly: true });
        const currentEntry = configConstrains[index] || {};
        oldEntry = Object.assign({}, currentEntry, oldEntry);
        // new entry
        let newEntry = filterEntry || {};
        const self = this;
        const defaultEntry = {
          propertyId,
          value: "",
          operator: self.getDefaultOperator({ propertyId }),
          index: 0,
        };
        newEntry = Object.assign({}, defaultEntry, oldEntry, newEntry);
        newEntry.operator = this.getDefaultOperator({ propertyId: newEntry.propertyId });
        return {
          oldEntry,
          newEntry,
        };
      }


      /**
       * Return the filtering type, based on oldEntry and newEntry
       * @param {Object} parameters
       * Need of of: propertyId | propertyDescriptor
       * @param {Object} [parameters.oldEntry]
       * @param {Object} [parameters.newEntry]
       * @returns {string} One of: "add" | "remove" | "move" | "modify"
       */
      _getFilteringType ({ oldEntry, newEntry }) {
        const queryFilter = this.getConfig({ propertyId: oldEntry.propertyId });
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
       * @param {Object} parameters
       * Need of of: propertyId | propertyDescriptor
       * The property to filter according to
       * @param {string} [parameters.propertyId]
       * @param {Object} [parameters.propertyDescriptor]
       * @param {string} [parameters.index] The index of the filter entry
       * @param {string} [parameters.filterEntry] The filter data used to update the filter configuration
       *  filterEntry = {property, operator, value}
       *  undefined values are defaulted to current values, then to default values.
       * @param {string} [parameters.filterEntry.property] The new property to filter according to
       * @param {string} [parameters.filterEntry.index] The new index the filter should go. -1 delete filter
       * @param {string} [parameters.filterEntry.operator] The operator of the filter.
       *  Should match the filter descriptor of the filter property
       * @param {string} [parameters.filterEntry.value] Value for the new filter entry
       * @returns {Promise}
       */
      filter ({ propertyId, propertyDescriptor, index, filterEntry }) {
        propertyId = this.logic.properties.getId({ propertyId, propertyDescriptor });
        const err = new Error(`Property "${propertyId}" is not filterable`);
        const filterEntries = this._computeFilterEntries({ propertyId, index, filterEntry });
        if (!filterEntries) { return void Promise.reject(err); }

        return new Promise ((resolve, reject) => {
          const oldEntry = filterEntries.oldEntry;
          const newEntry = filterEntries.newEntry;
          const filteringType = this._getFilteringType({ oldEntry, newEntry });
          // remove filter at current property and index
          if (oldEntry.index !== -1) {
            this.getConfig({
              propertyId: oldEntry.propertyId,
              constrainsOnly: true
            })
              .splice(index, 1);
          }
          // add filter at new property and index
          if (newEntry.index !== -1) {
            // create filterGroup if not exists
            if (!this.getConfig({ propertyId: newEntry.propertyId })) {
              this.logic.config.query.filters.push({
                property: newEntry.propertyId,
                matchAll: true,
                constrains: [],
              });
            }
            // add entry
            this.getConfig({ propertyId: newEntry.propertyId }).constrains.splice(newEntry.index, 0, {
              operator: newEntry.operator,
              value: newEntry.value,
            });
          }
          // remove filter group if empty
          if (this.getConfig({
            propertyId: oldEntry.propertyId,
            constrainsOnly: true,
          }).length === 0) {
            this.removeAll({ propertyId: oldEntry.propertyId });
          }
          // dispatch events
          this.logic.event.trigger({
            name: "filter",
            data: {
              type: filteringType,
              oldEntry: oldEntry,
              newEntry: newEntry,
            },
          });

          // TODO: CALL FUNCTION TO FETCH NEW DATA HERE
          resolve();
        });
      }


      /**
       * Add new filter entry, shorthand of filter:
       * @param {Object} parameters
       * Need of of: propertyId | propertyDescriptor
       * The property to filter according to
       * @param {string} [parameters.propertyId]
       * @param {Object} [parameters.propertyDescriptor]
       * @param {string} [parameters.operator] The operator of the filter. Should match the filter descriptor of the property
       * @param {string} [parameters.value] Default value for the new filter entry
       * @param {number} [parameters.index] Index of new filter entry. Undefined means last
       * @returns {Promise}
       */
      add ({ propertyId, propertyDescriptor, operator, value, index }) {
        propertyId = this.logic.properties.getId({ propertyId, propertyDescriptor });
        if (index === undefined) {
          index = ((this.getConfig({ propertyId }) || {}).constrains || []).length;
        }
        return this.filter({
          propertyId,
          index: -1,
          filterEntry: {
            propertyId,
            operator,
            value,
            index,
          },
        });
      }


      /**
       * Remove a filter entry in the configuration, then fetch new data
       * @param {Object} parameters
       * Need of of: propertyId | propertyDescriptor
       * The property to filter according to
       * @param {string} [parameters.propertyId]
       * @param {Object} [parameters.propertyDescriptor]
       * @param {string} [parameters.index] The index of the filter to remove. Undefined means last.
       * @returns {Promise}
       */
      remove ({ propertyId, propertyDescriptor, index }) {
        return this.filter({
          propertyId,
          propertyDescriptor,
          index,
          filterEntry: { index: -1 },
        });
      }


      /**
       * Remove all the filters associated to a property
       * @param {Object} parameters
       * Need of of: propertyId | propertyDescriptor
       * The property to filter according to
       * @param {string} [parameters.propertyId]
       * @param {Object} [parameters.propertyDescriptor]
       * @returns {Promise}
       */
      removeAll ({ propertyId, propertyDescriptor }) {
        propertyId = this.logic.properties.getId({ propertyId, propertyDescriptor });
        if (!propertyId) { return; }
        if (!this.logic.properties.isValid({ propertyId })) { return; }
        const filterIndex = this.logic.config.query.filters
          .findIndex(filterGroup => filterGroup.property === propertyId);
        if (filterIndex === -1) { return void Promise.reject(); }

        return new Promise ((resolve, reject) => {
          const removedFilterGroups = this.logic.config.query.filters.splice(filterIndex, 1);

          // dispatch events
          this.logic.event.trigger({
            name: "filter",
            data: {
              type: "removeAll",
              propertyId,
              removedFilters: removedFilterGroups[0].constrains,
            },
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
       * Get the displayer descriptor associated to a property, or a property type
       * If propertyId or propertyDescriptor is specified, returns the
       * displayer descriptor inherited from the type one and the default one
       * If propertyType or typeDescriptor is specified, returns the
       * displayer descriptor inherited from the default one
       * @param {Object} parameters
       * Need of of: propertyId | propertyDescriptor | propertyType | typeDescriptor
       * @param {string} [parameters.propertyId]
       * @param {Object} [parameters.propertyDescriptor]
       * @param {string} [parameters.propertyType]
       * @param {string} [parameters.typeDescriptor]
       * @returns {Object}
       */
      getDescriptor ({ propertyId, propertyDescriptor, propertyType, typeDescriptor }) {
        if (!propertyType && !typeDescriptor) {
          propertyDescriptor = this.logic.properties.getDescriptor({ propertyId, propertyDescriptor });
        }
        typeDescriptor = this.logic.properties.getTypeDescriptor({ propertyId, propertyDescriptor, propertyType, typeDescriptor });

        // Merge property displayer with type displayer (if their id correspond)
        const propertyDisplayerDescriptor = (propertyDescriptor || {}).displayer || {};
        const typeDisplayerDescriptor = (typeDescriptor || {}).displayer || {};
        let mergedDisplayerDescriptor;
        if (!propertyDisplayerDescriptor.id || propertyDisplayerDescriptor.id === typeDisplayerDescriptor.id) {
          mergedDisplayerDescriptor = Object.assign({}, typeDisplayerDescriptor, propertyDisplayerDescriptor);
        } else {
          mergedDisplayerDescriptor = Object.assign({}, propertyDisplayerDescriptor);
        }

        // Merge with default config
        const displayerId = mergedDisplayerDescriptor.id;
        const displayer = this.logic.config.meta.displayers.find(displayer => displayer.id === displayerId);
        if (mergedDisplayerDescriptor.id) {
          return Object.assign({}, displayer, mergedDisplayerDescriptor);
        } else {
          // default displayer
          return { id: this.logic.config.meta.defaultDisplayer };
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
      }


      /**
       * Return temporary config at given key if config is a string
       * or return directly the given config if it is valid
       * @param {Object} parameters
       * Need one of: configName | config
       * @param {string} [parameters.configName] The key of an already saved config
       * @param {Object} [parameters.config] A saved config object
       * @returns {Object}
       */
      _getConfig ({ configName, config }) {
        if (!config) {
          config = this.dataSaves[configName];
          if (!config) {
            return void console.error(`Error: temporary config at key "${configName}" can't be found`);
          }
        }
        // Check config validity
        if (typeof config !== "object") {
          return void console.error("Error: config object is invalid");
        }
        return config;
      }


      /**
       * Format a config object to only keep query and meta properties
       * JSON.parse(JSON.stringify()) is used to ensure that
       * the saved config is not reactive to further changes
       * @param {Object} parameters
       * @param {Object} parameters.config A saved config object
       */
      _format ({ config }) {
        return {
          query: JSON.parse(JSON.stringify(config.query)),
          meta: JSON.parse(JSON.stringify(config.meta)),
        };
      }


      /**
       * Save current or given Livedata configuration to a temporary state
       * This state could be reloaded later using the temporaryConfig.load method
       * @param {Object} parameters
       * @param {string} parameters.configName The key where to save the config
       * @param {Object} parameters.config A config object to save. Undefined use current config
       */
      save ({ configName, config }) {
        config = config || this.logic.config;
        const configToSave = this._format({ config });
        this.dataSaves[configName] = configToSave;
      }


      /**
       * Replace current config with the given one
       * @param {Object} parameters
       * Need one of: configName | config
       * @param {string} [parameters.configName] The key of an already saved config
       * @param {Object} [parameters.config] A saved config object
       */
      load ({ configName, config }) {
        config = this._getConfig({ configName, config });
        if (!config) { return; }
        this.logic.config.query = config.query;
        this.logic.config.meta = config.meta;
      }


      /**
       * Return whether the given config is equal (deeply) to the current config
       * @param {Object} parameters
       * Need one of: configName | config
       * @param {string} [parameters.configName] The key of an already saved config
       * @param {Object} [parameters.config] A saved config object
       * @returns {boolean}
       */
      equals ({ configName, config }) {
        config = this._getConfig({ configName, config });
        if (!config) { return; }
        return Utils.isDeepEqual(
          this._format({ config }),
          this._format({ config: this.logic.config })
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
       * @param {Object} [parameters]
       * @param {boolean} [parameters.on] true to toggle design mode ON, false for OFF.
       * undefined toggle current state
       * @returns {Promise}
       */
      toggle ({ on }) {
        if (on === undefined) {
          on = !this.activated;
        }
        if (on) {
          this.activated = true;
          this.logic.temporaryConfig.save({ configName: "edit" });
        } else {
          this.activated = false;
          this.logic.temporaryConfig.load({ configName: "edit" });
        }
      }

    };




    /**
     * ---------------------------------------------------------------
     * PANELS
     */


    class LogicPanels {

      constructor (logic) {
        this.logic = logic;
        // The array of opened panels
        this.opened = [];
      }
      /**
       * Return whether the given panel is opened
       * @param {Object} parameters
       * @param {string} parameters.panelId
       */
      isOpened ({ panelId }) {
        return Utils.uniqueArrayHas(this.opened, panelId);
      }


      /**
       * Open the given panel if not already opened
       * @param {Object} parameters
       * @param {string} parameters.panelId
       */
      open ({ panelId }) {
        Utils.uniqueArrayAdd(this.opened, panelId);
      }


      /**
       * Close the given panel if not already closed
       * @param {Object} parameters
       * @param {string} parameters.panelId
       */
      close ({ panelId }) {
        Utils.uniqueArrayRemove(this.opened, panelId);
      }


      /**
       * Open / close the given panel according to its current state
       * @param {Object} parameters
       * @param {string} parameters.panelId
       * @param {boolean} [force] true: open, false: close
       */
      toggle ({ panelId, force }) {
        Utils.uniqueArrayToggle(this.opened, panelId);
      }


      /**
       * Close all panels
       */
      closeAll () {
        this.opened = [];
      }

    };




  class Logic {

    constructor (element) {
      this.element = element;

      // The element where the Livedata vue component is mounted
      this.element = element;

      // The Livedata configuration object
      this.config = JSON.parse(element.getAttribute("data-config") || "{}");
      element.removeAttribute("data-config");

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
      this.panels = new LogicPanels(this);

      this.layout.change({ layoutId: this.config.meta.defaultLayout });
      this.temporaryConfig.save({ configName: "initial" });

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

  };


  return Logic;

});



