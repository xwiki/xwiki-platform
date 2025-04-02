/*
 * See the LICENSE file distributed with this work for additional
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
import { AutoSaver } from "./autoSaver";
import { HocuspocusProvider } from "@hocuspocus/provider";
import { Ref, ref } from "vue";

/**
 * Reactive reference used to share the realtime status between the UI components.
 * This ref is initialized by c-blocknote-view.vue.
 * @since 0.16
 */
const providerRef: Ref<HocuspocusProvider | undefined> = ref();

/**
 * Reactive reference to an autosaver the share the realtime status between the UI components.
 * This ref is initialized by c-blocknote-view.vue.
 * @since 0.16
 */
const autoSaverRef: Ref<AutoSaver | undefined> = ref();

export { autoSaverRef, providerRef };
