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
                str += "<span id='texttoolbar'>" + this.getTextToolbar() + "</span>";
                break;
            case "justifytoolbar":
                str += "<span id='justifytoolbar'>" + this.TOOLBAR_SPACER + this.getJustifyToolbar() + "</span>";
                break;
            case "listtoolbar":
                str += "<span id='listtoolbar'>" + this.TOOLBAR_SPACER + this.getListToolbar()+ "</span>";
                break;
            case "indenttoolbar":
                str += "<span id='indenttoolbar'>" + this.TOOLBAR_SPACER + this.getTabToolbar() + "</span>";
                break;
            case "undotoolbar":
                str += "<span id='undotoolbar'>" + this.TOOLBAR_SPACER + this.getUndoToolbar() + "</span>";
                break;
            case "titletoolbar":
                str += "<span id='titletoolbar'>" + this.TOOLBAR_SPACER + this.getTitleToolbar() + "</span>";
                break;
            case "styletoolbar":
                str += "<span id='styletoolbar'>" + this.TOOLBAR_SPACER + this.getStyleToolbar() + "</span>";
                break;
            case "horizontaltoolbar":
                str += "<span id='horizontaltoolbar'>" + this.TOOLBAR_SPACER + this.getHorizontalruleControls() + this.getRemoveformatControls() + "</span>";
                break;
            case "symboltoolbar":
                str += "<span id='symboltoolbar'>" + this.getSymbolToolbar() + "</span>";
                break
            case "suptoolbar":
                str += "<span id='suptoolbar'>" + this.TOOLBAR_SPACER + this.getSupAndSubToolbar() + "</span>";
                break;
            case "tabletoolbar":
                str += "<span id='tabletoolbar'>" + this.TOOLBAR_SPACER + this.getTableToolbar() + "</span>";
                break;
            case "tablerowtoolbar":
                str += "<span id='tablerowtoolbar'>" + this.TOOLBAR_SPACER + this.getTableRowToolbar() + "</span>";
                break;
            case "tablecoltoolbar":
                str += "<span id='tablecoltoolbar'>" + this.TOOLBAR_SPACER + this.getTableColToolbar() + "</span>";
                break;
            case "linktoolbar":
                str += "<span id='linktoolbar'>" + this.TOOLBAR_SPACER + this.getLinkToolbar() + "</span>";
                break;
            case "attachmenttoolbar":
                if (this.isPluginLoaded("attachments")) {
                    str += "<span id='attachmenttoolbar'>" + this.TOOLBAR_SPACER + this.getAttachmentsToolbar() + "</span>";
                }
                break;
            case "macrostoolbar":
                if (this.isPluginLoaded("macros")) {
                    str += "<span id='macrostoolbar'>" + this.getMacrosToolbar() + "</span>";
                }
                break
            case "togglebutton":
                str += "<span id='togglebutton'>" + this.getToggleButton() + "</span>";
                break;
        }
    }
    
    str += '</td></tr>\
			<tr><td align="center">\
		    <span id="{$editor_id}">IFRAME</span>\
		    </td></tr>\
            <tr><td>\
            <textarea name="content" id="content" cols="98" rows="23" style="display:none"></textarea>\
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