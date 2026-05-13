/**
 * See the NOTICE file distributed with this work for additional
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
// @ts-expect-error this is a JavaScript file, it is expected to not have types.
import { XWiki } from "../services/xwiki.js";

/**
 * Shared HTTP client for the Guided Tour REST API.
 * Encapsulates CSRF token resolution, fetch requests, and error notification.
 * @since 18.4.0RC1
 * @beta
 */
export class GuidedTourRestClient {
  /**
   * @param xm - A promise that resolves to the XWiki meta object containing the CSRF form token.
   */
  // @ts-expect-error xm is any
  constructor(xm: Promise) {
    // @ts-expect-error d is any
    this.xm = xm.then((d) => d).catch(console.error);
  }
  // @ts-expect-error xm is any
  private xm: Promise;

  // /**
  //  * Resolve the XWiki CSRF form token from the xwiki-meta module.
  //  */
  // private async getCSRFToken(): Promise<string> {
  //   return (this.xm instanceof Promise ? await this.xm : this.xm).form_token;
  // }

  /**
   * Perform a fetch request with the XWiki CSRF token header.
   * On non-OK responses a user-facing XWiki notification is shown and an error is thrown.
   * @param url - The full URL to request.
   * @param method - The HTTP method.
   * @param body - Optional JSON-serializable body for POST / PUT.
   */
  public async request<T>(
    url: string,
    method: "GET" | "POST" | "PUT" | "DELETE",
    body?: unknown,
  ): Promise<T> {
    const headers: Record<string, string> = {
      // "XWiki-Form-Token": await this.getCSRFToken(),
    };
    if (body !== undefined) {
      headers["Content-Type"] = "application/json";
    }
    const response = await fetch(url, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
    if (!response.ok) {
      this.handleError(response.status);
    }
    if (method === "GET") {
      return response.json();
    }
    return {} as T;
  }

  /**
   * Show an XWiki notification for the given HTTP status and throw.
   * @param status - The HTTP status code.
   */
  private handleError(status: number): never {
    let message: string;
    let type: "error" | "warning";
    switch (status) {
      case 401:
        message = "You don't have the rights to perform this operation.";
        type = "error";
        break;
      case 404:
        message = "The requested resource was not found.";
        type = "error";
        break;
      case 409:
        message = "The resource already exists.";
        type = "warning";
        break;
      default:
        message = "An unexpected error occurred.";
        type = "error";
        break;
    }
    new XWiki.widgets.Notification(message, type);
    throw new Error(`HTTP Error: ${status}`);
  }
}
