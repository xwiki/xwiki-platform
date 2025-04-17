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


define('xwiki-livedata', [
  "vue",
  "vue-i18n",
  "xwiki-livedata-vue",
  "xwiki-livedata-source",
  "xwiki-json-merge",
  "edit-bus",
  "xwiki-livedata-polyfills"
], function(
  Vue,
  VueI18n,
  xwikiLivedataVue,
  liveDataSourceModule,
  jsonMerge,
  editBus
) {
  const XWikiLivedata = xwikiLivedataVue.XWikiLivedata;
  /**
   * Make vue use the i18n plugin
   */
  Vue.use(VueI18n);


  /**
   * Map the element to its data object
   * So that each instance of the livedata on the page handle there own data
   */
  const instancesMap = new WeakMap();



  /**
   * The init function of the logic script
   * For each livedata element on the page, returns its corresponding data / API
   * If the data does not exist yet, create it from the element
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
   * A service providing footnotes related operations. 
   */
  class FootnotesService {
    constructor() {
      this._footnotes = [];
    }

    /**
     * Register a new footnote. If a footnote with the same translationKey is already registered this method has no
     * effect on the list of registered footnotes.
     * @param symbol the symbol to identify the entries related to the footnote 
     * @param translationKey the translation key of the footnote text 
     */
    put(symbol, translationKey) {
      if (!this._footnotes.some(footnote => footnote.translationKey === translationKey)) {
        this._footnotes.push({symbol, translationKey});
      }
    }
    
    reset() {
      this._footnotes.splice(0);
    }
    
    list() {
      return this._footnotes;
    }
  }

  // Initializes a promise to be resolved once the translations loading is done.
  var translationsLoadedResolve;
  const translationsLoaded = new Promise((resolve) => {
    translationsLoadedResolve = resolve;
  });

  /**
   * Class for a logic element
   * Contains the Livedata data object and methods to mutate it
   * Can be used in the layouts to display the data, and call its API
   * @param {HTMLElement} element The HTML Element corresponding to the Livedata
   */
  const Logic = function (element) {
    // Make sure to have one Live Data source instance per Live Data instance. 
    this.liveDataSource = liveDataSourceModule.init();
    this.element = element;
    this.data = JSON.parse(element.getAttribute("data-config") || "{}");
    this.contentTrusted = element.getAttribute("data-config-content-trusted") === "true"; 
    this.data.entries = Object.freeze(this.data.entries);

    // Reactive properties must be initialized before Vue is instantiated.
    this.firstEntriesLoading = true;
    this.currentLayoutId = "";
    this.changeLayout(this.data.meta.defaultLayout);
    this.entrySelection = {
      selected: [],
      deselected: [],
      isGlobal: false,
    };
    this.openedPanels = [];
    this.footnotes = new FootnotesService();
    this.panels = [];

    element.removeAttribute("data-config");

    const locale = document.documentElement.getAttribute('lang');

    const i18n = new VueI18n({
      locale: locale,
      messages: {},
      silentFallbackWarn: true,
    });

    // Vue.js replaces the container - prevent this by creating a placeholder for Vue.js to replace.
    const placeholderElement = document.createElement('div');
    this.element.appendChild(placeholderElement);

    // create Vuejs instance
    const vue = new Vue({
      el: placeholderElement,
      components: {
        "XWikiLivedata": XWikiLivedata,
      },
      template: "<XWikiLivedata :logic='logic'/>",
      i18n: i18n,
      data: {
        logic: this
      },
      mounted()
      {
        element.classList.remove('loading');
        // Trigger the "instanceCreated" event on the next tick to ensure that the constructor has returned and thus
        // all references to the logic instance have been initialized.
        this.$nextTick(function () {
          this.logic.triggerEvent('instanceCreated', {});
        });
      }
    });

    // Fetch the data if we don't have any. This call must be made just after the main Vue component is initialized as 
    // LivedataPersistentConfiguration must be mounted for the persisted filters to be loaded and applied when fetching 
    // the entries.
    // We use a dedicated field (firstEntriesLoading) for the first load as the fetch start/end events can be triggered 
    // before the loader components is loaded (and in this case the loader is never hidden even once the entries are
    // displayed).
    if (!this.data.data.entries.length) {
      this.updateEntries()
        // Mark the loader as finished, even if it fails as the loader should stop and a message be displayed to the 
        // user in this case.
        .finally(() => this.firstEntriesLoading = false);
    } else {
      this.firstEntriesLoading = false;
    }

    this.setEditBus(editBus.init(this));

    /**
     * Load given translations from the server
     *
     * @param {object} parameters
     * @param {string} componentName The name component who needs the translations
     * Used to avoid loading the same translations several times
     * @param {string} prefix The translation keys prefix
     * @param {string[]} keys
     */
    this.loadTranslations = async function ({ componentName, prefix, keys }) {
      // If translations were already loaded, return.
      if (this.loadTranslations[componentName]) return;
      this.loadTranslations[componentName] = true;
      // Fetch translation and load them.
      try {
        const translations = await this.liveDataSource.getTranslations(locale, prefix, keys);
        i18n.mergeLocaleMessage(locale, translations)
      } catch (error) {
        console.error(error);
      }
    }

    // Load needed translations for the Livedata
    const translationsPromise = this.loadTranslations({
      prefix: "livedata.",
      keys: [
        "dropdownMenu.title",
        "dropdownMenu.actions",
        "dropdownMenu.layouts",
        "dropdownMenu.panels",
        "dropdownMenu.panels.properties",
        "dropdownMenu.panels.sort",
        "dropdownMenu.panels.filter",
        "selection.selectInAllPages",
        "selection.infoBar.selectedCount",
        "selection.infoBar.allSelected",
        "selection.infoBar.allSelectedBut",
        "pagination.label",
        "pagination.label.empty",
        "pagination.currentEntries",
        "pagination.pageSize",
        "pagination.selectPageSize",
        "pagination.page",
        "pagination.first",
        "pagination.previous",
        "pagination.next",
        "pagination.last",
        "action.refresh",
        "action.addEntry",
        "action.columnName.sortable.hint",
        "action.columnName.default.hint",
        "action.resizeColumn.hint",
        "panel.filter.title",
        "panel.filter.noneFilterable",
        "panel.filter.addConstraint",
        "panel.filter.addProperty",
        "panel.filter.delete",
        "panel.filter.deleteAll",
        "panel.properties.title",
        "panel.sort.title",
        "panel.sort.noneSortable",
        "panel.sort.direction.ascending",
        "panel.sort.direction.descending",
        "panel.sort.add",
        "panel.sort.delete",
        "displayer.emptyValue",
        "displayer.link.noValue",
        "displayer.boolean.true",
        "displayer.boolean.false",
        "displayer.xObjectProperty.missingDocumentName.errorMessage",
        "displayer.xObjectProperty.failedToRetrieveField.errorMessage",
        "displayer.actions.edit",
        "displayer.actions.followLink",
        "filter.boolean.label",
        "filter.date.label",
        "filter.list.label",
        "filter.list.emptyLabel",
        "filter.number.label",
        "filter.text.label",
        "footnotes.computedTitle",
        "footnotes.propertyNotViewable",
        "bottombar.noEntries",
        "error.updateEntriesFailed"
      ],
    }).then(() => {
      translationsLoadedResolve(true);
    });

    // Return a translation only once the translations have been loaded from the server.
    this.translate = async (key, ...args) => {
      // Make sure that the translations are loaded from the server before translating.
      await translationsPromise;
      return vue.$t(key, args);
    }
    
    // Waits for the translations to be loaded before continuing.
    this.translationsLoaded = async() => {
      await translationsPromise;
    }

    // Registers panels once the translations have been loadded as they are otherwise hard to update.
    this.translationsLoaded().finally(() => {
      this.registerPanel({
        id: 'propertiesPanel',
        title: vue.$t('livedata.panel.properties.title'),
        name: vue.$t('livedata.dropdownMenu.panels.properties'),
        icon: 'list-bullets',
        component: 'LivedataAdvancedPanelProperties',
        order: 1000
      });
      this.registerPanel({
        id: 'sortPanel',
        title: vue.$t('livedata.panel.sort.title'),
        name: vue.$t('livedata.dropdownMenu.panels.sort'),
        icon: 'table-sort',
        component: 'LivedataAdvancedPanelSort',
        order: 2000
      });
      this.registerPanel({
        id: 'filterPanel',
        title: vue.$t('livedata.panel.filter.title'),
        name: vue.$t('livedata.dropdownMenu.panels.filter'),
        icon: 'filter',
        component: 'LivedataAdvancedPanelFilter',
        order: 3000
      });
    });
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
     * @param {Function} callback Function to call we the event is triggered: e => { ... }
     */
    onEvent (eventName, callback) {
      eventName = "xwiki:livedata:" + eventName;
      this.element.addEventListener(eventName, function (e) {
        callback(e);
      });
    },


    /**
     * Listen for custom events, mathching certain conditions
     * @param {String} eventName The name of the event, without the prefix "xwiki:livedata"
     * @param {Object|Function} condition The condition to execute the callback
     *  if Object, values of object properties must match e.detail properties values
     *  if Function, the function must return true. e.detail is passed as argument
     * @param {Function} callback Function to call we the event is triggered: e => { ... }
     */
    onEventWhere (eventName, condition, callback) {
      eventName = "xwiki:livedata:" + eventName;
      this.element.addEventListener(eventName, function (e) {
        // Object check
        if (typeof condition === "object") {
          const isDetailMatching = (data, detail) => Object.keys(data).every(key => {
            return typeof data[key] === "object"
              ? isDetailMatching(data[key], detail?.[key])
              : Object.is(data[key], detail?.[key]);
          });
          if (!isDetailMatching(condition, e.detail)) { return; }
        }
        // Function check
        if (typeof condition === "function") {
          if (!condition(e.detail)) { return; }
        }
        // call callback
        callback(e);
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
        console.error("Property descriptor of property `" + propertyId + "` does not exist");
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
      // Property descriptor config
      const propertyDescriptor = this.getPropertyDescriptor(propertyId);
      // Property type descriptor config
      const typeDescriptor = this.getPropertyTypeDescriptor(propertyId);
      // Merge the property and type displayer descriptors.
      const customDisplayerDescriptor = jsonMerge({}, typeDescriptor?.displayer, propertyDescriptor?.displayer);
      // Get the default displayer descriptor.
      const displayerId = customDisplayerDescriptor.id || this.data.meta.defaultDisplayer;
      const defaultDisplayerDescriptor = this.data.meta.displayers.find(displayer => displayer.id === displayerId);
      // Merge displayer descriptors.
      return jsonMerge({}, defaultDisplayerDescriptor, customDisplayerDescriptor);
    },


    /**
     * Get the filter descriptor associated to a property id
     * @param {String} propertyId
     * @returns {Object}
     */
    getFilterDescriptor(propertyId) {
      // Property descriptor config
      const propertyDescriptor = this.getPropertyDescriptor(propertyId);
      // Property type descriptor config
      const typeDescriptor = this.getPropertyTypeDescriptor(propertyId);
      // Merge the property and type filter descriptors.
      const customFilterDescriptor = jsonMerge({}, typeDescriptor?.filter, propertyDescriptor?.filter);
      // Get the default filter descriptor.
      const filterId = customFilterDescriptor.id || this.data.meta.defaultFilter;
      const defaultFilterDescriptor = this.data.meta.filters.find(filter => filter.id === filterId);
      // Merge filter descriptors.
      return jsonMerge({}, defaultFilterDescriptor, customFilterDescriptor);
    },




    /**
     * ---------------------------------------------------------------
     * LAYOUT
     */


    /**
     * Fetch the entries of the current page according to the query configuration.
     * @returns the fetched entries
     */
    fetchEntries() {
      // Before fetch event
      this.triggerEvent("beforeEntryFetch");
      // Fetch entries from data source
      return this.liveDataSource.getEntries(this.data.query)
        .then(data => {
          // After fetch event
          return data
        })
        .finally(() => this.triggerEvent("afterEntryFetch"));
    },


    updateEntries () {
      return this.fetchEntries()
        .then(data => {
          this.data.data = Object.freeze(data);
          Vue.nextTick(() => this.triggerEvent('entriesUpdated', {}));
          // Remove the outdated footnotes, they will be recomputed by the new entries.
          this.footnotes.reset()
        })
        .catch(err => {
          // Prevent undesired notifications of the end user for non business related errors (for instance, the user
          // left the page before the request was completed).
          // See https://developer.mozilla.org/en-US/docs/Web/API/XMLHttpRequest/readyState
          if (err.readyState === 4) {
            this.translate('livedata.error.updateEntriesFailed')
              .then(value => new XWiki.widgets.Notification(value, 'error'));
          }
          
          // Do not log if the request has been aborted (e.g., because a second request was started for the same LD with
          // new criteria).
          if(err.statusText !== 'abort') {
            console.error('Failed to fetch the entries', err);
          }
        });
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
      if (!this.data.meta.actions.find(action => action.id === "edit")) {
        return false;
      }

      // Check if we are allowed to edit the given entry.
      if (entry && !this.isEntryEditable(entry)) {
        return false;
      }

      // Check if the specified property is editable.
      return !propertyId || this.isPropertyEditable(propertyId);
    },

    /**
     * Returns whether the given entry is editable or not.
     *
     * @param {Object} entry
     * @returns {Boolean}
     */
    isEntryEditable (entry) {
      return this.isActionAllowed('edit', entry);
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
      return propertyDescriptor && (propertyDescriptor.editable !== undefined ? propertyDescriptor.editable :
        (propertyTypeDescriptor && propertyTypeDescriptor.editable));
    },

    /**
     * Set the value of the given entry property
     * @param {Object} parameters
     * @param {Object} parameters.entry The entry we want to modify
     * @param {number} parameters.propertyId The property id we want to modify in the entry
     * @param {string} parameters.value The new value of entry property
     */
    setValue({entry, propertyId, value}) {
      // TODO: Ensure entry is valid (need other current PR)
      // TODO: Ensure property is valid (need other current PR)
      if (!this.isEditable({entry, propertyId})) {
        return;
      }
      entry[propertyId] = value;
      const source = this.data.query.source;
      const entryId = this.getEntryId(entry);
      // Once the entry updated, reload the whole livedata because changing a single entry can have an impact on other 
      // properties of the entry, but also possibly on other entriers, or in the way they are sorted.
      this.liveDataSource.updateEntryProperty(source, entryId, propertyId, entry[propertyId])
        .then(() => this.updateEntries());
    },

    /**
     * Update the entry with the values object passed in parameter and s
     * @param {Object} entry the current entry
     * @param {Object} values the entry's values to update
     */
    setValues({entryId, values}) {
      const source = this.data.query.source;
      return this.liveDataSource.updateEntry(source, entryId, values)
        .then(() => this.updateEntries());

    },


    /**
     * Return whether adding new entries is enabled.
     */
    canAddEntry () {
      // Check if the add entry action is available.
      return this.data.meta.actions.find(action => action.id === "addEntry");
    },

    addEntry () {
      if (!this.canAddEntry()) { return; }
      const mockNewUrl = () => this.getEntryId(this.data.data.entries.slice(-1)[0]) + "0";
      // TODO: CALL FUNCTION TO CREATE NEW DATA HERE
      Promise.resolve({ /* MOCK DATA */
        "doc_url": mockNewUrl(),
        "doc_name": undefined,
        "doc_date": "1585311660000",
        "doc_title": undefined,
        "doc_author": "Author 1",
        "doc_creationDate": "1585311660000",
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
        fromIndex = this.data.query.properties.indexOf(from);
      } else {
        return;
      }
      if (fromIndex < 0 || toIndex < 0) { return; }
      this.data.query.properties.splice(toIndex, 0, this.data.query.properties.splice(fromIndex, 1)[0]);
    },




    /**
     * ---------------------------------------------------------------
     * SELECTION
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
     * ACTIONS
     */


    /**
     * @param {String|Object} specifies the action
     * @returns {Object} the descriptor of the specified live data action
     */
    getActionDescriptor(action) {
      const descriptor = typeof action === 'string' ? {id: action} : action;
      const baseDescriptor = this.data.meta.actions.find(baseDescriptor => baseDescriptor.id === descriptor.id);
      return jsonMerge({}, baseDescriptor, descriptor);
    },


    /**
     * @param {String|Object} specifies the action
     * @param {Object} the live data entry that is the target of the action
     * @returns {Boolean} whether the specified action is allowed to target the specified live data entry
     */
    isActionAllowed(action, entry) {
      const actionDescriptor = this.getActionDescriptor(action);
      return !actionDescriptor.allowProperty || entry[actionDescriptor.allowProperty];
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
      return propertyDescriptor && (propertyDescriptor.sortable !== undefined ? propertyDescriptor.sortable :
        (propertyTypeDescriptor && propertyTypeDescriptor.sortable));
    },


    /**
     * Returns the sortable properties from the live data query.
     *
     * @returns {Array}
     */
    getSortableProperties () {
      return this.data.query.properties.filter(property => this.isPropertySortable(property));
    },


    /**
     * Returns the sortable properties that don't have a sort entry in the live data query.
     *
     * @returns {Array}
     */
    getUnsortedProperties () {
      return this.getSortableProperties().filter(property => !this.getQuerySort(property));
    },


    /**
     * Get the sort query associated to a property id
     * @param {String} propertyId
     */
    getQuerySort (propertyId) {
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
        // Allow the user to remove a sort entry (level < 0) even if the property is not sortable.
        if (!(level < 0 || this.isPropertySortable(property))) {
          return void reject(err);
        }
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
        const fromIndex = this.data.query.sort.findIndex(querySort => querySort.property === propertyId);
        if (fromIndex < 0 || toIndex < 0) { return void reject(err); }
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
      return propertyDescriptor && (propertyDescriptor.filterable !== undefined ? propertyDescriptor.filterable :
        (propertyTypeDescriptor && propertyTypeDescriptor.filterable));
    },


    /**
     * Returns the filterable properties from the live data query.
     *
     * @returns {Array}
     */
    getFilterableProperties () {
      return this.data.query.properties.filter(property => this.isPropertyFilterable(property));
    },


    /**
     * Returns the filterable properties that don't have constraints in the live data query.
     *
     * @returns {Array}
     */
    getUnfilteredProperties () {
      return this.getFilterableProperties().filter(property => {
        const filter = this.getQueryFilterGroup(property);
        return !filter || filter.constraints.length === 0;
      });
    },


    /**
     * Get the filter in the query data object associated to a property id
     * @param {String} propertyId
     * @returns {Object}
     */
    getQueryFilterGroup (propertyId) {
      return this.data.query.filters.find(filter => filter.property === propertyId);
    },


    /**
     * Get the filters in the query data object associated to a property id
     * @param {String} propertyId
     * @returns {Array} The constraints array of the filter group, or empty array if it does not exist
     */
    getQueryFilters (propertyId) {
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
    _computeFilterEntries (property, index, filterEntry, {filterOperator} = {}) {
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
      if (filterOperator) {
        newEntry.operator = filterOperator;
      }
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
    filter(property, index, filterEntry, {filterOperator} = {}) {
      const err = new Error("Property `" + property + "` is not filterable");
      return new Promise ((resolve, reject) => {
        const filterEntries = this._computeFilterEntries(property, index, filterEntry, {filterOperator});
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
              // We use by default AND between filter groups (different properties) and OR inside a filter group (same
              // property)
              matchAll: false,
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
        const filterIndex = this.data.query.filters
          .findIndex(filterGroup => filterGroup.property === property);
        if (filterIndex < 0) { return void reject(); }
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
    
    //
    // Translations
    //

    /**
     * @returns {Promise<boolean>} the promise is resolved to true once the translations are loaded
     */
    translationsLoaded() {
      return translationsLoaded;
    },
    
    //
    // Edit Bus
    //

    setEditBus(editBusInstance) {
      this.editBusInstance = editBusInstance;
    },

    getEditBus() {
      return this.editBusInstance;
    },

    /**
     * Registers a panel.
     *
     * The panel must have the following attributes:
     * * id: the id of the panel, must be unique among all panels, also used as suffix of the class on the panel
     * * name: the name that shall be shown in the menu
     * * title: the title that shall be displayed in the title bar of the panel
     * * icon: the name of the icon for the menu and the title of the panel
     * * container: the Element that shall be attached to the extension panel's body, this should contain the main UI
     * * component: the component of the panel, should be "LiveDataAdvancedPanelExtension" for extension panels
     * * order: the ordering number, panels are sorted by this number in ascending order
     *
     * @param {Object} panel the panel to add
     */
    registerPanel(panel)
    {
      // Basic insertion sorting to avoid shuffling the (reactive) array.
      const index = this.panels.findIndex(p => p.order > panel.order);
      if (index === -1) {
        this.panels.push(panel);
      } else {
        this.panels.splice(index, 0, panel);
      }
    },

    //
    // Content status
    //

    /**
     * @returns {boolean} when false, the content is not trusted will be sanitized whenever Vue integrated escaping
     * is not enough. When true, the content is never sanitized
     */
    isContentTrusted() {
      return this.contentTrusted;
    }
  };





  // return the init function to be used in the layouts
  return init;

});



