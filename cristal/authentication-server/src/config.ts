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

const config = {
  github: {
    DEVICE_LOGIN_URL: "https://github.com/login/device/code",
    DEVICE_VERIFY_URL: "https://github.com/login/oauth/access_token",
    /**
     * Client ID of the GitHub app to use for log-in.
     * See https://docs.github.com/en/apps
     */
    APP_CLIENT_ID: "",
  },
  nextcloud: {
    OAUTH2_AUTHORIZE_PATH: "/index.php/apps/oauth2/authorize",
    /**
     * ID of the OAuth2 client to use for log-in.
     * See https://docs.nextcloud.com/server/latest/admin_manual/configuration_server/oauth2.html
     */
    OAUTH2_CLIENT_ID: "",
    /**
     * Secret of the OAuth2 client to use for log-in.
     * See https://docs.nextcloud.com/server/latest/admin_manual/configuration_server/oauth2.html
     */
    OAUTH2_CLIENT_SECRET: "",
    OAUTH2_TOKEN_PATH: "/index.php/apps/oauth2/api/v1/token",
  },
};

export { config };
