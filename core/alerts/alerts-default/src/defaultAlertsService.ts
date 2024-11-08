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

import { injectable } from "inversify";
import { Store, StoreDefinition, defineStore, storeToRefs } from "pinia";
import { Ref } from "vue";
import type {
  Action,
  Alert,
  AlertsService,
  Type as AlertType,
} from "@xwiki/cristal-alerts-api";

type Id = "alerts";
type State = {
  alerts: Alert[];
  id: number;
};
/**
 * Take a given type "Type" and wraps each of its fields in a readonly Ref.
 */
type WrappedRefs<Type> = {
  readonly [Property in keyof Type]: Ref<Type[Property]>;
};
type StateRefs = WrappedRefs<State>;
type Getters = Record<string, never>;
type Actions = {
  createAlert(type: AlertType, message: string, actions?: Action[]): void;
  deleteAlert(id: number): void;
};
type AlertsStoreDefinition = StoreDefinition<Id, State, Getters, Actions>;
type AlertsStore = Store<Id, State, Getters, Actions>;

const alertsStore: AlertsStoreDefinition = defineStore<
  Id,
  State,
  Getters,
  Actions
>("alerts", {
  state() {
    return {
      alerts: [],
      id: 0,
    };
  },
  actions: {
    createAlert(type: AlertType, message: string, actions?: Action[]) {
      this.alerts.push({
        id: this.id++,
        type: type,
        message: message,
        actions: actions,
      });
    },
    deleteAlert(id: number) {
      for (const i of this.alerts.keys()) {
        if (this.alerts[i].id == id) {
          this.alerts.splice(i, 1);
          break;
        }
      }
    },
  },
});

/**
 * @since 0.11
 */
@injectable()
export class DefaultAlertsService implements AlertsService {
  private readonly refs: StateRefs;
  private readonly store: AlertsStore;

  constructor() {
    // An internal store is kept to easily provide refs for updatable elements.
    this.store = alertsStore();
    this.refs = storeToRefs(this.store);
  }

  list(): StateRefs["alerts"] {
    return this.refs.alerts;
  }

  info(message: string, actions?: Action[]): void {
    this.store.createAlert("info", message, actions);
  }

  success(message: string, actions?: Action[]): void {
    this.store.createAlert("success", message, actions);
  }

  warning(message: string, actions?: Action[]): void {
    this.store.createAlert("warning", message, actions);
  }

  error(message: string, actions?: Action[]): void {
    this.store.createAlert("error", message, actions);
  }

  dismiss(id: number): void {
    this.store.deleteAlert(id);
  }
}
