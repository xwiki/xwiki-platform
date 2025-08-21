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

import {
  deleteAccessToken,
  deleteTokenType,
  getAccessToken,
  getTokenType,
  setAccessToken,
  setTokenType,
} from "./storage.js";
import axios from "axios";
import { BrowserWindow, ipcMain } from "electron";
import type { UserDetails } from "@xwiki/cristal-authentication-api";

function getAuthorizationValue() {
  return `${getTokenType()} ${getAccessToken()}`;
}

export function load(
  browserWindow: BrowserWindow,
  reload: (win: BrowserWindow) => void,
): void {
  ipcMain.handle(
    "authentication:github:login",
    async (
      _event,
      {
        baseUrl,
        deviceCode,
        interval,
        expiresIn,
      }: {
        baseUrl: string;
        deviceCode: string;
        interval: string;
        expiresIn: string;
      },
    ) => {
      const verificationUrl = new URL(`${baseUrl}/device-verify`);
      verificationUrl.searchParams.set("device_code", deviceCode);

      // This converts the interval polling time and expiration time provided by
      // GitHub to milliseconds.
      // We need to add a little more delay to the interval when polling, just to
      // be sure that we don't go to fast for GitHub (or we might get rate
      // limited).
      const intervalMs: number = parseInt(interval) * 1000 + 500;
      let expiresInMs: number = parseInt(expiresIn) * 1000;

      // This interval handles polling the backend for the access token, using
      // the interval time computed earlier.
      // The backend will return an error until the login process has succeeded.
      const intervalId = setInterval(async () => {
        const response = await fetch(verificationUrl);
        const jsonResponse: {
          error?: string;
          access_token?: string;
          token_type?: string;
        } = await response.json();
        if (!jsonResponse.error) {
          setAccessToken(jsonResponse.access_token!);
          setTokenType(jsonResponse.token_type!);
          clearInterval(intervalId);
          // We reload the content on successful login.
          reload(browserWindow);
        } else if (expiresInMs <= 0) {
          clearInterval(intervalId);
        }
        expiresInMs -= intervalMs;
      }, intervalMs);
    },
  );

  ipcMain.handle("authentication:github:isLoggedIn", () => {
    const tokenType = getTokenType();
    const accessTokenKey = getAccessToken();
    return tokenType && accessTokenKey;
  });

  ipcMain.handle(
    "authentication:github:userDetails",
    async (): Promise<UserDetails> => {
      const userinfoUrl = "https://api.github.com/user";
      const data = {
        headers: {
          Authorization: getAuthorizationValue(),
        },
      };
      const {
        data: { login, html_url, name, avatar_url },
      } = await axios.get(userinfoUrl, data);

      return { profile: html_url, username: login, name, avatar: avatar_url };
    },
  );
  ipcMain.handle("authentication:github:authorizationValue", () => {
    return {
      tokenType: getTokenType(),
      accessToken: getAccessToken(),
    };
  });
  ipcMain.handle("authentication:github:logout", () => {
    deleteAccessToken();
    deleteTokenType();
  });
}
