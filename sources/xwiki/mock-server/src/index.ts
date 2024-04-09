import express, { Express, Request, Response } from "express";
import dotenv from "dotenv";

dotenv.config();

const app: Express = express();
const port = process.env.PORT;

// Provide a root path with some content for playwright to be able to detect when the server is up and running.
app.get("/", (_req, res) => {
  res.send("READY");
});

app.get("/xwiki/rest/cristal/page", (req: Request, res: Response) => {
  const page = req.query.page || "Main.WebHome";

  res.appendHeader("Access-Control-Allow-Origin", "*");

  res.json({
    "@context": "https://schema.org",
    "@type": "CreativeWork",
    identifier: page,
    name: "WebHome",
    headline: page,
    creator: page,
    encodingFormat: "xwiki/2.1",
    text: `= Welcome to ${page} =

XWiki is the best tool to organize your knowledge.

[[XWiki Syntax>>Page2.WebHome]]
`,
    html: `<h1>Welcome to ${page}!</h1>

XWiki is the best tool to organize your knowledge.

<a href="Page2.WebHome">XWiki Syntax</a>
`,
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

app.listen(port, () => {
  console.log(`XWiki mock server listening on http://localhost:${port}`);
});
