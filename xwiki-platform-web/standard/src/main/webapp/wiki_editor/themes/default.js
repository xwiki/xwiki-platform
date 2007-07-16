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

    var toolbarParams = tinyMCE.getParam("wiki_editor_toolbar");
    var toolbars = toolbarParams.split(",");
    for (var i = 0; i < toolbars.length; i++) {
        var toolbar = this.trimString(toolbars[i]);
        switch (toolbar) {
            case "texttoolbar":
                str += this.getTextToolbar();
                break;
            case "listtoolbar":
                str += this.TOOLBAR_SPACER + this.getListToolbar();
                break;
            case "indenttoolbar":
                str += this.TOOLBAR_SPACER + this.getTabToolbar();
                break;
            case "undotoolbar":
                str += this.TOOLBAR_SPACER + this.getUndoToolbar();
                break;
            case "titletoolbar":
                str += this.TOOLBAR_SPACER + this.getTitleToolbar();
                break;
            case "styletoolbar":
                str += this.TOOLBAR_SPACER + this.getStyleToolbar();
                break;
            case "horizontaltoolbar":
                str += this.TOOLBAR_SPACER + this.getHorizontalruleControls() + this.getRemoveformatControls();
                break;
            case "symboltoolbar":
                str += this.getSymbolToolbar();
                break
            case "suptoolbar":
                str += this.TOOLBAR_SPACER + this.getSupAndSubToolbar();
                break;
            case "tabletoolbar":
                str += this.TOOLBAR_SPACER + this.getTableToolbar();
                break;
            case "tablerowtoolbar":
                str += this.TOOLBAR_SPACER + this.getTableRowToolbar();
                break;
            case "tablecoltoolbar":
                str += this.TOOLBAR_SPACER + this.getTableColToolbar();
                break;
            case "linktoolbar":
                str += this.TOOLBAR_SPACER + this.getLinkToolbar();    
                break;
            case "attachmenttoolbar":
                if (this.isPluginLoaded("attachments")) {
                    str += this.TOOLBAR_SPACER + this.getAttachmentsToolbar();
                }
                break;
            case "macrostoolbar":
                if (this.isPluginLoaded("macros")) {
                    str += this.getMacrosToolbar();
                }
                break
        }
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