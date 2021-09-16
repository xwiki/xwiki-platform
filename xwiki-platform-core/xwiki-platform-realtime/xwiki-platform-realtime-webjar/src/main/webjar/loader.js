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
define('xwiki-realtime-loader', [
  'jquery',
  'xwiki-meta',
  'xwiki-realtime-config',
  'xwiki-l10n!xwiki-realtime-messages',
  'xwiki-events-bridge'
], function($, xm, realtimeConfig, Messages) {
  'use strict';

  if (!realtimeConfig.webSocketURL) {
    console.log('The WebSocket URL is missing. Aborting attempt to configure a realtime session.');
    return false;
  }

  var module = {messages: Messages},
  documentReference = XWiki.Model.serialize(xm.documentReference),
  language = realtimeConfig.versionInfo?.locale || 'default',
  version = realtimeConfig.versionInfo?.version,
  versionTime = realtimeConfig.versionInfo?.time,

  getDocLock = module.getDocLock = function() {
    var lockedBy = document.querySelectorAll('p.xwikimessage .wikilink a');
    var force = document.querySelectorAll('a[href*="force=1"][href*="/edit/"]');
    return (lockedBy.length && force.length) ? force[0] : false;
  },

  isForced = module.isForced = window.location.href.indexOf("force=1") >= 0,
  isRt = module.isRt = window.location.href.indexOf("realtime=1") >= 0,

  getRTEditorURL = module.getEditorURL = function(href, info) {
    href = href.replace(/\?(.*)$/, function (all, args) {
      return '?' + args.split('&').filter(
        arg => !/^editor=/.test(arg) && !/^section=/.test(arg) && ['force=1', 'realtime=1'].indexOf(arg) < 0
      ).join('&');
    });
    if (href.indexOf('?') < 0) {
      href += '?';
    }
    href = href + info.href;
    return href;
  },

  allRt = {
    state: false
  },

  ajaxVersionURL = new XWiki.Document('Version', 'RTFrontend').getURL('get'),
  getConfig = module.getConfig = function() {
    var userReference = xm.userReference ? XWiki.Model.serialize(xm.userReference) : 'xwiki:XWiki.XWikiGuest';
    return {
      saverConfig: {
        ajaxMergeUrl: new XWiki.Document('Ajax', 'RTFrontend').getURL('get'),
        ajaxVersionURL,
        messages: Messages,
        language,
        version,
        safeSave
      },
      WebsocketURL: realtimeConfig.webSocketURL,
      htmlConverterUrl: new XWiki.Document('ConvertHTML', 'RTFrontend').getURL('get'),
      // userId === <userReference>-encoded(<userName>)%2d<random number>
      userName: userReference + '-' + encodeURIComponent(realtimeConfig.user.name + '-').replace(/-/g, '%2d') +
        String(Math.random()).substring(2),
      language,
      reference: documentReference,
      DEMO_MODE: realtimeConfig.demoMode,
      LOCALSTORAGE_DISALLOW: 'realtime-disallow',
      userAvatarURL: realtimeConfig.user.avatarURL,
      isAdvancedUser: realtimeConfig.user.advanced,
      network: allRt.network,
      abort: $.proxy(module, 'onRealtimeAbort'),
      onKeysChanged: $.proxy(module, 'onKeysChanged'),
      displayDisableModal: $.proxy(module, 'displayDisableModal'),
    };
  },

  checkSocket = function(callback) {
    var path = new XWiki.Document('GetKey', 'RTFrontend').getURL('get', 'outputSyntax=plain');
    // TODO: Pass the channel path instead of separate mod and editor parameters.
    var editorData = [{doc: documentReference, mod: language + '/content', editor: ''}];
    $.ajax({
      url: path,
      data: 'data=' + encodeURIComponent(JSON.stringify(editorData)),
      type: 'POST'
    }).done(function(dataText) {
      var data = JSON.parse(dataText);
      var types = [];
      if (data.error) {
        console.error("You don't have permissions to edit that document.");
        return;
      }
      var mods = data[documentReference];
      if (!mods) {
        console.error('Unknown error');
        return;
      }
      var content = mods[language + '/content'];
      if (!content) {
        console.error("Unknown error");
        return;
      }
      for (var editor in content) {
        // FIXME: We pass editor:'' above and the server will return only the channel for this editor.
        if (editor && content[editor].users && content[editor].users > 0) {
          types.push(editor);
        }
      }
      callback(types);
    });
  },

  getKeys = module.getKeys = function(editorData, callback) {
    var path = new XWiki.Document('GetKey', 'RTFrontend').getURL('get', 'outputSyntax=plain');
    var dataList = [];
    $.ajax({
      url: path,
      data: 'data=' + encodeURIComponent(JSON.stringify(editorData)),
      type: 'POST'
    }).done(function(dataText) {
      var data = JSON.parse(dataText);
      if (data.error) {
        console.error("You don't have permissions to edit that document.");
        return;
      }
      callback(data);
    });
  },

  lock = getDocLock();

  module.checkSessions = function(info) {
    if (lock) {
      // Found an edit lock link.
      checkSocket(function(types) {
        // Determine if it's a realtime session.
        if (types.length) {
          console.log('Found an active realtime session.');
          displayModal(null, types, null, info);
        } else {
          console.log("Couldn't find an active realtime session.");
          module.whenReady(function (rt) {
            if (rt) {
              displayModal(null, null, null, info);
            }
          });
        }
      });
    } else {
      // Do nothing.
    }
  };

  var displayModal = module.displayModal = function(createType, existingTypes, callback, info) {
    if (XWiki.widgets.RealtimeCreateModal) {
      return;
    }
    existingTypes = existingTypes || [];
    XWiki.widgets.RealtimeCreateModal = Class.create(XWiki.widgets.ModalPopup, {
      initialize: function($super) {
        $super(
          this.createContent(),
          {
            'show': {method: this.showDialog, keys: []},
            'close': {method: this.closeDialog, keys: ['Esc']}
          },
          {
            displayCloseButton: true,
            verticalPosition: 'center',
            // FIXME: Use color theme or remove this line.
            backgroundColor: '#FFF',
            removeOnClose: true
          }
        );
        this.showDialog();
        this.setClass('realtime-create-session');
        // FIXME: Use a better (namespaced) event name.
        $(document).trigger('insertButton');
      },

      /**
       * Gets the content of the modal dialog using AJAX.
       */
      createContent : function() {
        var message = Messages.requestASession;
        if (existingTypes.length > 1) {
          message = Messages['redirectDialog.pluralPrompt'];
        } else if (existingTypes.length === 1) {
          message = Messages.sessionInProgress;
        }

        var content = createModalContent(message, Messages.get('redirectDialog.create', info.name));
        var classesButtons = existingTypes.map(type => 'realtime-button-' + type).join(' ');
        var buttonsDiv = content.find('.realtime-buttons').addClass(classesButtons).data('modal', this);
        buttonsDiv.find('button').click(function() {
          callback();
          buttonsDiv.data('modal').closeDialog();
        }).toggle(!!createType);

        return content[0];
      }
    });
    return new XWiki.widgets.RealtimeCreateModal();
  },

  displayCustomModal = function(content) {
    XWiki.widgets.RealtimeRequestModal = Class.create(XWiki.widgets.ModalPopup, {
      initialize : function($super) {
        $super(
          this.createContent(),
          {
            'show': {method: this.showDialog, keys: []}
          },
          {
            displayCloseButton: false,
            verticalPosition : 'center',
            // FIXME: Use color theme or remove this line.
            backgroundColor : '#FFF',
            removeOnClose : true
          }
        );
        this.showDialog();
      },
      createContent: function() {
        $(content).find('button, input').click($.proxy(function() {
          this.closeDialog();
        }, this));
        return content;
      }
    });
    return new XWiki.widgets.RealtimeRequestModal();
  },

  createModalContent = function(message, primaryActionLabel) {
    var content = $(
      '<div class="modal-popup">' +
        '<p/>' +
        '<div class="realtime-buttons">' +
          '<button class="btn btn-primary"/>' +
        '</div>' +
      '</div>'
    );
    content.find('p').text(message);
    content.find('button').text(primaryActionLabel);
    return content;
  },

  getRequestContent = function(info, callback) {
    var content = createModalContent(Messages['requestDialog.prompt'], Messages.get('requestDialog.create', info.name));

    // Initialize auto-accept
    var autoAccept = $('<p/>').appendTo(content);
    var i = 30;
    var interval = setInterval(function() {
      i--;
      autoAccept.html(Messages['requestDialog.autoAccept'] + i + 's');
      if (i <= 0) {
        buttonCreate.click();
        clearInterval(interval);
        autoAccept.remove();
      }
    }, 1000);

    var buttonCreate = content.find('button').click(function() {
      clearInterval(interval);
      try {
        callback(true);
      } catch (e) {
        console.error(e);
      }
    });

    var buttonReject = $('<button class="btn btn-danger"/>').text(Messages['requestDialog.reject']);
    buttonReject.insertBefore(buttonCreate).click(function() {
      clearInterval(interval);
      try {
        callback(false);
      } catch (e) {
        console.error(e);
      }
    });

    return content[0];
  },

  getRejectContent = function(reason) {
    return createModalContent(
      reason === 'invalid' ? Messages['rejectDialog.invalid'] : Messages['rejectDialog.prompt'],
      Messages['rejectDialog.ok']
    )[0];
  },

  getReloadContent = function() {
    var content = createModalContent(Messages['reloadDialog.prompt'], Messages['reloadDialog.exit']);
    var buttonReload = $('<button class="btn btn-default"/>').text(Messages['reloadDialog.reload']);
    buttonReload.click($.proxy(window.location, 'reload', true)).insertAfter(content.find('button'));
    return content[0];
  },

  getSaveErrorContent = function() {
    return createModalContent(Messages['requestDialog.saveError'], Messages['rejectDialog.ok'])[0];
  };

  module.displayRequestErrorModal = function() {
    displayCustomModal(getSaveErrorContent());
  };

  module.displayDisableModal = function(callback) {
    var content = createModalContent(Messages['disableDialog.prompt'], Messages['disableDialog.ok']);

    var buttonOK = content.find('button').click($.proxy(callback, null, true));
    $('<button class="btn btn-default"/>').text(Messages['disableDialog.exit']).insertBefore(buttonOK)
      .click($.proxy(callback, null, false));
    return void displayCustomModal(content[0]);
  };

  var availableRt = {};
  module.setAvailableRt = function(type, info, cb) {
    availableRt[type] = {info, cb};
  };

  var isEditorCompatible = function() {
    var matchedType;
    Object.keys(availableRt).some(function(type) {
      if ((availableRt[type].info.compatible || []).indexOf(XWiki.editor) !== -1) {
        matchedType = type;
        return true;
      }
    });
    return matchedType;
  },

  unload = false;
  window.addEventListener('beforeunload', function() {
    unload = true;
    setTimeout(function() {
      unload = false;
    }, 5000);
  });

  var fullScreen = !!($('body').attr('data-maximized') || $('html').attr('style')),

  // Trigger a resize event to resize the editable area in fullscreen mode.
  resize = function() {
    var event;
    if (typeof(Event) === 'function') {
      event = new Event('resize');
    } else {
      event = document.createEvent('Event');
      event.initEvent('resize', true, true);
    }
    setTimeout(function() {
      window.dispatchEvent(event);
    });
  },

  // Place the warning box at the correct position when in fullscreen mode.
  getBoxPosition = function() {
    return fullScreen ? $('.buttons') : $('#hierarchy');
  },

  moveBox = function() {
    $('.xwiki-realtime-box').insertAfter(getBoxPosition()).show();
    $('.xwiki-realtime-box').css('margin-bottom', fullScreen ? '0' : '');
    resize();
  },

  // Detect fullscreen mode in CKeditor.
  // FIXME: Modify the CKEditor to fire the fullscreen events.
  observer = new MutationObserver(function(mutations) {
    mutations.forEach(function (mutation) {
      if (mutation.type === 'attributes' && mutation.attributeName === 'data-maximized') {
        fullScreen = $('body').attr('data-maximized') === 'true';
        moveBox();
      }
    });
  }).observe($('body')[0], {
    attributes: true
  });

  // Detect fullscreen mode in wiki editor.
  $(document).on('xwiki:fullscreen:exited', function() {
    fullScreen = false;
    moveBox();
  }).on('xwiki:fullscreen:entered', function() {
    fullScreen = true;
    moveBox();
  });

  // Scroll to the warning box when a message is displayed or updated.
  var scrollToBox = function($box) {
    moveBox();
    $box[0].scrollIntoView();
  },

  warningVisible = false,
  displayWarning = function() {
    var $after = getBoxPosition();
    if (unload || warningVisible || !$after.length) {
      return;
    }
    warningVisible = true;
    var $warning = $('<div>', {
      'class': 'xwiki-realtime-warning xwiki-realtime-box box warningmessage'
    }).insertAfter($after);
    scrollToBox($warning);
    $('<strong>').text(Messages.conflictsWarning).appendTo($warning);
    $('<br>').appendTo($warning);
    $('<span>').text(Messages.wsErrorConflicts).appendTo($warning);
    var editor = isEditorCompatible();
    if (!module.isRt && editor) {
      $('<br>').appendTo($warning);
      $('<span>').html(Messages.conflictsWarningInfo).appendTo($warning);
      $('<a>', {
        href: getRTEditorURL(window.location.href, availableRt[editor].info)
      }).text(Messages.conflictsWarningInfoLink).appendTo($warning);
    } else if (module.isRt) {
      $('<br>').appendTo($warning);
      $('<span>').text(Messages.conflictsWarningInfoRt).appendTo($warning);
    }
  },

  displayWsWarning = function(isError) {
    var $after = getBoxPosition();
    if (unload || warningVisible || !$after.length) {
      return;
    }
    warningVisible = true;
    var type = isError ? 'errormessage' : 'warningmessage';
    var $warning = $('<div>', {
      'class': 'xwiki-realtime-warning xwiki-realtime-box box ' + type
    }).insertAfter($after);
    scrollToBox($warning);
    $('<strong>').text(Messages.wsError).appendTo($warning);
    $('<br>').appendTo($warning);
    $('<span>').text(Messages.wsErrorInfo).appendTo($warning);
    if (module.isForced) {
      $('<br>').appendTo($warning);
      $('<span>').text(Messages.wsErrorConflicts).appendTo($warning);
    }
  },

  hideWarning = function() {
    warningVisible = false;
    $('.xwiki-realtime-warning').remove();
    resize();
  },

  connectingVisible = false,
  displayConnecting = function() {
    var $after = getBoxPosition();
    if (unload || connectingVisible || !$after.length) {
      return;
    }
    connectingVisible = true;
    var $warning = $('<div>', {
      'class': 'xwiki-realtime-connecting xwiki-realtime-box box infomessage'
    }).insertAfter($after);
    scrollToBox($warning);
    $('<strong>').text(Messages.connectingBox).appendTo($warning);
  },

  hideConnecting = function() {
    warningVisible = false;
    $('.xwiki-realtime-connecting').remove();
    resize();
  },

  wsErrorVisible = false,
  displayWsError = function() {
    var $after = getBoxPosition();
    if (unload || wsErrorVisible || !$after.length) {
      return;
    }
    wsErrorVisible = true;
    var $warning = $('<div>', {
      'class': 'xwiki-realtime-disconnected xwiki-realtime-box box errormessage'
    }).insertAfter($after);
    scrollToBox($warning);
    $('<strong>').text(Messages.connectionLost).appendTo($warning);
    $('<br>').appendTo($warning);
    $('<span>').text(Messages.connectionLostInfo).appendTo($warning);
  },

  hideWsError = function() {
    wsErrorVisible = false;
    $('.xwiki-realtime-disconnected').remove();
    resize();
  },

  tryParse = function(msg) {
    try {
      return JSON.parse(msg);
    } catch (e) {
      console.error('Cannot parse the message');
    }
  },

  getDocumentStatistics = function() {
    return {
      document: $('html').data('xwiki-document'),
      language
    };
  },

  checkVersion = function (callback) {
    $.ajax({
      url: ajaxVersionURL + '?xpage=plain',
      method: 'POST',
      dataType: 'json',
      success: function(data) {
        callback(null, data);
      },
      data: getDocumentStatistics(),
      error: function(error) {
        callback(error, null);
      }
    });
  },

  editForm = $('#edit').length ? $('#edit') : $('#inline'),
  shouldRedirect = false,
  previewButton = $('#mainEditArea').find('input[name="action_preview"]'),
  save = function(cont, preview) {
    if (preview) {
      previewButton.data('checked', true);
      previewButton.click();
    } else {
      shouldRedirect = !cont;
      $(document).trigger('xwiki:actions:save', {
        form: editForm[0],
        continue: 1
      });
    }
  },

  safeSave = function(cont, preview, old, callback) {
    old = old || {version, versionTime};
    callback = callback || save;
    callback(cont, preview);
  },

  // Join a channel with all users on this page (realtime, offline AND lock page)
  // 1. This channel allows users on "lock" page to contact the editing user and request a collaborative session, using
  //    the `request` and `answer` commands
  // 2. It is also used to know if someone else is editing the document concurrently (at least 2 users with 1 editing
  //    offline). In this case, a warning message can be displayed.
  //
  // When someone starts editing the page, they send a `join` message with a boolean 'realtime'. When other users
  // receive this message, they can tell if there is a risk of conflict and send a `displayWarning` command to the new
  // user.
  addMessageHandler = function() {
    if (!allRt.wChan) {
      return;
    }
    var channel = allRt.wChan;
    var network = allRt.network;
    // Handle leave events.
    channel.on('leave', function() {
      hideWarning();
      channel.bcast(JSON.stringify({
        cmd: 'isSomeoneOffline'
      }));
    });
    // Handle incoming messages.
    channel.on('message', function(msg, sender) {
      var data = tryParse(msg);
      switch(data?.cmd) {
        // Someone wants to create a realtime session. If the current user is editing offline, display the modal.
        case 'request': return onRequestMessage(data, channel);
        // Receiving an answer to a realtime session request.
        case 'answer': return onAnswerMessage(data);
        // Someone is joining the channel while we're editing, check if they are using realtime and if we are.
        case 'join': return onJoinMessage(data, sender, channel, network);
        // Someone wants to know if we're editing offline to know if the warning message should be displayed.
        case 'isSomeoneOffline': return onIsSomeoneOfflineMessage(sender, network);
      }      
    });
  },

  onRequestMessage = function(data, channel) {
    if (lock || !data.type) {
      return;
    }
    var response = {
      cmd: 'answer',
      type: data.type
    };
    // Make sure realtime is available for the requested editor.
    if (!availableRt[data.type]) {
      response.state = -1;
      channel.bcast(JSON.stringify(response));
    // Check if we're not already in realtime.
    } else if (module.isRt) {
      response.state = 2;
      channel.bcast(JSON.stringify(response));
    // Check if our current editor is realtime compatible, i.e. Object editor can't switch to WYSIWYG.
    } else if (!isEditorCompatible()) {
      response.state = 0;
      response.reason = 'invalid';
      channel.bcast(JSON.stringify(response));
    // We're editing offline: display the modal.
    } else {
      var content = getRequestContent(availableRt[data.type].info, function(state) {
        if (state) {
          // Accepted: save and create the realtime session.
          availableRt[data.type].cb();
        }
        response.state = state ? 1 : 0;
        channel.bcast(JSON.stringify(response));
      });
      setTimeout(function() {
        $('.xdialog-modal-container').css('z-index', '99999');
      });
      displayCustomModal(content);
    }
  },

  onAnswerMessage = function(data) {
    if (!allRt.request) {
      return;
    }
    var state = data.state;
    allRt.request(state);
    if (state === -1) {
      ErrorBox.show('unavailable');
    } else if (state === 0) {
      // Rejected
      $('.realtime-buttons').data('modal')?.closeDialog();
      displayCustomModal(getRejectContent(data.reason));
    }
  },

  onJoinMessage = function(data, sender, channel, network) {
    if (lock) {
      return;
    } else if (!data.realtime || !module.isRt) {
      displayWarning();
      network.sendto(sender, JSON.stringify({
        cmd: 'displayWarning'
      }));
    } else if (warningVisible) {
      hideWarning();
      channel.bcast(JSON.stringify({
        cmd: 'isSomeoneOffline'
      }));
    }
  },

  onIsSomeoneOfflineMessage = function(sender, network) {
    if (!lock && !module.isRt) {
      network.sendto(sender, JSON.stringify({
        cmd: 'displayWarning'
      }));
    }
  },

  joinAllUsers = function() {
    var config = getConfig();
    var keyData = [{
      doc: config.reference,
      mod: config.language + '/events',
      editor: 'all'
    }];
    getKeys(keyData, function(data) {
      var channelKey = data?.[config.reference]?.[config.language + '/events']?.all.key;
      if (channelKey) {
        require(['netflux-client', 'xwiki-realtime-errorBox'], function(Netflux, ErrorBox) {
          var onError = function (error) {
            allRt.error = true;
            displayWsWarning();
            console.error(error);
          };
          // Connect to the websocket server.
          Netflux.connect(config.WebsocketURL).then($.proxy(onNetfluxConnect, null, config, keyData, channelKey,
            onError), onError);
        });
      }
    });
  },

  onNetfluxConnect = function(config, keyData, channelKey, onError, network) {
    allRt.network = network;
    var onOpen = function(channel) {
      allRt.userList = channel.members;
      allRt.wChan = channel;
      addMessageHandler();
      // If we're in edit mode (not locked), tell the other users.
      if (!lock) {
        channel.bcast(JSON.stringify({
          cmd: 'join',
          realtime: module.isRt
        }));
      }
    };
    // Join the "all" channel.
    network.join(channelKey).then(onOpen, onError);
    // Add direct messages handler.
    network.on('message', function(msg, sender) {
      var data = tryParse(msg);
      if (data?.cmd === 'displayWarning') {
        displayWarning();
      }
    });
    // On reconnect, join the "all" channel again.
    network.on('reconnect', function() {
      hideWarning();
      hideWsError();
      getKeys(keyData, function(data) {
        var channelKey = data?.[config.reference]?.[config.language + '/events']?.all.key;
        network.join(channelKey).then(onOpen, onError);
      });
    });
    network.on('disconnect', function() {
      if (module.isRt) {
        displayWsError();
      } else {
        displayWsWarning();
      }
    });
  },

  fixActionButtonsInOfflineMode = function() {
    if (!editForm.length || module.isRt) {
      // Either not editing or online.
      return;
    }

    $(document).on('xwiki:document:saved', function () {
      checkVersion(function(error, data) {
        if (!error && data && data.version) {
          version = data.version;
          versionTime = data.versionTime;
        }
      });
      if (!shouldRedirect) {
        return;
      }
      // CKEditor tries to block the user from leaving the page with unsaved content. Our save mechanism doesn't
      // update the flag about unsaved content, so we have to do it manually.
      // FIXME: Trigger some event instead of using directly the CKEditor API.
      if (window.CKEDITOR) {
        try {
          CKEDITOR.instances.content.resetDirty();
        } catch (error) {
          // Ignore.
        }
      }
      window.location.href = window.XWiki.currentDocument.getURL('view');
    });

    // Replace the save actions to check the version first.
    var saveButton = $('#mainEditArea').find('input[name="action_save"]');
    // FIXME: Find a way to remove the click listeners without using the Prototype.js API.
    saveButton[0]?.stopObserving();
    saveButton.off('click').click(function (event) {
      event.preventDefault();
      event.stopPropagation();
      safeSave(false);
    });

    var saveAndContinueButton = $('#mainEditArea').find('input[name="action_saveandcontinue"]');
    // FIXME: Find a way to remove the click listeners without using the Prototype.js API.
    saveAndContinueButton[0]?.stopObserving();
    saveAndContinueButton.off('click').click(function (event) {
      event.preventDefault();
      event.stopPropagation();
      safeSave(true);
    });

    previewButton.click(function (event) {
      if (previewButton.data('checked')) {
        previewButton.data('checked', false);
      } else {
        event.preventDefault();
        event.stopPropagation();
        safeSave(null, true);
      }
    });
  };

  $.extend(module, {
    requestRt: function(type, callback) {
      if (!allRt.wChan) {
        setTimeout(function () {
          module.requestRt(type, callback);
        }, 500);
      } else if (allRt.userList.length === 1) {
        // No other user.
        callback(false);
      } else {
        allRt.request = callback;
        allRt.wChan.bcast(JSON.stringify({
          cmd: 'request',
          type: 'wysiwyg'
        }));
      }
    },

    onRealtimeAbort: function() {
      module.isRt = false;
      allRt.wChan?.bcast(JSON.stringify({
        cmd: 'join',
        realtime: module.isRt
      }));
    },

    whenReady: function(callback) {
      displayConnecting();
      // We want realtime enabled so we have to wait for the network to be ready.
      if (allRt.network) {
        hideConnecting();
        callback(true);
      } else if (allRt.error) {
        // Can't connect to network: hide the warning about "not being warned when some wants RT" and display error
        // about not being able to enable WebSocket.
        hideConnecting();
        hideWarning();
        displayWsWarning(true);
        callback(false);
      } else {
        setTimeout($.proxy(module, 'whenReady', callback), 100);
      }
    },

    onKeysChanged: function() {
      // The channel keys have changed while we were offline. We may not have the latest version of the document. The
      // safest solution is to reload.
      // FIXME: This module shouldn't know about CKEditor.
      sessionStorage.refreshCk = 'true';
      displayCustomModal(getReloadContent());
    }
  });

  $(fixActionButtonsInOfflineMode);
  joinAllUsers();

  return module;
});

