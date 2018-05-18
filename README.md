# cordova-plugin-native-webview

This plugin will open another native webview to nicely show your web resources inside of your app. At the save time, plugin suport a sharing feature, you can share website to wechat.

## Features

* Share to wechat
* Nice navigationbar
* WebView progress bar
* Suport custom color.
* Suport gesture(swap right to back).

## Example

![image](https://github.com/daihere1993/cordova-plugin-native-webview/blob/master/doc/demo.gif)


## Install

```bash
cordova plugin add https://github.com/daihere1993/cordova-plugin-native-webview
```

## Dependencies

If you want use sharing feature, you need add [cordova-plugin-wechat](https://github.com/xu-li/cordova-plugin-wechat).

```bash
cordova plugin add cordova-plugin-wechat --variable wechatappid=YOUR_WECHAT_APPID
```

## Configuration

Configure plugin colors in config.xml

```xml
<preference name="NativeWebViewNavBarColor" value="#ffffff" />
<preference name="NativeWebViewProgressBarColor" value="#059aef" />
<preference name="NativeWebViewIconButtonColor" value="#000" />
```

## Useage

```js
/**
 * url(String): web resouces url.
 * enableShare(Boolean): Whether enable sharing.
 * title(String): Webview title, if null, will get webview internal title.
 * thumbImageUrl(String): Thumb image url for sharing wechat.
 */
NativeWebView.oepn(url, enableShare, { title: title, thumbImageUrl: thumbImageUrl });
```

## LICENSE

[MIT LICENSE](http://opensource.org/licenses/MIT)
