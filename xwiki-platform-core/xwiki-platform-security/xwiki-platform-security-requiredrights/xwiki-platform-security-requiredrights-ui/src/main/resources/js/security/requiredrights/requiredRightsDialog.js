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
    prefix: 'core.viewers.information.requiredRights.',
    keys: [
        'modal.label',
        'enforcedNoRight',
        'enforced',
        'notEnforced',
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

    class RequiredRightsDialog {
        constructor()
        {
            this.dialogElement = document.createElement('div');
            this.dialogElement.className = 'modal fade';
            this.dialogElement.id = 'requiredRightsDialog';
            this.dialogElement.tabIndex = -1;
            this.dialogElement.role = 'dialog';
            this.dialogElement.setAttribute('aria-labelledby', 'required-rights-dialog-label');
            this.dialogElement.setAttribute('aria-hidden', 'true');
            this.dialogElement.innerHTML = `
                        <div class="modal-dialog" role="document">
                            <div class="modal-content">
                                <div class="modal-header">
                                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                                        <span aria-hidden="true">&times;</span>
                                    </button>
                                    <!-- TODO: translate! -->
                                    <h2 class="modal-title" id="required-rights-dialog-label">Required Rights</h2>
                                </div>
                                <div class="modal-body">
                                    <div class="current-rights"></div>
                                    <div>
                                        <a href="#" class="required-rights-advanced-toggle"
                                            aria-controls="required-rights-results" aria-expanded="false">
                                        <!-- TODO: localize and use icon theme. -->
                                        <span class="toggle-expand">
                                            <i class="fa fa-caret-right"></i>
                                            Show Details
                                        </span>
                                        <span class="toggle-collapse hidden">
                                            <i class="fa fa-caret-down"></i>
                                            Hide Details
                                        </span>
                                        </a>
                                    </div>
                                    <div id="required-rights-results" class="hidden" 
                                        aria-labelledby="advanced-toggle" aria-expanded="false">
                                    </div>
                                </div>
                                <div class="modal-footer">
                                    <button type="button" class="btn btn-secondary" data-dismiss="modal">Cancel</button>
                                    <button type="button" class="btn btn-primary">Save</button>
                                </div>
                            </div>
                        </div>
                    `;
            this.saveButton = this.dialogElement.querySelector('.modal-footer .btn-primary');
            this.currentRightsElement = this.dialogElement.querySelector('.current-rights');
            this.advancedToggle = this.dialogElement.querySelector('.required-rights-advanced-toggle');
            this.analysisResultsContainer = this.dialogElement.querySelector('#required-rights-results');

            this.advancedToggle.addEventListener('click', event => {
                event.preventDefault();
                this.toggleAdvanced();
            });
            this.saveButton.addEventListener('click', this.save.bind(this));
        }

        toggleAdvanced()
        {
            const expanded = this.analysisResultsContainer.classList.toggle('hidden');
            this.advancedToggle.setAttribute('aria-expanded', !expanded);
            this.advancedToggle.querySelector('.toggle-expand').classList.toggle('hidden');
            this.advancedToggle.querySelector('.toggle-collapse').classList.toggle('hidden');
        }

        save()
        {
            // Get the selected right
            const selectedRightInput = this.dialogElement.querySelector('input[name="rights"]:checked');

            const formData = {
                enforceRequiredRights: selectedRightInput?.dataset.xwikiEnforceRequiredRights === 'false' ? '0' : '1',
                form_token: xm.form_token
            };

            const selectedRight = selectedRightInput?.value ?? '';
            if (selectedRight === '') {
                // Delete the first object.
                formData['deletedObjects'] = 'XWiki.RequiredRightClass_0';
            } else {
                formData['addedObjects'] = 'XWiki.RequiredRightClass_0';
                // TODO: make this "wiki_admin" more generic.
                formData['XWiki.RequiredRightClass_0_level'] =
                    selectedRight === 'admin' ? 'wiki_admin' : selectedRight;
            }
            const url = new XWiki.Document(xm.documentReference).getURL('save');
            const notification = new XWiki.widgets.Notification(
                l10n['core.editors.saveandcontinue.notification.inprogress'], 'inprogress');
            $.post(url, formData)
                .then(() => {
                    // TODO: trigger an event editor.trigger('xwiki:document:saved');
                    notification.replace(new XWiki.widgets.Notification(
                        l10n['core.editors.saveandcontinue.notification.done'],
                        'done'));
                    // Close the dialog
                    $(this.dialogElement).modal('hide');
                }).catch(response => {
                // TODO: trigger an event editor.trigger('xwiki:document:saveFailed');
                notification.replace(new XWiki.widgets.Notification(
                    l10n.get('core.editors.saveandcontinue.notification.error', response.statusText),
                    'error'));
                return Promise.reject();
            });
        }

        addResultsHeading(level, content)
        {
            const headingElement = document.createElement(`h${level}`);
            headingElement.textContent = content;
            this.analysisResultsContainer.appendChild(headingElement);
        }

        addResults(results)
        {
            // Create a unique counter for this group of results
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
                                        <!-- TODO: icons from the API? -->
                                            <span class="icon-collapsed"><i class="fa fa-caret-right"></i></span>
                                            <span class="icon-expanded"><i class="fa fa-caret-down"></i></span>
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
            // Remove /translations/{language} from the REST URL if present
            const restURL = xm.restURL.replace(/\/translations\/[^/]+$/, '') + '/requiredRights';

            const response = await fetch(restURL);
            const data = await response.json();
            // Example response: {"analysisResults":[{"entityReference":"document:xwiki:Macro Analyzer.WebHome","locale":"","summaryMessageHTML":"An HTML macro with wiki content could require script right depending on the content, please review the content carefully.","detailedMessageHTML":"<dl class=\"xform\"><dt>wiki<span class=\"xHint\">Indicate if the wiki syntax in the macro will be interpreted or not.</span></dt><dd><div class=\"code box\">true</div></dd><dt>Content<span class=\"xHint\">The HTML content to insert in the page.</span></dt><dd><div class=\"code box\">Hello!</div></dd></dl>","requiredRights":[{"right":"script","entityType":"DOCUMENT","manualReviewNeeded":true}]}],"currentRights":{"enforce":true,"rights":[{"right":"programming","scope":null}]},"availableRights":[{"right":"","scope":"DOCUMENT","displayName":"None","hasRight":true,"definitelyRequiredRight":true,"maybeRequiredRight":false},{"right":"script","scope":"DOCUMENT","displayName":"Script","hasRight":true,"definitelyRequiredRight":false,"maybeRequiredRight":true},{"right":"admin","scope":"WIKI","displayName":"Wiki Admin","hasRight":true,"definitelyRequiredRight":false,"maybeRequiredRight":false},{"right":"programming","scope":null,"displayName":"Programming","hasRight":true,"definitelyRequiredRight":false,"maybeRequiredRight":false}]}
            // Create a bootstrap dialog to display the results
            // TODO: verify the dialog creation
            const dialog = new RequiredRightsDialog();
            // Add a click event listener to the advanced toggle
            // Show the current rights.
            const currentRights = data.currentRights;
            const availableRights = data.availableRights;
            const supportedRightScopes = {'script': 'DOCUMENT', 'admin': 'WIKI', 'programming': null};

            function getScopeName(scope)
            {
                // TODO: localization
                if (scope === null) {
                    return 'farm';
                } else {
                    return scope.toLowerCase();
                }
            }

            let currentRight = data.currentRights.enforce ? {
                right: '',
                scope: 'DOCUMENT'
            } : null;
            // Check if the configured right is supported.
            if (currentRights.rights.length > 0
                && currentRights.rights[0].scope === supportedRightScopes[currentRights.rights[0].right])
            {
                currentRight = currentRights.rights[0];
            } else if (currentRights.rights.length > 0) {
                // Display a warning that the configured right is not supported.
                // TODO: use a proper warning box.
                dialog.currentRightsElement.innerHTML = `
                            <h2>Warning</h2>
                            <p>The configured right is not supported. 
                            The configured right is ${currentRights.rights[0].right} for the
                                ${getScopeName(currentRights.rights[0].scope)} scope.</p>
                        `;
            }

            const noEnforceDescription = document.createElement('p');
            noEnforceDescription.textContent = "When no rights are enforced, all rights of the last author can be" +
                " used by content like scripts or objects of this page. All users with edit right on this page can" +
                " edit this page.";
            dialog.currentRightsElement.appendChild(noEnforceDescription);

            const noEnforceLabel = document.createElement('label');
            noEnforceLabel.textContent = "Don't enforce required rights "
            const noEnforceInput = document.createElement('input');
            noEnforceInput.type = 'radio';
            if (!data.currentRights.enforce) {
                noEnforceInput.checked = true;
                noEnforceLabel.classList.add('active');
            }
            noEnforceInput.disabled = !availableRights[0].hasRight;
            noEnforceInput.name = 'rights';
            noEnforceInput.value = '';
            noEnforceInput.dataset.xwikiEnforceRequiredRights = 'false';
            noEnforceLabel.appendChild(noEnforceInput);
            const noEnforceLabelContainer = document.createElement('p');
            noEnforceLabelContainer.appendChild(noEnforceLabel);
            dialog.currentRightsElement.appendChild(noEnforceLabelContainer);

            const enforceDescription = document.createElement('p');
            enforceDescription.textContent = "Enforce the following right, such that content of this page like" +
                " scripts or objects can only use the chosen right and users need to have this right (in addition to" +
                " edit right) to be able to edit this page. Every right also includes all rights before it.";
            dialog.currentRightsElement.appendChild(enforceDescription);

            // Display a nice visualization that shows the rights "Edit", "Script", "Wiki Admin" and "Programming" with the current right highlighted if it isn't null.
            const visualization = document.createElement('p');
            visualization.className = 'rights-visualization';

            availableRights.forEach(right => {
                const label = document.createElement('label');
                label.className = 'right ' + right.right;
                if (currentRight && currentRight.right === right.right) {
                    label.classList.add('active');
                }
                label.textContent = right.displayName + ' ';
                const input = document.createElement('input');
                input.type = 'radio';
                input.name = 'rights';
                input.value = right.right;
                input.dataset.xwikiEnforceRequiredRights = 'true';
                if (currentRight && currentRight.right === right.right) {
                    input.checked = true;
                }
                if (right.definitelyRequiredRight) {
                    label.classList.add('suggested');
                }
                if (right.maybeRequiredRight) {
                    label.classList.add('suggested-review');
                }
                input.disabled = !right.hasRight;
                label.appendChild(input);
                visualization.appendChild(label);
            })

            dialog.currentRightsElement.appendChild(visualization);

            // Display a list of the definitely required and maybe required rights.
            const definitelyRequiredRights = availableRights.filter(right => right.definitelyRequiredRight);
            const maybeRequiredRights = availableRights.filter(right => right.maybeRequiredRight);

            function displayRights(rightsToDisplay)
            {
                const rightsList = document.createElement('ul');
                rightsToDisplay.forEach(right => {
                    const listItem = document.createElement('li');
                    listItem.textContent = right.displayName;
                    rightsList.appendChild(listItem);
                });
                return rightsList;
            }

            const explanation = document.createElement('p');
            if (definitelyRequiredRights.filter(right => right.right !== '').length) {
                explanation.textContent = "According to the analysis, the following rights are definitely required:";
                dialog.currentRightsElement.appendChild(explanation);
                dialog.currentRightsElement.appendChild(displayRights(definitelyRequiredRights));
            } else {
                explanation.textContent = "According to the analysis, no rights are required by the content.";
                dialog.currentRightsElement.appendChild(explanation);
            }

            if (maybeRequiredRights.length) {
                const explanation = document.createElement('p');
                explanation.textContent = "The following rights might additionally be required, please" +
                    " review the content in the details below to check if they are actually required:";
                dialog.currentRightsElement.appendChild(explanation);
                dialog.currentRightsElement.appendChild(displayRights(maybeRequiredRights));
            }

            // Display the analysis results.
            // First, resolve the entity reference.
            const analysisResults = data.analysisResults.map(result => {
                // The client-side resolver isn't fully compatible with the entity references generated in Java.
                result.entityReference =
                    XWiki.Model.resolve(result.entityReference.replace(/^(object|class)_property/, '$1Property'));
                return result;
            });

            function groupBy(result, propertyExtractor)
            {
                return result.reduce((acc, result) => {
                    const value = propertyExtractor(result);
                    if (Array.isArray(value)) {
                        value.reduce((acc, key, index) => {
                            if (!acc[key]) {
                                acc[key] = index === value.length - 1 ? [] : {};
                            }
                            return acc[key];
                        }, acc).push(result);
                    } else {
                        if (!acc[value]) {
                            acc[value] = [];
                        }
                        acc[value].push(result);
                    }
                    return acc;
                }, {});
            }

            // Display the results where the entity type is DOCUMENT
            const documentResults = analysisResults.filter(
                result => result.entityReference.type === XWiki.EntityType.DOCUMENT);

            // Group the results by locale.
            const documentResultsByLocale = groupBy(documentResults, result => result.locale ?? '');
            // Display the results in the dialog. Start with the empty string locale, if any.
            if (documentResultsByLocale['']) {
                dialog.addResultsHeading(3, 'Content and Title');
                dialog.addResults(documentResultsByLocale['']);
                delete documentResultsByLocale[''];
            }
            // Display the results for each locale.
            for (const locale in documentResultsByLocale) {
                dialog.addResultsHeading(3, `Content and Title (${locale})`);
                dialog.addResults(documentResultsByLocale[locale]);
            }
            // Display results of type CLASS_PROPERTY, if any.
            const classPropertyResults = analysisResults.filter(
                result => result.entityReference.type === XWiki.EntityType.CLASS_PROPERTY);
            if (classPropertyResults.length > 0) {
                dialog.addResultsHeading(3, 'Class Properties');

                // Group the results by property name
                const classPropertyResultsByProperty = groupBy(classPropertyResults,
                    result => result.entityReference.name);

                for (const propertyName in classPropertyResultsByProperty) {
                    dialog.addResultsHeading(4, `Property ${propertyName}`);
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
                        dialog.addResultsHeading(3, `Object ${xClassName}[${objectIndex}]`);

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
                            dialog.addResultsHeading(4, `Property ${propertyName}`);
                            dialog.addResults(objectPropertyResultsByProperty[propertyName]);
                        }
                    }
                }
            }

            // Hide the toggle for the details if there are no details.
            if (!analysisResults.length) {
                dialog.advancedToggle.hidden = true;
            }

            document.body.appendChild(dialog.dialogElement);

            // Remove the dialog from the DOM when it has been closed.
            $(dialog.dialogElement).on('hidden.bs.modal', () => {
                dialog.dialogElement.remove();
            });

            // Display the dialog
            $(dialog.dialogElement).modal('show');
            return dialog;
        }
    };
});

require(['jquery', 'xwiki-requiredrights-dialog'], function ($, dialog) {
    $(document).on('click', 'button[data-xwiki-requiredrights-dialog="show"]', function (event) {
        event.preventDefault();
        dialog.show();
    });
});