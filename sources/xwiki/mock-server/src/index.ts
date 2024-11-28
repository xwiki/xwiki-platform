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

import dotenv from "dotenv";
import express, { Express, Request, Response } from "express";

dotenv.config();

const app: Express = express();
const port = process.env.PORT;

// Provide a root path with some content for playwright to be able to detect when the server is up and running.
app.get("/", (_req, res) => {
  res.send("READY");
});

let offlineCount = 0;

function getHtml(
  page: string,
  revision: string | undefined,
  options: { offline: boolean },
) {
  let html = `<h1>Welcome to ${page}!</h1>

XWiki is the best tool to organize your knowledge.

<a href="Page2.WebHome">XWiki Syntax</a>
${revision ? "Revision " + revision : ""}
`;

  if (options.offline) {
    // Add a movable part when offline, to make it easier to assert the
    // asynchronous refresh feature of offline mode.
    html += `<p class="offlinecount">${offlineCount}</p>`;
    offlineCount++;
  }
  return html;
}

app.get("/xwiki/rest/cristal/page", (req: Request, res: Response) => {
  res.appendHeader("Access-Control-Allow-Origin", "*");

  const id: string = req.query.page as string;
  let page: string = "";
  let name: string = "";
  let revision: string | undefined = undefined;
  let text: string = "";
  let html: string = "";

  switch (id) {
    case "Main.WebHome":
      page = "Main.WebHome";
      name = "WebHome";
      revision = (req.query.revision as string) || undefined;
      text = `= Welcome to ${page} =

XWiki is the best tool to organize your knowledge.

[[XWiki Syntax>>Page2.WebHome]]
${revision ? "Revision " + revision : ""}
}`;
      html = getHtml(page, revision, { offline: false });
      break;

    case "Main.Offline":
      page = "Main.Offline";
      name = "Offline";
      html = getHtml(page, revision, { offline: true });
      break;

    case "Main.NewPage.WebHome":
      name = "NewPage";
      break;

    default:
      page = id;
      name = "WebHome";
      html = getHtml(page, revision, { offline: false });
  }

  res.json({
    "@context": "https://schema.org",
    "@type": "CreativeWork",
    identifier: id,
    name: name,
    headline: page,
    headlineRaw: page,
    creator: page,
    encodingFormat: "xwiki/2.1",
    text: text,
    html: html,
  });
});

app.get("/xwiki/rest/cristal/panel", (req: Request, res: Response) => {
  const panel = req.query.panel || "Main.WebHome";

  res.appendHeader("Access-Control-Allow-Origin", "*");

  res.json({
    content: `<div>${panel}</div>`,
    source: `= ${panel} =`,
  });
});

app.get(
  "/xwiki/rest/wikis/xwiki/spaces/Main/pages/WebHome",
  (req: Request, res: Response) => {
    res.appendHeader("Access-Control-Allow-Origin", "*");

    res.json({
      hierarchy: {
        items: [
          {
            label: "xwiki",
            name: "xwiki",
            type: "wiki",
            url: `${req.protocol}://${req.headers.host}/xwiki/bin/view/Main/`,
          },
          {
            label: "Main",
            name: "Main",
            type: "space",
            url: `${req.protocol}://${req.headers.host}/xwiki/bin/view/Main/`,
          },
          {
            label: "WebHome",
            name: "WebHome",
            type: "document",
            url: `${req.protocol}://${req.headers.host}/xwiki/bin/view/Main/`,
          },
        ],
      },
    });
  },
);

app.get(
  "/xwiki/rest/wikis/xwiki/spaces/Main/pages/WebHome/history",
  (_req: Request, res: Response) => {
    res.appendHeader("Access-Control-Allow-Origin", "*");

    res.json({
      historySummaries: [
        {
          version: "3.1",
          modified: 1704139200000,
          modifier: "XWiki.User3",
          comment: "Best version",
        },
        {
          version: "2.1",
          modified: 1641067200000,
          modifier: "XWiki.User2",
          comment: "",
        },
        {
          version: "1.1",
          modified: 1577908800000,
          modifier: "XWiki.User1",
          comment: "Initial version",
        },
      ],
    });
  },
);

app.get("/xwiki/bin/get", (req: Request, res: Response) => {
  res.appendHeader("Access-Control-Allow-Origin", "*");

  if (req.query["sheet"] == "XWiki.DocumentTree") {
    switch (req.query["id"]) {
      case "#":
        res.json([
          {
            id: "document:xwiki:Help.WebHome",
            text: "Help",
            children: true,
            data: { type: "document" },
            a_attr: { href: "/xwiki/bin/view/Help/" },
          },
          {
            id: "document:xwiki:Terminal",
            text: "Terminal Page",
            children: false,
            data: { type: "document" },
            a_attr: { href: "/xwiki/bin/view/Terminal" },
          },
          {
            id: "document:xwiki:Deep1.WebHome",
            text: "Deep Page Root",
            children: true,
            data: { type: "document" },
            a_attr: { href: "/xwiki/bin/view/Deep1/" },
          },
          {
            id: "document:xwiki:Main.WebHome",
            text: "Cristal Wiki",
            children: false,
            data: { type: "document" },
            a_attr: { href: "/xwiki/bin/view/Main/" },
          },
        ]);
        break;

      case "document:xwiki:Deep1.WebHome":
        res.json([
          {
            id: "document:xwiki:Deep1.WebHome",
            text: "Translations",
            children: true,
            data: { type: "translations" },
          },
          {
            id: "document:xwiki:Deep1.WebHome",
            text: "Attachments",
            children: true,
            data: { type: "attachments" },
            a_attr: { href: "/xwiki/bin/view/Deep1/?viewer=attachments" },
          },
          {
            id: "document:xwiki:Deep1.Deep2",
            text: "Deep Page Leaf",
            children: false,
            data: { type: "document" },
            a_attr: { href: "/xwiki/bin/view/Deep1/Deep2" },
          },
        ]);
        break;

      default:
        res.json([]);
    }
  } else {
    res.json({});
  }
});

app.listen(port, () => {
  console.log(`XWiki mock server listening on http://localhost:${port}`);
});
