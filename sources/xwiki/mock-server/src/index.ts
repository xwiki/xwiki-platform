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

import express, { Express, Request, Response } from "express";
import dotenv from "dotenv";

dotenv.config();

const app: Express = express();
const port = process.env.PORT;

// Provide a root path with some content for playwright to be able to detect when the server is up and running.
app.get("/", (_req, res) => {
  res.send("READY");
});

let offlineCount = 0;

function getHtml(page: string, options: { offline: boolean }) {
  let html = `<h1>Welcome to ${page}!</h1>

XWiki is the best tool to organize your knowledge.

<a href="Page2.WebHome">XWiki Syntax</a>
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
  const page: string = (req.query.page as string) || "Main.WebHome";
  const offline = page === "Main.Offline";
  res.appendHeader("Access-Control-Allow-Origin", "*");

  const text = `= Welcome to ${page} =

XWiki is the best tool to organize your knowledge.

[[XWiki Syntax>>Page2.WebHome]]
`;
  const html = getHtml(page, {
    offline,
  });
  res.json({
    "@context": "https://schema.org",
    "@type": "CreativeWork",
    identifier: page,
    name: "WebHome",
    headline: page,
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

app.listen(port, () => {
  console.log(`XWiki mock server listening on http://localhost:${port}`);
});
