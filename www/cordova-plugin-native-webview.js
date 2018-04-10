var exec = require('cordova/exec');


module.exports = {
    open: function (url, options) {
        options = options || {};
        exec(null, null, 'CDVNativeWebView', 'open', [url]);
    }
}
