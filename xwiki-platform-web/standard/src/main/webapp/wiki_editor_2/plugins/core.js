/*
            Syntax Core Plugin

	     XWiki WYSIWYG Syntax Editor
	Created by Pedro Ornelas for XWiki.org
	under the Google Summer of Code 2005 program.
*/

WikiEditor.prototype.initCorePlugin = function() {

	// External/Internal conversion setup
    this.addExternalProcessor((/^\s*(1(\.1)*)\s+([^\r\n]*)$/im), 'convertHeadingExternal');
	this.addInternalProcessor((/\s*<h3\s*([^>]*)>([\s\S]+?)<\/h3>/i), 'convertHeadingInternal');

    this.addInternalProcessor((/<p[^>]*>&nbsp;<\/p>/gi), "");

    this.addExternalProcessor((/^\s*((\*+)|#)\s+([^\r\n]+)$/im), 'convertListExternal');
	this.addInternalProcessor((/\s*<(ul|ol)\s*([^>]*)>/i), 'convertListInternal');

	this.addExternalProcessor((/^s*----(\-)*\s*$/gim), '<hr class="line" \/>');
	this.addInternalProcessor((/<hr[^>]*>/gi), '----');

    // Must remove the html tag format so it won't interfere with paragraph conversion
	this.addExternalProcessor((/<%([\s\S]+?)%>/ig), '&lt;%$1%&gt;');

    this.addExternalProcessor((/((\s|\S)*)/i), 'convertParagraphExternal');
	this.addInternalProcessor((/<\s*p\s*([^>]*)>(.*?)<\s*\/\s*p\s*>/gi), '\r\n$2\r\n');

    this.addExternalProcessor((/\[(.*?)(>(.*?))?\]/i), 'convertLinkExternal');
	this.addInternalProcessor((/<a\s*([^>]*)(class=\"wikiexternallink\"|class=\"wikilink\")\s*([^>]*)>(.*?)<\/a>/i), 'convertLinkInternal');

    this.addExternalProcessor((/\{table\}([\s\S]+?)\{table\}/i), 'convertTableExternal');
    this.addInternalProcessor((/<table\s*([^>]*)class=\"wiki-table\"\s*([^>]*)>([\s\S]+?)<\/table>/i), 'convertTableInternal');

    this.addExternalProcessor((/\*(.+?)\*/gi), '<b class="bold">$1<\/b>');
    this.addExternalProcessor((/__(.+?)__/gi), '<b class="bold">$1<\/b>');
    this.addInternalProcessor((/<strong[^>]*>(.*?)<\/strong>/gi), '*$1*');

	this.addExternalProcessor((/~~(.+?)~~/gi), '<i class="italic">$1<\/i>');
	this.addInternalProcessor((/<em[^>]*>(.*?)<\/em>/gi), '~~$1~~');

	this.addExternalProcessor((/--(.+?)--/gi),  '<strike class="strike">$1<\/strike>');
	this.addInternalProcessor((/<strike[^>]*>(.*?)<\/strike>/gi), '--$1--');

	this.addInternalProcessor((/[#$][a-zA-Z0-9-_.]+\(([^&)]*&quot;[^)]*)+?\)/i), 'convertVelocityScriptsInternal');

    this.addInternalProcessor((/&lt;%([\s\S]+?)%&gt;/i), 'convertGroovyScriptsInternal');

    //this.addInternalProcessor((/&nbsp;(?!\|)/gi), "");

    var charStr = "À|Á|Â|Ã|Ä|Å|" +
                  "Æ|Ç|È|É|Ê|Ë|" +
                  "Ì|Í|Î|Ï|Ñ|Ò|" +
                  "Ó|Ô|Õ|Ö|Ø|Ù|" +
                  "Ú|Û|Ü|ß|à|á|" +
                  "â|ã|ä|å|æ|ç|" +
                  "è|é|ê|ë|ì|í|" +
                  "î|ï|ñ|ò|ó|ô|" +
                  "?|õ|ö|ø|ù|ú|" +
                  "û|ü|ÿ|" +
                  // Commercial symbols:
                  "?|©|®|¢|?|¥|" +
                  "£|¤|" +
                  //Other characters
                  ">|<|&|\"";
    var characterEntityStr = "&Agrave;|&Aacute;|&Acirc;|&Atilde;|&Auml;|&Aring;|" +
                             "&AElig;|&Ccedil;|&Egrave;|&Eacute;|&Ecirc;|&Euml;|" +
                             "&Igrave;|&Iacute;|&Icirc;|&Iuml;|&Ntilde;|&Ograve;|" +
                             "&Oacute;|&Ocirc;|&Otilde;|&Ouml;|&Oslash;|&Ugrave;|" +
                             "&Uacute;|&Ucirc;|&Uuml;|&szlig;|&agrave;|&aacute;|" +
                             "&acirc;|&atilde;|&auml;|&aring;|&aelig;|&ccedil;|" +
                             "&egrave;|&eacute;|&ecirc;|&euml;|&igrave;|&iacute;|" +
                             "&icirc;|&iuml;|&ntilde;|&ograve;|&oacute;|&ocirc;|" +
                             "&oelig;|&otilde;|&ouml;|&oslash;|&ugrave;|&uacute;|" +
                             "&ucirc;|&uuml;|&yuml;|" +
                             // Commercial symbols:
                             "&trade;|&copy;|&reg;|&cent;|&euro;|&yen;|" +
                             "&pound;|&curren;|" +
                             //Other characters
                             "&gt;|&lt;|&amp;|&quot;"

    var characterEntitys = characterEntityStr.split("|");
    var chars = charStr.split("|");
    for (var i= 0; i< characterEntitys.length; i++) {
        var regExp = new RegExp(characterEntitys[i],'g');
        this.addInternalProcessor((regExp), chars[i]);
    }

    this.setHtmlTagRemover('removeHtmlTags_Groovy');
    this.setHtmlTagRemover('removeSpecialHtmlTags');
    // Toolbar handlers
	this.addToolbarHandler('handleTextButtons');
	this.addToolbarHandler('handleListButtons');
    this.addToolbarHandler('handleIndentButtons');
    this.addToolbarHandler('handleUndoButtons');
	this.addToolbarHandler('handleTitlesList');
	this.addToolbarHandler('handleLinkButtons');
    this.addToolbarHandler('handleTableButtons');

    // Add Comands and Fix Commands(workarounds)
	this.addCommand('Title', 'titleCommand');
	this.addFixCommand('Title', 'fixTitle');

	this.addFixCommand("InsertUnorderedList", 'fixInsertUnorderedList');
	this.addFixCommand("Indent", 'fixInsertUnorderedList');
}

wikiEditor.initCorePlugin();

// This function will not strip groovy tags
WikiEditor.prototype.removeHtmlTags_Groovy = function(str) {
	var remove_html_tags_regexp = /<[^%][^>]*>/i;
    return str.replace(remove_html_tags_regexp, "");
}

//  This will remove some special Html tags to fix some bugs when switch between text and wysiwyg editor
//  We will replace or remove this method in future when find out the better solutions.
WikiEditor.prototype.removeSpecialHtmlTags = function(str) {
    str = str.replace(/<div class="paragraph">([\s\S]+?)<\/div>/g,'$1');
    str = str.replace(/<p class="paragraph">\s*([\s\S]+?)<\/p>/g,'$1');
    str = str.replace(/<span class="wikilink">\s*([\s\S]+?)<\/span>/g,'$1');
    str = str.replace(/<span class="wikiexternallink">\s*([\s\S]+?)<\/span>/g,'$1');
    str = str.replace(/<span class="bold">([\s\S]+?)<\/span>/g,'$1');
    str = str.replace(/<span class="italic">([\s\S]+?)<\/span>/g,'$1');
    str = str.replace(/<span class="strike">([\s\S]+?)<\/span>/g,'$1');    
    str = str.replace(/<\/?p[^>]*>/gi, "");
    str = str.replace(/<br \/>/g, '\r\n');
    return str;
}

WikiEditor.prototype.convertVelocityScriptsInternal = function(regexp, result, content) {
	var r = /&quot;/gi;
	return content.replace(regexp, result[0].replace(r, '"'));
}

WikiEditor.prototype.convertGroovyScriptsInternal = function(regexp, result, content) {
	var r = /&quot;/gi;
	var str = "<%" + result[1].replace(r, '"') + "%>";
	return content.replace(regexp, str);
}

WikiEditor.prototype.convertLinkInternal = function(regexp, result, content) {
    var txt;
    var str="";
    var href;
    if( (txt = this.trimString(result[4])) != "") {
        var att = this.readAttributes(result[1] + " " + result[3]);
        if(att && att["href"]) {
            href = this.trimString(att["href"]);
            href = href.replace(/%20/g," ");
            if(href.toLowerCase() == txt.toLowerCase()) {
                str = "[" + txt + "]";
            } else {
                str = "[" + txt + ">" + href + "]";
            }
        }
    }
    return content.replace(regexp, str);
}

WikiEditor.prototype.convertTableInternal = function(regexp, result, content) {
    var text = this.trimString(result[3]);
    var str = "";
    if (browser.isIE) str += "\r\n";
    str += "{table}\r\n";
    var rows = text.split("<\/tr>");
    for(var i=0; i< (rows.length - 1); i++) {
        if( i == 0) rows[i] = this.trimString(rows[i].replace(/(.*?)<tr\s*([^>]*)>/g, ""));
        else rows[i] = this.trimString(rows[i].replace(/<tr\s*([^>]*)>/g, ""));
        var cols = rows[i].split("<\/td>");
        for(var j=0; j< cols.length-1; j++) {
            cols[j] = cols[j].replace(/<td\s*([^>]*)>/g, "");  // remove <td> tag
            var lines = this._getLines(cols[j]);
            var colj = "";
            if (lines.length == 1) colj = cols[j].replace(/<br \/>/g, "").replace(/\r\n/g, "");
            else if (lines.length > 1)
                for (var l=0; l < lines.length; l++)
                    lines[l] = lines[l].replace(/<br \/>|\r|\n/g, "");
                for (var k=0; k < lines.length; k++)
                    if (lines[k] != "")
                        if (k < (lines.length - 2)) colj += (lines[k] + "\\\\" + "\r\n");
                        else if (k == (lines.length - 2))
                            if (lines[k+1] != "")
                                colj += lines[k] + "\\\\" + "\r\n" + lines[k+1];
                            else colj += lines[k];
            if (j != (cols.length - 2)) str += (colj + "|") ;
            else str += (colj + "\r\n");
        }
    }
    str += "{table}";
    if (browser.isIE) str += "\r\n";
    return content.replace(regexp, str);
}

WikiEditor.prototype.convertHeadingInternal = function(regexp, result, content) {
	var str = "";
	var txt;
	var headr = /heading((-1)+)/i;
	if( (txt = this.trimString(result[2])) != "") {
		var att = this.readAttributes(result[1]);
		if(att && att["class"]) {
			var r = headr.exec(att["class"]);
            if(r) {
				var n = r[1].split("-").length-1;
				str = "\r\n1";
				str += this.buildString(".1", n-1);
				str += " " + txt;
			}
		}
	}
    str += "\r\n";
	return content.replace(regexp, str);
}

WikiEditor.prototype.fixTitle = function(editor_id, node) {
    var newNode;
    if(this._titleChangeValue == 0) { // erase the title
        if(this.core.isMSIE) {
			newNode = node.ownerDocument.createElement("p");
			this._substituteNode(node, newNode);
		}else {
			this.core.execInstanceCommand(editor_id, "FormatBlock", false, "p");
			newNode = this.core.getParentElement(node, "p");
		}
		newNode.className = this.PARAGRAPH_CLASS_NAME;
	} else {
		var cn = "heading" + this.buildString("-1", this._titleChangeValue);
		if(this.core.isMSIE) {
            newNode = node.ownerDocument.createElement("h3");
            this._substituteNode(node, newNode);
		}else {
            if (this.core.getParentElement(node, "h3")) {
                this.core.execInstanceCommand(editor_id, "FormatBlock", false, "h3");
                newNode = this.core.getParentElement(node, "h3");
            } else {
                // This condition to fix bug in when mirgrate to tiny_mce 2
				newNode = node.ownerDocument.createElement("h3");
                this._substituteNode(node, newNode);
            }
        }
        newNode.className = cn;
	}
	this.core.triggerNodeChange();
}

WikiEditor.prototype._substituteNode = function(oldNode, newNode) {
	var parent = oldNode.parentNode;
	if(parent) {
		for(var i=0; oldNode.childNodes[i];i++) {
			newNode.appendChild(oldNode.childNodes[i]);
		}
        // fix bug oldNode is BODY in MSIE
        if (oldNode.nodeName.toLowerCase() == 'body') {
            oldNode.appendChild(newNode);
        } else {
            parent.insertBefore(newNode, oldNode);
            parent.removeChild(oldNode);
        }
    }
}

WikiEditor.prototype.titleCommand = function(editor_id, element, command, user_interface, value) {
	this._titleChangeValue = value;
	return this.dummyCommand();
}

WikiEditor.prototype.fixInsertUnorderedList = function(editor_id, node) {
	do {
		switch (node.nodeName.toLowerCase()) {
			case "ul":
				node.className = this.LIST_NORMAL_CLASS_NAME;
				break;
		}
	} while ((node = node.parentNode));
}

WikiEditor.prototype._cleanNode = function(editor_id, node) {
	do {
		switch (node.nodeName.toLowerCase()) {
			case "body":
				//this.__removeBlankParagraphs(node);
				return;
			case "p":
				//this._fixParagraph(node);
			case "h3":
			//case "ul":
			//case "ol":
				if(node.parentNode && node.parentNode.nodeName.toLowerCase() == "body") {
					//this._cleanBR(node);
				}

			break;
		}
	} while ((node = node.parentNode));
}

WikiEditor.prototype._removeBlankParagraphs = function(node) {

	do {
		if (node.nodeName.toLowerCase() == "body") {
			break;
		}
	} while ((node = node.parentNode));

	this.__removeBlankParagraphs(node);
}

WikiEditor.prototype.__removeBlankParagraphs = function(node) {
	if(node.nodeName.toLowerCase() == "p" && this.trimString(node.innerHTML) == "") {
		node.parentNode.removeChild(node);
		return;
	}
	for(var i=0; node.childNodes[i]; i++) {
		this.__removeBlankParagraphs(node.childNodes[i]);
	}
}

WikiEditor.prototype._fixParagraph = function(node) {
	// Garantee that all paragraphs have the necessary class
	if(node.className || node.className.toLowerCase() != this.PARAGRAPH_CLASS_NAME.toLowerCase()) {
		node.className = this.PARAGRAPH_CLASS_NAME;
	}
}

WikiEditor.prototype._cleanBR = function(node) {
	if(node.nodeName.toLowerCase() == "br") {
		node.parentNode.removeChild(node);
		return;
	}

	for(var i=0; node.childNodes[i]; i++) {
		this._cleanBR(node.childNodes[i]);
	}
}

WikiEditor.prototype.handleLinkButtons = function(editor_id, node, undo_index, undo_levels, visual_aid, any_selection) {
	// Reset
	tinyMCE.switchClass(editor_id + '_link', 'mceButtonDisabled', true);
	tinyMCE.switchClass(editor_id + '_unlink', 'mceButtonDisabled', true);

	// Get link
	var anchorLink = tinyMCE.getParentElement(node, "a", "href");

	if (anchorLink || any_selection)
	{
		tinyMCE.switchClass(editor_id + '_link', anchorLink ? 'mceButtonSelected' : 'mceButtonNormal', false);
	}
	if(anchorLink) {
		tinyMCE.switchClass(editor_id + '_unlink', 'mceButtonNormal', false);
	}
}

WikiEditor.prototype.handleTableButtons = function(editor_id, node, undo_index, undo_levels, visual_aid, any_selection) {
    tinyMCE.switchClass(editor_id + '_table', 'mceButtonNormal', false);
}

WikiEditor.prototype.handleTitlesList = function(editor_id, node, undo_index, undo_levels, visual_aid, any_selection) {
	var list = document.getElementById(editor_id + "_titleSelect");
	if(list) {
		var h3 = this.core.getParentElement(node, "h3");
		if(h3) {
			var classname = h3.className;
			var n = (classname.split("-").length)-1;
			this._selectByValue(list, n);
		} else {
			this._selectByValue(list, 0);
		}
	}
}

WikiEditor.prototype.handleIndentButtons = function(editor_id, node, undo_index, undo_levels, visual_aid, any_selection) {
    // indent, outdent for all element
    tinyMCE.switchClass(editor_id + '_outdent', 'mceButtonDisabled', true);
	tinyMCE.switchClass(editor_id + '_indent', 'mceButtonNormal');

    var indent = this.core.getParentElement(node, "blockquote");
    var ul = this.core.getParentElement(node, "ul");
    if (indent || ul) {
        tinyMCE.switchClass(editor_id + '_outdent', 'mceButtonNormal', false);
    }
}

WikiEditor.prototype.handleUndoButtons = function(editor_id, node, undo_index, undo_levels, visual_aid, any_selection) {
	if (undo_levels != -1)
	{
		tinyMCE.switchClass(editor_id + '_undo', 'mceButtonDisabled', true);
		tinyMCE.switchClass(editor_id + '_redo', 'mceButtonDisabled', true);
	}

	// Has redo levels
	if (undo_index != -1 && (undo_index < undo_levels-1 && undo_levels > 0))
	{
		tinyMCE.switchClass(editor_id + '_redo', 'mceButtonNormal', false);
	}

	// Has undo levels
	if (undo_index != -1 && (undo_index > 0 && undo_levels > 0))
	{
		tinyMCE.switchClass(editor_id + '_undo', 'mceButtonNormal', false);
	}
}

WikiEditor.prototype.handleListButtons = function(editor_id, node, undo_index, undo_levels, visual_aid, any_selection) {
	// Reset old states
	tinyMCE.switchClass(editor_id + '_bullist', 'mceButtonNormal');
	tinyMCE.switchClass(editor_id + '_numlist', 'mceButtonNormal');

    do {
		switch (node.nodeName.toLowerCase()) {
			case "ul":
				tinyMCE.switchClass(editor_id + '_bullist', 'mceButtonSelected');
				tinyMCE.switchClass(editor_id + '_outdent', 'mceButtonNormal', false);
				tinyMCE.switchClass(editor_id + '_indent', 'mceButtonNormal', false);
			break;

			case "ol":
				tinyMCE.switchClass(editor_id + '_numlist', 'mceButtonSelected');
			break;
		}
	} while ((node = node.parentNode));
}

WikiEditor.prototype._selectByValue = function(select_elm, value)
{
	if (select_elm)
	{
		for (var i=0; i<select_elm.options.length; i++)
		{
			if (select_elm.options[i].value == value)
			{
				select_elm.selectedIndex = i;
				return true;
			}
		}
	}

	return false;
};

WikiEditor.prototype.handleTextButtons = function(editor_id, node, undo_index, undo_levels, visual_aid, any_selection) {
	// Reset old states
	this.core.switchClass(editor_id + '_bold', 'mceButtonNormal');
	this.core.switchClass(editor_id + '_italic', 'mceButtonNormal');
	this.core.switchClass(editor_id + '_strikethrough', 'mceButtonNormal');

	// Handle elements
	do
	{
		switch (node.nodeName.toLowerCase())
		{
			case "b":
			case "strong":
				//alert("strong");
				this.core.switchClass(editor_id + '_bold', 'mceButtonSelected');
			break;

			case "i":
			case "em":
				this.core.switchClass(editor_id + '_italic', 'mceButtonSelected');
			break;

			case "strike":
				this.core.switchClass(editor_id + '_strikethrough', 'mceButtonSelected');
			break;
		}
	} while ((node = node.parentNode));
}

WikiEditor.prototype.getUndoToolbar = function() {
	return this.getUndoControls('undo') + this.getUndoControls('redo');
}

WikiEditor.prototype.getUndoControls = function(button_name) {
	str = "";
	switch(button_name) {
		case 'undo':
			str = this.createButtonHTML('undo', 'undo.gif', 'lang_undo_desc', 'Undo');
			break;
		case 'redo':
			str = this.createButtonHTML('redo', 'redo.gif', 'lang_redo_desc', 'Redo');
			break;
	}
	return str;
}

WikiEditor.prototype.getTabToolbar = function() {
	return this.getTabControls('outdent') + this.getTabControls('indent');
}

WikiEditor.prototype.getTabControls = function(button_name) {
	str = "";
	switch(button_name) {
		case 'outdent':
			str = this.createButtonHTML('outdent', 'outdent.gif', 'lang_outdent_desc', 'Outdent');
			break;
		case 'indent':
			str = this.createButtonHTML('indent', 'indent.gif', 'lang_indent_desc', 'Indent');
			break;
	}
	return str;
}

WikiEditor.prototype.getLinkToolbar = function() {
	return this.getLinkControls("link") + this.getLinkControls("unlink");
}

WikiEditor.prototype.getLinkControls = function(button_name) {
	var str="";
	switch(button_name) {
		case 'link':
			str = this.createButtonHTML('link', 'link.gif', 'lang_link_desc', 'mceLink', true);
			break;
		case 'unlink':
			str = this.createButtonHTML('unlink', 'unlink.gif', 'lang_unlink_desc', 'unlink');
			break;
	}
	return str;
}

WikiEditor.prototype.getTableToolbar = function() {
	return this.getTableControls("table");
}

WikiEditor.prototype.getTableControls = function(button_name) {
    var str="";
    switch(button_name) {
        case 'table':
            str = this.createButtonHTML('table', 'table.gif', 'lang_table_desc', 'mceInsertTable',true);
            break;
    }
    return str;
}

WikiEditor.prototype.getTableRowToolbar = function() {
	return this.getTableRowControls("row_before") + this.getTableRowControls("row_after") + this.getTableRowControls("delete_row");
}

WikiEditor.prototype.getTableRowControls = function(button_name) {
    var str="";
    switch(button_name) {
        case 'row_before':
            str = this.createButtonHTML('row_before', 'table_insert_row_before.gif', 'lang_table_row_before_desc', 'mceTableInsertRowBefore');
            break;
        case 'row_after':
            str = this.createButtonHTML('row_after', 'table_insert_row_after.gif', 'lang_table_row_after_desc', 'mceTableInsertRowAfter');
            break;
        case 'delete_row':
            str = this.createButtonHTML('delete_row', 'table_delete_row.gif', 'lang_table_delete_row_desc', 'mceTableDeleteRow');
            break;
    }
    return str;
}

WikiEditor.prototype.getTableColToolbar = function() {
	return this.getTableColControls("col_before") + this.getTableColControls("col_after") + this.getTableColControls("delete_col");
}

WikiEditor.prototype.getTableColControls = function(button_name) {
    var str="";
    switch(button_name) {
        case 'col_before':
            str = this.createButtonHTML('col_before', 'table_insert_col_before.gif', 'lang_table_col_before_desc', 'mceTableInsertColBefore');
            break;
        case 'col_after':
            str = this.createButtonHTML('col_after', 'table_insert_col_after.gif', 'lang_table_col_after_desc', 'mceTableInsertColAfter');
            break;
        case 'delete_col':
            str = this.createButtonHTML('delete_col', 'table_delete_col.gif', 'lang_table_delete_col_desc', 'mceTableDeleteCol');
            break;
    }
    return str;
}

WikiEditor.prototype.getTitleToolbar = function() {
	return this.getTitleControl();
}

WikiEditor.prototype.getTitleControl = function(button_name) {
	return '<select id="{$editor_id}_titleSelect" name="{$editor_id}_titleSelect" class="mceSelectList" onchange="tinyMCE.execInstanceCommand(\'{$editor_id}\',\'Title\',false,this.options[this.selectedIndex].value);wikiEditor.executedCommand(\'Title\');">' +
            '<option value="0">{$lang_wiki_title_menu}</option>' +
            '<option value="1">{$lang_wiki_title_1}</option>' +
            '<option value="2">{$lang_wiki_title_2}</option>' +
            '<option value="3">{$lang_wiki_title_3}</option>' +
            '<option value="4">{$lang_wiki_title_4}</option>' +
            '<option value="5">{$lang_wiki_title_5}</option>' +
           '</select>';
}

WikiEditor.prototype.getStyleToolbar = function() {
    return this.getStyleControl("fontselect") + this.getStyleControl("fontSizeSelect") + this.getStyleControl("mceForeColor") + this.getStyleControl("mceBackColor");
}

WikiEditor.prototype.getStyleControl = function(button_name) {
    switch(button_name) {
        case 'mceForeColor' :
            return this.createButtonHTML('forecolor', 'forecolor.gif', 'lang_theme_forecolor_desc', 'mceForeColor', true);
        case 'fontSizeSelect':
            return '<select id="{$editor_id}_fontSizeSelect" name="{$editor_id}_fontSizeSelect" onfocus="tinyMCE.addSelectAccessibility(event, this, window);" onchange="tinyMCE.execInstanceCommand(\'{$editor_id}\',\'FontSize\',false,this.options[this.selectedIndex].value);" class="mceSelectList">' +
                '<option value="0">{$lang_theme_font_size}</option>' +
				'<option value="1">1 (8 pt)</option>' +
				'<option value="2">2 (10 pt)</option>' +
				'<option value="3">3 (12 pt)</option>' +
				'<option value="4">4 (14 pt)</option>' +
				'<option value="5">5 (18 pt)</option>' +
				'<option value="6">6 (24 pt)</option>' +
				'<option value="7">7 (36 pt)</option>' +
				'</select>';

        case "fontselect":
            var fontHTML = '<select id="{$editor_id}_fontNameSelect" name="{$editor_id}_fontNameSelect" onfocus="tinyMCE.addSelectAccessibility(event, this, window);" onchange="tinyMCE.execInstanceCommand(\'{$editor_id}\',\'FontName\',false,this.options[this.selectedIndex].value);" class="mceSelectList"><option value="">{$lang_theme_fontdefault}</option>';
            var iFonts = 'Arial=arial,helvetica,sans-serif;Courier New=courier new,courier,monospace;Georgia=georgia,times new roman,times,serif;Tahoma=tahoma,arial,helvetica,sans-serif;Times New Roman=times new roman,times,serif;Verdana=verdana,arial,helvetica,sans-serif;Impact=impact;WingDings=wingdings';
            var nFonts = 'Andale Mono=andale mono,times;Arial=arial,helvetica,sans-serif;Arial Black=arial black,avant garde;Book Antiqua=book antiqua,palatino;Comic Sans MS=comic sans ms,sand;Courier New=courier new,courier;Georgia=georgia,palatino;Helvetica=helvetica;Impact=impact,chicago;Symbol=symbol;Tahoma=tahoma,arial,helvetica,sans-serif;Terminal=terminal,monaco;Times New Roman=times new roman,times;Trebuchet MS=trebuchet ms,geneva;Verdana=verdana,geneva;Webdings=webdings;Wingdings=wingdings,zapf dingbats';
            var fonts = tinyMCE.getParam("theme_advanced_fonts", nFonts).split(';');
            for (i=0; i<fonts.length; i++) {
                if (fonts[i] != '') {
                    var parts = fonts[i].split('=');
                    fontHTML += '<option value="' + parts[1] + '">' + parts[0] + '</option>';
                }
            }

            fontHTML += '</select>';
            return fontHTML;

       case 'mceBackColor' :
               return this.createButtonHTML('backcolor', 'backcolor.gif', 'lang_theme_backcolor_desc', 'mceBackColor', true)
    }
}

WikiEditor.prototype.getListToolbar = function() {
	return this.getListControls('bullist') + this.getListControls('numlist');
}

WikiEditor.prototype.getListControls = function(button_name) {
	var str="";

	switch(button_name) {
		case 'bullist':
			str = this.createButtonHTML('bullist', 'bullist.gif', 'lang_bullist_desc', 'InsertUnorderedList');
			break;
		case 'numlist':
            str = this.createButtonHTML('numlist', 'numlist.gif', '$lang_numlist_desc', 'InsertOrderedList');
			break;
	}

	return str;
}

WikiEditor.prototype.getTextToolbar = function() {
	return this.getTextControls('bold') + this.getTextControls('italic') + this.getTextControls('strikeout');
}

WikiEditor.prototype.getTextControls = function(button_name) {
	var str="";
	switch(button_name) {
		case 'bold':
			str = this.createButtonHTML('bold', '{$lang_bold_img}', 'lang_bold_desc', 'Bold');
			break;
		case 'italic':
			str = this.createButtonHTML('italic', '{$lang_italic_img}', 'lang_italic_desc', 'Italic');
			break;
		case 'strikeout':
			str = this.createButtonHTML('strikethrough', 'strikethrough.gif', 'lang_striketrough_desc', 'Strikethrough');
			break;
	}
	return str;
}

WikiEditor.prototype.PARAGRAPH_CLASS_NAME = "paragraph";

WikiEditor.prototype.convertParagraphExternal = function(regexp, result, content) {
	//alert(content);
	var lines = this._getLines(content);
	var str="";
	var line = "";
	var insideP = false;
	var firstLine = false;

	if(lines == null || lines.length == 0) {
		return "";
	}

	for(var i=0; i < lines.length; i++) {
		//alert("line(" + i + "): " + lines[i]);
		// Consume blank spaces
		line = this.trimString(lines[i]);
		var hh = this._hasHTML(line);
		if(line != "" && !hh) {
			if(!insideP) {
				insideP=true;
				firstLine = true;
				str += '<p class="' + this.PARAGRAPH_CLASS_NAME + '" >\r\n';
			}
			str += line + "\r\n";

			firstLine = false;
			continue;
		} else if(insideP) {
			insideP = false;
			str += '<\/p>\r\n';
		}
		if(hh) {
			str += line + "\r\n";
		}
	}
	if(insideP) {
		str += '<\/p>\r\n';
	}
	//alert(str);
	return str;
}

WikiEditor.prototype._getLines = function(content) {
	var s;

	if(this.core.isMSIE) {
		//var t = /(.*)\n/g;
		//content = content.replace(t, "$1&newline;");
		//alert(content);
		//s = '&newline;';
		s = '\n';
	} else {
		s = '\n';
	}

	return content.split(s);
}

WikiEditor.prototype._hasHTML = function(str) {
	var reg = /<[^>]+>/i;
	return (reg.exec(str)!=null);
}

WikiEditor.prototype.LIST_NORMAL_CLASS_NAME = "star";
WikiEditor.prototype.LIST_NUMERIC_CLASS_NAME = "";

WikiEditor.prototype.convertListExternal = function(regexp, result, content) {
	var subContent = content.substring(result["index"], content.length);
	var str = "";
	switch (result[1].charAt(0)) {
		case '*':
			str = this._convertRecursiveListExternal(regexp, subContent, 0);
			break;
		case '#':
			str = this._convertGenericListExternal(regexp, subContent, "ol", this.LIST_NUMERIC_CLASS_NAME);
			break;
	}
	return content.substring(0, result["index"]) + "\r\n" + str;
}

WikiEditor.prototype._convertGenericListExternal = function(regexp, content, tagname, classname) {
	var str = "<" + tagname + " class=\"" + classname + "\">\r\n";
	var r;
	var _content = content;
	RegExp.lastIndex = 0;
	while( (r = regexp.exec(_content)) && r["index"] == 0) {
		str += "<li>" + this.trimString(r[3]) + "<\/li>\r\n";
		//alert("d: " + r[0]);
		_content = _content.substring(r[0].length, _content.length);
		RegExp.lastIndex = 0;
	}

	str += "<\/" + tagname + ">\r\n" + _content;

	return str;
}

WikiEditor.prototype._convertRecursiveListExternal = function(regexp, content, depth) {
	var str = "";
	RegExp.lastIndex = 0;
	var r = regexp.exec(content);
	var currdepth = (r != null && r[1].charAt(0) == '*' && r["index"]==0) ? r[1].length : 0; // number of "*", if no list element found on next line then list section is over
	var lastPos = (currdepth > 0) ? r[0].length : 0;
	var subContent = content.substring(lastPos, content.length);
	var depthdif = currdepth - depth;
	var tag = (depthdif > 0) ? "<ul class=\"" + this.LIST_NORMAL_CLASS_NAME + "\">" : "<\/ul>";
	for(var i=0; i < Math.abs(currdepth-depth);i++) {
		str += tag + "\r\n";
	}

	if(currdepth > 0) {
		str += "<li>" + this.trimString(r[3]) + "<\/li>\r\n";
		str += this._convertRecursiveListExternal(regexp, subContent, currdepth);
	} else {
		str += content;
	}

	return str;
}

WikiEditor.prototype.LINK_EXTERNAL_CLASS_NAME = "wikiexternallink";
WikiEditor.prototype.LINK_INTERNAL_CLASS_NAME = "wikilink";

WikiEditor.prototype.convertLinkExternal = function(regexp, result, content) {
	var text = result[1];
	var url = (result[3])?(result[3]):(text);
	var classname;
	if(this.isExternalLink(url)) {
		classname = this.LINK_EXTERNAL_CLASS_NAME;
	} else {
		classname = this.LINK_INTERNAL_CLASS_NAME;
	}
	var str = "<a class=\"" + classname + "\" href=\"" + url + "\">" + text + "<\/a>";
	return content.replace(regexp, str);
}

WikiEditor.prototype.convertTableExternal = function(regexp, result, content) {
    var text = this.trimString(result[1]);
    var lines = this._getLines(text);
    var str = "<table class=\"wiki-table\" cellpadding=\"0\" cellspacing=\"0\" align=\"center\">"
    for (var i=0; i < lines.length; i++)
        lines[i] = this.trimString(lines[i].replace(/\r|\n/g, ""));
    var rows = new Array();  // rows of table
    var rowindex = 0;
    for (var i=0; i < lines.length; i++) {
        var row = "";
        var k = 0;
        do {
            row += lines[i+k];
            k++;
        } while ((lines[i + k] != null) && (lines[i+k-1].lastIndexOf("\\\\") == (lines[i+k-1].length - 2)))
        rows[rowindex] = row;
        rowindex++;
        i += (k - 1);
    }

    for (var i=0; i < rows.length; i ++) {
        str += "<tr>";
        var cols = rows[i].split("|");  // get cols
        for (var j=0; j < cols.length; j++) {
            if ( i== 0) str += "<td style='background:#b6c5f2;font-weight:bold;'>";
            else  str += "<td style='background:#FFFFFF'>";
            var linescol = cols[j].split("\\\\");
            if (linescol.length == 1) str += linescol[0];
            else if (linescol.length > 1)
                for (var k=0; k < linescol.length; k++) {
                    if (linescol[k] == "") linescol[k] = "&nbsp;"  // for empty paragraph
                    str += ("<p class='paragraph'>" + linescol[k] + "<\/p>");
                }
            str += "<\/td>";
        }
        str += "<\/tr>";
    }
    str += "<\/table>";
    return content.replace(regexp, str) ;
}

WikiEditor.prototype.isExternalLink = function(url) {
	var regexp = /(https?|ftp):\/\/[-a-zA-Z0-9+&@#\/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#\/%=~_|]/gi;
	return url.search(regexp)>-1;
}

WikiEditor.prototype.convertHeadingExternal = function(regexp, result, content) {
	var n = result[1].split(".").length;
	var str = '\r\n<h3 class="heading' + this.buildString("-1", n) + '">' + this.trimString(result[3]) + '<\/h3>';
	return content.substring(0, result["index"]) + str + content.substring(result["index"] + result[0].length, content.length);
}

WikiEditor.prototype.convertListInternal = function(regexp, result, content) {
    var bounds = this.replaceMatchingTag(content, result[1], null);
	var str = "";
	if(bounds && bounds["start"] > -1) {
		str = this._convertListInternal(content.substring(bounds["start"], bounds["end"]));
        return content.substring(0, bounds["start"]) + "\r\n" + str + content.substring(bounds["end"], content.length);
	}
    return content;
}

/*
	Will return a string containing the list in Wiki Syntax

	TODO: can be optimized with String.match() function
*/
WikiEditor.prototype._convertListInternal = function(content) {
	var list_regexp = /<\s*li\s*([^>]*)>(.*?)<\/\s*li\s*>/gi;
	var result;
	var str = "";

	while( (result = list_regexp.exec(content)) ) {
		var attributes = this.readAttributes(result[1]);
		RegExp.lastIndex = result["index"];  // Reset position so it will find the same tag when replacing

		var tstr = result[2];
        if (tstr == "<br />") tstr = "&nbsp;<br />";
        if (tstr == "") tstr = "&nbsp;";

        if(attributes && attributes["wikieditorlisttype"] && attributes["wikieditorlistdepth"]) { // Must have at least 2 wikieditor attributes + the string

			//tstr = this.convertBlockInternal(tstr); // Read line and convert

			// TODO: class will differentiate between list types in conjuction with the list type: ul, ol
			switch(attributes["wikieditorlisttype"]) {
				case 'ul':
					// Normal list
					str += this.buildString("*", parseInt(attributes["wikieditorlistdepth"], 10)) + " " + tstr + "\r\n";
                    str.replace(/<div>([\s\S]+?)<\/div>/g,'$1');
					break;
				case 'ol':
					// Numeric list
					str += "# " + tstr + "\r\n";
					break;
			}
		} else {
			// This construct is not valid, get rid of it.
		}
	}

	return str;
}
