var XWiki = (function(XWiki){

    var importer = XWiki.importer = XWiki.importer || {};

    var translations = {
                "availableDocuments" : "$msg.get('core.importer.availableDocuments')",
                "importHistoryLabel" : "$msg.get('core.importer.importHistory')",
                    "selectionEmpty" : "$msg.get('core.importer.selectionEmptyWarning')",
                            "import" : "$msg.get('core.importer.import')",
                           "package" : "$msg.get('core.importer.package')",
                       "description" : "$msg.get('core.importer.package.description')",
                           "version" : "$msg.get('core.importer.package.version')",
                           "licence" : "$msg.get('core.importer.package.licence')",
                            "author" : "$msg.get('core.importer.package.author')",
                  "documentSelected" : "$msg.get('core.importer.documentSelected')",
         "whenDocumentAlreadyExists" : "$msg.get('core.importer.whenDocumentAlreadyExists')",
                     "addNewVersion" : "$msg.get('core.importer.addNewVersion')",
            "replaceDocumentHistory" : "$msg.get('core.importer.replaceDocumentHistory')",
                      "resetHistory" : "$msg.get('core.importer.resetHistory')",
                    "importAsBackup" : "$msg.get('core.importer.importAsBackup')",
                            "select" : "$msg.get('core.importer.select')",
                               "all" : "$msg.get('core.importer.selectAll')",
                              "none" : "$msg.get('core.importer.selectNone')"
    };

    // FIXME: we should have those images outside SmartClient library to lessen the dependency towards the library
    var expandFolderImagePath = "$xwiki.getSkinFile('js/smartclient/skins/Enterprise/images/TreeGrid/opener_closed.png')";
    var collapseFolderImagePath = "$xwiki.getSkinFile('js/smartclient/skins/Enterprise/images/TreeGrid/opener_opened.png')";

    /**
     * Initialization hook for the rich UI.
     * We hijack clicks on package names links, to display the rich importer UI since javascript is available.
     * If javascript is not available, links remains true synchronous HTTP links and the less-rich UI is displayed.
     *
     * FIXME: right now disabled for IE6 - until the rich UI is fully debugged for this browser
     */
    if (!browser.isIE6x) {
      document.observe("dom:loaded", function(){
        $$("#packagelistcontainer ul.xlist li.xitem a.package").invoke("observe", "click", function(event) {
            var a = event.element(), file = a.href.substring(a.href.indexOf("&file=") + 6);

            event.stop(); // prevent loading the link.

            // Visually mark the selected package as active.
            $$('div#packagelistcontainer div.active').invoke('removeClassName','active');           
            event.element().up("div.package").addClassName("active");

            // Create a package explorer widget to let the user browse
            // and select/unselect the documents he wants.          
            new importer.PackageExplorer( "packagecontainer", decodeURIComponent(file) );        
        });
      });
    }
    
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
            
            var url = window.docgeturl + "?xpage=packageinfo&package=" + encodeURIComponent(name);
            
            var ajx = new Ajax.Request(url, {
                onSuccess: this.onSuccess.bindAsEventListener(this),
                on1223 : this.on1223.bindAsEventListener(this),
                on0 : this.on0.bindAsEventListener(this),
                onFailure : this.onFailure.bind(this)
            });
        },
        
        // IE converts 204 status code into 1223...
        on1223 : function(response) 
        {
          response.request.options.onSuccess(response);
        },

        // 0 is returned for network failures, except on IE where a strange large number (12031) is returned.
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
            var ajx = new importer.PackageInformationRequest(name,{
              onSuccess: this.onPackageInfosAvailable.bind(this),
              onFailure: this.onPackageInfosRequestFailed.bind(this)
            });
        },
        
        /**
         * Callback triggered when the package information has been successfully retrieved
         */
        onPackageInfosAvailable: function(transport) 
        {
            // Remove loading indicator.
            this.node.removeClassName("loading");
            
            // Clear the content in case a package already is present.          
            this.node.update(); 
            
            if (this.node.empty()) {
                this.node.insert( new Element("h4", {'class':'legend'}).update( translations["availableDocuments"] ));
            }
                        
            var pack = transport.responseText.evalJSON();
            
            this.infos = pack.infos;
            this.packageDocuments = pack.files;         

            this.container = new Element("div", {'id':'packageDescription'});
            this.node.insert(this.container);

            // Inject the package header.           
            this.container.insert( this.createPackageHeader(pack.infos) );
     
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
            this.list = new Element("ul", {'class':'xlist package'});
            this.container.insert( new Element("div", {'id':'package'}).update(this.list) );
            
            // Create the list of spaces and their documents
            Object.keys(this.packageDocuments).sort().each(this.addSpaceToPackage.bind(this));
            
            // Insert options and button to submit the form.
            this.container.insert(  this.createPackageFormSubmit( pack.infos) );
            
            this.container.down("div.packagesubmit input[type=radio]").checked = true;
            // The line above should not be needed, but as it appears IE will not let one check a checkbox before it's inserted in the DOM
        },

        onIgnoreAllDocuments: function()
        {           
            this.container.select("input[type=checkbox][class=space]").invoke("uncheck");
            this.container.select("input[type=checkbox][class=space]").invoke("fire","custom:click");
        },

        onRestoreAllDocuments : function()
        { 
            this.container.select("input[type=checkbox][class=space]").invoke("check");
            this.container.select("input[type=checkbox][class=space]").invoke("fire","custom:click");       
        },

        onPackageInfosRequestFailed: function(transport) 
        {           
            this.node.update(); 

            var errorMessage = "Failed to retrieve package information. Reason: ";
            if (transport.statusText == '' /* No response */ || response.status == 12031 /* In IE */) {
              errorMessage += "Server not responding";
            } else {
              errorMessage += transport.statusText;
            }
            this.node.removeClassName("loading");
            this.node.update( new Element("div", {'class':'errormessage'}).update(errorMessage) );            
        },

        /**
         * Builds the submit DOM fragment of the form, including history options
         */
        createPackageFormSubmit: function(infos)
        {           
            var submitBlock = new Element("div", {'class':'packagesubmit'});

            submitBlock.insert( new Element("em").update( translations["whenDocumentAlreadyExists"] ));

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
            if (this.countSelectedDocuments() == 0) {
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
            
            var spaces = Object.keys(this.packageDocuments);
            for (var i=0;i < spaces.length; i++) {
                var space = this.packageDocuments[spaces[i]];
                var documents = Object.keys(space);
                for (var j=0;j<documents.length;j++) {
                    var doc = space[documents[j]];
                    doc.each(function(item){
                        if (!this.isIgnored(spaces[i], documents[j], item.language)) {
                            var expectedName = item.fullName + ":" + item.language
                            pages.push( expectedName );
                            parameters["language_" + expectedName] = item.language;
                        }
                    }.bind(this));
                }
            }
            parameters["pages"] = pages;
            
            this.node.update();
            this.node.addClassName("loading");
            this.node.setStyle("min-height:200px");
            
            new Ajax.Request(window.location, {
              method:'post',
              parameters: parameters,
              onSuccess: function(transport) {
                 $('packagecontainer').removeClassName("loading");
                 $('packagecontainer').update(transport.responseText);
              },
              onFailure: function(transport) {
                   var errorMessage = "Failed to import documents. Reason: ";
                   if (transport.statusText == '' /* No response */ || response.status == 12031 /* In IE */) {
                     errorMessage += "Server not responding";
                   } else {
                     errorMessage += transport.statusText;
                   }
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
         * Adds a sigle space to the package explorer.
         */
        addSpaceToPackage: function(space) 
        {   
            var docNb = this.countDocumentsInSpace(space);
            var selection =  docNb + " / " + docNb + " " + translations["documentSelected"];
            
            var spaceItem = new Element("li", {'class':'xitem xunderline'});

            var spaceItemContainer = new Element("div", {'class':'xitemcontainer'});
            var spaceBox = new Element("input", {'type':'checkbox','checked':'checked', 'class':'space'});
            spaceBox.observe("click", function(originalEvent) {
                // Instead of directly binding the callback to the click event, we proxy it to a custom event
                // so that it's possible to programmatically fire this event,
                // since prototype.js does not support manual native event firing as of 1.6
                // See #onIgnoreAllDocuments and onRestoreAllDocuments
                spaceBox.fire("custom:click", originalEvent.memo);
            }.bind(this));
            spaceBox.observe("custom:click", this.spaceCheckboxClicked.bind(this));

            spaceItemContainer.insert(spaceBox);
            
            var expandImage = new Element("img", {'src': expandFolderImagePath });
            spaceItemContainer.insert(expandImage);

            var spaceName = new Element("div", {'class':'spacename'}).update(space)
            spaceItemContainer.insert(spaceName);

            var onToggle = function(event){
                event.element().up("li").down("div.pages").toggleClassName("hidden");
                event.element().up("li").down("img").src =
                    event.element().up("li").down("div.pages").hasClassName("hidden") ?
                    expandFolderImagePath :
                    collapseFolderImagePath
            };

            expandImage.observe("click", onToggle);
            spaceName.observe("click", onToggle);

            spaceItemContainer.insert(new Element("div", {'class':'selection'}).update(selection));
            spaceItemContainer.insert(new Element("div", {'class':'clearfloats'}));

            var pagesContainer = new Element("div", {'class':'pages hidden'});
            var list = new Element("ul", {'class':'xlist pages'});

            var self = this;

            // Fill in the space with the list of document it contains
            Object.keys(this.packageDocuments[space]).sort().each(function(page) {
                self.addDocumentToSpace(list, space, page);
            });
                
            pagesContainer.update(list);
            spaceItemContainer.insert(pagesContainer);

            spaceItem.insert(spaceItemContainer);
            this.list.insert(spaceItem);
            
            spaceBox.checked = true;
            // The line above should not be needed, but as it appears IE will not let one check a checkbox before it's inserted in the DOM
        },      
        
        /**
         * Adds a single document to a space
         *
         * @param list
         * @param space
         * @param page
         */
        addDocumentToSpace: function(list, space, page) 
        {
            var trList = this.packageDocuments[space][page], self = this;
            trList.sortBy(function(s){return s.language}).each(function(infos) {                
                var pageItem = new Element("li", {'class':'xitem xhighlight'});
                var pageItemContainer = new Element("div", {'class': 'xitemcontainer xpagecontainer'});
            
                var docBox = new Element("input", {'type':'checkbox','checked':'checked'});
                docBox.observe("click", self.documentCheckboxClicked.bind(self));
                pageItemContainer.insert( new Element("span", {'class':'checkbox'}).update(docBox) );
                
                pageItemContainer.insert(new Element("span", {'class':'documentName'}).update(page));
                if (infos.language != "") {
                   pageItemContainer.insert(new Element("span", {'class':'documentLanguage'}).update(" - " + infos.language));
			    }
                pageItemContainer.insert(new Element("div", {'class':'clearfloats'}));
                
                // Insert some hidden div to store exact fullName and language of the node.
                pageItem.insert(new Element("div", {'class': 'fullName hidden'}).update(infos.fullName));
                pageItem.insert(new Element("div", {'class': 'language hidden'}).update(infos.language));
                
                // Finally inserts the page item in the list of pages for that space.
                pageItem.insert(pageItemContainer);
                list.insert(pageItem);
                
                docBox.checked = true;
                // The line above should not be needed, but as it appears IE will not let one check a checkbox before it's inserted in the DOM
            });                         
        },
        
        countDocumentsInSpace: function(spaceName)
        {
            var self = this;
            if (typeof this.documentCount[spaceName] == "undefined") {
                this.documentCount[spaceName] = Object.keys(this.packageDocuments[spaceName]).inject(0, function(acc, elem) { 
                    // Not super efficient, but will do the trick.
                    return acc + self.packageDocuments[spaceName][elem].length; 
                });
            }
            delete self;
            return this.documentCount[spaceName];
        },
        
        countSelectedDocumentsInSpace: function(spaceName)
        {
            // compute the number of selected documents in that space substracting the ones marked ignored to the total
            var selected;
            if (typeof this.ignore[spaceName] == "undefined") {
                return this.countDocumentsInSpace(spaceName);
            }
            else {
                var self = this;
                return (this.countDocumentsInSpace(spaceName) - Object.keys(this.ignore[spaceName]).inject(0, function(acc, elem) { 
                    // Same. Not super efficient, but will do the count.
                    return acc + self.ignore[spaceName][elem].length; 
                }));
            }
        },
        
        countSelectedDocuments: function()
        {
            var self = this;
            return Object.keys(this.packageDocuments).inject(0, function(acc, elem) {
                return acc + self.countSelectedDocumentsInSpace(elem);
            });
        },
        
        /**
         * Update the number of selected docs displayed.
         */
        updateSelection: function(container, spaceName)
        {
            // First count the total number of documents per space,
            // It correspond to the sum of the number of translations for each document
            var total = this.countDocumentsInSpace(spaceName);
            var selected = this.countSelectedDocumentsInSpace(spaceName);
            
            container.down(".selection").update(selected + " / " + total + " " + translations["documentSelected"]);

            if (selected == 0) {
              // If all document checkboxes have been unchecked, ensure that the space box is unchecked as well
              container.down("input.space").uncheck();
            }
            else {
              // At least one document box is checked, let's make sure the space box is too
              container.down("input.space").check();
            } 
        },
        
        /**
         * Callback triggered when a space checkbox has been clicked (either selected or unselected).
         */
        spaceCheckboxClicked: function(event)
        {
            var selected = event.element().checked;
            var spaceName = event.element().up(".xitemcontainer").down(".spacename").innerHTML;
            var pages = event.element().up(".xitemcontainer").down("div.pages");
            if (!selected) {
                // An entire space has been unselected.
                // Add the whole space to the ignore list and make sure all its docs appears unselected.                
                this.ignoreSpace(spaceName);
                pages.select("input[type='checkbox']").invoke("uncheck");
            }
            else {
                this.restoreSpace(spaceName);
                pages.select("input[type='checkbox']").invoke("check");
            }
            this.updateSelection(event.element().up(".xitemcontainer"), spaceName);
        },
        
        /**
         * Callback when a checkbox has been clicked for a single document.
         */
        documentCheckboxClicked: function(event) 
        {
            var page = event.element().up("div").down("span.documentName").innerHTML.stripTags().strip();
            var space = event.element().up("li").up("div.xitemcontainer").down(".spacename").innerHTML;
            var language = event.element().up("li").down(".language").innerHTML;
            var selected = event.element().checked;
            if (!selected) {
                this.ignoreDocument(space, page, language);
            }
            else {
                this.restoreDocument(space, page, language);
            }
            this.updateSelection(event.element().up("li").up("div.xitemcontainer"), space);
        },
                
        /**
         * Checks wether the passed document as been marked as ignored for import by the user
         */
        isIgnored: function(space, docName, language) { 
            if (typeof this.ignore[space] == "undefined") {
                return false;
            }
            if (typeof this.ignore[space][docName] == "undefined") {
                return false;
            }

            for (var i=0;i<this.ignore[space][docName].length;i++) {
                if (this.ignore[space][docName][i].language == language) {
                    return true;
                }
            }
            return false;
        },

        /**
         * Ignore an entire space.
         */
        ignoreSpace: function(spaceName) 
        {
            this.ignore[spaceName] = Object.toJSON(this.packageDocuments[spaceName]).evalJSON();
            // Object#clone is swallow copy
            // here we emulate a deep copy by serializing/unserializing to/from JSON.
            // FIXME: not the most efficient for spaces with lot of documents.
        },
        
        /**
         * Restore an entire space in case some of its documents are ignored.
         */
        restoreSpace: function(spaceName)
        {
            if (typeof this.ignore[spaceName] != "undefined") {
                delete this.ignore[spaceName];
            }
        },
        
        /**
         * Ignore a single document
         */
        ignoreDocument: function(spaceName, documentName, language) 
        {
            if (typeof this.ignore[spaceName] == "undefined") {
                this.ignore[spaceName] = new Object();
            }
            if (typeof this.ignore[spaceName][documentName] == "undefined") {
                this.ignore[spaceName][documentName] = [];
            }
            this.ignore[spaceName][documentName][this.ignore[spaceName][documentName].length] = {"language":language};
        },
        
        /**
         * Restore a single document.
         */
        restoreDocument: function(spaceName, documentName, language) 
        {
            if (typeof this.ignore[spaceName] != "undefined" && typeof this.ignore[spaceName][documentName] != "undefined") {
                for(var i=0;i<this.ignore[spaceName][documentName].length;i++) {
                    if (this.ignore[spaceName][documentName][i].language === language) {
                        delete this.ignore[spaceName][documentName][i];
                        this.ignore[spaceName][documentName] = this.ignore[spaceName][documentName].compact();                      
                    }
                }
            }

        }

    });

    return XWiki;
    
})(XWiki || {});
