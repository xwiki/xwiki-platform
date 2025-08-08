/**
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

type StoreType = {
  tokenType: string;
  accessToken: string;
};

const schema = {
  tokenType: {
    type: "string",
  },

  accessToken: {
    type: "string",
  },
};

const storeInstance: Store<StoreType> = new Store<StoreType>({
  name: "authentication-github-main",
  schema,
  // TODO: add encryption key
});

function setTokenType(value: string): void {
  storeInstance.set(tokenTypeKey, value);
}

function setAccessToken(value: string): void {
  storeInstance.set(accessTokenKey, value);
}

function getTokenType(): string {
  return storeInstance.get(tokenTypeKey);
}

function getAccessToken(): string {
  return storeInstance.get(accessTokenKey);
}

function deleteTokenType(): void {
  storeInstance.delete(tokenTypeKey);
}

function deleteAccessToken(): void {
  storeInstance.delete(accessTokenKey);
}

export {
  deleteAccessToken,
  deleteTokenType,
  getAccessToken,
  getTokenType,
  setAccessToken,
  setTokenType,
};
