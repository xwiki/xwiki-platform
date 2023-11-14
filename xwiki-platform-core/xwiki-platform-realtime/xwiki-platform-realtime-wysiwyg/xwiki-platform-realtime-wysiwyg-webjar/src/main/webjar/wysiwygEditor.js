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
define('xwiki-realtime-wysiwygEditor', [
  'jquery',
  'xwiki-realtime-config',
  'xwiki-l10n!xwiki-realtime-messages',
  'xwiki-realtime-errorBox',
  'xwiki-realtime-toolbar',
  'chainpad-netflux',
  'xwiki-realtime-userData',
  'xwiki-realtime-typingTests',
  'xwiki-realtime-interface',
  'xwiki-realtime-saver',
  'chainpad',
  'xwiki-realtime-crypto',
  'deferred!ckeditor',
  'xwiki-realtime-wysiwygEditor-patches'
], function (
  /* jshint maxparams:false */
  $, realtimeConfig, Messages, ErrorBox, Toolbar, ChainPadNetflux, UserData, TypingTest, Interface, Saver,
  Chainpad, Crypto, ckeditorPromise, Patches
) {
  'use strict';

  const editorId = 'wysiwyg', module = {};

  function waitForEditorInstance(name) {
    name = name || 'content';
    return ckeditorPromise.then(ckeditor => new Promise((resolve, reject) => {
      const editor = ckeditor.instances[name];
      if (editor) {
        if (editor.status === 'ready') {
          resolve(editor);
        } else {
          editor.on('instanceReady', resolve.bind(null, editor));
        }
      } else {
        ckeditor.on('instanceReady', function (event) {
          if (event.editor.name === name) {
            resolve(event.editor);
          }
        });
      }
    }));
  }

  module.main = function(editorConfig, docKeys, useRt) {
    let channel = docKeys[editorId];
    let eventsChannel = docKeys.events;
    let userdataChannel = docKeys.userdata;

    /**
     * Update the channels keys for reconnecting WebSocket.
     */
    function updateKeys() {
      return docKeys._update().then(keys => {
        if (keys[editorId] && keys[editorId] !== channel) {
          channel = keys[editorId];
        }
        if (keys.events && keys.events !== eventsChannel) {
          eventsChannel = keys.events;
        }
        if (keys.userdata && keys.userdata !== userdataChannel) {
          userdataChannel = keys.userdata;
        }
        return keys;
      });
    }

    Interface.realtimeAllowed(useRt);
    let allowRealtimeCheckbox = $();
    // Don't display the checkbox in the following cases:
    // * useRt 0 (instead of true/false) => we can't connect to the websocket service
    // * realtime is disabled and we're not an advanced user
    if (useRt !== 0 && (useRt || editorConfig.isAdvancedUser)) {
      allowRealtimeCheckbox = Interface.createAllowRealtimeCheckbox(Interface.realtimeAllowed());
      allowRealtimeCheckbox.on('change', function() {
        if (allowRealtimeCheckbox.prop('checked')) {
          Interface.realtimeAllowed(true);
          // TODO: Join the RT session without reloading the page?
          window.location.href = editorConfig.rtURL;
        } else {
          editorConfig.displayDisableModal(function(state) {
            if (!state) {
              allowRealtimeCheckbox.prop('checked', true);
            } else {
              Interface.realtimeAllowed(false);
              module.onAbort();
            }
          });
        }
      });
    }

    if (!useRt) {
      // When someone is offline, they may have left their tab open for a long time and the lock may have disappeared.
      // We're refreshing it when the editor is focused so that other users will know that someone is editing the
      // document.
      waitForEditorInstance().then(editor => {
        editor.on('focus', function() {
          XWiki.EditLock = new XWiki.DocumentLock();
          XWiki.EditLock.lock();
        });
      });
    }

    if (!Interface.realtimeAllowed()) {
      console.log('Realtime is disallowed. Quitting');
      return;
    }

    Saver.configure({
      chainpad: ChainPad,
      editorType: editorId,
      editorName: 'WYSIWYG',
      isHTML: true,
      mergeContent: realtimeConfig.enableMerge !== 0
    });
    
    // Fix the magic line issue.
    function fixMagicLine(editor) {
      if (editor.plugins.magicline) {
        const ml = editor.plugins.magicline.backdoor ? editor.plugins.magicline.backdoor.that.line.$ :
          editor._.magiclineBackdoor.that.line.$;
        [ml, ml.parentElement].forEach(function(el) {
          el.setAttribute('class', 'rt-non-realtime');
        });
      }
    }

    // User position indicator style.
    const userIconStyle = [
      '<style>',
      '.rt-user-position {',
        'position : absolute;',
        'width : 15px;',
        'height: 15px;',
        'display: inline-block;',
        'background : #CCCCFF;',
        'border : 1px solid #AAAAAA;',
        'text-align : center;',
        'line-height: 15px;',
        'font-size: 11px;',
        'font-weight: bold;',
        'color: #3333FF;',
        'user-select: none;',
      '}',
      '</style>'
    ].join('\n');

    function whenReady(editor) {
      let initializing = true, editableContent,

      initEditableContent = function() {
        // Disable temporary attachment upload for now.
        if (editor.config['xwiki-upload']) {
          editor.config['xwiki-upload'].isTemporaryAttachmentSupported = false;
        }
        editableContent = editor.editable().$;
        $('head', editableContent.ownerDocument).append(userIconStyle);
        fixMagicLine(editor);
      };

      // Initialize the editable content when the editor is ready.
      initEditableContent();

      let afterRefresh = [];
      editor.on('afterCommandExec', function(event) {
        if (event?.data?.name === 'xwiki-refresh') {
          // Re-initialize the editable content after it is refreshed.
          initEditableContent();
          initializing = false;
          realtimeOptions.onLocal();
          afterRefresh.forEach(item => item());
          afterRefresh = [];
        }
      });

      const setEditable = module.setEditable = function(editable) {
        editableContent.setAttribute('contenteditable', editable);
        $('.buttons [name^="action_save"], .buttons [name^="action_preview"]').prop('disabled', !editable);
      };

      // Don't let the user edit until the real-time framework is ready.
      setEditable(false);

      // List of pretty name of all users (mapped with their server ID).
      let userData = {},
      // List of users still connected to the channel (server IDs).
      userList,
      // The real-time toolbar, showing the list of connected users, the merge message, the spinner and the lag.
      toolbar,
      // The editor wrapper used to update the edited content without losing the caret position.
      patchedEditor = new Patches(editor),

      findMacroComments = function(el) {
        const arr = [];
        for (const node of el.childNodes) {
          if (node.nodeType === 8 && node.data && /startmacro/.test(node.data)) {
            arr.push(node);
          } else {
            arr.push(...findMacroComments(node));
          }
        }
        return arr;
      },

      createSaver = function(info) {
        Saver.lastSaved.mergeMessage = Interface.createMergeMessageElement(
          toolbar.toolbar.find('.rt-toolbar-rightside'));
        Saver.setLastSavedContent(editor._.previousModeData);
        const saverCreateConfig = {
          // Id of the wiki page form.
          formId: window.XWiki.editor === 'wysiwyg' ? 'edit' : 'inline',
          setTextValue: function(newText, toConvert, callback) {
            function andThen(data) {
              patchedEditor.setHTML(data);

              // If available, transform the HTML comments for XWiki macros into macros before saving
              // (<!--startmacro:{...}-->). We can do that by using the "xwiki-refresh" command provided the by
              // CKEditor Integration application.
              if (editor.plugins['xwiki-macro'] && findMacroComments(editableContent).length > 0) {
                initializing = true;
                editor.execCommand('xwiki-refresh');
                afterRefresh.push(callback);
              } else {
                callback();
                realtimeOptions.onLocal();
              }
            }
            if (toConvert) {
              const object = {
                wiki: XWiki.currentWiki,
                space: XWiki.currentSpace,
                page: XWiki.currentPage,
                convert: true,
                text: newText
              };
              $.post(editorConfig.htmlConverterUrl, object).then(andThen).catch(() => {
                const debugLog = {
                  state: editorId + '/convertHTML',
                  postData: object
                };
                module.onAbort(null, 'converthtml', JSON.stringify(debugLog));
              });
            } else {
              andThen(newText);
            }
          },
          getSaveValue: function() {
            return {
              content: editor.getData(),
              RequiresHTMLConversion: 'content',
              'content_syntax': 'xwiki/2.1'
            };
          },
          getTextValue: function() {
            try {
              return editor.getData();
            } catch (e) {
              editor.showNotification(Messages['realtime.editor.getContentFailed'], 'warning');
              return null;
            }
          },
          realtime: info.realtime,
          userList: info.userList,
          userName: editorConfig.userName,
          network: info.network,
          channel: eventsChannel,
          safeCrash: function(reason, debugLog) {
            module.onAbort(null, reason, debugLog);
          }
        };
        Saver.create(saverCreateConfig);
      },

      getXPath = function(element) {
        let xpath = '';
        for ( ; element && element.nodeType == 1; element = element.parentNode ) {
          let id = $(element.parentNode).children(element.tagName).index(element) + 1;
          id = id > 1 ? '[' + id + ']' : '';
          xpath = '/' + element.tagName.toLowerCase() + id + xpath;
        }
        return xpath;
      },

      getPrettyName = function(userName) {
        return userName ? userName.replace(/^.*-([^-]*)%2d\d*$/, function(all, one) { 
          return decodeURIComponent(one);
        }) : userName;
      };

      editor.on('toDataFormat', function(evt) {
        const root = evt.data.dataValue;
        const toRemove = [];
        const toReplaceMacro = [];
        root.forEach( function( node ) {
          if (node.name === "style") {
            window.myNode = node;
            toRemove.push(node);
          }
          if (typeof node.hasClass === "function") {
            if (node.hasClass('rt-non-realtime')) {
              toRemove.push(node);
            } else if (node.hasClass('macro') &&
                node.attributes &&
                node.attributes['data-macro'] &&
                node.parent &&
                node.parent.attributes &&
                node.parent.attributes.contenteditable === "false") {
              toReplaceMacro.push(node);
            }
          }
        }, null, true );
        toRemove.forEach(function (el) {
        if (!el) { return; }
          el.forEach(function (node) {
            node.remove();
          });
        });
        let macroWidget;
        for (const widget in editor.widgets.instances) {
          if (widget.name && widget.name === 'xwiki-macro') {
            macroWidget = widget;
            break;
          }
        }
        if (macroWidget) {
          toReplaceMacro.forEach(function (el) {
            const container = el.parent;
            const newNode = macroWidget.downcast(el);
            const index = container.parent.children.indexOf(container);
            container.parent.children[index] = newNode;
          });
        }
      }, null, null, 12 );

      function changeUserIcons(newdata) {
        if (!realtimeConfig.marginAvatar) {
          return;
        }

        // If no new data (someone has just joined or left the channel), get the latest known values.
        const updatedData = newdata || userData;

        $(editableContent.ownerDocument).find('.rt-user-position').remove();
        const positions = {};
        const avatarWidth = 15, spacing = 3;
        let requiredPadding = 0;
        userList.users.filter(id => updatedData[id]?.['cursor_' + editorId]).forEach(id => {
          const data = updatedData[id];
          const name = getPrettyName(data.name);
          // Set the user position.
          const element = editableContent.ownerDocument.evaluate(data['cursor_' + editorId],
            editableContent.ownerDocument, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
          if (!element) {
            return;
          }
          const pos = $(element).offset();
          if (!positions[pos.top]) {
            positions[pos.top] = [id];
          } else {
            positions[pos.top].push(id);
          }
          const index = positions[pos.top].length - 1;
          const posTop = pos.top + spacing;
          const posLeft = spacing + index * (avatarWidth + spacing);
          requiredPadding = Math.max(requiredPadding, (posLeft + 2 * spacing));
          let $indicator;
          if (data.avatar) {
            $indicator = $('<img alt=""/>').attr('src', data.avatar);
          } else {
            $indicator = $('<div></div>').text(name.substring(0, 1));
          }
          $indicator.addClass('rt-non-realtime rt-user-position').attr({
            id: 'rt-user-' + id,
            title: name,
            contenteditable: 'false'
          }).css({
            'left': posLeft + 'px',
            'top': posTop + 'px'
          });
          $('html', editableContent.ownerDocument).append($indicator);
        });

        $(editableContent).css('padding-left', requiredPadding === 0 ? '' : ((requiredPadding + avatarWidth) + 'px'));
      }

      let isFirstOnReadyCall = true;

      const realtimeOptions = {
        initialState: patchedEditor.getHyperJSON() || '{}',
        websocketURL: editorConfig.WebsocketURL,
        userName: editorConfig.userName,
        channel: channel,
        crypto: Crypto,
        network: editorConfig.network,

        // OT
        //patchTransformer: Chainpad.NaiveJSONTransformer

        onRemote: function(info) {
          if (initializing) {
            return;
          }

          const shjson = info.realtime.getUserDoc();

          // Build a DOM from HJSON, diff, and patch the editor.
          patchedEditor.setHyperJSON(shjson);

          const shjson2 = patchedEditor.getHyperJSON();
          if (shjson2 !== shjson) {
            console.error('shjson2 !== shjson');
            const diff = Chainpad.Diff.diff(shjson, shjson2);
            console.log(shjson, diff);
            module.chainpad.contentUpdate(shjson2);
          } else {
            // Notify the content change.
            editor.fire('change');
          }
        },

        onInit: function(info) {
          userList = info.userList;
          const config = {
            userData,
            onUsernameClick: function(id) {
              const editableContentLocation = editableContent.ownerDocument.defaultView.location;
              const baseHref = editableContentLocation.href.split('#')[0] || '';
              editableContentLocation.href = baseHref + '#rt-user-' + id;
            }
          };
          toolbar = Toolbar.create({
            '$container': $('#cke_1_toolbox'),
            myUserName: info.myID,
            realtime: info.realtime,
            getLag: info.getLag,
            userList: info.userList,
            config
          });
          // When someone leaves, if they used Save&View, it removes the locks from the document. We're going to add it
          // again to be sure new users will see the lock page and be able to join.
          let oldUsers = JSON.parse(JSON.stringify(userList.users || []));
          userList.change.push(function() {
            if (userList.length) {
              // If someone has left, try to get the lock.
              if (oldUsers.some(user => userList.users.indexOf(user) === -1)) {
                XWiki.EditLock = new XWiki.DocumentLock();
                XWiki.EditLock.lock();
              }
              oldUsers = JSON.parse(JSON.stringify(userList.users || []));
            }
          });
        },

        onReady: function(info) {
          if (!initializing) {
            return;
          }

          $.extend(module, {
            chainpad: info.realtime,
            leaveChannel: info.leave,
            realtimeOptions
          });
          const shjson = module.chainpad.getUserDoc();

          if (isFirstOnReadyCall) {
            isFirstOnReadyCall = false;
            // Update the user list to link the wiki name to the user id.
            const userdataConfig = {
              myId: info.myId,
              userName: editorConfig.userName,
              userAvatar: editorConfig.userAvatarURL,
              onChange: userList.onChange,
              crypto: Crypto,
              editor: editorId,
              getCursor: function() {
                const selection = editor.getSelection();
                if (!selection) {
                  return '';
                }
                const ranges = selection.getRanges();
                if (!ranges?.[0]?.startContainer?.$) {
                  return '';
                }
                let node = ranges[0].startContainer.$;
                node = (node.nodeName === '#text') ? node.parentNode : node;
                const xpath = getXPath(node);
                return xpath;
              }
            };
            if (!realtimeConfig.marginAvatar) {
              delete userdataConfig.getCursor;
            }

            userData = UserData.start(info.network, userdataChannel, userdataConfig);
            userList.change.push(changeUserIcons);
          }

          patchedEditor.setHyperJSON(shjson);

          console.log('Unlocking editor');
          initializing = false;
          setEditable(true);
          module.chainpad.start();

          realtimeOptions.onLocal();
          createSaver(info);
        },

        onAbort: function(info, reason, debug) {
          console.log("Aborting the session!");
          const msg = reason || 'disconnected';
          module.chainpad.abort();
          try {
            // Don't break if the channel doesn't exist anymore.
            module.leaveChannel();
          } catch (e) {}
          module.aborted = true;
          editorConfig.abort();
          Saver.stop();
          toolbar.failed();
          toolbar.toolbar.remove();
          if (typeof userData.leave === 'function') {
            userData.leave();
          }
          changeUserIcons({});
          if (allowRealtimeCheckbox.prop('checked') && !module.aborted) {
            ErrorBox.show(msg, debug);
          }
        },

        onConnectionChange: function(info) {
          if (module.aborted) {
            return;
          }
          console.log('Connection status: ' + info.state);
          toolbar.failed();
          if (info.state) {
            initializing = true;
            toolbar.reconnecting(info.myId);
          } else {
            module.chainpad.abort();
            setEditable(false);
          }
        },

        beforeReconnecting: function(callback) {
          const oldChannel = channel;
          updateKeys().then(() => {
            if (channel !== oldChannel) {
              editorConfig.onKeysChanged();
              setEditable(false);
              allowRealtimeCheckbox.prop('checked', false);
              module.onAbort();
            } else {
              callback(channel, patchedEditor.getHyperJSON());
            }
          });
        },

        // This function resets the realtime fields after coming back from source mode.
        onLocalFromSource: function() {
          // Re-initialize the editable content when coming back from source mode because the WYSIWYG area is recreated.
          initEditableContent();
          this.onLocal();
        },

        onLocal: function() {
          if (initializing) {
            return;
          }
          // Stringify the JSON and send it into ChainPad.
          const shjson = patchedEditor.getHyperJSON();
          module.chainpad.contentUpdate(shjson);

          if (module.chainpad.getUserDoc() !== shjson) {
            console.error("realtime.getUserDoc() !== shjson");
          }
        }
      };

      module.onAbort = realtimeOptions.onAbort;

      module.realtimeInput = ChainPadNetflux.start(realtimeOptions);

      editor.on('change', function() {
        Saver.destroyDialog();
        if (!initializing) {
          Saver.setLocalEditFlag(true);
        }
        realtimeOptions.onLocal();
      });

      // Export the typing tests to the window.
      // call like `test = easyTest()`
      // terminate the test like `test.cancel()`
      window.easyTest = function () {
        let container, offset;
        const range = editor.getSelection()?.getRanges()?.[0];
        if (range) {
          container = range.startContainer.$;
          offset = range.startOffset;
        }
        const test = TypingTest.testInput(editableContent, container, offset, realtimeOptions.onLocal);
        realtimeOptions.onLocal();
        return test;
      };

      return editor;
    }

    return waitForEditorInstance().then(whenReady);
  };

  return module;
});
