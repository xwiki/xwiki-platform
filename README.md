# Cristal

Cristal is a new project to build a new modular wiki UI using modern web technologies, which will support multiple
backends (including of course [XWiki](https://www.xwiki.org/) to store wiki data, and provide back-end services).

* Project Lead: [Manuel Leduc](https://www.xwiki.org/xwiki/bin/view/XWiki/mleduc)
* Documentation & Downloads: N/A <!-- [Documentation & Download](https://extensions.xwiki.org/xwiki/bin/view/Extension/
  (extension
  name)))-->
* [Issue Tracker](https://jira.xwiki.org/projects/CRISTAL/summary)
* Communication: [Forum](https://forum.xwiki.org/c/cristal/18)<!--, [Chat](https://dev.xwiki.
  org/xwiki/bin/view/Community/Chat)-->
* [Development Practices](https://dev.xwiki.org)
* Minimal XWiki version supported: N/A <!-- XWiki (minimal xwiki version)-->
* License: LGPL 2.1
* Translations: N/A
* Sonar Dashboard: N/A <!--[![Status](https://sonarcloud.io/api/project_badges/measure?project=(group id):(artifact id)&metric=alert_status)](https://sonarcloud.io/dashboard?id=(group
  id):(artifact id))-->
* Continuous Integration Status: [![Build Status](https://ci.xwiki.org/job/XWiki%20Contrib/job/cristal/job/main/badge/icon)](https://ci.xwiki.org/job/XWiki%20Contrib/job/cristal/job/main/)

## Project Setup

```sh
pnpm install
```

### Starting the project

Starting on default port 9000.

```sh
pnpm run start
```

Starting on an arbitrary port (e.g., 9001)

```shell
HTTP_PORT=9001 pnpm run start
```

### Compile and Minify for Production

```sh
pnpm run build
```

### Run Unit Tests with [Vitest](https://vitest.dev/)

```sh
pnpm run test
```

## Run Functional Tests with [Playwright](https://playwright.dev/)

```sh
pnpm run --filter ./web test:e2e
## Or, if port 9000 is already used
HTTP_PORT=9001 pnpm run --filter ./web test:e2e
```

After a playwright upgrade, the following commands need to be executed to make sure to have the system dependencies
and browsers up to date.

```sh
pnpx playwright install
pnpx playwright install-deps
```

### Lint

```sh
pnpm lint
```
