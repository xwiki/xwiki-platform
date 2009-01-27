/* Import plugin specific language pack */
//tinyMCE.importPluginLanguagePack('wikieditor', 'en'); // <- Add a comma separated list of all supported languages

// modified by : Phung Hai Nam (phunghainam@xwiki) for XWiki.
// version : 10 Oct 2006

// Singleton class
var TinyMCE_wikipluginPlugin = {
	getInfo : function() {
		return {
			longname : 'Wiki Plugin'
		};
	},

	/**
	 * Gets executed when a TinyMCE editor instance is initialized.
	 */
	initInstance : function(inst) {
	},

	/**
	 * Executes a specific command, this function handles plugin commands.
	 */
	execCommand : function(editor_id, element, command, user_interface, value) {
		// Pass to next handler in chain
		return false;
	},

	handleNodeChange : function(editor_id, node, undo_index, undo_levels, visual_aid, any_selection) {
        return true;
	},

	/**
	 * Gets called when a TinyMCE editor instance gets filled with content on startup.
	 */
	setupContent : function(editor_id, body, doc) {
	},

	/**
	 * Gets called when the contents of a TinyMCE area is modified, in other words when a undo level is added.
	 */
	onChange : function(inst) {
	},

    handleEvent : function(e) {
		// Display event type in statusbar
		top.status = "wiki plugin event: " + e.type;

        return true; // Pass to next handler
	},

	/**
	 * Gets called when HTML contents is inserted/retrived from a TinyMCE editor instance.
	 */
	cleanup : function(type, content, inst) {

		switch (type) {
			case "get_from_editor":
                var sourceTextAreaId = inst.editorId + "_content";
                var sourceTextArea = document.getElementById(sourceTextAreaId);
                if (sourceTextArea && sourceTextArea.style.display != "none")
                  return sourceTextArea.value;
                else {
                  content = wikiEditor.convertInternal(content);
                }
                break;

            case "insert_to_editor":
                content = wikiEditor.convertExternal(content);
				break;

            case "get_from_editor_dom":
                content = wikiEditor.encodeNode(content);
                content = wikiEditor.tagListInternal(content);
                break;

            case "insert_to_editor_dom":
                break;
		}
		return content;
	}

};

tinyMCE.addPlugin("wikiplugin", TinyMCE_wikipluginPlugin);
