package io.karatelabs.js;

sealed interface JsPrimitive extends JsValue permits JsNumber, JsString, JsBoolean, JsBigInt {
}
