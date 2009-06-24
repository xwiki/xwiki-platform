/*

---( FoXWiki by Robin Fernandes )---
More info: http://soal.xwiki.com/xwiki/bin/view/Code/FoXWiki

This work is licensed under the Creative Commons Attribution 2.5 License. 
This means you are free to use any of this code for any purpose as long as
you give credit to Robin Fernandes. View a copy of the license here:
http://creativecommons.org/licenses/by/2.5/

*/

FoXWiki = new FoXWikiClass();

window.addEventListener("load", function(e) { FoXWiki.onLoad(e); }, false); 

document.addEventListener("FoXWikiDomEvent", function(e) { FoXWiki.onFoXWikiDomEvent(e); }, false, true);

function FoXWikiClass() {
	// Interface
	this.onLoad = onLoad;
	this.contextPopupShowing = contextPopupShowing;
	this.onXWikiAction = onXWikiAction;
	this.onFoXWikiDomEvent = onFoXWikiDomEvent;

	//URL transforms
	this.EditContent= new XWikiActionSetterClass("edit");
	this.EditForm 	= new XWikiActionSetterClass("inline");
	this.ViewPage 	= new XWikiActionSetterClass("view");
	this.DeletePage = new XWikiActionSetterClass("delete");
	this.EditObjects= new XWikiTemplateSetterClass("editobject");
	this.EditRights = new XWikiTemplateSetterClass("editrights");
	this.ViewCode 	= new XWikiTemplateSetterClass("code");

	// Implementation:
	var XWikiBase	= "xwiki/bin"; 		// TODO: make this customisable
	var XWikiView	= XWikiBase + "/view"; 	//if the URL contains this string, show menu.
	
	//Initialisation
	function onLoad(event) {
		var menu = document.getElementById("contentAreaContextMenu");
		menu.addEventListener("popupshowing", this.contextPopupShowing, false);	
		this.initialized = true;
	}

	//Called when context-menu is about to be shown, thanks to listener added 
	//in onLoad. Allows us to manipulate content of menu before it is displayed.
	function contextPopupShowing(event) {
		// Hide entire foxwiki menu if not inside an xwiki site.
		var isXWikiLink = gContextMenu.linkURL.indexOf(XWikiBase) > -1;
		gContextMenu.showItem("foxwiki-context-menu", isXWikiLink);
		gContextMenu.showItem("foxwiki-main-menuseparator", isXWikiLink);
		// Action links.
		var isXWikiViewLink = gContextMenu.linkURL.indexOf(XWikiView) > -1;
		gContextMenu.showItem("separator-01", isXWikiViewLink);
		gContextMenu.showItem("edit-content-item", isXWikiViewLink);
		gContextMenu.showItem("edit-form-item", isXWikiViewLink);
		gContextMenu.showItem("edit-objects-item", isXWikiViewLink);
		gContextMenu.showItem("edit-rights-item", isXWikiViewLink);
		gContextMenu.showItem("separator-02", isXWikiViewLink);
		gContextMenu.showItem("view-code-item", isXWikiViewLink);
		gContextMenu.showItem("view-page-item", isXWikiViewLink);
		gContextMenu.showItem("separator-03", isXWikiViewLink);
		gContextMenu.showItem("delete-page-item", isXWikiViewLink);
		// WebDAV Links.
		var isXWikiWebDAVLink = false;
		if( getDavUrl(gContextMenu.linkURL) != "" ) {
			isXWikiWebDAVLink = true;
		}		
		gContextMenu.showItem("edit-webdav-item", isXWikiWebDAVLink);
	}

	//Called from menu, takes a URL transform as an argument
	function onXWikiAction(e, urlTransform) {
		window._content.location = urlTransform.morphUrl(gContextMenu.linkURL);
	}

	// URL transform types
	function XWikiActionSetterClass(inAction)  {
		//public
		this.morphUrl=morphUrl;	
		//private
		var action = inAction;
		function morphUrl(inUrl) {
			//TODO: use XWikiBase and XWikiID variables.
			return inUrl.replace(/xwiki\/bin\/view/g, "xwiki/bin/"+action);
		}
	}

	function XWikiTemplateSetterClass(inTemplate)  {
		//public
		this.morphUrl=morphUrl;	
		//private			
		var template = inTemplate;
		function morphUrl(inUrl) {
			return inUrl + "?xpage=" + template;
		}			
	}

	function onFoXWikiDomEvent(evnt) {
		var davURL = evnt.target.getAttribute("davURL");
		launchLocalEditor(davURL);
		evnt.target.setAttribute("foxwiki", "true");
	}

};

/**
 * Preference manager.
 */
prefManager = Components.classes["@mozilla.org/preferences-service;1"]
		.getService(Components.interfaces.nsIPrefBranch);

/**
 * Called whe the user clicks the 'Edit WebDAV' menuitem
 */
function editWebdav() {
	launchLocalEditor(getDavUrl(gContextMenu.linkURL));
}

/*
 * Launches the application associated with 'ext' with the given url as it's first argument.
 */
function launchLocalEditor(webdavUrl) {
	var dot = webdavUrl.lastIndexOf(".");
	var ext = webdavUrl.substr(dot + 1);
	var associations = prefManager.getCharPref("extensions.foxwiki.associations").split(",");
	var launchAppPath = "";
	for( var i = 0; i < associations.length; i++ ) {
		parts = associations[i].split("=");
		if( parts[0] == ext ) {
			launchAppPath = parts[1];
			break;
		}
	}
	if( launchAppPath != "" ) {
		// Launch the local application (with dummy args for the moment)
		// create an nsILocalFile for the executable
		var file = Components.classes["@mozilla.org/file/local;1"]
			.createInstance(Components.interfaces.nsILocalFile);
		try {
			file.initWithPath(launchAppPath);
		} catch (err) {
			alert("Unable to initialize the executable with path [" + launchAppPath + "]");
			return;
		}
		// create an nsIProcess
		var process = Components.classes["@mozilla.org/process/util;1"]
			.createInstance(Components.interfaces.nsIProcess);
		try {
			process.init(file);
		} catch (err) {
			alert("Error while initializing the nsIProcess : " + err);
			return;
		}
		// Run the process.
		// If first param is true, calling thread will be blocked until
		// called process terminates.
		// Second and third params are used to pass command-line arguments
		// to the process.
		var args = (webdavUrl == "") ? [] : [webdavUrl];
		try {
			process.run(false, args, args.length);
		} catch (err) {
			alert("Error while executing the nsIProcess : " + err);
			return;
		}
		return;
	}
	alert("No application is associated for extension [" + ext + "], please adjust your foxwiki preferences.");
}

/*
 * We might be able to avoid using this function if we can invoke a function on the page to do the conversion.
 */
function getDavUrl(httpUrl) {
	var xwiki_attachment_signature = "/xwiki/bin/download/";
	var xwiki_webdav_signature = "/xwiki/webdav/spaces/";
	var davUrl = "";
	if( -1 != httpUrl.indexOf(xwiki_attachment_signature) ) {
		var parts = httpUrl.split(xwiki_attachment_signature);
		var elements = parts[1].split("/");
		if( elements.length == 3 ) {
			davUrl = parts[0] + xwiki_webdav_signature + elements[0] + "/" + elements[0] + "." + elements[1] + "/" + elements[2];
		}
	}
	return davUrl;
}
