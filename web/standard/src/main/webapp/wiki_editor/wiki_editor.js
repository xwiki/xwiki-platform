
/*
                WikiEditor
		   
	     XWiki WYSIWYG Syntax Editor
	Created by Pedro Ornelas for XWiki.org 
	under the Google Summer of Code 2005 program.
*/

function WikiEditor() { 
	this._instance = null;
	
	this._wsFilters = new Array();
	this._wsReplace = new Array();
	
	this._htmlFilters = new Array();
	this._htmlReplace = new Array();
	
	this._toolbarGenerators = new Array();
	this._toolbarHandlers = new Array();
	
	this._fixCommands = new Array();
	
	this._commands = new Array();
	
	// Get script base path
	var elements = document.getElementsByTagName('script');

	for (var i=0; i<elements.length; i++) {
		if (elements[i].src && (elements[i].src.indexOf("wiki_editor.js") != -1)) {
			var src = elements[i].src;

			this.srcMode = (src.indexOf('_src') != -1) ? '_src' : '';
			src = src.substring(0, src.lastIndexOf('/'));

			this.baseURL = src;
			break;
		}
	}
	this.scriptsBaseURL = this.baseURL.substring(0, this.baseURL.lastIndexOf("/"));
	//document.write('<script language="javascript" src="' + this.scriptsBaseURL + '/tiny_mce/tiny_mce.js" type="text/javascript"><\/script>');
}

WikiEditor.prototype.init = function(params) {
	//var t=this.trimString("\r\n");
	//alert("trim(" +t.length + "): " + t);
	
	// Initialize the core editor
	this._imagePath = "";
	this._commandIntercept = false;
	this._interceptedCommand = "";
	this._interceptedNode = null;
	this._interceptedEditor = "";
	this._htmlTagRemover = "__removeHtmlTags";
	this.core = tinyMCE;
	this._theme = 'default';
	this._loadedPlugins = new Array();
    this._useStyleToolbar = false;

    // Add the necessary plugin
	if(params["plugins"] == null) {
		params["plugins"] = "";
	}
	if(params["plugins"].indexOf("wikieditor") == -1) {
		params["plugins"] += ",wikiplugin";
	}
	// Use the wikieditor theme
	params["theme"] = "wikieditor";
	params["extended_valid_elements"] = "li[class|dir<ltr?rtl|id|lang|onclick|ondblclick|onkeydown|onkeypress|onkeyup"
  									+"|onmousedown|onmousemove|onmouseout|onmouseover|onmouseup|style|title|type"
									+"|value|wikieditorlistdepth|wikieditorlisttype]";
	params["relative_urls"] = true;
    params["remove_linebreaks"] = false;

    if(params["use_linkeditor_tabs"] == null) {
        params["use_linkeditor_tabs"] = "wiki_tab, web_tab, attachments_tab, email_tab, file_tab";
    }
    // if don't have this param then it's always default by wiki_tab

    this.setImagePath((params["wiki_images_path"] == null) ? "" : params["wiki_images_path"]);
	
	if(params["wiki_theme"] && params["wiki_theme"] != "") {
		this._theme = params["wiki_theme"];
	}

    if (params["wiki_use_style"] == 'true') {
        this._useStyleToolbar = params["wiki_use_style"];
    }

    this.core.init(params);

    // Load theme
	this.core.loadScript(this.baseURL + '/themes/' + this._theme + '.js');
	// Load plugins
	if(params["wiki_plugins"] && params["wiki_plugins"] != "") {
		var plugs = params["wiki_plugins"].split(/\s*,\s*/i);
		for(var i=0; i < plugs.length;i++) {
			this.core.loadScript(this.baseURL + '/plugins/' + plugs[i] + '.js');
			this._loadedPlugins.push(plugs[i]);
		}
	}

}

<!-- External Javascript functions -->
WikiEditor.prototype.getContent = function() {
	return this.core.getContent();
}

WikiEditor.prototype.setContent = function(html) {
	this.core.setContent(html);
}

WikiEditor.prototype.triggerSave = function(skip_cleanup, skip_callback) {
	this.core.triggerSave(skip_cleanup, skip_callback);
}

WikiEditor.prototype.updateContent = function(form_element_name) {
	this.core.updateContent(form_element_name);
}

<!-- Plugin & Theme related functions -->

WikiEditor.prototype.isPluginLoaded = function(name) {
	for(var i=0; i < this._loadedPlugins.length; i++) {
		if(this._loadedPlugins[i] == name)
			return true;
	}
	return false;
}

WikiEditor.prototype.setHtmlTagRemover = function(func) {
	this._htmlTagRemover = func;
}

WikiEditor.prototype.addExternalProcessor = function(filter, repl) {
	this._wsFilters.push(filter);
	this._wsReplace.push(repl);
}

WikiEditor.prototype.addExternalProcessorBefore = function(v, filter, repl) {
	this._insertBefore(this._wsFilters, this._wsReplace, v, filter, repl);
}

WikiEditor.prototype.addInternalProcessor = function(filter, repl) {
	this._htmlFilters.push(filter);
	this._htmlReplace.push(repl);
}

WikiEditor.prototype.addInternalProcessorBefore = function(v, filter, repl) {
	this._insertBefore(this._htmlFilters, this._htmlReplace, v, filter, repl);
}

WikiEditor.prototype._insertBefore = function(arr1, arr2, v, v1, v2) {
	var index=0;
	for(var i=0; i < arr2.length; i++) {
		if(arr2[i] == v) {
			index = i;
			break;
		}
	}
	for(var i=arr2.length; i > index;i--) {
		arr2[i] = arr2[i-1];
		arr1[i] = arr1[i-1];
	}
	arr1[index]=v1;
	arr2[index]=v2;
}

WikiEditor.prototype.addFixCommand = function(name, func) {
	this._fixCommands[name] = func;
}

WikiEditor.prototype.addCommand = function(name, func) {
	this._commands[name] = func;
}

WikiEditor.prototype.addToolbarGenerator = function(c) {
	this._toolbarGenerators.push(c);
}

WikiEditor.prototype.addToolbarHandler = function(c) {
	this._toolbarHandlers.push(c);
}

WikiEditor.prototype.setImagePath = function(path) {
	this._imagePath = path;
}

WikiEditor.prototype.getImagePath = function() {
	return this._imagePath;
}

<!-- Run-Time HTML Editor Management -->

WikiEditor.prototype.dummyCommand = function(editor_id, element, command, user_interface, value) {
	this.core.triggerNodeChange();
	return true;
}

WikiEditor.prototype.execCommand = function(editor_id, element, command, user_interface, value) {
	if(this._fixCommands[command] != null) {
		this._commandIntercept = true;
		this._interceptedCommand = command;
		this._interceptedEditor = editor_id;
	}
	
	if(this._commands[command] && this[this._commands[command]]) {
		return this[this._commands[command]](editor_id, element, command, user_interface, value);
	}
	
	return false;
}

// Called after a command has been executed from a button
WikiEditor.prototype.executedCommand = function(command) {
	if(command == this._interceptedCommand && this._interceptedNode) {
		this[this._fixCommands[command]](this._interceptedEditor, this._interceptedNode);
		this._interceptedCommand = "";
		this._interceptedNode = null;
	}
}

/*
	Will spread the nodeChange event to all the registered plugins
*/
WikiEditor.prototype.handleNodeChange = function(editor_id, node, undo_index, undo_levels, visual_aid, any_selection) 
{
	this._cleanNode(editor_id, node);
	
	if(this._commandIntercept) {
		this._commandIntercept = false;
		this._interceptedNode = node;
	}
	
	for(var i=0; i < this._toolbarHandlers.length; i++) {
		if(this[this._toolbarHandlers[i]]) {
			this[this._toolbarHandlers[i]](editor_id, node, undo_index, undo_levels, visual_aid, any_selection);
		}
	}
}

WikiEditor.prototype.getControlHTML = function(button_name) {
	var str = "";
	for(var i=0; i < this._toolbarGenerators.length; i++) {
		if( this[this._toolbarGenerators[i]] && (str = this[this._toolbarGenerators[i]](button_name)) != "" ) {
			break;
		}
	}
	return str;
}

<!-- External Conversion: from wiki syntax to internal html representation -->

WikiEditor.prototype.convertExternal = function(content) {
	var regexp, r;
	var lines;
	var lastIndex;
    content = content.replace(/\$(\d)/g, "&#036;$1");
    for(var i=0; i < this._wsFilters.length; i++) {
		RegExp.lastIndex = 0;
		lastIndex = -1;
		regexp = this._wsFilters[i];
		while((r = regexp.exec(content)) ) {
			if(r["index"] <= lastIndex) {
				break;
			}
			RegExp.lastIndex = lastIndex = r["index"];
			var repl = this._wsReplace[i];
			if(this[repl]) {
				// Regular expression replacement
				//alert("found2: "+r[0]);
				content = this[repl](regexp, r, content);
				//alert("changed2: "+content);
			} else {
				//alert("found2_: "+r[0]);
				// Specialized function
				content = content.replace(regexp, repl);
				//alert("changed2_: "+content);
			}
		}
	}
    content = unescape(content);
    content = content.replace(/\\<(.*?)\\>/g, '\\&lt;$1\\&gt;');
//	alert(content);
	return content;
}

<!-- Internal Conversion: from internal html representation to wiki syntax -->

/*
	This function is responsible for parsing an html content
	and returning a Wiki Syntax equivalent.
*/
WikiEditor.prototype.convertInternal = function(content) {
	var regexp, r;
	var lines;
	var lastIndex;
    content = content.replace(/\$(\d)/g, "&#036;$1");
    for(var i=0; i < this._htmlFilters.length; i++) {
		RegExp.lastIndex = 0;
		lastIndex = -1;
		regexp = this._htmlFilters[i];
		while((r = regexp.exec(content)) ) {
			if(r["index"] <= lastIndex) {
				//alert("break at " + r["index"] + " " + lastIndex);
				break;
			}
			RegExp.lastIndex = lastIndex = r["index"];

			var repl = this._htmlReplace[i];
			if(this[repl]) {
				// Regular expression replacement
				//alert("found2: "+r[0]);
				content = this[repl](regexp, r, content);
				//alert("changed2: "+content);
			} else {
				//alert("found2_: "+r[0]);
				// Specialized function
				content = content.replace(regexp, repl);
				//alert("changed2_: "+content);
			}
		}
	}
	content = this.trimString(this._removeHtmlTags(content));
    content = unescape(content);
    content = content.replace(/\&#036;/g, "$");
    content = content.replace(/\\<((\/)*blockquote)\\>/g, '<$1>');
    content = content.replace(/<blockquote>((<(\/)?blockquote>)|(\s*))*<\/blockquote>/g, '');
    content = content.replace(/[\r\n]{4,}/g, "\r\n\r\n");
    if (content.substring(content.length - 2) == "\\\\") {
        content = content.substring(0, content.lastIndexOf("\\\\"));
    }
    return content;
}

/*
	Will add auxiliary attributes the corresponding
	DOM tree for future processing.

	Note: the list are represented with <ol> <ul> tags
*/
WikiEditor.prototype.tagListInternal = function(content) {
	// Find all the list elements
	for(var i=0; content.childNodes[i];i++) {
		if(content.childNodes[i].nodeType == 1) {
			switch(content.childNodes[i].nodeName.toLowerCase()) {
				case 'ul':
				case 'ol':
					this._tagListInternal(content.childNodes[i], 1);
					break;
				default:
					this.tagListInternal(content.childNodes[i]);
			}
		}
	}
	return content;
}

/*
	Auxiliary function which will add auxiliary attributes the corresponding
	DOM tree for future processing.
*/
WikiEditor.prototype._tagListInternal = function(content, depth) {
	var str = "";
	
	for(var i=0; content.childNodes[i];i++) {
		if(content.childNodes[i].nodeType == 1) {
			switch(content.childNodes[i].nodeName.toLowerCase()) {
				case 'ul':
				case 'ol':
					str += this._tagListInternal(content.childNodes[i], depth+1);
					break;
				case 'li':
					// Add extra attributes to the li tag for later processing
					content.childNodes[i].setAttribute("wikieditorlisttype", content.nodeName.toLowerCase());
					content.childNodes[i].setAttribute("wikieditorlistdepth", depth);
					
					break;
			}
		}
	}
	
	return str;
}

WikiEditor.prototype.replaceMatchingTag = function(content, tag, newContent) {
	var btag_regexp = new RegExp("<" + tag + "[^>]*>", "gi"); // begin tag
	var etag_regexp = new RegExp("<\/\s*" + tag + "[^>]*>", "gi");  // end tag
	var btag_result = btag_regexp.exec(content);
	var etag_result = etag_regexp.exec(content);
	var bindex = this._getResultIndex(btag_result);
	var eindex = this._getResultIndex(etag_result);
	var tindex = bindex;
	var result = new Array();
	
	if(bindex > -1 && eindex > -1 && bindex < eindex) {
		do {
			// Find next beggining tag
			RegExp.lastIndex = tindex + btag_result[0].length;
			btag_result = btag_regexp.exec(content);
			tindex = this._getResultIndex(btag_result);
			
			if(tindex == -1) { // No more opening tags
				break;
			} else if (tindex < eindex){ // There's a nested tag
				RegExp.lastIndex = eindex + etag_result[0].length;
				etag_result = etag_regexp.exec(content);
				eindex = this._getResultIndex(etag_result);
			} else { // Next opening tag outside current opening tag context
				break;
			}
		} while(true);
		
		// Replace tag
		result["start"] = bindex;
		result["end"] = eindex + etag_result[0].length;
		if(typeof(newContent) == "undefined") {
			result["string"] = content;
		}else{
			result["string"] = content.substring(0, bindex) + newContent + content.substring(eindex + etag_result[0].length, content.length);
		}
	} else {
		// do nothing, no tag found or there is a remaining tag opened
		result["start"] = result["end"] = -1;
		result["string"] = content;
	}
	return result;
}

WikiEditor.prototype._getResultIndex = function(result) {
	return (result==null)?-1:result["index"];
}

WikiEditor.prototype.readAttributes = function(str) {
	var attrib_regexp = /\s*\w+\s*=\s*"[^"]*"\s*/gi;
	var attrib_local_regexp = /\s*(\w+)\s*=\s*"([^"]*)"\s*/i;
    var result = str.match(attrib_regexp);
    var attributes = new Array();
	var n=0;
    if (result != null)
        for(var i=0; i < result.length; i++) {
            var attrib = attrib_local_regexp.exec(result[i]);
            n++;
            attributes[attrib[1]] = attrib[2];
        }

    return (n>0)?attributes:null;
}

/*
	Removes all white space from the beggining and end of a string.
*/
WikiEditor.prototype.trimString = function(str) {
	var re = /(\S+(\s+\S+)*)+/i;
	var r = re.exec(str);
	return (r && r[1])?r[1]:"";
}

/*
	Returns the string added n times
*/
WikiEditor.prototype.buildString = function(str, n) {
	var aux="";
	for(var i=0; i < n;i++) {
		aux += str;
	}
	return aux;
}

WikiEditor.prototype._removeHtmlTags = function(str) {
	return this[this._htmlTagRemover](str);
}

WikiEditor.prototype.__removeHtmlTags = function(str) {
	var remove_html_tags_regexp = /<[^>]*>/g;
	return str.replace(remove_html_tags_regexp, "");
}

WikiEditor.prototype._escapeText = function(str){
    var newstr='';
    var t;
    var chr = "";
    var cc = "";
    var tn = "";
    for(var i=0; i<256; i++){
        tn = i.toString(16);
        if(tn.length < 2) tn = "0" + tn;
        cc += tn;
        chr += unescape("%" + tn);
    }
    cc = cc.toUpperCase();
    str.replace(String.fromCharCode(13) + "", "%13");
    for(var q=0; q < str.length; q++){
        t = str.substr(q, 1);
        for(var i=0; i<chr.length; i++){
            if(t == chr.substr(i, 1)){
                t = t.replace(chr.substr(i, 1), "%" + cc.substr(i*2, 2));
                i=chr.length;
            }
        }
        newstr += t;
    }
    return newstr;
}
<!-- Initilization global variable and include core editor -->
wikiEditor = new WikiEditor();
