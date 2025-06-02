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
import XWikiBlockNote from "@/components/XWikiBlockNote.vue";
import { container } from "@/services/container";
import { i18nResolver } from "@/services/i18nResolver";
import { createApp, reactive } from "vue";
import { createI18n } from "vue-i18n";

/**
 * Encapsulates the logic of a BlockNote instance, exposing the API that can be used to interact with it.
 */
export class Logic {
  constructor(host) {
    this._host = host;
    this._name = host.name || host.id || host.dataset.name;
    this._realtimeServerURL = null;

    this._data = reactive(this._parseDataFromHost());

    this._ready = new Promise((resolve, reject) => {
      this._resolveReady = resolve;
      this._rejectReady = reject;
    });

    const skinManager = container.get("SkinManager");

    const i18n = createI18n({ legacy: false });
    this._i18nPromise = i18nResolver(i18n)
      .then(() => i18n)
      .catch(() => i18n);

    // eslint-disable-next-line @typescript-eslint/no-this-alias
    const logic = this;
    this._vueApp = createApp(XWikiBlockNote, this._data)
      .mixin({
        mounted() {
          host.classList.remove("loading");
          logic._resolveReady(logic);
        },
      })
      .provide("logic", this)
      .provide("container", container)
      .use(i18n);

    skinManager.loadDesignSystem(this._vueApp, container);

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

  get realtimeServerURL() {
    return this._realtimeServerURL;
  }

  /**
   * Returns a translation only once the translations have been loaded from the server.
   *
   * @param {String} key the translation key to translate
   * @param {...*} args the arguments to pass to the translation function
   */
  async translate(key, ...args) {
    // Make sure that the translations are loaded from the server before translating.
    const i18n = await this._i18nPromise;
    return i18n.global.t(key, args);
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
