# cordova-plugin-native-webview

As we all know, ionic app is a webview. If we want open a webview in ionic app. We have two ways:

* Iframe
* Open a native webview

If iframe is very easy, but will meet some troubles that's not a good choose.

Second option, cordova official suport a plugin: [cordova-plugin-inappbrowser](https://github.com/apache/cordova-plugin-inappbrowser). That is huge and complex plugin, furthermore that is extremely ugly.

Now, [cordova-plugin-native-webview](https://github.com/daihere1993/cordova-plugin-native-webview) is here~~🎉🎉🎉


## Purpose

If you want open a webview that in cordova app. 

This plugin is awesome.

## Feature

* Nice navigationbar
* WebView progress bar
* Suport gesture(swap right to back).
* Suport custom color. 

## Useage

1. Set some color in config.xml.

```xml
<preference name="NativeWebViewNavBarColor" value="#ffffff" />
<preference name="NativeWebViewProgressBarColor" value="#059aef" />
<preference name="NativeWebViewIconButtonColor" value="#000" />
```

2. Invoke ``NativeWebView.oepn(url)`` in js.