/*--------------------------------------------------------------------------------------------------
**
**  PROCOLOR v1.0
**
**  Copyright (c) 2009 by the Phantom Inker (inker2576@yahoo.com).
**  All Rights Reserved.
**
**--------------------------------------------------------------------------------------------------
**
**  OVERVIEW
**  --------
**
**  ProColor is yet another web color picker, this time for Prototype, and hue-wheel-based,
**  which seems to be a friendlier concept to artists than many of the RGB-box-based pickers.
**
**--------------------------------------------------------------------------------------------------
*/
/*!
**  PROCOLOR LICENSE
**  ----------------
**
**  Redistribution and use in source and binary forms, with or without
**  modification, are permitted provided that the following conditions are met:
**
**    * Redistributions of source code must retain the above copyright
**       notice, this list of conditions and the following disclaimer.
**
**    * Redistributions in binary form must reproduce the above copyright
**       notice, this list of conditions and the following disclaimer in the
**       documentation and/or other materials provided with the distribution.
**
**  THIS SOFTWARE IS PROVIDED BY THE PHANTOM INKER AND CONTRIBUTORS "AS IS"
**  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
**  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
**  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
**  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
**  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
**  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
**  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
**  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
**  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
**  THE POSSIBILITY OF SUCH DAMAGE.
*/
/**--------------------------------------------------------------------------------------------------
**
**  USAGE
**  -----
**
**  Usage is straightforward.  Include this file (or better yet, the compressed
**  version), and then make this call to create a color-picker:
**
**    new ProColor(options);
**
**  For example, for a static color picker:
**
**    new ProColor({ mode:'static', parent:container_element });
**
**  Or for a drop-down/popup color picker:
**
**    new ProColor({ mode:'popup', input:input_element });
**
**  If you want a drop-down color picker the easy way, it's better to use the button factory
**  method to create them:
**
**    <input type='text' value='#FF0000' id='mycolor' />
**    ...
**    ProColor.prototype.attachButton('mycolor', options);
**
**  Or if that's still too hard, you can make it even simpler by using the automatic factory
**  method that runs during onload.  This factory method uses the current setting of
**  ProColor.prototype.options for its options, and creates a button attached to any <input>
**  whose class contains 'procolor':
**
**    <input type='text' value='#FF0000' class='procolor' />
**
**--------------------------------------------------------------------------------------------------
**
**  OPTIONS
**  -------
**
**  ProColor offers many different options for configuring its behavior.  All of the important
**  ones are documented below.
**
**  Options:
**
**    mode:          Either 'static' or 'popup'.
**
**    input:         Input textbox or hidden field to record color in.  (Format: '#RRGGBB' string.)
**
**    showInField:   Show the current color as the input field's background color (default: false).
**
**    parent:        In static mode, the containing element to insert this into.
**                   In popup mode, this is the element the popup should be attached to (usually
**                   the same as input).
**
**    closeButton:   Show close button (default: false for 'static', true for 'popup').
**
**    color:         Initial color to select (default: extract from input field, or use #FFFFFF).
**
**    imgPath:       URL to dir containing ProColor's .png files, if they're not in the same place
**                   as the .js file.  This is directly prepended to the names of the files, so
**                   it usually must have a trailing '/'.
**
**    width:         Dimensions of the color picker, in pixels.  Default width is 360.
**    height:        Dimensions of the color picker, in pixels.  Default height is 192.
**
**    offsetx:       Offset of color picker's content, from its left edge, in pixels.  Default is 0.
**    offsety:       Offset of color picker's content, from its top edge, in pixels.  Default is 0.
**
**    editbg:        Background color of color picker's edit fields.  Default is '#FFFFFF'.
**    edittext:      Color of text in color picker's edit fields:  Default is '#4C4C4C'.
**
**    outputFormat:  How to format the text in the textbox.  A format string that accepts:
**
**                     {RR}   Two-digit red hex value.
**                     {GG}   Two-digit green hex value.
**                     {BB}   Two-digit blue hex value.
**                     {R}    One-digit red hex value.
**                     {G}    One-digit green hex value.
**                     {B}    One-digit blue hex value.
**                     {red}  Decimal red (0-255).
**                     {grn}  Decimal green (0-255).
**                     {blu}  Decimal blue (0-255).
**                     {hue}  Decimal hue (0-359).
**                     {sat}  Decimal saturation (0-100).
**                     {brt}  Decimal brightness (0-100).
**                     .      All other characters will be copied as-is.
**
**                   In addition, you may use tokens like {rr} to request lowercase hex instead
**                   of uppercase.  The default format string is "#{RR}{GG}{BB}".
**
**    onOpening:     Called when the color picker is about to appear:  callback(this, 'opening').
**    onOpened:      Called after the color picker has appeared:  callback(this, 'opened').
**    onClosing:     Called when the color picker is about to be destroyed:  callback(this, 'closing').
**    onClosed:      Called after the color picker has been destroyed:  callback(this, 'closed').
**
**    onChanging:    Called when the color is changing (many events):  callback(this, 'changing').
**    onChanged:     Called when the color has changed (at mouseup):  callback(this, 'changed').
**
**    onCancelClick: Called when user presses Esc or clicks outside popup:  callback(this, 'cancelclick').
**    onAcceptClick: Called when user presses Enter or double-clicks:  callback(this, 'acceptclick').
**
**    onCloseButton: Called when user clicks close button, before closing:  callback(this, 'closebutton').
**
**--------------------------------------------------------------------------------------------------
**
**  CHANGELOG
**  ---------
**
**  Version 1.0 (2009-02-26) - Initial release.
**
**------------------------------------------------------------------------------------------------*/


/*--------------------------------------------------------------------------------------------------
**  Make sure that Prototype has already been loaded.
*/

if (typeof Prototype == 'undefined')
	alert("ProColor Error:  Prototype is not loaded. Please make sure that your page includes prototype.js before it includes procolor.js.");
if (Prototype.Version < "1.6")
	alert("ProColor Error:  Minimum Prototype 1.6.0 is required; you are using " + Prototype.Version);


/*--------------------------------------------------------------------------------------------------
**  Add two useful methods to Element.  Many other libraries add build(), and ours
**  is, in theory, compatible with all of their versions.
*/

Element.addMethods({
	/* Create a child element with the given options (attributes) and style. */
	build: function(element, type, options, style) {
		var e = $(document.createElement(type));
		$H(options).each(function(pair) { e[pair.key] = pair.value; });
		if (style) e.setStyle(style);
		element.appendChild(e);
		return e;
	},

	/* Return true if this event was a mouse event that occurred inside the given element. */
	isEventIn: function(element, event) {
		var d = element.getDimensions();
		var p = element.cumulativeOffset();
		var x = event.pointerX(), y = event.pointerY();
		return (x >= p.left && y >= p.top && x < p.left+d.width && y < p.top+d.height);
	}
});


/*--------------------------------------------------------------------------------------------------
**  This is a simple class for capturing all mouse input after a mouse button
**  is held down until the mouse button is released.
*/

var MouseCapture = Class.create({

	initialize: function() { },
	
	onEvent: function(event, callback) {
		if (callback && event.type != 'mouseover' && event.type != 'mouseout')
			callback(event, event.type);
		event.stop();
	},

	setCursor: function(c) {
		if (this.div)
			this.div.setStyle({ cursor: c });
	},

	begin: function(callback) {
		/* Create our event listener.  We'll need this object now, and later on to
		   be able to stop listening to events too. */
		this.listener = this.onEvent.bindAsEventListener(this, callback);

		/* Start observing events. */
		Event.observe(document, 'mouseup', this.listener);
		Event.observe(document, 'mousemove', this.listener);
		Event.observe(document, 'mousedown', this.listener);
		Event.observe(document, 'mouseover', this.listener);
		Event.observe(document, 'mouseout', this.listener);
		Event.observe(document, 'keyup', this.listener);
		Event.observe(document, 'keydown', this.listener);

		/* Don't let the browser perform text-selection while capturing. */
		this.old_body_ondrag = document.body.ondrag;
		this.old_body_onselectstart = document.body.onselectstart;
		document.body.ondrag = function () { return false; };
		document.body.onselectstart = function () { return false; };

		var body = Element.extend(document.body);
		var dim = body.getDimensions();

		/* Build a (nearly) invisible <div> that covers the entire document and that
		   will capture all events, even those that would go to iframes. */
		this.div = body.build('div', { }, {
			display: 'block',
			position: 'absolute',
			top: '0px', left: '0px',
			width: dim.width + 'px', height: dim.height + 'px',
			zIndex: 999999999,
			cursor: 'default',
			backgroundColor: '#FFFFFF',
			opacity: 0.0001
		});
	},

	end: function() {
		/* Remove our invisible event-capturing <div>. */
		this.div.remove();

		/* Stop event observing. */
		Event.stopObserving(document, 'mouseup', this.listener);
		Event.stopObserving(document, 'mousemove', this.listener);
		Event.stopObserving(document, 'mousedown', this.listener);
		Event.stopObserving(document, 'mouseover', this.listener);
		Event.stopObserving(document, 'mouseout', this.listener);
		Event.stopObserving(document, 'keyup', this.listener);
		Event.stopObserving(document, 'keydown', this.listener);

		/* Reenable selection. */
		document.body.ondrag = this.old_body_ondrag;
		document.body.onselectstart = this.old_body_onselectstart;
		delete this.old_body_ondrag;
		delete this.old_body_onselectstart;
	}

});


/*--------------------------------------------------------------------------------------------------
**  The ProColor class, which can be used to instantiate ProColor color-picker objects.
*/

var ProColor = Class.create({


	/*------------------------------------------------------------------------------------------
	**  Default object properties.
	*/
	
	/* The entire color palette, minus the easy grayscale colors, and packed down to
	   consume as little code space as possible.  This is stored three hex digits per
	   color, in the same order as the visible palette. */
	palette: [
		"F00C30900633963C63",
		"FC9FC0C93F93F96F60",
		"993CC3FF0FF6FF9FFC",
		"3306639966939C39C6",
		"0F09C99F9CFCCF69F0",
		"3C36960630933C6396",
		"6999CC9FF0FF6FC3C9",
		"36603303936F69F0CF",
		"00666C00F66F99FCCF",
		"C9F90F60C63F30930C",
		"96CC6FC3F90C639306",
		"C9C969C6CF9F909606",
		"F0FF9CC69F39F69F06",
		"F66F99FCC936C36C39"
	],

	/* These are the default options used by the optional automatic setup routine that
	   runs at page onload. */
	options: {
        imgPath:"#set($imgPath = $xwiki.getSkinFile('uicomponents/widgets/colorpicker/img/procolor_win_bg.png'))#set($imgPath = $imgPath.substring(0, $imgPath.lastIndexOf('bg.png')))${imgPath}",
		showInField: true
	},

	/*------------------------------------------------------------------------------------------
	**  Creation and destruction.
	*/

	/* Create this color picker, with the given creation options. */
	initialize: function(options) {

		/* Determine if we're on an old browser that can't handle alpha PNGs. */
		var browser_version = navigator.appVersion.split("MSIE");
		var browser_version_number = parseFloat(browser_version[1]);
		this.old_ie = (browser_version_number >= 5.5 && browser_version_number < 7
			&& document.body.filters);

		/* Set initial values. */
		this.div = null;
		this.color = null;
		this.listeners = { };
		this.dblclk = { time: 0 };

		if (options.parent) this.parent = $(options.parent);
		else this.parent = Element.extend(document.body);

		if (options.input) this.input = $(options.input);
		else this.input = null;

		this.options = {
			mode: 'static',
			width: 360, height: 192,
			offsetx: 13, offsety: 8,
			input: null,
			showInField: false,
			parent: null,
			closeButton: (options.mode == 'popup'),
			imgPath: "#set($imgPath = $xwiki.getSkinFile('uicomponents/widgets/colorpicker/img/procolor_win_bg.png'))#set($imgPath = $imgPath.substring(0, $imgPath.lastIndexOf('bg.png')))${imgPath}",
			color: (!this.input ? '#FFFFFF'
				: this.input.tagName == 'INPUT' ? this.input.value
				: this.input.innerHTML),
			editbg: '#FFFFFF',
			edittext: '#4C4C4C',
			outputFormat: '#{RR}{GG}{BB}',
			onOpening: null,
			onOpened: null,
			onClosing: null,
			onClosed: null,
			onCloseButton: null,
			onChanging: null,
			onChanged: null,
			onCancelClick: null,
			onAcceptClick: null
		};
		for (var i in options)
			this.options[i] = options[i];

		if (this.options.onOpening)
			this.options.onOpening(this, 'opening');

		this.createDiv();

		if (this.options.mode == 'popup') {
			this.positionPopup();
			Event.observe(document, "mousedown",
				this.closeClickHandler = this.handleCloseClick.bindAsEventListener(this));
			Event.observe(document, "keypress",
				this.keyPressHandler = this.handleKeyPress.bindAsEventListener(this));
		}

		if (this.options.onOpened)
			this.options.onOpened(this, 'opened');
	},

	/* Reposition the popup properly relative to its parent element. */
	positionPopup: function() {
		var c_pos = this.div.cumulativeOffset(), c_left = c_pos[0], c_top = c_pos[1];
		var c_dim = this.div.getDimensions(), c_height = c_dim.height, c_width = c_dim.width; 
		var w_height = document.viewport.getHeight(), w_width = document.viewport.getWidth();
		var w_scroll = document.viewport.getScrollOffsets();
		var e_dim = $(this.input).cumulativeOffset(), e_top = e_dim[1], e_left = e_dim[0];
		var e_height = $(this.input).getDimensions().height, e_bottom = e_top + e_height;

		if (e_left + c_width > w_width - 8)
			e_left = w_width - 8 - c_width;
		if (e_left < 8) e_left = 8;

		var above = (e_bottom + c_height > w_scroll.top + w_height) && (e_bottom - c_height > w_scroll.top);
		var left_px = e_left.toString() + "px";
		var top_px = (above ? (e_top - c_height) : (e_top + e_height)).toString() + "px";

		this.div.style.left = left_px; this.div.style.top = top_px;

		this.div.setStyle({ visibility:"", display:"block" });
	},

	/* Create the main color picker <div> element.  This sets up the entire color picker
	   and sets up suitable event watching.  The color-picker will be created VISIBLE.
	   This returns nothing; it merely updates members of this object. */
	createDiv: function() {
		var style = {
			display: 'block',
			width: this.options.width + 'px',
			height: this.options.height + 'px',
			backgroundPosition: '0% 0%',
			backgroundAttachment: 'scroll',
			backgroundRepeat: 'no-repeat',
			backgroundImage: 'url(' + this.options.imgPath + (this.old_ie ? 'bg.gif)' : 'bg.png)'),
			color: '#BBBBD7',
			left: 0,
			top: 0
		};
		if (this.options.mode == 'popup') {
			style.position = "absolute";
			style.display = "none";
			style.visibility = "hidden";
			style.zIndex = 999999;
		}
		else style.position = "relative";
		this.div = $(this.parent).build('div', { className: "procolor_box" }, style);

		this.img_palette = this.loadBgImage(this.div, 'palette_raw.png', 0, 1, 66, 174);
		this.img_bar_lower = this.loadBgImage(this.div, 'bars.png', 77, 1, 20, 174, { zIndex: 1, opacity: 1.0 });
		this.img_bar_middle = this.loadBgImage(this.div, 'bars.png', 77, 1, 20, 174, { zIndex: 2, opacity: 0.0 });
		this.img_bar_upper = this.loadBgImage(this.div, 'bars.png', 77, 1, 20, 174, { zIndex: 3, opacity: 0.0 });
		this.img_wheel_rgb = this.loadBgImage(this.div, 'wheel_rgb.jpg', 105, 0, 176, 176, { zIndex: 1, opacity: 1.0 });
		this.img_wheel_black = this.loadBgImage(this.div, 'wheel_black.png', 105, 0, 176, 176, { zIndex: 2, opacity: 1.0 });
		this.img_boxes = this.loadBgImage(this.div, 'boxes.png', 287, 25, 50, 149, { zIndex: 1 });

		this.img_bar_dragger = this.createImageButton(this.div, 'sel_rect',
			73 + this.options.offsetx, -1 + this.options.offsety, 24, 8, { zIndex: 4 });
		this.img_wheel_dragger = this.createImageButton(this.div, 'sel_circle',
			184 + this.options.offsetx, 79 + this.options.offsety, 17, 17, { zIndex: 5 });

		this.listeners.wheel = this.onWheelEvent.bindAsEventListener(this);
		this.img_wheel_black.observe('mousedown', this.listeners.wheel);
		this.img_wheel_dragger.observe('mousedown', this.listeners.wheel);
		this.img_wheel_dragger.observe('keydown', this.listeners.wheel);

		this.listeners.bar = this.onBarEvent.bindAsEventListener(this);
		this.img_bar_upper.observe('mousedown', this.listeners.bar);
		this.img_bar_dragger.observe('mousedown', this.listeners.bar);
		this.img_bar_dragger.observe('keydown', this.listeners.bar);

		this.listeners.palette = this.onPaletteEvent.bindAsEventListener(this);
		this.img_palette.observe('mousedown', this.listeners.palette);

		this.wheel = { left: 106 + this.options.offsetx, top: 1 + this.options.offsety,
			width: 174, height: 174 };
		this.bar = { left: 77 + this.options.offsetx, top: 2 + this.options.offsety,
			width: 20, height: 172 };
		this.wheelsel = { width: 17, height: 17 };
		this.barsel = { width: 24, height: 8 };

		this.listeners.rgb = this.onNumberBox.bindAsEventListener(this, 'rgb', 0, 255, 0);
		this.r_edit = this.createEdit(this.div, 287, 25, 37, 20, this.listeners.rgb);
		this.g_edit = this.createEdit(this.div, 287, 47, 37, 20, this.listeners.rgb);
		this.b_edit = this.createEdit(this.div, 287, 69, 37, 20, this.listeners.rgb);

		this.listeners.hue = this.onNumberBox.bindAsEventListener(this, 'hsb', 0, 359, 360);
		this.listeners.satbrt = this.onNumberBox.bindAsEventListener(this, 'hsb', 0, 100, 0);
		this.hue_edit = this.createEdit(this.div, 287, 110, 37, 20, this.listeners.hue);
		this.sat_edit = this.createEdit(this.div, 287, 132, 37, 20, this.listeners.satbrt);
		this.brt_edit = this.createEdit(this.div, 287, 154, 37, 20, this.listeners.satbrt);

		if (this.options.closeButton) {
			this.img_close = this.createImageButton(this.div, 'close', this.options.width - 40, 0, 24, 16);
			this.listeners.close = this.onCloseEvent.bindAsEventListener(this);
			this.img_close.observe('mouseover', this.listeners.close);
			this.img_close.observe('mouseout', this.listeners.close);
			this.img_close.observe('mousedown', this.listeners.close);
			this.img_close.observe('keydown', this.listeners.close);
			this.img_close.trackingMouse = false;
		}

		if (this.input && this.input.tagName == 'INPUT') {
			this.listeners.input = this.onInput.bindAsEventListener(this);
			this.input.observe('keyup', this.listeners.input);
			this.input.observe('focus', this.listeners.input);
			this.input.observe('blur', this.listeners.input);
		}
		else this.listeners.input = null;

		var rgb = this.decodeHexColor(this.options.color);
		if (!rgb) rgb = { r:0, g:0, b:0 };
		this.update('rgb', rgb, []);
	},

	/* Create a <div> at the given relative coordinates with an image as its background,
	   applying the given CSS style options. */
	loadBgImage: function(parent, filename, x, y, w, h, options) {
		var style = {
			display: 'block',
			position: 'absolute',
			width: w + 'px',
			height: h + 'px',
			left: x + this.options.offsetx + 'px',
			top: y + this.options.offsety + 'px',
			padding: '0',
			backgroundImage: 'url(' + this.options.imgPath + filename + ')'
		};
		if (options)
			for (var i in options)
				style[i] = options[i];
		return parent.build('div', { }, style);
	},

	/* Create an <a href='#'> at the given absolute coordinates,
	   applying the given CSS style options.  This is remarkably similar to
	   creating the <div> above, except that this object is able to take the
	   keyboard focus. */
	createImageButton: function(parent, filename, x, y, w, h, options) {
		var style = {
			display: 'block',
			position: 'absolute',
			width: w + 'px',
			height: h + 'px',
			left: x + 'px',
			top: y + 'px',
			border: '0',
			cursor: 'default',
			padding: '0',
			fontSize: '1px', /* Make IE6 respect the width and height */
			backgroundImage: 'url(' + this.options.imgPath + filename
				+ (this.old_ie ? '.gif)' : '.png)')
		};
		if (options)
			for (var i in options)
				style[i] = options[i];
		var element = parent.build('a', { href:'#' }, style);
		return element;
	},

	/* Create an edit field at the given relative coordinates with the given event listener. */
	createEdit: function(parent, x, y, w, h, listener) {
		x += 5; w -= 9;
		y += 2; h -= 6;
		var style = {
			display: 'inline',
			position: 'absolute',
			width: w + 'px',
			height: h + 'px',
			left: x + this.options.offsetx + 'px',
			top: y + this.options.offsety + 'px',
			verticalAlign: 'top',
			backgroundColor: this.options.editbg,
			padding: '0',
			color: this.options.edittext,
			fontFamily: 'Verdana,Tahoma,Arial,sans-serif,sans serif,sans',
			fontSize: '12px',
			fontStyle: 'Normal',
			fontVariant: 'Normal',
			fontWeight: 'Normal',
			textAlign: 'right',
			direction: 'ltr',
			border: 0,
			zIndex: 10
		};

		var element = parent.build('input', { type:'text', value:'0', maxLength:3 }, style);

		element.observe('keypress', listener);
		element.observe('keyup', listener);
		element.observe('focus', listener);
		element.observe('blur', listener);
		
		return element;
	},

	/* Close the color picker and "destroy" it.  After this has been called, the
	   color picker will not be visible on the screen anymore, and its <div> element
	   will no longer exist.  However, the "this.color" value may still be used to
	   identify the last color selected, and may continue to be used until this
	   object is garbage-collected.  After close() has been called, you may call
	   this.initialize(...) to safely create a new <div> element (and color picker)
	   within this object again. */
	close: function() {
		if (!this.div) return false;

		if (this.options.onClosing)
			this.options.onClosing(this, 'closing');

		if (this.options.closeButton)
			Event.stopObserving(this.img_close);

		Event.stopObserving(this.r_edit);
		Event.stopObserving(this.g_edit);
		Event.stopObserving(this.b_edit);
		Event.stopObserving(this.hue_edit);
		Event.stopObserving(this.sat_edit);
		Event.stopObserving(this.brt_edit);

		Event.stopObserving(this.img_wheel_black);
		Event.stopObserving(this.img_bar_upper);
		Event.stopObserving(this.img_palette);

		Event.stopObserving(this.img_wheel_dragger);
		Event.stopObserving(this.img_bar_dragger);

		if (this.listeners.input) {
			Event.stopObserving(this.input, 'keyup', this.listeners.input);
			Event.stopObserving(this.input, 'focus', this.listeners.input);
			Event.stopObserving(this.input, 'blur', this.listeners.input);
		}

		Event.stopObserving(document, "mousedown", this.closeClickHandler);
		Event.stopObserving(document, "keypress", this.keyPressHandler);
		this.div.remove();
		this.div = null;

		this.listeners = { };
		this.dblclk = { };

		if (this.input.type != "hidden" && !this.input.disabled)
			this.input.focus();

		if (this.options.onClosed)
			this.options.onClosed(this, 'closed');
	},


	/*------------------------------------------------------------------------------------------
	**  Updating the view (edit boxes) when the model (internal data) changes.
	*/

	updateState: { },
	updateTimeout: false,

	/* This wrapper around update (below) is a hack for IE that delays and queues
	   high-speed updates to avoid overloading the browser's page renderer. */
	queuedUpdate: function(mode, color, sources) {
		if (!Prototype.Browser.IE)
			this.update(mode, color, sources);
		else {
			var t = this;
			t.updateState = { mode:mode, color:color, sources:sources };
			if (t.updateTimeout == false) {
				t.updateTimeout = setTimeout(function() {
					t.updateTimeout = false;
					t.update(t.updateState.mode, t.updateState.color, t.updateState.sources);
				}, 25);
			}
		}
	},

	/* This concludes a series of high-speed updates. */
	finalUpdate: function() {
		if (this.updateTimeout) {
			clearTimeout(this.updateTimeout);
			this.updateTimeout = false;
			this.update(this.updateState.mode, this.updateState.color, this.updateState.sources);
			this.updateState = { };
		}
	},

	/* When one of the input fields changes, update all of the others to
	   reflect the same state (but take care not to update the originating
	   field). */
	update: function(mode, color, sources) {

		/* Carefully ensure that all of r, g, b, hue, sat, and brt are members
		   of 'color', and that all of their values are numbers. */
		if (typeof(color) != 'object')
			color = { r:0, g:0, b:0, hue:0, sat:0, brt:0 };
		color.r = this.toNumber(color.r);
		color.g = this.toNumber(color.g);
		color.b = this.toNumber(color.b);
		color.hue = this.toNumber(color.hue);
		color.sat = this.toNumber(color.sat);
		color.brt = this.toNumber(color.brt);

		/* Determine both the RGB and HSB values from our given color. */
		var rgb, hsb;
		if (mode == 'rgb') {
			rgb = color;
			hsb = this.RGBtoHSB(rgb);
		}
		else if (mode == 'hsb') {
			hsb = color;
			rgb = this.HSBtoRGB(hsb);
		}

		if (rgb.r < 0) rgb.r = 0;
		if (rgb.r > 255) rgb.r = 255;
		if (rgb.g < 0) rgb.g = 0;
		if (rgb.g > 255) rgb.g = 255;
		if (rgb.b < 0) rgb.b = 0;
		if (rgb.b > 255) rgb.b = 255;

		hsb.hue = hsb.hue % 360;
		if (hsb.hue < 0) hsb.hue += 360;
		if (hsb.sat < 0) hsb.sat = 0;
		if (hsb.sat > 100) hsb.sat = 100;
		if (hsb.brt < 0) hsb.brt = 0;
		if (hsb.brt > 100) hsb.brt = 100;

		/* Determine which sources we're *not* going to update visually. */
		source = { };
		sources.each(function(s) { source[s] = true; });

		/* Positional representations allow fractional RGB or HSB values, so do those first. */

		var t = this;
		if (!source.wheel) {
			var o = hsb.brt / 100;
			if (o > 0.9999) o = 0.9999;	/* Make sure we can still collect events */

			t.img_wheel_black.setOpacity(1.0 - o);

			var angle = ((hsb.hue + 270) % 360) * (Math.PI * 2 / 360);
			var c = Math.cos(angle);
			var s = Math.sin(angle);
			var x = Math.floor(c * hsb.sat * t.wheel.width / 200 + 0.5) + t.wheel.left + ((t.wheel.width-4)/2) + 2;
			var y = Math.floor(s * hsb.sat * t.wheel.height / 200 + 0.5) + t.wheel.top + ((t.wheel.height-4)/2) + 2;
			t.img_wheel_dragger.setStyle({ left:x - Math.floor(t.wheelsel.width/2) + "px",
				top:y - Math.floor(t.wheelsel.height/2) + "px" });
		}
		if (!source.bar) {
			var base_hue = Math.floor(hsb.hue / 60);
			var next_hue = (base_hue + 1) % 6;
			t.img_bar_lower.setStyle({ backgroundPosition:(-base_hue * 20 - 20) + "px 0px" });
			t.img_bar_middle.setStyle({ backgroundPosition:(-next_hue * 20 - 20) + "px 0px" });
			t.img_bar_middle.setOpacity((hsb.hue - base_hue * 60) / 60);

			var o = (100 - hsb.sat) / 100;
			if (o < 0.0001) o = 0.0001;	/* Make sure we can still collect events */
			t.img_bar_upper.setOpacity(o);

			t.img_bar_dragger.setStyle({ top:t.bar.top - Math.floor(t.barsel.height/2)
				+ Math.floor((100-hsb.brt) * t.bar.height/100+0.5) + "px",
				left:t.bar.left+Math.floor(t.bar.width-t.barsel.width)/2 + "px" });
		}

		/* Non-positional representations require integer RGB or HSB values, so clean up
		   any fractional bits. */
		rgb.r = Math.floor(rgb.r + 0.5);
		rgb.g = Math.floor(rgb.g + 0.5);
		rgb.b = Math.floor(rgb.b + 0.5);
		hsb.hue = Math.floor(hsb.hue + 0.5);
		hsb.sat = Math.floor(hsb.sat + 0.5);
		hsb.brt = Math.floor(hsb.brt + 0.5);
		hsb.hue = hsb.hue % 360;

		/* Compute this.color, which is always the color in CSS-friendly six-digit hex form. */
		this.color = '#' + (rgb.r.toColorPart() + rgb.g.toColorPart() + rgb.b.toColorPart()).toUpperCase();

		if (!source.rgb) {
			this.r_edit.value = rgb.r;
			this.g_edit.value = rgb.g;
			this.b_edit.value = rgb.b;
		}
		if (!source.hsb) {
			this.hue_edit.value = hsb.hue;
			this.sat_edit.value = hsb.sat;
			this.brt_edit.value = hsb.brt;
		}
		
		/* If desired, update the input field. */
		if (this.options.input) {
			var input = $(this.options.input);
			if (!source.input) {
				var value = this.internalFormatOutput(rgb, hsb, this.options.outputFormat);
				if (input.tagName == 'INPUT')
					input.value = value;
				else input.innerHTML = value;
			}
			if (this.options.showInField) {
				var tc = this.computeTextColor(rgb);
				input.setStyle({ backgroundColor:this.color,
					color: '#' + tc.r.toColorPart() + tc.g.toColorPart() + tc.b.toColorPart() });
			}
		}
	},


	/*------------------------------------------------------------------------------------------
	**  Various data converters, encoders, and decoders.
	*/

	/* Safely convert an unknown chunk of data to a decimal value, even if it's a string with
	   all sorts of weird characters in it. */
	toNumber: function(x) {
		switch (typeof x) {
		case 'number':
			return x;
		case 'string':
			if (matches = /^[^0-9.+-]*([+-]?(?:[0-9]*\.[0-9]+|[0-9]+(?:\.[0-9]*)?))(?:[^0-9]|$)/.exec(x))
				return Number(matches[1]);
			else return 0;
		case 'boolean':
			return x ? 1 : 0;
		case 'object':
			return x ? 1 : 0;
		case 'function':
			return 1;
		default:
		case 'undefined':
			return 0;
		}
	},

	/* Format a color, specified as an arbitrary hex string, using the given formatting
	   string, as defined in the "formatOutput" option for ProColor. */
	formatOutput: function(color_string, format) {
		if (!format) format = "#{RR}{GG}{BB}";
		var rgb = this.decodeHexColor(color_string);
		if (!rgb) rgb = { r:0, g:0, b:0 };
		return this.internalFormatOutput(rgb, this.RGBtoHSB(rgb), format);
	},

	/* Format a color, specified as an RGB object and an HSB object, using the given formatting
	   string, as defined in the "formatOutput" option for ProColor. */
	internalFormatOutput: function(rgb, hsb, format) {
		/* The previous version of this used String.split(), but String.split() is
		   badly broken on IE, and we don't want to include extra libraries to fix
		   it, so instead we use some cleverness with String.match() here, which
		   *does* work on IE. */
		var pieces = format.match(/(\{\w+\}|[^{]+)/g);
		var output = '';

		pieces.each(function(piece) {
			var result;
			switch (piece) {
			case '{RR}': result = rgb.r.toColorPart().toUpperCase(); break;
			case '{GG}': result = rgb.g.toColorPart().toUpperCase(); break;
			case '{BB}': result = rgb.b.toColorPart().toUpperCase(); break;
			case '{rr}': result = rgb.r.toColorPart().toLowerCase(); break;
			case '{gg}': result = rgb.g.toColorPart().toLowerCase(); break;
			case '{bb}': result = rgb.b.toColorPart().toLowerCase(); break;
			case '{R}': result = this.halfColorPart(rgb.r).toUpperCase(); break;
			case '{G}': result = this.halfColorPart(rgb.g).toUpperCase(); break;
			case '{B}': result = this.halfColorPart(rgb.b).toUpperCase(); break;
			case '{r}': result = this.halfColorPart(rgb.r).toLowerCase(); break;
			case '{g}': result = this.halfColorPart(rgb.g).toLowerCase(); break;
			case '{b}': result = this.halfColorPart(rgb.b).toLowerCase(); break;
			case '{red}': result = rgb.r.toString(); break;
			case '{grn}': result = rgb.g.toString(); break;
			case '{blu}': result = rgb.b.toString(); break;
			case '{hue}': result = hsb.hue.toString(); break;
			case '{sat}': result = hsb.sat.toString(); break;
			case '{brt}': result = hsb.brt.toString(); break;
			default: result = piece; break;
			}
			output += result;
		});

		return output;
	},

	/* Take a standard 0-255 RGB value and convert it to its single-digit-hex pseudo-equivalent.
	   CSS allows us to specify colors as #369, for example, so this function can be used to pack
	   a truecolor 24-bit color value down to a three-digit 12-bit version like that. */
	halfColorPart: function(v) {
		return Math.floor((v + 8) / 17).toString(16);
	},

	/* Given a hex color represented in a string, convert that to an RGB object.  This does
	   its best to decode colors no matter how weird they are, skipping over non-hex characters
	   and accepting inputs of varying length.  Usually, it returns an RGB object, but if it
	   absolutely positively cannot convert the input string to RGB, it will return false. */
	decodeHexColor: function(string) {
		var matches;

		/* Six-to-eight hex values.  Treat as RRGGBB, RRGGBBA, or RRGGBBAA. */
		if (matches = /^[^0-9A-Fa-f]*([0-9A-Fa-f]{2})([0-9A-Fa-f]{2})([0-9A-Fa-f]{2})([0-9A-Fa-f]{0,2})(?:[^0-9A-Fa-f]|$)/.exec(string))
			return { r:parseInt(matches[1], 16), g:parseInt(matches[2], 16), b:parseInt(matches[3],16) };

		/* Five hex values.  Treat as RRGGB. */
		if (matches = /^[^0-9A-Fa-f]*([0-9A-Fa-f]{2})([0-9A-Fa-f]{2})([0-9A-Fa-f])(?:[^0-9A-Fa-f]|$)/.exec(string)) {
			var b = parseInt(matches[3], 16);
			return { r:parseInt(matches[1], 16), g:parseInt(matches[2], 16), b:b*16+b };
		}

		/* Four hex values.  Treat as RRGG, B=G. */
		if (matches = /^[^0-9A-Fa-f]*([0-9A-Fa-f]{2})([0-9A-Fa-f]{2})(?:[^0-9A-Fa-f]|$)/.exec(string)) {
			var g = parseInt(matches[2], 16);
			return { r:parseInt(matches[1], 16), g:g, b:g };
		}

		/* Three hex values.  Treat as RGB. */
		if (matches = /^[^0-9A-Fa-f]*([0-9A-Fa-f])([0-9A-Fa-f])([0-9A-Fa-f])([0-9A-Fa-f]{0,2})(?:[^0-9A-Fa-f]|$)/.exec(string)) {
			var r = parseInt(matches[1], 16);
			var g = parseInt(matches[2], 16);
			var b = parseInt(matches[3], 16);
			return { r:r*16+r, g:g*16+g, b:b*16+b };
		}

		/* Two hex values.  Treat as 8-bit grayscale. */
		if (matches = /^[^0-9A-Fa-f]*([0-9A-Fa-f]{2})(?:[^0-9A-Fa-f]|$)/.exec(string)) {
			var g = parseInt(matches[1], 16);
			return { r:g, g:g, b:g };
		}

		/* One hex value.  Treat as 4-bit grayscale. */
		if (matches = /^[^0-9A-Fa-f]*([0-9A-Fa-f])(?:[^0-9A-Fa-f]|$)/.exec(string)) {
			var g = parseInt(matches[1], 16);
			g = g * 16 + g;
			return { r:g, g:g, b:g };
		}

		/* Zero hex values, or more than eight:  Just unknown garbage. */
		return false;
	},

	/* Compute the "true" brightness of a color, taking into account typical human-eye
	   brightness perception of the three primary colors. */
	trueBrightness: function(rgb) {
		return (rgb.r / 255 * 0.30) + (rgb.g / 255 * 0.59) + (rgb.b / 255 * 0.11);
	},

	/* Compute a text color given a background color.  This attempts to find a text color
	   that is reasonably legible against the given background color, and it usually finds
	   a reasonably tolerable one. */
	computeTextColor: function(rgb) {
		var brt = this.trueBrightness(rgb);
		if (brt < 0.5) {
			var m = Math.floor(brt * 20) + 3;
			var im = 255 * (m - 1);
		}
		else {
			var m = Math.floor((1.0 - brt) * 20) + 3;
			var im = 0;
		}
		return { r:Math.floor((rgb.r+im)/m), g:Math.floor((rgb.g+im)/m), b:Math.floor((rgb.b+im)/m) };
	},

	/* Convert an RGB object to a HSB object.  The HSB object may have fractional values in it.
	   The input RGB object is safely converted to numbers and rounded to integers before
	   conversion to HSB. */
	RGBtoHSB: function(rgb) {
		var r = Math.floor(this.toNumber(rgb.r) + 0.5);
		var g = Math.floor(this.toNumber(rgb.g) + 0.5);
		var b = Math.floor(this.toNumber(rgb.b) + 0.5);
		if (r < 0) r = 0;
		if (r > 255) r = 255;
		if (g < 0) g = 0;
		if (g > 255) g = 255;
		if (b < 0) b = 0;
		if (b > 255) b = 255;

		var max, delta, diff, offset;
		var h = r, s = g, v = b;

		if (r > g) {
			if (r > b) v = max = r, offset =   0, diff = g - b;
			else       v = max = b, offset = 240, diff = r - g;
			delta = max - ((g < b) ? g : b);
		}
		else {
			if (g > b) v = max = g, offset = 120, diff = b - r;
			else       v = max = b, offset = 240, diff = r - g;
			delta = max - ((r < b) ? r : b);
		}

		if (max != 0) s = Math.floor((delta * 100) / max + 0.5);
		else s = 0;

		if (s != 0) {
			h = (offset + Math.floor(diff * 120 / (delta * 2) + 0.5)) % 360;
			if (h < 0) h += 360;
		}
		else h = 0;
		
		v = Math.floor(v * 100 / 255 + 0.5);
		
		return { hue:h, sat:s, brt:v };
	},

	/* Convert a HSB object to an RGB object.  The HSB object may have fractional values in it.
	   The input HSB object is safely converted to numbers before conversion to RGB, but is not
	   rounded to integers, which yields a more accurate conversion.  The RGB values, however,
	   will be integers, and limited to 0-255 each. */
	HSBtoRGB: function(hsb) {
		var v = this.toNumber(hsb.brt);
		if (v < 0) v = 0;
		if (v > 100) v = 100;
		v = v * 255 / 100;

		var s = this.toNumber(hsb.sat);
		if (s <= 0) {
			v = Math.floor(v + 0.5);
			return { r:v, g:v, b:v };
		}
		if (s > 100) s = 100;

		var h = this.toNumber(hsb.hue);
		h = h % 360;
		if (h < 0) h += 360;

		var vs = v * s / 100;
		var vsf = (vs * ((h * 256 / 60) % 256)) / 256;

		var r, g, b;
		switch (Math.floor(h / 60)) {
		case 0: r = v;        g = v-vs+vsf; b = v-vs;     break;
		case 1: r = v-vsf;    g = v;        b = v-vs;     break;
		case 2: r = v-vs;     g = v;        b = v-vs+vsf; break;
		case 3: r = v-vs;     g = v-vsf;    b = v;        break;
		case 4: r = v-vs+vsf; g = v-vs;     b = v;        break;
		case 5: r = v;        g = v-vs;     b = v-vsf;    break;
		}

		r = Math.floor(r + 0.5);
		g = Math.floor(g + 0.5);
		b = Math.floor(b + 0.5);
		if (r < 0) r = 0;
		if (r > 255) r = 255;
		if (g < 0) g = 0;
		if (g > 255) g = 255;
		if (b < 0) b = 0;
		if (b > 255) b = 255;

		return { r:r, g:g, b:b };
	},


	/*------------------------------------------------------------------------------------------
	**  Supporting functions for the event handlers (controllers).
	*/

	/* When the user has hit Enter or double-clicked, accept the color and close the popup. */
	acceptAndClose: function() {
		if (this.options.onAcceptClick)
			this.options.onAcceptClick(this, 'acceptclick');
		if (this.options.mode == 'popup')
			this.close();
	},

	/* When the user has hit Esc or clicked outside the popup, close the popup. */
	cancelAndClose: function() {
		if (this.options.onCancelClick)
			this.options.onCancelClick(this, 'cancelclick');
		if (this.options.mode == 'popup')
			this.close();
	},

	/* Because of the way we capture mouse events for tracking, we can't use normal
	   'dblclick' events, because the browser will never fire them.  So we use
	   simulated 'dblclick' testing here to achieve a similar result. */
	closeOnDoubleClick: function(e) {
		var x = e.pointerX(), y = e.pointerY();
		var date = new Date;
		var time = date.getTime();

		if (Math.abs(this.dblclk.x - x) < 3
			&& Math.abs(this.dblclk.y - y) < 3
			&& time - this.dblclk.time <= 500) {
			this.dblclk.time = 0;
			this.acceptAndClose();
			return true;
		}
		else {
			this.dblclk.x = x;
			this.dblclk.y = y;
			this.dblclk.time = time;
			return false;
		}
	},

	/* Compute the brightness given a mouse click on the given *absolute* coordinate
	   of the brightness slider.  Returns a simple integer, 0 to 100. */
	brtFromPoint: function(x, y) {
		var d = this.img_bar_upper.getDimensions();
		var p = this.img_bar_upper.cumulativeOffset();
		if (y < p.top) return 100;
		else if (y >= p.top + d.height) return 0;
		else return ((p.top + d.height - 1 - y) * 100 / d.height);
	},

	/* Compute the hue (angle) and saturation (radius) from the given *absolute*
	   coordinate of the wheel.  Returns a partial hsb object (with no brightness value). */
	hueSatFromPoint: function(x, y) {
		var d = this.img_wheel_rgb.getDimensions();
		var p = this.img_wheel_rgb.cumulativeOffset();
		var dy = (y - (p.top + d.height / 2)) / ((d.height - 4) / 2);
		var dx = (x - (p.left + d.width / 2)) / ((d.width - 4) / 2);
		var sat = Math.sqrt(dy * dy + dx * dx) * 100 + 0.5;
		var hue = Math.atan2(dy, dx) * 180 / Math.PI + 90;
		if (sat <= 0) hue = sat = 0;
		if (sat > 100) sat = 100;
		if (hue < 0) hue += 360;
		return { sat:sat, hue:hue };
	},

	/* Given pixel coordinates of the mouse, return the palette color that lies
	   under those coordinates.  If the mouse lies outside the palette, return false. */
	colorFromPalette: function(x, y) {
		var d = this.img_palette.getDimensions();
		var p = this.img_palette.cumulativeOffset();

		if (x < p.left || x >= p.left + d.width)
			return false;
		x = Math.floor((x - p.left) / 11);

		if (y >= p.top && y < p.top + 12)
			line = "000333666999CCCFFF";
		else if (y > p.top + 20 && y < p.top + 20 + this.palette.length * 11)
			line = this.palette[Math.floor((y - (p.top + 20)) / 11)];
		else return false;
		
		var r = parseInt(line.substr(x*3  , 1), 16);
		var g = parseInt(line.substr(x*3+1, 1), 16);
		var b = parseInt(line.substr(x*3+2, 1), 16);

		return { r: r*16 + r, g: g*16 + g, b: b * 16 + b };
	},

	/* Perform an update for input from an edit box, correctly updating only the edit
	   boxes that weren't changed by the user.  'mode' is either 'rgb' or 'hsb', and
	   determines which edit boxes the user changed. */
	updateByMode: function(mode) {
		this.update(mode, {
			r:this.r_edit.value, g:this.g_edit.value, b:this.b_edit.value,
			hue:this.hue_edit.value, sat:this.sat_edit.value, brt:this.brt_edit.value
		}, [mode]);
	},


	/*------------------------------------------------------------------------------------------
	**  Event handlers (controllers) for the various input widgets, and their
	**  supporting functions.
	*/

	/* Handle events on the close button (X button) */
	onCloseEvent: function(e) {
		switch (e.type) {
		case 'keydown':
			switch (e.keyCode) {
			case Event.KEY_RETURN:
			case 32:
				if (this.options.onCloseButton)
					if (!this.options.onCloseButton(this, 'closebutton'))
						break;
				if (this.options.mode == 'popup')
					this.close();
				break;
			}
			break;
		case 'mouseover':
			if (!this.img_close.trackingMouse)
				this.img_close.setStyle({ backgroundPosition:"0px -16px" });
			break;
		case 'mouseout':
			if (!this.img_close.trackingMouse)
				this.img_close.setStyle({ backgroundPosition:"0px 0px" });
			break;
		case 'mousedown':
			this.img_close.trackingMouse = true;
			this.img_close.setStyle({ backgroundPosition:"0px -32px" });
			e.stop();

			if (this.img_close.focus)
				this.img_close.focus();

			var over = true;
			var capture = new MouseCapture;
			capture.setCursor('default');

			capture.begin((function(event, type) {
				switch (type) {
				case 'mouseup':
				case 'keyup':
					capture.end();
					this.img_close.trackingMouse = false;
					if (!over)
						this.img_close.setStyle({ backgroundPosition:"0px 0px" });
					else {
						this.img_close.setStyle({ backgroundPosition:"0px -16px" });
						if (this.options.onCloseButton)
							if (!this.options.onCloseButton(this, 'closebutton'))
								break;
						if (this.options.mode == 'popup')
							this.close();
					}
					break;
				case 'mousemove':
					over = (this.img_close.getStyle('backgroundPosition') == '0px -32px');
					if (this.img_close.isEventIn(event) != over) {
						over = !over;
						if (!over)
							this.img_close.setStyle({ backgroundPosition:"0px 0px" });
						else this.img_close.setStyle({ backgroundPosition:"0px -32px" });
					}
					break;
				}
			}).bind(this));
			break;
		}
	},

	/* Handle events on the brightness slider. */
	onBarEvent: function(e) {
		switch (e.type) {

		case 'keydown':
			var t = this;
			var brt = this.toNumber(t.brt_edit.value), oldbrt = brt;
			var shiftKey = e.shiftKey || e.ctrlKey || e.altKey;
			switch (e.keyCode) {
			case Event.KEY_UP:
				brt += shiftKey ? 10 : 1;
				e.stop();
				break;
			case Event.KEY_DOWN:
				brt -= shiftKey ? 10 : 1;
				e.stop();
				break;
			case Event.KEY_PAGEUP:
				brt += shiftKey ? 25 : 10;
				e.stop();
				break;
			case Event.KEY_PAGEDOWN:
				brt -= shiftKey ? 25 : 10;
				e.stop();
				break;
			case Event.KEY_HOME:
				brt = 100;
				e.stop();
				break;
			case Event.KEY_END:
				brt = 0;
				e.stop();
				break;
			}
			if (brt < 0) brt = 0;
			if (brt > 100) brt = 100;
			if (t.options.onChanging && (oldbrt != brt))
				t.options.onChanging(t, 'changing');
			t.update('hsb', { hue:t.hue_edit.value, sat:t.sat_edit.value, brt:brt }, []);
			if (t.options.onChanged && (oldbrt != brt))
				t.options.onChanged(t, 'changed');
			break;

		case 'mousedown':
			e.stop();
			var t = this;
			t.oldcolor = t.color;

			if (t.img_bar_dragger.focus)
				t.img_bar_dragger.focus();

			var do_update = function(e) {
				var hsb = {
					hue: t.hue_edit.value,
					sat: t.sat_edit.value,
					brt: t.brtFromPoint(e.pointerX(), e.pointerY())
				};
				t.queuedUpdate('hsb', hsb, []);
				if (t.options.onChanging)
					t.options.onChanging(t, 'changing');
			};
			do_update(e);

			if (this.closeOnDoubleClick(e))
				break;

			var capture = new MouseCapture;
			capture.setCursor('default');

			capture.begin(function(event, type) {
				switch (type) {
				case 'mouseup':
				case 'keyup':
					capture.end();
					t.finalUpdate();
					if (t.options.onChanged && t.oldcolor != t.color)
						t.options.onChanged(t, 'changed');
					break;
				case 'mousemove':
					do_update(event);
					break;
				}
			});
			break;
		}
	},

	/* Handle events on the color wheel. */
	onWheelEvent: function(e) {
		switch (e.type) {

		case 'keydown':
			var t = this;
			var hue = this.toNumber(t.hue_edit.value), oldhue = hue;
			var sat = this.toNumber(t.sat_edit.value), oldsat = sat;
			var shiftKey = e.shiftKey || e.ctrlKey || e.altKey;
			switch (e.keyCode) {
			case Event.KEY_UP:
				sat += shiftKey ? 10 : 1;
				e.stop();
				break;
			case Event.KEY_DOWN:
				sat -= shiftKey ? 10 : 1;
				e.stop();
				break;
			case Event.KEY_LEFT:
				hue -= shiftKey ? 10 : 1;
				e.stop();
				break;
			case Event.KEY_RIGHT:
				hue += shiftKey ? 10 : 1;
				e.stop();
				break;
			case Event.KEY_PAGEUP:
				sat += shiftKey ? 25 : 10;
				e.stop();
				break;
			case Event.KEY_PAGEDOWN:
				sat -= shiftKey ? 25 : 10;
				e.stop();
				break;
			case Event.KEY_HOME:
				sat = 100;
				e.stop();
				break;
			case Event.KEY_END:
				sat = 0;
				e.stop();
				break;
			}
			if (sat < 0) sat = 0;
			if (sat > 100) sat = 100;
			hue = hue % 360;
			if (hue < 0) hue += 360;
			if (t.options.onChanging && (oldhue != hue || oldsat != sat))
				t.options.onChanging(t, 'changing');
			t.update('hsb', { hue:hue, sat:sat, brt:t.brt_edit.value }, []);
			if (t.options.onChanged && (oldhue != hue || oldsat != sat))
				t.options.onChanged(t, 'changed');
			break;

		case 'mousedown':
			e.stop();
			var t = this;
			t.oldcolor = t.color;

			if (t.img_wheel_dragger.focus)
				t.img_wheel_dragger.focus();

			var do_update = function(e) {
				var hsb = t.hueSatFromPoint(e.pointerX(), e.pointerY());
				hsb.brt = t.brt_edit.value;
				t.queuedUpdate('hsb', hsb, []);
				if (t.options.onChanging)
					t.options.onChanging(t, 'changing');
			};
			do_update(e);

			if (this.closeOnDoubleClick(e))
				break;

			var capture = new MouseCapture;
			capture.setCursor('default');

			capture.begin(function(event, type) {
				switch (type) {
				case 'mouseup':
				case 'keyup':
					capture.end();
					t.finalUpdate();
					if (t.options.onChanged && t.oldcolor != t.color)
						t.options.onChanged(t, 'changed');
					break;
				case 'mousemove':
					do_update(event);
					break;
				}
			});
			break;
		}
	},

	/* Handle events on the color palette. */
	onPaletteEvent: function(e) {
		switch (e.type) {

		case 'mousedown':
			var t = this;
			t.oldcolor = t.color;

			if (t.img_palette.focus)
				t.img_palette.focus();

			var do_update = function(e) {
				var rgb = t.colorFromPalette(e.pointerX(), e.pointerY());
				if (rgb)
					t.queuedUpdate('rgb', rgb, []);
				if (t.options.onChanging)
					t.options.onChanging(t, 'changing');
				return rgb;
			};
			if (!do_update(e)) break;

			e.stop();

			if (this.closeOnDoubleClick(e))
				break;

			var capture = new MouseCapture;
			capture.setCursor('default');

			capture.begin(function(event, type) {
				switch (type) {
				case 'mouseup':
				case 'keyup':
					capture.end();
					t.finalUpdate();
					if (t.options.onChanged && t.oldcolor != t.color)
						t.options.onChanged(t, 'changed');
					break;
				case 'mousemove':
					do_update(event);
					break;
				}
			});
			break;
		}
	},

	/* Handle events in one of the numeric input boxes on the right. */
	onNumberBox: function(event, mode, min, max, mod) {
		var element = Event.element(event);

		switch (event.type) {

		case 'keypress':
			/* Only allow numbers to be typed into the input field.  This is more complex
			   than it sounds, because the browsers do *not* agree on what events appear
			   for keypresses, so we have to handle multiple "standards" here. */
			if (!event || event.ctrlKey || event.altKey) {
				/* Broken event, or ctrl/alt being held down. */
				event.stop();
			}
			else if ((event.charCode >= 0x30 && event.charCode <= 0x39)
				|| (event.keyCode >= 0x30 && event.keyCode <= 0x39)) {
				/* A number, as either a charCode (Firefox) or as a keyCode (IE). */
			}
			else if (event.keyCode > 0 && event.keyCode < 48) {
				/* A control key, like an arrow key or tab. */
			}
			else {
				/* All other unknown keys are rejected. */
				event.stop();
			}
			break;

		case 'keyup':
			if (event.keyCode != Event.KEY_TAB) {
				this.updateByMode(mode);
				if (this.options.onChanging)
					this.options.onChanging(this, 'changing');
			}
			if (event.keyCode == Event.KEY_RETURN) {
				this.acceptAndClose();
				event.stop();
			}
			if (event.keyCode == Event.KEY_ESC) {
				this.cancelAndClose();
				event.stop();
			}
			break;

		case 'focus':
			this.oldcolor = this.color;
			break;

		case 'blur':
			var v = this.toNumber(element.value);
			if (v < min || v > max) {
				if (mod) {
					v %= mod;
					if (v < 0) v += mod;
				}
				else if (v < min) v = min;
				else v = max;
				element.value = v;
				this.updateByMode(mode);
			}
			if (this.options.onChanged && this.oldcolor != this.color)
				this.options.onChanged(this, 'changed');
			break;
		}
	},

	/* Handle events in the primary formatted-text input field. */
	onInput: function(event) {
		switch (event.type) {
		case 'keyup':
			if (event.keyCode != Event.KEY_TAB) {
				this.update('rgb', this.decodeHexColor(this.input.value), ['input']);
				if (this.options.onChanging)
					this.options.onChanging(this, 'changing');
			}
			if (event.keyCode == Event.KEY_RETURN) {
				this.acceptAndClose();
				event.stop();
			}
			if (event.keyCode == Event.KEY_ESC) {
				this.cancelAndClose();
				event.stop();
			}
			break;
		case 'focus':
			this.oldcolor = this.color;
			break;
		case 'blur':
			var rgb = this.decodeHexColor(this.input.value);
			this.update('rgb', rgb, []);
			if (this.oldcolor != this.color) {
				this.input.value = this.internalFormatOutput(rgb, this.RGBtoHSB(rgb),
					this.options.outputFormat);
				if (this.options.onChanged)
					this.options.onChanged(this, 'changed');
			}
			break;
		}
	},


	/*------------------------------------------------------------------------------------------
	**  Safety closing methods:  These let the user click outside the popup or hit Escape
	**  to close it.
	*/

	/* Handle clicks outside the popup. */
	handleCloseClick: function(e) {
		var elem = $(Event.element(e));
		if (elem == this.div || elem.descendantOf(this.div)) return;
		if (elem == this.input || elem.descendantOf(this.input)) return;
		this.cancelAndClose();
	},

	/* Handle key presses that would close the popup. */
	handleKeyPress: function(e) {
		if (e.keyCode == Event.KEY_ESC)
			this.cancelAndClose();
	},

	/*------------------------------------------------------------------------------------------
	**  Special factory method:  This creates a popup button immediately after the given
	**  element, and sets up a watch on that element so that if the element changes, its
	**  colors will be updated accordingly even if the popup is not open.
	*/

	attachButton: function(e, options) {
		e = $(e);
		if (!e) return;

		var imgPath;
		if (options.imgPath) imgPath = options.imgPath;
		else imgPath = "";

		var button = $(document.createElement('a'));
		button.setStyle({ display:'inline-block', visibility:'visible',
			border:'0px', textDecoration:'none', verticalAlign:'bottom',
			width:'40px', height:'24px', padding:'0px', marginLeft:'2px',
			backgroundImage:'url(' + imgPath + 'drop.png)', backgroundPosition:'0px 0px',
			backgroundRepeat:'no-repeat', cursor:'default' });
		button.href = '#';

		for (var m in this.buttonMembers)
			button[m] = this.buttonMembers[m];

		var color = options.color;

		button.buttonHandler = button.eventHandler.bindAsEventListener(button);
		button.options = Object.clone(options);
		button.options.input = e;
		button.options.pc = this;
		delete button.options.color;
		if (!button.options.outputFormat)
			button.options.outputFormat = '#{RR}{GG}{BB}';

		Event.observe(button, "click", button.buttonHandler);
		Event.observe(button, "mouseover", button.buttonHandler);
		Event.observe(button, "mouseout", button.buttonHandler);
		Event.observe(button, "mousedown", button.buttonHandler);
		Event.observe(button, "mouseup", button.buttonHandler);
		Event.observe(button, "keydown", button.buttonHandler);
		Event.observe(button, "keyup", button.buttonHandler);

		if (e.nextSibling)
			e.parentNode.insertBefore(button, e.nextSibling);
		else e.parentNode.appendChild(button);

		button.inputHandler = button.onInput.bindAsEventListener(button);
		button.observeInput();

		if (color) e.value = color;
		button.colorSync(true);

		return button;
	},

	buttonMembers: {
		pressed: false,
		hovered: false,
		inputHandler: false,
		options: { },

		setImg: function(n) {
			this.setStyle({ backgroundPosition:'0px ' + (n*-24) + 'px' });
		},

		observeInput: function() {
			Event.observe(this.options.input, 'keyup', this.inputHandler);
			Event.observe(this.options.input, 'focus', this.inputHandler);
			Event.observe(this.options.input, 'blur', this.inputHandler);
		},

		stopObservingInput: function() {
			Event.stopObserving(this.options.input, 'keyup', this.inputHandler);
			Event.stopObserving(this.options.input, 'focus', this.inputHandler);
			Event.stopObserving(this.options.input, 'blur', this.inputHandler);
		},

		toggle: function() {
			if (this.pressed) {
				this.setImg(this.hovered ? 1 : 0);
				this.pressed = false;
				if (this.popup) {
					this.popup.close();
					this.popup = null;
				}
				this.observeInput();
			}
			else {
				var t = this;
				this.setImg(2);
				this.pressed = true;
				this.stopObservingInput();
				this.popup = new ProColor(Object.extend(Object.clone(this.options), {
					mode: 'popup',
					closeButton: true,
					onClosed: function(p, a) {
						t.popup = null;
						t.setImg(this.hovered ? 1 : 0);
						t.pressed = false;
						t.observeInput();
						if (t.options.onClosed)
							t.options.onClosed(p, a);
					},
					parent: null
				}));
			}
		},

		colorSync: function(update_value) {
			var rgb = this.options.pc.decodeHexColor(this.options.input.value);
			if (rgb) {
				if (this.options.showInField) {
					var tc = this.options.pc.computeTextColor(rgb);
					this.options.input.setStyle({
						backgroundColor: '#' + rgb.r.toColorPart() + rgb.g.toColorPart() + rgb.b.toColorPart(),
						color: '#' + tc.r.toColorPart() + tc.g.toColorPart() + tc.b.toColorPart()
					});
				}
				var newvalue = this.options.pc.internalFormatOutput(rgb,
					this.options.pc.RGBtoHSB(rgb), this.options.outputFormat);
				if (newvalue != this.options.input.value && this.options.onChanging)
						this.options.onChanging(this.options.input, 'changing');
				if (update_value) {
					if (newvalue != this.options.input.value) {
						this.options.input.value = newvalue;
						if (this.options.onChanged)
							this.options.onChanged(this.options.input, 'changed');
					}
				}
			}
		},

		onInput: function(event) {
			switch (event.type) {
			case 'keyup':
				if (event.keyCode != Event.KEY_TAB && this.options.showInField)
					this.colorSync(false);
				break;
			case 'focus':
			case 'blur':
				this.colorSync(true);
				break;
			}
		},

		eventHandler: function(event) {
			switch (event.type) {
			case 'click':
				event.stop();
				break;
			case 'keydown':
				switch (event.keyCode) {
				case Event.KEY_RETURN:
				case 32:
					this.toggle();
					event.stop();
					break;
				}
				break;
			case 'keyup':
				break;
			case 'mousedown':
				this.focus();
				/* We open the color-picker after a very short delay, because if we open
				   it immediately, the browser gets confused about the focus change and
				   closes it immediately as well.  So the delay gives just enough time for
				   this event to propagate the rest of the way and leave the browser in a
				   sensible state. */
				var t = this;
				setTimeout(function() { t.toggle(); }, 20);
				break;
			case 'mouseup':
				break;
			case 'mouseover':
				if (this.pressed)
					this.setImg(2);
				else this.setImg(1);
				this.hovered = true;
				break;
			case 'mouseout':
				if (this.pressed)
					this.setImg(2);
				else this.setImg(0);
				this.hovered = false;
				break;
			}
		}
	}

});


/*--------------------------------------------------------------------------------------------------
**  After the document has loaded, catch any <input> fields with a 'procolor' classname
**  and add dropdown buttons to them.  If you need to alter the default options used by this,
**  you can do so anywhere in your page by updating ProColor.prototype.options with your
**  desired options.
*/

Event.observe(window, 'load', function() {
	$$('input.procolor').each(function(e) {
		ProColor.prototype.attachButton(e, ProColor.prototype.options);
	});
});
