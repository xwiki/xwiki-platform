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

import { InfoAction } from "@xwiki/cristal-info-actions-api";
import { injectable } from "inversify";
import { ref } from "vue";

@injectable()
export class CommentsInfoAction implements InfoAction {
  iconName = "chat";
  id = "comments";
  order = 2000;

  async counter() {
    return ref(99);
  }

  async enabled(): Promise<boolean> {
    // TODO: Fix CRISTAL-47 to enable it.
    return false;
  }
}
