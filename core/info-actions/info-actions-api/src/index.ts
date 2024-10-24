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
import { Ref } from "vue";

/**
 * The information to provide for an info action element.
 *
 * @since 0.9
 */
interface InfoAction {
  id: string;
  iconName: string;
  counter(): Promise<Ref<number>>;
  order: number;

  /**
   * And optional refresh action. Otherwise, we assume asking for the counter
   * is enough.
   * @param page - an option page reference, otherwise the current page is used
   * @since 0.10
   */
  refresh?(page?: string): Promise<void>;
}

/**
 * Provide the operations to interact with the info actions. Currently, returns
 * the full list of available info actions.
 *
 * @since 0.9
 */
interface InfoActionsService {
  list(): Promise<InfoAction[]>;
}

export type { InfoActionsService, InfoAction };
