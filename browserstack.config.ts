const cp = require('child_process');
const clientPlaywrightVersion = cp
  .execSync('npx playwright --version')
  .toString()
  .trim()
  .split(' ')[1];

// BrowserStack Specific Capabilities.
// Set 'browserstack.local:true For Local testing
const caps = {
  browser: 'chrome',
  os: 'osx',
  os_version: 'catalina',
  name: 'My first playwright test',
  build: 'playwright-build',
  'browserstack.username': process.env.BROWSERSTACK_USERNAME || 'USERNAME',
  'browserstack.accessKey': process.env.BROWSERSTACK_ACCESS_KEY || 'ACCESSKEY',
  'browserstack.local': process.env.BROWSERSTACK_LOCAL || true, 
  'client.playwrightVersion': clientPlaywrightVersion,
};

// Patching the capabilities dynamically according to the project name.
const patchCaps = (name, title) => {
  const combination = name.split(/@browserstack/)[0];
  const [browerCaps, osCaps] = combination.split(/:/);
  const [browser, browser_version] = browerCaps.split(/@/);
  const osCapsSplit = osCaps.split(/ /);
  const os = osCapsSplit.shift();
  const os_version = osCapsSplit.join(' ');
  caps.browser = browser ? browser : 'chrome';
  caps.os_version = browser_version ? browser_version : 'latest';
  caps.os = os ? os : 'osx';
  caps.os_version = os_version ? os_version : 'catalina';
  caps.name = title;
};

exports.getCdpEndpoint = (name, title) => {
    patchCaps(name, title)    
    const cdpUrl = `wss://cdp.browserstack.com/playwright?caps=${encodeURIComponent(JSON.stringify(caps))}`
    console.log(`--> ${cdpUrl}`)
    return cdpUrl;
}
