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
/*!
#set ($iconNames = ['check', 'remove', 'error', 'cross'])
#set ($icons = {})
#foreach ($name in $iconNames)
#set ($discard = $icons.put($name, $services.icon.renderHTML($name)))
#end
#set($maxAttachmentSize = "$!escapetool.javascript($xwiki.getSpacePreference('upload_maxsize'))")
#[[*/
// Start JavaScript-only code.
(function(icons, maxAttachmentSize) {
  "use strict";
define('upload-translations', {
  prefix: 'core.widgets.html5upload.',
  keys: [
    'status.icon.inprogress',
    'status.icon.done',
    'status.icon.canceled',
    'status.icon.error',
    'item.cancel',
    'item.canceled',
    'cancelAll',
    'hideStatus',
    'status.fileSize',
    'status.remaining',
    'error.unknown',
    'error.invalidType',
    'error.invalidSize',
    'error.aborted',
    'status.finishing',
    'status.finished'
  ]
});
define('xwiki-upload', ['jquery', 'xwiki-l10n!upload-translations', 'xwiki-events-bridge'], function($, l10n) {
  /**
   * Internal small utility functions.
   */
  const UploadUtils = class {
    /**
     * Prevents the default behavior of the given event and stops its propagation.
     *
     * @param event the event to stop, can be undefined
     */
    static stopEvent(event)
    {
      event?.preventDefault();
      event?.stopPropagation();
    }

    /**
     * Replaces the #{name} placeholders from the given message template with the corresponding parameter values.
     *
     * @param template the message template
     * @param parameters the values to use for the placeholders
     * @return the formatted message
     */
    static formatMessage(template, parameters)
    {
      return template.replace(/#\{(\w+)\}/g, (match, key) => parameters[key] ?? '');
    }

    /**
     * Convert seconds to human readable time format.
     *
     * @param seconds the number of seconds to convert
     * @return a string in the format "HH:mm:ss"
     */
    static secondsToTime(seconds)
    {
      let hr = Math.floor(seconds / 3600);
      let min = Math.floor((seconds - (hr * 3600)) / 60);
      let sec = Math.floor(seconds - (hr * 3600) - (min * 60));

      if (hr < 10) {
        hr = "0" + hr;
      }
      if (min < 10) {
        min = "0" + min;
      }
      if (sec < 10) {
        sec = "0" + sec;
      }
      return hr + ':' + min + ':' + sec;
    }

    /**
     * Convert bytes to a human-readable, localized size format.
     *
     * @param bytes the number of bytes to convert
     * @param isRate whether the value represents a data rate (appends a localized "per second" unit)
     * @return a string representing the size (or rate) in bytes, kilobytes, megabytes or gigabytes, localized using
     *         the page language, with 1 decimal precision
     */
    static bytesToSize(bytes, isRate = false)
    {
      if (bytes === 0) return 'N/A';
      let units = ['byte', 'kilobyte', 'megabyte', 'gigabyte'];
      let unitIndex = 0;
      let value = bytes;
      while (value >= 1000 && unitIndex < units.length - 1) {
        value /= 1000;
        unitIndex++;
      }
      let unit = units[unitIndex] + (isRate ? '-per-second' : '');
      return new Intl.NumberFormat(document.documentElement.lang || undefined,
          {style: 'unit', unit, unitDisplay: 'narrow', maximumFractionDigits: 1}).format(value);
    }

    /**
     * Create and return a new DIV element.
     *
     * @param cssClass a classname to set on the element
     * @param content optional content that should be appended in the element
     * @return the newly created element
     */
    static createDiv(cssClass, content)
    {
      let result = document.createElement('div');
      if (cssClass.length) result.classList.add(...cssClass.split(' '));
      result.innerHTML = content||'';
      return result;
    }

    /**
     * Create and return a new SPAN element.
     *
     * @param cssClass a classname to set on the element
     * @param content optional content that should be appended in the element
     * @return the newly created element
     */
    static createSpan(cssClass, content)
    {
      let result = document.createElement('span');
      if (cssClass.length) {
        result.classList.add(...cssClass.split(' '));
      }
      result.innerHTML = content || '';
      return result;
    }

    /**
     * Create and return a new button element, an <tt>button.btn.btn-default</tt> element.
     *
     * @param content the text to display on the button
     * @param handler optional event handler to attach to the <tt>click</tt> event
     * @return the newly created element
     */
    static createButton(content, handler)
    {
      let result = document.createElement('button');
      result.classList.add('btn', 'btn-default');
      result.innerHTML = content || '';
      if (handler) {
        result.addEventListener('click', handler);
      }
      return result;
    }
  }

  /**
   * Handles one selected file.
   */
  class FileUploadItem {
    /**
     * Constructor; checks if the file is valid, prepares the upload status UI, and starts reading the file.
     * Actually uploading the file must be triggered manually.
     *
     * @param file the local file to be uploaded
     * @param container the HTML node that contains the UI.
     * @param formData
     * @param options upload configuration
     */
    constructor(file, container, formData, options)
    {
      this.file = file;
      this.container = container;
      this.formData = formData;
      this.options = options;
      this.initProgressParameters();
      this.generateStatusUI();
    }

    /**
     * Generates upload status UI, consisting of optional file information (name, type, size), and optional progress bar.
     */
    generateStatusUI()
    {
      let statusUI = this.statusUI = {};

      statusUI.UPLOAD_STATUS = UploadUtils.createDiv('upload-status upload-inprogress upload-waiting');
      statusUI.UPLOAD_STATUS_MAIN = document.createElement('div');
      statusUI.UPLOAD_STATUS.append(statusUI.UPLOAD_STATUS_MAIN);

      if (this.options.enableFileInfo) {
        statusUI.FILE_INFO = UploadUtils.createDiv('file-info');
        statusUI.FILE_NAME = UploadUtils.createSpan('file-name');
        statusUI.FILE_NAME.textContent = this.file.name;
        statusUI.FILE_NAME.title = this.file.type;
        statusUI.FILE_SIZE_CONTAINER = UploadUtils.createSpan('progress-info');
        statusUI.FILE_SIZE = UploadUtils.createSpan('file-size', UploadUtils.bytesToSize(this.file.size));
        statusUI.FILE_SIZE_CONTAINER.append(statusUI.FILE_SIZE);
        statusUI.FILE_SIZE_ALTERNATIVE = UploadUtils.createSpan('sr-only', l10n['status.fileSize']);
        statusUI.FILE_SIZE.append(statusUI.FILE_SIZE_ALTERNATIVE);
        statusUI.FILE_CANCEL = UploadUtils.createButton(icons['cross'], event => this.cancelUpload(event));
        statusUI.FILE_CANCEL.classList.add('upload-cancel');
        statusUI.FILE_CANCEL_ALTERNATIVE = UploadUtils.createSpan('sr-only', l10n['item.cancel']);
        statusUI.FILE_CANCEL.append(statusUI.FILE_CANCEL_ALTERNATIVE);
        // We want to put the button next to everything else.
        statusUI.UPLOAD_STATUS.append(statusUI.FILE_CANCEL);
        statusUI.FILE_INFO.append(statusUI.FILE_NAME, statusUI.FILE_SIZE_CONTAINER);
        statusUI.UPLOAD_STATUS_MAIN.append(statusUI.FILE_INFO);
      }

      if (this.options.enableProgressInfo) {
        statusUI.PROGRESS_INFO = UploadUtils.createDiv('progress-info');
        statusUI.PROGRESS = document.createElement('progress');
        statusUI.PROGRESS.setAttribute('value', 0);
        statusUI.PROGRESS_PERCENTAGE = UploadUtils.createSpan('progress-percentage', '&nbsp;');
        statusUI.PROGRESS_SPEED = UploadUtils.createSpan('progress-speed', '&nbsp;');
        statusUI.PROGRESS_REMAINING = UploadUtils.createSpan('progress-remaining', '&nbsp;');
        statusUI.PROGRESS_REMAINING_TEXT = UploadUtils.createSpan('progress-remaining-text', '');
        statusUI.PROGRESS_TRANSFERED = UploadUtils.createSpan('progress-transfered', '&nbsp;');

        statusUI.PROGRESS_INFO.append(statusUI.PROGRESS);
        // If the file info is displayed, we can put some of this info above the progress bar.
        if (this.options.enableFileInfo) {
          statusUI.FILE_SIZE_CONTAINER.append(statusUI.PROGRESS_TRANSFERED, statusUI.PROGRESS_PERCENTAGE);
        } else {
          statusUI.PROGRESS_INFO.append(statusUI.PROGRESS_TRANSFERED, statusUI.PROGRESS_PERCENTAGE);
        }
        statusUI.PROGRESS_TIME = UploadUtils.createDiv('progress-time');
        let remainingText = l10n.get('status.remaining', '__timer__').split('__timer__');
        statusUI.PROGRESS_REMAINING_TEXT.append(remainingText[0]);
        statusUI.PROGRESS_REMAINING_TEXT.append(statusUI.PROGRESS_REMAINING);
        statusUI.PROGRESS_REMAINING_TEXT.append(remainingText[1]);
        statusUI.PROGRESS_TIME.append(statusUI.PROGRESS_REMAINING_TEXT, 
          statusUI.PROGRESS_SPEED);
        statusUI.PROGRESS_INFO.append(statusUI.PROGRESS_TIME);
        statusUI.UPLOAD_STATUS_MAIN.append(statusUI.PROGRESS_INFO);
      }

      if (this.options.responseContainer) {
        statusUI.UPLOAD_RESPONSE = this.options.responseContainer;
      } else {
        statusUI.UPLOAD_RESPONSE = UploadUtils.createDiv('upload-response');
        statusUI.UPLOAD_STATUS_MAIN.append(statusUI.UPLOAD_RESPONSE);
      }

      // Set up the icons and their text alternatives
      // The inprogress icon is a special case, we set the content using a GIF background in CSS
      statusUI.STATUS_UPLOAD_RESULT = UploadUtils.createSpan('upload-result');
      statusUI.RESULT_DONE = UploadUtils.createDiv('result-done', icons['check']);
      statusUI.RESULT_DONE_TEXT = UploadUtils.createSpan('', l10n['status.icon.done']);
      statusUI.RESULT_DONE.append(statusUI.RESULT_DONE_TEXT);
      statusUI.RESULT_CANCELED = UploadUtils.createDiv('result-canceled', icons['remove']);
      statusUI.RESULT_CANCELED_TEXT = UploadUtils.createSpan('', l10n['status.icon.canceled']);
      statusUI.RESULT_CANCELED.append(statusUI.RESULT_CANCELED_TEXT);
      statusUI.RESULT_ERROR = UploadUtils.createDiv('result-error', icons['error']);
      statusUI.RESULT_ERROR_TEXT = UploadUtils.createSpan('', l10n['status.icon.error']);
      statusUI.RESULT_ERROR.append(statusUI.RESULT_ERROR_TEXT);
      statusUI.STATUS_UPLOAD_RESULT.append(statusUI.RESULT_DONE, statusUI.RESULT_CANCELED, statusUI.RESULT_ERROR);
      statusUI.UPLOAD_STATUS_MAIN.append(statusUI.STATUS_UPLOAD_RESULT);

      this.container.append(statusUI.UPLOAD_STATUS);

      return statusUI;
    }

    /**
     * Initialize the progress parameters.
     */
    initProgressParameters()
    {
      this.progressData = {
        bytesUploaded: 0,
        bytesTotal: 0,
        previousBytesUploaded: 0,
        resultFileSize: '',
        latestSpeed: 0,
        updatesPerSecond: 2,
        updatesDone: 0
      }
    }

    /**
     * Start uploading this file, creating a new XHR object with the file data.
     *
     * @param event optional form submit event
     */
    startUploading(event)
    {
      if (this.canceled) {
        // Uploading has already been canceled by the user
        this.onUploadAbort();
        return;
      }

      UploadUtils.stopEvent(event);

      let formData = new FormData();
      formData.append(this.formData.input.name, this.file);
      let fields = this.formData.additionalFields;
      Object.keys(fields).forEach(function (key) {
        fields[key] && formData.append(key, fields[key]);
      });

      if (this.formData.comment) {
        const commentValue = this.formData.comment.value;
        commentValue && formData.append('comment', commentValue);
      }

      // Create XMLHttpRequest object, adding few event listeners, and POST the data
      let request = this.request = new XMLHttpRequest();
      
      this.statusUI.UPLOAD_STATUS.classList.remove('upload-waiting');
      if (this.options.enableProgressInfo) {
        // Progress listener
        request.upload.addEventListener('progress', event => this.onUploadProgress(event), false);
        // Set inner timer
        this.timer = setInterval(this.doInnerUpdates.bind(this), Math.round(1000 / this.progressData.updatesPerSecond));
      }
      request.upload.addEventListener('load', () => this.onUploadFinish(), false);
      request.addEventListener('load', event => this.onRequestDone(event), false);
      request.addEventListener('error', () => this.onUploadError(), false);
      request.addEventListener('abort', () => this.onUploadAbort(), false);
      request.open('POST', this.formData.action);
      request.send(formData);
    }

    /**
     * Cancel an ongoing upload or prevent it from starting.
     *
     * @param event the click event
     */
    cancelUpload(event)
    {
      UploadUtils.stopEvent(event);
      if (this.completed) {
        return;
      }
      this.request?.abort();
      this.canceled = true;
      clearInterval(this.timer);
      this.statusUI.UPLOAD_STATUS.classList.remove('upload-inprogress');
      this.statusUI.UPLOAD_STATUS.classList.add('upload-canceled');
    }

    /**
     * Update upload progress UI.
     */
    doInnerUpdates()
    {
      this.progressData.updatesDone = this.progressData.updatesDone + 1;
      let secondsPassed = this.progressData.updatesDone / this.progressData.updatesPerSecond;
      let uploaded = this.progressData.bytesUploaded;
      let diff = uploaded - this.progressData.previousBytesUploaded;

      // If nothing new loaded, exit
      if (diff === 0) {
        return;
      }

      this.progressData.previousBytesUploaded = uploaded;
      let bytesPerSecond = uploaded / secondsPassed;
      let bytesRemaining = this.progressData.bytesTotal - this.progressData.previousBytesUploaded;
      let secondsRemaining = bytesRemaining / bytesPerSecond;

      let crtBytesPerSecond = diff * this.progressData.updatesPerSecond;

      // update speed info
      let speed = UploadUtils.bytesToSize(crtBytesPerSecond, true);
      this.progressData.latestSpeed = speed;
      this.statusUI.PROGRESS_SPEED.textContent = `(${speed})`;
      this.statusUI.PROGRESS_REMAINING.textContent = UploadUtils.secondsToTime(secondsRemaining);
    }

    /**
     * Function called by the XHR whenever the upload progresses.
     *
     * @param event the ProgressEvent fired by the browser
     */
    onUploadProgress(event)
    {
      if (event.lengthComputable) {
        this.progressData.bytesUploaded = event.loaded;
        this.progressData.bytesTotal = event.total;
        const percentageCompleted = Math.round(event.loaded * 100 / event.total);
        const bytesTransfered = UploadUtils.bytesToSize(this.progressData.bytesUploaded);

        this.statusUI.PROGRESS_PERCENTAGE.textContent = percentageCompleted + '%';
        this.statusUI.PROGRESS.setAttribute('value', percentageCompleted / 100);
        this.statusUI.PROGRESS_TRANSFERED.textContent = '(' + bytesTransfered + ')';
      } else {
        this.statusUI.PROGRESS.textContent = 'n/a'; //Unable to compute
      }
    }

    /**
     * Function called by the XHR when the upload finishes successfully (just sending the file).
     */
    onUploadFinish()
    {
      this.completed = true;
      clearInterval(this.timer);
      $(this.formData.input).trigger('xwiki:html5upload:message', [{
        content: 'UPLOAD_FINISHING', type: 'inprogress', source: this,
        parameters: {name: this.file.name}
      }]);
    }

    /**
     * Function called by the XHR when the request finishes successfully (both sending the file and receiving the response).
     *
     * @param event the ProgressEvent fired by the browser
     */
    onRequestDone(event)
    {
      if (typeof event?.target?.status === 'number' &&
          (event.target.status < 200 || event.target.status >= 300))
      {
        this.onUploadError();
        return;
      }

      if (event?.target?.responseText) {
        this.statusUI.UPLOAD_RESPONSE.innerHTML = event.target.responseText;
      }

      if (this.options.enableProgressInfo) {
        this.statusUI.PROGRESS_PERCENTAGE.textContent = '100%';
        this.statusUI.PROGRESS_REMAINING.textContent = '00:00:00';
        this.statusUI.PROGRESS_TRANSFERED.textContent = UploadUtils.bytesToSize(this.file.size);
        if (this.progressData.latestSpeed === 0) {
          this.statusUI.PROGRESS_SPEED.textContent = '(' + UploadUtils.bytesToSize(this.file.size, true) + ')';
        }
      }
      $(this.formData.input).trigger('xwiki:html5upload:message', [{
        content: 'UPLOAD_FINISHED', type: 'done', source: this,
        parameters: {name: this.file.name, size: UploadUtils.bytesToSize(this.file.size)}
      }]);
      $(this.formData.input).trigger('xwiki:html5upload:fileFinished', [{source: this}]);
      clearInterval(this.timer);
      this.statusUI.UPLOAD_STATUS.classList.remove('upload-inprogress');
      this.statusUI.UPLOAD_STATUS.classList.add('upload-done');
    }

    /**
     * Function called by the XHR when the upload finishes unsuccessfully.
     */
    onUploadError()
    {
      this.statusUI.UPLOAD_STATUS.classList.remove('upload-inprogress');
      this.statusUI.UPLOAD_STATUS.classList.add('upload-error');
      this.abnormalUploadFinish('UNKNOWN_ERROR');
    }

    /**
     * Function called by the XHR when the upload is aborted.
     */
    onUploadAbort()
    {
      this.abnormalUploadFinish('UPLOAD_ABORTED');
    }

    /**
     * Internal function called when the upload finishes with an error.
     *
     * @param message the identifier of the message to display to the user
     */
    abnormalUploadFinish(message)
    {
      clearInterval(this.timer);
      $(this.formData.input).trigger('xwiki:html5upload:message', [{
        content: message, type: 'error', source: this, parameters:
            {name: this.file.name}
      }]);
      $(this.formData.input).trigger('xwiki:html5upload:fileFinished', [{source: this}]);
    }
  }

  // Determine the configured maximum attachment size.
  // 32MB is the default maximum size used inside the FileUploadPlugin.
  // There's no easy way of getting that internal value, so we just assume it didn't change.
  // 32MB = 33554432 Bytes 
  maxAttachmentSize = Number.parseInt(maxAttachmentSize || 33554432);
  
  /**
   * HTML5 file uploader associated with an input of type file.
   */
  class FileUploader {
    /** Default configuration. */
    options = {
      /** Maximum accepted file size. */
      maxFilesize: maxAttachmentSize,
      /** Regular expression defining accepted MIME types. */
      fileFilter: /.*/i,
      /** Should information (name, type, size) about each selected file be displayed? */
      enableFileInfo: true,
      /** Should a progress bar be displayed as each file is uploaded? */
      enableProgressInfo: true,
      /** Should the progress information disappear automatically once all the uploads are completed? */
      progressAutohide: false,
      /** Should the upload start as soon as the files are selected, or wait for a submit event? */
      autoUpload: true,
      /** Where to send the files? If no URL is given, then the file is sent to the normal target of the form. */
      targetURL: null,
      /** Where should the server response be displayed? If no container is specified, then a new div will be appended below the upload status progress bar. */
      responseContainer: null,
      /** A custom URL to be used for obtaining the response after the files are uploaded. */
      responseURL: null
    }

    /** Templates for feedback messages displayed to the user. */
    messages = {
      UNKNOWN_ERROR: l10n.get('error.unknown', '#{name}'),
      INVALID_FILE_TYPE: l10n.get('error.invalidType', '#{name}'),
      UPLOAD_LIMIT_EXCEEDED: l10n.get('error.invalidSize', '#{name}', '#{size}'),
      UPLOAD_ABORTED: l10n.get('error.aborted', '#{name}'),
      UPLOAD_FINISHING: l10n.get('status.finishing', '#{name}'),
      UPLOAD_FINISHED: l10n.get('status.finished', '#{name}', '#{size}')
    }

    /**
     * Constructor which attaches event handlers to the form and prepares the progress UI.
     *
     * @param input the file input to enhance
     * @param options configuration
     */
    constructor(input, options)
    {
      // Update the options
      this.options = {...this.options, ...options};

      if (input.__x_html5uploader) {
        return;
      } else {
        input.__x_html5uploader = this;
      }

      // Make sure the input for which the uploader is being generated is of type file, and it belongs to a form
      if (input.type !== 'file') return;
      this.input = input;
      this.inputContainer = this.input.parentElement?.closest('.fileupload-field') || this.input;
      this.form = input.form;
      if (!this.form) {
        return;
      }

      // Any mentions of a filename filter present in the form?
      const customFilter = this.form.querySelector('input[type=hidden][name=' + input.name + '__filter]');
      if (!this.options.fileFilter && customFilter && customFilter.value !== '') {
        this.options.fileFilter = new RegExp(customFilter.value, "i");
      }

      // What is the URL where the file should be sent?
      this.options.targetURL = this.options.targetURL || this.form.action;

      // Get the input that contains the comment
      let comment = this.form.querySelector('input[name=comment]');

      // Prepare common form data to send with each uploaded file
      this.formData = {
        input: this.input,
        action: this.options.targetURL,
        comment: comment,
        additionalFields: {}
      };
      let redirect = this.form.querySelector('input[name=xredirect]');
      this.formData.additionalFields.xredirect = this.options.responseURL || redirect?.value;
      let form_token = this.form.querySelector('input[name=form_token]');
      form_token && (this.formData.additionalFields.form_token = form_token.value);

      // Attach event listeners to the target file input
      this.onUploadNextFile = this.onUploadNextFile.bind(this);
      this.input.addEventListener('change', () => this.onFilesSelected());
      $(this.input).on('xwiki:html5upload:start', () => this.showUploadStatus())
        .on('xwiki:html5upload:start', this.onUploadNextFile)
        .on('xwiki:html5upload:fileFinished', this.onUploadNextFile)
        .on('xwiki:html5upload:message', (event, data) => this.onMessage(event, data))
        .on('xwiki:html5upload:done', () => this.onUploadDone());

      // Generate the upload status UI (initially hidden)
      this.generateStatusUI();
    }

    /**
     * Generates upload status UI, consisting of a container for individual file upload UI, a hide button, and a cancel button.
     */
    generateStatusUI()
    {
      let statusUI = this.statusUI = {};
      statusUI.CONTAINER = UploadUtils.createDiv('upload-status-container');
      statusUI.LIST = UploadUtils.createDiv('upload-status-list');
      statusUI.CANCEL = UploadUtils.createButton(
          l10n['cancelAll'],
          event => this.cancelUpload(event)
      );
      statusUI.HIDE = UploadUtils.createButton(
          l10n['hideStatus'],
          event => this.hideUploadStatus(event)
      );
      statusUI.HIDE.style.display = 'none';
      statusUI.CONTAINER.append(statusUI.LIST, statusUI.CANCEL, statusUI.HIDE);
    }

    /**
     * Display the upload status UI and hide the target input when files are selected.
     */
    showUploadStatus()
    {
      this.inputContainer.parentNode.append(this.statusUI.CONTAINER);
      this.statusUI.HIDE.style.display = 'none';
      this.statusUI.CANCEL.style.display = '';
    }

    /**
     * Hide the upload status UI and re-display the target input when the upload is completed.
     */
    hideUploadStatus(event)
    {
      UploadUtils.stopEvent(event);
      this.input.value = '';
      this.statusUI.CONTAINER.remove();
      this.statusUI.LIST.replaceChildren();
    }

    /**
     * Event handler called when the user selects local files to upload.
     */
    onFilesSelected()
    {
      const total = this.input.files.length;
      this.fileUploadItems = [];
      for (let i = 0; i < total; ++i) {
        let file = this.input.files[i];
        try {
          const event = $.Event('xwiki:actions:beforeUpload');
          $(this.input).trigger(event, [{
            file: file
          }]);
          // Queue the file only if no listener cancelled the event.
          if (!event.isDefaultPrevented()) {
            this.fileUploadItems.push(new FileUploadItem(file, this.statusUI.LIST, this.formData, this.options));
          }
        } catch (ex) {
          console.error(ex);
          this.showMessage(ex, 'error', {
            size: UploadUtils.bytesToSize(this.options?.maxFilesize),
            name: file.name, type: file.type
          });
        }
      }
      $(this.input).trigger('xwiki:html5upload:start');
    }

    /**
     * Event handler called when a new file from the current selection is ready to be uploaded.
     */
    onUploadNextFile()
    {
      let next = this.currentUpload = this.fileUploadItems.shift();
      if (next) {
        next.startUploading();
      } else {
        $(this.input).trigger('xwiki:html5upload:done');
      }
    }

    /**
     * Cancel all pending uploads.
     *
     * @param event an optional UI event
     */
    cancelUpload(event)
    {
      UploadUtils.stopEvent(event);
      this.fileUploadItems.forEach(fileUploadItem => fileUploadItem.cancelUpload());
      this.currentUpload?.cancelUpload();
      $(this.input).trigger('xwiki:html5upload:done');
    }

    /**
     * Event handler called when all selected files have been processed.
     * If autohide is enabled, the actual hide function call is scheduled,
     * otherwise the hide button becomes visible.
     */
    onUploadDone()
    {
      this.statusUI.CANCEL.style.display = 'none';
      if (this.options.progressAutohide) {
        setTimeout(() => this.hideUploadStatus(), 2000);
      } else {
        this.statusUI.HIDE.style.display = '';
      }
    }

    /**
     * Event handler called when a message is received from a FileUploadItem object.
     *
     * @param event the event
     * @param data the event data, which must hold the <tt>source</tt> object, the <tt>content</tt> of the message, and,
     * optionally, a message <tt>type</tt> and a <tt>parameters</tt> map
     */
    onMessage(event, data)
    {
      if (!(data?.source && data?.content)) {
        return;
      }
      if (data.source._currentMessage) {
        data.source._currentMessage.hide();
      }
      data.source._currentMessage = this.showMessage(data.content, data.type, data.parameters);
    }

    /**
     * Display a feedback message to inform the user about the upload status.
     *
     * @param message the identifier of the message to display, a key in the messages object
     * @param type the type of the notification message, see types supported by XWiki.widgets.Notification
     * @param parameters optional message template parameters
     * @return an XWiki.widgets.Notification object displaying the requested message
     */
    showMessage(message, type, parameters)
    {
      const template = this.messages[message];
      let formattedMessage = message;
      if (typeof template === 'string') {
        formattedMessage = UploadUtils.formatMessage(template, parameters || {});
      } else if (typeof template?.evaluate === 'function') {
        // Support message templates created with Prototype.js' Template (backwards compatibility).
        formattedMessage = template.evaluate(parameters || {});
      }
      return new XWiki.widgets.Notification(formattedMessage, type || "plain");
    }

    /**
     * A function that can be called externally for hiding the form submit buttons.
     * It is suited for forms that only contain only one file input with an HTML5 uploader attached to it.
     */
    hideFormButtons()
    {
      if (!this.form.classList.contains('html5upload-initialized')) {
        this.form.classList.add('html5upload-initialized');
        if (this.options.autoUpload) {
          // Hide submit buttons
          this.form.querySelectorAll('input[type=submit]').forEach(submitButton => {
            submitButton.style.display = 'none';
          });
        }
        const cancelButton = this.form.querySelector('.cancel');
        if (cancelButton) {
          cancelButton.style.display = 'none';
        }
      }
    }
  }
  // This is a global scope variable set for backwards compatibility only.
  // Using XWiki.FileUploader is deprecated.
  // Use requireJS with 'xwiki-upload' instead. For example see history.js .
  // This can be moved to legacy once all uses of XWiki.FileUploader have been removed from XS.
  XWiki.FileUploader = FileUploader;
  return FileUploader;
});
// End JavaScript-only code.
}).apply(']]#', $jsontool.serialize([$icons, $maxAttachmentSize]));
