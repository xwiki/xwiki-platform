/*
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
/*
 * Bridge between the page test framework (JVM side) and axe-core (Node.js side).
 *
 * The bridge is started once per JVM and kept alive: it reads one JSON request per line
 * on stdin and answers with one JSON response per line on stdout, which keeps the Node.js
 * startup cost out of the per-page validation time.
 *
 * The JVM side passes this whole ES module to `node --input-type=module -e <script> <npmRoot>`,
 * where npmRoot is the directory holding the package.json and node_modules of
 * "@xwiki/platform-test-page-accessibility" (axe-core, jsdom). The dependencies are resolved from
 * that directory rather than from the location of this script, so the script keeps working when
 * loaded from a jar on the classpath.
 *
 * Request: {"id": <number>, "html": <string>, "tags": [<string>, ...],
 *           "disabledRules": [<string>, ...]}
 * Response: {"id": <number>, "ok": true, "violations": [...], "incomplete": [...]}
 * or:       {"id": <number>, "ok": false, "error": <string>}
 */
import { readFileSync } from "node:fs";
import { join } from "node:path";
import { createRequire } from "node:module";
import { createInterface } from "node:readline";

const npmRoot = process.argv[1];

// jsdom and axe-core are CommonJS packages of the npm root: load them from there instead of
// from the location of this script.
const requireFromNpmRoot = createRequire(join(npmRoot, "package.json"));
const { JSDOM, VirtualConsole } = requireFromNpmRoot("jsdom");
const axeSource = readFileSync(requireFromNpmRoot.resolve("axe-core/axe.min.js"), "utf-8");

// Copy only the fields the JVM side reports on, to keep the pipes small.
function compactRules(rules)
{
    return rules.map((rule) => ({
        id: rule.id,
        impact: rule.impact,
        help: rule.help,
        description: rule.description,
        helpUrl: rule.helpUrl,
        tags: rule.tags,
        nodes: rule.nodes.map((node) => ({
            html: node.html,
            target: node.target,
            failureSummary: node.failureSummary
        }))
    }));
}

async function analyze(request)
{
    const isolatedConsole = new VirtualConsole();
    // The rendered HTML usually comes as a content fragment. jsdom provides neither the page context
    // (title, language, landmarks) nor a style/layout engine around it: full-page and CSS-dependent
    // rules never produce meaningful results there and are expected to be disabled through the
    // default disabled rules of the java side (org.xwiki.test.page.accessibility.WCAGCustomizer).
    const dom = new JSDOM(request.html, { runScripts: "outside-only", virtualConsole: isolatedConsole });
    try {
        dom.window.eval(axeSource);
        const options = {
            runOnly: { type: "tags", values: request.tags },
            rules: {},
            resultTypes: ["violations", "incomplete"]
        };
        for (const rule of request.disabledRules || []) {
            options.rules[rule] = { enabled: false };
        }
        const results = await dom.window.axe.run(dom.window.document, options);
        return { violations: compactRules(results.violations), incomplete: compactRules(results.incomplete) };
    } finally {
        dom.window.close();
    }
}

function respond(request, result)
{
    const response = { id: request ? request.id : null };
    if (result instanceof Error) {
        response.ok = false;
        response.error = result.message;
    } else {
        response.ok = true;
        Object.assign(response, result);
    }
    process.stdout.write(JSON.stringify(response) + "\n");
}

const input = createInterface({ input: process.stdin, terminal: false });
input.on("line", async (line) => {
    if (!line.trim()) {
        return;
    }
    let request;
    try {
        request = JSON.parse(line);
        respond(request, await analyze(request));
    } catch (error) {
        respond(request, error);
    }
});
input.on("close", () => process.exit(0));
