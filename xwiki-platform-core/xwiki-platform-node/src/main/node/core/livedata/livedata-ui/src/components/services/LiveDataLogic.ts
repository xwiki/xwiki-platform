/**
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
import { EditBusService } from "./editBus";
import { FootnotesService } from "./footnote";
import { i18nResolver } from "./i18nResolver";
import { jsonMerge } from "./jsonMerge";
import { componentStore } from "@xwiki/platform-livedata-componentstore";
import { nextTick, reactive, ref } from "vue";
import { useI18n } from "vue-i18n";
import type {
  ActionDescriptor,
  Filter,
  FilterDescriptor,
  LayoutDescriptor,
  LiveDataSource,
  Logic,
  LogicData,
  Panel,
  PropertyDescriptor,
  QueryConstraint,
  SortEntry,
  TranslationQuery,
  Translations,
  Values,
} from "@xwiki/platform-livedata-api";
import type { Reactive, Ref } from "vue";
import type { ComposerTranslation } from "vue-i18n";

/**
 * Defines the internal logic of the Live Data.
 * This component expects a LiveDataSource providing the logic required to retrieve or update data from a remote source.
 * @since 18.2.0RC1
 * @beta
 */
export class LiveDataLogic implements Logic {
  public data: Reactive<LogicData>;
  public currentLayoutId: Ref<string>;
  public readonly t: ComposerTranslation;
  public entrySelection: {
    isGlobal: boolean;
    deselected: unknown[];
    selected: unknown[];
  } = { isGlobal: false, deselected: [], selected: [] };
  public firstEntriesLoading: Ref<boolean>;
  public openedPanels: Reactive<Panel[]>;
  private element?: HTMLElement;
  private readonly footnotes: FootnotesService;
  private panels: Panel[] = reactive([]);
  private readonly editBusInstance: EditBusService;
  private readonly isTranslationsLoaded: Promise<boolean>;

  constructor(
    private readonly liveDataSource: LiveDataSource,
    data: string,
    private readonly contentTrusted: boolean,
    resolveTranslations: (query: TranslationQuery) => Promise<Translations>,
  ) {
    this.data = reactive(JSON.parse(data));
    // Reactive properties must be initialized before Vue is instantiated.
    this.firstEntriesLoading = ref(true);
    this.currentLayoutId = ref("");
    this.changeLayout(this.data.meta.defaultLayout);
    this.entrySelection = reactive({
      selected: [],
      deselected: [],
      isGlobal: false,
    });
    this.openedPanels = reactive([]);
    this.t = useI18n().t;
    this.footnotes = new FootnotesService();
    this.editBusInstance = new EditBusService(this);
    this.isTranslationsLoaded = this.initI18n(resolveTranslations);
  }

  private async initI18n(
    resolveTranslations: (query: TranslationQuery) => Promise<Translations>,
  ): Promise<boolean> {
    try {
      await i18nResolver(resolveTranslations);
      return true;
    } catch {
      return false;
    }
  }

  setElement(element: HTMLElement): void {
    this.element = element;
  }

  /**
   * ---------------------------------------------------------------
   * EVENTS
   */

  /**
   * Send custom events. The livedata object reference is automatically added.
   * @param eventName - The name of the event, without the prefix "xwiki:livedata"
   * @param eventData - The data associated with the event.
   */
  triggerEvent(eventName: string, eventData?: object): void {
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
    this.element?.dispatchEvent(event);
  }

  /**
   * Listen for custom events.
   * @param event - The name of the event, without the prefix "xwiki:livedata"
   * @param  callback - Function to call we the event is triggered
   */
  onEvent(event: string, callback: (e: Event) => void): void {
    const eventName = "xwiki:livedata:" + event;
    if (!this.element) {
      throw new Error("The logic is not you mounted to an element");
    }
    this.element.addEventListener(eventName, function (e) {
      callback(e);
    });
  }

  /**
   * Listen for custom events, matching certain conditions.
   *
   * @param eventName - The name of the event, without the prefix "xwiki:livedata"
   * @param condition - The condition to execute the callback
   *  if Object, values of object properties must match e.detail properties values
   *  if Function, the function must return true. e.detail is passed as argument
   * @param callback -  Function to call we the event is triggered
   */
  onEventWhere(
    eventName: string,
    condition: object | ((p: unknown) => boolean),
    callback: (e: Event) => void,
  ): void {
    eventName = "xwiki:livedata:" + eventName;
    if (!this.element) {
      throw new Error("The logic is not you mounted to an element");
    }
    this.element.addEventListener(eventName, function (e: Event) {
      // Object check
      if (typeof condition === "object") {
        const isDetailMatching = (data: object, detail: unknown): boolean => {
          const keys = Object.keys(data);
          return keys.every((key) => {
            // @ts-expect-error leftover from javascript port.
            const datum = data[key];
            // @ts-expect-error leftover from javascript port.
            const detailElement = detail?.[key];
            return typeof datum === "object"
              ? isDetailMatching(datum, detailElement)
              : Object.is(datum, detailElement);
          });
        };
        // @ts-expect-error we except the kind of event to have a detail property
        if (!isDetailMatching(condition, e.detail)) {
          return;
        }
      }
      // Function check
      if (typeof condition === "function") {
        // @ts-expect-error we except the kind of event to have a detail property
        if (!condition(e.detail)) {
          return;
        }
      }
      // call callback
      callback(e);
    });
  }

  /**
   * ---------------------------------------------------------------
   * UTILS
   */

  /**
   * Return the list of layout ids.
   * @returns the list of available layout ids.
   */
  getLayoutIds() {
    return this.data.meta.layouts.map(
      (layoutDescriptor) => layoutDescriptor.id,
    );
  }

  /**
   * Return the id of the given entry.
   * @param values - an entry
   * @returns the entry id
   */
  getEntryId(values: Values): string | undefined {
    const idProperty = this.data.meta.entryDescriptor.idProperty || "id";
    if (values[idProperty] === undefined) {
      console.warn("Entry has no id (at property [" + idProperty + "]", values);
      return;
    }
    return values[idProperty] ?? undefined;
  }

  /*
    As Sets are not reactive in Vue 2.x, if we want to create
    a reactive collection of unique objects, we have to use arrays.
    So here are some handy functions to do what Sets do, but with arrays
  */

  /**
   * Return whether the array has the given item.
   *
   * @param uniqueArray - An array of unique items
   * @param item - a given item
   */
  uniqueArrayHas<T>(uniqueArray: T[], item: T) {
    return uniqueArray.includes(item);
  }

  /**
   * Add the given item if not present in the array, or does nothing.
   * @param uniqueArray - An array of unique items
   * @param item - an item to add if not already there
   */
  uniqueArrayAdd<T>(uniqueArray: T[], item: T) {
    if (this.uniqueArrayHas(uniqueArray, item)) {
      return;
    }
    uniqueArray.push(item);
  }

  /**
   * Remove the given item from the array if present, or does nothing.
   * @param uniqueArray - An array of unique items
   * @param item - an item
   */
  uniqueArrayRemove<T>(uniqueArray: T[], item: T) {
    const index = uniqueArray.indexOf(item);
    if (index === -1) {
      return;
    }
    uniqueArray.splice(index, 1);
  }

  /**
   * Toggle the given item from the array, ensuring its uniqueness.
   * @param uniqueArray - An array of unique items
   * @param item - an item
   * @param force - when true, force adding the item, when false, force remove the item. When undefined, decide
   * based on the array content
   */
  uniqueArrayToggle<T>(uniqueArray: T[], item: T, force: boolean | undefined) {
    if (force === undefined) {
      force = !this.uniqueArrayHas(uniqueArray, item);
    }
    if (force) {
      this.uniqueArrayAdd(uniqueArray, item);
    } else {
      this.uniqueArrayRemove(uniqueArray, item);
    }
  }

  /**
   * ---------------------------------------------------------------
   * DESCRIPTORS
   */

  /**
   * Returns the property descriptors of displayable properties
   * @returns a list of propertyt descriptors
   */
  getPropertyDescriptors(): (PropertyDescriptor | undefined)[] {
    return this.data.query.properties.map((propertyId) =>
      this.getPropertyDescriptor(propertyId),
    );
  }

  /**
   * Return the property descriptor corresponding to a property id.
   * @param propertyId - a property id
   * @returns a property descriptor, or undefined when not found
   */
  getPropertyDescriptor(propertyId: string): PropertyDescriptor | undefined {
    const propertyDescriptor = this.data.meta.propertyDescriptors.find(
      (propertyDescriptor) => propertyDescriptor.id === propertyId,
    );
    if (!propertyDescriptor) {
      console.error(
        "Property descriptor of property `" + propertyId + "` does not exist",
      );
    }
    return propertyDescriptor;
  }

  /**
   * Return the property type descriptor corresponding to a property id.
   * @param propertyId - a property id
   * @returns the property descriptor
   */
  getPropertyTypeDescriptor(
    propertyId: string,
  ): PropertyDescriptor | undefined {
    const propertyDescriptor = this.getPropertyDescriptor(propertyId);
    if (!propertyDescriptor) {
      return;
    }
    return this.data.meta.propertyTypes.find(
      (typeDescriptor) => typeDescriptor.id === propertyDescriptor.type,
    );
  }

  /**
   * Return the layout descriptor corresponding to a layout id
   * @param layoutId - a layout id
   * @returns the layout descriptor
   */
  getLayoutDescriptor(layoutId: string): LayoutDescriptor | undefined {
    return this.data.meta.layouts.find(
      (layoutDescriptor) => layoutDescriptor.id === layoutId,
    );
  }

  /**
   * Get the displayer descriptor associated to a property id
   * @param propertyId - a property id
   * @returns the displayer descriptor
   */
  getDisplayerDescriptor(propertyId: string) {
    // Property descriptor config
    const propertyDescriptor = this.getPropertyDescriptor(propertyId);
    // Property type descriptor config
    const typeDescriptor = this.getPropertyTypeDescriptor(propertyId);
    // Merge the property and type displayer descriptors.
    const customDisplayerDescriptor = jsonMerge(
      {},
      typeDescriptor?.displayer,
      propertyDescriptor?.displayer,
    );
    // Get the default displayer descriptor.
    const displayerId =
      // @ts-expect-error leftover from initial javascript version
      customDisplayerDescriptor.id || this.data.meta.defaultDisplayer;
    const defaultDisplayerDescriptor = this.data.meta.displayers.find(
      (displayer) => displayer.id === displayerId,
    );
    // Merge displayer descriptors.
    return jsonMerge({}, defaultDisplayerDescriptor, customDisplayerDescriptor);
  }

  /**
   * Get the filter descriptor associated to a property id
   * @param propertyId - the property id
   * @returns the filter descriptor
   */
  getFilterDescriptor(propertyId: string): FilterDescriptor {
    // Property descriptor config
    const propertyDescriptor = this.getPropertyDescriptor(propertyId);
    // Property type descriptor config
    const typeDescriptor = this.getPropertyTypeDescriptor(propertyId);
    // Merge the property and type filter descriptors.
    const customFilterDescriptor = jsonMerge(
      {},
      typeDescriptor?.filter,
      propertyDescriptor?.filter,
    );
    // Get the default filter descriptor.
    const filterId =
      // @ts-expect-error leftover from initial javascript version
      customFilterDescriptor.id || this.data.meta.defaultFilter;
    const defaultFilterDescriptor = this.data.meta.filters.find(
      (filter) => filter.id === filterId,
    );
    // Merge filter descriptors.
    return jsonMerge(
      {},
      defaultFilterDescriptor,
      customFilterDescriptor,
    ) as FilterDescriptor;
  }

  /**
   * ---------------------------------------------------------------
   * LAYOUT
   */

  /**
   * Fetch the entries of the current page according to the query configuration.
   * @returns the fetched entries
   */
  async fetchEntries(): Promise<{ count: number; entries: Values[] }> {
    // Before fetch event
    this.triggerEvent("beforeEntryFetch");
    // Fetch entries from data source
    try {
      return await this.liveDataSource.getEntries(this.data.query!);
    } finally {
      this.triggerEvent("afterEntryFetch");
    }
  }

  updateEntries() {
    return (
      this.fetchEntries()
        // eslint-disable-next-line promise/always-return
        .then((data) => {
          this.data.data = data;
          // Before triggering 'entriesUpdated', we wait for the next tick to be sure to have the DOM updated
          // first.
          // It turns out this is not enough when components are resolved asynchronously.
          // Therefore, we preemptively resolve the components that are going to be displayed here. Since they are
          // cached, the rendering of the displayers will not be delayed later on, and listeners of entriesUpdated
          // have access to a fully rendered DOM. Note that this approach is not optimal, and we should aim for a
          // mechanism that does not rely on direct DOM access. Instead, we should provide way to alter the data
          // externally before starting the rendering. (see https://jira.xwiki.org/browse/XWIKI-23423)
          const preloadDisplayer = this.getPropertyDescriptors()
            .filter((it) => it != undefined && this.isPropertyVisible(it.id))
            .map((it) =>
              componentStore.load(
                "displayer",
                (this.getDisplayerDescriptor(it!.id) as { id: string }).id,
              ),
            );
          // eslint-disable-next-line promise/catch-or-return,promise/always-return,promise/no-nesting
          Promise.all(preloadDisplayer).then(() => {
            nextTick(() => this.triggerEvent("entriesUpdated", {}));
          });
          // Remove the outdated footnotes, they will be recomputed by the new entries.
          this.footnotes.reset();
        })
        .catch((err) => {
          // Prevent undesired notifications of the end user for non business related errors (for
          // instance, the user left the page before the request was completed). See
          // https://developer.mozilla.org/en-US/docs/Web/API/XMLHttpRequest/readyState
          if (err.readyState === 4) {
            // eslint-disable-next-line promise/catch-or-return,promise/no-nesting
            this.translate("livedata.error.updateEntriesFailed").then(
              // @ts-expect-error XWiki.widget is excepted to be globally accessible
              (value) => new XWiki.widgets.Notification(value, "error"),
            );
          }

          // Do not log if the request has been aborted (e.g., because a second request was started
          // for the same LD with new criteria).
          if (err.statusText !== "abort") {
            console.error("Failed to fetch the entries", err);
          }
        })
    );
  }

  /**
   * Return whether the Livedata is editable or not
   * if entry given, return whether it is editable
   * if property given, return whether it is editable (for any entries)
   * If entry and property given, return whether specific value is editable
   * @param entry - the entry object
   * @param propertyId - the property id
   */
  isEditable({
    entry,
    propertyId,
  }: { entry?: Values; propertyId?: string } = {}) {
    // TODO: Ensure entry is valid (need other current PR)
    // TODO: Ensure property is valid (need other current PR)

    // Check if the edit entry action is available.
    if (!this.data.meta.actions.find((action) => action.id === "edit")) {
      return false;
    }

    // Check if we are allowed to edit the given entry.
    if (entry && !this.isEntryEditable(entry)) {
      return false;
    }

    // Check if the specified property is editable.
    return !propertyId || this.isPropertyEditable(propertyId);
  }

  /**
   * Returns whether the given entry is editable or not.
   *
   * @param entry - the entry
   * @returns true of the action is editable, false otherwise
   */
  isEntryEditable(entry: Values) {
    return this.isActionAllowed("edit", entry);
  }

  /**
   * Returns whether a certain property is editable or not.
   *
   * @param propertyId - a property id
   * @returns true if the property is editable
   */
  isPropertyEditable(propertyId: string) {
    const propertyDescriptor = this.getPropertyDescriptor(propertyId);
    const propertyTypeDescriptor = this.getPropertyTypeDescriptor(propertyId);
    return (
      propertyDescriptor &&
      (propertyDescriptor?.editable !== undefined
        ? propertyDescriptor.editable
        : propertyTypeDescriptor && propertyTypeDescriptor.editable)
    );
  }

  /**
   * Set the value of the given entry property
   * @param entry - The entry we want to modify
   * @param propertyId - The property id we want to modify in the entry
   * @param value - The new value of entry property
   */
  setValue({
    entry,
    propertyId,
    value,
  }: {
    entry: Values;
    propertyId: string;
    value: string;
  }): void {
    // TODO: Ensure entry is valid (need other current PR)
    // TODO: Ensure property is valid (need other current PR)
    if (!this.isEditable({ entry, propertyId })) {
      return;
    }
    entry[propertyId] = value;
    const source = this.data.query.source;
    const entryId = this.getEntryId(entry);
    // Once the entry updated, reload the whole livedata because changing a single entry can have
    // an impact on other properties of the entry, but also possibly on other entriers, or in the
    // way they are sorted.
    // eslint-disable-next-line promise/catch-or-return
    this.liveDataSource
      .updateEntryProperty(source, entryId ?? "", propertyId, entry[propertyId])
      .then(() => this.updateEntries());
  }

  /**
   * Update the entry with the values object passed in parameter and s
   * @param  entryId -  the current entry
   * @param  values - the entry's values to update
   */
  setValues({ entryId, values }: { entryId: string; values: unknown }) {
    const source = this.data.query.source;
    return this.liveDataSource
      .updateEntry(source, entryId, values)
      .then(() => this.updateEntries());
  }

  /**
   * Return whether adding new entries is enabled.
   */
  canAddEntry() {
    // Check if the add entry action is available.
    return this.data.meta.actions.find((action) => action.id === "addEntry");
  }

  addEntry() {
    throw new Error("not implemented");
  }

  /**
   * ---------------------------------------------------------------
   * LAYOUT
   */

  /**
   * Load a layout, or default layout if none specified
   * @param layoutId -  The id of the layout to load with requireJS
   * @returns a promise that completes when the layout is changed
   */
  changeLayout(layoutId: string) {
    // bad layout
    if (!this.getLayoutDescriptor(layoutId)) {
      console.error(
        "Layout of id `" + layoutId + "` does not have a descriptor",
      );
      return;
    }
    // set layout
    const previousLayoutId = this.currentLayoutId.value;
    this.currentLayoutId.value = layoutId;
    // dispatch events
    this.triggerEvent("layoutChange", {
      layoutId: layoutId,
      previousLayoutId: previousLayoutId,
    });
  }

  /**
   * ---------------------------------------------------------------
   * PAGINATION
   */

  /**
   * Get total number of pages
   * @returns the page count
   */
  getPageCount(): number {
    return Math.ceil(this.data.data.count / this.data.query.limit);
  }

  /**
   * Get the page corresponding to the specified entry (0-based index)
   * @param entryIndex - The index of the entry. Uses current entry if undefined.
   * @returns the computed page index
   */
  getPageIndex(entryIndex?: number): number {
    if (entryIndex === undefined) {
      entryIndex = this.data.query.offset;
    }
    return Math.floor(entryIndex / this.data.query.limit);
  }

  /**
   * Set page index (0-based index), then fetch new data
   * @param pageIndex - the page index
   * @returns a promise that completed when the page index is updated
   */
  async setPageIndex(pageIndex: number) {
    if (pageIndex < 0 || pageIndex >= this.getPageCount()) {
      throw new Error("Page index out of range");
    }
    const previousPageIndex = this.getPageIndex();
    this.data.query.offset = this.getFirstIndexOfPage(pageIndex);
    this.triggerEvent("pageChange", {
      pageIndex: pageIndex,
      previousPageIndex: previousPageIndex,
    });
    return this.updateEntries();
  }

  /**
   * Get the first entry index of the given page index
   * @param pageIndex - The page index. Uses current page if undefined.
   * @returns the first entry index of the given page index
   */
  getFirstIndexOfPage(pageIndex?: number) {
    if (pageIndex === undefined) {
      pageIndex = this.getPageIndex();
    }
    if (0 <= pageIndex && pageIndex < this.getPageCount()) {
      return pageIndex * this.data.query.limit;
    } else {
      return -1;
    }
  }

  /**
   * Get the last entry index of the given page index
   * @param pageIndex - The page index. Uses current page if undefined.
   * @returns the last entry index of the given page index
   */
  getLastIndexOfPage(pageIndex?: number) {
    if (pageIndex === undefined) {
      pageIndex = this.getPageIndex();
    }
    if (0 <= pageIndex && pageIndex < this.getPageCount()) {
      return (
        Math.min(
          this.getFirstIndexOfPage(pageIndex) + this.data.query.limit,
          this.data.data.count,
        ) - 1
      );
    } else {
      return -1;
    }
  }

  /**
   * Set the pagination page size, then fetch new data
   * @param pageSize - the page size
   * @returns a promise the completes when the page size is updated
   */
  async setPageSize(pageSize: number) {
    if (pageSize < 0) {
      throw new Error();
    }
    const previousPageSize = this.data.query.limit;
    if (pageSize === previousPageSize) {
      return;
    }
    this.data.query.limit = pageSize;
    // Reset the offset whenever the page size changes.
    this.data.query.offset = 0;
    this.triggerEvent("pageSizeChange", {
      pageSize: pageSize,
      previousPageSize: previousPageSize,
    });
    return this.updateEntries();
  }

  /**
   * ---------------------------------------------------------------
   * DISPLAY
   */

  /**
   * Returns whether a certain property is visible
   * @param propertyId - the property id
   * @returns true if the property is visible
   */
  isPropertyVisible(propertyId: string) {
    const propertyDescriptor = this.getPropertyDescriptor(propertyId);
    return propertyDescriptor?.visible ?? false;
  }

  /**
   * Set whether the given property should be visible
   * @param propertyId - the property id
   * @param  visible - the new visibility
   */
  setPropertyVisible(propertyId: string, visible: boolean) {
    const propertyDescriptor = this.getPropertyDescriptor(propertyId);
    if (propertyDescriptor) {
      propertyDescriptor.visible = visible;
    }
  }

  /**
   * Move a property to a certain index in the property order list
   * @param from - The id or index of the property to move
   * @param toIndex - the target index
   */
  reorderProperty(from: string | number, toIndex: number) {
    let fromIndex;
    if (typeof from === "number") {
      fromIndex = from;
    } else if (typeof from === "string") {
      fromIndex = this.data.query.properties.indexOf(from);
    } else {
      return;
    }
    if (fromIndex < 0 || toIndex < 0) {
      return;
    }
    this.data.query.properties.splice(
      toIndex,
      0,
      this.data.query.properties.splice(fromIndex, 1)[0],
    );
  }

  /**
   * ---------------------------------------------------------------
   * SELECTION
   */

  /**
   * Return whether selecting entries is enabled. If an entry is given, return whether that entry
   * can be selected.
   *
   * @param entry - an entry
   */
  isSelectionEnabled({ entry }: { entry?: Values } = {}) {
    // An entry is selectable if it has an id specified.
    return (
      this.data.meta.selection.enabled && (!entry || this.getEntryId(entry))
    );
  }

  /**
   * Return whether the entry is currently selected
   * @param values - an entry
   * @returns true if the entry is selected
   */
  isEntrySelected(values: Values) {
    const entryId = this.getEntryId(values);
    if (this.entrySelection.isGlobal) {
      return !this.uniqueArrayHas(this.entrySelection.deselected, entryId);
    } else {
      return this.uniqueArrayHas(this.entrySelection.selected, entryId);
    }
  }

  /**
   * Select the specified entries
   * @param entries - a set of entries to select
   */
  selectEntries(entries: Values[] | Values) {
    if (!this.isSelectionEnabled()) {
      return;
    }
    const entryArray = entries instanceof Array ? entries : [entries];
    entryArray.forEach((entry) => {
      if (!this.isSelectionEnabled({ entry })) {
        return;
      }
      const entryId = this.getEntryId(entry);
      if (this.entrySelection.isGlobal) {
        this.uniqueArrayRemove(this.entrySelection.deselected, entryId);
      } else {
        this.uniqueArrayAdd(this.entrySelection.selected, entryId);
      }
      this.triggerEvent("select", {
        entry: entry,
      });
    });
  }

  /**
   * Deselect the specified entries
   * @param  entries - a set of entries to deselect
   */
  deselectEntries(entries: Values[] | Values) {
    if (!this.isSelectionEnabled()) {
      return;
    }
    const entryArray = entries instanceof Array ? entries : [entries];
    entryArray.forEach((entry) => {
      if (!this.isSelectionEnabled({ entry })) {
        return;
      }
      const entryId = this.getEntryId(entry);
      if (this.entrySelection.isGlobal) {
        this.uniqueArrayAdd(this.entrySelection.deselected, entryId);
      } else {
        this.uniqueArrayRemove(this.entrySelection.selected, entryId);
      }
      this.triggerEvent("deselect", {
        entry: entry,
      });
    });
  }

  /**
   * Toggle the selection of the specified entries
   * @param entries - a set of entries to toggle
   * @param select - Whether to select or not the entries. Undefined toggle current state
   */
  toggleSelectEntries(entries: Values[] | Values, select?: boolean) {
    if (!this.isSelectionEnabled()) {
      return;
    }
    const entryArray = entries instanceof Array ? entries : [entries];
    entryArray.forEach((entry) => {
      if (!this.isSelectionEnabled({ entry })) {
        return;
      }
      if (select === undefined) {
        select = !this.isEntrySelected(entry);
      }
      if (select) {
        this.selectEntries(entry);
      } else {
        this.deselectEntries(entry);
      }
    });
  }

  /**
   * Get number of selectable entries in page
   * @returns the count
   */
  selectableCountInPage(): number {
    if (!this.isSelectionEnabled()) {
      return 0;
    }
    return this.data.data.entries.filter((entry) =>
      this.isSelectionEnabled({ entry }),
    ).length;
  }

  /**
   * Set the entry selection globally across pages
   * @param global - the new global state for the selection
   */
  setEntrySelectGlobal(global: boolean) {
    if (!this.isSelectionEnabled()) {
      return;
    }
    this.entrySelection.isGlobal = global;
    this.entrySelection.selected.splice(0);
    this.entrySelection.deselected.splice(0);
    this.triggerEvent("selectGlobal", {
      state: global,
    });
  }

  /**
   * ---------------------------------------------------------------
   * ACTIONS
   */

  /**
   * @param action - the action
   * @returns the descriptor of the specified live data action
   */
  getActionDescriptor(action: string | ActionDescriptor): ActionDescriptor {
    const descriptor = typeof action === "string" ? { id: action } : action;
    const baseDescriptor = this.data.meta.actions.find(
      (baseDescriptor) => baseDescriptor.id === descriptor.id,
    );
    return jsonMerge({}, baseDescriptor, descriptor) as ActionDescriptor;
  }

  /**
   * @param action - the action
   * @param values - the live data entry that is the target of the action
   * @returns whether the specified action is allowed to target the specified live data entry
   */
  isActionAllowed(action: string | ActionDescriptor, values: Values) {
    const actionDescriptor = this.getActionDescriptor(action);
    return (
      !actionDescriptor.allowProperty || values[actionDescriptor.allowProperty]
    );
  }

  /**
   * ---------------------------------------------------------------
   * SORT
   */

  /**
   * Returns whether a certain property is sortable or not.
   *
   * @param propertyId - a property id
   * @returns true if the property is sortable
   */
  isPropertySortable(propertyId: string) {
    const propertyDescriptor = this.getPropertyDescriptor(propertyId);
    const propertyTypeDescriptor = this.getPropertyTypeDescriptor(propertyId);
    return (
      propertyDescriptor &&
      (propertyDescriptor.sortable !== undefined
        ? propertyDescriptor.sortable
        : propertyTypeDescriptor && propertyTypeDescriptor.sortable)
    );
  }

  /**
   * Returns the sortable properties from the live data query.
   *
   * @returns the list of sortable properties
   */
  getSortableProperties(): string[] {
    return this.data.query.properties.filter((property) =>
      this.isPropertySortable(property),
    );
  }

  /**
   * Returns the sortable properties that don't have a sort entry in the live data query.
   *
   * @returns the list of unused properties
   */
  getUnsortedProperties(): string[] {
    return this.getSortableProperties().filter(
      (property) => !this.getQuerySort(property),
    );
  }

  /**
   * Get the sort query associated to a property id
   * @param  propertyId - a property id
   */
  getQuerySort(propertyId: string): SortEntry | undefined {
    return this.data.query.sort.find((sort) => sort.property === propertyId);
  }

  /**
   * Update sort configuration based on parameters, then fetch new data
   * @param property -  The property to sort according to
   * @param level - The sort level for the property (0 is the highest).
   *   Undefined means keep current. Negative value removes property sort.
   * @param descending - Specify whether the sort should be descending or not. Undefined means toggle current direction
   * @returns a promise the completes when the sort is done
   */
  // eslint-disable-next-line max-statements
  async sort(
    property: string,
    level: number,
    descending?: boolean,
  ): Promise<void> {
    // Allow the user to remove a sort entry (level < 0) even if the property is not sortable.
    if (!(level < 0 || this.isPropertySortable(property))) {
      throw new Error(`Property \`${property}\` is not sortable`);
    }
    // find property current sort level
    const currentLevel = this.data.query.sort.findIndex(
      (sortObject) => sortObject.property === property,
    );
    // default level
    if (level === undefined) {
      level = currentLevel !== -1 ? currentLevel : 0;
    } else if (level < 0) {
      level = -1;
    }
    // default descending
    if (descending === undefined) {
      descending =
        currentLevel !== -1
          ? !this.data.query.sort[currentLevel].descending
          : false;
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

    return this.updateEntries();
  }

  /**
   * Add new sort entry, shorthand of sort:
   * If the property is already sorting, does nothing
   * @param property - The property to add to the sort
   * @param descending - Specify whether the sort should be descending or not.
   *   Undefined means toggle current direction
   * @returns a promise the completes when the new sort is applied
   */
  addSort(property: string, descending: boolean | undefined) {
    const err = new Error("Property `" + property + "` is already sorting");
    const propertyQuerySort = this.data.query.sort.find(
      (sortObject) => sortObject.property === property,
    );
    if (propertyQuerySort) {
      return Promise.reject(err);
    }
    return this.sort(property, this.data.query.sort.length, descending);
  }

  /**
   * Remove a sort entry, shorthand of sort:
   * @param property - The property to remove to the sort
   * @returns a promise that completes when the new sorting is done
   */
  removeSort(property: string) {
    return this.sort(property, -1);
  }

  /**
   * Move a sort entry to a certain index in the query sort list
   * @param propertyId - The property to reorder the sort
   * @param toIndex - the new index
   */
  reorderSort(propertyId: string, toIndex: number) {
    const fromIndex = this.data.query.sort.findIndex(
      (querySort) => querySort.property === propertyId,
    );
    if (fromIndex < 0 || toIndex < 0) {
      throw new Error(`Property \`${propertyId}\` is not sortable`);
    }
    this.data.query.sort.splice(
      toIndex,
      0,
      this.data.query.sort.splice(fromIndex, 1)[0],
    );

    // dispatch events
    this.triggerEvent("sort", {
      type: "move",
      property: propertyId,
      level: toIndex,
    });

    this.updateEntries();
  }

  /**
   * ---------------------------------------------------------------
   * FILTER
   */

  /**
   * Returns whether a certain property is filterable or not
   * @param propertyId - a property id
   * @returns true if the property is sortable
   */
  isPropertyFilterable(propertyId: string) {
    const propertyDescriptor = this.getPropertyDescriptor(propertyId);
    const propertyTypeDescriptor = this.getPropertyTypeDescriptor(propertyId);
    return (
      propertyDescriptor &&
      (propertyDescriptor.filterable !== undefined
        ? propertyDescriptor.filterable
        : propertyTypeDescriptor && propertyTypeDescriptor.filterable)
    );
  }

  /**
   * Returns the filterable properties from the live data query.
   *
   * @returns the list of filterable properties
   */
  getFilterableProperties(): string[] {
    return this.data.query.properties.filter((property) =>
      this.isPropertyFilterable(property),
    );
  }

  /**
   * Returns the filterable properties that don't have constraints in the live data query.
   *
   * @returns the list of unfiltered properties
   */
  getUnfilteredProperties(): string[] {
    return this.getFilterableProperties().filter((property) => {
      const filter = this.getQueryFilterGroup(property);
      return !filter || filter.constraints.length === 0;
    });
  }

  /**
   * Get the filter in the query data object associated to a property id
   * @param propertyId - a property id
   * @returns the filters for the property
   */
  getQueryFilterGroup(propertyId: string): Filter | undefined {
    return this.data.query.filters.find(
      (filter) => filter.property === propertyId,
    );
  }

  /**
   * Get the filters in the query data object associated to a property id
   * @param propertyId - property id
   * @returns The constraints array of the filter group, or empty array if it does not exist
   */
  getQueryFilters(propertyId: string): QueryConstraint[] {
    const queryFilterGroup = this.getQueryFilterGroup(propertyId);
    return (queryFilterGroup && queryFilterGroup.constraints) || [];
  }

  /**
   * Get the default filter operator associated to a property id
   * @param  propertyId - the property id
   * @returns the default operator for the filter
   */
  // eslint-disable-next-line max-statements
  getFilterDefaultOperator(propertyId: string) {
    // get valid operator descriptor
    const filterDescriptor = this.getFilterDescriptor(propertyId);
    if (!filterDescriptor) {
      return;
    }
    const filterOperators = filterDescriptor.operators;
    if (!(filterOperators instanceof Array)) {
      return;
    }
    if (filterOperators.length === 0) {
      return;
    }
    // get default operator
    const defaultOperator = filterDescriptor.defaultOperator;
    const isDefaultOperatorValid = !!filterOperators.find(
      (operator) => operator.id === defaultOperator,
    );
    if (defaultOperator && isDefaultOperatorValid) {
      return defaultOperator;
    } else {
      return filterOperators[0].id;
    }
  }

  /**
   * Return an object containing the new and old filter entries corresponding to parameters
   *  oldEntry: the filter entry to be modified
   *  newEntry: what this entry should be modified to
   * @param property - The property to filter according to
   * @param index - The index of the filter entry
   * @param filterEntry - The filter data used to update the filter configuration
   *  (see Logic.prototype.filter for more)
   * @param filterOperator - the filter operator to apply
   * @returns pairs of oldEntry and newEntry, with oldEntry / newEntry being property, index, operator, and value.
   */
  // eslint-disable-next-line max-statements
  private _computeFilterEntries(
    property: string,
    index: number,
    filterEntry: { index: number },
    { filterOperator }: { filterOperator?: unknown } = {},
  ) {
    if (!this.isPropertyFilterable(property)) {
      return;
    }
    // default indexes
    index = index || 0;
    if (index < 0) {
      index = -1;
    }
    if (filterEntry.index < 0) {
      filterEntry.index = -1;
    }
    // old entry
    let oldEntry = {
      property: property,
      index: index,
    };
    const queryFilters = this.getQueryFilters(property);
    const currentEntry = queryFilters[index] || {};
    oldEntry = Object.assign({}, currentEntry, oldEntry);
    // new entry (copy properties that are not undefined from filterEntry)
    let newEntry = Object.fromEntries(
      Object.entries(filterEntry || {}).filter(
        (entry) => entry[1] !== undefined,
      ),
    );
    // eslint-disable-next-line @typescript-eslint/no-this-alias
    const self = this;
    const defaultEntry = {
      property: property,
      value: "",
      operator: self.getFilterDefaultOperator(property),
      index: 0,
    };
    newEntry = Object.assign({}, defaultEntry, oldEntry, newEntry);
    if (filterOperator) {
      // @ts-expect-error leftover from initial javascript implementation
      newEntry.operator = filterOperator;
    }
    // check newEntry operator
    const newEntryValidOperator = this.getFilterDescriptor(
      // @ts-expect-error leftover from initial javascript implementation
      newEntry.property,
    ).operators.some(
      (operator) =>
        // @ts-expect-error leftover from initial javascript implementation
        operator.id === newEntry.operator,
    );
    if (!newEntryValidOperator) {
      // @ts-expect-error leftover from initial javascript implementation
      newEntry.operator = self.getFilterDefaultOperator(newEntry.property);
    }
    return {
      oldEntry: oldEntry,
      newEntry: newEntry,
    };
  }

  /**
   * Return the filtering type, based on oldEntry and newEntry
   * @param oldEntry - the old entry
   * @param newEntry - the new entry
   * @returns  the type of filtering
   */
  private _getFilteringType(
    oldEntry: { property: string; index: number },
    newEntry: { index: number },
  ): "add" | "remove" | "move" | "modify" {
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
   * Update filter configuration based on parameters, then fetch new data.
   * @param property - The property to filter according to
   * @param  index - The index of the filter entry
   * @param filterEntry - The filter data used to update the filter configuration
   *  filterEntry = \{property, operator, value\}
   *  undefined values are defaulted to current values, then to default values.
   * @param property - The new property to filter according to
   * @param index - The new index the filter should go. -1 delete filter
   * @param operator - The operator of the filter.
   *  Should match the filter descriptor of the filter property
   * @param filterOperator - the operator to apply, when undefined the default filter of the entry is applied
   * @param skipFetch - when true, no fetch is triggered and only the reactive variables are updated, this is
   *  useful in case of asynchronous operations that need to update the UI without triggering a fetch straight away.
   * @returns the filtered results
   */
  // eslint-disable-next-line max-statements
  async filter(
    property: string,
    index: number,
    filterEntry: { index: number },
    {
      filterOperator,
      skipFetch,
    }: { filterOperator?: unknown; skipFetch?: boolean } = {},
  ) {
    const filterEntries = this._computeFilterEntries(
      property,
      index,
      filterEntry,
      { filterOperator },
    );
    if (!filterEntries) {
      throw new Error("Property `" + property + "` is not filterable");
    }
    const oldEntry = filterEntries.oldEntry;
    const newEntry = filterEntries.newEntry;
    // @ts-expect-error leftover from javascript implementation
    const filteringType = this._getFilteringType(oldEntry, newEntry);
    // remove filter at current property and index
    if (oldEntry.index !== -1) {
      this.getQueryFilters(oldEntry.property).splice(index, 1);
    }
    // add filter at new property and index
    if (newEntry.index !== -1) {
      // create filterGroup if not exists
      // @ts-expect-error leftover from javascript implementation
      if (!this.getQueryFilterGroup(newEntry.property)) {
        this.data.query.filters.push({
          // @ts-expect-error leftover from javascript implementation
          property: newEntry.property,
          // We use by default AND between filter groups (different properties) and OR inside a
          // filter group (same property)
          matchAll: false,
          constraints: [],
        });
      }
      // add entry
      // @ts-expect-error leftover from javascript implementation
      this.getQueryFilterGroup(newEntry.property).constraints.splice(
        newEntry.index,
        0,
        {
          // @ts-expect-error leftover from javascript implementation
          operator: newEntry.operator,
          value: newEntry.value,
        },
      );
    }
    // remove filter group if empty
    if (this.getQueryFilters(oldEntry.property).length === 0) {
      await this.removeAllFilters(oldEntry.property);
    }
    // Reset the offset whenever the filters are updated.
    this.data.query.offset = 0;
    // dispatch events
    this.triggerEvent("filter", {
      type: filteringType,
      oldEntry: oldEntry,
      newEntry: newEntry,
    });

    if (!skipFetch) {
      await this.updateEntries();
    }
  }

  /**
   * Add new filter entry, shorthand of filter:
   * @param property - Which property to add the filter to
   * @param operator - The operator of the filter. Should match the filter descriptor of the
   *   property
   * @param value - Default value for the new filter entry
   * @param  index - Index of new filter entry. Undefined means last
   * @returns a promise that completes when the filter is added
   */
  addFilter(property: string, operator: unknown, value: string, index: number) {
    if (index === undefined) {
      index = ((this.getQueryFilterGroup(property) || {}).constraints || [])
        .length;
    }

    return this.filter(property, -1, {
      // @ts-expect-error leftover from initial javascript implementation
      property: property,
      operator: operator,
      value: value,
      index: index,
    });
  }

  /**
   * Remove a filter entry in the configuration, then fetch new data
   * @param property - Property to remove the filter to
   * @param index - The index of the filter to remove. Undefined means last.
   * @returns a promise that completes when the filter is removed
   */
  removeFilter(property: string, index: number) {
    return this.filter(property, index, { index: -1 });
  }

  /**
   * Remove all the filters associated to a property
   * @param  property - Property to remove the filters to
   * @returns a promise that completes when the filters are removed
   */
  removeAllFilters(property: unknown) {
    const filterIndex = this.data.query.filters.findIndex(
      (filterGroup) => filterGroup.property === property,
    );
    if (filterIndex < 0) {
      throw new Error();
    }
    const removedFilterGroups = this.data.query.filters.splice(filterIndex, 1);
    // Reset the offset whenever the filters are updated.
    this.data.query.offset = 0;
    // dispatch events
    this.triggerEvent("filter", {
      type: "removeAll",
      property: property,
      removedFilters: removedFilterGroups[0].constraints,
    });
    return this.updateEntries();
  }

  //
  // Translations
  //

  /**
   * @returns the promise with the translations loaded
   */
  async translationsLoaded(): Promise<boolean> {
    return this.isTranslationsLoaded;
  }

  //
  // Edit Bus
  //

  getEditBus() {
    return this.editBusInstance;
  }

  /**
   * Registers a panel.
   *
   * The panel must have the following attributes:
   * * id: the id of the panel, must be unique among all panels, also used as suffix of the class
   * on the panel
   * * name: the name that shall be shown in the menu
   * * title: the title that shall be displayed in the title bar of the panel
   * * icon: the name of the icon for the menu and the title of the panel
   * * container: the Element that shall be attached to the extension panel's body, this should
   * contain the main UI
   * * component: the component of the panel, should be "LiveDataAdvancedPanelExtension" for
   * extension panels
   * * order: the ordering number, panels are sorted by this number in ascending order
   *
   * @param panel - the panel to add
   */
  registerPanel(panel: Panel) {
    // Basic insertion sorting to avoid shuffling the (reactive) array.
    const index = this.panels.findIndex((p) => p.order > panel.order);
    if (index === -1) {
      this.panels.push(panel);
    } else {
      this.panels.splice(index, 0, panel);
    }
  }

  //
  // Content status
  //

  /**
   * @returns when false, the content is not trusted will be sanitized whenever Vue integrated escaping is not enough.
   *     When true, the content is never sanitized
   */
  isContentTrusted() {
    return this.contentTrusted;
  }

  async translate(key: string, ...args: unknown[]) {
    // Make sure that the translations are loaded from the server before translating.
    await this.translationsLoaded();
    return this.t(key, args);
  }
}
