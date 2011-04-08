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
}

wikiEditor.initMacrosPlugin();

WikiEditor.prototype.insertMacro = function(editor_id, name) {
	this.core.execInstanceCommand(editor_id, "mceInsertRawHTML", false, name);
}

WikiEditor.prototype.handleMacrosButtons = function(editor_id, node, undo_index, undo_levels, visual_aid, any_selection) {
   tinyMCE.switchClass(editor_id + '_macro', 'mceButtonNormal');
	do
	{
		switch (node.nodeName.toLowerCase())
		{
			case "macro":
					tinyMCE.switchClass(editor_id + '_macro', 'mceButtonSelected');
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
			str = this.createButtonHTML('macro', 'macro.gif', 'lang_macro_desc', 'wikiMacro', true);
			break;
	}
	return str;
}
