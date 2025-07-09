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
import type { SkinManager } from "@xwiki/cristal-api";
import { createPinia } from "pinia";
import { App, createApp, reactive } from "vue";
import { createI18n, I18n } from "vue-i18n";
import XWikiBlockNote from "../components/XWikiBlockNote.vue";
import { container } from "./container";
import { i18nResolver } from "./i18nResolver";

type Data = {
  initialValue?: string;
  value?: string;
};

/**
 * Encapsulates the logic of a BlockNote instance, exposing the API that can be used to interact with it.
 */
export class Logic {
  private readonly _host: HTMLElement;
  private readonly _name?: string;
  private readonly _realtimeServerURL?: string;
  private readonly _data: Data;
  private readonly _ready: Promise<Logic>;
  private _resolveReady?: (logic: Logic) => void;
  private readonly _i18nPromise: Promise<I18n>;
  private readonly _vueApp: App;
  private readonly _root: InstanceType<typeof XWikiBlockNote>;

  constructor(host: HTMLElement) {
    this._host = host;
    this._name = host.getAttribute("name") ?? host.id ?? host.dataset.name;
    this._data = reactive(this._parseDataFromHost());

    this._ready = new Promise((resolve) => {
      this._resolveReady = resolve;
    });

    const skinManager: SkinManager = container.get("SkinManager");

    const locale = document.documentElement.getAttribute("lang") || "en";
    const i18n = createI18n({ legacy: false, locale });
    this._i18nPromise = i18nResolver(i18n);

    // eslint-disable-next-line @typescript-eslint/no-this-alias
    const logic = this;
    this._vueApp = createApp(XWikiBlockNote, this.data)
      .mixin({
        mounted() {
          host.classList.remove("loading");
          logic._resolveReady?.(logic);
        },
      })
      .provide("logic", this)
      .provide("container", container)
      .use(createPinia())
      .use(i18n);

    skinManager.loadDesignSystem(this._vueApp, container);

    this._root = this._vueApp.mount(host) as InstanceType<typeof XWikiBlockNote>;
  }

  /**
   * @returns {String} the name of the form field associated with this BlockNote instance; this is the key used to
   *            submit the edited content
   */
  get name(): string | undefined {
    return this._name;
  }

  /**
   * @returns {HTMLElement} the HTML Element that hosts this BlockNote instance
   */
  get host(): HTMLElement {
    return this._host;
  }

  /**
   * @returns {Data} the data managed by this BlockNote instance
   */
  get data(): Data {
    this._data.value = this._root.updateValue();
    return this._data;
  }

  /**
   * @returns {Promise} a promise that resolves when the BlockNote instance is ready
   */
  get ready(): Promise<Logic> {
    return this._ready;
  }

  get realtimeServerURL(): string | undefined {
    return this._realtimeServerURL;
  }

  /**
   * Returns a translation only once the translations have been loaded from the server.
   *
   * @param {String} key the translation key to translate
   * @param {...*} args the arguments to pass to the translation function
   */
  async translate(key: string, ...args: unknown[]): Promise<string> {
    // Make sure that the translations are loaded from the server before translating.
    const i18n = await this._i18nPromise;
    // FIXME: This type assertion shouldn't be necessary but I haven't found a way to avoid it. Using the Key type from
    // vue-i18n doesn't help and, what's more strange, it fails to build even when calling i18n.global.t("some.key")
    return (i18n.global.t as (key: string, params: unknown[]) => string)(key, args);
  }

  /**
   * Destroys this BlockNote instance, cleaning up any resources it holds.
   */
  destroy(): void {
    this._vueApp.unmount();
  }

  /**
   * @returns {Object} the data parsed from the host element
   */
  _parseDataFromHost(): Data {
    const data = Object.assign(this._host.dataset.config ? JSON.parse(this._host.dataset.config) : {}, {
      ...this._host.dataset,
    });
    delete data.config;
    data.initialValue = data.value;
    return data;
  }
}
