function t(n) {
  throw console.error({ unreachable: n }), new Error("Reached a theoretically unreachable statement");
}
function o(n, r, e) {
  if (!r.includes(n))
    throw new Error(e + ": " + n);
  return n;
}
function i(n) {
  try {
    return n();
  } catch {
    return null;
  }
}
function u(n) {
  try {
    return n();
  } catch (r) {
    return r instanceof Error ? r : typeof r == "string" ? new Error(r) : typeof r == "number" || typeof r == "boolean" ? new Error(r.toString()) : r === null ? new Error("null") : r === void 0 ? new Error("undefined") : (console.error({ throw: r }), new Error("<thrown unknown value type>"));
  }
}
async function l(n) {
  try {
    return await n();
  } catch (r) {
    return r instanceof Error ? r : typeof r == "string" ? new Error(r) : typeof r == "number" || typeof r == "boolean" ? new Error(r.toString()) : r === null ? new Error("null") : r === void 0 ? new Error("undefined") : (console.error({ throw: r }), new Error("<thrown unknown value type>"));
  }
}
function f(n) {
  return n;
}
function c(n, r) {
  return n.map(r).filter((e) => e != null);
}
function a(n) {
  return Object.entries(n);
}
export {
  o as assertInArray,
  t as assertUnreachable,
  c as filterMap,
  a as objectEntries,
  f as provideTypeInference,
  i as tryFallible,
  u as tryFallibleOrError,
  l as tryFalliblePromiseOrError
};
//# sourceMappingURL=index.es.js.map
