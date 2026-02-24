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
import XWikiLivedata from "./components/XWikiLivedata.vue";
// @ts-expect-error this is a JavaScript Vue component, it is expected to not have types.
import BaseDisplayer from "./components/displayers/BaseDisplayer.vue";
// @ts-expect-error this is a JavaScript file, it is expected to not have types.
import displayerMixin from "./components/displayers/displayerMixin";
// @ts-expect-error this is a JavaScript file, it is expected to not have types.
import { populateStore } from "./components/populateStore.js";
// @ts-expect-error this is a JavaScript Vue component, it is expected to not have types.
import XWikiIcon from "./components/utilities/XWikiIcon.vue";
// @ts-expect-error this is a JavaScript file, it is expected to not have types.
import { loadById } from "./services/require.js";

export {
  BaseDisplayer,
  XWikiIcon,
  XWikiLivedata,
  displayerMixin,
  loadById,
  populateStore,
};
