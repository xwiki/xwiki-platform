/**
 * TODO: Rewrite this config to ESM
 * But currently electron-builder doesn't support ESM configs
 * @see https://github.com/develar/read-config-file/issues/10
 */

/**
 * @type {() => import('electron-builder').Configuration}
 * @see https://www.electron.build/configuration/configuration
 */
module.exports = async function () {
  const {getVersion} = await import('./version/getVersion.mjs');

  return {
    directories: {
      output: 'dist',
      buildResources: 'buildResources',
    },
    files: ['./renderer/dist/**',
            './main/dist/**',
            './preload/dist/**'],
    extraMetadata: {
      version: getVersion(),
    },

    // Specify linux target just for disabling snap compilation
    linux: {
      target: 'deb',
      category: 'Application'
    },
    mac : {
      target: 'dmg',
      category: 'Application'
    },
    win : {
      target: 'nsis',
    }
  };
};
