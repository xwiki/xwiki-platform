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
import { readonly, ref, toValue, useId } from "vue";
import type { MaybeRefOrGetter, Ref } from "vue";

/**
 * Handles a Bootstrap 3 dropdown whose menu is a popover of its toggle button: the identifier that
 * links them, the open state of the menu, and the clicks and keys that open or close it.
 *
 * @param menu - the menu element, which has the popover attribute
 * @param closeOnClick - whether the menu closes on any click inside it, like a Bootstrap dropdown
 * @returns the menu identifier for the popovertarget attribute of the toggle, whether the menu is
 *   open, and the listeners to bind to the menu and to the toggle
 * @since 18.10.0RC1
 */
export function usePopoverDropdown(
  menu: Readonly<Ref<HTMLElement | null>>,
  closeOnClick: MaybeRefOrGetter<boolean> = true,
) {
  const menuId = useId();
  const isOpen = ref(false);

  // Listener of the click events of the menu.
  function onMenuClick() {
    if (toValue(closeOnClick)) {
      menu.value?.hidePopover();
    }
  }

  // Listener of the beforetoggle events of the menu, fired when its popover opens or closes. Unlike
  // the toggle events, which some browsers fire late, they are fired before the next key reaches
  // the Bootstrap dropdown script, which needs the open class of the dropdown.
  function onMenuToggle(event: ToggleEvent) {
    isOpen.value = event.newState === "open";
  }

  // Listener of the arrow keys on the toggle. Like the Bootstrap dropdown script does for its
  // toggles, they open the menu and move the focus to its first item.
  function onToggleArrowKey() {
    if (!isOpen.value) {
      menu.value?.showPopover();
    }
    menu.value?.querySelector<HTMLElement>("li:not(.disabled) a")?.focus();
  }

  // Listener of the Escape key on the toggle. While the menu is open, the key is kept from the page
  // keyboard shortcuts, which would prevent it from closing the popover.
  function onToggleEscape(event: KeyboardEvent) {
    if (isOpen.value) {
      event.stopPropagation();
    }
  }

  return {
    menuId,
    isOpen: readonly(isOpen),
    onMenuClick,
    onMenuToggle,
    onToggleArrowKey,
    onToggleEscape,
  };
}
