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
import RootComponent from "./vue/RootComponent.vue";
import { initialize } from "../api";
import { translatorFactory as translatorFactoryFile } from "../impl-file";
import { translatorFactory as translatorFactoryFileRest } from "../impl-rest";
import { createApp } from "vue";
import { createI18n } from "vue-i18n";
import "../consumer-requirejs";

declare const XWiki: {
  contextPath: string;
  currentWiki: string;
};

const translation = initialize(
  translatorFactoryFile("translations"),
  translatorFactoryFileRest(
    `${XWiki.contextPath}/rest/wikis/${encodeURIComponent(XWiki.currentWiki)}/localization/translations`,
  ),
);

const i18n = createI18n({});

createApp(RootComponent)
  .provide("resolver", translation)
  .use(i18n)
  .mount("#app");
