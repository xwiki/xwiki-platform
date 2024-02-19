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
  'xwiki-realtime-errorBox'
], function(
  /* jshint maxparams:false */
  $, chainpadNetflux, jsonSortify, Crypto, xwikiMeta, doc, ErrorBox
) {
  'use strict';

  function warn(...args) {
    console.log(...args);
  }

  function debug(...args) {
    console.log(...args);
  }

  function verbose() {
    // Do nothing for now.
  }

  const SAVE_DOC_TIME = 60000;
  // How often to check if the document has been saved recently.
  const SAVE_DOC_CHECK_CYCLE = 20000;

  function now() {
    return new Date().getTime();
  }

  const Saver = {
    configure: function(config) {
      $.extend(this.mainConfig, config, {
        safeCrash: function(reason) {
          warn(reason);
        }
      });
      $.extend(this.lastSaved, {
        version: doc.version,
        time: doc.modified
      });
    },

    interrupt: function() {
      if (this.lastSaved.receivedISAVE) {
        warn("Another client sent an ISAVED message.");
        warn("Aborting save action.");
        // Unset the flag, or else it will persist.
        this.lastSaved.receivedISAVE = false;
        // Return true such that calling functions know to abort.
        return true;
      }
      return false;
    },

    update: function(content) {
      $.extend(this.lastSaved, {
        time: now(),
        content,
        wasEditedLocally: false
      });
    },

    destroyDialog: function(callback) {
      const $box = $('.xdialog-box.xdialog-box-confirmation'),
        $content = $box.find('.xdialog-content');
      if ($box.length) {
        $content.find('.button.cancel').click();
      }
      if (typeof callback === 'function') {
        callback(!!$box.length);
      }
    },

    // Realtime editors should call this on local edits.
    setLocalEditFlag: function(condition) {
      this.lastSaved.wasEditedLocally = condition;
    }
  };

  const mainConfig = Saver.mainConfig = {};

  // Contains the realtime data.
  let rtData = {};

  const lastSaved = window.lastSaved = Saver.lastSaved = {
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
  };

  function bumpVersion(callback, versionData) {
    const success = (doc) => {
      debug('Triggering lastSaved refresh on remote clients.');
      lastSaved.version = doc.version;
      lastSaved.content = doc.content;
      /* jshint camelcase:false */
      const contentHash = mainConfig.chainpad?.hex_sha256?.(doc.content) || '';
      saveMessage(lastSaved.version, contentHash);
      if (typeof callback === 'function') {
        callback(doc);
      }
    };
    if (versionData) {
      success(versionData);
    } else {
      doc.reload().then(success).catch(error => {
        const debugLog = {
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
  }

  // sends an ISAVED message
  function saveMessage(version, hash) {
    const newState = {
      version: version,
      by: mainConfig.userName,
      hash: hash,
      editorName: mainConfig.editorName
    };
    rtData[mainConfig.editorType] = newState;
    mainConfig.onLocal();

    mainConfig.chainpad.onSettle(function() {
      lastSaved.onReceiveOwnIsave?.();
    });
  }

  // Only used within Saver.create().
  function redirectToView() {
    window.location.href = window.XWiki.currentDocument.getURL('view');
  }

  function onMessage(data) {
    // Set a flag so any concurrent processes know to abort.
    lastSaved.receivedISAVE = true;

    // RT_event-on_isave_receive
    //
    // Clients update lastSaved.version when they perform a save, then they send an ISAVED with the version. A single
    // user might have multiple windows open, for some reason, but might still have different save cycles. Checking
    // whether the received version matches the local version tells us whether the ISAVED was set by our *browser*. If
    // not, we should treat it as foreign.

    const newSave = (type, msg) => {
      const msgSender = msg.by;
      const msgVersion = msg.version;
      const msgEditor = type;
      const msgEditorName = msg.editorName;

      const displaySaverName = (isMerged) => {
        // A merge dialog might be open, if so, remove it and say as much.
        Saver.destroyDialog(function(dialogDestroyed) {
          if (dialogDestroyed) {
            // Tell the user about the merge resolution.
            lastSaved.mergeMessage('conflictResolved', [msgVersion]);
          } else if (!mainConfig.initializing) {
            let sender;
            // Otherwise say there was a remote save.
            // http://jira.xwiki.org/browse/RTWIKI-34
            if (mainConfig.userList) {
              sender = msgSender.replace(/^.*-([^-]*)%2d\d*$/, function(all, one) {
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
        } else {
          lastSaved.onReceiveOwnIsave?.();
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
    for (let editor in data) {
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
  }

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
    const netfluxNetwork = config.network;
    const channel = config.channel;

    lastSaved.time = now();

    const onOpen = () => {

      // There's a very small chance that the preview button might cause problems, so let's just get rid of it.
      $('[name="action_preview"]').remove();

      // Wait to get saved event.
      const onSavedHandler = mainConfig.onSaved = (event) => {
        // This means your save has worked. Cache the last version.
        const lastVersion = lastSaved.version;
        const toSave = mainConfig.getTextValue();
        // Update your content.
        this.update(toSave);

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

      // TimeOut
      function check() {
        // Schedule the next check before doing anything else, otherwise we stop the autosave.
        clearTimeout(mainConfig.autosaveTimeout);
        verbose("Saver.create.check");
        const periodDuration = Math.random() * SAVE_DOC_CHECK_CYCLE;
        mainConfig.autosaveTimeout = setTimeout(check, periodDuration);
        verbose(`Will attempt to save again in ${periodDuration} ms.`);

        let toSave;
        if (!lastSaved.wasEditedLocally || (toSave = mainConfig.getTextValue()) === lastSaved.content) {
          verbose("No changes made since last save. Avoiding unnecessary commits.");
          return;
        }

        if (toSave === null) {
          warn("Unable to get the edited content. Can't save.");
          return;
        }

        if (now() - lastSaved.time < SAVE_DOC_TIME) {
          verbose("Local changes detected, but not enough time has passed since the last check.");
          return;
        }

        // The merge conflict modal is displayed after clicking on the save button when there is a merge conflict.
        // Clicking the save button again would reopen the same modal and reset the fields the user did not submit yet.
        // We don't save if another user has saved since the last time we checked.
        if (!$('#previewDiffModal').is(':visible') && !Saver.interrupt()) {
          $(`#${config.formId} [name="action_saveandcontinue"]`).click();
        }
      }

      // Prevent the save buttons from reloading the page. Instead, reset the editor's content.
      $.extend(XWiki.actionButtons.AjaxSaveAndContinue.prototype, {
        reloadEditor: function() {
          // TODO: Handle the page title.
          mainConfig.getTextAtCurrentRevision().then((data) => {
            mainConfig.setTextValue(data);
          });
        }
      });

      check();
    };

    const module = window.SAVER_MODULE = {};
    mainConfig.initializing = true;

    const rtConfig = {
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
          const data = JSON.parse(module.chainpad.getUserDoc());
          onMessage(data);
        } catch (e) {
          warn("Unable to parse realtime data from the saver", e);
        }
      },

      onReady: function(info) {
        module.chainpad = mainConfig.chainpad = info.realtime;
        module.leave = mainConfig.leaveChannel = info.leave;
        try {
          const data = JSON.parse(module.chainpad.getUserDoc());
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
        const sjson = jsonSortify(rtData);
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
