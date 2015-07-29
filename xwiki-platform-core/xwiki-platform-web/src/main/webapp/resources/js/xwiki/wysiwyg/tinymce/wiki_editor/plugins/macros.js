/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
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
