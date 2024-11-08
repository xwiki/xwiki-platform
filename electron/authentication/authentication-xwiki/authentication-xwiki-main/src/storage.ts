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

import Store from "electron-store";

const tokenTypeKey = "tokenType";

const accessTokenKey = "accessToken";

const schema = {
  tokenType: {
    type: "string",
  },

  accessToken: {
    type: "string",
  },
};

const storeInstance: Store = new Store({
  name: "authentication-xwiki-main",
  schema,
  // TODO: add encryption key
});

function set<T>(key: string, value: T) {
  // @ts-expect-error type resolution failing because of electron-store library bug
  storeInstance.set(key, value);
}

function get<T>(key: string): T {
  // @ts-expect-error type resolution failing because of electron-store library bug
  return storeInstance.get(key) as T;
}

function rm(key: string) {
  // @ts-expect-error type resolution failing because of electron-store library bug
  storeInstance.delete(key);
}

function setTokenType(value: string): void {
  set(tokenTypeKey, value);
}

function setAccessToken(value: string): void {
  set(accessTokenKey, value);
}

function getTokenType(): string {
  return get(tokenTypeKey);
}

function getAccessToken(): string {
  return get(accessTokenKey);
}

function deleteTokenType(): void {
  rm(tokenTypeKey);
}

function deleteAccessToken(): void {
  rm(accessTokenKey);
}

export {
  deleteAccessToken,
  deleteTokenType,
  getAccessToken,
  getTokenType,
  setAccessToken,
  setTokenType,
};
