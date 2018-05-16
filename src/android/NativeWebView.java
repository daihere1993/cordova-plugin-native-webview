/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/
package nativewebview;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.HttpAuthHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tencent.mm.opensdk.modelmsg.SendMessageToWX;
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage;
import com.tencent.mm.opensdk.modelmsg.WXTextObject;
import com.tencent.mm.opensdk.modelmsg.WXWebpageObject;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaHttpAuthHandler;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaPreferences;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginManager;
import org.apache.cordova.PluginResult;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;


@SuppressLint("SetJavaScriptEnabled")
public class NativeWebView extends CordovaPlugin {

  public static final String LOG_TAG = "NativeWebView";
  public static final String PREFS_NAME = "NativeWebView";
  private static final String EXIT_EVENT = "exit";
  private static final String LOAD_START_EVENT = "loadstart";
  private static final String LOAD_STOP_EVENT = "loadstop";
  private static final String LOAD_ERROR_EVENT = "loaderror";
  public static final String WXAPPID_PROPERTY_KEY = "wechatappid";

  public static final String ERROR_WECHAT_NOT_INSTALLED = "未安装微信";
  public static final String ERROR_INVALID_PARAMETERS = "参数格式错误";
  public static final String ERROR_SEND_REQUEST_FAILED = "发送请求失败";
  public static final String ERROR_WECHAT_RESPONSE_COMMON = "普通错误";
  public static final String ERROR_WECHAT_RESPONSE_USER_CANCEL = "用户点击取消并返回";
  public static final String ERROR_WECHAT_RESPONSE_SENT_FAILED = "发送失败";
  public static final String ERROR_WECHAT_RESPONSE_AUTH_DENIED = "授权失败";
  public static final String ERROR_WECHAT_RESPONSE_UNSUPPORT = "微信不支持";
  public static final String ERROR_WECHAT_RESPONSE_UNKNOWN = "未知错误";
  public static final String EXTERNAL_STORAGE_IMAGE_PREFIX = "external://";
  public static final int BACK_CLICK = 0;
  public static final int CLOSE_CLICK = 1;
  public static final int SHARE_CLICK = 2;
  public static final int WECHAT_CLICK = 3;
  public static final int TIMELINE_CLICK = 4;
  private String appId = "wx8f27eaf87a204f6a";

  protected static IWXAPI wxAPI;
  private NativeWebViewDialog dialog;
  private Dialog shareDialog;
  private Resources activityRes;
  private Context context;
  private WebView nativeWebView;
  private TextView textview;
  private String title;
  private String imgUrl;
  protected CordovaPreferences preferences = new CordovaPreferences();
  protected static CallbackContext currentCallbackContext;
  private String currentUrl = "";
  private boolean showShare = false;
  private boolean commonTitle = false;
  private boolean commonUrl = false;
  private boolean clearAllCache = false;
  private boolean clearSessionCache = false;
  private boolean hadwareBackButton = true;
  private boolean mediaPlaybackRequiresUserGesture = false;
  private boolean shouldPauseNativeWebView = false;
  private boolean useWideViewPort = true;
  private ValueCallback<Uri> mUploadCallback;
  private ValueCallback<Uri[]> mUploadCallbackLollipop;
  private final static int FILECHOOSER_REQUESTCODE = 1;
  private final static int FILECHOOSER_REQUESTCODE_LOLLIPOP = 2;
  private int toolbarColor = Color.WHITE;
  private ProgressBar progressbar;
  private boolean isClose = false;
  private boolean youliao = false;
  public static boolean openWechat = false;
  private Uri uri;
  private Activity activity;

  public NativeWebView(){
  }
 public NativeWebView(Activity activity){
   this.activity = activity;
 }
 public NativeWebView(Activity activity, boolean youliao){
    this.activity = activity;
    this.youliao = true;
 }
  public NativeWebView(Activity activity,Boolean showShare){
    this.activity = activity;
    this.showShare = showShare;
    myInit();
  }
  /**
   * Executes the request and returns PluginResult.
   *
   * @param action the action to execute.
   * @param args JSONArry of arguments for the plugin.
   * @param callbackContext the callbackContext used when calling back into JavaScript.
   * @return A PluginResult object with a status and message.
   */

  public boolean execute(String action, CordovaArgs args, final CallbackContext callbackContext) throws JSONException {
    if (action.equals("open")) {
      currentCallbackContext = callbackContext;
      final String url = args.getString(0);
      if(!args.isNull(1)){
        showShare = true;
      }
      if(!args.isNull(2)){
        JSONObject jsonObject = args.getJSONObject(2);
        if(jsonObject.has("title")){
          commonTitle = true;
          title = jsonObject.getString("title");
        }
        if(jsonObject.has("thumbImageUrl")){
          commonUrl = true;
          imgUrl = jsonObject.getString("thumbImageUrl");
        }
      }
      activity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          String result = "";
          result = showWebPage(url);
          PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, result);
          pluginResult.setKeepCallback(true);
          callbackContext.sendPluginResult(pluginResult);
        }
      });
    }
    else {
      return false;
    }
    return true;
  }
  @Override
  protected void pluginInitialize() {

    super.pluginInitialize();
    if(activity==null){
      activity = cordova.getActivity();
    }
    myInit();
  }
  protected void myInit(){
    String id = getAppId();
    // save app id
    saveAppId(activity, id);

    // init api
    initWXAPI();

    Log.d(LOG_TAG, "plugin initialized.");
  }
  protected void initWXAPI() {
    IWXAPI api = getWxAPI(activity);

    if (api != null) {
      api.registerApp(getAppId());
    }
  }

  /**
   * Get weixin api
   * @param ctx
   * @return
   */
  public static IWXAPI getWxAPI(Context ctx) {
    if (wxAPI == null) {
      String appId = getSavedAppId(ctx);

      if (!appId.isEmpty()) {
        wxAPI = WXAPIFactory.createWXAPI(ctx, appId, true);
      }
    }

    return wxAPI;
  }
  public String getAppId() {
    if (this.appId == null) {
      this.appId = preferences.getString(WXAPPID_PROPERTY_KEY, "");
    }

    return this.appId;
  }

  /**
   * Get saved app id
   * @param ctx
   * @return
   */
  public static String getSavedAppId(Context ctx) {
    SharedPreferences settings = ctx.getSharedPreferences(PREFS_NAME, 0);
    return settings.getString(WXAPPID_PROPERTY_KEY, "");
  }

  /**
   * Save app id into SharedPreferences
   * @param ctx
   * @param id
   */
  public static void saveAppId(Context ctx, String id) {
    if (id.isEmpty()) {
      return ;
    }

    SharedPreferences settings = ctx.getSharedPreferences(PREFS_NAME, 0);
    SharedPreferences.Editor editor = settings.edit();
    editor.putString(WXAPPID_PROPERTY_KEY, id);
    editor.commit();
  }
  @Override
  public void onStart(){
    if (dialog!=null){
      dialog.show();
    }
  }
  public static CallbackContext getCurrentCallbackContext() {
    return currentCallbackContext;
  }
  /**
   * Called when the view navigates.
   */
  @Override
  public void onReset() {
    closeDialog();
  }

  /**
   * Called when the system is about to start resuming a previous activity.
   */
  @Override
  public void onPause(boolean multitasking) {
    if (shouldPauseNativeWebView) {
      nativeWebView.onPause();
    }
    if(dialog!=null && !openWechat){
      hideDialog();
    }
  }

  /**
   * Called when the activity will start interacting with the user.
   */
  @Override
  public void onResume(boolean multitasking) {
    if (shouldPauseNativeWebView) {
      nativeWebView.onResume();
    }
  }

  /**
   * Called by AccelBroker when listener is to be shut down.
   * Stop listener.
   */
  public void onDestroy() {
    closeDialog();
  }


  /**
   * Closes the dialog
   */
  public void closeDialog() {
    isClose = true;
    this.activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        final WebView childView = nativeWebView;
        // The JS protects against multiple calls, so this should happen only when
        // closeDialog() is called by other native code.
        if (childView == null) {
          return;
        }

        childView.setWebViewClient(new WebViewClient() {
          // NB: wait for about:blank before dismissing
          public void onPageFinished(WebView view, String url) {
            if (dialog != null) {
              dialog.dismiss();
              dialog = null;
            }
          }
        });
        // NB: From SDK 19: "If you call methods on WebView from any thread
        // other than your app's UI thread, it can cause unexpected results."
        // http://developer.android.com/guide/webapps/migrating.html#Threads
        childView.loadUrl("about:blank");

        try {
          JSONObject obj = new JSONObject();
          obj.put("type", EXIT_EVENT);
          sendUpdate(obj, false);
        } catch (JSONException ex) {
          LOG.d(LOG_TAG, "Should never happen");
        }
      }
    });
  }

  public void hideDialog(){
    dialog.hide();
  }
  /**
   * Checks to see if it is possible to go back one page in history, then does so.
   */
  public void goBack() {
    this.nativeWebView.goBack();
  }

  /**
   * Can the web browser go back?
   * @return boolean
   */
  public boolean canGoBack() {
    return this.nativeWebView.canGoBack();
  }

  /**
   * Has the user set the hardware back button to go back
   * @return boolean
   */
  public boolean hardwareBack() {
    return hadwareBackButton;
  }

  /**
   * listener
   * @return
   */
  public void onClickListener(int type){
    switch (type){
      case BACK_CLICK:
        if(hardwareBack()&&canGoBack()){goBack();}
        else closeDialog();
        break;
      case CLOSE_CLICK:
        closeDialog();
        break;
      case SHARE_CLICK:
        setDialog();
        break;
      case WECHAT_CLICK:
        shareContent("webpage","session");
        break;
      case TIMELINE_CLICK:
        shareContent("webpage","timeline");
        break;
    }
  }

  private void shareContent(final String contentType,final String sceneType){
    final IWXAPI api = getWxAPI(activity);
    openWechat = true;
    if (!api.isWXAppInstalled()) {
      Log.e(LOG_TAG, ERROR_WECHAT_NOT_INSTALLED );
      return;
    }
    activity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          WXMediaMessage msg = null;
          if(contentType.equals("webpage")){
            WXWebpageObject webpage = new WXWebpageObject();
            webpage.webpageUrl = currentUrl;
            msg = new WXMediaMessage(webpage);
            msg.title = textview.getText().toString();
            if(commonUrl){
              Bitmap thumbnail = getBitmap(imgUrl);
              if(thumbnail!=null){
                msg.setThumbImage(thumbnail);
                thumbnail.recycle();
              }
            }

          }
          SendMessageToWX.Req req = new SendMessageToWX.Req();
          req.transaction =  buildTransaction(contentType);
          req.message = msg;
          req.scene = sceneType.equals("session")?SendMessageToWX.Req.WXSceneSession:SendMessageToWX.Req.WXSceneTimeline;
          api.sendReq(req);
          shareDialog.dismiss();
        }
      });

  }

  private String buildTransaction(final String type) {
    return (type == null) ? String.valueOf(System.currentTimeMillis()) : type + System.currentTimeMillis();
  }

  private NativeWebView getNativeWebView(){
    return this;
  }

  /**
   * Display a new browser with the specified URL.
   *
   * @param url the url to load.
   */
  public String showWebPage(final String url) {
    // Determine if we should hide the location bar.
    isClose = false;
    openWechat = false;
    mediaPlaybackRequiresUserGesture = false;


    final CordovaWebView thatWebView = this.webView;

    // Create dialog in new thread
    Runnable runnable = new Runnable() {
      /**
       * Convert our DIP units to Pixels
       *
       * @return int
       */
      private int dpToPixels(int dipValue) {
        int value = (int) TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP,
          (float) dipValue,
          activity.getResources().getDisplayMetrics()
        );

        return value;
      }

      @Override
      public int hashCode() {
        return super.hashCode();
      }

      @SuppressLint("NewApi")
      public void run() {

        // CB-6702 NativeWebView hangs when opening more than one instance
        if (dialog != null) {
          dialog.dismiss();
        };
        activityRes = activity.getResources();
        // Let's create the main dialog
        dialog = new NativeWebViewDialog(activity, android.R.style.Theme_NoTitleBar);
        context = activity;
        int animation_id = activityRes.getIdentifier("AnimationMyDialog", "style", context.getPackageName());
        int states_id = activityRes.getIdentifier("progress_bar_states", "drawable", context.getPackageName());
        dialog.getWindow().getAttributes().windowAnimations = animation_id;
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setNativeWebView(getNativeWebView());

        // Main container layout
        final LinearLayout main = new LinearLayout(activity);
        main.setOrientation(LinearLayout.VERTICAL);

        // Toolbar layout
        RelativeLayout toolbar = new RelativeLayout(activity);
        //Please, no more black!
        toolbarColor = Color.parseColor(preferences.getString("NativeWebViewNavBarColor", "#ffffff"));
        toolbar.setBackgroundColor(toolbarColor);
        toolbar.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, this.dpToPixels(44)));
        toolbar.setPadding(this.dpToPixels(2), this.dpToPixels(2), this.dpToPixels(2), this.dpToPixels(2));
        toolbar.setHorizontalGravity(Gravity.LEFT);
        toolbar.setVerticalGravity(Gravity.TOP);

        // Action Button Container layout
        RelativeLayout actionButtonContainer = new RelativeLayout(activity);
        actionButtonContainer.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        actionButtonContainer.setHorizontalGravity(Gravity.LEFT);
        actionButtonContainer.setVerticalGravity(Gravity.CENTER_VERTICAL);
        actionButtonContainer.setId(Integer.valueOf(1));

        // Back button
        ImageButton back = new ImageButton(activity);
        RelativeLayout.LayoutParams backLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        backLayoutParams.addRule(RelativeLayout.ALIGN_LEFT);
        back.setLayoutParams(backLayoutParams);
        back.setContentDescription("Back Button");
        back.setId(Integer.valueOf(2));
        int backResId = activityRes.getIdentifier("arrow_left", "drawable", activity.getPackageName());
        Drawable backIcon = activityRes.getDrawable(backResId);
        back.setColorFilter(android.graphics.Color.parseColor(preferences.getString("NativeWebViewIconButtonColor","#000000")));
        if (Build.VERSION.SDK_INT >= 16)
          back.setBackground(null);
        else
          back.setBackgroundDrawable(null);
        back.setImageDrawable(backIcon);
        back.setScaleType(ImageView.ScaleType.FIT_CENTER);
        back.setPadding(0, this.dpToPixels(10), 0, this.dpToPixels(10));
        if (Build.VERSION.SDK_INT >= 16)
          back.getAdjustViewBounds();

        back.setOnClickListener(new View.OnClickListener() {
          public void onClick(View v) {
            onClickListener(BACK_CLICK);
          }
        });

        // Forward button
        ImageButton forward = new ImageButton(activity);
        RelativeLayout.LayoutParams forwardLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        forwardLayoutParams.addRule(RelativeLayout.RIGHT_OF, 2);
        forward.setLayoutParams(forwardLayoutParams);
        forward.setContentDescription("Forward Button");
        forward.setId(Integer.valueOf(3));
        int fwdResId = activityRes.getIdentifier("close_round", "drawable", activity.getPackageName());
        Drawable fwdIcon = activityRes.getDrawable(fwdResId);
        forward.setColorFilter(android.graphics.Color.parseColor(preferences.getString("NativeWebViewIconButtonColor","#000000")));
        if (Build.VERSION.SDK_INT >= 16)
          forward.setBackground(null);
        else
          forward.setBackgroundDrawable(null);
        forward.setImageDrawable(fwdIcon);
        forward.setScaleType(ImageView.ScaleType.FIT_CENTER);
        forward.setPadding(0, this.dpToPixels(10), 0, this.dpToPixels(10));
        if (Build.VERSION.SDK_INT >= 16)
          forward.getAdjustViewBounds();

        forward.setOnClickListener(new View.OnClickListener() {
          public void onClick(View v) {
            onClickListener(CLOSE_CLICK);
          }
        });

        //share button
        ImageButton share = new ImageButton(activity);
        int shareResId = activityRes.getIdentifier("share", "drawable", activity.getPackageName());
        Drawable shareIcon = activityRes.getDrawable(shareResId);
        share.setColorFilter(android.graphics.Color.parseColor(preferences.getString("NativeWebViewIconButtonColor","#000000")));
        share.setImageDrawable(shareIcon);
        share.setScaleType(ImageView.ScaleType.FIT_CENTER);
        if (Build.VERSION.SDK_INT >= 16)
          share.getAdjustViewBounds();
        RelativeLayout.LayoutParams closeLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        closeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        share.setLayoutParams(closeLayoutParams);

        if (Build.VERSION.SDK_INT >= 16)
          share.setBackground(null);
        else
          share.setBackgroundDrawable(null);

        share.setContentDescription("share Button");
        share.setId(Integer.valueOf(5));
        share.setOnClickListener(new View.OnClickListener() {
          public void onClick(View v) {
            onClickListener(SHARE_CLICK);
          }
        });
        //Text View Box
        textview = new TextView(activity);
        RelativeLayout.LayoutParams textLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        textLayoutParams.addRule(RelativeLayout.RIGHT_OF, 1);
        textview.setLayoutParams(textLayoutParams);
        textview.setId(Integer.valueOf(10));
        textview.setSingleLine(true);
        textview.setTextSize(22);
        //textview.setText("加载中...");
        textview.setEllipsize(TextUtils.TruncateAt.valueOf("END"));
        textview.setTextColor(Color.BLACK);
        textview.setGravity(Gravity.CENTER);
        textview.setPadding(this.dpToPixels(5),this.dpToPixels(2),this.dpToPixels(68),0);

        //add progressbar
        progressbar = new ProgressBar(context, null, android.R.style.Widget_ProgressBar_Horizontal);
        progressbar.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, 3));
        Drawable drawable = context.getResources().getDrawable(states_id);
        progressbar.setProgressDrawable(drawable);
        progressbar.setProgressTintList(ColorStateList.valueOf(Color.parseColor(preferences.getString("NativeWebViewProgressBarColor", "#059aef"))));

        // WebView
        nativeWebView = new WebView(context);
        nativeWebView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        nativeWebView.setId(Integer.valueOf(6));
        // File Chooser Implemented ChromeClient
        nativeWebView.setWebChromeClient(new NativeWebViewChromeClient(thatWebView) {
          // For Android 5.0+
          public boolean onShowFileChooser (WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams)
          {
            LOG.d(LOG_TAG, "File Chooser 5.0+");
            // If callback exists, finish it.
            if(mUploadCallbackLollipop != null) {
              mUploadCallbackLollipop.onReceiveValue(null);
            }
            mUploadCallbackLollipop = filePathCallback;

            // Create File Chooser Intent
            Intent content = new Intent(Intent.ACTION_GET_CONTENT);
            content.addCategory(Intent.CATEGORY_OPENABLE);
            content.setType("*/*");

            // Run cordova startActivityForResult
            cordova.startActivityForResult(NativeWebView.this, Intent.createChooser(content, "Select File"), FILECHOOSER_REQUESTCODE_LOLLIPOP);
            return true;
          }

          // For Android 4.1+
          public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture)
          {
            LOG.d(LOG_TAG, "File Chooser 4.1+");
            // Call file chooser for Android 3.0+
            openFileChooser(uploadMsg, acceptType);
          }

          // For Android 3.0+
          public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType)
          {
            LOG.d(LOG_TAG, "File Chooser 3.0+");
            mUploadCallback = uploadMsg;
            Intent content = new Intent(Intent.ACTION_GET_CONTENT);
            content.addCategory(Intent.CATEGORY_OPENABLE);

            // run startActivityForResult
            cordova.startActivityForResult(NativeWebView.this, Intent.createChooser(content, "Select File"), FILECHOOSER_REQUESTCODE);
          }

          @Override
          public void onProgressChanged(WebView view, int newProgress) {
            if (newProgress == 100) {
              progressbar.setVisibility(GONE);
            } else {
              if (progressbar.getVisibility() == GONE && !isClose)
                progressbar.setVisibility(VISIBLE);
              progressbar.setProgress(newProgress);
            }
            super.onProgressChanged(view, newProgress);
          }
        });
        WebViewClient client = new NativeWebViewClient(thatWebView);
        nativeWebView.setWebViewClient(client);
        WebSettings settings = nativeWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setPluginState(android.webkit.WebSettings.PluginState.ON);

        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
          settings.setMediaPlaybackRequiresUserGesture(mediaPlaybackRequiresUserGesture);
        }

        String overrideUserAgent = preferences.getString("OverrideUserAgent", null);
        String appendUserAgent = preferences.getString("AppendUserAgent", null);

        if (overrideUserAgent != null) {
          settings.setUserAgentString(overrideUserAgent);
        }
        if (appendUserAgent != null) {
          settings.setUserAgentString(settings.getUserAgentString() + appendUserAgent);
        }

        //Toggle whether this is enabled or not!
        Bundle appSettings = activity.getIntent().getExtras();
        boolean enableDatabase = appSettings == null ? true : appSettings.getBoolean("NativeWebViewStorageEnabled", true);
        if (enableDatabase) {
          String databasePath = activity.getApplicationContext().getDir("inAppBrowserDB", Context.MODE_PRIVATE).getPath();
          settings.setDatabasePath(databasePath);
          settings.setDatabaseEnabled(true);
        }
        settings.setDomStorageEnabled(true);

        if (clearAllCache) {
          CookieManager.getInstance().removeAllCookie();
        } else if (clearSessionCache) {
          CookieManager.getInstance().removeSessionCookie();
        }

        // Enable Thirdparty Cookies on >=Android 5.0 device
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
          CookieManager.getInstance().setAcceptThirdPartyCookies(nativeWebView,true);
        }

        nativeWebView.loadUrl(url);
        nativeWebView.setId(Integer.valueOf(6));
        nativeWebView.getSettings().setLoadWithOverviewMode(true);
        nativeWebView.getSettings().setUseWideViewPort(useWideViewPort);
        nativeWebView.requestFocus();
        nativeWebView.requestFocusFromTouch();

        // Add the back and forward buttons to our action button container layout
        actionButtonContainer.addView(back);
        actionButtonContainer.addView(forward);
        toolbar.addView(actionButtonContainer);
        toolbar.addView(textview);
        if(showShare){
          toolbar.addView(share);
        }


        // Add our toolbar to our main view/layout
        main.addView(toolbar);

        main.addView(progressbar);
        // Add our webview to our main view/layout
        RelativeLayout webViewLayout = new RelativeLayout(activity);
        webViewLayout.addView(nativeWebView);
        main.addView(webViewLayout);

        // Don't add the footer unless it's been enabled
//                if (showFooter) {
//                    webViewLayout.addView(footer);
//                }

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;

        dialog.setContentView(main);
        dialog.show();
        dialog.getWindow().setAttributes(lp);
        // the goal of openhidden is to load the url and not display it
      }
    };
    activity.runOnUiThread(runnable);
    return "";
  }

  private void  setDialog(){
    shareDialog = new Dialog(activity,activityRes.getIdentifier("BottomDialog", "style", context.getPackageName()));
    LinearLayout root = (LinearLayout) LayoutInflater.from(context).inflate(
      activityRes.getIdentifier("bottom_dialog", "layout", context.getPackageName()), null);
    //初始化视图
    root.findViewById(activityRes.getIdentifier("wechat_icon", "id", context.getPackageName())).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        onClickListener(WECHAT_CLICK);
      }
    });
    root.findViewById(activityRes.getIdentifier("timeline_icon", "id", context.getPackageName())).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        onClickListener(TIMELINE_CLICK);
      }
    });
    shareDialog.setContentView(root);
    Window dialogWindow = shareDialog.getWindow();
    dialogWindow.setGravity(Gravity.BOTTOM);
//        dialogWindow.setWindowAnimations(R.style.dialogstyle); // 添加动画
    WindowManager.LayoutParams lp = dialogWindow.getAttributes(); // 获取对话框当前的参数值
    lp.x = 0; // 新位置X坐标
    lp.y = 0; // 新位置Y坐标
    lp.width = (int) context.getResources().getDisplayMetrics().widthPixels; // 宽度
    root.measure(0, 0);
    lp.height = root.getMeasuredHeight();

    lp.alpha = 9f; // 透明度
    dialogWindow.setAttributes(lp);
    shareDialog.show();
  }
  /**
   * Create a new plugin success result and send it back to JavaScript
   *
   * @param obj a JSONObject contain event payload information
   */
  private void sendUpdate(JSONObject obj, boolean keepCallback) {
    sendUpdate(obj, keepCallback, PluginResult.Status.OK);
  }

  /**
   * Create a new plugin result and send it back to JavaScript
   *
   * @param obj a JSONObject contain event payload information
   * @param status the status code to return to the JavaScript environment
   */
  private void sendUpdate(JSONObject obj, boolean keepCallback, PluginResult.Status status) {
    if (currentCallbackContext != null) {
      PluginResult result = new PluginResult(status, obj);
      result.setKeepCallback(keepCallback);
      currentCallbackContext.sendPluginResult(result);
      if (!keepCallback) {
        currentCallbackContext = null;
      }
    }
  }

  /**
   * Receive File Data from File Chooser
   *
   * @param requestCode the requested code from chromeclient
   * @param resultCode the result code returned from android system
   * @param intent the data from android file chooser
   */
  public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    // For Android >= 5.0
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      LOG.d(LOG_TAG, "onActivityResult (For Android >= 5.0)");
      // If RequestCode or Callback is Invalid
      if(requestCode != FILECHOOSER_REQUESTCODE_LOLLIPOP || mUploadCallbackLollipop == null) {
        super.onActivityResult(requestCode, resultCode, intent);
        return;
      }
      mUploadCallbackLollipop.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, intent));
      mUploadCallbackLollipop = null;
    }
    // For Android < 5.0
    else {
      LOG.d(LOG_TAG, "onActivityResult (For Android < 5.0)");
      // If RequestCode or Callback is Invalid
      if(requestCode != FILECHOOSER_REQUESTCODE || mUploadCallback == null) {
        super.onActivityResult(requestCode, resultCode, intent);
        return;
      }

      if (null == mUploadCallback) return;
      Uri result = intent == null || resultCode != activity.RESULT_OK ? null : intent.getData();

      mUploadCallback.onReceiveValue(result);
      mUploadCallback = null;
    }
  }


  /**
   * The webview client receives notifications about appView
   */
  public class NativeWebViewClient extends WebViewClient {
    CordovaWebView webView;

    /**
     * Constructor.
     *
     * @param webView
     */
    public NativeWebViewClient(CordovaWebView webView) {
      this.webView = webView;
    }

    /**
     * Override the URL that should be loaded
     *
     * This handles a small subset of all the URIs that would be encountered.
     *
     * @param webView
     * @param url
     */
    @Override
    public boolean shouldOverrideUrlLoading(WebView webView, String url) {
      if (url.startsWith(WebView.SCHEME_TEL)) {
        try {
          Intent intent = new Intent(Intent.ACTION_DIAL);
          intent.setData(Uri.parse(url));
          activity.startActivity(intent);
          return true;
        } catch (android.content.ActivityNotFoundException e) {
          LOG.e(LOG_TAG, "Error dialing " + url + ": " + e.toString());
        }
      } else if (url.startsWith("geo:") || url.startsWith(WebView.SCHEME_MAILTO) || url.startsWith("market:") || url.startsWith("intent:")) {
        try {
          Intent intent = new Intent(Intent.ACTION_VIEW);
          intent.setData(Uri.parse(url));
          activity.startActivity(intent);
          return true;
        } catch (android.content.ActivityNotFoundException e) {
          LOG.e(LOG_TAG, "Error with " + url + ": " + e.toString());
        }
      }
      // If sms:5551212?body=This is the message
      else if (url.startsWith("sms:")) {
        try {
          Intent intent = new Intent(Intent.ACTION_VIEW);

          // Get address
          String address = null;
          int parmIndex = url.indexOf('?');
          if (parmIndex == -1) {
            address = url.substring(4);
          } else {
            address = url.substring(4, parmIndex);

            // If body, then set sms body
            Uri uri = Uri.parse(url);
            String query = uri.getQuery();
            if (query != null) {
              if (query.startsWith("body=")) {
                intent.putExtra("sms_body", query.substring(5));
              }
            }
          }
          intent.setData(Uri.parse("sms:" + address));
          intent.putExtra("address", address);
          intent.setType("vnd.android-dir/mms-sms");
          activity.startActivity(intent);
          return true;
        } catch (android.content.ActivityNotFoundException e) {
          LOG.e(LOG_TAG, "Error sending sms " + url + ":" + e.toString());
        }
      }
      return false;
    }


    /*
     * onPageStarted fires the LOAD_START_EVENT
     *
     * @param view
     * @param url
     * @param favicon
     */
    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
      super.onPageStarted(view, url, favicon);
      currentUrl = url;
      if(commonTitle){
        textview.setText(title);
      }else{
        textview.setText("加载中...");
      }
      String newloc = "";
      if (url.startsWith("http:") || url.startsWith("https:") || url.startsWith("file:")) {
        newloc = url;
      }
      else
      {
        // Assume that everything is HTTP at this point, because if we don't specify,
        // it really should be.  Complain loudly about this!!!
        LOG.e(LOG_TAG, "Possible Uncaught/Unknown URI");
        newloc = "http://" + url;
      }


      try {
        JSONObject obj = new JSONObject();
        obj.put("type", LOAD_START_EVENT);
        obj.put("url", newloc);
        sendUpdate(obj, true);
      } catch (JSONException ex) {
        LOG.e(LOG_TAG, "URI passed in has caused a JSON error.");
      }
    }



    public void onPageFinished(WebView view, String url) {
      super.onPageFinished(view, url);
      //removeProgress();
      //set title
      String pageTitle;
      if(!commonTitle){
        if(youliao){
          uri = Uri.parse(url);
          pageTitle = uri.getQueryParameter("dt");
        }else{
          pageTitle = view.getTitle();
        }
        if((textview.getText()==""||textview.getText()!=pageTitle)){
            textview.setText(pageTitle);
          }
      }

      // CB-10395 NativeWebView's WebView not storing cookies reliable to local device storage
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
        CookieManager.getInstance().flush();
      } else {
        CookieSyncManager.getInstance().sync();
      }

      // https://issues.apache.org/jira/browse/CB-11248
      view.clearFocus();
      view.requestFocus();

      try {
        JSONObject obj = new JSONObject();
        obj.put("type", LOAD_STOP_EVENT);
        obj.put("url", url);

        sendUpdate(obj, true);
      } catch (JSONException ex) {
        LOG.d(LOG_TAG, "Should never happen");
      }
    }

    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
      super.onReceivedError(view, errorCode, description, failingUrl);

      try {
        JSONObject obj = new JSONObject();
        obj.put("type", LOAD_ERROR_EVENT);
        obj.put("url", failingUrl);
        obj.put("code", errorCode);
        obj.put("message", description);

        sendUpdate(obj, true, PluginResult.Status.ERROR);
      } catch (JSONException ex) {
        LOG.d(LOG_TAG, "Should never happen");
      }
    }


    /**
     * On received http auth request.
     */
    @Override
    public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {

      // Check if there is some plugin which can resolve this auth challenge
      PluginManager pluginManager = null;
      try {
        Method gpm = webView.getClass().getMethod("getPluginManager");
        pluginManager = (PluginManager)gpm.invoke(webView);
      } catch (NoSuchMethodException e) {
        LOG.d(LOG_TAG, e.getLocalizedMessage());
      } catch (IllegalAccessException e) {
        LOG.d(LOG_TAG, e.getLocalizedMessage());
      } catch (InvocationTargetException e) {
        LOG.d(LOG_TAG, e.getLocalizedMessage());
      }

      if (pluginManager == null) {
        try {
          Field pmf = webView.getClass().getField("pluginManager");
          pluginManager = (PluginManager)pmf.get(webView);
        } catch (NoSuchFieldException e) {
          LOG.d(LOG_TAG, e.getLocalizedMessage());
        } catch (IllegalAccessException e) {
          LOG.d(LOG_TAG, e.getLocalizedMessage());
        }
      }

      if (pluginManager != null && pluginManager.onReceivedHttpAuthRequest(webView, new CordovaHttpAuthHandler(handler), host, realm)) {
        return;
      }

      // By default handle 401 like we'd normally do!
      super.onReceivedHttpAuthRequest(view, handler, host, realm);
    }
  }
  protected Bitmap getBitmap(String url) {
    Bitmap bmp = null;
    try {
      // get input stream
      InputStream inputStream = getFileInputStream(url);
      if (inputStream == null) {
        return null;
      }

      // decode it
      // @TODO make sure the image is not too big, or it will cause out of memory
      BitmapFactory.Options options = new BitmapFactory.Options();
      bmp = BitmapFactory.decodeStream(inputStream, null, options);

      // scale
//      if (maxSize > 0 && (options.outWidth > maxSize || options.outHeight > maxSize)) {
//
//        Log.d(LOG_TAG, String.format("Bitmap was decoded, dimension: %d x %d, max allowed size: %d.",
//          options.outWidth, options.outHeight, maxSize));
//
//        int width = 0;
//        int height = 0;
//
//        if (options.outWidth > options.outHeight) {
//          width = maxSize;
//          height = width * options.outHeight / options.outWidth;
//        } else {
//          height = maxSize;
//          width = height * options.outWidth / options.outHeight;
//        }
//
//        Bitmap scaled = Bitmap.createScaledBitmap(bmp, width, height, true);
//        bmp.recycle();
//
//        bmp = scaled;
//      }

      inputStream.close();

    }  catch (IOException e) {
      bmp = null;
      e.printStackTrace();
    }

    return bmp;
  }

  /**
   * Get input stream from a url
   *
   * @param url
   * @return
   */
  protected InputStream getFileInputStream(String url) {
    try {

      InputStream inputStream = null;

      if (URLUtil.isHttpUrl(url) || URLUtil.isHttpsUrl(url)) {

        File file = Util.downloadAndCacheFile(webView.getContext(), url);

        if (file == null) {
          Log.d(LOG_TAG, String.format("File could not be downloaded from %s.", url));
          return null;
        }

        url = file.getAbsolutePath();
        inputStream = new FileInputStream(file);

        Log.d(LOG_TAG, String.format("File was downloaded and cached to %s.", url));

      } else if (url.startsWith("data:image")) {  // base64 image

        String imageDataBytes = url.substring(url.indexOf(",") + 1);
        byte imageBytes[] = Base64.decode(imageDataBytes.getBytes(), Base64.DEFAULT);
        inputStream = new ByteArrayInputStream(imageBytes);

        Log.d(LOG_TAG, "Image is in base64 format.");

      } else if (url.startsWith(EXTERNAL_STORAGE_IMAGE_PREFIX)) { // external path

        url = Environment.getExternalStorageDirectory().getAbsolutePath() + url.substring(EXTERNAL_STORAGE_IMAGE_PREFIX.length());
        inputStream = new FileInputStream(url);

        Log.d(LOG_TAG, String.format("File is located on external storage at %s.", url));

      } else if (!url.startsWith("/")) { // relative path

        inputStream = activity.getApplicationContext().getAssets().open(url);

        Log.d(LOG_TAG, String.format("File is located in assets folder at %s.", url));

      } else {

        inputStream = new FileInputStream(url);

        Log.d(LOG_TAG, String.format("File is located at %s.", url));

      }

      return inputStream;

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return null;
  }
}

