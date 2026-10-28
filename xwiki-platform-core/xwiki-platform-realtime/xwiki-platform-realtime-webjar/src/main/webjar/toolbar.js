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
/**
 * Loads the realtime toolbar, which is implemented as an ES module, along with everything it needs from the edit page
 * (translations, configuration, etc.). The module value is a promise of the toolbar class, see the next module.
 */
define('xwiki-realtime-toolbar-promise', [
  'es-module!@xwiki/platform-collaboration-toolbar',
  'xwiki-realtime-document'
], function(toolbarModule, xwikiDocument) {
  'use strict';

  async function createToolbarClass({Toolbar, createXWikiToolbarEnvironment, findXWikiEditForm,
      loadXWikiToolbarTranslations}) {
    const [environment, i18n] = await Promise.all([createXWikiToolbarEnvironment(), loadXWikiToolbarTranslations()]);
    return class extends Toolbar {
      /**
       * @param {Object} session the realtime session actions triggered by the toolbar:
       *   {@code save(continueEditing)}, {@code leave()} (optional) and {@code selectUser(userId)} (optional)
       */
      constructor(session) {
        // The ChainPad realtime editors edit the current document, from the edit form of the current page.
        super({form: findXWikiEditForm(), document: xwikiDocument, session, environment, i18n});
      }
    };
  }

  // RequireJS doesn't accept async functions as module factories, but the module value can be a promise.
  return createToolbarClass(toolbarModule);
});

/**
 * The realtime toolbar class, fully loaded so that realtime toolbars can be created synchronously.
 */
define('xwiki-realtime-toolbar', ['await!xwiki-realtime-toolbar-promise'], function(Toolbar) {
  'use strict';
  return Toolbar;
});
