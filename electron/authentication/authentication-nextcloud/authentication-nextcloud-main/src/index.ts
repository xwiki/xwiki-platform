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
} from "./storage.js";
import axios from "axios";
import { BrowserWindow, ipcMain, shell } from "electron";
import type { UserDetails } from "@xwiki/cristal-authentication-api";

const callbackUrl = "http://callback/";

async function getTokenFromCallbackCode(
  code: string,
  baseUrl: string,
  authenticationBaseUrl: string,
) {
  const tokenUrl = new URL(`${authenticationBaseUrl}/token`);
  tokenUrl.searchParams.set("base_url", baseUrl);
  tokenUrl.searchParams.set("code", code);
  tokenUrl.searchParams.set("redirect_uri", callbackUrl);
  const response = await axios.get(tokenUrl.toString());
  setTokenType(response.data.token_type, "oauth2");
  setAccessToken(response.data.access_token, "oauth2");
  setRefreshToken(response.data.refresh_token, "oauth2");
  // We apply a safety margin of 10s to the expiration date.
  setExpiryDate(Date.now() + (response.data.expires_in - 10) * 1000, "oauth2");
  setUserId(response.data.user_id, "oauth2");
}

function initAuth(
  win: BrowserWindow,
  reload: (win: BrowserWindow) => void,
  baseUrl: string,
  authenticationBaseUrl: string,
) {
  win.webContents.session.webRequest.onBeforeRequest(
    {
      urls: [`${callbackUrl}*`],
    },
    async ({ url }, callback) => {
      if (url.startsWith(authenticationBaseUrl)) {
        // Allow for the redirects from the oidc server to be performed without being blocked.
        callback({ cancel: false });
      } else {
        const parsedURL = new URL(url);
        await getTokenFromCallbackCode(
          parsedURL.searchParams.get("code")!,
          baseUrl,
          authenticationBaseUrl,
        );
        mainWin.show();
        reload(mainWin);
        win?.close();
      }
    },
  );
}

async function createWindow(url: string) {
  const win = new BrowserWindow({
    width: 800,
    height: 600,
    webPreferences: {
      nodeIntegration: false,
    },
  });

  win.setMenu(null);

  await win.loadURL(url);

  return win;
}

let authWin: BrowserWindow;
let mainWin: BrowserWindow;

export function load(
  browserWindow: BrowserWindow,
  reload: (win: BrowserWindow) => void,
): void {
  ipcMain.handle(
    "authentication:nextcloud:loginOauth2",
    async (
      _event,
      {
        baseUrl,
        authenticationBaseUrl,
      }: {
        baseUrl: string;
        authenticationBaseUrl: string;
      },
    ): Promise<void> => {
      const authorizationUrl = new URL(`${authenticationBaseUrl}/authorize`);
      authorizationUrl.searchParams.set("base_url", baseUrl);
      authorizationUrl.searchParams.set("redirect_uri", callbackUrl);
      // Save the window asking for login (i.e., the main window), before creating
      // a new windows for the oidc web page. Then, hide the main window for the
      // duration of the authentication process.
      mainWin = browserWindow;
      authWin = await createWindow(authorizationUrl.toString());

      initAuth(authWin, reload, baseUrl, authenticationBaseUrl);
      mainWin.hide();
    },
  );

  ipcMain.handle(
    "authentication:nextcloud:loginBasic",
    async (
      _event,
      {
        username,
        password,
      }: {
        username: string;
        password: string;
      },
    ): Promise<void> => {
      setAccessToken(btoa(`${username}:${password}`), "basic");
      setTokenType("Basic", "basic");
      setUserId(username, "basic");
      // We reload the content on successful login.
      reload(browserWindow);
    },
  );

  ipcMain.handle(
    "authentication:nextcloud:loginFlow",
    async (
      _event,
      {
        baseUrl,
      }: {
        baseUrl: string;
      },
    ): Promise<void> => {
      const loginFlowUrl = `${baseUrl}/index.php/login/v2`;

      const loginFlowResponse = await fetch(loginFlowUrl, { method: "POST" });
      const jsonLoginFlowResponse: {
        poll: { token: string; endpoint: string };
        login: string;
      } = await loginFlowResponse.json();

      shell.openExternal(jsonLoginFlowResponse.login);

      // This interval handles polling Nextcloud for the access token.
      // It will return a 404 error until the login process has succeeded.
      const intervalId = setInterval(async () => {
        const response = await fetch(jsonLoginFlowResponse.poll.endpoint, {
          method: "POST",
          body: new URLSearchParams({
            token: jsonLoginFlowResponse.poll.token,
          }),
          headers: {
            "Content-Type": "application/x-www-form-urlencoded",
          },
        });
        if (response.ok) {
          const jsonResponse: {
            loginName: string;
            appPassword: string;
          } = await response.json();
          setAccessToken(
            btoa(`${jsonResponse.loginName}:${jsonResponse.appPassword}`),
            "login-flow",
          );
          setTokenType("Basic", "login-flow");
          setUserId(jsonResponse.loginName, "login-flow");
          clearInterval(intervalId);
          // We reload the content on successful login.
          reload(browserWindow);
        }
      }, 3000);
    },
  );

  ipcMain.handle(
    "authentication:nextcloud:isLoggedIn",
    async (_event, { mode }: { mode: string }) => {
      const tokenType = getTokenType(mode);
      const accessTokenKey = getAccessToken(mode);
      return tokenType && accessTokenKey;
    },
  );

  ipcMain.handle(
    "authentication:nextcloud:userDetails",
    async (
      _event,
      { baseUrl, mode }: { baseUrl: string; mode: string },
    ): Promise<UserDetails> => {
      const userId = getUserId(mode);
      return {
        profile: `${baseUrl}/u/${userId}`,
        username: userId,
        name: userId!, // TODO: Find a way to get the display name (CRISTAL-589).
        avatar: `${baseUrl}/avatar/${userId}/64`, // We want the 64x64 avatar.
      };
    },
  );

  ipcMain.handle(
    "authentication:nextcloud:authorizationValue",
    async (_event, { mode }: { mode: string }) => {
      return {
        tokenType: getTokenType(mode),
        accessToken: getAccessToken(mode),
      };
    },
  );

  ipcMain.handle(
    "authentication:nextcloud:logout",
    async (_event, { mode }: { mode: string }): Promise<void> => {
      deleteAccessToken(mode);
      deleteTokenType(mode);
      deleteRefreshToken(mode);
      deleteExpiryDate(mode);
      deleteUserId(mode);
    },
  );

  ipcMain.handle(
    "authentication:nextcloud:refreshToken",
    async (
      _event,
      {
        baseUrl,
        authenticationBaseUrl,
      }: { baseUrl: string; authenticationBaseUrl: string },
    ): Promise<void> => {
      if (Date.now() > getExpiryDate("oauth2")) {
        const refreshUrl = new URL(`${authenticationBaseUrl}/refresh`);
        refreshUrl.searchParams.set("base_url", baseUrl);
        refreshUrl.searchParams.set("refresh_token", getRefreshToken("oauth2"));

        const {
          data: {
            access_token: accessToken,
            refresh_token: refreshToken,
            expires_in: expiresIn,
          },
        } = await axios.get(refreshUrl.toString());

        setAccessToken(accessToken, "oauth2");
        setRefreshToken(refreshToken, "oauth2");
        // We apply a safety margin of 10s to the expiration date.
        setExpiryDate(Date.now() + (expiresIn - 10) * 1000, "oauth2");
      }
    },
  );
}
