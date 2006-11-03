/* Import plugin specific language pack */
//tinyMCE.importPluginLanguagePack('wikieditor', 'en'); // <- Add a comma separated list of all supported languages


function TinyMCE_wikiplugin_initInstance(inst) {
}

function TinyMCE_wikiplugin_execCommand(editor_id, element, command, user_interface, value) {
	return false;
}

function TinyMCE_wikiplugin_handleNodeChange(editor_id, node, undo_index, undo_levels, visual_aid, any_selection) {
	return true;
}

/**
 * Gets executed when contents is inserted / retrived.
 */
function TinyMCE_wikiplugin_cleanup(type, content) {
	switch (type) {
		case "get_from_editor":
			//alert("[FROM before] Value HTML string: " + content);

			content = wikiEditor.convertInternal(content);

			//alert("[FROM after] Value HTML string: " + content);

			break;

		case "get_from_editor_dom":

			content = wikiEditor.tagListInternal(content);
			//alert("[FROM] Value DOM Element " + content.innerHTML);

			break;

		case "insert_to_editor":
			//alert("[TO not modified] Value HTML string: " + content);
			content = wikiEditor.convertExternal(content);

            break;
	}

	return content;
}