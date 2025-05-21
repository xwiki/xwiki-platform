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
import { createApp, reactive } from "vue";
import XWikiBlockNote from "@/components/XWikiBlockNote.vue";

/**
 * Encapsulates the logic of a BlockNote instance, exposing the API that can be used to interact with it.
 */
export class Logic {
  constructor(host) {
    this._host = host;
    this._name = host.name || host.id || host.dataset.name;

    this._data = reactive(this._parseDataFromHost());

    this._ready = new Promise((resolve, reject) => {
      this._resolveReady = resolve;
      this._rejectReady = reject;
    });

    const logic = this;
    this._vueApp = createApp(XWikiBlockNote, this._data)
      .mixin({
        mounted() {
          host.classList.remove("loading");
          logic._resolveReady(logic);
        },
      })
      .provide("logic", this);
    this._vueApp.mount(host);
  }

  /**
   * @returns {String} the name of the form field associated with this BlockNote instance; this is the key used to
   *            submit the edited content
   */
  get name() {
    return this._name;
  }

  /**
   * @returns {HTMLElement} the HTML Element that hosts this BlockNote instance
   */
  get host() {
    return this._host;
  }

  /**
   * @returns {Object} the data managed by this BlockNote instance
   */
  get data() {
    return this._data;
  }

  /**
   * @returns {Promise} a promise that resolves when the BlockNote instance is ready
   */
  get ready() {
    return this._ready;
  }

  /**
   * Destroys this BlockNote instance, cleaning up any resources it holds.
   */
  destroy() {
    this._vueApp.unmount();
  }

  /**
   * @returns {Object} the data parsed from the host element
   */
  _parseDataFromHost() {
    const data = { ...this._host.dataset };
    delete data.config;
    return Object.assign(this._host.dataset.config ? JSON.parse(this._host.dataset.config) : {}, data);
  }
}
