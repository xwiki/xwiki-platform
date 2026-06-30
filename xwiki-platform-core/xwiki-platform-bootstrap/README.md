This directory material is a fork of twbs/bootstrap 3.4.x branch with some specific fixes for XWiki.

## Build

This module is built with pnpm + Vite through the `webjar-node` Maven packaging.
The LESS sources and the glyphicon fonts are packaged  as is.
The Bootstrap QUnit suite under `src/main/node/tests` is run in a headless browser with Playwright (`pnpm run test:unit`).

## Copyright and license

Code and documentation copyright 2011-2019 Twitter, Inc. Code released under [the MIT license](https://github.com/twbs/bootstrap/blob/master/LICENSE). Docs released under [Creative Commons](https://github.com/twbs/bootstrap/blob/master/docs/LICENSE).
