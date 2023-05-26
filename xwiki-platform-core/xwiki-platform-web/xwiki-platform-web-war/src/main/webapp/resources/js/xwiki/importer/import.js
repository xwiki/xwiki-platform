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
var XWiki = (function(XWiki){

    var importer = XWiki.importer = XWiki.importer || {};

    var translations = {
                "availableDocuments" : "$services.localization.render('core.importer.availableDocuments')",
                "importHistoryLabel" : "$services.localization.render('core.importer.importHistory')",
                    "selectionEmpty" : "$services.localization.render('core.importer.selectionEmptyWarning')",
                            "import" : "$services.localization.render('core.importer.import')",
                           "package" : "$services.localization.render('core.importer.package')",
                       "description" : "$services.localization.render('core.importer.package.description')",
                           "version" : "$services.localization.render('core.importer.package.version')",
                           "licence" : "$services.localization.render('core.importer.package.licence')",
                            "author" : "$services.localization.render('core.importer.package.author')",
                  "documentSelected" : "$services.localization.render('core.importer.documentSelected')",
         "whenDocumentAlreadyExists" : "$services.localization.render('core.importer.whenDocumentAlreadyExists')",
                     "addNewVersion" : "$services.localization.render('core.importer.addNewVersion')",
            "replaceDocumentHistory" : "$services.localization.render('core.importer.replaceDocumentHistory')",
                      "resetHistory" : "$services.localization.render('core.importer.resetHistory')",
                    "importAsBackup" : "$services.localization.render('core.importer.importAsBackup')",
                            "select" : "$services.localization.render('core.importer.select')",
                               "all" : "$services.localization.render('core.importer.selectAll')",
                              "none" : "$services.localization.render('core.importer.selectNone')"
    };

    /**
     * Initialization hook for the rich UI.
     * We hijack clicks on package names links, to display the rich importer UI since javascript is available.
     * If javascript is not available, links remains true synchronous HTTP links and the less-rich UI is displayed.
     */
    var hookRichImporterUI = function() {
        $$("#packagelistcontainer a.package").invoke("observe", "click", function(event) {
            var a = event.element(), file = a.href.substring(a.href.indexOf("&file=") + 6);

            event.stop(); // prevent loading the link.

            // Visually mark the selected package as active.
            $$('div#packagelistcontainer div.active').invoke('removeClassName','active');
            event.element().up("div.package").addClassName("active");

            // Create a package explorer widget to let the user browse
            // and select/unselect the documents he wants.
            new importer.PackageExplorer( "packagecontainer", decodeURIComponent(file) );
        });
        $$("#packagelistcontainer .deletelink").invoke("observe", "click", function(event) {
            event.stop();
            new XWiki.widgets.ConfirmedAjaxRequest(event.findElement('a').href,
                {onSuccess: function() {
                    if (event.element().up('div.active')) {
                        $('packagecontainer').update();
                    }
                    event.findElement('li').remove();
                }},
                {confirmationText: "$services.localization.render('core.viewers.attachments.delete.confirm')"}
            );
        });
    }
    document.observe("xwiki:dom:loaded", function() {
      hookRichImporterUI();
      /** Attach the HTML5 uploader, if available */
      var form = $('AddAttachment');
      if (form && typeof(XWiki.FileUploader) != 'undefined') {
        var input = form.down("input[type='file']");
        var html5Uploader = new XWiki.FileUploader(input, {
          'progressAutohide' : true,
          'responseContainer' : $('packagelistcontainer'),
          'responseURL' : window.docgeturl + '?xpage=packagelist&forceTestRights=1',
          'maxFilesize' : parseInt(input.readAttribute('data-max-file-size'))
        });
        form.observe("xwiki:html5upload:done", hookRichImporterUI);
        html5Uploader.hideFormButtons();
      }
    });

    /**
     * Extend input elements with check and uncheck methods to be able to check/uncheck
     * a large collection of checkboxes at once using Enumerable#invoke
     */
    Element.addMethods('input', {
        uncheck:function(elem){
            elem = $(elem);
            elem.checked = false;
            return elem;
        },
        check:function(elem){
            elem = $(elem);
            elem.checked = true;
            return elem;
        }
    });

    /**
     * Helper class to request the server informations about a package via AJAX.
     */
    importer.PackageInformationRequest = Class.create({

        /**
         * Constructor of this class
         */
        initialize:function(name, options)
        {
            this.name = name;

            this.successCallback = options.onSuccess || function(){};
            this.failureCallback = options.onFailure || function(){};

            var url = window.docgeturl + "?xpage=packagedescriptor&package=" + encodeURIComponent(name);

            new Ajax.Request(url, {
                onSuccess: this.onSuccess.bindAsEventListener(this),
                on0 : this.on0.bindAsEventListener(this),
                onFailure : this.onFailure.bind(this)
            });
        },

        // 0 is returned for network failures.
        on0 : function(response)
        {
          response.request.options.onFailure(response);
        },

        onSuccess : function(response)
        {
          this.successCallback(response);
        },

        onFailure : function(response)
        {
          this.failureCallback(response);
        }
    });

    /**
     * A widget that allows to browse the contents of a package (a XAR).
     */
    importer.PackageExplorer = Class.create({

        /**
         * Constructor of our widget.
         *
         * @param id the DOM id to use as parent of this package explorer widget
         * @param name the name of the XAR to display the explorer
         */
        initialize:function(id, name)
        {
            this.node = $(id);
            this.name =  name;

            // A object to store the documents the import should ignore.
            // Entries will be added to that object each time the user
            // Unselect a page or entire space in the list of available documents.
            this.ignore = {};

            this.documentCount = {};

            // Request the server for information about the desired package,
            // and bind the response to the proper callbacks (success or failure).
            this.node.addClassName("loading");
            new importer.PackageInformationRequest(name,{
              onSuccess: this.onPackageInfosAvailable.bind(this),
              onFailure: this.onPackageInfosRequestFailed.bind(this)
            });
        },

        /**
         * Callback triggered when the package information has been successfully retrieved
         */
        onPackageInfosAvailable: function(transport)
        {
            var pack = transport.responseText.evalJSON();

            this.infos = pack.infos;
            this.entities = XWiki.EntityReferenceTree.fromJSONObject(pack.entities);

            // Load tree css
            (function loadCss(url) {
              var link = document.createElement("link");
               link.type = "text/css";
               link.rel = "stylesheet";
               link.href = url;
              document.getElementsByTagName("head")[0].appendChild(link);
             })("$services.webjars.url('org.xwiki.platform:xwiki-platform-tree-webjar', 'tree.min.css', {'evaluate': true})");

            // Insert the package tree
            require(['xwiki-tree'], this.initXTree.bind(this));
        },

        initXTree: function($)
        {
            // Remove loading indicator.
            this.node.removeClassName("loading");

            // Clear the content in case a package already is present.
            this.node.update();

            if (this.node.empty()) {
              this.node.insert( new Element("h4", {'class':'legend'}).update( translations["availableDocuments"] ));
            }

            this.container = new Element("div", {'id':'packageDescription'});
            this.node.insert(this.container);

            // Inject the package header.
            this.container.insert( this.createPackageHeader(this.infos) );

            // Inject the block with select all/none links
            var noneLink = new Element("span").update( translations["none"] );
            noneLink.observe("click", this.onIgnoreAllDocuments.bind(this));
            var allLink = new Element("span").update( translations["all"] );
            allLink.observe("click", this.onRestoreAllDocuments.bind(this));
            this.container.insert( new Element("div", {'class':'selectLinks'})
                                       .insert( translations["select"] )
                                       .insert( noneLink )
                                       .insert(", ")
                                       .insert( allLink )  );

            // Create and inject in the DOM the parent UL HTML element
            // that will contain the list of space and documents present in the package.
            this.list = new Element("ul");
            this.container.insert(new Element("div", {
                'id' : 'package',
                'class' : 'package xtree jstree-no-links'
              }).update(this.list));

            // Add the entities tree
            Object.values(this.entities.children).each(this.addSpace.bind(this));

            // Insert options and button to submit the form.
            this.container.insert(  this.createPackageFormSubmit(this.infos) );

            $('#package').xtree({
              plugins: ['checkbox']
            });

            this.xtree = $.jstree.reference($('#package'));
            this.xtree.check_all();
        },

        onIgnoreAllDocuments: function()
        {
            this.xtree.uncheck_all();
        },

        onRestoreAllDocuments : function()
        {
            this.xtree.check_all();
        },

        onPackageInfosRequestFailed: function(transport)
        {
            this.node.update();

            var errorMessage = "Failed to retrieve package information. Reason: " +
              (transport.statusText || "Server not responding");
            this.node.removeClassName("loading");
            this.node.update( new Element("div", {'class':'errormessage'}).update(errorMessage) );
        },

        /**
         * Builds the submit DOM fragment of the form, including history options
         */
        createPackageFormSubmit: function(infos)
        {
            var submitBlock = new Element("div", {'class':'packagesubmit'});

            submitBlock.insert( new Element("div").update( translations["whenDocumentAlreadyExists"] ));

            var defaultChoiceRadioButton =  new Element("input", { 'type':'radio','name':'historyStrategy',
                                                                      'checked':'checked', 'value': 'add' });

            submitBlock.insert(  new Element("div", {'class':'historyStrategyOption'})
                                       .insert( defaultChoiceRadioButton )
                                       .insert( translations["addNewVersion"] )  );

            submitBlock.insert(  new Element("div", {'class':'historyStrategyOption'})
                                       .insert( new Element("input", { 'type':'radio','name':'historyStrategy', 'value': 'replace' }) )
                                       .insert( translations["replaceDocumentHistory"] )  );

            submitBlock.insert(  new Element("div", {'class':'historyStrategyOption'})
                                       .insert( new Element("input", { 'type':'radio','name':'historyStrategy', 'value': 'reset' }) )
                                       .insert(translations["resetHistory"] )  );

            if (XWiki.hasBackupPackImportRights) {
                    var importAsBackupCheckbox = new Element("input", { 'type':'checkbox', 'name':'importAsBackup', 'value':'true' });
                    if (infos.backup) {
                      importAsBackupCheckbox.checked = true;
                    }
                    submitBlock.insert(  new Element("div", {'class':'importOption'})
                                               .insert(importAsBackupCheckbox)
                                               .insert(translations["importAsBackup"])  );
            }

            var submit = new Element("span", {'class':'buttonwrapper'});
            var button = new Element("input", {'type':'submit', 'value': translations["import"], 'class':'button'});
            button.observe("click", this.onPackageSubmit.bind(this));
            submit.insert(button);

            submitBlock.insert(submit);

            return submitBlock;
        },

        /**
         * Callback when package is submitted.
         */
        onPackageSubmit: function()
        {
            var selectedNodes = this.xtree.get_bottom_checked(true);

            if (selectedNodes.length == 0) {
              // Refuse to import since no document remains selected.
              // Displays a warning and exit.
              var warning = new Element("span", {'class':'warningmessage'}).update( translations["selectionEmpty"] );
              if (!$("packagecontainer").down("div.packagesubmit span.warningmessage")) {
                 // Display the warning only if not present yet in the DOM (in case the user clicks like a maniac).
                 $('packagecontainer').select('div.packagesubmit input').last().insert({'after' : warning});
                 Element.remove.delay(5, warning);
              }

              return;
            }

            // Create form and submit ajax request.
            var parameters = {};

            parameters["action"] = "import";
            parameters["name"] = this.name;

            parameters["historyStrategy"] = $('packageDescription').down("input[type=radio][value='add']").checked ? "add" :
                                            ($('packageDescription').down("input[type=radio][value='replace']").checked ? "replace" : "reset");
            if (XWiki.hasBackupPackImportRights) {
                parameters["importAsBackup"] = $('packageDescription').down("input[type=checkbox][name='importAsBackup']").checked ? "true" : "false";
            }
            parameters["ajax"] = "1";

            var pages = [];

            selectedNodes.each(function(node) {
              var expectedName = node.data.reference + ":" + node.data.locale
              pages.push( expectedName );
              parameters["language_" + expectedName] = node.data.locale;
            });

            parameters["pages"] = pages;

            this.node.update();
            this.node.addClassName("loading");
            this.node.setStyle("min-height:200px");

            // Make sure the request goes through the Import action, where the actual import takes place.
            new Ajax.Request(XWiki.currentDocument.getURL('import', Object.toQueryString(window.location.href.parseQuery())), {
              method:'post',
              parameters: parameters,
              onSuccess: function(transport) {
                 $('packagecontainer').removeClassName("loading");
                 $('packagecontainer').update(transport.responseText);
              },
              onFailure: function(transport) {
                   var errorMessage = "Failed to import documents. Reason: " +
                     (transport.statusText || "Server not responding");
                   $('packagecontainer').removeClassName("loading");
                   $('packagecontainer').update( new Element("div", {'class':'errormessage'}).update(errorMessage) );
              }
            });
        },

        /**
         * Create the header block of the package, that contains
         * - The filename of the package (for example: xwiki-enterprise-wiki-2.0.xar)
         * - The name of the package (for example: XWiki Products - Enterprise - Wiki)
         * - The version of the package (for example: 2.0.3)
         * - The licence of the package (for example LGPL)
         * - The username of the author of the package (for example XWiki.Admin)
         * - Wether the package is a back up pack or not (contains revisions along with documents)
         *
         * @param infos the array that contains the informations to build the header upon
         */
        createPackageHeader:function(infos)
        {
            var packageInfos = new Element("div", {'class':'packageinfos'});
            packageInfos.insert(  new Element("div")
                     .insert( new Element("span", {'class':'label'}).update(translations["package"]) )
                     .insert( new Element("span", {'class':'filename'}).update(this.name) )  );
            if( infos.name !== "") {
              packageInfos.insert(  new Element("div")
                                      .insert( new Element("span", {'class':'label'}).update(translations["description"]) )
                                      .insert( new Element("span", {'class':'name'}).update(infos.name) )  );
            }
            if (infos.version !== "") {
              packageInfos.insert(  new Element("div")
                                      .insert( new Element("span", {'class':'label'}).update(translations["version"]) )
                                      .insert( new Element("span", {'class':'version'}).update(infos.version) )  );
            }
            if (infos.author !== "") {
              packageInfos.insert(  new Element("div")
                                      .insert( new Element("span", {'class':'label'}).update(translations["author"]) )
                                      .insert( new Element("span", {'class':'author'}).update(infos.author) )  );
            }
            if (infos.licence !== "") {
              packageInfos.insert(  new Element("div")
                                      .insert( new Element("span", {'class':'label'}).update(translations["licence"]) )
                                      .insert( new Element("span", {'class':'licence'}).update(infos.licence) )  );
            }
            return packageInfos;
        },

        /**
         * Adds a space to the package explorer.
         */
        addSpace: function(spaceNodeMap)
        {
            this.addSpaceToList(this.list, spaceNodeMap[XWiki.EntityType.SPACE]);
        },

        /**
         * Adds a space to parent space.
         */
        addSpaceToList: function(parentList, spaceNode)
        {
            var spaceItem = new Element("li", {
                'data-type' : 'space',
                'data-reference' : spaceNode.reference,
                'data-jstree':
                  '{'+
                    '"icon":"fa fa-folder-o",' +
                    '"iconOpened":"fa fa-folder-open-o"'
                  +'}'
              }).update(spaceNode.reference.name);

            var list = new Element("ul");

            var self = this;

            // Add children
            Object.values(spaceNode.children).each(function(childNodeMap) {
              // Can be a space child.
              if (childNodeMap.hasOwnProperty(XWiki.EntityType.SPACE)) {
                self.addSpaceToList(list, childNodeMap[XWiki.EntityType.SPACE]);
              }
              // Can also be a document child, with the same name.
              if (childNodeMap.hasOwnProperty(XWiki.EntityType.DOCUMENT)) {
                self.addDocumentToList(list, childNodeMap[XWiki.EntityType.DOCUMENT]);
              }
            });

            spaceItem.insert(list);

            parentList.insert(spaceItem);
        },

        /**
         * Adds a single document to a space
         */
        addDocumentToList: function(list, documentNode)
        {
            Object.values(documentNode.locales).each(function(localeReference) {
              var displayName = localeReference.name;
              if (localeReference.locale != '') {
                  displayName += " - " + localeReference.locale;
              }

              var pageItem = new Element("li", {
                'data-type' : 'document',
                'data-reference' : localeReference,
                'data-locale' : localeReference.locale,
                'data-jstree':
                  '{'+
                    '"icon":"fa fa-file-o"'
                  +'}'
              }).update(displayName);

              list.insert(pageItem);
          });
        },
    });

    return XWiki;

})(XWiki || {});
