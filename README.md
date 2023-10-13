# Cristal

Cristal is a new project to build a new modular wiki UI using modern web technologies, which will support multiple
backends (including of course [XWiki](https://www.xwiki.org/) to store Wiki data.

* Project Lead: [Manuel Leduc](https://www.xwiki.org/xwiki/bin/view/XWiki/mleduc)
* Documentation & Downloads: N/A <!-- [Documentation & Download](https://extensions.xwiki.org/xwiki/bin/view/Extension/
  (extension
  name)))-->
* [Issue Tracker](https://jira.xwiki.org/projects/CRISTAL/summary)
* Communication: [Forum](https://forum.xwiki.org/c/cristal/18)<!--, [Chat](https://dev.xwiki.
  org/xwiki/bin/view/Community/Chat)-->
* [Development Practices](https://dev.xwiki.org)
* Minimal XWiki version supported: XWiki (minimal xwiki version)
* License: LGPL 2.1
* Translations: N/A
<!--* Sonar
  Dashboard: [![Status](https://sonarcloud.io/api/project_badges/measure?project=(group id):(artifact id)&metric=alert_status)](https://sonarcloud.io/dashboard?id=(group
  id):(artifact id))-->
<!--* Continuous Integration
  Status: [![Build Status](https://ci.xwiki.org/job/XWiki%20Contrib/job/(project id on ci)/job/master/badge/icon)](https://ci.xwiki.org/job/XWiki%20Contrib/job/(projct
  id on ci)/job/master/)-->


## Recommended IDE Setup

[VSCode](https://code.visualstudio.com/) + [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar) (and disable Vetur) + [TypeScript Vue Plugin (Volar)](https://marketplace.visualstudio.com/items?itemName=Vue.vscode-typescript-vue-plugin).

## Type Support for `.vue` Imports in TS

TypeScript cannot handle type information for `.vue` imports by default, so we replace the `tsc` CLI with `vue-tsc` for type checking. In editors, we need [TypeScript Vue Plugin (Volar)](https://marketplace.visualstudio.com/items?itemName=Vue.vscode-typescript-vue-plugin) to make the TypeScript language service aware of `.vue` types.

If the standalone TypeScript plugin doesn't feel fast enough to you, Volar has also implemented a [Take Over Mode](https://github.com/johnsoncodehk/volar/discussions/471#discussioncomment-1361669) that is more performant. You can enable it by the following steps:

1. Disable the built-in TypeScript Extension
    1) Run `Extensions: Show Built-in Extensions` from VSCode's command palette
    2) Find `TypeScript and JavaScript Language Features`, right click and select `Disable (Workspace)`
2. Reload the VSCode window by running `Developer: Reload Window` from the command palette.

## Customize configuration

See [Vite Configuration Reference](https://vitejs.dev/config/).

## Project Setup

```sh
npm install
```

### Compile and Hot-Reload for Development

```sh
npm run dev
```

### Type-Check, Compile and Minify for Production

```sh
npm run build
```

### Run Unit Tests with [Vitest](https://vitest.dev/)

```sh
npm run test:unit
```

### Run End-to-End Tests with [Cypress](https://www.cypress.io/)

```sh
npm run test:e2e:dev
```

This runs the end-to-end tests against the Vite development server.
It is much faster than the production build.

But it's still recommended to test the production build with `test:e2e` before deploying (e.g. in CI environments):

```sh
npm run build
npm run test:e2e
```

### Lint with [ESLint](https://eslint.org/)

```sh
npm run lint
```
