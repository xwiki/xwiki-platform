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

import type { Ref } from "vue";

type Action = { name: string; callback: () => void };
type Type = "info" | "success" | "warning" | "error";

/**
 * @since 0.11
 */
interface Alert {
  id: number;
  type: Type;
  message: string;
  actions?: Action[];
}

/**
 * Service to create and manage alert messages in Cristal.
 *
 * @since 0.11
 */
interface AlertsService {
  /**
   * List created and not dismissed alerts.
   * @returns the current alerts
   */
  list(): Ref<Alert[]>;

  /**
   * Create an "info" alert.
   * @param message - the content of the alert
   * @param actions - possible actions to include in the alert
   */
  info(message: string, actions?: Action[]): void;

  /**
   * Create a "success" alert.
   * @param message - the content of the alert
   * @param actions - possible actions to include in the alert
   */
  success(message: string, actions?: Action[]): void;

  /**
   * Create a "warning" alert.
   * @param message - the content of the alert
   * @param actions - possible actions to include in the alert
   */
  warning(message: string, actions?: Action[]): void;

  /**
   * Create an "error" alert.
   * @param message - the content of the alert
   * @param actions - possible actions to include in the alert
   */
  error(message: string, actions?: Action[]): void;

  /**
   * Dismiss an alert.
   * @param id - the id of the alert to dismiss
   */
  dismiss(id: number): void;
}

export type { Action, AlertsService, Alert, Type };
