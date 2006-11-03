/* Import theme specific language pack */
tinyMCE.importThemeLanguagePack('wikieditor');

// Modified by : Phung Hai Nam (phunghainam@xwiki.com) for XWiki
// Version : 10 Oct 2006

var TinyMCE_WikieditorTheme = {

   /**
    * Returns HTML code for the specificed control.
    */
    getControlHTML : function(button_name) {
        return wikiEditor.getControlHTML(button_name);
    },

    /**
     * Theme specific exec command handeling.
     */
    execCommand : function(editor_id, element, command, user_interface, value) {
        switch (command) {
            case "mceLink":
                var inst = tinyMCE.getInstanceById(editor_id);
				var doc = inst.getDoc();
				var selectedText = "";

				if (tinyMCE.isMSIE) {
					var rng = doc.selection.createRange();
					selectedText = rng.text;
				} else
					selectedText = inst.getSel().toString();

				if (!tinyMCE.linkElement) {
					if ((tinyMCE.selectedElement.nodeName.toLowerCase() != "img") && (selectedText.length <= 0))
						return true;
				}

				var href = "", target = "", title = "", onclick = "", action = "insert", style_class = "";
                var text = "";

                if ((tinyMCE.selectedElement.nodeName.toLowerCase() != "img") && (selectedText.length > 0))
                    text = selectedText;

                if (tinyMCE.selectedElement.nodeName.toLowerCase() == "a")
					tinyMCE.linkElement = tinyMCE.selectedElement;

				// Is anchor not a link
				if (tinyMCE.linkElement != null && tinyMCE.getAttrib(tinyMCE.linkElement, 'href') == "")
					tinyMCE.linkElement = null;

				if (tinyMCE.linkElement) {
                    if (tinyMCE.isMSIE) {
				    	text = tinyMCE.linkElement.innerHTML;
				    } else
                        text = tinyMCE.linkElement.text;
                    
                    href = tinyMCE.getAttrib(tinyMCE.linkElement, 'href');
                    target = tinyMCE.getAttrib(tinyMCE.linkElement, 'target');
					title = tinyMCE.getAttrib(tinyMCE.linkElement, 'title');
					onclick = tinyMCE.getAttrib(tinyMCE.linkElement, 'onclick');
					style_class = tinyMCE.getAttrib(tinyMCE.linkElement, 'class');

					// Try old onclick to if copy/pasted content
					if (onclick == "")
						onclick = tinyMCE.getAttrib(tinyMCE.linkElement, 'onclick');

					onclick = tinyMCE.cleanupEventStr(onclick);

					href = eval(tinyMCE.settings['urlconverter_callback'] + "(href, tinyMCE.linkElement, true);");

					// Use mce_href if defined
					mceRealHref = tinyMCE.getAttrib(tinyMCE.linkElement, 'mce_href');
					if (mceRealHref != "") {
						href = mceRealHref;

						if (tinyMCE.getParam('convert_urls'))
							href = eval(tinyMCE.settings['urlconverter_callback'] + "(href, tinyMCE.linkElement, true);");
					}

					action = "update";
				}

                var template = new Array();
				template['file'] = 'link.htm';
				template['width'] = 600;
				template['height'] = 450;

				// Language specific width and height addons
				template['width'] += tinyMCE.getLang('lang_insert_link_delta_width', 0);
				template['height'] += tinyMCE.getLang('lang_insert_link_delta_height', 0);

				if (inst.settings['insertlink_callback']) {
					var returnVal = eval(inst.settings['insertlink_callback'] + "(href, target, title, onclick, action, style_class);");
					if (returnVal && returnVal['href'])
						TinyMCE_WikieditorTheme._insertLink(returnVal['href'], returnVal['target'], returnVal['title'], returnVal['onclick'], returnVal['style_class']);
				} else {
					tinyMCE.openWindow(template, {editor_id : editor_id, action : action, text : text, href : href, target : target, title : title, onclick : onclick, action : action, className : style_class, inline : "yes", scrollbars : 'yes',  resizable : 'no', mce_windowresize: false});
				}

				return true;

            default :
                return wikiEditor.execCommand(editor_id, element, command, user_interface, value);
        }
    },

    getEditorTemplate : function(settings, editorId) {
        return wikiEditor.getEditorTemplate(settings, editorId);
	},

    /**
     * Node change handler.
     */
    handleNodeChange : function(editor_id, node, undo_index, undo_levels, visual_aid, any_selection) {
        wikiEditor.handleNodeChange(editor_id, node, undo_index, undo_levels, visual_aid, any_selection);
        return true;
    },

    insertImage : function(src, width, height, align) {
        this._insertImage(wikiEditor.getImagePath() + src, "", "", "", "", width, height, align, "", "", "")
    },

   _insertImage : function(src, alt, border, hspace, vspace, width, height, align, title, onmouseover, onmouseout) {
		tinyMCE.execCommand('mceBeginUndoLevel');

		if (src == "")
			return;

		if (!tinyMCE.imgElement && tinyMCE.isSafari) {
			var html = "";

			html += '<img class="wikiimage" src="' + src + '" alt="' + alt + '"';
			html += ' border="' + border + '" hspace="' + hspace + '"';
			html += ' vspace="' + vspace + '" width="' + width + '"';
			html += ' height="' + height + '" align="' + align + '" title="' + title + '" onmouseover="' + onmouseover + '" onmouseout="' + onmouseout + '" />';

			tinyMCE.execCommand("mceInsertContent", false, html);
		} else {
			if (!tinyMCE.imgElement && tinyMCE.selectedInstance) {
				if (tinyMCE.isSafari)
					tinyMCE.execCommand("mceInsertContent", false, '<img src="' + tinyMCE.uniqueURL + '" />');
				else
					tinyMCE.selectedInstance.contentDocument.execCommand("insertimage", false, tinyMCE.uniqueURL);

				tinyMCE.imgElement = tinyMCE.getElementByAttributeValue(tinyMCE.selectedInstance.contentDocument.body, "img", "src", tinyMCE.uniqueURL);
			}
		}

		if (tinyMCE.imgElement) {
			var needsRepaint = false;
			var msrc = src;

			src = eval(tinyMCE.settings['urlconverter_callback'] + "(src, tinyMCE.imgElement);");

			if (tinyMCE.getParam('convert_urls'))
				msrc = src;

			if (onmouseover && onmouseover != "")
				onmouseover = "this.src='" + eval(tinyMCE.settings['urlconverter_callback'] + "(onmouseover, tinyMCE.imgElement);") + "';";

			if (onmouseout && onmouseout != "")
				onmouseout = "this.src='" + eval(tinyMCE.settings['urlconverter_callback'] + "(onmouseout, tinyMCE.imgElement);") + "';";

			// Use alt as title if it's undefined
			if (typeof(title) == "undefined")
				title = alt;

			if (width != tinyMCE.imgElement.getAttribute("width") || height != tinyMCE.imgElement.getAttribute("height") || align != tinyMCE.imgElement.getAttribute("align"))
				needsRepaint = true;

			tinyMCE.setAttrib(tinyMCE.imgElement, 'src', src);
            tinyMCE.setAttrib(tinyMCE.imgElement, 'class', "wikiimage");
            tinyMCE.setAttrib(tinyMCE.imgElement, 'mce_src', msrc);
			tinyMCE.setAttrib(tinyMCE.imgElement, 'alt', alt);
			tinyMCE.setAttrib(tinyMCE.imgElement, 'title', title);
			tinyMCE.setAttrib(tinyMCE.imgElement, 'align', align);
			tinyMCE.setAttrib(tinyMCE.imgElement, 'border', border, true);
			tinyMCE.setAttrib(tinyMCE.imgElement, 'hspace', hspace, true);
			tinyMCE.setAttrib(tinyMCE.imgElement, 'vspace', vspace, true);
			tinyMCE.setAttrib(tinyMCE.imgElement, 'width', width, true);
			tinyMCE.setAttrib(tinyMCE.imgElement, 'height', height, true);
			tinyMCE.setAttrib(tinyMCE.imgElement, 'onmouseover', onmouseover);
			tinyMCE.setAttrib(tinyMCE.imgElement, 'onmouseout', onmouseout);

			// Fix for bug #989846 - Image resize bug
			if (width && width != "")
				tinyMCE.imgElement.style.pixelWidth = width;

			if (height && height != "")
				tinyMCE.imgElement.style.pixelHeight = height;

			if (needsRepaint)
				tinyMCE.selectedInstance.repaint();
		}

		tinyMCE.execCommand('mceEndUndoLevel');
	},

    insertLink : function(href, target, text, space, title, dummy, style_class) {
        var classname;
	    if(wikiEditor.isExternalLink(href)) {
		    classname = wikiEditor.LINK_EXTERNAL_CLASS_NAME;
	    } else {
		    classname = wikiEditor.LINK_INTERNAL_CLASS_NAME;
	    }
        this._insertLink(href, target, text, space, title, dummy, classname);
    },

    _insertLink : function(href, target, text, space, title, onclick, style_class) {
		tinyMCE.execCommand('mceBeginUndoLevel');

        if (space != null && space != "")
            href = space + "." + href;

        if (href == null || href == "")
            return;

        if (tinyMCE.selectedInstance && tinyMCE.selectedElement && tinyMCE.selectedElement.nodeName.toLowerCase() == "img") {
			var doc = tinyMCE.selectedInstance.getDoc();
			var linkElement = tinyMCE.getParentElement(tinyMCE.selectedElement, "a");
			var newLink = false;

			if (!linkElement) {
				linkElement = doc.createElement("a");
				newLink = true;
			}

            var mhref = href;
			var thref = eval(tinyMCE.settings['urlconverter_callback'] + "(href, linkElement);");
			mhref = tinyMCE.getParam('convert_urls') ? href : mhref;
			tinyMCE.setAttrib(linkElement, 'href', thref);
			tinyMCE.setAttrib(linkElement, 'mce_href', mhref);
			tinyMCE.setAttrib(linkElement, 'target', target);
			tinyMCE.setAttrib(linkElement, 'title', title);
			tinyMCE.setAttrib(linkElement, 'onclick', onclick);
			tinyMCE.setAttrib(linkElement, 'class', style_class);

			if (newLink) {
				linkElement.appendChild(tinyMCE.selectedElement.cloneNode(true));
				tinyMCE.selectedElement.parentNode.replaceChild(linkElement, tinyMCE.selectedElement);
			}

			return;
		}

		if (!tinyMCE.linkElement && tinyMCE.selectedInstance) {
            if (tinyMCE.isSafari) {
                tinyMCE.execCommand("mceInsertContent", false, '<a href="' + tinyMCE.uniqueURL + '">' + tinyMCE.selectedInstance.selection.getSelectedHTML() + '</a>');
			} else
				tinyMCE.selectedInstance.contentDocument.execCommand("createlink", false, tinyMCE.uniqueURL);

			tinyMCE.linkElement = tinyMCE.getElementByAttributeValue(tinyMCE.selectedInstance.contentDocument.body, "a", "href", tinyMCE.uniqueURL);

			var elementArray = tinyMCE.getElementsByAttributeValue(tinyMCE.selectedInstance.contentDocument.body, "a", "href", tinyMCE.uniqueURL);

			for (var i=0; i<elementArray.length; i++) {
				var mhref = href;
				var thref = eval(tinyMCE.settings['urlconverter_callback'] + "(href, elementArray[i]);");
				mhref = tinyMCE.getParam('convert_urls') ? href : mhref;

                tinyMCE.setAttrib(elementArray[i], 'href', thref);
				tinyMCE.setAttrib(elementArray[i], 'mce_href', mhref);
				tinyMCE.setAttrib(elementArray[i], 'target', target);
				tinyMCE.setAttrib(elementArray[i], 'title', title);
				tinyMCE.setAttrib(elementArray[i], 'onclick', onclick);
				tinyMCE.setAttrib(elementArray[i], 'class', style_class);
			}

			tinyMCE.linkElement = elementArray[0];
		}

		if (tinyMCE.linkElement) {
            var mhref = href;
			href = eval(tinyMCE.settings['urlconverter_callback'] + "(href, tinyMCE.linkElement);");
			mhref = tinyMCE.getParam('convert_urls') ? href : mhref;

            tinyMCE.linkElement.innerHTML = text;

            tinyMCE.setAttrib(tinyMCE.linkElement, 'href', href);

            tinyMCE.setAttrib(tinyMCE.linkElement, 'mce_href', mhref);
			tinyMCE.setAttrib(tinyMCE.linkElement, 'target', target);
			tinyMCE.setAttrib(tinyMCE.linkElement, 'title', title);
			tinyMCE.setAttrib(tinyMCE.linkElement, 'onclick', onclick);
			tinyMCE.setAttrib(tinyMCE.linkElement, 'class', style_class);
		}

		tinyMCE.execCommand('mceEndUndoLevel');
	}
};

tinyMCE.addTheme("wikieditor", TinyMCE_WikieditorTheme);
