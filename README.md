# Cristal

<span>
<img src="https://cristal.xwiki.org/xwiki/bin/download/Main/WebHome/main.png" alt="Wireframe of a Cristal 
Page" style="box-shadow: 0 4px 8px 0 rgba(0,0,0,0.2)">
</span>

## What's Cristal

* A new [xwiki.org](xwiki.org) project to provide a modern knowledge base front-end
* A wiki UI to rule them all: Provide a UI that can be plugged onto various content backends (XWiki, local file system,
  GitHub, etc.)
* Usable in various forms:
  * From a URL in your browser
  * Executable locally on your computer (Electron application).
  * Embeddable in backends. For example, the intent on the long run is to have Cristal be used by [XWiki](xwiki.org) as
    its native UI
* Ability to work offline, reconnect, and sync content.

<div style="text-align: center">
<a href="https://ci.xwiki.org/job/XWiki%20Contrib/job/cristal/job/main/" rel="nofollow"><img src="https://camo.githubusercontent.com/789e283b375b4b15c2a538db753691975072aef8805d0be1b9228cbbda1384ac/68747470733a2f2f63692e7877696b692e6f72672f6a6f622f5857696b69253230436f6e747269622f6a6f622f6372697374616c2f6a6f622f6d61696e2f62616467652f69636f6e" alt="Build Status" data-canonical-src="https://ci.xwiki.org/job/XWiki%20Contrib/job/cristal/job/main/badge/icon" style="max-width: 100%;"></a>
<a href="http://www.typescriptlang.org" rel="nofollow"><img src="https://img.shields.io/badge/%3C%2F%3E-TypeScript-%230074c1.svg" alt="TypeScript"></a>
<a href="https://github.com/prettier/prettier"><img src="https://img.shields.io/badge/code_style-prettier-ff69b4.svg?style=flat" alt="Prettier"></a>
</div>

## Useful links

* [Roadmap](https://cristal.xwiki.org/xwiki/bin/view/Roadmaps/)
* [Architecture](https://cristal.xwiki.org/xwiki/bin/view/Architecture/)
* [Technological Choices](https://cristal.xwiki.org/xwiki/bin/view/TechnologicalChoices/)
* [Wireframes](https://cristal.xwiki.org/xwiki/bin/view/Wireframes/)
* [UI Components](https://cristal.xwiki.org/xwiki/bin/view/UIComponents/)
* [Backends](https://cristal.xwiki.org/xwiki/bin/view/Backends/)
* [GitHub repository](https://github.com/xwiki-contrib/cristal/)
* [Forum](https://forum.xwiki.org/c/cristal/)

## Project details

* Project Lead: [Manuel Leduc](https://www.xwiki.org/xwiki/bin/view/XWiki/mleduc)
* Documentation: [Documentation](https://cristal.xwiki.org/)
* Downloads:
  * `@xwiki/cristal-*` on https://www.npmjs.com/org/xwiki
  * binary artifacts on https://github.com/xwiki-contrib/cristal/releases
* [Issue Tracker](https://jira.xwiki.org/projects/CRISTAL/summary)
* Communication: [Forum](https://forum.xwiki.org/c/cristal/18)<!--, [Chat](https://dev.xwiki.
  org/xwiki/bin/view/Community/Chat)-->
* [Development Practices](https://dev.xwiki.org)
* [Supported backends](https://cristal.xwiki.org/xwiki/bin/view/Backends/)
* License: LGPL 2.1
* Translations: all components of https://l10n.xwiki.org/projects/xwiki-contrib/cristal/
* Sonar Dashboard: N/A <!--[![Status](https://sonarcloud.io/api/project_badges/measure?project=(group id):(artifact id)&metric=alert_status)](https://sonarcloud.io/dashboard?id=(group
  id):(artifact id))-->

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

### Starting an electron instance

```shell
pnpm run start:electron
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
## Make sure to have the system dependencies and browsers up to date. By filtering on the web module we make sure the
## version of playwright that gets installed is the one specified in package.json
pnpm --filter ./web exec playwright install --with-deps

## Run the tests from the web module.
pnpm run --filter ./web test:e2e

## Or, if port 9000 is already used
HTTP_PORT=9001 pnpm run --filter ./web test:e2e
```

### Lint

```sh
pnpm lint
```
