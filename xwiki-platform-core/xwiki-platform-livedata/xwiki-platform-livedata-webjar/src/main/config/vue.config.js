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

module.exports = {
  // See https://cli.vuejs.org/config/
  // We don't know the public path at build time so we have to leave it empty and set it at runtime using the
  // __webpack_public_path__ variable (see https://webpack.js.org/configuration/output/#outputpublicpath).
  publicPath: '',
  filenameHashing: false,

  configureWebpack: config => {
    // Skip LESS parsing because we want the LESS code to be evaluated at runtime, on the server, in the context of the
    // XWiki skin so that we can use skin and color theme variables.
    const lessRules = config.module.rules.find(rule => /less/.test(rule.test));
    lessRules.oneOf.forEach(context => {
      const loaders = context.use;
      const lessLoaderIndex = loaders.findIndex(loader => /less-loader/.test(loader.loader));
      if (lessLoaderIndex !== -1) {
        loaders.splice(lessLoaderIndex, 1);
      }
    });
    // Export all styles as LESS files to be included in the WebJar.
    const miniCssExtractPlugin = config.plugins.find(plugin => plugin.constructor.name === "MiniCssExtractPlugin");
    miniCssExtractPlugin.options.filename = "[name].less";
    miniCssExtractPlugin.options.chunkFilename = "less/[name].less?evaluate=true";
  },

  chainWebpack: config => {
    // Provided dependencies (that shouldn't be bundled).
    config.externals({
      "vue": "vue",
      "vue-i18n": "vue-i18n",
      "jquery": "jquery",
      "daterangepicker": "daterangepicker",
      "moment": "moment",
      "moment-jdateformatparser": "moment-jdateformatparser",
      "xwiki-selectize": "xwiki-selectize",
      "xwiki-livedata-xObjectPropertyHelper": "xwiki-livedata-xObjectPropertyHelper",
      "xwiki-meta": "xwiki-meta"
    })
  },
  css: {
    // We want to extract the styles as LESS files in order to be able to evaluate them at rutime in the context of the
    // XWiki skin and color theme.
    extract: true,
  },
};
