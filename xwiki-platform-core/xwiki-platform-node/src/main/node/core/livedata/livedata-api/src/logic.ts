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
import type { LogicData } from "./logicData";
import type { Panel } from "./panel";
import type { Values } from "./values";
import type { Reactive, Ref } from "vue";

/**
 * Present the public API of the logic used inside the Live Data UI.
 * It provides the operations and data to display Live Datas. It is build to be shared by most of the UI elements of
 * a Live Data.
 * @since 18.2.0RC1
 * @beta
 */
export interface Logic {
  /**
   * The Live Data data, fetch from a source.
   */
  data?: Reactive<LogicData>;
  /**
   * The id of the current layout.
   */
  currentLayoutId?: Ref<string>;

  /**
   * Set the root element of the Live Data.
   * @param element - the element to bind the Live Data to.
   */
  setElement(element: HTMLElement): void;

  /**
   * Listen for an event.
   * @param event - the event to listen to
   * @param callback - a callback, taking the received event in paramter
   */
  onEvent(event: string, callback: (e: Event) => void): void;

  /**
   * A promise completing when the translations are loaded. It can contain true if the translations loaded
   * successfully, false otherwise.
   */
  translationsLoaded(): Promise<boolean>;

  /**
   * When true, the content is trusted. When false, the content is not trusted and will be sanitized.
   */
  isContentTrusted(): boolean;

  /**
   * Update the entry with the values object passed in parameter and s
   * @param  entryId -  the current entry
   * @param  values - the entry's values to update
   */
  setValues({
    entryId,
    values,
  }: {
    entryId: string;
    values: unknown;
  }): Promise<unknown>;

  /**
   * Return the id of the given entry.
   * @param entry - an entry
   * @returns the entry id
   */
  getEntryId(entry: Values): string | undefined;

  /**
   * Trigger a refresh of the Live Data.
   * @returns a promise completing when the refresh is done
   */
  updateEntries(): Promise<void>;

  /**
   * Send custom events. The livedata object reference is automatically added.
   * @param eventName - The name of the event, without the prefix "xwiki:livedata"
   * @param eventData - The data associated with the event.
   */
  triggerEvent(eventName: string, eventData?: object): void;

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
  ): void;

  /**
   * Load a layout, or default layout if none specified
   * @param layoutId -  The id of the layout to load with requireJS
   * @returns a promise that completes when the layout is changed
   */
  changeLayout(layoutId: string): void;

  /**
   * Get total number of pages
   * @returns the page count
   */
  getPageCount(): number;

  /**
   * Update sort configuration based on parameters, then fetch new data
   * @param property -  The property to sort according to
   * @param level - The sort level for the property (0 is the highest).
   *   Undefined means keep current. Negative value removes property sort.
   * @param descending - Specify whether the sort should be descending or not. Undefined means toggle current direction
   * @returns a promise the completes when the sort is done
   */

  sort(property: string, level: number, descending?: boolean): Promise<void>;

  /**
   * Add new sort entry, shorthand of sort:
   * If the property is already sorting, does nothing
   * @param property - The property to add to the sort
   * @param descending - Specify whether the sort should be descending or not.
   *   Undefined means toggle current direction
   * @returns a promise the completes when the new sort is applied
   */
  addSort(property: string, descending: boolean | undefined): Promise<void>;

  /**
   * Remove a sort entry, shorthand of sort:
   * @param property - The property to remove to the sort
   * @returns a promise that completes when the new sorting is done
   */
  removeSort(property: string): Promise<void>;

  /**
   * Move a sort entry to a certain index in the query sort list
   * @param propertyId - The property to reorder the sort
   * @param toIndex - the new index
   */
  reorderSort(propertyId: string, toIndex: number): void;

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

  filter(
    property: string,
    index: number,
    filterEntry: { index: number },
    {
      filterOperator,
      skipFetch,
    }: { filterOperator?: unknown; skipFetch?: boolean },
  ): Promise<void>;

  /**
   * Add new filter entry, shorthand of filter:
   * @param property - Which property to add the filter to
   * @param operator - The operator of the filter. Should match the filter descriptor of the
   *   property
   * @param value - Default value for the new filter entry
   * @param  index - Index of new filter entry. Undefined means last
   * @returns a promise that completes when the filter is added
   */
  addFilter(
    property: string,
    operator: unknown,
    value: string,
    index: number,
  ): Promise<void>;

  /**
   * Remove a filter entry in the configuration, then fetch new data
   * @param property - Property to remove the filter to
   * @param index - The index of the filter to remove. Undefined means last.
   * @returns a promise that completes when the filter is removed
   */
  removeFilter(property: string, index: number): Promise<void>;

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
  registerPanel(panel: Panel): void;
}
