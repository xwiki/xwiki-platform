/*
            Syntax Macros Plugin

	     XWiki WYSIWYG Syntax Editor
	Created by PhungHaiNam(phunghainam@xwiki.com) for XWiki.org
*/

WikiEditor.prototype.initMacrosPlugin = function() {
	if(!this.isPluginLoaded('core')) {
		alert("Macros Plugin: You must load the core syntax plugin before!");
		return;
	}

    this.addToolbarHandler('handleMacrosButtons');

	this.addCommand('wikiMacro', 'macroCommand');
}

wikiEditor.initMacrosPlugin();

WikiEditor.prototype.insertMacro = function(editor_id, name) {
	this.core.execInstanceCommand(editor_id, "mceInsertRawHTML", false, name);
}


WikiEditor.prototype.macroCommand = function(editor_id, element, command, user_interface, value) {
	var template = new Array();

    template['file'] = 'macro.htm';
	template['width'] = 420;
	template['height'] = 290;

    tinyMCE.openWindow(template, {editor_id : editor_id, scrollbars : 'yes',  resizable : 'no', mce_windowresize: false});

    return this.dummyCommand();
}

WikiEditor.prototype.handleMacrosButtons = function(editor_id, node, undo_index, undo_levels, visual_aid, any_selection) {
   tinyMCE.switchClassSticky(editor_id + '_image', 'mceButtonNormal');
	do
	{
		switch (node.nodeName.toLowerCase())
		{
			case "macro":
					tinyMCE.switchClassSticky(editor_id + '_image', 'mceButtonSelected');
				break;
		}
	} while ((node = node.parentNode));
}

WikiEditor.prototype.getMacrosToolbar = function() {
    return this.getMacrosControls("macro");
}

WikiEditor.prototype.getMacrosControls = function(button_name) {
	var str="";
	switch(button_name) {
		case 'macro':
			str = this.createButtonHTML('macro', 'macro.gif', '{$lang_macro_desc}', 'wikiMacro', true);
			break;
	}
	return str;
}
