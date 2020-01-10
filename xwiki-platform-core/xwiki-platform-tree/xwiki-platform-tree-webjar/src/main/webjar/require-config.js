/*!
#set ($minified = '.min')
#if (!$services.debug.minify)
  #set ($minified = '')
#end
*/
require.config({
  paths: {
    jsTree: '$!services.webjars.url("jstree", "jstree${minified}")',
    JobRunner: '$!services.webjars.url("org.xwiki.platform:xwiki-platform-job-webjar", "jobRunner${minified}")',
    'tree-finder': '$!services.webjars.url("org.xwiki.platform:xwiki-platform-tree-webjar", "finder${minified}")',
    tree: '$!services.webjars.url("org.xwiki.platform:xwiki-platform-tree-webjar", "tree${minified}")'
  },
  shim: {
    jsTree: {
      deps: ['jquery']
    }
  }
});
