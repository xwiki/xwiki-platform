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
var Drag={
  "obj" : null,
  "init" : function(a, aRoot) {
    a.onmousedown = Drag.start;
    a.root = aRoot;
    if (isNaN(parseInt(a.root.style.left))) a.root.style.left = "0px";
    if (isNaN(parseInt(a.root.style.top))) a.root.style.top = "0px";
    a.root.onDragStart = new Function();
    a.root.onDragEnd = new Function();
    a.root.onDrag = new Function();
  },
  "start" : function(evt) {
    var obj = Drag.obj = this;
    evt = Drag.fixE(evt);
    var c = parseInt(obj.root.style.top);
    var d = parseInt(obj.root.style.left);
    obj.lastMouseX = evt.clientX;
    obj.lastMouseY = evt.clientY;
    obj.root.onDragStart(d, c, evt.clientX, evt.clientY);
    document.onmousemove = Drag.drag;
    document.onmouseup = Drag.end;
    return false;
  },
  "drag" : function(evt) {
    evt = Drag.fixE(evt);
    var obj = Drag.obj;
    var cy = evt.clientY;
    var cx = evt.clientX;
    var e = parseInt(obj.root.style.top);
    var f = parseInt(obj.root.style.left);
    var h,g;
    h = f + cx - obj.lastMouseX;
    g = e + cy - obj.lastMouseY;
    obj.root.style.left = h + "px";
    obj.root.style.top = g + "px";
    obj.lastMouseX = cx;
    obj.lastMouseY = cy;
    obj.root.onDrag(h, g, cy, cx);
    return false;
  },
  "end" : function() {
    document.onmousemove = null;
    document.onmouseup = null;
    Drag.obj.root.onDragEnd(parseInt(Drag.obj.root.style.left), parseInt(Drag.obj.root.style.top));
    Drag.obj = null;
  },
  "fixE":function(evt) {
    if (typeof evt == "undefined") evt = window.event;
    if (typeof evt.layerX == "undefined") evt.layerX = evt.offsetX;
    if (typeof evt.layerY == "undefined") evt.layerY = evt.offsetY;
    return evt;
  }
};
