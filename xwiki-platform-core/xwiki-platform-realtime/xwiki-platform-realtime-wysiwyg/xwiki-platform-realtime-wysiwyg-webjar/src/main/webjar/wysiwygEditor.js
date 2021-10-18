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
  'xwiki-realtime-errorBox',
  'xwiki-realtime-toolbar',
  'chainpad-netflux',
  'hyper-json',
  'xwiki-realtime-cursor',
  'xwiki-realtime-userData',
  'xwiki-realtime-typingTests',
  'json.sortify',
  'xwiki-realtime-interface',
  'xwiki-realtime-saver',
  'chainpad',
  'xwiki-realtime-crypto',
  'diff-dom',
  'deferred!ckeditor'
], function (
  /* jshint maxparams:false */
  $, realtimeConfig, ErrorBox, Toolbar, ChainPadNetflux, Hyperjson, Cursor, UserData, TypingTest, JSONSortify,
  Interface, Saver, Chainpad, Crypto, DiffDom, ckeditorPromise
) {
  'use strict';

  var editorId = 'wysiwyg', module = {
    Hyperjson
  };

  var hasClass = function(actual, expectedList, some) {
    var actualList = (actual || '').split(/\s+/);
    if (typeof expectedList === 'string') {
      expectedList = [expectedList];
    }
    return expectedList[some ? 'some' : 'every'](expectedClass => actualList.indexOf(expectedClass) >= 0);
  };

  // Filter elements to serialize
  var isMacroStuff = function(element) {
    var isMac = typeof element.getAttribute === 'function' &&
      (element.getAttribute('data-cke-hidden-sel') || /cke_widget_drag/.test(element.getAttribute('class')) ||
        hasClass(element.getAttribute('class'), 'cke_image_resizer'));
    return isMac;
  };

  var isNonRealtime = function(element) {
    return hasClass(element?.getAttribute?.('class'), 'rt-non-realtime');
  };

  var shouldSerialize = function(el) {
    return !isNonRealtime(el) && !isMacroStuff(el);
  };

  // Filter attributes in the serialized elements.
  var macroFilter = function(hj) {
    // Send a widget ID == 0 to avoid a fight between broswers about it and prevent the container from having the
    // "selected" class (blue border).
    if (hasClass(hj[1].class, ['cke_widget_wrapper', 'cke_widget_block'])) {
      hj[1].class = 'cke_widget_wrapper cke_widget_block';
      hj[1]['data-cke-widget-id'] = '0';
    } else if (hasClass(hj[1].class, ['cke_widget_wrapper', 'cke_widget_inline'])) {
      hj[1].class = 'cke_widget_wrapper cke_widget_inline';
      hj[1]['data-cke-widget-id'] = '0';
    } else if (hj[1]['data-macro'] && hasClass(hj[1].class, 'macro')) {
      // Don't send the "upcasted" attribute which can be removed, generating a shjson != shjson2 error.
      delete hj[1]['data-cke-widget-upcasted'];
    } else if (hasClass(hj[1].class, ['cke_widget_drag_handler', 'cke_image_resizer'], /* some */ true)) {
      // Remove the title attribute of the drag&drop icons since they are localized and create fights over the language
      // to use.
      delete hj[1].title;
    }
    delete hj[1]['aria-label'];
    return hj;
  };
  var bodyFilter = function (hj) {
    if (hj[0] === "BODY") {
      // The "style" contains the padding created for the user position indicators.
      // We don't want to share that value since it is related to the new userdata channel and not the content channel.
      hj[1].style = undefined;
      // "contenteditable" in the body is changed during initialization, we should not get the new value from the wire.
      if (hj[1].contenteditable) { hj[1].contenteditable = "false"; }
    }
    return hj;
  };
  /* catch `type="_moz"` and body's inline style before it goes over the wire */
  var brFilter = function (hj) {
    if (hj[1].type === '_moz') { hj[1].type = undefined; }
    return hj;
  };
  var hjFilter = function (hj) {
    hj = brFilter(hj);
    hj = bodyFilter(hj);
    hj = macroFilter(hj);
    return hj;
  };

  var stringifyDOM = window.stringifyDOM = function(dom) {
    return JSONSortify(Hyperjson.fromDOM(dom, shouldSerialize, hjFilter));
  };

  var waitForEditorInstance = function(name, ckeditor) {
    var deferred = $.Deferred();
    var editor = ckeditor.instances[name];
    if (editor) {
      if (editor.status === 'ready') {
        deferred.resolve(editor);
      } else {
        editor.on('instanceReady', $.proxy(deferred, 'resolve', editor));
      }
    } else {
      ckeditor.on('instanceReady', function(event) {
        if (event.editor.name === name) {
          deferred.resolve(editor);
        }
      });
    }
    return deferred.promise();
  };

  module.main = function(editorConfig, docKeys, useRt) {
    var WebsocketURL = editorConfig.WebsocketURL;
    var htmlConverterUrl = editorConfig.htmlConverterUrl;
    var userName = editorConfig.userName;
    var language = editorConfig.language;
    var userAvatar = editorConfig.userAvatarURL;
    var network = editorConfig.network;
    var saverConfig = editorConfig.saverConfig || {};
    saverConfig.chainpad = Chainpad;
    saverConfig.editorType = editorId;
    saverConfig.editorName = 'WYSIWYG';
    saverConfig.isHTML = true;
    saverConfig.mergeContent = true;
    var Messages = saverConfig.messages || {};

    saverConfig.mergeContent = realtimeConfig.enableMerge !== 0;

    var channel = docKeys[editorId];
    var eventsChannel = docKeys.events;
    var userdataChannel = docKeys.userdata;

    /**
     * Update the channels keys for reconnecting WebSocket.
     */
    var updateKeys = function() {
      return docKeys._update().done(function(keys) {
        if (keys[editorId] && keys[editorId] !== channel) {
          channel = keys[editorId];
        }
        if (keys.events && keys.events !== eventsChannel) {
          eventsChannel = keys.events;
        }
        if (keys.userdata && keys.userdata !== userdataChannel) {
          userdataChannel = keys.userdata;
        }
      });
    };

    // DISALLOW REALTIME
    var allowRealtimeCbId = Interface.uid();
    Interface.realtimeAllowed(useRt);

    // Don't display the checkbox in the following cases:
    // * useRt 0 (instead of true/false) => we can't connect to the websocket service
    // * realtime is disabled and we're not an advanced user
    if (useRt !== 0 && (useRt || editorConfig.isAdvancedUser)) {
      Interface.createAllowRealtimeCheckbox(allowRealtimeCbId, Interface.realtimeAllowed(), Messages.allowRealtime);

      var $disallowButton = $('#' + allowRealtimeCbId);
      $disallowButton.change(function() {
        if ($disallowButton.prop('checked')) {
          Interface.realtimeAllowed(true);
          // TODO: Join the RT session without reloading the page?
          window.location.href = editorConfig.rtURL;
        } else {
          editorConfig.displayDisableModal(function(state) {
            if (!state) {
              $disallowButton.prop('checked', true);
              return;
            }
            Interface.realtimeAllowed(false);
            module.onAbort();
          });
        }
      });
    }

    if (!useRt) {
      try {
        // When someone is offline, they may have left their tab open for a long time and
        // the lock may have disappeared. We're refreshing it when the editor is focused
        // so that other users will now that someone is editing the document.
        var whenReady2 = function (editor) {
          editor.on('focus', function(e) {
            XWiki.EditLock = new XWiki.DocumentLock();
            XWiki.EditLock.lock();
          });
        };
        var untilThen2 = function () {
          if (window.CKEDITOR &&
            window.CKEDITOR.instances &&
            window.CKEDITOR.instances.content) {
            var editor = window.CKEDITOR.instances.content;
            if (editor.status === "ready") {
              whenReady2(editor, $iframe[0]);
            } else {
              editor.on('instanceReady', function() { whenReady2(editor); });
            }
            return;
          }
          setTimeout(untilThen2, 100);
        };
        untilThen2();
      } catch (e) {}
    }


    if (!Interface.realtimeAllowed()) {
      console.log("Realtime is disallowed. Quitting");
      return;
    }
    // END DISALLOW REALTIME

    // configure Saver with the merge URL and language settings
    Saver.configure(saverConfig);

    var whenReady = function(editor, iframe) {

      var inner = window.inner = iframe.contentWindow.body;
      var innerDoc = window.innerDoc = iframe.contentWindow.document;
      var cursor = window.cursor = Cursor(inner);
      var initializing = true;

      
      // Fix the magic line issue.
      var fixMagicLine = function() {
        if (editor.plugins.magicline) {
          var ml = editor.plugins.magicline.backdoor ? editor.plugins.magicline.backdoor.that.line.$ :
            editor._.magiclineBackdoor.that.line.$;
          [ml, ml.parentElement].forEach(function(el) {
            el.setAttribute('class', 'rt-non-realtime');
          });
        } else {
          setTimeout(fixMagicLine, 100);
        }
      };
      fixMagicLine();

      // User position indicator style.
      var userIconStyle = [
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
      var addStyle = function() {
        var iframe = jQuery('iframe')[0];
        inner = iframe.contentWindow.body;
        innerDoc = iframe.contentWindow.document;
        $('head', innerDoc).append(userIconStyle);
        fixMagicLine();
      };
      addStyle();

      var afterRefresh = [];
      editor.on('afterCommandExec', function(event) {
        if (event?.data?.name === 'xwiki-refresh') {
          initializing = false;
          realtimeOptions.onLocal();
          afterRefresh.forEach(item => item());
          afterRefresh = [];
          fixMagicLine();
        }
      });

      // Add the style again when modifying a macro (which reloads the iframe).
      iframe.onload = addStyle;

      var setEditable = module.setEditable = function(bool) {
        window.inner.setAttribute('contenteditable', bool);
        $('.buttons [name^="action_save"], .buttons [name^="action_preview"]').prop('disabled', bool);
      };

      // Don't let the user edit until the real-time framework is ready.
      setEditable(false);

      var forbiddenTags = [
        'SCRIPT',
        'IFRAME',
        'OBJECT',
        'APPLET',
        'VIDEO',
        'AUDIO'
      ];

      var preDiffFilters = [
        // Don't accept attributes that begin with 'on' these are probably listeners, and we don't want to send scripts
        // over the wire.
        info => {
          if (['addAttribute', 'modifyAttribute'].indexOf(info.diff.action) !== -1 && /^on/.test(info.diff.name)) {
            return `Rejecting forbidden element attribute with name (${info.diff.name})`;
          }
        },

        // Reject any elements which would insert any one of our forbidden tag types: script, iframe, object, applet,
        // video or audio.
        info => {
          if (['addElement', 'replaceElement'].indexOf(info.diff.action) !== -1) {
            if (info.diff.element && forbiddenTags.indexOf(info.diff.element.nodeName) !== -1) {
              return `Rejecting forbidden tag of type (${info.diff.element.nodeName})`;
            } else if (info.diff.newValue && forbiddenTags.indexOf(info.diff.newValue.nodeType) !== -1) {
              return `Rejecting forbidden tag of type (${info.diff.newValue.nodeName})`;
            }
          }
        },

        // Reject the rt-non-realtime class (magic line).
        info => 'removeElement' === info.diff.action && isNonRealtime(info.node),

        // Reject the CKEditor drag and resize handlers.
        info => hasClass(info.node?.getAttribute?.('class'),
          ['cke_widget_drag_handler_container', 'cke_widget_drag_handler', 'cke_image_resizer'], true),

        // Don't change the aria-label properties because they depend on the browser locale and they can create fights.
        info => info.diff.name === "aria-label" &&
          ['modifyAttribute', 'removeAttribute', 'addAttribute'].indexOf(info.diff.action) !== -1,

        // The "style" attribute in the "body" contains the padding used to display the user position indicators. It's
        // not related to the content channel, but to the userdata channel.
        info => info.node?.tagName === 'BODY' && (info.diff.action === 'modifyAttribute' ||
          (info.diff.action === 'removeAttribute' && info.diff.name === 'style'))
      ];

      var DD = new DiffDom({
        preDiffApply: function(info) {
          // Apply our filters.
          if (preDiffFilters.some(filter => {
            var result = filter(info);
            if (typeof result === 'string') {
              console.log(result);
            }
            return result;
          })) {
            // Reject the change.
            return true;
          }

          //
          // Cursor indicators
          //

          var cursor = window.cursor;
          // No use trying to recover the cursor if it doesn't exist.
          if (!cursor.exists()) {
            return;
          }

          // Frame is either 0, 1, 2, or 3, depending on which cursor frames were affected: none, first, last, or both.
          var frame = info.frame = cursor.inNode(info.node);

          if (!frame) {
            return;
          }

          if (typeof info.diff.oldValue === 'string' && typeof info.diff.newValue === 'string') {
            var pushes = cursor.pushDelta(info.diff.oldValue, info.diff.newValue);
            if (frame & 1) {
              // Push cursor start if necessary.
              if (pushes.commonStart < cursor.Range.start.offset) {
                cursor.Range.start.offset += pushes.delta;
              }
            }
            if (frame & 2) {
              // Push cursor end if necessary.
              if (pushes.commonStart < cursor.Range.end.offset) {
                cursor.Range.end.offset += pushes.delta;
              }
            }
          }
        },

        postDiffApply: function(info) {
          var cursor = window.cursor;
          if (info.frame) {
            if (info.node) {
              if (info.frame & 1) {
                cursor.fixStart(info.node);
              }
              if (info.frame & 2) {
                cursor.fixEnd(info.node);
              }
            } else {
              console.error("info.node did not exist");
            }

            var sel = cursor.makeSelection();
            var range = cursor.makeRange();

            cursor.fixSelection(sel, range);
          }
        }
      });

      // List of pretty name of all users (mapped with their server ID).
      var userData = {};
      // List of users still connected to the channel (server IDs).
      var userList;
      var myId;

      var fixMacros = function() {
        var dataValues = {};
        var $elements = $(window.innerDoc).find('[data-cke-widget-data]');
        $elements.each(function(idx, element) {
          dataValues[idx] = $(element).attr('data-cke-widget-data');
        });
        editor.widgets.instances = {};
        editor.widgets.checkWidgets();
        $elements.each(function(idx, element) {
          $(element).attr('data-cke-widget-data', dataValues[idx]);
        });
      };

      // Apply patches and try not to lose the cursor in the process!
      var applyHjson = function(shjson) {
        var userDocStateDom = Hyperjson.toDOM(JSON.parse(shjson));
        userDocStateDom.setAttribute('contenteditable', 'true');
        var patch = DD.diff(window.inner, userDocStateDom);
        DD.apply(window.inner, patch);
        try {
          fixMacros();
        } catch (e) {
          console.log("Unable to fix the macros.", e);
        }
      };

      var findMacroComments = function(el) {
        var arr = [];
        for (var i = 0; i < el.childNodes.length; i++) {
          var node = el.childNodes[i];
          if (node.nodeType === 8 && node.data && /startmacro/.test(node.data)) {
            arr.push(node);
          } else {
            arr.push.apply(arr, findMacroComments(node));
          }
        }
        return arr;
      };

      var createSaver = function(info) {
        Saver.lastSaved.mergeMessage = Interface.createMergeMessageElement(
          toolbar.toolbar.find('.rt-toolbar-rightside'));
        Saver.setLastSavedContent(editor._.previousModeData);
        var saverCreateConfig = {
          // Id of the wiki page form.
          formId: window.XWiki.editor === "wysiwyg" ? "edit" : "inline",
          setTextValue: function(newText, toConvert, callback) {
            var andThen = function(data) {
              var doc = window.DOMDoc = (new DOMParser()).parseFromString(data,"text/html");
              window.cursor.update();
              doc.body.setAttribute("contenteditable", "true");
              var patch = (DD).diff(window.inner, doc.body);
              (DD).apply(window.inner, patch);

              // If available, transform the HTML comments for XWiki macros into macros before saving
              // (<!--startmacro:{...}-->). We can do that by using the "xwiki-refresh" command provided the by
              // CKEditor Integration application.
              if (editor.plugins['xwiki-macro'] && findMacroComments(window.inner).length > 0) {
                initializing = true;
                editor.execCommand('xwiki-refresh');
                afterRefresh.push(callback);
              } else {
                callback();
                realtimeOptions.onLocal();
              }
            };
            if (toConvert) {
              var object = {
                wiki: XWiki.currentWiki,
                space: XWiki.currentSpace,
                page: XWiki.currentPage,
                convert: true,
                text: newText
              };
              $.post(htmlConverterUrl + '?xpage=plain&outputSyntax=plain', object).done(function(data) {
                andThen(data);
              }).fail(function(err) {
                var debugLog = {
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
              // ckError: "The content cannot be saved because of a CKEditor internal error. You should try to copy
              //   your important changes and reload the editor.",
              // ckError: "Le contenu n'a pas pu être sauvé à cause d'une erreur interne de CKEditor. Vous devriez
              //   essayer de copier vos modifications importantes et de recharger la page.",
              editor.showNotification(Messages.ckError, 'warning');
              return null;
            }
          },
          realtime: info.realtime,
          userList: info.userList,
          userName,
          network: info.network,
          channel: eventsChannel,
          safeCrash: function(reason, debugLog) {
            module.onAbort(null, reason, debugLog);
          }
        };
        Saver.create(saverCreateConfig);
      };

      var getXPath = function(element) {
        var xpath = '';
        for ( ; element && element.nodeType == 1; element = element.parentNode ) {
          var id = $(element.parentNode).children(element.tagName).index(element) + 1;
          id = id > 1 ? '[' + id + ']' : '';
          xpath = '/' + element.tagName.toLowerCase() + id + xpath;
        }
        return xpath;
      };

      var getPrettyName = function(userName) {
        return userName ? userName.replace(/^.*-([^-]*)%2d[0-9]*$/, function(all, one) { 
          return decodeURIComponent(one);
        }) : userName;
      };

      editor.on('toDataFormat', function(evt) {
        var root = evt.data.dataValue;
        var toRemove = [];
        var toReplaceMacro = [];
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
        var macroWidget;
        for (var widget in editor.widgets.instances) {
          if (widget.name && widget.name === 'xwiki-macro') {
            macroWidget = widget;
            break;
          }
        }
        if (macroWidget) {
          toReplaceMacro.forEach(function (el) {
            var container = el.parent;
            var newNode = macroWidget.downcast(el);
            var index = container.parent.children.indexOf(container);
            container.parent.children[index] = newNode;
          });
        }
      }, null, null, 12 );

      var changeUserIcons = function(newdata) {
        if (!realtimeConfig.marginAvatar) {
          return;
        }

        // If no new data (someone has just joined or left the channel), get the latest known values.
        var updatedData = newdata || userData;

        $(window.innerDoc).find('.rt-user-position').remove();
        var positions = {};
        var requiredPadding = 0;
        userList.users.filter(id => updatedData[id]?.['cursor_' + editorId]).forEach(id => {
          var data = updatedData[id];
          var name = getPrettyName(data.name);
          // Set the user position.
          var element = window.innerDoc.evaluate(data['cursor_' + editorId], window.innerDoc, null,
            XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
          if (!element) {
            return;
          }
          var pos = $(element).offset();
          if (!positions[pos.top]) {
            positions[pos.top] = [id];
          } else {
            positions[pos.top].push(id);
          }
          var index = positions[pos.top].length - 1;
          var posTop = pos.top + 3;
          var posLeft = index * 16;
          requiredPadding = Math.max(requiredPadding, (posLeft + 10));
          var $indicator;
          if (data.avatar) {
            $indicator = $('<img alt=""/>').attr('src', data.avatar);
          } else {
            $indicator = $('<div/>').text(name.substring(0, 1));
          }
          $indicator.addClass('rt-non-realtime rt-user-position').attr({
            id: 'rt-user-' + id,
            title: name,
            contenteditable: 'false'
          }).css({
            'left': posLeft + 'px',
            'top': posTop + 'px'
          });
          $('html', window.innerDoc).append($indicator);
        });

        $(window.inner).css('padding-left', requiredPadding === 0 ? '' : ((requiredPadding + 15) + 'px'));
      };

      var isFirstOnReadyCall = true;

      var realtimeOptions = {
        // provide initialstate...
        initialState: stringifyDOM(inner) || '{}',

        // the websocket URL
        websocketURL: WebsocketURL,

        // our username
        userName: userName,

        // the channel we will communicate over
        channel: channel,

        // Crypto object to avoid loading it twice in Cryptpad
        crypto: Crypto,

        // Network loaded in realtime-frontend
        network: network,

        // OT
        //patchTransformer: Chainpad.NaiveJSONTransformer

        onRemote: function(info) {
          if (initializing) {
            return;
          }

          var shjson = info.realtime.getUserDoc();

          // Remember where the cursor is.
          window.cursor.update();

          // Build a DOM from HJSON, diff, and patch the editor.
          applyHjson(shjson);

          var shjson2 = stringifyDOM(window.inner);
          if (shjson2 !== shjson) {
            console.error('shjson2 !== shjson');
            var diff = Chainpad.Diff.diff(shjson, shjson2);
            console.log(shjson, diff);
            module.chainpad.contentUpdate(shjson2);
          }
        },

        onInit: function(info) {
          var $bar = $('#cke_1_toolbox');
          userList = info.userList;
          var config = {
            userData,
            onUsernameClick: function(id) {
              var basehref = iframe.contentWindow.location.href.split('#')[0] || '';
              iframe.contentWindow.location.href = basehref + '#rt-user-' + id;
            }
          };
          toolbar = Toolbar.create({
            '$container': $bar,
            myUserName: info.myID,
            realtime: info.realtime,
            getLag: info.getLag,
            userList: info.userList,
            config
          });
          // When someone leaves, if they used Save&View, it removes the locks from the document. We're going to add it
          // again to be sure new users will see the lock page and be able to join.
          var oldUsers = JSON.parse(JSON.stringify(userList.users || []));
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
          var shjson = module.chainpad.getUserDoc();

          myId = info.myId;

          if (isFirstOnReadyCall) {
            isFirstOnReadyCall = false;
            // Update the user list to link the wiki name to the user id.
            var userdataConfig = {
              myId: info.myId,
              userName,
              userAvatar,
              onChange: userList.onChange,
              crypto: Crypto,
              editor: editorId,
              getCursor: function() {
                var selection = editor.getSelection();
                if (!selection) {
                  return '';
                }
                var ranges = selection.getRanges();
                if (!ranges || !ranges[0] || !ranges[0].startContainer || !ranges[0].startContainer.$) {
                  return '';
                }
                var node = ranges[0].startContainer.$;
                node = (node.nodeName === '#text') ? node.parentNode : node;
                var xpath = getXPath(node);
                return xpath;
              }
            };
            if (!realtimeConfig.marginAvatar) {
              delete userdataConfig.getCursor;
            }

            userData = UserData.start(info.network, userdataChannel, userdataConfig);
            userList.change.push(changeUserIcons);
          }

          applyHjson(shjson);

          console.log('Unlocking editor');
          initializing = false;
          setEditable(true);
          module.chainpad.start();

          realtimeOptions.onLocal();
          createSaver(info);
        },

        onAbort: function(info, reason, debug) {
          console.log("Aborting the session!");
          var msg = reason || 'disconnected';
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
          if ($disallowButton[0].checked && !module.aborted) {
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
          var oldChannel = channel;
          updateKeys().done(function() {
            if (channel !== oldChannel) {
              editorConfig.onKeysChanged();
              setEditable(false);
              $disallowButton.prop('checked', false);
              module.onAbort();
            } else {
              callback(channel, stringifyDOM(window.inner));
            }
          });
        },

        // This function resets the realtime fields after coming back from source mode.
        onLocalFromSource: function() {
          var iframe = jQuery('iframe')[0]; 
          window.inner = iframe.contentWindow.body;
          window.innerDoc = iframe.contentWindow.document;
          window.cursor = Cursor(window.inner);
          iframe.onload = addStyle;
          addStyle();
          this.onLocal();
        },

        onLocal: function() {
          if (initializing) {
            return;
          }
          // Stringify the JSON and send it into ChainPad.
          var shjson = stringifyDOM(window.inner);
          module.chainpad.contentUpdate(shjson);

          if (module.chainpad.getUserDoc() !== shjson) {
            console.error("realtime.getUserDoc() !== shjson");
          }
        }
      };

      module.onAbort = realtimeOptions.onAbort;

      module.realtimeInput = ChainPadNetflux.start(realtimeOptions);

      // Hitting enter makes a new line, but places the cursor inside of the <br> instead of the <p>. This makes it such
      // that you cannot type until you click, which is rather unnacceptable. If the cursor is ever inside such a <br>,
      // you probably want to push it out to the parent element, which ought to be a paragraph tag. This needs to be
      // done on keydown, otherwise the first such keypress will not be inserted into the P.
      window.inner.addEventListener('keydown', window.cursor.brFix);

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
        window.cursor.update();
        var start = window.cursor.Range.start;
        var test = TypingTest.testInput(inner, start.el, start.offset, realtimeOptions.onLocal);
        realtimeOptions.onLocal();
        return test;
      };
    };

    return ckeditorPromise.then($.proxy(waitForEditorInstance, null, 'content')).done(function(editor) {
      // FIXME: This works only with the stand-alone (classic) editor.
      whenReady(editor, $(editor.container.$).find('iframe')[0]);
    });
  };

  return module;
});
