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
  'xwiki-events-bridge'
], function(
  /* jshint maxparams:false */
  $, xm, realtimeConfig, doc, Messages
) {
  'use strict';

  if (!realtimeConfig.webSocketURL) {
    console.error('The WebSocket URL is missing. Aborting attempt to configure a realtime session.');
    return;
  }

  let module = {
    isForced: globalThis.location.href.includes('force=1'),
  },

  allRt = {
    state: false
  };

  class RealtimeContext {
    constructor(info) {
      this.info = info;
      this.network = allRt.network;

      // Realtime enabled by default.
      this.realtimeEnabled = !globalThis.location.href.includes('realtime=false');
      this.webSocketURL = realtimeConfig.webSocketURL;

      const userReference = xm.userReference ? XWiki.Model.serialize(xm.userReference) : 'xwiki:XWiki.XWikiGuest';
      this.user = {
        // sessionId === <userReference>-encoded(<userName>)%2d<randomNumber>
        sessionId: userReference + '-' + encodeURIComponent(realtimeConfig.user.name + '-').replaceAll('-', '%2d') +
          String(Math.random()).substring(2),
        name: realtimeConfig.user.name,
        reference: userReference,
        avatar: realtimeConfig.user.avatarURL
      };

      RealtimeContext.instances = RealtimeContext.instances || {};
      RealtimeContext.instances[info.field] = this;
    }

    async updateChannels() {
      const channels = await doc.getChannels({
        path: [
          `translations/${doc.language}/saver`,
          `translations/${doc.language}/userData`,
          `translations/${doc.language}/fields/${this.info.field}/editors/${this.info.type}`,
          // Check also if the field is edited in real-time with other editors at the same time.
          `translations/${doc.language}/fields/${this.info.field}/editors/`,
        ],
        create: true
      });
      this.channels = this._parseChannels(channels);
      return this.channels;
    }

    _parseChannels(channels) {
      let keys = {};
      const saverChannel = channels.getByPath(['translations', doc.language, 'saver']);
      const userDataChannel = channels.getByPath(['translations', doc.language, 'userData']);
      const editorChannel = channels.getByPath(['translations', doc.language, 'fields', this.info.field, 'editors',
        this.info.type]);
      if (!saverChannel || !userDataChannel || !editorChannel) {
        console.error('Missing document channels.');
      } else {
        keys = $.extend(keys, {
          [this.info.type]: editorChannel.key,
          [this.info.type + '_users']: editorChannel.userCount,
          saver: saverChannel.key,
          userData: userDataChannel.key,
          active: {}
        });
        // Collect the other active real-time editing session (for the specified document field) that are using a
        // different editor (e.g. the WYSIWYG editor).
        channels.getByPathPrefix([
          'translations', doc.language, 'fields', this.info.field, 'editors'
        ]).forEach(channel => {
          if (channel.userCount > 0 && JSON.stringify(channel.path) !== JSON.stringify(editorChannel.path)) {
            keys.active[channel.path.slice(5).join('/')] = channel;
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
        RealtimeContext.detectConcurrentEditing();
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
        RealtimeContext.detectConcurrentEditing();
      }
    }

    setConcurrentEditing(concurrentEditing) {
      const show = !this.concurrentEditing;
      this.concurrentEditing = concurrentEditing;
      $('.realtime-warning').popover('destroy').remove();
      if (concurrentEditing) {
        this._createConcurrentEditingWarning(show);
      }
    }

    _createConcurrentEditingWarning(show) {
      const template = document.querySelector('template#realtime-warning');
      const popoverToggle = template.content.querySelector('.realtime-warning-' +
        (this.realtimeEnabled ? 'connected' : 'disconnected')).cloneNode(true);
      let toolbar = document.querySelector(
        this.realtimeEnabled ? '.realtime-edit-toolbar-left' : '.buttons:not(.realtime-edit-toolbar)'
      );
      toolbar.append(popoverToggle);
      $(popoverToggle).popover();
      if (show) {
        this._showConcurrentEditingWarning(popoverToggle);
      }
    }

    _showConcurrentEditingWarning(popoverToggle) {
      // The warning message is shown when leaving collaboration or when switching to Source mode, in which case there
      // is a toolbar switch, so it's best to wait a bit for the toolbar layout to settle otherwise the position of the
      // popover won't match the position of the toggle button. Moreover, we want to prevent quickly showing and hiding
      // the popover in cases where the user is disconnected for a short while.
      setTimeout(() => {
        $(popoverToggle).popover('show');
      }, 1000);
      // Auto-hide the warning message after 10 seconds.
      const autoHideTimeout = setTimeout(() => {
        $(popoverToggle).popover('hide');
      }, 10000);
      $(popoverToggle).one('hide.bs.popover', () => {
        clearTimeout(autoHideTimeout);
      });
    }

    /**
     * Hides the concurrent editing warning messages and asks the other users is they are editing offline (i.e. outside
     * the realtime collaboration session), in which case the warning message is displayed again (see
     * onIsSomeoneOfflineMessage).
     */
    static detectConcurrentEditing() {
      if (Object.values(RealtimeContext.instances).some(instance => instance.concurrentEditing)) {
        RealtimeContext.clearConcurrentEditing();
        // Check if someone is editing offline any of the document fields we're editing.
        const editedFields = RealtimeContext.getEditedFields();
        if (editedFields.length) {
          allRt.wChan?.bcast(JSON.stringify({
            cmd: 'isSomeoneOffline',
            fields: editedFields
          }));
        }
      }
    }

    static clearConcurrentEditing() {
      Object.values(RealtimeContext.instances).forEach(instance => instance.setConcurrentEditing(false));
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

  let displayCustomModal = function(content) {
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
        globalThis.location.href = module.getEditorURL(globalThis.location.href, info);
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
      return (availableRt[availableType].info.compatible || []).includes(type);
    });
  },

  tryParse = function(message) {
    try {
      return JSON.parse(message);
    } catch (error) {
      console.error('Cannot parse the message.', {message, error});
    }
  },

  // Join a channel with all users on this page:
  // 1. This channel allows users to contact the editing user and request a collaborative session, using the `request`
  //    and `answer` commands
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
    // Whenever someone leaves the edit mode, check if the concurrent editing warning message is still needed.
    channel.on('leave', RealtimeContext.detectConcurrentEditing);
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
        case 'leave': return RealtimeContext.detectConcurrentEditing();
        // Someone wants to know if we're editing offline to know if the warning message should be displayed.
        case 'isSomeoneOffline': return onIsSomeoneOfflineMessage(data, sender, network);
      }
    });
  },

  onRequestMessage = function(data, channel) {
    if (!data.field || !data.type) {
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
    } else if (getCompatibleEditor(data.type)) {
      // We're editing offline: display the modal.
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
    } else {
      response.state = 0;
      response.reason = 'invalid';
      channel.bcast(JSON.stringify(response));
    }
  },

  onAnswerMessage = function(data) {
    if (!allRt.request) {
      return;
    }
    const state = data.state;
    allRt.request(state);
    if (state === -1) {
      console.debug('We lost the connection to the editing session.');
    } else if (state === 0) {
      // Rejected
      $('.realtime-buttons').data('modal')?.closeDialog();
      displayCustomModal(getRejectContent(data.reason));
    }
  },

  onJoinMessage = function(data, sender, network) {
    const realtimeContext = RealtimeContext.instances[data.field];
    if (!realtimeContext) {
      return;
    // Someone has started editing the same document field as us.
    } else if (!data.realtime || !realtimeContext.realtimeEnabled) {
      // One of us is editing offline (outside the realtime session).
      realtimeContext.setConcurrentEditing(true);
      network.sendto(sender, JSON.stringify({
        cmd: 'displayWarning',
        fields: [data.field]
      }));
    } else {
      // We're both editing in realtime. Maybe we need to hide the concurrent editing warning message.
      RealtimeContext.detectConcurrentEditing();
    }
  },

  onIsSomeoneOfflineMessage = function(data, sender, network) {
    const offlineFields = RealtimeContext.getOfflineEditedFields(data.fields);
    if (offlineFields.length) {
      network.sendto(sender, JSON.stringify({
        cmd: 'displayWarning',
        fields: offlineFields
      }));
    }
  },

  getAllUsersChannel = async function() {
    const channels = await doc.getChannels({
      path: `translations/${doc.language}/loader`,
      create: true
    });
    if (channels.length) {
      return channels[0];
    } else {
      // We can't create / access the All Users channel.
      throw new Error(Messages.forbidden);
    }
  },

  joinAllUsers = async function() {
    if (!allRt.network) {
      allRt.network = await connectToNetfluxWebSocket();
    }

    const channelInfo = await getAllUsersChannel();
    if (channelInfo.key !== allRt.channelInfo?.key) {
      // Either we haven't joined the All Users channel yet, or the key has changed. The later can happen when the
      // current document language is changed (e.g. because the user has switched from editing the original document
      // translation to editing another translation, without reloading the web page, from inplace editing). In this
      // case we have to leave the current channel before joining the new one.
      allRt.wChan?.leave('Switching to a different All Users channel');
      await onOpen(channelInfo);
    }
  },

  createNetwork = async function() {
    const Netflux = await new Promise((resolve, reject) => {
      require(['netflux-client'], resolve, reject);
    });
    // We pass a custom WebSocket factory to Netflux in order to avoid repeated connection attempts if the first one
    // fails, which is often a sign that the HTTP proxy in front on XWiki is blocking the WebSocket requests or is badly
    // forwarding them. We want to reconnect only if the first connection attempt succeeds (e.g. if we lose the
    // connection while editing in realtime). The problem is that the Netflux client never stops trying to connect and
    // we want to fallback to editing alone if the WebSocket connection is not available from the start.
    let failed = false;
    const network = await Netflux.connect(realtimeConfig.webSocketURL, (url) => {
      const webSocket = new WebSocket(url);
      const maybeDisconnect = () => {
        if (!allRt.wChan) {
          // The WebSocket connection was closed before we could join the All Users channel, which is a sign that the
          // server-side is lacking proper support for WebSockets.
          failed = true;
          // HACK: Make Netflux client think we have connected successfully and received the identity in order to
          // resolve the network promise. There's no other way to force settling the promise unfortunately.
          webSocket._onident();
        }
      };
      webSocket.addEventListener('error', maybeDisconnect);
      webSocket.addEventListener('close', maybeDisconnect);
      return webSocket;
    });
    if (failed) {
      network.disconnect();
      throw new Error("Failed to connect to the Netflux WebSocket. Make sure the servlet container has WebSocket " +
        "support enabled and, if you're using an HTTP proxy, that it properly forwards WebSocket requests.");
    }
    return network;
  },

  connectToNetfluxWebSocket = async function() {
    const network = await createNetwork();
    // Add direct messages handler.
    network.on('message', msg => {
      const data = tryParse(msg);
      if (data?.cmd === 'displayWarning') {
        // Some fields are being edited concurrently. Warn about merge conflicts, but only for the fields that we're
        // still editing.
        data.fields.forEach(field => RealtimeContext.instances[field]?.setConcurrentEditing(true));
      }
    });
    // On reconnect, join the "all" channel again.
    network.on('reconnect', async () => {
      try {
        // Hide the concurrent editing warnings.
        RealtimeContext.clearConcurrentEditing();
        await module.toBeReady();
      } catch (error) {
        console.error(error);
      }
    });
    let expectedDisconnect = false;
    window.addEventListener('beforeunload', () => {
      expectedDisconnect = true;
    });
    network.on('disconnect', () => {
      if (!expectedDisconnect && Object.keys(RealtimeContext.instances).length) {
        console.debug('We lost the connection to the editing session.');
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
  };

  $.extend(module, {
    requestRt: function({field, type, callback}) {
      if (!allRt.wChan) {
        setTimeout(() => {
          this.requestRt({field, type, callback});
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

    getEditorURL: function(href, info) {
      const currentURL = new URL(href);
      const baseURL = new URL("?", currentURL).toString();
      const params = new URLSearchParams(currentURL.search);
      ['editor', 'section', 'force', 'realtime'].forEach(param => params.delete(param));
      const hash = info.href.includes('#') ? '' : currentURL.hash;
      return baseURL + params.toString() + info.href + hash;
    },

    bootstrap: async function(info) {
      this.setAvailableRt(info);
      // We currently support editing in realtime only the content field (using either the Wiki editor, the standalone
      // WYSIWYG editor or the Inplace editor).
      if (info.field === 'content' && globalThis.XWiki.editor === info.type) {
        // The current editor is supported. Check if we can join a realtime session.
        const realtimeContext = new RealtimeContext(info);
        const keys = await realtimeContext.updateChannels();
        if (!keys[info.type] || !keys.saver || !keys.userData) {
          // We can't create / access the document Netflux channels required for realtime editing.
          throw new Error(Messages.forbidden);
        } else if (Object.keys(keys.active).length && !keys[info.type + '_users']) {
          // There is an active real-time editing session for the document content but it uses a different editor. We
          // don't want to activate another editing session for the current editor because the auto-save from each
          // session will create a lot of merge conflicts.
          throw new Error(`The current editor [${info.type}] is not compatible with the existing realtime ` +
            `editing session that uses the ${Object.keys(keys.active)} editor.`);
        }
        return await this.beforeLaunchRealtime(realtimeContext);
      }
    },

    beforeLaunchRealtime: async function(realtimeContext) {
      if (realtimeContext.realtimeEnabled) {
        try {
          await this.toBeReady();
        } finally {
          realtimeContext.realtimeEnabled = !!allRt.wChan;
          realtimeContext.network = allRt.network;
        }
      }

      return realtimeContext;
    },

    toBeReady: function() {
      this.ready = this.ready.then(joinAllUsers);
      return this.ready;
    },

    ready: joinAllUsers()
  });

  return module;
});

