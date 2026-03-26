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
define('xwiki-realtime-wikiEditor-loader', [
  'jquery',
  'xwiki-realtime-loader',
  'xwiki-l10n!xwiki-realtime-messages'
], function($, Loader, Messages) {
  'use strict';

  const editorId = 'wiki', info = {
    type: editorId,
    // FIXME: Don't hard-code the field name. We should be able to edit any document field (e.g. TextArea xobject
    // properties), not just the document content.
    field: 'content',
    href: '&editor=wiki&force=1',
    name: 'Wiki',
    compatible: ['wiki', 'wysiwyg']
  };

  Loader.bootstrap(info).then(realtimeContext => {
    // "Fail" silently if realtime collaboration is not supported in this context.
    if (realtimeContext) {
      return new Promise((resolve, reject) => {
        require(['xwiki-realtime-wikiEditor'], function (RealtimeWikiEditor) {
          try {
            realtimeContext.rtURL = Loader.getEditorURL(globalThis.location.href, info);
            // TODO: The editor initialization is asynchronous so the resolved value should be a Promise in order to
            // notify the user in case of errors.
            resolve(new RealtimeWikiEditor(realtimeContext));
          } catch (error) {
            reject(error);
          }
        }, reject);
      });
    }
  }).catch(error => {
    new XWiki.widgets.Notification(Messages['join.error'], 'error');
    // Provide more details in the console for debugging.
    console.error(Messages['join.error'], error);
  });
});
