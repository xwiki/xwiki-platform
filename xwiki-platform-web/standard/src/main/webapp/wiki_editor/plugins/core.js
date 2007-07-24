/*
            Syntax Core Plugin

	     XWiki WYSIWYG Syntax Editor
	Created by Pedro Ornelas for XWiki.org
	under the Google Summer of Code 2005 program.
*/

WikiEditor.prototype.initCorePlugin = function() {

	// External/Internal conversion setup
    this.addExternalProcessor((/\{code(.*?)\}([\s\S]+?)\{code\}/i),"convertCodeMacroExternal");
    this.addInternalProcessor(/<div\s*([^>]*)(class=\"code\")\s*([^>]*)>\s*<pre>([\s\S]+?)<\/pre>\s*<\/div>/i, 'convertCodeMacroInternal');

    this.addExternalProcessor((/^\s*(1(\.1)*)\s+([^\r\n]*)$/im), 'convertHeadingExternal');
	this.addInternalProcessor((/\s*<h[1-7]\s*(([^>]*)class=\"heading([^>]*))>([\s\S]+?)<\/h[1-7]>/i), 'convertHeadingInternal');

    this.addInternalProcessor((/<p[^>]*>&nbsp;<\/p>/gi), "");

    this.addExternalProcessor((/\\\\([\r\n]+)/gi), '<br />');

    this.addExternalProcessor((/----(-*)/i), 'convertHRExternal');
	this.addInternalProcessor((/<hr(.*?)>/i), 'convertHRInternal');

    this.addExternalProcessor((/^\s*(\*+)\s+([^\r\n]+)$/im), 'convertListExternal');
    this.addExternalProcessor((/^\s*(#+)\s+([^\r\n]+)$/im), 'convertListExternal');
    this.addExternalProcessor((/^\s*(1+)\.\s+([^\r\n]+)$/im), 'convertListExternal');
    this.addExternalProcessor((/^\s*(\-+)\s+([^\r\n]+)$/im), 'convertListExternal');

    this.addInternalProcessor((/\s*<(ul|ol)\s*([^>]*)>/i), 'convertListInternal');

    // Must remove the html tag format so it won't interfere with paragraph conversion
	this.addExternalProcessor((/<%([\s\S]+?)%>/ig), '&lt;%$1%&gt;');

    this.addExternalProcessor((/((\s|\S)*)/i), 'convertParagraphExternal');
	this.addInternalProcessor((/<p(.*?)>([\s\S]+?)<\/p>/i), 'convertParagraphInternal');

    this.addInternalProcessor((/(<br\s*\/>|<br\s*>)(\s*\r*\n*)/gi), '\\\\\r\n');

    this.addExternalProcessor((/\[(.*?)((>|\|)(.*?))?((>|\|)(.*?))?\]/i), 'convertLinkExternal');
    this.addInternalProcessor((/<a\s*([^>]*)>(.*?)<\/a>/i), 'convertLinkInternal');

    this.addExternalProcessor((/\{table\}([\s\S]+?)\{table\}/i), 'convertTableExternal');
    this.addInternalProcessor((/<table\s*([^>]*)class=\"wiki-table\"\s*([^>]*)>([\s\S]+?)<\/table>/i), 'convertTableInternal');

    this.addExternalProcessor((/\*(\s*)(.+?)(\s*)\*/gi), '$1<b class="bold">$2<\/b>$3');
    this.addInternalProcessor((/<strong[^>]*>(\s*)(.*?)(\s*)<\/strong>/i), 'convertBoldTextInternal');

	this.addExternalProcessor((/~~(\s*)(.+?)(\s*)~~/gi), '$1<i class="italic">$2<\/i>$3');
	this.addInternalProcessor((/<em[^>]*>(\s*)(.*?)(\s*)<\/em>/i), 'convertItalicTextInternal');

    this.addExternalProcessor((/__(\s*)(.+?)(\s*)__/gi), '$1<u>$2<\/u>$3');
    this.addInternalProcessor((/<u[^>]*>(\s*)(.*?)(\s*)<\/u>/i), 'convertUnderLineTextInternal');

    this.addExternalProcessor((/--(\s*)(.+?(\s*))--/gi),  '$1<strike class="strike">$2<\/strike>$3');
	this.addInternalProcessor((/<strike[^>]*>(\s*)(.*?)(\s*)<\/strike>/i), 'convertStrikeTextInternal');

	this.addInternalProcessor((/[#$][a-zA-Z0-9-_.]+\(([^&)]*&quot;[^)]*)+?\)/i), 'convertVelocityScriptsInternal');

    this.addInternalProcessor((/&lt;%([\s\S]+?)%&gt;/i), 'convertGroovyScriptsInternal');

    this.addExternalProcessorBefore('convertParagraphExternal', (/##([^\r\n]*)$|(#\*([\s\S]+?)\*#)/im), 'convertVelocityCommentExternal');
    this.addInternalProcessorBefore('convertStyleInternal', (/<span\s*([^>]*)class=\"vcomment\"\s*([^>]*)>([\s\S]+?)(\r?\n?)<\/span>/i), 'convertVelocityCommentInternal');

    this.addExternalProcessorBefore('convertTableExternal', (/\{style:\s*(.*?)\}([\s\S]+?)\{style\}/i), 'convertStyleExternal');
    this.addInternalProcessorBefore('convertTableInternal', (/<(font|span|div)\s*(.*?)>([\s\S]+?)<\/(font|span|div)>/i), 'convertStyleInternal');

    //this.addInternalProcessor((/&nbsp;(?!\|)/gi), "");
    this.setHtmlTagRemover('removeHtmlTags_Groovy');
    this.setHtmlTagRemover('removeSpecialHtmlTags');

    // Toolbar handlers
	this.addToolbarHandler('handleTextButtons');
	this.addToolbarHandler('handleListButtons');
    this.addToolbarHandler('handleIndentButtons');
    this.addToolbarHandler('handleUndoButtons');
	this.addToolbarHandler('handleTitlesList');
    this.addToolbarHandler('handleStylesList');
	this.addToolbarHandler('handleLinkButtons');
    this.addToolbarHandler('handleHorizontalRuleButtons');
    this.addToolbarHandler('handleSupAndSubButons');
    this.addToolbarHandler('handleTableButtons');
    this.addToolbarHandler('handleAlignButtons');

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
    str = str.replace(/<span class="(wikilink|wikiexternallink)">\s*([\s\S]+?)<\/span>/g,'$2');
    str = str.replace(/<span class="(bold|italic|underline|strike)">([\s\S]+?)<\/span>/g,'$2');
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

WikiEditor.prototype.convertBoldTextInternal = function(regexp, result, content) {
    var str = result[1];
    if  (result[2] != "") {
        str += "*" + result[2] + "*";
    }
    str += result[3];
    return content.replace(regexp, str);
}

WikiEditor.prototype.convertItalicTextInternal = function(regexp, result, content) {
    var str = result[1];
    if  (result[2] != "") {
        str += "~~" + result[2] + "~~";
    }
    str += result[3];
    return content.replace(regexp, str);
}

WikiEditor.prototype.convertUnderLineTextInternal = function(regexp, result, content) {
    var str = result[1];
    if  (result[2] != "") {
        str += "__" + result[2] + "__";
    }
    str += result[3];
    return content.replace(regexp, str);
}

WikiEditor.prototype.convertStrikeTextInternal = function(regexp, result, content) {
    var str = result[1];
    if  (result[2] != "") {
        str += "--" + result[2] + "--";
    }
    str += result[3];
    return content.replace(regexp, str);
}

WikiEditor.prototype.convertLinkInternal = function(regexp, result, content) {
    var txt;
    var str="";
    var href;
    var target;
    var separator = ">";
    if( (txt = this.trimString(result[2])) != "") {
        var att = this.readAttributes(result[1]);
        if(att && att["id"]) {
            separator = "|";
        }
        if(att && att["href"]) {
            href = this.trimString(att["href"]);
            if (att["title"] && att["title"] != "") {
                href = this.trimString(att["title"]);
            }
            href = unescape(href);
            if ((href.toLowerCase() == txt.toLowerCase()) && (!att["target"] || (att["target"] == "_self"))) {
                str = "[" + txt + "]";
            } else if(att["target"] && att["target"] != "_self") {
                target = this.trimString(att["target"]);
                str = "[" + txt + separator + href + separator + target + "]";
            } else
                str = "[" + txt + separator + href + "]";
        }

        } else {
        str = result[2];
    }
    return content.replace(regexp, str);
}

WikiEditor.prototype.convertTableInternal = function(regexp, result, content) {
    var text = this.trimRNString(result[3]);
    var str = "";
    if (tinyMCE.isMSIE) str += "\r\n";
    str += "{table}";
    var rows = text.split("<\/tr>");
    for(var i=0; i<(rows.length - 1); i++) {
        var trow = "";
        if (i == 0) trow = rows[0].replace(/(.*?)<tr(.*?)>/g, "");
        else trow = rows[i].replace(/<tr(.*?)>/g, "")
        var cols = trow.split("<\/td>");
        for (var j=0; j<(cols.length-1); j++) {
            var cell = this.trimRNString(cols[j].replace(/<td(.*?)>/g, ""));
            if ((cell.lastIndexOf("\\\\") > 1) && (cell.lastIndexOf("\\\\") == (cell.length-2))) {
                cell = cell.substring(0, cell.lastIndexOf("\\\\"));
            }
            cell = cell.replace(/[\r\n]{3,}/g, "\\\\\r\n\\\\\r\n");
            if (cell == "") cell = "&nbsp;"
            if (j == 0) {
                str += "\r\n" + cell;
            } else {
                str += "|" + cell;
            }
        }
    }
    str += "\r\n{table}";
    if (tinyMCE.isMSIE) str += "\r\n";
    return content.replace(regexp, str);
}

WikiEditor.prototype.convertHeadingInternal = function(regexp, result, content) {
	var str = "";
	var txt;
	var headr = /heading((-1)+)/i;
	if( (txt = this.trimString(result[4])) != "") {
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
    var value = "";
    if (this._titleChangeValue==0) {
        this.core.execInstanceCommand(editor_id, "mceRemoveNode", false, node);
    } if (this._titleChangeValue==6) {
        this.core.execInstanceCommand(editor_id, "FormatBlock", false, "<div>");
        var selectedBlock = tinyMCE.selectedInstance.selection.getFocusElement();
        selectedBlock.setAttribute("class", "code");
        var b = tinyMCE.selectedInstance.selection.getBookmark();
        var preNode = tinyMCE.selectedInstance.getDoc().createElement("<pre>");
        var childsBlock = selectedBlock.childNodes;

        for (var i=0; i<childsBlock.length; i++){
			preNode.appendChild(childsBlock[i].cloneNode(true));
            selectedBlock.removeChild(childsBlock[i], true);
        }

        selectedBlock.appendChild(preNode);
        tinyMCE.selectedInstance.selection.moveToBookmark(b);
    } else {
        value = "<h" + this._titleChangeValue + ">";
        this.core.execInstanceCommand(editor_id, "FormatBlock", false, value);
        var childs = tinyMCE.selectedInstance.contentDocument.body.childNodes;
        if (childs) {
         for( var x = 0; childs[x]; x++ ) {
            var child = childs[x];
            var nn = child.nodeName;
            if ((nn.length == 2) && (nn.charAt(0) == "H")) {
                 var level = parseInt(nn.charAt(1));
                 var cn = "heading" + this.buildString("-1", level);
                 child.className = cn;
            }
         }
        }
    }
    tinyMCE.triggerNodeChange();
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
                // alert(node.className);
                node.className = "";
                //this._fixParagraph(node);
                break;
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
		node.parentNode.replaceChild(document.createTextNode("\\\\"), node);
		//node.parentNode.removeChild(node);
		return;
	}

	for(var i=0; node.childNodes[i]; i++) {
		this._cleanBR(node.childNodes[i]);
	}
}

WikiEditor.prototype.handleSupAndSubButons = function(editor_id, node, undo_index, undo_levels, visual_aid, any_selection) {
    tinyMCE.switchClass(editor_id + '_sup', 'mceButtonNormal');
    tinyMCE.switchClass(editor_id + '_sub', 'mceButtonNormal');
    switch (node.nodeName.toLowerCase()) {
        case "sup":
            tinyMCE.switchClass(editor_id + '_sup', 'mceButtonSelected');
            break;
        case "sub":
            tinyMCE.switchClass(editor_id + '_sub', 'mceButtonSelected');
            break;
    }
}

WikiEditor.prototype.handleHorizontalRuleButtons = function(editor_id, node, undo_index, undo_levels, visual_aid, any_selection) {
   tinyMCE.switchClass(editor_id + '_hr', 'mceButtonNormal');
   if (node.nodeName == "HR") {
       tinyMCE.switchClass(editor_id + '_hr', 'mceButtonSelected');
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

WikiEditor.prototype.handleAlignButtons = function(editor_id, node, undo_index, undo_levels, visual_aid, any_selection) {
    tinyMCE.switchClass(editor_id + '_justifyleft', 'mceButtonNormal');
    tinyMCE.switchClass(editor_id + '_justifyright', 'mceButtonNormal');
    tinyMCE.switchClass(editor_id + '_justifycenter', 'mceButtonNormal');
    tinyMCE.switchClass(editor_id + '_justifyfull', 'mceButtonNormal');

    var alignNode = node;
	var	breakOut = false;
    do {
        if (!alignNode.getAttribute || !alignNode.getAttribute('align'))
            continue;

        switch (alignNode.getAttribute('align').toLowerCase()) {
            case "left":
                tinyMCE.switchClass(editor_id + '_justifyleft', 'mceButtonSelected');
                breakOut = true;
            break;

            case "right":
                tinyMCE.switchClass(editor_id + '_justifyright', 'mceButtonSelected');
                breakOut = true;
            break;

            case "middle":
            case "center":
                tinyMCE.switchClass(editor_id + '_justifycenter', 'mceButtonSelected');
                breakOut = true;
            break;

            case "justify":
                tinyMCE.switchClass(editor_id + '_justifyfull', 'mceButtonSelected');
                breakOut = true;
            break;
        }
    } while (!breakOut && (alignNode = alignNode.parentNode) != null);

    var div = tinyMCE.getParentElement(node, "div");
	if (div && div.style.textAlign == "center")
	    tinyMCE.switchClass(editor_id + '_justifycenter', 'mceButtonSelected');
}

WikiEditor.prototype.handleTitlesList = function(editor_id, node, undo_index, undo_levels, visual_aid, any_selection) {
	var list = document.getElementById(editor_id + "_titleSelect");
	if(list) {
		var h3 = this.core.getParentElement(node, "h1,h2,h3,h4,h5,h6,");
		if(h3) {
			var classname = h3.className;
			var n = (classname.split("-").length)-1;
			this._selectByValue(list, n);
		} else {
			this._selectByValue(list, 0);
		}
        var code = this.core.getParentElement(node, "div");
        if (code && (code.className == 'code')) {
            this._selectByValue(list, 6);
        }
    }
}

WikiEditor.prototype.handleStylesList = function(editor_id, node, undo_index, undo_levels, visual_aid, any_selection) {
    // Select font size
    var selectElm = document.getElementById(editor_id + "_fontSizeSelect");
    if (selectElm) {
        var elm = tinyMCE.getParentElement(node);
        if (elm) {
            var size = tinyMCE.getAttrib(elm, "size");
            if (size == '') {
                var sizes = new Array('', '8px', '10px', '12px', '14px', '18px', '24px', '36px');
                size = '' + elm.style.fontSize;
                for (var i=0; i<sizes.length; i++) {
                    if (('' + sizes[i]) == size) {
                        size = i;
                        break;
                    }
                }
            }
            if (!this._selectByValue(selectElm, size))
                this._selectByValue(selectElm, "");
        } else
            this._selectByValue(selectElm, "0");
    }
    // Select font name
    selectElm = document.getElementById(editor_id + "_fontNameSelect");
    if (selectElm) {
        var elm = tinyMCE.getParentElement(node);
        if (elm) {
            var family = tinyMCE.getAttrib(elm, "face");
            if (family == '')
                family = '' + elm.style.fontFamily;
            if (!this._selectByValue(selectElm, family))
                this._selectByValue(selectElm, "");
        } else
            this._selectByValue(selectElm, "");
    }
}

WikiEditor.prototype.handleIndentButtons = function(editor_id, node, undo_index, undo_levels, visual_aid, any_selection) {
    // indent, outdent for all element
    tinyMCE.switchClass(editor_id + '_outdent', 'mceButtonDisabled', true);
	tinyMCE.switchClass(editor_id + '_indent', 'mceButtonNormal');

    var indent = this.core.getParentElement(node, "blockquote");
    var ul = this.core.getParentElement(node, "ul");
    var ol = this.core.getParentElement(node, "ol");
    if (indent || ul || ol) {
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
    this.core.switchClass(editor_id + '_underline', 'mceButtonNormal');
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

            case "u":
				this.core.switchClass(editor_id + '_underline', 'mceButtonSelected');
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

WikiEditor.prototype.getSymbolToolbar = function() {
    return this.getSymbolControls("charmap");
}

WikiEditor.prototype.getSymbolControls = function(button_name) {
    var str = "";
    switch (button_name) {
        case 'charmap':
            str = this.createButtonHTML('charmap', 'charmap.gif', 'lang_theme_charmap_desc', 'mceCharMap');
            break;
    }
    return str;
}

WikiEditor.prototype.getJustifyToolbar = function() {
    return this.getJustifyControls('justifyleft') + this.getJustifyControls('justifycenter') + this.getJustifyControls('justifyright') + this.getJustifyControls('justifyfull');
}

WikiEditor.prototype.getJustifyControls = function(button_name) {
    var str = "";
    switch (button_name) {
        case 'justifyleft':
            str = this.createButtonHTML('justifyleft', 'justifyleft.gif', 'lang_justifyleft_desc', 'JustifyLeft');
            break;
        case 'justifycenter':
            str = this.createButtonHTML('justifycenter', 'justifycenter.gif', 'lang_justifycenter_desc', 'JustifyCenter');
            break;
        case 'justifyright' :
            str = this.createButtonHTML('justifyright', 'justifyright.gif', 'lang_justifyright_desc', 'JustifyRight');
            break;
        case 'justifyfull' :
            str = this.createButtonHTML('justifyfull', 'justifyfull.gif', 'lang_justifyfull_desc', 'JustifyFull');
            break;
    }
    return str;
}

WikiEditor.prototype.getSupAndSubToolbar = function() {
    return this.getSupAndSubControls('sup') + this.getSupAndSubControls('sub');
}

WikiEditor.prototype.getSupAndSubControls = function(button_name) {
    var str = "";
    switch(button_name) {
        case 'sup':
            str = this.createButtonHTML('sup', 'sup.gif', 'lang_theme_sup_desc', 'superscript');
            break;
        case 'sub':
            str = this.createButtonHTML('sub', 'sub.gif', 'lang_theme_sub_desc', 'subscript');
            break;
    }
    return str;
}

WikiEditor.prototype.getTabToolbar = function() {
	return this.getTabControls('outdent') + this.getTabControls('indent');
}

WikiEditor.prototype.getTabControls = function(button_name) {
	var str = "";
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

WikiEditor.prototype.getHorizontalruleControls = function() {
    var str = this.createButtonHTML('hr', 'hr.gif', 'lang_theme_hr_desc', 'inserthorizontalrule');    
    return str;
}

WikiEditor.prototype.getRemoveformatControls = function() {
    var str = this.createButtonHTML('removeformat', 'removeformat.gif', 'lang_theme_removeformat_desc', 'removeformat');
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
            '<option value="6">{$lang_wiki_title_6}</option>' +
           '</select>';
}

WikiEditor.prototype.getStyleToolbar = function() {
    return this.getStyleControl("fontselect") + " " + this.getStyleControl("fontSizeSelect") + this.getStyleControl("mceForeColor") + this.getStyleControl("mceBackColor");
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
            str = this.createButtonHTML('numlist', 'numlist.gif', 'lang_numlist_desc', 'InsertOrderedList');
			break;
	}

	return str;
}

WikiEditor.prototype.getTextToolbar = function() {
	return this.getTextControls('bold') + this.getTextControls('italic') + this.getTextControls('underline') + this.getTextControls('strikeout');
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
        case 'underline':
            str = this.createButtonHTML('underline', '{$lang_underline_img}', 'lang_underline_desc', 'Underline');
            break;
        case 'strikeout':
			str = this.createButtonHTML('strikethrough', 'strikethrough.gif', 'lang_striketrough_desc', 'Strikethrough');
			break;
	}
	return str;
}

WikiEditor.prototype.convertParagraphInternal = function(regexp, result, content) {
    var str = this.trimString(result[2]);
    // remove // at the end of a paragraph
    if (str.substring(str.length - 6) == "<br />") {
        str = str.substring(0, str.lastIndexOf("<br />"));
    }
    str = "\r\n" + str + "\r\n";
    return content.replace(regexp, str);
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
		line = lines[i];
		var hh = this._hasHTML(line);
        var hbr = this._onlyHasBr(line);
        if(line != "" && (!hh || hbr)) {
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
    str = str.replace(/<p\s*(.*?)>\s*<\/p>/g,'');
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

WikiEditor.prototype._onlyHasBr = function(str) {
    str = str.replace(/<br \/>/g, " ");
    var reg = /<[^>]+>/i;
    return (reg.exec(str) == null);
}

WikiEditor.prototype.LIST_NORMAL_CLASS_NAME = "star";
WikiEditor.prototype.LIST_MINUS_CLASS_NAME = "minus";
WikiEditor.prototype.LIST_NUMERIC_CLASS_NAME = "";
WikiEditor.prototype.LIST_NUMERIC_CLASS_NAME_1 = "norder";

WikiEditor.prototype.convertListExternal = function(regexp, result, content) {
    var subContent = content.substring(result["index"], content.length);
    //subContent = this._convertNewLine2BrInList(subContent);
    var str = "";
    switch (result[1].charAt(0)) {
		case '*':
			str = this._convertRecursiveListExternal(regexp, subContent, 0 , this.LIST_NORMAL_CLASS_NAME);
			break;
        case '-':
            str = this._convertRecursiveListExternal(regexp, subContent, 0, this.LIST_MINUS_CLASS_NAME);
			break;
        case '#':
            str = this._convertRecursiveListExternal(regexp, subContent, 0, this.LIST_NUMERIC_CLASS_NAME);
			break;
        case '1':
            str = this._convertRecursiveListExternal(regexp, subContent, 0, this.LIST_NUMERIC_CLASS_NAME);
            break;
    }
    return content.substring(0, result["index"]) + "\r\n" + str;
}

WikiEditor.prototype._convertNewLine2BrInList = function(content) {
    var lines = this._getLines(content);
    var tempContent = "";
    for (var i=0; i< lines.length; i++) {
        if ((lines[i].charAt(0) == '*') || (lines[i].charAt(0) == '#') || (lines[i].charAt(0) == '1')) {
            tempContent += lines[i];
            var j = 1;
            if (((i+j) <= lines.length)) {
                while (((i+j) < lines.length) && ((lines[i+j].charAt(0) != '*') && (lines[i+j].charAt(0) != '#') && ((lines[i+j].charAt(0) != '1'))) && this.trimString(lines[i+j]) != "") {
                    tempContent += "\r\n" + lines[i+j];
                    j++;
                }
            }
            tempContent += "\r\n";
            i = (i + j-1);
        } else {
            tempContent += lines[i] + "\r\n";
        }
    }

    return tempContent;
}

WikiEditor.prototype._convertGenericListExternal = function(regexp, content, tagname, classname) {
	var str = "<" + tagname + " class=\"" + classname + "\">\r\n";
	var r;
	var _content = content;
	RegExp.lastIndex = 0;
    while( (r = regexp.exec(_content)) && r["index"] == 0) {
        str += "<li>" + this.trimString(r[2]) + "<\/li>\r\n";
		_content = _content.substring(r[0].length, _content.length);
		RegExp.lastIndex = 0;
	}

	str += "<\/" + tagname + ">\r\n" + _content;

	return str;
}

WikiEditor.prototype._convertRecursiveListExternal = function(regexp, content, depth, classname) {
    var str = "";
    var otag = (classname == this.LIST_NUMERIC_CLASS_NAME) ? "ol" : "ul";
    RegExp.lastIndex = 0;
	var r = regexp.exec(content);
    var currdepth = (r != null && ((r[1].charAt(0) == '*') || (r[1].charAt(0) == '-') || (r[1].charAt(0) == '#') || (r[1].charAt(0) == '1')) && r["index"]==0) ? r[1].length : 0; // number of "*", if no list element found on next line then list section is over
	var lastPos = (currdepth > 0) ? r[0].length : 0;
	var subContent = content.substring(lastPos, content.length);
    var depthdif = currdepth - depth;
	var tag = (depthdif > 0) ? "<" + otag + " class=\"" + classname + "\">" : "<\/" + otag + ">";
	for(var i=0; i < Math.abs(currdepth-depth);i++) {
		str += tag + "\r\n";
	}

	if(currdepth > 0) {
        str += "<li>" + this.trimString(r[2]) + "<\/li>\r\n";
        str += this._convertRecursiveListExternal(regexp, subContent, currdepth, classname);
	} else {
		str += content;
	}
	return str;
}

WikiEditor.prototype.LINK_EXTERNAL_CLASS_NAME = "wikiexternallink";
WikiEditor.prototype.LINK_INTERNAL_CLASS_NAME = "wikilink";

WikiEditor.prototype.convertLinkExternal = function(regexp, result, content) {
	var text = result[1];
    var separator = this.trimString(result[3]);
    var url = (result[4])?(result[4]):(text);
    var target = this.trimString(result[7]);
    var classname;
	var str = "<a class=\"" + classname + "\" href=\"" + url + "\"";
    if(this.isExternalLink(url)) {
		classname = this.LINK_EXTERNAL_CLASS_NAME;
        str += " title=\"" + url + "\"";
    } else {
		classname = this.LINK_INTERNAL_CLASS_NAME;
	}
    if (separator == "|") {
        str += " id=\"" +  url + "\"";
    }
    if ((target != "undefined") && (target != "") && (target != "_self")) {
        str += " target=\"" + result[7] + "\"";
    }
    str += ">" + text + "<\/a>";
	return content.replace(regexp, str);
}

WikiEditor.prototype.convertTableExternal = function(regexp, result, content) {
    var text = this.trimRNString(result[1]);
    var _lines = this._getLines(text);
    var str = "<table class=\"wiki-table\" cellpadding=\"0\" cellspacing=\"0\" align=\"center\">"
    var lines = new Array();
    var numColumns = 0;
    for (var i=0; i < _lines.length; i++) {
        _lines[i] = this.trimRNString(_lines[i]);
        _lines[i] = _lines[i].replace(/<\/?p[^>]*>/gi, "");
        if (_lines[i] != "") {
            lines[numColumns] = _lines[i];
            numColumns++;
        }
    }
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
        if (i==0) {
           str += "<tr class='table-head'>";
        } else if ((i%2) == 1) {
            str += "<tr class='table-odd'>";
        } else {
            str += "<tr class='table-even'>";
        }
        var cols = rows[i].split("|");  // get cols
        for (var j=0; j < cols.length; j++) {
            if ( i== 0) str += "<td>";
            else  str += "<td>";
            var linescol = cols[j].split("\\\\");
            if (linescol.length == 1) str += linescol[0];
            else if (linescol.length > 1)
                for (var k=0; k < linescol.length; k++) {
                    if (linescol[k] == "") linescol[k] = "&nbsp;"  // for empty paragraph
                    str += (linescol[k] + "<br />\r\n");
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
	var str = '\r\n<h' + n + ' class="heading' + this.buildString("-1", n) + '">' + this.trimString(result[3]) + '<\/h' + n + '>';
	return content.substring(0, result["index"]) + str + content.substring(result["index"] + result[0].length, content.length);
}

WikiEditor.prototype.convertListInternal = function(regexp, result, content) {
    var bounds = this.replaceMatchingTag(content, result[1], null);
    var lclass = "";
    var newContent = "";
    var attributes = this.readAttributes(result[2]);
    if (attributes && attributes["class"]) {
        lclass = attributes["class"];
    }
    var str = "";
	if(bounds && bounds["start"] > -1) {
        str = this._convertListInternal(content.substring(bounds["start"], bounds["end"]), lclass);
        newContent = content.substring(0, bounds["start"]) + "\r\n";
        var afterContent = content.substring(bounds["end"], content.length)
        if (!this.core.isMSIE && afterContent.substring(0,6) == "<br />") {
            afterContent = "\r\n" + afterContent.substring(6, afterContent.length);
        }
        newContent += str + "\r\n" + afterContent;
        return newContent;
	}
    return content;
}

/*
	Will return a string containing the list in Wiki Syntax

	TODO: can be optimized with String.match() function
*/
WikiEditor.prototype._convertListInternal = function(content, lclass) {
    var list_regexp = /<\s*li\s*([^>]*)>\s*(.*?)\s*<\/\s*li\s*>/gi;
	var result;
	var str = "";

	while( (result = list_regexp.exec(content)) ) {
        var attributes = this.readAttributes(result[1]);
		RegExp.lastIndex = result["index"];  // Reset position so it will find the same tag when replacing

		var tstr = result[2];

        if (tstr == "<br />") {
            tstr = "&nbsp;";
        } else if (this.trimString(tstr) == "") {
            tstr = "&nbsp;";
        }

        var start = tstr.length - 6;
        if (tstr.substring(start) == "<br />")
          tstr = tstr.substring(0, start);

        if(attributes && attributes["wikieditorlisttype"] && attributes["wikieditorlistdepth"]) { // Must have at least 2 wikieditor attributes + the string

			//tstr = this.convertBlockInternal(tstr); // Read line and convert

			// TODO: class will differentiate between list types in conjuction with the list type: ul, ol
			switch(attributes["wikieditorlisttype"]) {
				case 'ul':
                    if ((lclass != null) && (lclass == "minus")) {
                        str += this.buildString("-", parseInt(attributes["wikieditorlistdepth"], 10)) + " " + tstr + "\r\n";
                    } else {
                        //  Normal list
                        str += this.buildString("*", parseInt(attributes["wikieditorlistdepth"], 10)) + " " + tstr + "\r\n";
                    }
                    str.replace(/<div>([\s\S]+?)<\/div>/g,'$1');
					break;
				case 'ol':
                    str += this.buildString("1", parseInt(attributes["wikieditorlistdepth"], 10)) + ". " + tstr + "\r\n";
                    break;
			}
		} else {
			// This construct is not valid, get rid of it.
		}
	}

	return str;
}

WikiEditor.prototype.convertStyleExternal = function(regexp, result, content) {
    var str = "";
    var atts = result[1].split("|");
    var tag = "font", style = "", myclass = "", id = "", name = "", doc = "", align = "";
    var hasIcon = false;
    for (var i=0; i < atts.length; i++) {
        var att = this.trimString(atts[i].substring(0, atts[i].indexOf("=")));
        var value = this.trimString(atts[i].substring(atts[i].indexOf("=") + 1, atts[i].length));
        if (att == "class") {
            myclass = value;
        }
        else if (att == "id") {
            id = value;
        }
        else if (att == "name") {
            name = value;
        }
        else if (att == "type") {
            tag = value;
        }
        else if (att == "align") {
            align = value;
        }
        else if (att == "icon") {
            style += "background-image: url(" + value + ");";
            hasIcon = true;
        } else {
            style += att + ":" + value + ";";
        }
    }
    str += "<" + tag;
    if (id != "") {
        str += " id=\"" + id + "\"";
    }
    if (myclass != "") {
        str += " class=\"" + myclass + "\"";
    } else if (hasIcon){
        str += " class=\"stylemacro\"";
    }
    if (name != "") {
        str += " name=\"" + name + "\"";
    }
    if (align != "") {
        str += " align=\"" + align + "\"";
    }
    if (style != "") {
        str += " style=\"" + style + "\"";
    }
    str += ">";
    str += result[2];
    str += "</" + tag + ">";
    return content.replace(regexp, str) ;
}


WikiEditor.prototype.convertStyleInternal = function(regexp, result, content) {
    if (this.trimString(result[3]) == "") {
        return content.replace(regexp, result[3]);
    }
    content = content.replace(/<div class="paragraph">([\s\S]+?)<\/div>/g,'$1');
    content = content.replace(/<span class="(wikilink|wikiexternallink)">\s*([\s\S]+?)<\/span>/g,'$2');
    content = content.replace(/<span class="(bold|italic|underline|strike)">([\s\S]+?)<\/span>/g,'$2');
    var type = result[1];
    var str = "";
    if (type == "span" || type =="div") {
        var attributes = this.readAttributes(result[2]);
        if (type == 'div') {
            str += "\r\n";
        }
        str += "{style:type=" + type;

        if (attributes) {
            if (attributes["id"]) {
                str += "|id=" + attributes["id"] ;
            }
            if (attributes["align"]) {
                str += "|align=" + attributes["align"] ;
            }
            if (attributes["class"] && attributes["class"] != "stylemacro") {
                str += "|class=" + attributes["class"] ;
            }
            if (attributes["name"]) {
                str += "|name=" + attributes["name"] ;
            }
            if (attributes["style"]) {
                var atts = attributes["style"].split(";");
                for (var i=0; i < atts.length ; i++) {
                    var att = this.trimString(atts[i].substring(0, atts[i].indexOf(":")));
                    var value = this.trimString(atts[i].substring(atts[i].indexOf(":") + 1 , atts[i].length));
                    var styleAtts = ["font-size", "font-family", "background-color", "color", "width", "height", "float", "border"];
                    for (var j=0 ; j < styleAtts.length; j++) {
                        if (att == styleAtts[j]) {
                            str += "|" + att + "=" + value;
                            break;
                        }
                    }
                    if (att == "background-image") {
                        var iconimage ;
                        if (value.indexOf("url") >= 0) {
                            iconimage = value.substring(value.indexOf("(") + 2, value.indexOf(")")-1);
                            str += "|icon=" + iconimage;
                        }
                    }
                }
            }
        }
        str += "}";
        str += result[3];
        str += "{style}";
        if (type == 'div') {
            str += "\r\n";
        }
    }
    // alert("str = " + str);
    return content.replace(regexp, str);
}

WikiEditor.prototype.VELOCITY_COMMENT_CLASS_NAME = "vcomment";

WikiEditor.prototype.convertVelocityCommentExternal = function(regexp, result, content) {
    var str = "";
    var vcomment = "";
    if ((result[1] != null) && (result[1] != "undefined") && (result[1] != "")) {
        vcomment = result[1];
    } else if ((result[3] != null) && (result[3] != "undefined") && (result[3] != "")) {
        vcomment = result[3];
    }
    str = "<span class='" + this.VELOCITY_COMMENT_CLASS_NAME + "'>" + vcomment + "</span>";
    return content.replace(regexp, str);
}

WikiEditor.prototype.convertVelocityCommentInternal = function(regexp, result, content) {
    var str = "";
    var vcomment = result[3];
    if ((vcomment.indexOf("\n") > -1) || (vcomment.indexOf("<p") > -1)) {
        str = "#*" + vcomment + "*#";
    } else {
        if (this.core.isMSIE) {
            str = "\r\n" + "##" + vcomment + "\r\n";
        } else {
            str = "##" + vcomment;
        }
    }

    if (result[4] != null) {
        str += result[4];
    }
    return content.replace(regexp, str);
}

WikiEditor.prototype.convertCodeMacroExternal = function(regexp, result, content) {
    var str = "";
    var type = "";
    if (result[1] != null && result[1] != "") {
        type = this.trimString(result[1].substring(result[1].indexOf(":") + 1, result[1].length));
    }
    str += "<div";
    if (type != "") str += " id=\"" + type + "\"";    // use id property to store type of code because another atts will be remove in tinymce
    str += " class=\"code\"><pre>";
    str += result[2].toString().replace(/</g, "&#60").replace(/>/g, "&#62");;
    str += "</pre></div>";
    str = this._escapeText(str);
    return content.replace(regexp, str);
}

WikiEditor.prototype.convertCodeMacroInternal = function(regexp, result, content) {
    var str = "";
    var temp = result[4];
    var attributes = this.readAttributes(result[1] + result[3]);
    str += "{code";
    if (attributes && attributes["id"]) str += ":" +  this.trimString(attributes["id"].toString());
    str += "}\r\n";
    str += this._escapeText(this.trimString(result[4]).replace(/<br \/>/g, "\r\n"));
    str += "\r\n";
    str += "{code}";
    str = "\r\n" + str + "\r\n";
    return content.replace(regexp, str);
}

WikiEditor.prototype.convertHRExternal = function(regexp, result, content) {
    var str = "";
    var count = 0;
    if (result[1] && result[1] != "") {
        count = result[1].toString().length;
    }
    if (count > 0) {
        str = "<hr class=\"line\" name=\"" + count + "\"\/>"
    } else {
        str = "<hr class=\"line\"\/>"
    }
    return content.replace(regexp, str);
}

WikiEditor.prototype.convertHRInternal = function(regexp, result, content) {
    var str = "----";
    var atts = this.readAttributes(result[1]);
    if (atts && atts["name"])  {
        str += this.buildString("-", atts["name"]);
    }
    if (this.core.isMSIE) {
        str += "\r\n";
    } else if (result[1] == null || result[1] == "" || this.trimString(result[1].toString()) == "/") {
        str += "\n";
    }
    return content.replace(regexp, str);
}