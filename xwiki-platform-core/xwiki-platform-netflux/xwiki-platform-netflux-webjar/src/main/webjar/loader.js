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
define('xwiki-rte-loader', ['jquery', 'xwiki-meta', 'xwiki-l10n!xwiki-rte-messages'], function($, xm, Messages) {
  'use strict';

  // TODO set the input config
  var context = JSON.parse($('#rte-context').text());
  var userAvatarUrl, ADVANCED_USER = true, DEMO_MODE, PRETTY_USER, WEBSOCKET_URL;

  var module = {
    messages: Messages
  };
  var LOCAL_STORAGE_DISALLOW = 'realtime-disallow';
  var USER = xm.userReference ? XWiki.Model.serialize(xm.userReference) : 'xwiki:XWiki.XWikiGuest';

  if (!WEBSOCKET_URL) {
    console.log("The provided websocketURL was empty, aborting attempt to" +
      "configure a realtime session.");
    return false;
  }
  if (!window.XWiki) {
    console.log("WARNING: XWiki js object not defined.");
    return false;
  }

  var getParameterByName = function (name, url) {
    if (!url) url = window.location.href;
    name = name.replace(/[\[\]]/g, "\\$&");
    var regex = new RegExp("[?&]" + name + "(=([^&#]*)|&|#|$)"),
      results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';
    return decodeURIComponent(results[2].replace(/\+/g, " "));
  };

  var documentReference = xm.documentReference ? xm.documentReference+'' : xm.wiki+':'+xm.document;

  var language, version, versionTime;
  var languageSelector = $('#realtime-frontend-getversion');
  if (languageSelector.length) {
    var json = JSON.parse($(languageSelector).html());
    language = json.locale;
    version = json.version;
    versionTime = json.time;
  }
  else {
    console.log('WARNING: Unable to get the language and version number from the UIExtension.' +
      ' Using the old method to get them.');
    languageSelector = document.querySelectorAll('form input[name="language"]');// [0].value;
    language = languageSelector[0] && languageSelector[0].value;
    version = $('html').data('xwiki-version');
  }
  if (!language || language === '') { language = 'default'; }

  var getDocLock = module.getDocLock = function () {
    var lockedBy = document.querySelectorAll('p.xwikimessage .wikilink a');
    var force = document.querySelectorAll('a[href*="force=1"][href*="/edit/"]');
    return (lockedBy.length && force.length) ? force[0] : false;
  };
  var isForced = module.isForced = (window.location.href.indexOf("force=1") >= 0);
  var isRt = module.isRt = (window.location.href.indexOf("realtime=1") >= 0);

  // used to insert some descriptive text before the lock link
  var prependLink = function (link, text) {
    var p = document.createElement('p');
    p.innerHTML = text;
    link.parentElement.insertBefore(p, link);
  };

  var getRTEditorURL = module.getEditorURL = function (href, info) {
    href = href.replace(/\?(.*)$/, function (all, args) {
      return '?' + args.split('&').filter(function (arg) {
        if (arg === 'editor=wysiwyg') { return false; }
        if (arg === 'editor=wiki') { return false; }
        if (arg === 'editor=object') { return false; }
        if (arg === 'editor=inline') { return false; }
        if (arg === 'sheet=CKEditor.EditSheet') { return false; }
        if (arg === 'force=1') { return false; }
        if (arg === 'realtime=1') { return false; }
        if (/^section=/.test(arg)) { return false; }
        else { return true; }
      }).join('&');
    });
    if(href.indexOf('?') === -1) { href += '?'; }
    href = href + info.href;
    return href;
  };

  var allRt = {
    state: false
  };

  var ajaxVersionUrl = "$xwiki.getURL('RTFrontend.Version','get')";
  var safeSave;
  var getConfig = module.getConfig = function () {
    // Username === <USER>-encoded(<PRETTY_USER>)%2d<random number>
    var userName = USER + '-' + encodeURIComponent(PRETTY_USER + '-').replace(/-/g, '%2d') +
      String(Math.random()).substring(2);

    return {
      saverConfig: {
        ajaxMergeUrl: "$xwiki.getURL('RTFrontend.Ajax','get')",
        ajaxVersionUrl,
        messages: Messages,
        language,
        version,
        safeSave
      },
      WebsocketURL: WEBSOCKET_URL,
      htmlConverterUrl: "$xwiki.getURL('RTFrontend.ConvertHTML','get')",
      userName,
      language,
      reference: documentReference,
      DEMO_MODE,
      LOCALSTORAGE_DISALLOW: LOCAL_STORAGE_DISALLOW,
      userAvatarURL,
      isAdvancedUser: ADVANCED_USER,
      network: allRt.network,
      abort: function () { module.onRealtimeAbort(); },
      onKeysChanged: function () { module.onKeysChanged(); },
      displayDisableModal: function (cb) { module.displayDisableModal(cb); },
    };
  };

  var checkSocket = function (callback) {
    var path = new XWiki.Document('GetKey', 'RTFrontend').getURL('get', 'outputSyntax=plain');
    var editorData = [{doc: documentReference, mod: language+'/content', editor: ''}];
    $.ajax({
      url: path,
      data: 'data='+encodeURIComponent(JSON.stringify(editorData)),
      type: 'POST'
    }).done(function(dataText) {
      var data = JSON.parse(dataText);
      var types = [],
        users = 0;
      if (data.error) { console.error("You don't have permissions to edit that document"); return; }
      var mods = data[documentReference];
      if (!mods) { console.error("Unknown error"); return; }
      var content = window.teste = mods[language+'/content'];
      if (!content) { console.error("Unknown error"); return; }
      for (var editor in content) {
        if(editor) {
          if (content[editor].users && content[editor].users > 0) {
            if (content[editor].users > users) {
              types.push(editor);
            }
          }
        }
      }
      if (types.length === 0) { callback(false); }
      else { callback(true, types); }
    });
  };

  var getKeys = module.getKeys = function(editorData, callback) {
    var path = new XWiki.Document('GetKey', 'RTFrontend').getURL('get', 'outputSyntax=plain');
    var dataList = [];

    $.ajax({
      url: path,
      data: 'data='+encodeURIComponent(JSON.stringify(editorData)),
      type: 'POST'
    }).done(function(dataText) {
      var data = JSON.parse(dataText);
      if(data.error) { console.error("You don't have permissions to edit that document"); return; }
      callback(data);
    });
  };

  var realtimeDisallowed = function() {
    return !!localStorage.getItem(LOCAL_STORAGE_DISALLOW);
  };
  var lock = getDocLock();

  var checkSession = module.checkSessions = function(info) {
    if (lock) {
      // found a lock link

      checkSocket(function (active, types) {
        // determine if it's a realtime session
        if (active) {
          console.log("Found an active realtime");
          //if (realtimeDisallowed()) {
            // do nothing
          //} else {
            displayModal(null, types, null, info);
          //}
        } else {
          console.log("Couldn't find an active realtime session");
          module.whenReady(function (rt) {
            if (rt) {
              displayModal(null, null, null, info);
            }
          });
        }
      });
    } else {
      // do nothing
    }
  };

  var displayModal = module.displayModal = function(createType, existingTypes, callback, info) {
    if (XWiki.widgets.RealtimeCreateModal) {
      return;
    }
    existingTypes = existingTypes || [];
    XWiki.widgets.RealtimeCreateModal = Class.create(XWiki.widgets.ModalPopup, {
      /** Default parameters can be added to the custom class. */
      defaultInteractionParameters : {
      },
      /** Constructor. Registers the key listener that pops up the dialog. */
      initialize : function($super, interactionParameters) {
        this.interactionParameters = Object.extend(Object.clone(this.defaultInteractionParameters),
          interactionParameters || {});
        // call constructor from ModalPopup with params content, shortcuts, options
        $super(
        this.createContent(this.interactionParameters, this),
          {
            "show" : { method : this.showDialog, keys : [] },
            "close" : { method : this.closeDialog, keys : ['Esc'] }
          },
          {
            displayCloseButton : true,
            verticalPosition : "center",
            backgroundColor : "#FFF",
            removeOnClose : true
          }
        );
        this.showDialog();
        this.setClass("realtime-create-session");
        $(document).trigger('insertButton');
      },
      /** Get the content of the modal dialog using ajax */
      createContent : function (data, modal) {
        var content = new Element('div', {'class': 'modal-popup'});

        // Create buttons container
        var classesButtons = '';
        existingTypes.forEach(function (elmt) { classesButtons += " realtime-button-"+elmt; });
        var buttonsDiv = new Element('div', {'class': 'realtime-buttons'+classesButtons});
        $(buttonsDiv).data('modal', modal);

        // Add text description
        if (existingTypes.length > 1) {
          content.insert(Messages['redirectDialog.pluralPrompt']);
        } else if (existingTypes.length === 1) {
          content.insert(Messages.sessionInProgress);
        } else {
          content.insert(Messages.requestASession);
        }
        content.insert(buttonsDiv);

        // Create new session button
        var br = new Element('br');
        if (createType) {
          var buttonCreate = new Element('button', {'class': 'btn btn-primary'});
          buttonCreate.insert(Messages.get('redirectDialog.create', info.name));
          $(buttonCreate).on('click', function() {
            callback();
            modal.closeDialog();
          });
          buttonsDiv.insert(br);
          buttonsDiv.insert(buttonCreate);
        }
        return content;
      }
    });
    return new XWiki.widgets.RealtimeCreateModal();
  };
  var cmi = 0;
  var displayCustomModal = function (content) {
    var i = cmi++;
    XWiki.widgets.RealtimeRequestModal = Class.create(XWiki.widgets.ModalPopup, {
      /** Default parameters can be added to the custom class. */
      defaultInteractionParameters : {},
      /** Constructor. Registers the key listener that pops up the dialog. */
      initialize : function($super, interactionParameters) {
        this.interactionParameters = Object.extend(Object.clone(this.defaultInteractionParameters),
          interactionParameters || {});
        // call constructor from ModalPopup with params content, shortcuts, options
        $super(
        this.createContent(this.interactionParameters, this),
          {
            "show" : { method : this.showDialog, keys : [] },
            //"close" : { method : this.closeDialog, keys : ['Esc'] }
          },
          {
            displayCloseButton : false,
            verticalPosition : "center",
            backgroundColor : "#FFF",
            removeOnClose : true
          }
        );
        this.showDialog();
      },
      /** Get the content of the modal dialog using ajax */
      createContent : function (data, modal) {
        $(content).find('button, input').click(function () {
          modal.closeDialog();
        });
        return content;
      }
    });
    return new XWiki.widgets.RealtimeRequestModal();
  };

  var getRequestContent = function (info, callback) {
    var content = new Element('div', {'class': 'modal-popup'});

    // Create buttons container
    var buttonsDiv = new Element('div', {'class': 'realtime-buttons'});

    // Add text description
    content.insert(Messages['requestDialog.prompt']);
    content.insert(buttonsDiv);

    // Create new session button
    var it;
    var buttonCreate = new Element('button', {'class': 'btn btn-primary'});
    buttonCreate.insert(Messages.get('requestDialog.create', info.name));
    $(buttonCreate).on('click', function() {
      clearInterval(it);
      try {
        callback(true);
      } catch (e) { console.error(e); }
    });
    var buttonReject = new Element('button', {'class': 'btn btn-danger'});
    buttonReject.insert(Messages['requestDialog.reject']);
    $(buttonReject).on('click', function() {
      clearInterval(it);
      try {
        callback(false);
      } catch (e) { console.error(e); }
    });
    var autoAccept = new Element('div');
    buttonsDiv.insert(new Element('br'));
    buttonsDiv.insert(buttonCreate);
    buttonsDiv.insert(buttonReject);
    buttonsDiv.insert(new Element('br'));
    buttonsDiv.insert(autoAccept);

    // Initialize auto-accept
    var i = 30;
    it = setInterval(function () {
      i--;
      autoAccept.innerHTML = "<br>" + Messages['requestDialog.autoAccept'] + i+"s";
      if (i <= 0) {
        $(buttonCreate).click();
        clearInterval(it);
        $(autoAccept).remove();
      }
    }, 1000);

    return content;
  };

  var getRejectContent = function (reason) {
    var content = new Element('div', {'class': 'modal-popup'});
    var buttonsDiv = new Element('div', {'class': 'realtime-buttons'});

    content.insert(reason === 'invalid' ? Messages['rejectDialog.invalid']
                      : Messages['rejectDialog.prompt']);
    content.insert(buttonsDiv);

    var br = new Element('br');
    var buttonCreate = new Element('button', {'class': 'btn btn-primary'});
    buttonCreate.insert(Messages['rejectDialog.ok']);
    buttonsDiv.insert(br);
    buttonsDiv.insert(buttonCreate);
    return content;
  };

  var getReloadContent = function () {
    var content = new Element('div', {'class': 'modal-popup'});
    var buttonsDiv = new Element('div', {'class': 'realtime-buttons'});

    content.insert(Messages['reloadDialog.prompt']);
    content.insert(buttonsDiv);

    var br = new Element('br');
    var buttonReload = new Element('button', {'class': 'btn btn-default'});
    buttonReload.insert(Messages['reloadDialog.reload']);
    $(buttonReload).on('click', function() {
      window.location.reload(true);
    });
    var buttonExit = new Element('button', {'class': 'btn btn-primary'});
    buttonExit.insert(Messages['reloadDialog.exit']);
    buttonsDiv.insert(br);
    buttonsDiv.insert(buttonExit);
    buttonsDiv.insert(buttonReload);
    return content;
  };

  var getVersionContent = function (old, oldTime, latest, latestTime) {
    var content = new Element('div', {'class': 'modal-popup'});
    var buttonsDiv = new Element('div', {'class': 'realtime-buttons'});

    var o = Number(oldTime);
    if (o) { o = " - " + new Date(o).toLocaleString(); }
    var l = Number(latestTime);
    if (l) { l = " - " + new Date(l).toLocaleString(); }


    content.insert(Messages['versionDialog.prompt']);
    content.insert(new Element('br'));
    content.insert(new Element('br'));
    content.insert(Messages['versionDialog.old'] + " " + old + o);
    content.insert(new Element('br'));
    content.insert(Messages['versionDialog.latest'] + " " + latest + l);
    content.insert(new Element('br'));
    var a = new Element('a');
    a.setAttribute('target', '_blank');
    a.setAttribute('href', XWiki.currentDocument.getURL("view", "viewer=changes&rev1="+old+"&rev2="+latest));
    a.innerHTML = Messages['versionDialog.link'];
    content.insert(a);
    content.insert(buttonsDiv);

    var br = new Element('br');
    var buttonCreate = new Element('button', {'class': 'btn btn-primary'});
    buttonCreate.insert(Messages['rejectDialog.ok']);
    buttonsDiv.insert(br);
    buttonsDiv.insert(buttonCreate);
    return content;
  };

  var getSaveErrorContent = function () {
    var content = new Element('div', {'class': 'modal-popup'});
    var buttonsDiv = new Element('div', {'class': 'realtime-buttons'});

    content.insert(Messages['requestDialog.saveError']);
    content.insert(new Element('br'));
    content.insert(buttonsDiv);

    var br = new Element('br');
    var buttonCreate = new Element('button', {'class': 'btn btn-primary'});
    buttonCreate.insert(Messages['rejectDialog.ok']);
    buttonsDiv.insert(br);
    buttonsDiv.insert(buttonCreate);
    return content;
  };
  module.displayRequestErrorModal = function () {
    displayCustomModal(getSaveErrorContent());
  };

  module.displayDisableModal = function (cb) {
    var content = new Element('div', {'class': 'modal-popup'});
    var buttonsDiv = new Element('div', {'class': 'realtime-buttons'});

    content.insert(Messages['disableDialog.prompt']);
    content.insert(buttonsDiv);

    var br = new Element('br');
    var buttonOk = new Element('button', {'class': 'btn btn-primary'});
    buttonOk.insert(Messages['disableDialog.ok']);
    $(buttonOk).on('click', function() {
      cb(true);
    });
    var buttonExit = new Element('button', {'class': 'btn btn-default'});
    $(buttonExit).on('click', function() {
      cb(false);
    });
    buttonExit.insert(Messages['disableDialog.exit']);
    buttonsDiv.insert(br);
    buttonsDiv.insert(buttonExit);
    buttonsDiv.insert(buttonOk);
    return void displayCustomModal(content);
  };

  var availableRt = {};
  module.setAvailableRt = function (type, info, cb) {
    availableRt[type] = {
      info: info,
      cb: cb
    };
  };

  var isEditorCompatible = function () {
    var ret;
    Object.keys(availableRt).some(function (type) {
      if ((availableRt[type].info.compatible || []).indexOf(XWiki.editor) !== -1) {
        ret = type;
        return true;
      }
    });
    return ret;
  };

  var unload = false;
  window.addEventListener('beforeunload', function () {
    unload = true;
    setTimeout(function () {
      unload = false;
    }, 5000);
  });

  var fullScreen = false;
  if ($('body').attr('data-maximized') || $('html').attr('style')) {
    fullScreen = true;
  }

  // Trigger a resize event to resize the editable area in fullscreen mode
  var resize = function () {
    // Trigger a resize event to resize the editable area in fullscreen mode
    var evt;
    if (typeof(Event) === "function") {
      evt = new Event('resize');
    } else {
      evt = document.createEvent('Event');
      evt.initEvent('resize', true, true);
    }
    setTimeout(function () {
      window.dispatchEvent(evt);
    });
  };

  // PLace the warning box at the correct position when in fullscreen mode
  var getBoxPosition = function () {
    return fullScreen ? $('.buttons') : $('#hierarchy');
  };
  var moveBox = function () {
    $('.xwiki-realtime-box').insertAfter(getBoxPosition()).show();
    $('.xwiki-realtime-box').css('margin-bottom', fullScreen ? '0' : '');
    resize();
  };

  // Detect fullscreen mode in ckeditor
  var observer = new MutationObserver(function (mutations) {
    mutations.forEach(function (m) {
      if (m.type === "attributes" && m.attributeName === "data-maximized") {
        var value = $('body').attr('data-maximized') === "true";
        fullScreen = value;
        moveBox();
      }
    });
  });
  observer.observe($('body')[0], {
    attributes: true
  });
  // Detect fullscreen mode in wiki editor
  document.observe('xwiki:fullscreen:exited', function () {
    fullScreen = false;
    moveBox();
  });
  document.observe('xwiki:fullscreen:entered', function () {
    fullScreen = true;
    moveBox();
  });

  // Scroll to the warning box when a message is displayed or updated
  var scrollToBox = function ($box) {
    moveBox();
    $box[0].scrollIntoView();
  };

  var warningVisible = false;
  var displayWarning = function () {
    if (unload) { return; }
    if (warningVisible) { return; }
    var $after = getBoxPosition();
    if (!$after.length) { return; }
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
  };
  var displayWsWarning = function (isError) {
    if (unload) { return; }
    if (warningVisible) { return; }
    var $after = getBoxPosition();
    if (!$after.length) { return; }
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
  };
  var hideWarning = function () {
    warningVisible = false;
    $('.xwiki-realtime-warning').remove();
    resize();
  };
  var connectingVisible = false;
  var displayConnecting = function () {
    if (unload) { return; }
    if (connectingVisible) { return; }
    var $after = getBoxPosition();
    if (!$after.length) { return; }
    connectingVisible = true;
    var $warning = $('<div>', {
      'class': 'xwiki-realtime-connecting xwiki-realtime-box box infomessage'
    }).insertAfter($after);
    scrollToBox($warning);
    $('<strong>').text(Messages.connectingBox).appendTo($warning);
  };
  var hideConnecting = function () {
    warningVisible = false;
    $('.xwiki-realtime-connecting').remove();
    resize();
  };
  var wsErrorVisible = false;
  var displayWsError = function () {
    if (unload) { return; }
    if (wsErrorVisible) { return; }
    var $after = getBoxPosition();
    if (!$after.length) { return; }
    wsErrorVisible = true;
    var $warning = $('<div>', {
      'class': 'xwiki-realtime-disconnected xwiki-realtime-box box errormessage'
    }).insertAfter($after);
    scrollToBox($warning);
    $('<strong>').text(Messages.connectionLost).appendTo($warning);
    $('<br>').appendTo($warning);
    $('<span>').text(Messages.connectionLostInfo).appendTo($warning);
  };
  var hideWsError = function () {
    wsErrorVisible = false;
    $('.xwiki-realtime-disconnected').remove();
    resize();
  };

  var tryParse = function (msg) {
    try {
      return JSON.parse(msg);
    } catch (e) {
      console.error("Cannot parse the message");
    }
  };

  // Protect against overriding content saved by someone else
  var saveButton = $('#mainEditArea').find('input[name="action_save"]');
  var saveButton2 = $('#mainEditArea').find('input[name="action_saveandcontinue"]');
  var previewButton = $('#mainEditArea').find('input[name="action_preview"]');


  var getDocumentStatistics = function () {
    var result = {
      document: $('html').data('xwiki-document'),
      language: language
    };
    return result;
  };
  var checkVersion = function (cb) {
    var url = ajaxVersionUrl + '?xpage=plain';
    var stats = getDocumentStatistics();
    $.ajax({
      url: url,
      method: 'POST',
      dataType: 'json',
      success: function (data) {
        cb(null, data);
      },
      data: stats,
      error: function (err) {
        cb(err, null);
      }
    });
  };
  var editForm = $('#edit').length ? $('#edit') : $('#inline');
  var shouldRedirect = false;
  var save = function (cont, preview) {
    if (preview) {
      previewButton.data('checked', true);
      previewButton.click();
      return;
    }
    shouldRedirect = !cont;
    document.fire('xwiki:actions:save', {
      form: editForm[0],
      continue: 1
    });
  };
  safeSave = function (cont, preview, old, cb) {
    old = old || {
      version: version,
      versionTime: versionTime
    };
    cb = cb || save;
    cb(cont, preview);
  };

  if (editForm.length && !module.isRt) {
    document.observe('xwiki:document:saved', function (e) {
      checkVersion(function (err, data) {
        if (err) { return; }
        if (data && data.version) {
          version = data.version;
          versionTime = data.versionTime;
        }
      });
      if (!shouldRedirect) { return; }
      // CkEditor tries to block the user from leaving the page with unsaved content.
      // Our save mechanism doesn't update the flag about unsaved content, so we have
      // to do it manually
      if (window.CKEDITOR) {
        try {
          CKEDITOR.instances.content.resetDirty();
        } catch (error) {
          // Ignore.
        }
      }
      window.location.href = window.XWiki.currentDocument.getURL('view');
    });
  }

  // If we're in offline edit mode, replace the save actions to check the version first
  if (editForm.length && !module.isRt && saveButton.length) {
    saveButton[0].stopObserving();
    saveButton.off('click').click(function (ev) {
      ev.preventDefault();
      ev.stopPropagation();
      safeSave(false);
    });
  }
  if (editForm.length && !module.isRt && saveButton2.length) {
    saveButton2[0].stopObserving();
    saveButton2.off('click').click(function (ev) {
      ev.preventDefault();
      ev.stopPropagation();
      safeSave(true);
    });
  }

  if (editForm.length && !module.isRt && previewButton.length) {
    $(function () {
      previewButton.click(function (ev) {
        if (!previewButton.data('checked')) {
          ev.preventDefault();
          ev.stopPropagation();
          safeSave(null, true);
          return;
        }
        previewButton.data('checked', false);
      });
    });
  }

  // Join a channel with all users on this page (realtime, offline AND lock page)
  // 1. This channel allows users on "lock" page to contact the editing user
  //  and request a collaborative session, using the `request` and `answer` commands
  // 2. It is also used to know if someone else is editing the document concurrently
  //  (at least 2 users with 1 editing offline). In this case, a warning message can
  //  be displayed.
  //  When someone starts editing the page, they send a `join` message with a
  //  boolean 'realtime'. When other users receive this message, they can tell if
  //  there is a risk of conflict and send a `displayWarning` command to the new user.
  var addMessageHandler = function () {
    if (!allRt.wChan) { return; }
    var wc = allRt.wChan;
    var network = allRt.network;
    // Handle leave events
    wc.on('leave', function () {
      hideWarning();
      wc.bcast(JSON.stringify({
        cmd: 'isSomeoneOffline'
      }));
    });
    // Handle incoming messages
    wc.on('message', function (msg, sender) {
      var data = tryParse(msg);
      if (!data) { return; }

      // Someone wants to create a realtime session. If the current user is editing
      // offline, display the modal
      if (data.cmd === "request") {
        if (lock) { return; }
        if (!data.type) { return; }
        var res = {
          cmd: "answer",
          type: data.type
        };
        // Make sure realtime is available for the requested editor
        if (!availableRt[data.type]) {
          res.state = -1;
          return void wc.bcast(JSON.stringify(res));
        }
        // Check if we're not already in realtime
        if (module.isRt) {
          res.state = 2;
          return void wc.bcast(JSON.stringify(res));
        }
        // Check if our current editor is realtime compatible
        // i.e. Object editor can't switch to wysiwyg
        if (!isEditorCompatible()) {
          res.state = 0;
          res.reason = 'invalid';
          return void wc.bcast(JSON.stringify(res));
        }
        // We're editing offline: display the modal
        var content = getRequestContent(availableRt[data.type].info, function (state) {
          if (state) {
            // Accepted: save and create the realtime session
            availableRt[data.type].cb();
          }
          res.state = state ? 1 : 0;
          return void wc.bcast(JSON.stringify(res));
        });
        setTimeout(function () {
          $('.xdialog-modal-container').css('z-index', '99999');
        });
        return void displayCustomModal(content);
      }
      // Receiving an answer to a realtime session request
      if (data.cmd === "answer") {
        if (!allRt.request) { return; }
        var state = data.state;
        allRt.request(state);
        if (state === -1) { return void ErrorBox.show('unavailable'); }
        if (state === 0) {
          // Rejected
          if ($('.realtime-buttons').length) {
            var m = $('.realtime-buttons').data('modal');
            if (m) {
              m.closeDialog();
            }
          }
          return void displayCustomModal(getRejectContent(data.reason));
        }
      }
      // Someone is joining the channel while we're editing, check if they
      // are using realtime and if we are
      if (data.cmd === "join") {
        if (lock) { return; }
        if (!data.realtime || !module.isRt) {
          displayWarning();
          network.sendto(sender, JSON.stringify({
            cmd: 'displayWarning'
          }));
        } else if (warningVisible) {
          hideWarning();
          wc.bcast(JSON.stringify({
            cmd: 'isSomeoneOffline'
          }));
        }
        return;
      }
      // Someone wants to know if we're editing offline to know if the warning
      // message should be displayed
      if (data.cmd === 'isSomeoneOffline') {
        if (lock || module.isRt) { return; }
        network.sendto(sender, JSON.stringify({
          cmd: 'displayWarning'
        }));
        return;
      }
    });
  };
  var joinAllUsers = function () {
    var config = getConfig();
    var keyData = [{doc: config.reference, mod: config.language+'/events', editor: "all"}];
    getKeys(keyData, function (d) {
      var doc = d && d[config.reference];
      var ev = doc && doc[config.language+'/events'];
      if (ev && ev.all) {
        var key = ev.all.key;
        var users = ev.all.users;
        require(['RTFrontend_netflux', 'RTFrontend_errorbox'], function (Netflux, ErrorBox) {
          var onError = function (err) {
            allRt.error = true;
            displayWsWarning();
            console.error(err);
          };
          // Connect to the websocket server
          Netflux.connect(config.WebsocketURL).then(function (network) {
            allRt.network = network;
            var onOpen = function (wc) {
              allRt.userList = wc.members;
              allRt.wChan = wc;
              addMessageHandler();
              // If we're in edit mode (not locked), tell the other users
              if (!lock) {
                return void wc.bcast(JSON.stringify({
                  cmd: 'join',
                  realtime: module.isRt
                }));
              }
            };
            // Join the "all" channel
            network.join(key).then(onOpen, onError);
            // Add direct messages handler
            network.on('message', function (msg, sender) {
              var data = tryParse(msg);
              if (!data) { return; }

              if (data.cmd === 'displayWarning') {
                displayWarning();
                return;
              }
            });
            // On reconnect, join the "all" channel again
            network.on('reconnect', function () {
              hideWarning();
              hideWsError();
              getKeys(keyData, function (d) {
                var doc = d && d[config.reference];
                var ev = doc && doc[config.language+'/events'];
                var key = ev.all.key;
                network.join(key).then(onOpen, onError);
              });
            });
            network.on('disconnect', function () {
              if (module.isRt) {
                displayWsError();
              } else {
                displayWsWarning();
              }
            });
          }, onError);
        });
      }
    });
  };
  module.requestRt = function (type, cb) {
    if (!allRt.wChan) {
      return void setTimeout(function () {
        module.requestRt(type, cb);
      }, 500);
    }
    if (allRt.userList.length === 1) { // no other user
      return void cb(false);
    }
    var data = JSON.stringify({
      cmd: 'request',
      type: 'wysiwyg'
    });
    allRt.request = cb;
    allRt.wChan.bcast(data);
  };
  module.onRealtimeAbort = function () {
    module.isRt = false;
    if (!allRt.wChan) { return; }
    allRt.wChan.bcast(JSON.stringify({
      cmd: 'join',
      realtime: module.isRt
    }));
  };
  joinAllUsers();

  module.whenReady = function (cb) {
    displayConnecting();
    // We want realtime enabled so we have to wait for the network to be ready
    if (allRt.network) {
      hideConnecting();
      return void cb(true);
    }
    if (allRt.error) {
      // Can't connect to network: hide the warning about "not being warned when some wants RT"
      // and display error about not being able to enable WS
      hideConnecting();
      hideWarning();
      displayWsWarning(true);
      return void cb(false);
    }
    setTimeout(function () {
      module.whenReady(cb);
    }, 100);
  };

  module.onKeysChanged = function () {
    // The channel keys have changed while we were offline.
    // We may not have the latest version of the document.
    // The safest solution is to reload.
    sessionStorage.refreshCk = "true";
    var content = getReloadContent();
    return void displayCustomModal(content);
  };

  return module;
});

