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
	"start":function(a){	
			var b=Drag.obj=this;
			a=Drag.fixE(a);
			var c=parseInt(b.root.style.top);
			var d=parseInt(b.root.style.left);
			b.root.onDragStart(d,c,a.clientX,a.clientY);
			b.lastMouseX=a.clientX;
			b.lastMouseY=a.clientY;
			document.onmousemove=Drag.drag;
			document.onmouseup=Drag.end;
			return false;
		},	
	"drag":function(a){
			a=Drag.fixE(a);
			var b=Drag.obj;
			var c=a.clientY;
			var d=a.clientX;
			var e=parseInt(b.root.style.top);
			var f=parseInt(b.root.style.left);
			var h,g;
			h=f+d-b.lastMouseX;
			g=e+c-b.lastMouseY;
			b.root.style.left=h+"px";
			b.root.style.top=g+"px";			
			b.lastMouseX=d;
			b.lastMouseY=c;
			b.root.onDrag(h,g,a.clientX,a.clientY);
			return false;
		},
	"end":function(){			
			document.onmousemove=null;
			document.onmouseup=null;
			Drag.obj.root.onDragEnd(parseInt(Drag.obj.root.style.left),parseInt(Drag.obj.root.style.top));
			Drag.obj=null;
		},
	"fixE":function(a){
			if(typeof a=="undefined")a=window.event;
			if(typeof a.layerX=="undefined")a.layerX=a.offsetX;
			if(typeof a.layerY=="undefined")a.layerY=a.offsetY;
			return a;
		}
};