(function () {
  // Code executed before Prototype.js is loaded.
  const originalArrayFrom = Array.from;
  setTimeout(() => {
    // Code executed after Prototype.js is loaded.
    Array.from = originalArrayFrom;
  }, 0);
})();
