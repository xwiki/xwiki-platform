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

import { loadById } from "@xwiki/platform-xwiki-utils";
import { onBeforeUnmount, onMounted } from "vue";
import type { Ref } from "vue";

/**
 * The subset of the jQuery API used to control the Bootstrap 3 modals, which are implemented as a jQuery plugin.
 */
type JQueryModal = {
  modal(action: "show" | "hide"): void;
  on(events: string, handler: () => void): void;
  off(events: string, handler: () => void): void;
};

type JQuery = (element: Element) => JQueryModal;

/**
 * The Bootstrap modal events the realtime toolbar listens to.
 */
type ModalEvent = "show" | "shown" | "hidden";

let jQueryPromise: Promise<JQuery> | undefined;

function getJQuery(): Promise<JQuery> {
  jQueryPromise ??= loadById<JQuery>("jquery");
  return jQueryPromise;
}

/**
 * Hides the given Bootstrap modal.
 *
 * @param modal - the modal to hide
 */
async function hideModal(modal: HTMLElement): Promise<void> {
  (await getJQuery())(modal).modal("hide");
}

/**
 * Listens to a Bootstrap modal event. Note that these are jQuery events, so they can't be caught with native event
 * listeners.
 *
 * @param modal - the modal to listen to
 * @param event - the modal event to listen to
 * @param listener - the function to call when the event is fired
 * @returns a function that removes the listener
 */
async function onModalEvent(
  modal: HTMLElement,
  event: ModalEvent,
  listener: () => void,
): Promise<() => void> {
  const $ = await getJQuery();
  const eventName = `${event}.bs.modal`;
  $(modal).on(eventName, listener);
  return () => $(modal).off(eventName, listener);
}

/**
 * Listens to a Bootstrap modal event while the calling component is mounted.
 *
 * @param modal - the modal to listen to
 * @param event - the modal event to listen to
 * @param listener - the function to call when the event is fired
 */
function useModalEvent(
  modal: Readonly<Ref<HTMLElement | null>>,
  event: ModalEvent,
  listener: () => void,
): void {
  let removeListener: Promise<() => void> | undefined;
  onMounted(() => {
    removeListener = onModalEvent(modal.value!, event, listener);
  });
  onBeforeUnmount(async () => (await removeListener)?.());
}

export { hideModal, onModalEvent, useModalEvent };
export type { ModalEvent };
