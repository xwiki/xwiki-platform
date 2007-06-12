/*
           Default Theme
		   
	     XWiki WYSIWYG Syntax Editor
	Created by Pedro Ornelas for XWiki.org 
	under the Google Summer of Code 2005 program.
*/

WikiEditor.prototype.getEditorTemplate = function(settings, editor_id) {
	var template = new Array();

	var str = '\
		<table class="mceEditor" border="0" cellpadding="0" cellspacing="0" width="{$width}" height="{$height}"><tbody>\
		<tr><td class="mceToolbar" align="center" height="1">';

	str += this.getTextToolbar() + this.TOOLBAR_SPACER + this.getListToolbar() + this.TOOLBAR_SPACER +
           this.getTabToolbar() + this.TOOLBAR_SPACER + this.getUndoToolbar() + this.TOOLBAR_SPACER + this.getTitleToolbar();

    if (this._useStyleToolbar) {
        str += this.TOOLBAR_SPACER + this.getStyleToolbar();
    }

    str += this.TOOLBAR_SPACER + this.getTableToolbar() + this.TOOLBAR_SPACER + this.getTableRowToolbar() +
           this.TOOLBAR_SPACER + this.getTableColToolbar() + this.TOOLBAR_SPACER + this.getLinkToolbar();
    str += this.TOOLBAR_SPACER + this.getHorizontalruleControls() + this.getRemoveformatControls();

    if (this.isPluginLoaded("attachments")) {
        str += this.TOOLBAR_SPACER + this.getAttachmentsToolbar();
	}
    if (this.isPluginLoaded("macros")) {
        str +=  this.getMacrosToolbar();
    }
    str += '</td></tr>\
			<tr><td align="center">\
		    <span id="{$editor_id}">IFRAME</span>\
		    </td></tr>\
            </tbody></table>';
	template['html'] = str;
	template['delta_width'] = 0;
	template['delta_height'] = -40;
	//var elm1 = document.getElementById('elm1');
	//elm1.value = str;
	return template;
}

WikiEditor.prototype.TOOLBAR_SPACER = ' <img src="{$themeurl}/images/spacer.gif" width="1" height="15" class="mceSeparatorLine">';

// TODO: Use a tile map so that only one image is needed for loading (optimization)
WikiEditor.prototype.createButtonHTML = function(id, img, lang, cmd, ui, val, posTileMap) {
    cmd = 'tinyMCE.execInstanceCommand(\'{$editor_id}\',\'' + cmd + '\'';

    if (typeof(ui) != "undefined" && ui != null)
        cmd += ',' + ui;

    if (typeof(val) != "undefined" && val != null)
        cmd += ",'" + val + "'";

    cmd += ');';

    if(typeof(posTileMap) == "undefined") {
       // Use individual image
        return  '<a id="{$editor_id}_' + id + '" href="javascript:' + cmd + '" onclick="' + cmd + 'return false;" onmousedown="return false;" class="mceButtonNormal" target="_self">' +
                '<img src="{$themeurl}/images/' + img + '" title="{$' + lang + '}" />' +
                '</a>';
    } else {
        // TODO: use a tile map
    }
}