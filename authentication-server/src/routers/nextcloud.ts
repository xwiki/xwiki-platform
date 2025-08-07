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

import { config } from "../config";
import express from "express";
import type { Request, Response, Router } from "express";

export const router: Router = express.Router();

router.get(
  "/authorize",
  async (
    req: Request<
      unknown,
      unknown,
      unknown,
      { base_url: string; redirect_uri: string }
    >,
    res: Response,
  ) => {
    const oauthAuthorizeUrl = new URL(
      `${req.query.base_url}${config.nextcloud.OAUTH2_AUTHORIZE_PATH}`,
    );
    oauthAuthorizeUrl.searchParams.set("response_type", "code");
    oauthAuthorizeUrl.searchParams.set(
      "client_id",
      config.nextcloud.OAUTH2_CLIENT_ID,
    );
    oauthAuthorizeUrl.searchParams.set("redirect_uri", req.query.redirect_uri);

    res.redirect(oauthAuthorizeUrl.toString());
  },
);

router.get(
  "/token",
  async (
    req: Request<
      unknown,
      unknown,
      unknown,
      { base_url: string; code: string; redirect_uri: string }
    >,
    res: Response,
  ) => {
    const tokenUrl = new URL(
      `${req.query.base_url}${config.nextcloud.OAUTH2_TOKEN_PATH}`,
    );
    const deviceVerifyResponse = await fetch(tokenUrl, {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
      },
      body: new URLSearchParams({
        grant_type: "authorization_code",
        code: req.query.code,
        redirect_uri: req.query.redirect_uri,
        client_id: config.nextcloud.OAUTH2_CLIENT_ID,
        client_secret: config.nextcloud.OAUTH2_CLIENT_SECRET,
      }),
    });
    res.setHeader("Access-Control-Allow-Origin", "*");
    res.send(await deviceVerifyResponse.text());
  },
);

router.get(
  "/refresh",
  async (
    req: Request<
      unknown,
      unknown,
      unknown,
      { base_url: string; refresh_token: string }
    >,
    res: Response,
  ) => {
    const tokenUrl = new URL(
      `${req.query.base_url}${config.nextcloud.OAUTH2_TOKEN_PATH}`,
    );
    const deviceVerifyResponse = await fetch(tokenUrl, {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
      },
      body: new URLSearchParams({
        grant_type: "refresh_token",
        client_id: config.nextcloud.OAUTH2_CLIENT_ID,
        client_secret: config.nextcloud.OAUTH2_CLIENT_SECRET,
        refresh_token: req.query.refresh_token,
      }),
    });
    res.setHeader("Access-Control-Allow-Origin", "*");
    res.send(await deviceVerifyResponse.text());
  },
);
