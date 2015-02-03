/*!
#set ($minified = '.min')
#if ($request.minify == 'false')
  #set ($minified = '')
#end
*/
require.config({
  paths: {
    jsTree: [
      '//cdnjs.cloudflare.com/ajax/libs/jstree/${jstree.version}/jstree.min',
      "$!services.webjars.url('jstree', 'jstree.min.js')"
    ],
    JobRunner: '$!services.webjars.url("org.xwiki.platform:xwiki-platform-job-webjar", "jobRunner${minified}.js")',
    'tree-finder': '$!services.webjars.url("org.xwiki.platform:xwiki-platform-tree-webjar", "finder${minified}.js")',
    tree: '$!services.webjars.url("org.xwiki.platform:xwiki-platform-tree-webjar", "tree${minified}.js")'
  },
  shim: {
    jsTree: {
      deps: ['jquery']
    }
  }
});
