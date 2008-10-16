/* 
 * Gain access to the Prefences service.
 */
var prefManager = Components.classes["@mozilla.org/preferences-service;1"]
                            .getService(Components.interfaces.nsIPrefBranch);


/*
 * Called when WebDAV preferences screen is loaded.
 */
function populateAssociationsList() {
	var prefs = prefManager.getCharPref("extensions.foxwiki.associations");
	if( prefs == "" ) {
		return;
	}
	var associations = prefs.split(",");
	var associationsList = document.getElementById("associationsList");
	for( var i = 0; i < associations.length; i++ ) {
		var parts = associations[i].split("=");
	
		var treeItem = document.createElement("treeitem");
		var treeRow = document.createElement("treerow");
		treeItem.appendChild(treeRow);

		var ext = document.createElement("treecell");
		ext.setAttribute("label", parts[0]);
		treeRow.appendChild(ext);

		var appPath = document.createElement("treecell");
		appPath.setAttribute("label", parts[1]);
		treeRow.appendChild(appPath);

		associationsList.appendChild(treeItem);	
	}
}

/**
 * Called when the associations table (tree) is changed.
 */
function saveAssociationsList() {
	var associationsList = document.getElementById("associationsList").childNodes;
	var prefs = "";
	for( var i = 0; i < associationsList.length; i++ ) {
		var columns = associationsList[i].childNodes[0].childNodes;
		var pref = columns[0].getAttribute("label") + "="
                      + columns[1].getAttribute("label");
		if( prefs == "" ) {
			prefs = pref;
		} else {
			prefs += "," + pref;
		}
	}
	return prefs;
}

/**
 * Called when the user clicks the 'Add' Button.
 */
function addAssociation() {
	var extensionsInput = document.getElementById("fileExtensionsInputField").value.replace(/^\s+|\s+$/g, '') ;
	var appPathInput = document.getElementById("applicationPathInputField").value.replace(/^\s+|\s+$/g, '') ;
	var associationsList = document.getElementById("associationsList");
	// TODO : We might want to validate these strings a little bit more thoroughly.
	if( extensionsInput == "" || appPathInput == "" ) {
		alert("A required field is either missing or is invalid.");
		return;
	}
	var extensions = extensionsInput.split(",");
	for( var i = 0; i < extensions.length; i++ ) {
		var extension = extensions[i].replace(/^\s+|\s+$/g, '');
		if( extension == "" ) {
			continue;
		}
		var treeItem = document.createElement("treeitem");
		var treeRow = document.createElement("treerow");
		treeItem.appendChild(treeRow);

		var ext = document.createElement("treecell");
		ext.setAttribute("label", extension);
		treeRow.appendChild(ext);

		var appPath = document.createElement("treecell");
		appPath.setAttribute("label", appPathInput);
		treeRow.appendChild(appPath);

		associationsList.appendChild(treeItem);	
	}
	document.getElementById("fileExtensionsInputField").value = "";
	document.getElementById("applicationPathInputField").value = "";
	var associationsTree = document.getElementById("associationsTree");
      	document.getElementById("webdavpreferences").userChangedValue(associationsTree);
}

/**
 * Called when the user clicks the 'Delete' Button.
 */
function deleteAssociation() {
	var associationsTree = document.getElementById("associationsTree");
	var index = associationsTree.currentIndex;
	if(index != -1) {
		var associationsList = document.getElementById("associationsList");
		var toRemove = associationsList.childNodes.item(index);
		associationsList.removeChild(toRemove);
	      	document.getElementById("webdavpreferences").userChangedValue(associationsTree);
	}
}

/**
 * Called when the user clicks the 'Edit' Button.
 */
function editAssociation() {
	var associationsTree = document.getElementById("associationsTree");
	var index = associationsTree.currentIndex;
	if(index != -1) {
		var associationsList = document.getElementById("associationsList");
		var toEdit = associationsList.childNodes[index];
		var vals = toEdit.childNodes[0].childNodes;
		document.getElementById("fileExtensionsInputField").value = vals[0].getAttribute("label");
		document.getElementById("applicationPathInputField").value = vals[1].getAttribute("label");
		associationsList.removeChild(toEdit);
	}	
}

/**
 * Called when the user clicks the 'Brows' Button.
 */
function browsApplications() {
	const nsIFilePicker = Components.interfaces.nsIFilePicker;
	var fp = Components.classes["@mozilla.org/filepicker;1"].createInstance(nsIFilePicker);
	fp.init(window, "Brows Local Applications", nsIFilePicker.modeOpen);
	fp.appendFilters(nsIFilePicker.filterAll | nsIFilePicker.filterApps);
	var rv = fp.show();
	if (rv == nsIFilePicker.returnOK || rv == nsIFilePicker.returnReplace) {
		document.getElementById("applicationPathInputField").value = fp.file.path;
	}
}
