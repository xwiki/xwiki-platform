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
import {XWikiLivedata} from "@xwiki/platform-livedata-ui";
import {XWikiLiveDataSource, buildTranslations} from "@xwiki/platform-livedata-xwiki"
import {createApp} from "vue";
import Vue3TouchEvents from "vue3-touch-events";
import {createI18n} from "vue-i18n";

/**
 * The init function of the logic script
 * For each livedata element on the page, returns its corresponding data / API
 * If the data does not exist yet, create it from the element
 * @param {HTMLElement} element The HTML Element corresponding to the Livedata component
 * @param $ a jquery instance
 */
function init(element, $) {

  const locale = document.documentElement.getAttribute("lang");
  const i18n = createI18n({ legacy: false, locale });

  const data = element.dataset.config
  element.removeAttribute("data-config")
  const contentTrusted = element.getAttribute("data-config-content-trusted") === "true";

  // Vue.js replaces the container - prevent this by creating a placeholder for Vue.js to replace.
  const placeholderElement = document.createElement("div");
  element.appendChild(placeholderElement);

  createApp(XWikiLivedata, {
    data,
    liveDataSource: new XWikiLiveDataSource($),
    contentTrusted,
    resolveTranslations: buildTranslations(locale, i18n),
  })
    .mixin({
      mounted() {
        element.classList.remove("loading");
      },
    })
    .provide("jQuery", $)
    .use(i18n)
    .use(Vue3TouchEvents)
    .mount(element)
}

export { init };
