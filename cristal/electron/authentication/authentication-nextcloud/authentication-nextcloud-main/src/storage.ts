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

const refreshTokenKey = "refreshToken";

const expiryDateKey = "expiryDate";

const userIdKey = "userId";

const schema = {
  tokenType: {
    type: "string",
  },

  accessToken: {
    type: "string",
  },

  refreshToken: {
    type: "string",
  },

  expiryDate: {
    type: "number",
  },

  userId: {
    type: "string",
  },
};

type StoreType = {
  tokenType: string;
  accessToken: string;
  refreshToken: string;
  expiryDate: number;
  userId: string;
};

const storeInstance: Store<StoreType> = new Store<StoreType>({
  name: "authentication-nextcloud-main",
  schema,
  // TODO: add encryption key
});

function setTokenType(value: string, mode: string): void {
  storeInstance.set(`${tokenTypeKey}-${mode}`, value);
}

function setAccessToken(value: string, mode: string): void {
  storeInstance.set(`${accessTokenKey}-${mode}`, value);
}

function setRefreshToken(value: string, mode: string): void {
  storeInstance.set(`${refreshTokenKey}-${mode}`, value);
}

function setExpiryDate(value: number, mode: string): void {
  storeInstance.set(`${expiryDateKey}-${mode}`, value);
}

function setUserId(value: string, mode: string): void {
  storeInstance.set(`${userIdKey}-${mode}`, value);
}

function getTokenType(mode: string): string {
  return storeInstance.get(`${tokenTypeKey}-${mode}`);
}

function getAccessToken(mode: string): string {
  return storeInstance.get(`${accessTokenKey}-${mode}`);
}

function getRefreshToken(mode: string): string {
  return storeInstance.get(`${refreshTokenKey}-${mode}`);
}

function getExpiryDate(mode: string): number {
  return storeInstance.get(`${expiryDateKey}-${mode}`);
}

function getUserId(mode: string): string {
  return storeInstance.get(`${userIdKey}-${mode}`);
}

function deleteTokenType(mode: string): void {
  // @ts-expect-error resolution failing because of electron-store library bug
  storeInstance.delete(`${tokenTypeKey}-${mode}`);
}

function deleteAccessToken(mode: string): void {
  // @ts-expect-error resolution failing because of electron-store library bug
  storeInstance.delete(`${accessTokenKey}-${mode}`);
}

function deleteRefreshToken(mode: string): void {
  // @ts-expect-error resolution failing because of electron-store library bug
  storeInstance.delete(`${refreshTokenKey}-${mode}`);
}

function deleteExpiryDate(mode: string): void {
  // @ts-expect-error resolution failing because of electron-store library bug
  storeInstance.delete(`${expiryDateKey}-${mode}`);
}

function deleteUserId(mode: string): void {
  // @ts-expect-error resolution failing because of electron-store library bug
  storeInstance.delete(`${userIdKey}-${mode}`);
}

export {
  deleteAccessToken,
  deleteExpiryDate,
  deleteRefreshToken,
  deleteTokenType,
  deleteUserId,
  getAccessToken,
  getExpiryDate,
  getRefreshToken,
  getTokenType,
  getUserId,
  setAccessToken,
  setExpiryDate,
  setRefreshToken,
  setTokenType,
  setUserId,
};
