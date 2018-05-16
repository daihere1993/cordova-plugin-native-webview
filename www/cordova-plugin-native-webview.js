var exec = require('cordova/exec');


module.exports = {
    /**
     * shareParams: {
     *  title,
     *  thumbImageUrl
     * }
     */
    open: function (url, enableShare, shareParams) {
        exec(null, null, 'CDVNativeWebView', 'open', [url, enableShare, shareParams]);
    }
}
