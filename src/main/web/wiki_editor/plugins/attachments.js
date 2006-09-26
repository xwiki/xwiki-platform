/*
            Syntax Attachments Plugin
		   
	     XWiki WYSIWYG Syntax Editor
	Created by Pedro Ornelas for XWiki.org 
	under the Google Summer of Code 2005 program.
*/

WikiEditor.prototype.initAttachmentsPlugin = function() {
	if(!this.isPluginLoaded('core')) {
		alert("Attachment Plugin: You must load the core syntax plugin before!");
		return;
	}
	
	this.addExternalProcessor((/{\s*image\s*:\s*(.*?)(\|(.*?))?(\|(.*?))?}/i), 'convertImageExternal');
	this.addInternalProcessor((/<img\s*([^>]*)(class=\"wikiimage\")\s*([^>]*)\/>/i), 'convertImageInternal');

	this.addExternalProcessor((/{\s*attach\s*:\s*(.*?)}/i), 'convertAttachmentExternal');
	this.addInternalProcessorBefore('convertLinkInternal', (/<a.*wikiattachment:-:(.*?)".*>.*?<\s*\/\s*a\s*>/i), '{attach:$1}');

	this.addToolbarHandler('handleAttachmentsButtons');
	
	this.addCommand('wikiAttachment', 'attachmentCommand');
    this.addCommand('mceImage', 'imageCommand');
}

wikiEditor.initAttachmentsPlugin();

WikiEditor.prototype.ATTACHMENT_CLASS_NAME = "";
WikiEditor.prototype.insertAttachment = function(editor_id, name) {
	this.core.execInstanceCommand(editor_id, "mceInsertRawHTML", false, '<a href="wikiattachment:-:' + name + '" class="' + this.ATTACHMENT_CLASS_NAME + '">' + name + '<\/a>');
}

WikiEditor.prototype.attachmentCommand = function(editor_id, element, command, user_interface, value) {
	var template = new Array();

    template['file'] = 'attachment.htm';
	template['width'] = 520;
	template['height'] = 330;

    tinyMCE.openWindow(template, {editor_id : editor_id, scrollbars : 'yes',  resizable : 'no', mce_windowresize: false});

    return this.dummyCommand();
}

WikiEditor.prototype.imageCommand = function(editor_id, element, command, user_interface, value) {
	var template = new Array();

    template['file'] = 'image.htm';
    template['width'] = 520;
	template['height'] = 330;
    tinyMCE.openWindow(template, {editor_id : editor_id, scrollbars : 'yes', resizable : 'no', mce_windowresize: false});
    return this.dummyCommand();
}

WikiEditor.prototype.insertImage = function(src, width, height) {
	this.core.insertImage(this.getImagePath() + src, "", "", "", "", width, height, "", "", "", "");
}

WikiEditor.prototype.convertImageInternal = function(regexp, result, content) {
	var str="";
    var attributes = this.trimString(result[1] + " " + result[3]);
    var att = this.readAttributes(attributes);
	var href;
    
    if(att && (href = att["src"]) != null) {
        href = this.trimString(href);
        if (window.navigator.appName.substring(0,9) == "Microsoft") {
              href = "/xwiki/bin/" + href.substring(href.indexOf("/",3) + 1);
        }
        var imgname_reg = new RegExp(this.getImagePath() + "(.*)", "i");
        var r = imgname_reg.exec(href);
        if(r) {
            var imgname = r[1].replace(/%20/g," ");;
			str = "{image:" + imgname;
            var width=att["width"]?this.trimString(att["width"]):"";
			var height=att["height"]?this.trimString(att["height"]):"";
			if(width != "" || height != "") {
				str += "|" + (height?height:"") + "|" + (width?width:"");
			}
			str += "}";
		}
	}
	return content.replace(regexp, str);
}

WikiEditor.prototype.IMAGE_CLASS_NAME = "wikiimage";

WikiEditor.prototype.convertImageExternal = function(regexp, result, content) {
	var str = "<img id=\"" + result[1] + "\" class=\"" + this.IMAGE_CLASS_NAME + "\" src=\"" + this.getImagePath() + result[1] + "\" ";
	var width, height;
	if( result[5] && (width = this.trimString(result[5])) != "") {
		str += "width=\"" + width + "\" ";
	}
	if( result[3] && (height = this.trimString(result[3])) != "") {
		str += "height=\"" + height + "\" ";
	}
	
	str += "\/>";
	
	return content.replace(regexp, str);
}

WikiEditor.prototype.handleAttachmentsButtons = function(editor_id, node, undo_index, undo_levels, visual_aid, any_selection) {
   tinyMCE.switchClassSticky(editor_id + '_image', 'mceButtonNormal');
	do
	{
		switch (node.nodeName.toLowerCase())
		{
			case "img":
					tinyMCE.switchClassSticky(editor_id + '_image', 'mceButtonSelected');
				break;
		}
	} while ((node = node.parentNode));
}

WikiEditor.prototype.getAttachmentsToolbar = function() {
	return this.getAttachmentsControls("image") + this.getAttachmentsControls("attachment");
}

WikiEditor.prototype.getAttachmentsControls = function(button_name) {
	var str="";
	switch(button_name) {
		case 'image':
			str = this.createButtonHTML('image', 'image.gif', '{$lang_image_desc}', 'mceImage', true);
			break;
		case 'attachment':
			str = this.createButtonHTML('attachment', 'attachment.gif', '{$lang_attachment_desc}', 'wikiAttachment', true);
			break;
	}
	return str;
}

WikiEditor.prototype.convertAttachmentExternal = function(regexp, result, content) {
	var str = '<a href="wikiattachment:-:' + result[1] + '" class="' + this.ATTACHMENT_CLASS_NAME + '">' + result[1] + '<\/a>';
	return content.replace(regexp, str);
}