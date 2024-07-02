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
    console.error('The WebSocket URL is missing. Aborting attempt to configure a realtime session.');
    return;
  }

  let module = {
    isForced: window.location.href.indexOf('force=1') >= 0
  },

  // FIXME: The real-time JavaScript code is not loaded anymore on the "lock" page so this code is not really used. We
  // need to decide if we want to re-add the real-time JavaScript code on the lock page and how.
  getDocLock = module.getDocLock = function() {
    const lockedBy = document.querySelectorAll('p.xwikimessage .wikilink a');
    const force = document.querySelectorAll('a[href*="force=1"][href*="/edit/"]');
    return (lockedBy.length && force.length) ? force[0] : null;
  },

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

  // Returns a promise that resolves with the list of editor channels available for the specified field of the current
  // document in the current language.
  checkSocket = function(field) {
    const path = `${doc.language}/${field}/`;
    return doc.getChannels({path}).then(function(channels) {
      return channels.filter(channel => channel?.path?.length > 2 && channel?.userCount > 0)
        .map(channel => channel.path.slice(2).join('/'));
    });
  },

  lock = getDocLock();

  class RealtimeContext {
    constructor(info) {
      this.info = info;
      this.network = allRt.network;

      // Realtime enabled by default.
      this.realtimeEnabled = window.location.href.indexOf('realtime=false') < 0;
      this.webSocketURL = realtimeConfig.webSocketURL;

      const userReference = xm.userReference ? XWiki.Model.serialize(xm.userReference) : 'xwiki:XWiki.XWikiGuest';
      this.user = {
        // userId === <userReference>-encoded(<userName>)%2d<randomNumber>
        name: userReference + '-' + encodeURIComponent(realtimeConfig.user.name + '-').replace(/-/g, '%2d') +
          String(Math.random()).substring(2),
        avatarURL: realtimeConfig.user.avatarURL,
        advanced: realtimeConfig.user.advanced
      };

      RealtimeContext.instances = RealtimeContext.instances || {};
      RealtimeContext.instances[info.field] = this;
    }

    async updateChannels() {
      const channels = await doc.getChannels({
        path: [
          doc.language + '/events/1.0',
          doc.language + '/events/userdata',
          `${doc.language}/${this.info.field}/${this.info.type}`,
          // Check also if the field is edited in real-time with other editors at the same time.
          `${doc.language}/${this.info.field}/`,
        ],
        create: true
      });
      this.channels = this._parseChannels(channels);
      return this.channels;
    }

    _parseChannels(channels) {
      let keys = {};
      const eventsChannel = channels.getByPath([doc.language, 'events', '1.0']);
      const userDataChannel = channels.getByPath([doc.language, 'events', 'userdata']);
      const editorChannel = channels.getByPath([doc.language, this.info.field, this.info.type]);
      if (!eventsChannel || !userDataChannel || !editorChannel) {
        console.error('Missing document channels.');
      } else {
        keys = $.extend(keys, {
          [this.info.type]: editorChannel.key,
          [this.info.type + '_users']: editorChannel.userCount,
          events: eventsChannel.key,
          userdata: userDataChannel.key,
          active: {}
        });
        // Collect the other active real-time editing session (for the specified document field) that are using a
        // different editor (e.g. the WYSIWYG editor).
        channels.getByPathPrefix([doc.language, this.info.field]).forEach(channel => {
          if (channel.userCount > 0 && JSON.stringify(channel.path) !== JSON.stringify(editorChannel.path)) {
            keys.active[channel.path.slice(2).join('/')] = channel;
          }
        });
      }
      return keys;
    }

    /**
     * Notify the other users that we're editing in realtime or not.
     * 
     * @param {boolean} realtimeEnabled whether the user is editing in realtime or not
     */
    setRealtimeEnabled(realtimeEnabled) {
      this.realtimeEnabled = realtimeEnabled;
      // Notify the others only if we're still editing (only if this context is still associated with the edited field).
      if (RealtimeContext.instances[this.info.field] === this) {
        allRt.wChan?.bcast(JSON.stringify({
          cmd: 'join',
          field: this.info.field,
          realtime: realtimeEnabled
        }));
        updateWarning();
      }
    }

    destroy() {
      if (RealtimeContext.instances[this.info.field] === this) {
        delete RealtimeContext.instances[this.info.field];
        // Notify the others that we stopped editing this field.
        allRt.wChan?.bcast(JSON.stringify({
          cmd: 'leave',
          field: this.info.field
        }));
        updateWarning();
      }
    }

    displayReloadModal() {
      // The channel keys have changed while we were offline. We may not have the latest version of the document. The
      // safest solution is to reload.
      const content = createModalContent(Messages['reloadDialog.prompt'], Messages['reloadDialog.exit']);
      const buttonReload = $('<button class="btn btn-default"></button>').text(Messages['reloadDialog.reload']);
      buttonReload.on('click', window.location.reload.bind(window.location, true)).insertAfter(content.find('button'));
      displayCustomModal(content[0]);
    }

    displayDisableModal(callback) {
      const content = createModalContent(Messages['disableDialog.prompt'], Messages['disableDialog.ok']);
      const buttonOK = content.find('button').on('click', callback.bind(null, true));
      $('<button class="btn btn-default"></button>').text(Messages['disableDialog.exit']).insertBefore(buttonOK)
        .on('click', callback.bind(null, false));
      displayCustomModal(content[0]);
    }

    static getEditedFields() {
      return Object.keys(RealtimeContext.instances);
    }

    static getOfflineEditedFields(fields) {
      fields = fields || RealtimeContext.getEditedFields();
      return fields.filter(field => RealtimeContext.isOfflineEditedField(field));
    }

    static isOfflineEditedField(field) {
      return RealtimeContext.instances[field] && !RealtimeContext.instances[field].realtimeEnabled;
    }

    static getRealtimeEditedFields(fields) {
      fields = fields || RealtimeContext.getEditedFields();
      return fields.filter(field => RealtimeContext.instances[field]?.realtimeEnabled);
    }
  }

  module.checkSessions = function(info) {
    if (lock) {
      // Found an edit lock link.
      checkSocket(info.field).then(types => {
        // Determine if it's a realtime session.
        if (types.length) {
          console.debug('Found an active realtime session.');
          displayModal(null, types, null, info);
        } else {
          console.debug("Couldn't find an active realtime session.");
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

  let displayModal = module.displayModal = function(createType, existingTypes, callback, info) {
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
        let message = Messages.requestASession;
        if (existingTypes.length > 1) {
          message = Messages['redirectDialog.pluralPrompt'];
        } else if (existingTypes.length === 1) {
          message = Messages.sessionInProgress;
        }

        const content = createModalContent(message, Messages.get('redirectDialog.create', info.name));
        const classesButtons = existingTypes.map(type => 'realtime-button-' + type).join(' ');
        const buttonsDiv = content.find('.realtime-buttons').addClass(classesButtons).data('modal', this);
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
    const content = $(
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
    const content = createModalContent(Messages['requestDialog.prompt'],
      Messages.get('requestDialog.create', info.name));

    // Initialize auto-create
    const autoCreate = $('<p></p>').appendTo(content);
    let i = 30;
    const interval = setInterval(function() {
      i--;
      autoCreate.text(Messages.get('requestDialog.autoCreate', i));
      if (i <= 0) {
        buttonCreate.click();
        clearInterval(interval);
        autoCreate.remove();
      }
    }, 1000);

    const buttonCreate = content.find('button').on('click', function() {
      clearInterval(interval);
      try {
        callback(true);
      } catch (e) {
        console.error(e);
      }
    });

    const buttonReject = $('<button class="btn btn-danger"></button>').text(Messages['requestDialog.reject']);
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

  getSaveErrorContent = function() {
    return createModalContent(Messages['requestDialog.saveError'], Messages['rejectDialog.ok'])[0];
  };

  module.displayRequestErrorModal = function() {
    displayCustomModal(getSaveErrorContent());
  };

  const availableRt = {};
  module.setAvailableRt = function(info) {
    availableRt[info.type] = {
      info,
      cb: createRt.bind(null, info)
    };
  };

  let createRtCalled = false,
  createRt = function(info) {
    if (createRtCalled) {
      return;
    }
    createRtCalled = true;
    const $saveButton = $('#mainEditArea').find('input[name="action_saveandcontinue"]');
    if ($saveButton.length) {
      const comment = $('#commentinput');
      const previousComment = comment.val();
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

  getCompatibleEditor = function(type) {
    return Object.keys(availableRt).find((availableType) => {
      return (availableRt[availableType].info.compatible || []).indexOf(type) !== -1;
    });
  },

  unload = false;
  window.addEventListener('beforeunload', function() {
    unload = true;
    setTimeout(function() {
      unload = false;
    }, 5000);
  });

  let fullScreen = !!($('body').attr('data-maximized') || $('html').attr('style')),

  // Trigger a resize event to resize the editable area in fullscreen mode.
  resize = function() {
    setTimeout(function() {
      window.dispatchEvent(new Event('resize'));
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
  let scrollToBox = function($box) {
    moveBox();
    $box[0].scrollIntoView();
  },

  warningVisible = false,
  displayWarning = function(field) {
    const $after = getBoxPosition();
    if (unload || warningVisible || !$after.length) {
      return;
    }
    warningVisible = true;
    const $warning = $('<div></div>', {
      'class': 'xwiki-realtime-warning xwiki-realtime-box box warningmessage'
    }).insertAfter($after);
    scrollToBox($warning);
    $('<strong></strong>').text(Messages.conflictsWarning).appendTo($warning);
    $('<br/>').appendTo($warning);
    $('<span></span>').text(Messages.wsErrorConflicts).appendTo($warning);
    const realtimeContext = RealtimeContext.instances[field];
    const compatibleEditor = getCompatibleEditor(realtimeContext?.info?.type);
    if (realtimeContext && !realtimeContext.realtimeEnabled && compatibleEditor) {
      $('<br/>').appendTo($warning);
      // The parameter is the edit link but we can't inject it directly because we need to escape the HTML.
      let suggestion = Messages.get('conflictsWarningSuggestion', '__0__');
      // The translation message shouldn't contain HTML.
      suggestion = $('<div></div>').text(suggestion).html();
      // The link label shouldn't contain HTML.
      const link = $('<a></a>', {
        href: getRTEditorURL(window.location.href, availableRt[compatibleEditor].info)
      }).text(Messages.conflictsWarningInfoLink).prop('outerHTML');
      // Inject the link and append the suggestion.
      $warning.append(suggestion.replace('__0__', link));
    } else if (realtimeContext?.realtimeEnabled) {
      $('<br/>').appendTo($warning);
      $('<span></span>').text(Messages.conflictsWarningInfoRt).appendTo($warning);
    }
  },

  displayWsWarning = function(isError) {
    const $after = getBoxPosition();
    if (unload || warningVisible || !$after.length) {
      return;
    }
    warningVisible = true;
    const type = isError ? 'errormessage' : 'warningmessage';
    const $warning = $('<div></div>', {
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

  /**
   * Hides the warning message and asks the other users is they are editing offline (i.e. outside the realtime session),
   * in which case the warning message is displayed again (see onIsSomeoneOfflineMessage).
   */
  updateWarning = function() {
    if (warningVisible) {
      hideWarning();
      // Check if someone is editing offline any of the document fields we're editing.
      const editedFields = RealtimeContext.getEditedFields();
      if (editedFields.length) {
        allRt.wChan?.bcast(JSON.stringify({
          cmd: 'isSomeoneOffline',
          fields: editedFields
        }));
      }
    }
  },

  connectingVisible = false,
  displayConnecting = function() {
    const $after = getBoxPosition();
    if (unload || connectingVisible || !$after.length) {
      return;
    }
    connectingVisible = true;
    const $warning = $('<div></div>', {
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
    const $after = getBoxPosition();
    if (unload || wsErrorVisible || !$after.length) {
      return;
    }
    wsErrorVisible = true;
    const $warning = $('<div></div>', {
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

  tryParse = function(message) {
    try {
      return JSON.parse(message);
    } catch (error) {
      console.error('Cannot parse the message.', {message, error});
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
    const channel = allRt.wChan;
    const network = allRt.network;
    // Whenever someone leaves the edit mode, check if the warning message is still needed.
    channel.on('leave', updateWarning);
    // Handle incoming messages.
    channel.on('message', function(msg, sender) {
      const data = tryParse(msg);
      switch(data?.cmd) {
        // Someone wants to create a realtime session. If the current user is editing offline, display the modal.
        case 'request': return onRequestMessage(data, channel);
        // Receiving an answer to a realtime session request.
        case 'answer': return onAnswerMessage(data);
        // Someone is joining the channel while we're editing, check if they are using realtime and if we are.
        case 'join': return onJoinMessage(data, sender, network);
        // Someone has stopped editing a document field. Check if the warning message is still needed.
        case 'leave': return updateWarning();
        // Someone wants to know if we're editing offline to know if the warning message should be displayed.
        case 'isSomeoneOffline': return onIsSomeoneOfflineMessage(data, sender, network);
      }
    });
  },

  onRequestMessage = function(data, channel) {
    if (lock || !data.field || !data.type) {
      return;
    }
    const response = {
      cmd: 'answer',
      field: data.field,
      type: data.type
    };
    // Make sure realtime is available for the requested editor.
    if (!availableRt[data.type]) {
      response.state = -1;
      channel.bcast(JSON.stringify(response));
    // Check if we're not already in realtime.
    } else if (RealtimeContext.instances[data.field]?.realtimeEnabled) {
      response.state = 2;
      channel.bcast(JSON.stringify(response));
    // Check if our current editor is realtime compatible, i.e. Object editor can't switch to WYSIWYG.
    } else if (!getCompatibleEditor(data.type)) {
      response.state = 0;
      response.reason = 'invalid';
      channel.bcast(JSON.stringify(response));
    // We're editing offline: display the modal.
    } else {
      const content = getRequestContent(availableRt[data.type].info, function(state) {
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
    const state = data.state;
    allRt.request(state);
    if (state === -1) {
      ErrorBox.show('unavailable');
    } else if (state === 0) {
      // Rejected
      $('.realtime-buttons').data('modal')?.closeDialog();
      displayCustomModal(getRejectContent(data.reason));
    }
  },

  onJoinMessage = function(data, sender, network) {
    const realtimeContext = RealtimeContext.instances[data.field];
    if (lock || !realtimeContext) {
      return;
    // Someone has started editing the same document field as us.
    } else if (!data.realtime || !realtimeContext.realtimeEnabled) {
      // One of us is editing offline (outside the realtime session).
      displayWarning(data.field);
      network.sendto(sender, JSON.stringify({
        cmd: 'displayWarning',
        fields: [data.field]
      }));
    } else {
      // We're both editing in realtime. Maybe we need to hide the warning message.
      updateWarning();
    }
  },

  onIsSomeoneOfflineMessage = function(data, sender, network) {
    const offlineFields = RealtimeContext.getOfflineEditedFields(data.fields);
    if (!lock && offlineFields.length) {
      network.sendto(sender, JSON.stringify({
        cmd: 'displayWarning',
        fields: offlineFields
      }));
    }
  },

  getAllUsersChannel = function() {
    return doc.getChannels({
      path: doc.language + '/events/all',
      create: true
    }).then(channels => channels[0]);
  },

  joinAllUsers = async function() {
    const channelInfo = await getAllUsersChannel();
    if (!channelInfo?.key) {
      // We can't join the all users (events) channel if we don't know its key.
      return;
    }
    if (!allRt.network) {
      allRt.network = await connectToNetfluxWebSocket();
    }
    await onOpen(channelInfo);
  },

  connectToNetfluxWebSocket = async function() {
    const Netflux = await new Promise((resolve, reject) => {
      require(['netflux-client'], resolve, reject);
    });
    const network = await Netflux.connect(realtimeConfig.webSocketURL);
    // Add direct messages handler.
    network.on('message', msg => {
      const data = tryParse(msg);
      if (data?.cmd === 'displayWarning') {
        // Display the warning message only for the fields that we're still editing.
        data.fields.filter(field => RealtimeContext.instances[field]).forEach(displayWarning);
      }
    });
    // On reconnect, join the "all" channel again.
    network.on('reconnect', () => {
      hideWarning();
      hideWsError();
      module.ready = joinAllUsers();
    });
    network.on('disconnect', () => {
      if (RealtimeContext.getRealtimeEditedFields().length) {
        displayWsError();
      } else if (Object.keys(RealtimeContext.instances).length) {
        displayWsWarning();
      }
    });
    return network;
  },

  onOpen = async channelInfo => {
    const channel = await allRt.network.join(channelInfo.key);
    allRt.userList = channel.members;
    allRt.wChan = channel;
    allRt.channelInfo = channelInfo;
    addMessageHandler();
  },

  maybeRejoinAllUsers = () => {
    if (allRt.channelInfo && allRt.channelInfo.path[0] !== doc.language) {
      // The document language has changed since we joined the all users channel (e.g. because the user has switched
      // from editing the original document translation to editing another translation, without reloading the web page,
      // from inplace editing). We need to join the all users channel associated with the new document language.
      // Leave the current channel first.
      allRt.wChan.leave('Switched to a different document translation');
      // Then join the new channel.
      module.ready = joinAllUsers();
    }
  },

  onError = error => {
    allRt.error = true;
    displayWsWarning();
    console.error(error);
  },

  beforeLaunchRealtime = function(realtimeContext) {
    return new Promise(resolve => {
      if (realtimeContext.realtimeEnabled) {
        module.whenReady(function(wsAvailable) {
          realtimeContext.realtimeEnabled = wsAvailable;
          realtimeContext.network = allRt.network;
          resolve(realtimeContext);
        });
      } else {
        resolve(realtimeContext);
      }
    });
  };

  $.extend(module, {
    requestRt: function({field, type, callback}) {
      if (!allRt.wChan) {
        setTimeout(function () {
          module.requestRt({field, type, callback});
        }, 500);
      } else if (allRt.userList.length === 1) {
        // No other user.
        callback(false);
      } else {
        allRt.request = callback;
        allRt.wChan.bcast(JSON.stringify({
          cmd: 'request',
          field,
          type
        }));
      }
    },

    whenReady: async function(callback) {
      displayConnecting();
      maybeRejoinAllUsers();
      try {
        await module.ready;
        callback(true);
      } catch (error) {
        hideWarning();
        onError(error);
        callback(false);
      } finally {
        hideConnecting();
      }
    },

    bootstrap: async function(info) {
      this.setAvailableRt(info);
      if (lock) {
        // Found a lock link. Check active sessions.
        this.checkSessions(info);
        throw new Error('Lock detected');
      } else if (window.XWiki.editor === info.type) {
        // No lock and we are using the right editor. Start realtime.
        const realtimeContext = new RealtimeContext(info);
        const keys = await realtimeContext.updateChannels();
        if (!keys[info.type] || !keys.events || !keys.userdata) {
          ErrorBox.show('unavailable');
          const error = new Error('You are not allowed to create a new realtime session for that document.');
          console.error(error);
          throw error;
        } else if (Object.keys(keys.active).length && !keys[info.type + '_users']) {
          // Let the user choose between joining the existing real-time session (with a different editor) or create
          // a new real-time session with the current editor.
          console.debug('Join the existing realtime session or create a new one.');
          await new Promise(resolve => {
            displayModal(info.type, Object.keys(keys.active), resolve, info);
          });
        }
        return await beforeLaunchRealtime(realtimeContext);
      }
    },

    ready: joinAllUsers()
  });

  return module;
});

