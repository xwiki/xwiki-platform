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
define('xwiki-realtime-toolbar', [
  'jquery',
  'xwiki-realtime-config',
  'xwiki-realtime-document',
  'moment',
  'moment-jdateformatparser'
], function($, realtimeConfig, xwikiDocument, moment) {
  'use strict';

  class Toolbar {
    constructor(config) {
      this._config = {
        save: () => {},
        leave: () => {},
        selectUser: (userId) => {},
        ...config
      };

      // Create the toolbar.
      const toolbarTemplate = document.querySelector('template#realtime-edit-toolbar');
      this._toolbar = toolbarTemplate.content.querySelector('.realtime-edit-toolbar').cloneNode(true);

      this._dateFormat = moment().toMomentFormatString(realtimeConfig.dateFormat || 'yyyy/MM/dd HH:mm');

      // Replace the old toolbar.
      this._oldToolbar = document.querySelector('.bottombuttons.sticky-buttons > .buttons,' +
        // The old toolbar is moved when editing fullscreen with the standalone editor.
        ' .cke_maximized > .buttons,' +
        ' .inplace-editing-buttons.sticky-buttons > .buttons');
      // Inherit some styles from the old toolbar.
      this._toolbar.classList.add(...this._oldToolbar.classList);
      this._oldToolbar.before(this._toolbar);
      this._oldToolbar.hidden = true;

      if (this._toolbar.querySelector('.realtime-action-summarize')) {
        this._createChangeSummaryModal();
      }
      this._createVersionModal();
      this._createLeaveModal();
      this._activateDoneButton();

      this.onConnectionStatusChange(1 /* connecting */);
    }

    _activateDoneButton() {
      this._doneButton = this._toolbar.querySelector('.realtime-action-done');
      if (this._doneButton.dataset.toggle === 'modal') {
        // The Done button opens the Summarize & Done modal. This happens when version summary is mandatory.
        return;
      }
      // The Done button saves the document directly.
      this._doneButton.addEventListener('click', event => {
        event.preventDefault();
        // FIXME: We can't rely on the save status to determine whether to save or cancel (e.g. to prevent creating a
        // new version when there are no changes and the document is not new) because:
        // * the save status takes into account only the fields that are synchronized in realtime
        // * not all form fields are synchronized
        // The document title is currently not synchronized, so when someone changes the title the others don't know
        // that the document is dirty and thus has to be saved.
        // We should catch changes on all non-hidden form fields (that the user can change) and synchronize them. Once
        // we do this we can use the following code:
        //
        // const saveStatus = this._toolbar.querySelector('.realtime-save-status').getAttribute('value');
        // if (saveStatus === 'clean' && !xwikiDocument.isNew) {
        //   // Leave without saving in order to avoid creating a new version without changes.
        //   this._oldToolbar.querySelector('[name="action_cancel"]').click();
        // } else {
        //   this._config.save();
        // }
        //
        this._config.save();
      });
    }

    _createLeaveModal() {
      let leaveModal = document.querySelector('#realtime-leave-modal');
      if (!leaveModal) {
        const template = document.querySelector('template#realtime-leave-modal-template');
        leaveModal = template.content.querySelector('#realtime-leave-modal').cloneNode(true);
        document.body.appendChild(leaveModal);
      }

      const leaveButton = $(leaveModal).find('.modal-footer .btn-primary');
      // The autofocus HTML attribute has no effect in Bootstrap modals.
      $(leaveModal).off('shown.bs.modal.realtime').on('shown.bs.modal.realtime', () => {
        leaveButton.trigger('focus');
      });
      leaveButton.off('click.realtime').on('click.realtime', () => {
        this._config.leave();
      });
    }

    _createChangeSummaryModal() {
      this._changeSummaryModal = document.querySelector('#realtime-changeSummaryModal');
      if (!this._changeSummaryModal) {
        const template = document.querySelector('template#realtime-changeSummaryModal-template');
        this._changeSummaryModal = template.content.querySelector('#realtime-changeSummaryModal').cloneNode(true);
        document.body.appendChild(this._changeSummaryModal);
      }
  
      const changesTab = this._changeSummaryModal.querySelector('#realtime-changeSummaryModal-changesTab');
      $(this._changeSummaryModal).off('show.bs.modal.realtime').on('show.bs.modal.realtime', event => {
        this._changeSummaryModal.dataset.continue = event.relatedTarget.dataset.continue;
        this._changeSummaryModal.dataset.previousVersion = this._lastReviewedVersion;

        // Select the summary tab.
        $(this._changeSummaryModal.querySelector('a[aria-controls="realtime-changeSummaryModal-summaryTab"]'))
          .tab('show');
        // Force a reload next time the user activates the changes tab.
        changesTab.dataset.state = '';
      });
  
      const changeSummaryTextArea = this._changeSummaryModal.querySelector('textarea[name=summary]');
      $(this._changeSummaryModal).off('shown.bs.modal.realtime').on('shown.bs.modal.realtime', event => {
        changeSummaryTextArea.focus();
        changeSummaryTextArea.select();
      });

      const changesTabToggle = this._changeSummaryModal
        .querySelector('a[aria-controls="realtime-changeSummaryModal-changesTab"]');
      $(changesTabToggle).off('show.bs.tab.realtime').on('show.bs.tab.realtime', event => {
        if (!changesTab.dataset.state) {
          this._loadChanges(changesTab);
        }
      });
  
      this._summarizeSubmit = this._changeSummaryModal.querySelector('.modal-footer button.btn-primary');
      $(this._summarizeSubmit).off('click.realtime').on('click.realtime', event => {
        event.preventDefault();
        this._saveChangeSummary();
      });
    }

    async _loadChanges(changesTab) {
      changesTab.dataset.state = 'loading';
      if (xwikiDocument.isNew) {
        changesTab.dataset.state = 'isNew';
      } else {
        const previousVersion = this._changeSummaryModal.dataset.previousVersion;

        try {
          const html = await this._fetchChanges(previousVersion);
          const wrapper = document.createElement('div');
          wrapper.innerHTML = html;
          const diff = wrapper.querySelector('.diff-group');
          if (diff) {
            changesTab.dataset.state = 'loaded';
            changesTab.querySelector('.diff-group').replaceWith(diff);
          } else {
            changesTab.dataset.state = 'empty';
          }
        } catch (error) {
          changesTab.dataset.state = 'error';
          console.error(error);
        }
      }
    }

    async _fetchChanges(previousVersion) {
      const formData = this._getFormData();
      formData.set('rev1', previousVersion);
      formData.set('xpage', 'changesinline');
      // We have to use the edit URL in order to compare the previous version with the unsaved version.
      const changesURL = xwikiDocument.getURL('edit');

      const response = await fetch(changesURL, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
          'X-Requested-With': 'XMLHttpRequest'
        },
        body: formData
      });

      if (!response.ok) {
        throw new Error(`Response status: ${response.status}`);
      }

      return await response.text();
    }

    _getFormData() {
      // Notify the others that the form is about to be submitted.
      $(this._toolbar).trigger('xwiki:actions:beforePreview');
      const formData = new FormData(this.getForm());
      return new URLSearchParams(formData);
    }

    async _saveChangeSummary() {
      // Select the summary tab in order to be able to show validation errors, e.g. when the summary is mandatory.
      $(this._changeSummaryModal.querySelector('a[aria-controls="realtime-changeSummaryModal-summaryTab"]'))
        .tab('show');

      const changeSummaryTextArea = this._changeSummaryModal.querySelector('textarea[name=summary]');
      if (!changeSummaryTextArea.reportValidity()) {
        // The provided change summary is not valid.
        return;
      }

      const commentInput = this._oldToolbar.querySelector('input[name="comment"]');
      const minorChangeCheckbox = this._changeSummaryModal.querySelector('input[name="minorChange"]');
      const continueEditing = this._changeSummaryModal.dataset.continue === 'true';
      // Put the change summary and the minor edit checkbox in the form.
      commentInput.value = changeSummaryTextArea.value;
      if (!xwikiDocument.isNew && minorChangeCheckbox) {
        const minorEditCheckbox = this._oldToolbar.querySelector('input[name="minorEdit"]');
        minorEditCheckbox.checked = minorChangeCheckbox.checked;
      }
      $(this._changeSummaryModal).modal('hide');

      const lastReviewedVersion = this._lastReviewedVersion;
      delete this._lastReviewedVersion;
      try {
        await this._config.save(continueEditing);
      } catch (error) {
        this._lastReviewedVersion = lastReviewedVersion;
        console.log(error);
      }
    }

    _createVersionModal() {
      this._versionModal = document.querySelector('#realtime-version-modal');
      if (!this._versionModal) {
        const template = document.querySelector('template#realtime-version-modal-template');
        this._versionModal = template.content.querySelector('#realtime-version-modal').cloneNode(true);
      }

      $(this._versionModal).off('show.bs.modal.realtime').on('show.bs.modal.realtime', event => {
        let version = event.relatedTarget.dataset.version;
        this._versionModal.dataset.version = version;
        version = JSON.parse(version);

        this._versionModal.querySelector('.modal-title .realtime-version-number').textContent = version.number;
        this._versionModal.querySelector('.modal-title .realtime-version-date').textContent = moment(version.date)
          .format(this._dateFormat);

        const versionAuthor = this._versionModal.querySelector('.realtime-version-author');
        versionAuthor.innerHTML = '';
        const userElement = this._displayUser(version.author).firstChild;
        $(userElement).off('click.realtime');
        versionAuthor.appendChild(userElement);

        this._versionModal.querySelector('realtime-document-viewer').setAttribute('version', version.number);
      });

      // Attach the modal after setting the version in order to avoid requesting the latest version.
      if (!this._versionModal.parentElement) {
        document.body.appendChild(this._versionModal);
      }
    }

    onSaveStatusChange(status) {
      this._setStatus('.realtime-save-status', status);
      // Prevent the user from saving while the document is being saved.
      this._disableSaveTriggersIf(status === 1);
    }

    _disableSaveTriggersIf(condition) {
      // Disable the Done button only if it doesn't open a modal, which is the case when version summary is mandatory.
      if (this._doneButton.dataset.toggle !== 'modal') {
        this._doneButton.disabled = condition;
      }
      // The summarize action is not available if version summaries are disabled from the wiki administration.
      if (this._summarizeSubmit) {
        this._summarizeSubmit.disabled = condition;
      }
    }

    onCreateVersion(version) {
      if (!this._lastReviewedVersion) {
        this._lastReviewedVersion = version.number;
      }
      // Make sure we don't add the same version twice. This can happen if some of the previous versions were deleted
      // and their version numbers are being reused.
      const versions = [...this._toolbar.querySelectorAll('.realtime-version')].filter(existingVersion => {
        if (JSON.parse(existingVersion.dataset.version).number === version.number) {
          // The version element is wrapped in a list item element, that we need to remove as well.
          existingVersion.parentElement.remove();
          return false;
        }
        return true;
      });
      // Limit the number of versions shown in the dropdown.
      const limit = Number.parseInt(this._toolbar.querySelector('.realtime-versions').dataset.limit) || 5;
      if (versions.length >= limit) {
        // The version element is wrapped in a list item element, that we need to remove as well.
        versions[0].parentElement.remove();
      }
      // Insert the new version.
      const versionWrapper = this._createVersionElement(version);
      this._toolbar.querySelector('.realtime-versions > .divider').before(versionWrapper);
    }

    _createVersionElement(version) {
      const template = document.querySelector('template#realtime-version');
      const versionElement = template.content.querySelector('.realtime-version').cloneNode(true);
      versionElement.dataset.version = JSON.stringify(version);
      versionElement.href = xwikiDocument.getURL('view', $.param({
        'rev': version.number,
        'language': xwikiDocument.language
      }));
      versionElement.querySelector('.realtime-version-number').textContent = version.number;
      versionElement.querySelector('.realtime-version-date').textContent = moment(version.date)
        .format(this._dateFormat);

      // Replace the version icon with the version author, if author details are available.
      if (version.author.reference) {
        const versionIcon = versionElement.querySelector('.realtime-version-icon');
        versionIcon.replaceWith(this._createVersionAuthorElement(version.author));
      }

      const wrapper = document.createElement('li');
      wrapper.appendChild(versionElement);
      return wrapper;
    }

    _createVersionAuthorElement(author) {
      const userElement = this._displayUser(author, true).firstChild;
      const authorElement = document.createElement('span');
      authorElement.append(...userElement.childNodes);
      userElement.getAttributeNames().forEach(attributeName => {
        if (attributeName !== 'href') {
          authorElement.setAttribute(attributeName, userElement.getAttribute(attributeName));
        }
      });
      return authorElement;
    }

    onConnectionStatusChange(status, userId) {
      this._setStatus('.realtime-connection-status', status);
      const notConnected = status !== 2 /* connected */;
      this._disableSaveTriggersIf(notConnected);
      if (notConnected) {
        this.onUserListChange([]);
      }
      if (userId) {
        this._toolbar.dataset.userId = userId;
      }
    }

    _setStatus(selector, status) {
      const values = ['dirty', 'cleaning', 'clean'];
      this._toolbar.querySelector(selector).setAttribute('value', values[status]);
    }

    onUserListChange(users) {
      // The user data we receive also holds information that is not displayed on the toolbar, most notably the caret
      // position of each user, which changes very often. Skip the update when nothing we display has changed, in order
      // to avoid needless DOM churn. Recreating the coeditor elements every few seconds makes them visibly flicker
      // (they briefly disappear and re-appear, both on the toolbar and in an open users dropdown), dismisses the
      // avatar tooltip while it is being hovered, and invalidates any reference held on them (e.g. by the functional
      // tests).
      const signature = JSON.stringify(users.map(user => [user.id, user.reference, user.name, user.avatar]));
      if (signature === this._userListSignature) {
        return;
      }
      this._userListSignature = signature;

      const usersWrapper = this._toolbar.querySelector('.realtime-users');
      const limit = Number.parseInt(usersWrapper.dataset.limit) || 4;
      this._updateUserList(usersWrapper, users.slice(0, limit), true);

      const usersDropdown = this._toolbar.querySelector('.realtime-users-dropdown');
      usersDropdown.hidden = users.length <= limit;
      if (!usersDropdown.hidden) {
        usersDropdown.querySelector('.dropdown-toggle').dataset.more = users.length - limit;
        this._updateUserList(usersDropdown.querySelector('.dropdown-menu'), users);
      }
    }

    _updateUserList(listElement, users, compact = false) {
      const existingWrappers = new Map();
      listElement.querySelectorAll(':scope > li > .realtime-user').forEach(userElement => {
        existingWrappers.set(userElement.dataset.id, userElement.parentElement);
      });

      // Reuse the element of a user that is already listed, rather than recreating it, so that its avatar is not
      // reloaded and so that the references others may hold on it (e.g. the functional tests) remain valid.
      let nextNode = listElement.firstChild;
      users.forEach(user => {
        let wrapper = existingWrappers.get(user.id);
        if (wrapper) {
          existingWrappers.delete(user.id);
          this._updateUser(wrapper.firstElementChild, user);
        } else {
          wrapper = this._displayUser(user, compact);
        }
        if (wrapper === nextNode) {
          nextNode = nextNode.nextSibling;
        } else {
          listElement.insertBefore(wrapper, nextNode);
        }
      });

      // Whatever is left after the last user we placed is either a user that left the editing session or some leftover
      // from the template (e.g. the placeholder list item).
      while (nextNode) {
        const nodeToRemove = nextNode;
        nextNode = nextNode.nextSibling;
        nodeToRemove.remove();
      }
    }

    _displayUser(user, compact = false) {
      const template = document.querySelector('template#realtime-user');
      const userElement = template.content.querySelector('.realtime-user').cloneNode(true);
      if (compact) {
        userElement.classList.add('realtime-user-compact');
      }
      this._updateUser(userElement, user);

      const wrapper = document.createElement('li');
      wrapper.appendChild(userElement);
      return wrapper;
    }

    _updateUser(userElement, user) {
      userElement.href = new XWiki.Document(XWiki.Model.resolve(user.reference, XWiki.EntityType.DOCUMENT)).getURL();
      userElement.dataset.id = user.id;
      userElement.dataset.reference = user.reference;
      $(userElement).off('click.realtime').on('click.realtime', event => {
        event.preventDefault();
        this._config.selectUser(user.id);
      });

      const avatar = userElement.querySelector('.realtime-user-avatar');
      // Fall back on the default avatar if there's no avatar specified.
      avatar.setAttribute('src', user.avatar || this._getDefaultAvatarURL());
      avatar.alt = user.name;
      avatar.title = user.name;
      avatar.parentElement.dataset.abbr = this._getUserNameAbbreviation(user.name);

      userElement.querySelector('.realtime-user-name').textContent = user.name;
    }

    _getDefaultAvatarURL() {
      if (this._defaultAvatarURL === undefined) {
        const template = document.querySelector('template#realtime-user');
        this._defaultAvatarURL = template.content.querySelector('.realtime-user-avatar').getAttribute('src');
      }
      return this._defaultAvatarURL;
    }

    _getUserNameAbbreviation(name) {
      const names = name.split(/\s+/);
      if (names.length === 1) {
        return names[0].substring(0, 1).toUpperCase() + names[0].substring(1, 2).toLowerCase();
      } else {
        // We assume that the first and the last names are the most important.
        const firstName = names[0];
        const lastName = names.at(-1);
        return firstName[0].toUpperCase() + lastName[0].toUpperCase();
      }
    }

    getForm() {
      return this._oldToolbar.closest('form, .form, body');
    }

    destroy() {
      this._oldToolbar.hidden = false;
      this._toolbar.remove();
      this._changeSummaryModal?.remove();
    }
  }

  class Spinner extends HTMLElement {
    constructor() {
      super();

      const shadowRoot = this.attachShadow({mode: 'open'});
      const template = document.querySelector('template#realtime-spinner');
      shadowRoot.appendChild(template.content.cloneNode(true));
    }
  }
  customElements.define("realtime-spinner", Spinner);

  class Status extends HTMLElement {
    constructor() {
      super();

      const shadowRoot = this.attachShadow({mode: 'open'});
      const template = document.querySelector('template#realtime-status');
      shadowRoot.appendChild(template.content.cloneNode(true));
    }
  }
  customElements.define("realtime-status", Status);

  class DocumentViewer extends HTMLElement {
    static get observedAttributes() {
      return ['reference', 'locale', 'version'];
    }

    constructor() {
      super();

      this.attachShadow({mode: 'open'});
    }

    connectedCallback() {
      this._fetchDocument();
    }

    attributeChangedCallback(name, oldValue, newValue) {
      if (this.isConnected) {
        this._fetchDocument();
      }
    }

    async _fetchDocument() {
      this.shadowRoot.innerHTML = '';
      this.shadowRoot.appendChild(document.createElement('realtime-spinner'));

      const reference = XWiki.Model.resolve(this.getAttribute('reference'), XWiki.EntityType.DOCUMENT);
      const locale = this.getAttribute('locale');
      const version = this.getAttribute('version');
      const url = new XWiki.Document(reference).getURL('view', $.param({
        xpage: 'displaycontent',
        htmlHeaderAndFooter: true,
        outputTitle:true,
        language: locale,
        rev: version
      }));

      try {
        const response = await fetch(url);
        if (!response.ok) {
          throw new Error(response.statusText);
        }
        const html = await response.text();
        this.shadowRoot.innerHTML = html;
      } catch (error) {
        console.error(error);
        const alt = this.getAttribute('alt');
        if (alt) {
          this.shadowRoot.innerHTML = '';
          this.shadowRoot.appendChild(document.createTextNode(alt));
        }
      }
    }
  }
  customElements.define("realtime-document-viewer", DocumentViewer);

  return Toolbar;
});
