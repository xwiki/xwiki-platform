var Drag={
        "obj":null,
	"init":function(a, aRoot){
			a.onmousedown=Drag.start;
			a.root = aRoot;
			if(isNaN(parseInt(a.root.style.left)))a.root.style.left="0px";
			if(isNaN(parseInt(a.root.style.top)))a.root.style.top="0px";
			a.root.onDragStart=new Function();
			a.root.onDragEnd=new Function();
			a.root.onDrag=new Function();
		},
	"start":function(evt){
			var obj=Drag.obj=this;
			evt=Drag.fixE(evt);
			var c=parseInt(obj.root.style.top);
			var d=parseInt(obj.root.style.left);
			obj.lastMouseX=evt.clientX;
			obj.lastMouseY=evt.clientY;
			obj.root.onDragStart(d,c,evt.clientX,evt.clientY);
			document.onmousemove=Drag.drag;
			document.onmouseup=Drag.end;
			return false;
		},
	"drag":function(evt){
			evt=Drag.fixE(evt);
			var obj=Drag.obj;
			var cy=evt.clientY;
			var cx=evt.clientX;
			var e=parseInt(obj.root.style.top);
			var f=parseInt(obj.root.style.left);
			var h,g;
			h=f+cx-obj.lastMouseX;
			g=e+cy-obj.lastMouseY;
			obj.root.style.left=h+"px";
			obj.root.style.top=g+"px";			
			obj.lastMouseX=cx;
			obj.lastMouseY=cy;
			obj.root.onDrag(h,g,cy,cx);
			return false;
		},
	"end":function(){			
			document.onmousemove=null;
			document.onmouseup=null;
			Drag.obj.root.onDragEnd(parseInt(Drag.obj.root.style.left),parseInt(Drag.obj.root.style.top));
			Drag.obj=null;
		},
	"fixE":function(evt){
			if(typeof evt=="undefined")evt=window.event;
			if(typeof evt.layerX=="undefined")evt.layerX=evt.offsetX;
			if(typeof evt.layerY=="undefined")evt.layerY=evt.offsetY;
			return evt;
		}
};