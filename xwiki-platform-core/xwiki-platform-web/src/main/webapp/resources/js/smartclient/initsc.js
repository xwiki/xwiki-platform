// prevent collision with prototype, see http://forums.smartclient.com/showthread.php?t=853
window.isc_useSimpleNames = false;
//
// TODO : When XWIKI-3582 will be fixed we should better use the line below.
// window.isomorphicDir = "$xwiki.getSkinFile('js/smartclient/')";
//
window.isomorphicDir = "${request.getContextPath()}/resources/js/smartclient/";
