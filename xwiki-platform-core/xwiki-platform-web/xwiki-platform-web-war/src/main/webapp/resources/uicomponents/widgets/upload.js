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
var XWiki = (function(XWiki) {
  // Only enable this widget if the needed JS APIs are present
  if (typeof (File) === 'undefined' || typeof (FormData) === 'undefined' || typeof (XMLHttpRequest) === 'undefined') {return XWiki;}

  /**
   * Small utility functions.
   */
  var UploadUtils = {
    /**
     * Convert seconds to human readable time format.
     *
     * @param seconds the number of seconds to convert
     * @return a string in the format "HH:mm:ss"
     */
    secondsToTime : function (seconds) {
      var hr = Math.floor(seconds / 3600);
      var min = Math.floor((seconds - (hr * 3600))/60);
      var sec = Math.floor(seconds - (hr * 3600) -  (min * 60));

      if (hr < 10) {hr = "0" + hr; }
      if (min < 10) {min = "0" + min;}
      if (sec < 10) {sec = "0" + sec;}
      return hr + ':' + min + ':' + sec;
    },

    /**
     * Convert bytes to human readable size format.
     *
     * @param bytes the number of bytes to convert
     * @return a string representing the size in bytes, kilobytes or megabytes, with 1 decimal precision
     */
    bytesToSize : function (bytes) {
      var sizes = ['b', 'Kb', 'Mb'];
      if (bytes == 0) return 'n/a';
      var i = parseInt(Math.floor(Math.log(bytes) / Math.log(1024)));
      if (i >= sizes.length) {
        i = sizes.length - 1;
      }
      return (bytes / Math.pow(1024, i)).toFixed(1) + ' ' + sizes[i];
    },

    /**
     * Create and return a new DIV element.
     *
     * @param cssClass a classname to set on the element
     * @param content optional content that should be inserted in the element
     * @return the newly created element
     */
    createDiv : function (cssClass, content) {
      return new Element('div', {'class' : cssClass || ''}).update(content || '');
    },

    /**
     * Create and return a new SPAN element.
     *
     * @param cssClass a classname to set on the element
     * @param content optional content that should be inserted in the element
     * @return the newly created element
     */
    createSpan : function (cssClass, content) {
      return new Element('span', {'class' : cssClass || ''}).update(content || '');
    },

    /**
     * Create and return a new XWiki button element, an <tt>a.secondary.button</tt> element inside a <tt>span.buttonwrapper</tt>.
     *
     * @param text the text to display on the button
     * @param handler optional event handler to attach to the <tt>click</tt> event
     * @return the newly created element
     */
    createButton : function (text, handler) {
      return new Element('a', {'class' : 'button secondary', 'href' : '#'})
        .update(text || '').wrap('span', {'class' : 'buttonwrapper'})
        .observe('click', handler || Prototype.emptyFunction);
    }
  }

  /**
   * Handles one selected file.
   */
  var FileUploadItem = Class.create({
    /**
     * Constructor; checks if the file is valid, prepares the upload status UI, and starts reading the file.
     * Actually uploading the file must be triggered manually.
     *
     * @param file the local file to be uploaded
     * @param options upload configuration
     */
    initialize : function(file, container, formData, options) {
      this.file = file;
      this.container = container;
      this.formData = formData;
      this.options = options;
      this.initProgressParameters();
      this.generateStatusUI();
    },

    /**
     * Generates upload status UI, consisting of optional file information (name, type, size), and optional progress bar.
     */
    generateStatusUI : function() {
      var statusUI = this.statusUI = {};

      statusUI.UPLOAD_STATUS = UploadUtils.createDiv('upload-status upload-inprogress');

      if (this.options.enableFileInfo) {
        statusUI.FILE_INFO   = UploadUtils.createDiv('file-info');
        (statusUI.FILE_NAME  = UploadUtils.createSpan('file-name', this.file.name.escapeHTML())).title = this.file.type;
        statusUI.FILE_SIZE   = UploadUtils.createSpan('file-size', ' (' + UploadUtils.bytesToSize(this.file.size) + ')');
        statusUI.FILE_CANCEL = UploadUtils.createButton("$services.localization.render('core.widgets.html5upload.item.cancel')", this.cancelUpload.bindAsEventListener(this));
        // TODO MIME type icon?

        statusUI.FILE_INFO.insert(statusUI.FILE_NAME).insert(statusUI.FILE_SIZE).insert(statusUI.FILE_CANCEL);
        statusUI.UPLOAD_STATUS.insert(statusUI.FILE_INFO);
      }

      if (this.options.enableProgressInfo) {
        statusUI.PROGRESS_INFO       = UploadUtils.createDiv('progress-info');
        statusUI.PROGRESS_CONTAINER  = UploadUtils.createDiv('progress-container');
        statusUI.PROGRESS            = UploadUtils.createDiv('progress');
        statusUI.PROGRESS_PERCENTAGE = UploadUtils.createSpan('progress-percentage', '&nbsp;');
        statusUI.PROGRESS_SPEED      = UploadUtils.createSpan('progress-speed', '&nbsp;');
        statusUI.PROGRESS_REMAINING  = UploadUtils.createSpan('progress-remaining', '&nbsp;');
        statusUI.PROGRESS_TRANSFERED = UploadUtils.createSpan('progress-transfered', '&nbsp;');

        statusUI.PROGRESS_INFO.insert(statusUI.PROGRESS_CONTAINER.insert(statusUI.PROGRESS))
                              .insert(statusUI.PROGRESS_PERCENTAGE)
                              .insert(statusUI.PROGRESS_TRANSFERED)
                              .insert(UploadUtils.createDiv('progress-time').insert(statusUI.PROGRESS_SPEED)
                                                                            .insert(statusUI.PROGRESS_REMAINING)
                                                                            .insert(UploadUtils.createDiv('clearfloats'))
        );
        statusUI.UPLOAD_STATUS.insert(statusUI.PROGRESS_INFO);
      }

      if (this.options.responseContainer) {
        statusUI.UPLOAD_RESPONSE = this.options.responseContainer;
      } else {
        statusUI.UPLOAD_RESPONSE = UploadUtils.createDiv('upload-response');
        statusUI.UPLOAD_STATUS.insert(statusUI.UPLOAD_RESPONSE);
      }

      this.container.insert(statusUI.UPLOAD_STATUS);

      return statusUI;
    },

    /**
     * Initialize the progress parameters.
     */
    initProgressParameters : function () {
      this.progressData = {
        bytesUploaded : 0,
        bytesTotal : 0,
        previousBytesUploaded : 0,
        resultFileSize : '',
        latestSpeed : 0,
        updatesPerSecond : 2,
        updatesDone : 0
      }
    },

    /**
     * Start uploading this file, creating a new XHR object with the file data.
     *
     * @param event optional form submit event
     */
    startUploading : function (event) {
      if (this.canceled) {
        // Uploading has already been canceled by the user
        this.onUploadAbort();
        return;
      }

      if (event) {
        event.stop();
      }

      var formData = new FormData();
      formData.append(this.formData.input.name, this.file);
      var fields = this.formData.additionalFields;
      Object.keys(fields).each(function(key) {
        fields[key] && formData.append(key, fields[key]);
      });

      if (this.formData.comment) {
        var commentValue = this.formData.comment.value;
        commentValue && formData.append('comment', commentValue);
      }

      // Create XMLHttpRequest object, adding few event listeners, and POST the data
      var request = this.request = new XMLHttpRequest();

      if (this.options.enableProgressInfo) {
        // Progress listener
        request.upload.addEventListener('progress', this.onUploadProgress.bindAsEventListener(this), false);
        // Set inner timer
        this.timer = setInterval(this.doInnerUpdates.bind(this), Math.round(1000 / this.progressData.updatesPerSecond));
      }
      request.upload.addEventListener('load', this.onUploadFinish.bindAsEventListener(this), false);
      request.addEventListener('load', this.onRequestDone.bindAsEventListener(this), false);
      request.addEventListener('error', this.onUploadError.bindAsEventListener(this), false);
      request.addEventListener('abort', this.onUploadAbort.bindAsEventListener(this), false);
      request.open('POST', this.formData.action);
      request.send(formData);
    },

    /**
     * Cancel an ongoing upload or prevent it from starting.
     *
     * @param event the click event
     */
    cancelUpload : function(event) {
      event && event.stop();
      if (this.completed) {
        return;
      }
      this.request && this.request.abort();
      this.canceled = true;
      clearInterval(this.timer);
      this.statusUI.FILE_CANCEL.addClassName('upload-canceled-label').removeClassName('buttonwrapper').update("$services.localization.render('core.widgets.html5upload.item.canceled')");
      this.statusUI.UPLOAD_STATUS.removeClassName('upload-inprogress').addClassName('upload-canceled');
    },

    /**
     * Update upload progress UI.
     */
    doInnerUpdates : function () {
      this.progressData.updatesDone = this.progressData.updatesDone + 1;
      var secondsPassed = this.progressData.updatesDone / this.progressData.updatesPerSecond;
      var uploaded = this.progressData.bytesUploaded;
      var diff = uploaded - this.progressData.previousBytesUploaded;

      // If nothing new loaded, exit
      if (diff === 0) {
        return;
      }

      this.progressData.previousBytesUploaded = uploaded;
      var bytesPerSecond = uploaded / secondsPassed;
      var bytesRemaining = this.progressData.bytesTotal - this.progressData.previousBytesUploaded;
      var secondsRemaining = bytesRemaining / bytesPerSecond;

      var crtBytesPerSecond = diff * this.progressData.updatesPerSecond;

      // update speed info
      var speed = UploadUtils.bytesToSize(crtBytesPerSecond) + '/s';
      this.progressData.latestSpeed = speed;
      this.statusUI.PROGRESS_SPEED.update(speed);
      this.statusUI.PROGRESS_REMAINING.update(' | ' + UploadUtils.secondsToTime(secondsRemaining));
    },

    /**
     * Function called by the XHR whenever the upload progresses.
     *
     * @param event the ProgressEvent fired by the browser
     */
    onUploadProgress : function(event) {
      if (event.lengthComputable) {
        this.progressData.bytesUploaded = event.loaded;
        this.progressData.bytesTotal = event.total;
        var percentageCompleted = Math.round(event.loaded * 100 / event.total);
        var bytesTransfered = UploadUtils.bytesToSize(this.progressData.bytesUploaded);

        this.statusUI.PROGRESS_PERCENTAGE.update(percentageCompleted + '%');
        this.statusUI.PROGRESS.style.width = percentageCompleted + '%';
        this.statusUI.PROGRESS_TRANSFERED.update('(' + bytesTransfered + ')');
      } else {
        this.statusUI.PROGRESS.update('n/a'); //Unable to compute
      }
    },
    /**
     * Function called by the XHR when the upload finishes successfully (just sending the file).
     *
     * @param event the ProgressEvent fired by the browser
     */
    onUploadFinish : function(event) {
      this.completed = true;
      clearInterval(this.timer);
      if (this.statusUI.FILE_CANCEL) {
        this.statusUI.FILE_CANCEL.addClassName('hidden');
      }
      this.formData.input.fire('xwiki:html5upload:message', {content: 'UPLOAD_FINISHING', type: 'inprogress', source: this,
        parameters : {name : this.file.name.escapeHTML()}
      });
    },

    /**
     * Function called by the XHR when the request finishes successfully (both sending the file and receiving the response).
     *
     * @param event the ProgressEvent fired by the browser
     */
    onRequestDone : function(event) {
      if (event && event.target && typeof event.target.status === 'number') {
        if (event.target.status >= 200 && event.target.status < 300) {
          this.statusUI.UPLOAD_RESPONSE.update(event.target.responseText);
        } else {
          this.onUploadError();
          return;
        }
      }

      if (this.options.enableProgressInfo) {
        this.statusUI.PROGRESS_PERCENTAGE.update('100%');
        this.statusUI.PROGRESS.style.width = "100%";
        this.statusUI.PROGRESS_REMAINING.update(' | 00:00:00');
        this.statusUI.PROGRESS_TRANSFERED.update('(' + UploadUtils.bytesToSize(this.file.size) + ')');
        if (this.progressData.latestSpeed === 0) {
          this.statusUI.PROGRESS_SPEED.update(UploadUtils.bytesToSize(this.file.size) + "/s");
        }
      }
      this.formData.input.fire('xwiki:html5upload:message', {content: 'UPLOAD_FINISHED', type: 'done', source: this,
        parameters : {name : this.file.name.escapeHTML(), size : UploadUtils.bytesToSize(this.file.size)}
      });
      this.formData.input.fire('xwiki:html5upload:fileFinished', {source: this});
      clearInterval(this.timer);
      this.statusUI.UPLOAD_STATUS.removeClassName('upload-inprogress').addClassName('upload-done');
    },

    /**
     * Function called by the XHR when the upload finishes unsuccessfully.
     */
    onUploadError : function() {
      this.statusUI.FILE_CANCEL.remove();
      this.statusUI.UPLOAD_STATUS.removeClassName('upload-inprogress').addClassName('upload-error');
      this.abnormalUploadFinish('UNKNOWN_ERROR');
    },

    /**
     * Function called by the XHR when the upload is aborted.
     */
    onUploadAbort : function() {
      this.abnormalUploadFinish('UPLOAD_ABORTED');
    },

    /**
     * Internal function called when the upload finishes with an error.
     *
     * @param message the identifier of the message to display to the user
     */
    abnormalUploadFinish : function (message) {
      clearInterval(this.timer);
      this.formData.input.fire('xwiki:html5upload:message', {content: message, type: 'error', source: this, parameters :
       {name : this.file.name.escapeHTML()}});
      this.formData.input.fire('xwiki:html5upload:fileFinished', {source: this});
    }
  });

  // Determine the configured maximum attachment size.
  var maxAttachmentSize = '$!escapetool.javascript($xwiki.getSpacePreference("upload_maxsize"))';
  // 32MB is the default maximum size used inside the FileUploadPlugin.
  // There's no easy way of getting that internal value, so we just assume it didn't change.
  maxAttachmentSize = parseInt(maxAttachmentSize || 33554432);

  /**
   * HTML5 file uploader associated with an input of type file.
   */
  XWiki.FileUploader = Class.create({
    /** Default configuration. */
    options : {
      /** Maximum accepted file size. */
      maxFilesize : maxAttachmentSize,
      /** Regular expression defining accepted MIME types. */
      fileFilter  : /.*/i,
      /** Should information (name, type, size) about each selected file be displayed? */
      enableFileInfo : true,
      /** Should a progress bar be displayed as each file is uploaded? */
      enableProgressInfo : true,
      /** Should the progress information disappear automatically once all the uploads are completed? */
      progressAutohide : false,
      /** Should the upload start as soon as the files are selected, or wait for a submit event? */
      autoUpload : true,
      /** Where to send the files? If no URL is given, then the file is sent to the normal target of the form. */
      targetURL : null,
      /** Where should the server response be displayed? If no container is specified, then a new div will be inserted below the upload status progress bar. */
      responseContainer : null,
      /** A custom URL to be used for obtaining the response after the files are uploaded. */
      responseURL : null
    },

    /** Templates for feedback messages displayed to the user. */
    messages : {
      UNKNOWN_ERROR         : new Template("$services.localization.render('core.widgets.html5upload.error.unknown', ['#{name}'])"),
      INVALID_FILE_TYPE     : new Template("$services.localization.render('core.widgets.html5upload.error.invalidType', ['#{name}'])"),
      UPLOAD_LIMIT_EXCEEDED : new Template("$services.localization.render('core.widgets.html5upload.error.invalidSize', ['#{name}', '#{size}'])"),
      UPLOAD_ABORTED        : new Template("$services.localization.render('core.widgets.html5upload.error.aborted', ['#{name}'])"),
      UPLOAD_FINISHING      : new Template("$services.localization.render('core.widgets.html5upload.status.finishing', ['#{name}'])"),
      UPLOAD_FINISHED       : new Template("$services.localization.render('core.widgets.html5upload.status.finished', ['#{name}', '#{size}'])")
    },

    /**
     * Constructor which attaches event handlers to the form and prepares the progress UI.
     *
     * @param input the file input to enhance
     * @param options configuration
     */
    initialize : function(input, options) {
      // Update the options
      this.options = Object.extend(Object.clone(this.options), options || { });

      if (input.__x_html5uploader) {
        return;
      } else {
         input.__x_html5uploader = this;
      }

      // Make sure the input for which the uploader is being generated is of type file, and it belongs to a form
      if (input.type != 'file') {
        return;
      };
      this.input = input;
      this.inputContainer = this.input.up('.fileupload-field') || this.input;
      this.form = input.form;
      if (!this.form) {
        return;
      }

      // Any mentions of a filename filter present in the form?
      var customFilter = this.form.down('input[type=hidden][name=' + input.name + '__filter]');
      if (!this.options.fileFilter && customFilter && customFilter.value != '') {
         this.options.fileFilter = new RegExp(customFilter.value, "i");
      }

      // What is the URL where the file should be sent?
      this.options.targetURL = this.options.targetURL || this.form.action;

      // Get the input that contains the comment
      var comment = this.form.down('input[name=comment]');

      // Prepare common form data to send with each uploaded file
      this.formData = {
        input : this.input,
        action : this.options.targetURL,
        comment: comment,
        additionalFields : {}
      };
      var redirect = this.form.down('input[name=xredirect]');
      this.formData.additionalFields.xredirect = this.options.responseURL || redirect && redirect.value;
      var form_token = this.form.down('input[name=form_token]');
      form_token && (this.formData.additionalFields.form_token = form_token.value);

      // Attach event listeners to the target file input
      this.onUploadNextFile = this.onUploadNextFile.bindAsEventListener(this);
      this.input.observe('change', this.onFilesSelected.bindAsEventListener(this));
      this.input.observe('xwiki:html5upload:start', this.showUploadStatus.bindAsEventListener(this));
      this.input.observe('xwiki:html5upload:start', this.onUploadNextFile);
      this.input.observe('xwiki:html5upload:fileFinished', this.onUploadNextFile);
      this.input.observe('xwiki:html5upload:message', this.onMessage.bindAsEventListener(this));
      this.input.observe('xwiki:html5upload:done', this.onUploadDone.bindAsEventListener(this));

      // Generate the upload status UI (initially hidden)
      this.generateStatusUI();
    },

    /**
     * Generates upload status UI, consisting of a container for individual file upload UI, a hide button, and a cancel button.
     */
    generateStatusUI : function() {
      var statusUI = this.statusUI = {};
      statusUI.CONTAINER = UploadUtils.createDiv('upload-status-container');
      statusUI.LIST = UploadUtils.createDiv('upload-status-list');
      statusUI.CANCEL = UploadUtils.createButton(
        "$services.localization.render('core.widgets.html5upload.cancelAll')",
         this.cancelUpload.bindAsEventListener(this)
      );
      statusUI.HIDE = UploadUtils.createButton(
        "$services.localization.render('core.widgets.html5upload.hideStatus')",
         this.hideUploadStatus.bindAsEventListener(this)
      );
      statusUI.HIDE.hide();
      statusUI.CONTAINER.insert(statusUI.LIST).insert(statusUI.CANCEL).insert(statusUI.HIDE);
    },

    /**
     * Display the upload status UI and hide the target input when files are selected.
     */
    showUploadStatus : function() {
      this.inputContainer.insert({after: this.statusUI.CONTAINER});
      this.statusUI.HIDE.hide();
      this.statusUI.CANCEL.show();
    },

    /**
     * Hide the upload status UI and re-display the target input when the upload is completed.
     */
    hideUploadStatus : function(event) {
      event && event.stop();
      this.input.value = '';
      this.statusUI.CONTAINER.up() && this.statusUI.CONTAINER.remove();
      this.statusUI.LIST.update('');
    },

    /**
     * Event handler called when the user selects local files to upload.
     */
    onFilesSelected : function () {
      var total = this.input.files.length;
      this.fileUploadItems = [];
      for (var i = 0; i < total; ++i) {
        var file = this.input.files[i];
        try {
          var event = Event.fire(this.input, 'xwiki:actions:beforeUpload', {
            file: file
          });
          // Queue the file only if no listener cancelled the event.
          if (!event.defaultPrevented) {
            this.fileUploadItems.push(new FileUploadItem(file, this.statusUI.LIST, this.formData, this.options));
          }
        } catch (ex) {
          this.showMessage(ex, 'error', {size : UploadUtils.bytesToSize(this.options && this.options.maxFilesize),
                                         name : file.name.escapeHTML(), type: file.type
          });
        }
      }
      Event.fire(this.input, 'xwiki:html5upload:start');
    },

    /**
     * Event handler called when a new file from the current selection is ready to be uploaded.
     */
    onUploadNextFile : function () {
      var next = this.currentUpload = this.fileUploadItems.shift();
      if (next) {
        next.startUploading();
      } else {
        Event.fire(this.input, 'xwiki:html5upload:done');
      }
    },

    /**
     * Cancel all pending uploads.
     *
     * @param event an optional UI event
     */
    cancelUpload : function (event) {
      event && event.stop();
      this.fileUploadItems.invoke('cancelUpload');
      this.currentUpload && this.currentUpload.cancelUpload();
      this.input.fire('xwiki:html5upload:done');
    },

    /**
     * Event handler called when all selected files have been processed.
     * If autohide is enabled, the actual hide function call is scheduled,
     * otherwise the hide button becomes visible.
     */
    onUploadDone : function() {
      this.statusUI.CANCEL.hide();
      if (this.options.progressAutohide) {
        setTimeout(this.hideUploadStatus.bind(this), 2000);
      } else {
        this.statusUI.HIDE.show();
      }
    },

    /**
     * Event handler called when a message is received from a FileUploadItem object.
     *
     * @param event the event, which must hold in its memo the <tt>source</tt> object,
     * the <tt>content</tt> of the message, and, optionally, a message <tt>type</tt> and a <tt>parameters</tt> map
     */
    onMessage : function (event) {
      if (!(event.memo && event.memo.source && event.memo.content)) {
        return;
      }
      if (event.memo.source._currentMessage) {
        event.memo.source._currentMessage.hide();
      }
      event.memo.source._currentMessage = this.showMessage(event.memo.content, event.memo.type, event.memo.parameters);
    },

    /**
     * Display a feedback message to inform the user about the upload status.
     *
     * @param message the identifier of the message to display, a key in the messages object
     * @param type the type of the notification message, see types supported by XWiki.widgets.Notification
     * @param parameters optional Template parameters
     * @return an XWiki.widgets.Notification object displaying the requested message
     */
    showMessage : function(message, type, parameters) {
      var formattedMessage = this.messages[message] || message;
      if (formattedMessage instanceof Template) {
        formattedMessage = formattedMessage.evaluate(parameters || {});
      }
      return new XWiki.widgets.Notification(formattedMessage, type || "plain");
    },

    /**
     * A function that can be called externally for hiding the form submit buttons.
     * It is suited for forms that only contain only one file input with an HTML5 uploader attached to it.
     */
    hideFormButtons : function () {
      if (!this.form.hasClassName('html5upload-initialized')) {
        this.form.addClassName('html5upload-initialized');
        if (this.options.autoUpload) {
          // Hide submit buttons
          this.form.select('input[type=submit]').invoke('hide');
        }
        var cancelButton = this.form.down('.cancel');
        if (cancelButton) {
          cancelButton.hide();
        }
      }
    }
  });
  return XWiki;
}(XWiki || {}));
