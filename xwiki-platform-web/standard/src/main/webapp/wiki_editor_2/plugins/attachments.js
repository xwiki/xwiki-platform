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
	
	this.addExternalProcessorBefore("convertTableExternal", (/{\s*image\s*:\s*(.*?)(\|(.*?))?(\|(.*?))?(\|(.*?))?(\|(.*?))?}/i), 'convertImageExternal');
	this.addInternalProcessorBefore("convertStyleInternal", (/(<div\s*class=\"img(.*?)\"\s*>\s*)?<img\s*([^>]*)(class=\"wikiimage\")\s*([^>]*)\/>(<\/div>)?/i), 'convertImageInternal');

	this.addExternalProcessorBefore("convertTableExternal", (/{\s*attach\s*:\s*(.*?)(\|(.*?))?}/i), 'convertAttachmentExternal');
    this.addInternalProcessorBefore('convertLinkInternal', (/<a\s*href=\"wikiattachment:-:(.*?)\"\s*([^>]*)>(.*?)<\/a>/i), 'convertAttachmentInternal');

	this.addToolbarHandler('handleAttachmentsButtons');
	
	this.addCommand('wikiAttachment', 'attachmentCommand');
    this.addCommand('mceImage', 'imageCommand');
}

wikiEditor.initAttachmentsPlugin();

WikiEditor.prototype.ATTACHMENT_CLASS_NAME = "";

WikiEditor.prototype.insertAttachment = function(editor_id, title, name) {
    var text = ((title != null) && this.trimString(title) != "") ? title : name;
    this.core.execInstanceCommand(editor_id, "mceInsertRawHTML", false, '<a href="wikiattachment:-:' + name + '" class="' + this.ATTACHMENT_CLASS_NAME + '">' + text + '<\/a>');
}

WikiEditor.prototype.attachmentCommand = function(editor_id, element, command, user_interface, value) {
	var href = "", action = "insert";
    var template = new Array();

    template['file'] = 'attachment.htm';
	template['width'] = 550;
	template['height'] = 400 + (tinyMCE.isMSIE ? 25 : 0);

    tinyMCE.openWindow(template, {editor_id : editor_id, href : href, action : action, scrollbars : 'yes',  resizable : 'no', mce_windowresize: false});

    return this.dummyCommand();
}

WikiEditor.prototype.imageCommand = function(editor_id, element, command, user_interface, value) {
    var src = "", alt = "", border = "", hspace = "", vspace = "", width = "", height = "", align = "", halign = "";
    var title = "", onmouseover = "", onmouseout = "", action = "insert";
    var img = tinyMCE.imgElement;
    var inst = tinyMCE.getInstanceById(editor_id);

    if (tinyMCE.selectedElement != null && tinyMCE.selectedElement.nodeName.toLowerCase() == "img") {
        img = tinyMCE.selectedElement;
        tinyMCE.imgElement = img;
        var parent = tinyMCE.selectedElement.parentNode;
        var parentClassName = parent.className;
        if (parent.nodeName.toLowerCase() == "div") {
            halign = parentClassName.substring(3, parentClassName.length);
        } 
    }

    if (img) {
        // Is it a internal MCE visual aid image, then skip this one.
        if (tinyMCE.getAttrib(img, 'name').indexOf('mce_') == 0)
            return true;

        src = tinyMCE.getAttrib(img, 'src');
        alt = tinyMCE.getAttrib(img, 'alt');

        // Try polling out the title
        if (alt == "")
            alt = tinyMCE.getAttrib(img, 'title');

        // Fix width/height attributes if the styles is specified
        if (tinyMCE.isGecko) {
            var w = img.style.width;
            if (w != null && w != "")
                img.setAttribute("width", w);

            var h = img.style.height;
            if (h != null && h != "")
                img.setAttribute("height", h);
        }

        border = tinyMCE.getAttrib(img, 'border');
        hspace = tinyMCE.getAttrib(img, 'hspace');
        vspace = tinyMCE.getAttrib(img, 'vspace');
        width = tinyMCE.getAttrib(img, 'width');
        height = tinyMCE.getAttrib(img, 'height');
        align = tinyMCE.getAttrib(img, 'align');

        onmouseover = tinyMCE.getAttrib(img, 'onmouseover');
        onmouseout = tinyMCE.getAttrib(img, 'onmouseout');
        title = tinyMCE.getAttrib(img, 'title');

        // Is realy specified?
        if (tinyMCE.isMSIE) {
            width = img.attributes['width'].specified ? width : "";
            height = img.attributes['height'].specified ? height : "";
        }

        src = eval(tinyMCE.settings['urlconverter_callback'] + "(src, img, true);");

        src = src.substring(src.lastIndexOf("/") + 1, src.length);

        // Use mce_src if defined
        mceRealSrc = tinyMCE.getAttrib(img, 'mce_src');
        if (mceRealSrc != "") {
            src = mceRealSrc;

            if (tinyMCE.getParam('convert_urls'))
                src = eval(tinyMCE.settings['urlconverter_callback'] + "(src, img, true);");
                src = src.substring(src.lastIndexOf("/") + 1, src.length);
        }

        action = "update";
    }

    var template = new Array();

    template['file'] = 'image.htm';
    template['width'] = 550;
	template['height'] = 400 + (tinyMCE.isMSIE ? 25 : 0);

    if (inst.settings['insertimage_callback']) {
        var returnVal = eval(inst.settings['insertimage_callback'] + "(src, alt, border, hspace, vspace, width, height, align, title, onmouseover, onmouseout, action);");
        if (returnVal && returnVal['src'])
            TinyMCE_WikieditorTheme.insertImage(returnVal['src'], returnVal['width'], returnVal['height'], returnVal['align']);
    } else {
        tinyMCE.openWindow(template, {editor_id : editor_id, scrollbars : 'yes', resizable : 'no', mce_windowresize: false, src : src, alt : alt, border : border, hspace : hspace, vspace : vspace, width : width, height : height, align : align, halign : halign, title : title, onmouseover : onmouseover, onmouseout : onmouseout, action : action, inline : "yes"});
    }

    return this.dummyCommand();
}

WikiEditor.prototype.convertImageInternal = function(regexp, result, content) {
    var str="";
    var href;
    var halign = "";
    var attributes = this.trimString(result[3] + " " + result[5]);
    var att = this.readAttributes(attributes);
    if (result[2] && this.trimString(result[2]) != "") {
        halign = this.trimString(result[2]);
    }
    if(att && (href = att["src"]) != null) {
        href = this.trimString(href);
        href = "/xwiki/bin/" + href.substring(href.indexOf("/",3) + 1);
        var imgname_reg = new RegExp(this.getImagePath() + "(.*)", "i");
        var r = imgname_reg.exec(href);
        if(r) {
            var imgname = r[1].replace(/%20/g," ");
			str = "{image:" + imgname;
            var width=att["width"] ? this.trimString(att["width"]) : "";
			var height=att["height"] ? this.trimString(att["height"]) : "";
            var align=att["align"] ? this.trimString(att["align"]) : "";
            if (width != "" || height != "" || align != "" || halign != "") {
				str += "|" + (height ? height : " ") + "|" + (width ? width : " ");
                if (halign && halign != "") {
                    str += "|" + (align ? align : " ") + "|" + (halign ? halign : "");
                } else if (align != "") {
                    str += "|" + (align ? align : "");
                }
            }

            str += "}";
		}
	}
    return content.replace(regexp, str);
}

WikiEditor.prototype.IMAGE_CLASS_NAME = "wikiimage";

WikiEditor.prototype.convertImageExternal = function(regexp, result, content) {
    var width, height, align;
    var halign; this.trimString(result[9]);
    var str = "";
    if (result[9]) {
        halign = this.trimString(result[9]);
    } else {
        halign = "";
    }
    if (halign != "") {
        str += "<div class=\"img" + halign + "\">"
    }
    str += "<img id=\"" + result[1] + "\" class=\"" + this.IMAGE_CLASS_NAME + "\" src=\"" + this.getImagePath() + result[1] + "\" ";
	if( result[5] && (width = this.trimString(result[5])) != "") {
		str += "width=\"" + width + "\" ";
	}
	if( result[3] && (height = this.trimString(result[3])) != "") {
		str += "height=\"" + height + "\" ";
	}

    if( result[7] && (align = this.trimString(result[7])) != "") {
		str += "align=\"" + align + "\" ";
	}
    if(halign != "") {
		str += "halign=\"" + halign + "\" ";
	}

    str += "\/>";
    
    if (halign != "") {
        str += "</div>";
    }
    return content.replace(regexp, str);
}

WikiEditor.prototype.handleAttachmentsButtons = function(editor_id, node, undo_index, undo_levels, visual_aid, any_selection) {
   tinyMCE.switchClass(editor_id + '_image', 'mceButtonNormal');
	do
	{
		switch (node.nodeName.toLowerCase())
		{
			case "img":
					tinyMCE.switchClass(editor_id + '_image', 'mceButtonSelected');
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
			str = this.createButtonHTML('image', 'image.gif', 'lang_image_desc', 'mceImage', true);
			break;
		case 'attachment':
			str = this.createButtonHTML('attachment', 'attachment.gif', 'lang_attachment_desc', 'wikiAttachment', true);
			break;
	}
	return str;
}

WikiEditor.prototype.convertAttachmentExternal = function(regexp, result, content) {
    var href = ((typeof(result[3]) == "undefined") || (this.trimString(result[3]) == "")) ? result[1] : result[3] ;
    var str = '<a href="wikiattachment:-:' + href + '" class="' + this.ATTACHMENT_CLASS_NAME + '">' + result[1] + '<\/a>';
    return content.replace(regexp, str);
}

WikiEditor.prototype.convertAttachmentInternal = function(regexp, result, content) {
    result[1] = result[1].replace(/%20/gi, " ");
    result[3] = result[3].replace(/%20/gi, " ");
    var str;
    if (result[1] == result[3]) str = "{attach:" + result[1] + "}";
    else  if ((result[1] == "undefined") || (this.trimString(result[1]) == "")) str = "{attach:" + result[3] + "}";
    else str = "{attach:" + result[3] + "|" + result[1] + "}";
    return content.replace(regexp, str);
}
