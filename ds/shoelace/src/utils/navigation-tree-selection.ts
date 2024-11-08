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

import type SlTreeItem from "@shoelace-style/shoelace/dist/components/tree-item/tree-item";
import type { Ref } from "vue";

/**
 * Utility class to manage the selected node in a Navigation Tree for Shoelace ds.
 *
 * @since 0.11
 **/
export class NavigationTreeSelection {
  private selectedTreeItem: SlTreeItem | undefined = undefined;
  private treeItems: Ref<Map<string, SlTreeItem>>;

  constructor(treeItems: Ref<Map<string, SlTreeItem>>) {
    this.treeItems = treeItems;
  }

  public updateSelection(id: string) {
    if (this.selectedTreeItem) {
      this.selectedTreeItem.selected = false;
    }
    this.selectedTreeItem = this.treeItems.value.get(id);
    this.selectedTreeItem!.selected = true;
  }

  public getSelection(): SlTreeItem | undefined {
    return this.selectedTreeItem;
  }

  public onSelectionChange(event: unknown) {
    // We don't want users to manually select a node, so we undo any change.
    (
      event as { detail: { selection: SlTreeItem[] } }
    ).detail.selection[0].selected = false;
    if (this.selectedTreeItem) {
      this.selectedTreeItem!.selected = true;
    }
  }
}
