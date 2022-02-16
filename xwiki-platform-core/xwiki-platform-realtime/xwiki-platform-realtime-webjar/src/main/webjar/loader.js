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
  'xwiki-realtime-document',
  'xwiki-l10n!xwiki-realtime-messages',
  'xwiki-realtime-errorBox',
  'xwiki-events-bridge'
], function(
  /* jshint maxparams:false */
  $, xm, realtimeConfig, doc, Messages, ErrorBox
) {
  'use strict';

  if (!realtimeConfig.webSocketURL) {
    console.log('The WebSocket URL is missing. Aborting attempt to configure a realtime session.');
    return false;
  }

  var module = {messages: Messages},

  // FIXME: The real-time JavaScript code is not loaded anymore on the "lock" page so this code is not really used. We
  // need to decide if we want to re-add the real-time JavaScript code on the lock page and how.
  getDocLock = module.getDocLock = function() {
    var lockedBy = document.querySelectorAll('p.xwikimessage .wikilink a');
    var force = document.querySelectorAll('a[href*="force=1"][href*="/edit/"]');
    return (lockedBy.length && force.length) ? force[0] : false;
  },

  isForced = module.isForced = window.location.href.indexOf('force=1') >= 0,
  // Real-time enabled by default.
  isRt = module.isRt = window.location.href.indexOf('realtime=false') < 0,

  getRTEditorURL = module.getEditorURL = function(href, info) {
    href = href.replace(/\?(.*)$/, function (all, args) {
      return '?' + args.split('&').filter(
        arg => ['editor', 'section', 'force', 'realtime'].indexOf(arg.split('=', 1)[0]) < 0
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

  getConfig = module.getConfig = function() {
    var userReference = xm.userReference ? XWiki.Model.serialize(xm.userReference) : 'xwiki:XWiki.XWikiGuest';
    return {
      WebsocketURL: realtimeConfig.webSocketURL,
      htmlConverterUrl: new XWiki.Document('ConvertHTML', 'RTFrontend').getURL('get', 'xpage=plain&outputSyntax=plain'),
      // userId === <userReference>-encoded(<userName>)%2d<randomNumber>
      userName: userReference + '-' + encodeURIComponent(realtimeConfig.user.name + '-').replace(/-/g, '%2d') +
        String(Math.random()).substring(2),
      userAvatarURL: realtimeConfig.user.avatarURL,
      isAdvancedUser: realtimeConfig.user.advanced,
      network: allRt.network,
      abort: module.onRealtimeAbort.bind(module),
      onKeysChanged: module.onKeysChanged.bind(module),
      displayDisableModal: module.displayDisableModal.bind(module),
    };
  },

  // Returns a promise that resolves with the list of editor channels available for the content field of the current
  // document in the current language.
  checkSocket = function() {
    var path = doc.language + '/content/';
    return doc.getChannels({path}).then(function(channels) {
      return channels.filter(channel => channel?.path?.length > 2 && channel?.userCount > 0)
        .map(channel => channel.path.slice(2).join('/'));
    });
  },

  lock = getDocLock();

  module.checkSessions = function(info) {
    if (lock) {
      // Found an edit lock link.
      checkSocket().then(types => {
        // Determine if it's a realtime session.
        if (types.length) {
          console.log('Found an active realtime session.');
          displayModal(null, types, null, info);
        } else {
          console.log("Couldn't find an active realtime session.");
          module.whenReady(function(rt) {
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
        buttonsDiv.find('button').on('click', function() {
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
        $(content).find('button, input').on('click', this.closeDialog.bind(this));
        return content;
      }
    });
    return new XWiki.widgets.RealtimeRequestModal();
  },

  createModalContent = function(message, primaryActionLabel) {
    var content = $(
      '<div class="modal-popup">' +
        '<p></p>' +
        '<div class="realtime-buttons">' +
          '<button class="btn btn-primary"></button>' +
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
    var autoAccept = $('<p></p>').appendTo(content);
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

    var buttonCreate = content.find('button').on('click', function() {
      clearInterval(interval);
      try {
        callback(true);
      } catch (e) {
        console.error(e);
      }
    });

    var buttonReject = $('<button class="btn btn-danger"></button>').text(Messages['requestDialog.reject']);
    buttonReject.insertBefore(buttonCreate).on('click', function() {
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
    var buttonReload = $('<button class="btn btn-default"></button>').text(Messages['reloadDialog.reload']);
    buttonReload.on('click', window.location.reload.bind(window.location, true)).insertAfter(content.find('button'));
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

    var buttonOK = content.find('button').on('click', callback.bind(null, true));
    $('<button class="btn btn-default"></button>').text(Messages['disableDialog.exit']).insertBefore(buttonOK)
      .on('click', callback.bind(null, false));
    return void displayCustomModal(content[0]);
  };

  var availableRt = {};
  module.setAvailableRt = function(info) {
    availableRt[info.type] = {
      info,
      cb: createRt.bind(null, info)
    };
  };

  var createRtCalled = false,
  createRt = function(info) {
    if (createRtCalled) {
      return;
    }
    createRtCalled = true;
    var $saveButton = $('#mainEditArea').find('input[name="action_saveandcontinue"]');
    if ($saveButton.length) {
      var comment = $('#commentinput');
      var previousComment = comment.val();
      comment.val(Messages.autoAcceptSave);
      $saveButton.click();
      $(document).one('xwiki:document:saved.createRt', function() {
        $(document).off('xwiki:document:saveFailed.createRt');
        comment.val(previousComment);
        window.location.href = module.getEditorURL(window.location.href, info);
      });
      $(document).one('xwiki:document:saveFailed.createRt', function() {
        $(document).off('xwiki:document:saved.createRt');
        comment.val(previousComment);
        module.displayRequestErrorModal();
      });
    }
  },

  isEditorCompatible = function() {
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
  };

  // Detect fullscreen mode in CKeditor.
  // FIXME: Modify the CKEditor to fire the fullscreen events.
  new MutationObserver(function(mutations) {
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
    var $warning = $('<div></div>', {
      'class': 'xwiki-realtime-warning xwiki-realtime-box box warningmessage'
    }).insertAfter($after);
    scrollToBox($warning);
    $('<strong></strong>').text(Messages.conflictsWarning).appendTo($warning);
    $('<br/>').appendTo($warning);
    $('<span></span>').text(Messages.wsErrorConflicts).appendTo($warning);
    var editor = isEditorCompatible();
    if (!module.isRt && editor) {
      $('<br/>').appendTo($warning);
      $('<span></span>').html(Messages.conflictsWarningInfo).appendTo($warning);
      $('<a></a>', {
        href: getRTEditorURL(window.location.href, availableRt[editor].info)
      }).text(Messages.conflictsWarningInfoLink).appendTo($warning);
    } else if (module.isRt) {
      $('<br/>').appendTo($warning);
      $('<span></span>').text(Messages.conflictsWarningInfoRt).appendTo($warning);
    }
  },

  displayWsWarning = function(isError) {
    var $after = getBoxPosition();
    if (unload || warningVisible || !$after.length) {
      return;
    }
    warningVisible = true;
    var type = isError ? 'errormessage' : 'warningmessage';
    var $warning = $('<div></div>', {
      'class': 'xwiki-realtime-warning xwiki-realtime-box box ' + type
    }).insertAfter($after);
    scrollToBox($warning);
    $('<strong></strong>').text(Messages.wsError).appendTo($warning);
    $('<br/>').appendTo($warning);
    $('<span></span>').text(Messages.wsErrorInfo).appendTo($warning);
    if (module.isForced) {
      $('<br/>').appendTo($warning);
      $('<span></span>').text(Messages.wsErrorConflicts).appendTo($warning);
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
    var $warning = $('<div></div>', {
      'class': 'xwiki-realtime-connecting xwiki-realtime-box box infomessage'
    }).insertAfter($after);
    scrollToBox($warning);
    $('<strong></strong>').text(Messages.connectingBox).appendTo($warning);
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
    var $warning = $('<div></div>', {
      'class': 'xwiki-realtime-disconnected xwiki-realtime-box box errormessage'
    }).insertAfter($after);
    scrollToBox($warning);
    $('<strong></strong>').text(Messages.connectionLost).appendTo($warning);
    $('<br/>').appendTo($warning);
    $('<span></span>').text(Messages.connectionLostInfo).appendTo($warning);
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
    var getChannels = doc.getChannels.bind(doc, {
      path: doc.language + '/events/all',
      create: true
    });
    getChannels().then(channels => {
      var channelKey = channels[0].key;
      if (channelKey) {
        require(['netflux-client', 'xwiki-realtime-errorBox'], function(Netflux, ErrorBox) {
          var onError = function (error) {
            allRt.error = true;
            displayWsWarning();
            console.error(error);
          };
          // Connect to the websocket server.
          Netflux.connect(config.WebsocketURL).then(onNetfluxConnect.bind(null, config, getChannels, channelKey,
            onError), onError);
        });
      }
    });
  },

  onNetfluxConnect = function(config, getChannels, channelKey, onError, network) {
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
      getChannels().then(channels => network.join(channels[0].key)).then(onOpen, onError);
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

    $(document).on('xwiki:document:saved', function() {
      doc.reload();
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
    $('#mainEditArea').find('input[name="action_save"], input[name="action_saveandcontinue"]').each(function() {
      // TODO: Update this if we rewrite actionButtons.js to use jQuery instead of Prototype.js
      var clickListeners = Event.cache[this._prototypeUID || this.uniqueID]?.click?.map(entry => entry.handler);
      $(this).data('prototype.js/clickListeners', clickListeners);
      this.stopObserving('click');
    }).off('click.realtime-loader').on('click.realtime-loader', function(event) {
      event.preventDefault();
      event.stopPropagation();
      save(/* continue: */ $(this).attr('name') !== 'action_save');
    });

    previewButton.on('click', function(event) {
      if (previewButton.data('checked')) {
        previewButton.data('checked', false);
      } else {
        event.preventDefault();
        event.stopPropagation();
        save(false, true);
      }
    });
  },

  beforeLaunchRealtime = function(keys) {
    return new Promise((resolve, reject) => {
      if (module.isRt) {
        module.whenReady(function(wsAvailable) {
          module.isRt = wsAvailable;
          resolve(keys);
        });
      } else {
        resolve(keys);
      }
    });
  },

  parseKeyData = function(editorId, channels) {
    var keys = {};
    var eventsChannel = channels.getByPath([doc.language, 'events', '1.0']);
    var userDataChannel = channels.getByPath([doc.language, 'events', 'userdata']);
    var editorChannel = channels.getByPath([doc.language, 'content', editorId]);
    if (!eventsChannel || !userDataChannel || !editorChannel) {
      console.error('Missing document channels.');
    } else {
      keys = $.extend(keys, {
        [editorId]: editorChannel.key,
        [editorId + '_users']: editorChannel.userCount,
        events: eventsChannel.key,
        userdata: userDataChannel.key,
        active: {}
      });
      // Collect the other active real-time editing session (for the document content field) that are using a different
      // editor (e.g. the WYSIWYG editor).
      channels.getByPathPrefix([doc.language, 'content']).forEach(channel => {
        if (channel.userCount > 0 && JSON.stringify(channel.path) !== JSON.stringify(editorChannel.path)) {
          keys.active[channel.path.slice(2).join('/')] = channel;
        }
      });
    }
    return keys;
  };

  $.extend(module, {
    updateKeys: function(editorId) {
      return doc.getChannels({
        path: [
          doc.language + '/events/1.0',
          doc.language + '/events/userdata',
          doc.language + '/content/' + editorId,
          // Check also if the content is edited in real-time with other editors at the same time.
          doc.language + '/content/',
        ],
        create: true
      }).then(parseKeyData.bind(module, editorId));
    },

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
        setTimeout(module.whenReady.bind(module, callback), 100);
      }
    },

    onKeysChanged: function() {
      // The channel keys have changed while we were offline. We may not have the latest version of the document. The
      // safest solution is to reload.
      // FIXME: This module shouldn't know about CKEditor.
      sessionStorage.refreshCk = 'true';
      displayCustomModal(getReloadContent());
    },

    bootstrap: function(info) {
      return new Promise((resolve, reject) => {
        this.setAvailableRt(info);
        if (lock) {
          // Found a lock link. Check active sessions.
          this.checkSessions(info);
          reject();
        } else if (window.XWiki.editor === info.type) {
          // No lock and we are using the right editor. Start realtime.
          this.updateKeys(info.type).then(keys => {
            if (!keys[info.type] || !keys.events || !keys.userdata) {
              ErrorBox.show('unavailable');
              console.error('You are not allowed to create a new realtime session for that document.');
              reject();
            } else if (!Object.keys(keys.active).length || keys[info.type + '_users'] > 0) {
              resolve(keys);
            } else {
              // Let the user choose between joining the existing real-time session (with a different editor) or create
              // a new real-time session with the current editor.
              console.log('Join the existing realtime session or create a new one.');
              this.displayModal(info.type, Object.keys(keys.active), resolve.bind(null, keys), info);
            }
          });
        }
      }).then(beforeLaunchRealtime);
    }
  });

  $(fixActionButtonsInOfflineMode);
  joinAllUsers();

  return module;
});

