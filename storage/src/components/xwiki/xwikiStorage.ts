/**
 * See the LICENSE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 * This file is part of the Cristal Wiki software prototype
 * @copyright  Copyright (c) 2023 XWiki SAS
 * @license    http://opensource.org/licenses/AGPL-3.0 AGPL-3.0
 *
 **/

import { inject, injectable } from "inversify";
import "reflect-metadata";
import type { Document, PageData } from "@cristal/api";
import { DefaultPageData, JSONLDDocument, type Logger } from "@cristal/api";
import { AbstractStorage } from "../abstractStorage";

@injectable()
export class XWikiStorage extends AbstractStorage {
  constructor(@inject<Logger>("Logger") logger: Logger) {
    super(logger, "storage.components.xwikiStorage");
  }

  public async isStorageReady(): Promise<boolean> {
    return true;
  }

  getPageRestURL(page: string, syntax: string): string {
    this.logger?.debug("XWiki Loading page", page);
    return (
      this.wikiConfig.baseURL +
      this.wikiConfig.baseRestURL +
      "&page=" +
      page +
      "&format=" +
      syntax
    );
  }

  getPageFromViewURL(url: string): string | null {
    let page = null;
    if (url.startsWith(this.wikiConfig.baseURL)) {
      const uri = url.replace(this.wikiConfig.baseURL, "");
      page = uri.replace("/bin", "").replace("/view/", "").replaceAll("/", ".");
      if (page.endsWith(".")) {
        page += "WebHome";
      }
    } else {
      page = url;
    }
    return page;
  }

  getImageURL(page: string, image: string): string {
    if (page == "") {
      page = "Main.WebHome";
    }
    const imageURL =
      this.wikiConfig.baseURL +
      "/bin/download/" +
      page.replace(".", "/") +
      "/" +
      image;
    this.logger?.debug("final image url ", imageURL);
    return imageURL;
  }

  async getPageContent(page: string, syntax: string): Promise<PageData> {
    this.logger?.debug("XWiki Loading page", page);
    if (page == "") {
      page = "Main.WebHome";
    }
    const url = this.getPageRestURL(page, syntax);
    this.logger?.debug("XWiki Loading url", url);
    const response = await fetch(url, { cache: "no-store" });
    const json = await response.json();
    let source = "";
    let html = "";
    let jsonContent = {};
    if (syntax == "jsonld") {
      jsonContent = json;
      if (this.wikiConfig.serverRendering) {
        this.logger?.debug("Using server side rendering for jsonld");
        source = json.source;
        html = json.html;
      } else {
        this.logger?.debug("Using client side rendering for jsonld");
        source = json.source;
        html = "";
      }
    } else if (syntax == "html") {
      if (this.wikiConfig.serverRendering) {
        this.logger?.debug("Using server side rendering for html");
        source = json.source;
        html = "";
      } else {
        this.logger?.debug("Using client side rendering for html");
        source = json.source;
        html = "";
      }
    } else {
      source = "";
      html = "";
    }

    const pageContentData = new DefaultPageData();
    pageContentData.source = source;
    pageContentData.syntax = "xwiki";
    pageContentData.html = html;
    pageContentData.document = new JSONLDDocument(jsonContent);
    pageContentData.css = json.css;
    pageContentData.js = json.js;
    pageContentData.version = pageContentData.document.get("version");
    return pageContentData;
  }

  async getPanelContent(panel: string, contextPage: string): Promise<PageData> {
    const url =
      this.wikiConfig.baseURL +
      "/rest/cristal/panel?media=json" +
      "&page=" +
      contextPage +
      "&panel=" +
      panel;
    this.logger?.debug("XWiki Loading url", url);
    const response = await fetch(url, { cache: "no-store" });
    const json = await response.json();
    const panelContentData = new DefaultPageData();
    panelContentData.source = json.source;
    panelContentData.syntax = "xwiki";
    panelContentData.html = json.content;
    panelContentData.css = json.css;
    panelContentData.js = json.js;
    return panelContentData;
  }

  async getEditField(document: Document, fieldName: string): Promise<string> {
    // http://localhost:15680/xwiki/bin/get/Blog/BlogIntroduction?xpage=display&mode=edit&property=Blog.BlogPostClass.category
    // http://localhost:15680/xwiki/bin/get/Help/Applications/Movies/Modern%20Times?xpage=display&mode=edit&property=Help.Applications.Movies.Code.MoviesClass%5B0%5D.staticList1&type=object&language=
    //
    try {
      // TODO get rid of any
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const fieldMapping: any = document.get("xwikiMapping")[fieldName];
      let type = fieldMapping["type"];
      let xwikiFieldName = fieldMapping["fieldName"];
      if (type == null) {
        type = "document";
      }
      if (xwikiFieldName == null) {
        xwikiFieldName = fieldName;
      }
      const url =
        this.wikiConfig.baseURL +
        "/bin/get/" +
        document.getIdentifier().replaceAll(".", "/") +
        "?xpage=display&mode=edit&type=" +
        type +
        "&property=" +
        document.get("className") +
        "." +
        xwikiFieldName;
      this.logger?.debug("XWiki Loading Field url", url);
      const response = await fetch(url, { cache: "no-store" });
      return await response.text();
    } catch (e) {
      this.logger?.error(
        "Exception looking for edit field for field",
        fieldName,
        e,
      );
      return "";
    }
  }

  async save(page: string, content: string): Promise<unknown> {
    const url = this.wikiConfig.baseURL;
    const segments = ["rest", "wikis", "xwiki"];
    const referenceParts = page.split(".");
    for (let i = 0; i < referenceParts.length; i++) {
      const segment = i < referenceParts.length - 1 ? "spaces" : "pages";
      segments.push(segment);
      segments.push(referenceParts[i]);
    }

    const fullUrl = `${url}/${segments.join("/")}`;

    await fetch(fullUrl, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        // TODO: externalize credentials
        Authorization: `Basic ${btoa("Admin:admin")}`,
      },
      // TODO: the syntax provided by the save is ignored and the content is always saved as markdown.
      body: JSON.stringify({ content, syntax: "markdown/1.2" }),
    });

    return;
  }
}
