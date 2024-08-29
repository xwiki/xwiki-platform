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
var offsetxpoint=-60; //Customize x offset of tooltip
var offsetypoint=20; //Customize y offset of tooltip
var ie=document.all
var ns6=document.getElementById && !document.all
var enabletip=false;
var tipobj=$("dhtmltooltip");
var tippedNode = undefined;

function ietruebody(){
  return (document.compatMode && document.compatMode!="BackCompat")? document.documentElement : document.body
}

function showtip(node, txt, w, align){
  Event.observe(document, "mousemove", positionTip);
  Event.observe(node, "mouseout", hideTip);
  if (align) {
    tipobj.style.textAlign=align;
  }
  tippedNode = node;
  tipobj.style.visibility = "hidden";
  tipobj.style.display = "block";
  tipobj.style.width = "auto";
  tipobj.innerHTML=txt;
  if (tipobj.offsetWidth > w) {
    tipobj.style.width = w + "px";
  }
  tipobj.style.display = "none";
  tipobj.style.visibility="visible";
  enabletip = true;
  return false;
}

function positionTip(e){
  if (enabletip){
    var curX=Event.pointerX(e);
    var curY=Event.pointerY(e);
    //Find out how close the mouse is to the corner of the window
    var rightedge=ie&&!window.opera? ietruebody().clientWidth-event.clientX-offsetxpoint : window.innerWidth-e.clientX-offsetxpoint-20
    var bottomedge=ie&&!window.opera? ietruebody().clientHeight-event.clientY-offsetypoint : window.innerHeight-e.clientY-offsetypoint-20

    var leftedge=(offsetxpoint<0) ? offsetxpoint*(-1) : -1000

    //if the horizontal distance isn't enough to accomodate the width of the context menu
    if (rightedge<tipobj.offsetWidth) {
      //move the horizontal position of the menu to the left by it's width
      tipobj.style.left=ie? ietruebody().scrollLeft+event.clientX-tipobj.offsetWidth+"px" : window.pageXOffset+e.clientX-tipobj.offsetWidth+"px"
    } else if (curX<leftedge) {
      tipobj.style.left="5px"
    } else {
      //position the horizontal position of the menu where the mouse is positioned
      tipobj.style.left=curX+offsetxpoint+"px"
          }
    //same concept with the vertical position
    if (bottomedge<tipobj.offsetHeight) {
      tipobj.style.top=ie? ietruebody().scrollTop+event.clientY-tipobj.offsetHeight-offsetypoint+"px" :
        window.pageYOffset+e.clientY-tipobj.offsetHeight-offsetypoint+"px"
    } else {
      tipobj.style.top=curY+offsetypoint+"px"
    }
    tipobj.style.display="block";
  }
}

function hideTip(e){
  if (!window.enabletip) {
    return;
  }
  Event.stopObserving(document, "mousemove", positionTip);
  enabletip=false
  tipobj.style.display="none";
}
Event.observe(window, "load", function() {$("body").appendChild(tipobj);});