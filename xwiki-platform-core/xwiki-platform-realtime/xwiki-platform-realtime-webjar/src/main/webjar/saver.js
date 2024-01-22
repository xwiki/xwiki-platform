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
define('xwiki-realtime-saver', [
  'jquery',
  'chainpad-netflux',
  'json.sortify',
  'xwiki-realtime-crypto',
  'xwiki-meta',
  'xwiki-realtime-document',
  'xwiki-l10n!xwiki-realtime-messages',
  'xwiki-realtime-errorBox'
], /* jshint maxparams:false */ function($, chainpadNetflux, jsonSortify, Crypto, xwikiMeta, doc, Messages, ErrorBox) {
  'use strict';

  var warn = function() {
    console.log.apply(console, arguments);
  }, debug = function() {
    console.log.apply(console, arguments);
  }, verbose = function() {};

  var SAVE_DOC_TIME = 60000,
    // How often to check if the document has been saved recently.
    SAVE_DOC_CHECK_CYCLE = 20000;

  var now = function() {
    return new Date().getTime();
  };

  var Saver = {};

  var mainConfig = Saver.mainConfig = {};

  // Contains the realtime data.
  var rtData = {};

  var lastSaved = window.lastSaved = Saver.lastSaved = {
    content: '',
    time: 0,
    // http://jira.xwiki.org/browse/RTWIKI-37
    hasModifications: false,
    // For future tracking of 'edited since last save'. Only show the merge dialog to those who have edited.
    wasEditedLocally: false,
    receivedISAVE: false,
    shouldRedirect: false,
    isavedSignature: '',
    mergeMessage: function() {}
  },

  configure = Saver.configure = function(config) {
    $.extend(mainConfig, config, {
      safeCrash: function(reason) {
        warn(reason);
      }
    });
    $.extend(lastSaved, {
      version: doc.version,
      time: doc.modified
    });
  },

  updateLastSaved = Saver.update = function(content) {
    $.extend(lastSaved, {
      time: now(),
      content: content,
      wasEditedLocally: false
    });
  },

  isaveInterrupt = Saver.interrupt = function() {
    if (lastSaved.receivedISAVE) {
      warn("Another client sent an ISAVED message.");
      warn("Aborting save action.");
      // Unset the flag, or else it will persist.
      lastSaved.receivedISAVE = false;
      // Return true such that calling functions know to abort.
      return true;
    }
    return false;
  },

  bumpVersion = function(callback, versionData) {
    var success = function(doc) {
      debug('Triggering lastSaved refresh on remote clients.');
      lastSaved.version = doc.version;
      lastSaved.content = doc.content;
      /* jshint camelcase:false */
      var contentHash = (mainConfig.chainpad && mainConfig.chainpad.hex_sha256 &&
        mainConfig.chainpad.hex_sha256(doc.content)) || '';
      saveMessage(lastSaved.version, contentHash);
      if (typeof callback === 'function') {
        callback(doc);
      }
    };
    if (versionData) {
      success(versionData);
    } else {
      doc.reload().then(success).catch(error => {
        var debugLog = {
          state: 'bumpVersion',
          lastSavedVersion: lastSaved.version,
          lastSavedContent: lastSaved.content,
          cUser: mainConfig.userName,
          cContent: mainConfig.getTextValue()
        };
        mainConfig.safeCrash('updateversion', JSON.stringify(debugLog));
        warn(error);
      });
    }
  },

  // http://jira.xwiki.org/browse/RTWIKI-29
  saveDocument = function(data) {
    return doc.save($.extend({
      // TODO make this translatable
      comment: 'Auto-Saved by Realtime Session'
    }, data)).catch(response => {
      var debugLog = {
        state: 'saveDocument',
        lastSavedVersion: lastSaved.version,
        lastSavedContent: lastSaved.content,
        cUser: mainConfig.userName,
        cContent: mainConfig.getTextValue(),
        err: response.statusText
      };
      ErrorBox.show('save', JSON.stringify(debugLog));
      warn(response.statusText);
      return Promise.reject();
    });
  },

  // sends an ISAVED message
  saveMessage = function(version, hash) {
    var newState = {
      version: version,
      by: mainConfig.userName,
      hash: hash,
      editorName: mainConfig.editorName
    };
    rtData[mainConfig.editorType] = newState;
    mainConfig.onLocal();

    mainConfig.chainpad.onSettle(function() {
      if (typeof lastSaved.onReceiveOwnIsave === 'function') {
        lastSaved.onReceiveOwnIsave();
      }
    });
  },

  destroyDialog = Saver.destroyDialog = function(callback) {
    var $box = $('.xdialog-box.xdialog-box-confirmation'),
      $content = $box.find('.xdialog-content');
    if ($box.length) {
      $content.find('.button.cancel').click();
    }
    if (typeof callback === 'function') {
      callback(!!$box.length);
    }
  },

  // Only used within Saver.create().
  redirectToView = function() {
    window.location.href = window.XWiki.currentDocument.getURL('view');
  },

  // Have rtwiki call this on local edits.
  setLocalEditFlag = Saver.setLocalEditFlag = function(condition) {
    lastSaved.wasEditedLocally = condition;
  },

  onMessage = function(data) {
    // Set a flag so any concurrent processes know to abort.
    lastSaved.receivedISAVE = true;

    // RT_event-on_isave_receive
    //
    // Clients update lastSaved.version when they perform a save, then they send an ISAVED with the version. A single
    // user might have multiple windows open, for some reason, but might still have different save cycles. Checking
    // whether the received version matches the local version tells us whether the ISAVED was set by our *browser*. If
    // not, we should treat it as foreign.

    var newSave = function(type, msg) {
      var msgSender = msg.by;
      var msgVersion = msg.version;
      var msgEditor = type;
      var msgEditorName = msg.editorName;

      var displaySaverName = function(isMerged) {
        // A merge dialog might be open, if so, remove it and say as much.
        destroyDialog(function(dialogDestroyed) {
          if (dialogDestroyed) {
            // Tell the user about the merge resolution.
            lastSaved.mergeMessage('conflictResolved', [msgVersion]);
          } else if (!mainConfig.initializing) {
            var sender;
            // Otherwise say there was a remote save.
            // http://jira.xwiki.org/browse/RTWIKI-34
            if (mainConfig.userList) {
              sender = msgSender.replace(/^.*-([^-]*)%2d[0-9]*$/, function(all, one) {
                return decodeURIComponent(one);
              });
            }
            if (isMerged) {
              lastSaved.mergeMessage('savedRemote', [msgVersion, sender]);
            } else {
              lastSaved.mergeMessage('savedRemoteNoMerge', [msgVersion, sender, msgEditorName]);
            }
          }
        });
      };

      if (msgEditor === mainConfig.editorType) {
        if (lastSaved.version !== msgVersion) {
          displaySaverName(true);

          if (!mainConfig.initializing) {
            debug('A remote client saved and incremented the latest common ancestor.');
          }

          // Update lastSaved attributes.
          lastSaved.wasEditedLocally = false;

          // Update the local latest common ancestor version string.
          lastSaved.version = msgVersion;

          // Remember the state of the textArea when last saved so that we can avoid additional minor versions. There's
          // a *tiny* race condition here but it's probably not an issue.
          lastSaved.content = mainConfig.getTextValue();

          // Update the document meta in order to ensure proper merge on save (when using the form action buttons).
          doc.update({
            version: lastSaved.version,
            modified: now(),
            isNew: false
          });
        } else if (typeof lastSaved.onReceiveOwnIsave === 'function') {
          lastSaved.onReceiveOwnIsave();
        }
        lastSaved.time = now();
      } else {
        displaySaverName(false);
      }
    };

    // If the channel data is empty, do nothing (initial call in onReady).
    if (Object.keys(data).length === 0) {
      return;
    }
    for (var editor in data) {
      if (typeof data[editor] !== "object" || Object.keys(data[editor]).length !== 4) {
        // Corrupted data.
        continue;
      }
      if (rtData[editor] && jsonSortify(rtData[editor]) === jsonSortify(data[editor])) {
        // No change.
        continue;
      }
      newSave(editor, data[editor]);
      xwikiMeta.refreshVersion();
    }
    rtData = data;

    return false;
  };

  /**
   * This contains some of the more complicated logic in this script. Clients check for remote changes on random
   * intervals. If another client has saved outside of the realtime session, changes are merged on the server using
   * XWiki's threeway merge algo. The changes are integrated into the local textarea, which replicates across realtime
   * sessions. If the resulting state does not match the last saved content, then the contents are saved as a new
   * version. Other members of the session are notified of the save, and the resulting new version. They then update
   * their local state to match. During this process, a series of checks are made to reduce the number of unnecessary
   * saves, as well as the number of unnecessary merges.
   */
  Saver.create = function(config) {
    $.extend(mainConfig, config);
    mainConfig.formId = mainConfig.formId || 'edit';
    var netfluxNetwork = config.network;
    var channel = config.channel;

    lastSaved.time = now();

    var onOpen = function(chan) {

      // There's a very small chance that the preview button might cause problems, so let's just get rid of it.
      $('[name="action_preview"]').remove();

      // Wait to get saved event.
      const onSavedHandler = mainConfig.onSaved = function(event) {
        // This means your save has worked. Cache the last version.
        const lastVersion = lastSaved.version;
        const toSave = mainConfig.getTextValue();
        // Update your content.
        updateLastSaved(toSave);

        doc.reload().then(doc => {
          lastSaved.onReceiveOwnIsave = function() {
            // Once you get your isaved back, redirect.
            debug("lastSaved.shouldRedirect " + lastSaved.shouldRedirect);
            if (lastSaved.shouldRedirect) {
              debug('Saver.create.saveandview.receivedOwnIsaved');
              debug("redirecting!");
              redirectToView();
            } else {
              debug('Saver.create.saveandcontinue.receivedOwnIsaved');
            }
            // Clean up after yourself..
            lastSaved.onReceiveOwnIsave = null;
          };
          // Bump the version, fire your isaved.
          bumpVersion(function(doc) {
            if (doc.version === '1.1') {
              debug('Created document version 1.1');
            } else {
              debug(`Version bumped from ${lastVersion} to ${doc.version}.`);
            }
            lastSaved.mergeMessage('saved', [doc.version]);
          }, doc);
        }).catch(error => {
          warn(error);
          ErrorBox.show('save');
        });

        return true;
      };
      $(document).on('xwiki:document:saved.realtime-saver', onSavedHandler);

      var onSaveFailedHandler = mainConfig.onSaveFailed = function(ev) {
        var debugLog = {
          state: 'savedFailed',
          lastSavedVersion: lastSaved.version,
          lastSavedContent: lastSaved.content,
          cUser: mainConfig.userName,
          cContent: mainConfig.getTextValue()
        };
        if (ev.memo.response.status == 409) {
         console.log("XWiki conflict system detected. No RT error box should be shown");
        } else {
         ErrorBox.show('save', JSON.stringify(debugLog));
         warn("save failed!!!");
         console.log(ev);
        }
      };

      // TimeOut
      var check = function() {
        lastSaved.receivedISAVE = false;

        const toSave = mainConfig.getTextValue();
        if (toSave === null) {
          warn("Unable to get the content of the document. Don't save.");
          return;
        }

        if (lastSaved.content === toSave) {
          verbose("No changes made since last save. Avoiding unnecessary commits.");
          return;
        }

        clearTimeout(mainConfig.autosaveTimeout);
        verbose("Saver.create.check");
        var periodDuration = Math.random() * SAVE_DOC_CHECK_CYCLE;
        mainConfig.autosaveTimeout = setTimeout(check, periodDuration);

        verbose(`Will attempt to save again in ${periodDuration} ms.`);
        if (!lastSaved.wasEditedLocally || now() - lastSaved.time < SAVE_DOC_TIME) {
          verbose("!lastSaved.wasEditedLocally || (Now - lastSaved.time) < SAVE_DOC_TIME");
          return;
        }

        // The merge conflict modal is displayed after clicking on the save button when there is a merge conflict.
        // Clicking the save button again would reopen the same modal and reset the fields the user did not submit yet.
        if (!$('#previewDiffModal').is(':visible')) {
          $(`#${config.formId} [name="action_saveandcontinue"]`).click();
        }
      };

      // Prevent the save buttons from reloading the page. Instead, reset the editor's content.'
      var overrideAjaxSaveAndContinue = function() {
        var originalAjaxSaveAndContinue = $.extend({}, XWiki.actionButtons.AjaxSaveAndContinue.prototype);
        $.extend(XWiki.actionButtons.AjaxSaveAndContinue.prototype, {
          reloadEditor: function() {
            // TODO: Handle the page title.
            mainConfig.getTextAtCurrentRevision().then((data) => {mainConfig.setTextValue(data);});
          }
        });
      };

      overrideAjaxSaveAndContinue();
      check();
    };

    var module = window.SAVER_MODULE = {};
    mainConfig.initializing = true;

    var rtConfig = {
      initialState: '{}',
      network: netfluxNetwork,
      userName: mainConfig.userName || '',
      channel: channel,
      crypto: Crypto,

      onRemote: function(info) {
        if (mainConfig.initializing) {
          return;
        }

        try {
          var data = JSON.parse(module.chainpad.getUserDoc());
          onMessage(data);
        } catch (e) {
          warn("Unable to parse realtime data from the saver", e);
        }
      },

      onReady: function(info) {
        module.chainpad = mainConfig.chainpad = info.realtime;
        module.leave = mainConfig.leaveChannel = info.leave;
        try {
          var data = JSON.parse(module.chainpad.getUserDoc());
          onMessage(data);
        } catch (e) {
          warn("Unable to parse realtime data from the saver", e);
        }
        mainConfig.initializing = false;
        onOpen();
      },

      onLocal: function(info) {
        if (mainConfig.initializing) {
          return;
        }
        var sjson = jsonSortify(rtData);
        module.chainpad.contentUpdate(sjson);
        if (module.chainpad.getUserDoc() !== sjson) {
          warn("Saver: userDoc !== sjson");
        }
      },

      onAbort: function() {
        Saver.stop();
      }
    };

    mainConfig.onLocal = rtConfig.onLocal;

    chainpadNetflux.start(rtConfig);
  }; // END Saver.create()

  // Stop the autosaver / merge when the user disallows realtime or when the WebSocket is disconnected.
  Saver.stop = function() {
    if (mainConfig.realtime) {
      mainConfig.realtime.abort();
    }
    if (mainConfig.leaveChannel) {
      mainConfig.leaveChannel();
      delete mainConfig.leaveChannel;
    }
    clearTimeout(mainConfig.autosaveTimeout);
    rtData = {};
  };

  Saver.setLastSavedContent = function(content) {
    lastSaved.content = content;
  };

  return Saver;
});
