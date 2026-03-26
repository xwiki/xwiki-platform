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
define('xwiki-requiredrights-messages', {
    prefix: 'security.requiredrights.ui.',
    keys: [
        'modal.label',
        'modal.noEnforceOption',
        'modal.noEnforceOption.hint1',
        'modal.noEnforceOption.hint2',
        'modal.enforceOption',
        'modal.enforceOption.hint1',
        'modal.enforceOption.hint2',
        'modal.unsupportedRights',
        'modal.unsupportedRightItem',
        'modal.rightsSelection',
        'modal.rightsSelection.hint',
        'modal.required',
        'modal.required.hint',
        'modal.maybeRequired',
        'modal.maybeRequired.hint',
        'modal.enough',
        'modal.enough.hint',
        'modal.maybeEnough',
        'modal.maybeEnough.hint',
        'modal.statusHelp',
        'modal.analysisDetails',
        'modal.analysisDetails',
        'modal.contentAndTitle',
        'modal.localizedContentAndTitle',
        'modal.classProperties',
        'modal.property',
        'modal.object',
        'modal.cancel',
        'modal.save',
        'modal.close',
        'saving.inProgress',
        'saving.success',
        'saving.error',
        'contentUpdate.inProgress',
        'contentUpdate.done',
        'contentUpdate.failed',
        'right.script',
        'right.programming',
        'right.admin'
    ]
});

/**
 * Module to handle the Required Rights dialog functionality.
 */
define('xwiki-requiredrights-dialog', [
    'xwiki-meta',
    'jquery',
    'xwiki-l10n!xwiki-requiredrights-messages'
], function (xm, $, l10n) {
    'use strict';

    /**
     * Class to load icons from the XWiki REST API.
     * Creates DOM elements for requested icons.
     */
    class IconLoader {
        /**
         * Creates a new IconLoader instance.
         */
        constructor()
        {
            // Cache for icon data (from API response).
            this.iconCache = {};
        }

        /**
         * Loads a set of icons from the server.
         * @param {string[]} iconNames - Array of icon names to load.
         * @returns {Promise} Promise that resolves when icons are loaded.
         */
        loadIcons(iconNames)
        {
            // Deduplicate icon names
            const uniqueIcons = [...new Set(iconNames)];

            // Filter out already cached icons (either as data or as pending promises)
            const iconsToLoad = uniqueIcons.filter(name => !this.iconCache[name]);

            // If no new icons to load, return a resolved promise
            if (iconsToLoad.length === 0) {
                return Promise.resolve();
            }

            // Build the query string
            const iconQuery = iconsToLoad.map(icon => `name=${icon}`).join('&');
            const encodedWikiName = encodeURIComponent(XWiki.currentWiki);
            const iconURL = `${XWiki.contextPath}/rest/wikis/${encodedWikiName}/iconThemes/icons?${iconQuery}`;

            return fetch(iconURL, {
                headers: {
                    'Accept': 'application/json'
                }
            })
                .then(response => {
                    if (!response.ok) {
                        throw new Error(`Failed to load icons: ${response.status} ${response.statusText}`);
                    }
                    return response.json();
                })
                .then(data => {
                    // Cache the loaded icons
                    data.icons.forEach(icon => {
                        this.iconCache[icon.name] = icon;
                    });
                    return data.icons;
                })
                .catch(error => {
                    console.error('Error loading icons:', error);
                    throw error;
                });
        }

        /**
         * Gets a DOM element for the specified icon.
         * @param {string} iconName - The name of the icon.
         * @returns {HTMLElement} The DOM element for the icon.
         */
        getIconElement(iconName)
        {
            // Throw an error if the icon hasn't been requested.
            if (!this.iconCache[iconName]) {
                throw new Error(`Icon "${iconName}" not loaded. Call loadIcons() first.`);
            }

            if (this.iconCache[iconName].iconSetType === 'IMAGE') {
                const img = document.createElement('img');
                img.src = this.iconCache[iconName].url;
                img.alt = '';
                return img;
            } else if (this.iconCache[iconName].iconSetType === 'FONT') {
                const span = document.createElement('span');
                span.className = this.iconCache[iconName].cssClass;
                span.setAttribute('aria-hidden', 'true');
                return span;
            } else {
                // Fallback for unknown icon type
                const fallback = document.createElement('span');
                fallback.textContent = this.iconCache[iconName].name;
                return fallback;
            }
        }
    }

    // Create a single instance shared by all required rights dialogs.
    const iconLoader = new IconLoader();

    // Remove /translations/{language} from the REST URL if present
    const restURL = xm.restURL.replace(/\/translations\/[^/]+$/, '') + '/requiredRights';

    class RequiredRightsDialog {
        constructor(currentRights)
        {
            this.currentRights = currentRights;
            this.dialogElement = document.createElement('div');
            this.dialogElement.className = 'modal fade';
            this.dialogElement.id = 'required-rights-dialog';
            this.dialogElement.tabIndex = -1;
            this.dialogElement.role = 'dialog';
            this.dialogElement.setAttribute('aria-labelledby', 'required-rights-dialog-label');
            this.dialogElement.innerHTML = `
                        <div class="modal-dialog" role="document">
                            <form class="modal-content">
                                <div class="modal-header">
                                    <button type="button" class="close" data-dismiss="modal"></button>
                                    <h2 class="modal-title" id="required-rights-dialog-label"></h2>
                                </div>
                                <div class="modal-body">
                                    <div class="enforce-selection"></div>
                                    <div class="rights-selection">
                                        <h3></h3>
                                        <p></p>
                                        <ul></ul>
                                    </div>
                                    <div class="required-rights-advanced-toggle-container">
                                        <a href="#" class="required-rights-advanced-toggle"
                                            aria-controls="required-rights-results" aria-expanded="false">
                                            <span class="icon-collapsed"></span>
                                            <span class="icon-expanded"></span>
                                        </a>
                                    </div>
                                    <div id="required-rights-results" class="hidden" 
                                        aria-labelledby="advanced-toggle" aria-expanded="false">
                                    </div>
                                </div>
                                <div class="modal-footer">
                                    <button type="button" class="btn btn-default" data-dismiss="modal"></button>
                                    <button class="btn btn-primary"></button>
                                </div>
                            </form>
                        </div>
                    `;
            this.dialogElement.querySelector('.modal-title').textContent = l10n['modal.label'];
            this.dialogElement.querySelector('.rights-selection h3').textContent = l10n['modal.rightsSelection'];
            this.dialogElement.querySelector('.rights-selection p').textContent = l10n['modal.rightsSelection.hint'];
            this.dialogElement.querySelector('.required-rights-advanced-toggle').append(l10n['modal.analysisDetails']);
            this.dialogElement.querySelector('.btn-default').textContent = l10n['modal.cancel'];
            this.dialogElement.querySelector('.btn-primary').textContent = l10n['modal.save'];
            this.saveButton = this.dialogElement.querySelector('.modal-footer .btn-primary');
            this.formElement = this.dialogElement.querySelector('form.modal-content');
            this.enforceSelectionElement = this.dialogElement.querySelector('.enforce-selection');
            this.advancedToggle = this.dialogElement.querySelector('.required-rights-advanced-toggle');
            this.advancedToggle.querySelector('.icon-collapsed').append(iconLoader.getIconElement('caret-right'));
            this.advancedToggle.querySelector('.icon-expanded').append(iconLoader.getIconElement('caret-down'));
            const closeButton = this.dialogElement.querySelector('button.close');
            closeButton.append(iconLoader.getIconElement('cross'));
            closeButton.ariaLabel = l10n['modal.close'];
            closeButton.title = l10n['modal.close'];
            this.advancedToggleContainer =
                this.dialogElement.querySelector('.required-rights-advanced-toggle-container');
            this.analysisResultsContainer = this.dialogElement.querySelector('#required-rights-results');
            this.rightsList = this.dialogElement.querySelector('.rights-selection ul');

            this.advancedToggle.addEventListener('click', event => {
                event.preventDefault();
                this.toggleAdvanced();
            });
            this.formElement.addEventListener('submit', event => {
                event.preventDefault();
                this.save();
            });
        }

        toggleAdvanced()
        {
            const expanded = this.analysisResultsContainer.classList.toggle('hidden');
            this.advancedToggle.setAttribute('aria-expanded', !expanded);
        }

        save()
        {
            $(this.saveButton).trigger('xwiki:actions:beforeSave');
            // Get the selected right
            const selectedRightInput = this.dialogElement.querySelector('input[name="rights"]:checked');
            const enforceInput = this.dialogElement.querySelector('input[name="enforceRequiredRights"]:checked');

            const updatedData = {
                'enforce': enforceInput.value === '1',
                'rights': []
            }

            const selectedRight = selectedRightInput?.value ?? '';
            if (selectedRight !== '') {
                updatedData.rights.push({'right': selectedRight, 'scope': selectedRightInput?.dataset.scope ?? null});
            }

            const notification = new XWiki.widgets.Notification(l10n['saving.inProgress'], 'inprogress');
            fetch(restURL, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify(updatedData)
            })
                .then(response => {
                    if (!response.ok) {
                        $(this.saveButton).trigger('xwiki:document:saveFailed');
                        notification.replace(
                            new XWiki.widgets.Notification(l10n.get('saving.error', response.statusText), 'error'));
                    } else {
                        response.text().then(updatedRightsJSON => {
                            if (updatedRightsJSON !== JSON.stringify(this.currentRights)) {
                                // Only trigger the events if the rights have actually changed.
                                $(this.saveButton).trigger('xwiki:document:saved');
                                $(this.saveButton).trigger('xwiki:document:requiredRightsUpdated', {
                                    previousRequiredRights: this.currentRights,
                                    savedRequiredRights: JSON.parse(updatedRightsJSON),
                                    documentReference: XWiki.currentDocument.documentReference
                                });
                            }
                            notification.replace(new XWiki.widgets.Notification(l10n['saving.success'], 'done'));
                            // Close the dialog
                            $(this.dialogElement).modal('hide');
                        });
                    }
                })
                .catch(error => {
                    $(this.saveButton).trigger('xwiki:document:saveFailed');
                    const errorMessage = error.message ? error.message : 'Unknown error';
                    notification.replace(
                        new XWiki.widgets.Notification(l10n.get('saving.error', errorMessage), 'error'));
                });
        }

        /**
         * Creates a radio option with description list for rights enforcement options
         * @param {string} labelText - The text to display in the label
         * @param {string} value - The radio button value
         * @param {boolean} checked - Whether this option is checked
         * @param {boolean} disabled - Whether this option is disabled
         * @param {string[]} descriptions - Array of description items to display in a list
         */
        createEnforcementOption(labelText, value, checked, disabled, descriptions)
        {
            const radioLabel = document.createElement('label');
            const radioInput = document.createElement('input');
            radioInput.type = 'radio';
            radioInput.checked = checked;
            radioInput.disabled = disabled;
            radioInput.name = 'enforceRequiredRights';
            radioInput.value = value;
            radioLabel.append(radioInput, ' ', labelText);

            const labelContainer = document.createElement('p');
            labelContainer.appendChild(radioLabel);
            this.enforceSelectionElement.appendChild(labelContainer);

            if (descriptions.length > 0) {
                const descriptionContainer = document.createElement('ul');
                descriptionContainer.className = 'enforce-description';
                descriptions.forEach(text => {
                    const listItem = document.createElement('li');
                    listItem.textContent = text;
                    descriptionContainer.appendChild(listItem);
                });
                this.enforceSelectionElement.appendChild(descriptionContainer);
            }
        }

        /**
         * Adds a right to the list of rights.
         * @param labelText the human-readable name of the right
         * @param value the value of the right
         * @param scope the scope of the right
         * @param checked whether the right is checked
         * @param disabled whether the right is disabled
         * @param status "required" if the right is required according to the analysis, "maybeRequired" if it might be
         * required according to the analysis.
         */
        addRight(labelText, value, scope, checked, disabled, status)
        {
            const listItem = document.createElement('li');
            const labelWrapper = document.createElement('div');
            labelWrapper.classList.add('label-wrapper');
            listItem.appendChild(labelWrapper);
            const labelElement = document.createElement('label');
            labelWrapper.appendChild(labelElement);
            const inputElement = document.createElement('input');
            inputElement.type = 'radio';
            inputElement.name = 'rights';
            inputElement.checked = checked;
            inputElement.value = value;
            inputElement.disabled = disabled;
            inputElement.dataset.scope = scope;
            labelElement.append(inputElement, ' ', labelText);

            if (status !== "") {
                const statusContainer = document.createElement('p');
                const questionMark = document.createElement('button');
                questionMark.className = 'btn btn-default tip';
                questionMark.dataset.toggle = 'tooltip';
                questionMark.type = 'button';
                questionMark.append(iconLoader.getIconElement('question'));
                const srLabel = document.createElement('span');
                srLabel.className = 'sr-only';
                srLabel.textContent = l10n['modal.statusHelp'];
                questionMark.append(srLabel);

                // Convert from camelCase to kebab-case for the class name.
                const className = status.replace(/([A-Z])/g, '-$1').toLowerCase();
                listItem.classList.add(className);
                statusContainer.append(l10n['modal.' + status]);
                questionMark.title = l10n.get('modal.' + status + '.hint', labelText);

                statusContainer.append(' ', questionMark);
                listItem.appendChild(statusContainer);
            }
            this.rightsList.appendChild(listItem);
        }

        addResultsHeading(level, content)
        {
            const headingElement = document.createElement(`h${level}`);
            headingElement.textContent = content;
            this.analysisResultsContainer.appendChild(headingElement);
        }

        addResults(results)
        {
            // Create a unique ID for this group of results
            const groupId = 'required-rights-result-list-' + Math.random().toString(36).substring(2, 9);

            const panelGroup = document.createElement('div');
            panelGroup.className = 'panel-group';
            panelGroup.id = groupId;
            panelGroup.setAttribute('role', 'tablist');
            panelGroup.setAttribute('aria-multiselectable', 'true');

            results.forEach((result, index) => {
                const panelId = `${groupId}-${index}`;
                const panel = document.createElement('div');
                panel.className = 'panel panel-default';
                panel.innerHTML = `
                                <div class="panel-heading" role="tab" id="heading-${panelId}">
                                    <span class="panel-title">
                                        <a role="button"
                                        data-toggle="collapse"
                                        data-parent="#${groupId}"
                                        href="#collapse-${panelId}"
                                        aria-expanded="false"
                                        aria-controls="collapse-${panelId}">
                                            <span class="icon-collapsed"></span>
                                            <span class="icon-expanded"></span>
                                            ${result.summaryMessageHTML}
                                        </a>
                                    </span>
                                </div>
                                <div id="collapse-${panelId}" class="panel-collapse collapse" role="tabpanel" 
                                    aria-labelledby="heading-${panelId}">
                                    <div class="panel-body">
                                        ${result.detailedMessageHTML}
                                    </div>
                                </div>
                            `;
                const panelToggle = panel.querySelector('.panel-title a');
                panelToggle.querySelector('.icon-collapsed').append(iconLoader.getIconElement('caret-right'));
                panelToggle.querySelector('.icon-expanded').append(iconLoader.getIconElement('caret-down'));

                panelGroup.appendChild(panel);
            });

            this.analysisResultsContainer.appendChild(panelGroup);
        }
    }

    return {
        /**
         * Load and display the required rights dialog
         * @return {Promise} A promise that resolves when the dialog is shown
         */
        show: async function () {
            const iconsPromise = iconLoader.loadIcons(['question', 'cross', 'caret-right', 'caret-down']);

            const response = await fetch(restURL);
            const data = await response.json();
            // Create a bootstrap dialog to display the results
            const currentRights = data.currentRights;
            const availableRights = data.availableRights;

            // Wait for the icons to be loaded before displaying the dialog.
            await iconsPromise;
            const dialog = new RequiredRightsDialog(currentRights);

            let indexOfCurrentRight = 0;
            const unsupportedRights = [];
            for (const right of currentRights.rights) {
                const rightIndex = availableRights.findIndex(r => r.right === right.right && r.scope === right.scope);
                if (rightIndex !== -1) {
                    // Keep the one with the highest index, which should be the most powerful right.
                    if (rightIndex > indexOfCurrentRight) {
                        indexOfCurrentRight = rightIndex;
                    }
                } else {
                    unsupportedRights.push(right);
                }
            }

            const currentRight = availableRights[indexOfCurrentRight];

            if (unsupportedRights.length > 0) {
                const warningBox = document.createElement('div');
                warningBox.className = 'box warningmessage';
                const warningContent = document.createElement('div');
                warningBox.appendChild(warningContent);
                const warningParagraph = document.createElement('p');
                warningContent.appendChild(warningParagraph);
                warningParagraph.textContent = l10n['modal.unsupportedRights'];
                const unsupportedRightsList = document.createElement('ul');
                warningContent.appendChild(unsupportedRightsList);
                unsupportedRights.forEach(right => {
                    const listItem = document.createElement('li');
                    listItem.textContent = l10n.get('modal.unsupportedRightItem', right.right, right.scope);
                    unsupportedRightsList.appendChild(listItem);
                });
                dialog.enforceSelectionElement.appendChild(warningBox);
            }

            // Create the "Don't enforce" option.
            dialog.createEnforcementOption(
                l10n['modal.noEnforceOption'],
                '0',
                !data.currentRights.enforce,
                !availableRights[0].hasRight,
                [
                    l10n['modal.noEnforceOption.hint1'],
                    l10n['modal.noEnforceOption.hint2']
                ]
            );

            // Create the "Enforce" option.
            dialog.createEnforcementOption(
                l10n['modal.enforceOption'],
                '1',
                data.currentRights.enforce,
                !availableRights[0].hasRight,
                [
                    l10n['modal.enforceOption.hint1'],
                    l10n['modal.enforceOption.hint2']
                ]
            );

            // Display a nice visualization that shows the rights "Edit", "Script", "Wiki Admin" and "Programming" with the current right highlighted if it isn't null.
            availableRights.forEach(right => {
                const checked = currentRight.right === right.right && currentRight.scope === right.scope;
                let status = '';
                if (right.right === '' && right.definitelyRequiredRight) {
                    // Check if there is any right that is maybe required.
                    if (availableRights.some(r => r.maybeRequiredRight)) {
                        status = 'maybeEnough';
                    } else {
                        status = 'enough';
                    }
                } else if (right.definitelyRequiredRight) {
                    status = 'required';
                } else if (right.maybeRequiredRight) {
                    status = 'maybeRequired';
                }
                dialog.addRight(right.displayName, right.right, right.scope, checked, !right.hasRight, status);
            });

            // Display the analysis results.
            // First, resolve the entity reference.
            const analysisResults = data.analysisResults.map(result => {
                // The client-side resolver isn't fully compatible with the entity references generated in Java.
                // This should be removed when https://jira.xwiki.org/browse/XWIKI-22869 is fixed.
                result.entityReference =
                    XWiki.Model.resolve(result.entityReference.replace(/^(object|class)_property/, '$1Property'));
                return result;
            });

            function groupBy(result, propertyExtractor)
            {
                const grouped = {};

                for (let i = 0; i < result.length; i++) {
                    const item = result[i];
                    const value = propertyExtractor(item);

                    if (Array.isArray(value)) {
                        let current = grouped;

                        for (let j = 0; j < value.length; j++) {
                            const key = value[j];
                            const isLastKey = j === value.length - 1;

                            if (!current[key]) {
                                current[key] = isLastKey ? [] : {};
                            }

                            current = current[key];
                        }

                        current.push(item);
                    } else {
                        if (!grouped[value]) {
                            grouped[value] = [];
                        }

                        grouped[value].push(item);
                    }
                }

                return grouped;
            }

            // Display the results where the entity type is DOCUMENT
            const documentResults = analysisResults.filter(
                result => result.entityReference.type === XWiki.EntityType.DOCUMENT);

            // Group the results by locale.
            const documentResultsByLocale = groupBy(documentResults, result => result.locale ?? '');
            // Display the results in the dialog. Start with the empty string locale, if any.
            if (documentResultsByLocale['']) {
                dialog.addResultsHeading(3, l10n['modal.contentAndTitle']);
                dialog.addResults(documentResultsByLocale['']);
                delete documentResultsByLocale[''];
            }
            // Display the results for each locale.
            for (const locale in documentResultsByLocale) {
                dialog.addResultsHeading(3, l10n.get('modal.localizedContentAndTitle', locale));
                dialog.addResults(documentResultsByLocale[locale]);
            }
            // Display results of type CLASS_PROPERTY, if any.
            const classPropertyResults = analysisResults.filter(
                result => result.entityReference.type === XWiki.EntityType.CLASS_PROPERTY);
            if (classPropertyResults.length > 0) {
                dialog.addResultsHeading(3, l10n['modal.classProperties']);

                // Group the results by property name
                const classPropertyResultsByProperty = groupBy(classPropertyResults,
                    result => result.entityReference.name);

                for (const propertyName in classPropertyResultsByProperty) {
                    dialog.addResultsHeading(4, l10n.get('modal.property', propertyName));
                    dialog.addResults(classPropertyResultsByProperty[propertyName]);
                }
            }
            // Display objects and their properties. Group the results by XClass name and object index.
            // Consider both results of type OBJECT and OBJECT_PROPERTY.
            const objectResults = analysisResults.filter(
                result => result.entityReference.type === XWiki.EntityType.OBJECT
                    || result.entityReference.type === XWiki.EntityType.OBJECT_PROPERTY);
            if (objectResults.length > 0) {
                // Group the results by XClass name and object index
                const objectResultsByXClassAndObject = groupBy(objectResults, result => {
                    let xClass;
                    if (result.entityReference.type === XWiki.EntityType.OBJECT) {
                        xClass = result.entityReference.name;
                    } else {
                        xClass = result.entityReference.parent.name;
                    }
                    // xClass is of them form 'ClassName[objectIndex]'. Extract the class name and object index as int.
                    const match = xClass.match(/^(.*)\[(\d+)]$/);
                    return [match[1], parseInt(match[2])];
                });

                for (const xClassName in objectResultsByXClassAndObject) {
                    for (const objectIndex in objectResultsByXClassAndObject[xClassName]) {
                        dialog.addResultsHeading(3, l10n.get('modal.object', xClassName, objectIndex));

                        // First display results of type OBJECT
                        const objectResults = objectResultsByXClassAndObject[xClassName][objectIndex].filter(
                            result => result.entityReference.type === XWiki.EntityType.OBJECT);
                        dialog.addResults(objectResults);

                        // Then display results of type OBJECT_PROPERTY, grouped by property name.
                        const objectPropertyResults = objectResultsByXClassAndObject[xClassName][objectIndex].filter(
                            result => result.entityReference.type === XWiki.EntityType.OBJECT_PROPERTY);

                        const objectPropertyResultsByProperty = groupBy(objectPropertyResults,
                            result => result.entityReference.name);

                        for (const propertyName in objectPropertyResultsByProperty) {
                            dialog.addResultsHeading(4, l10n.get('modal.property', propertyName));
                            dialog.addResults(objectPropertyResultsByProperty[propertyName]);
                        }
                    }
                }
            }

            // Hide the toggle for the details if there are no details.
            if (!analysisResults.length) {
                dialog.advancedToggleContainer.hidden = true;
            }

            document.body.appendChild(dialog.dialogElement);

            // Remove the dialog from the DOM when it has been closed.
            $(dialog.dialogElement).on('hidden.bs.modal', () => {
                dialog.dialogElement.remove();
            });

            // Enable the tooltips
            $(dialog.dialogElement).find('[data-toggle="tooltip"]').tooltip({'trigger': 'hover focus click'});

            // Display the dialog
            $(dialog.dialogElement).modal('show');
        }
    };
});

require(['jquery', 'xwiki-requiredrights-dialog'], function ($, dialog) {
    const selector = 'button[data-xwiki-requiredrights-dialog="show"]';

    function init(root)
    {
        $(root).find(selector).prop('disabled', false);
    }

    $(function () {
        $(document).on('click', 'button[data-xwiki-requiredrights-dialog="show"]', function (event) {
            event.preventDefault();
            dialog.show();
        });

        init(document);

        $(document).on('xwiki:dom:updated', (event, data) => {
            data.elements.forEach(init);
        });
    });
});

require(['jquery', 'xwiki-l10n!xwiki-requiredrights-messages'], function ($, l10n) {
    $(document).on('xwiki:document:requiredRightsUpdated.viewMode', function (event, data) {
        const contentWrapper = $('#xwikicontent').not('[contenteditable]');
        if (contentWrapper.length && XWiki.currentDocument.documentReference.equals(data.documentReference)) {
            const notification = new XWiki.widgets.Notification(l10n['contentUpdate.inProgress'], 'inprogress');
            return loadContent().then(output => {
                // Update the displayed document title and content.
                $('#document-title h1').html(output.renderedTitle);
                contentWrapper.html(output.renderedContent);
                // Let others know that the DOM has been updated, in order to enhance it.
                $(document).trigger('xwiki:dom:updated', {'elements': contentWrapper.toArray()});
                notification.replace(new XWiki.widgets.Notification(l10n['contentUpdate.done'], 'done'));
            }).catch(() => {
                notification.replace(new XWiki.widgets.Notification(l10n['contentUpdate.failed'], 'error'));
            });
        }

        function loadContent()
        {
            const data = {
                // Get only the document content and title (without the header, footer, panels, etc.)
                xpage: 'get',
                // The displayed document title can depend on the rights.
                outputTitle: 'true'
            };
            return $.get(XWiki.currentDocument.getURL('view'), new URLSearchParams(data).toString())
                .then(function (html) {
                    // Extract the rendered title and content.
                    const container = $('<div></div>').html(html);
                    return {
                        renderedTitle: container.find('#document-title h1').html(),
                        renderedContent: container.find('#xwikicontent').html()
                    };
                });
        }
    });
});