/* Import theme specific language pack */
tinyMCE.importThemeLanguagePack('wikieditor');

// Variable declarations

/**
 * Returns HTML code for the specificed control.
 */
function TinyMCE_wikieditor_getControlHTML(button_name)
{
	return wikiEditor.getControlHTML(button_name);
}

/**
 * Theme specific exec command handeling.
 */
function TinyMCE_wikieditor_execCommand(editor_id, element, command, user_interface, value)
{
	return wikiEditor.execCommand(editor_id, element, command, user_interface, value);
}

/**
 * Editor instance template function.
 */
function TinyMCE_wikieditor_getEditorTemplate(settings, editorId)
{
	return wikiEditor.getEditorTemplate(settings, editorId);
}

/**
 * Insert link template function.
 */
function TinyMCE_wikieditor_getInsertLinkTemplate()
{
	var template = new Array();

	template['file'] = 'link.htm';
	template['width'] = 300;
	template['height'] = 150;

	// Language specific width and height addons
	template['width'] += tinyMCE.getLang('lang_insert_link_delta_width', 0);
	template['height'] += tinyMCE.getLang('lang_insert_link_delta_height', 0);

	return template;
};

/**
 * Insert image template function.
 */
function TinyMCE_wikieditor_getInsertImageTemplate()
{
	var template = new Array();

	template['file'] = 'image.htm?src={$src}';
	template['width'] = 340;
	template['height'] = 280;

	// Language specific width and height addons
	template['width'] += tinyMCE.getLang('lang_insert_image_delta_width', 0);
	template['height'] += tinyMCE.getLang('lang_insert_image_delta_height', 0);

	return template;
};

/**
 * Node change handler.
 */
function TinyMCE_wikieditor_handleNodeChange (editor_id, node, undo_index,
															  undo_levels, visual_aid, any_selection)
{
	wikiEditor.handleNodeChange(editor_id, node, undo_index, undo_levels, visual_aid, any_selection);
	return true;
};
