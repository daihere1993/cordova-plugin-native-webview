//
//  CDVNativeWebView.m
//
//  Created by Luke Wuu on 09/04/2018.
//

#import "CDVNativeWebView.h"
#import "CDVWechat.h"
#import <ionicons.h>

#define SCREEN_WIDTH ([[UIScreen mainScreen] bounds].size.width)
#define SCREEN_HEIGHT ([[UIScreen mainScreen] bounds].size.height)

@interface CDVNativeWebView()
@property (nonatomic, strong) NSString *navBarColor;
@property (nonatomic, strong) NSString *progressBarColor;
@property (nonatomic, strong) NSString *iconButtonColor;
@property (nonatomic, strong) NSString *url;
@property (nonatomic, strong) NSString *thumbImageUrl;
@property (nonatomic, strong) PersentAnimation *persentAnimation;
@property (nonatomic, strong) DismissAnimation *dismissAnimation;
@property (nonatomic, strong) SwapeRightInteractiveTransition *swapTransition;
@property (nonatomic, strong) NSString *wechatAppId;
@end

@implementation CDVNativeWebView

- (id)settingForKey:(NSString*)key
{
    return [self.commandDelegate.settings objectForKey:[key lowercaseString]];
}

- (void)pluginInitialize {

    NSString* appId = [[self.commandDelegate settings] objectForKey:@"wechatappid"];
    if (appId){
        _wechatAppId = appId;
        [WXApi registerApp: appId];
    }

    _persentAnimation = [PersentAnimation new];
    _dismissAnimation = [DismissAnimation new];
    _swapTransition = [SwapeRightInteractiveTransition new];
}

- (NSString *)navBarColor {
    if (_navBarColor == nil) {
        _navBarColor = [self settingForKey:@"NativeWebViewNavBarColor"];
    }
    return _navBarColor;
}

- (NSString *)progressBarColor {
    if (_progressBarColor == nil) {
        _progressBarColor = [self settingForKey:@"NativeWebViewProgressBarColor"];
    }
    return _progressBarColor;
}

- (NSString *)iconButtonColor {
    if (_iconButtonColor == nil) {
        _iconButtonColor = [self settingForKey:@"NativeWebViewIconButtonColor"];
    }
    return _iconButtonColor;
}

- (void)open:(CDVInvokedUrlCommand *)command {
    self.url = [command argumentAtIndex:0];
    BOOL enableShare = [[command argumentAtIndex:1] boolValue];
    NSDictionary *shareParams = [command argumentAtIndex:2];

    if (enableShare && shareParams) {
        self.webviewTitle = [shareParams valueForKey:@"title"];
        self.thumbImageUrl = [shareParams valueForKey:@"thumbImageUrl"];
    }

    CDVNativeWebViewController *nwVC = [[CDVNativeWebViewController alloc] initWithUrl:self.url];
    nwVC.navBarColor = self.navBarColor ? [CDVNativeWebViewUtils getColorByHexString:self.navBarColor] : nil;
    nwVC.progressBarColor = self.progressBarColor ? [CDVNativeWebViewUtils getColorByHexString:self.progressBarColor] : nil;
    nwVC.iconButtonColor = self.iconButtonColor ? [CDVNativeWebViewUtils getColorByHexString:self.iconButtonColor] : nil;
    nwVC.enableShare = enableShare;
    nwVC.delegate = self;
    [nwVC start];
    UINavigationController *nav = [[UINavigationController alloc] initWithRootViewController:nwVC];
    nav.transitioningDelegate = self;
    // Add gesture
    [self.swapTransition wireToViewController:nav];

    dispatch_async(dispatch_get_main_queue(), ^{
        CGRect frame = [[UIScreen mainScreen] bounds];
        UIWindow *tmpWindow = [[UIWindow alloc] initWithFrame:frame];
        UIViewController *tmpVC = [[UIViewController alloc] init];
        tmpVC.transitioningDelegate = self;
        [tmpWindow setRootViewController:tmpVC];
        [tmpWindow setWindowLevel:UIWindowLevelNormal];
        [tmpWindow makeKeyAndVisible];
        [tmpVC presentViewController:nav animated:YES completion:nil];
    });
}

#pragma mark CDVShareDelegate

- (SendMessageToWXReq *)getConfigureWechatReq {
    WXMediaMessage *message = [WXMediaMessage message];
    message.title = self.webviewTitle? self.webviewTitle : @"资讯";
    if (self.thumbImageUrl) {
        NSData *data = [NSData dataWithContentsOfURL:[NSURL URLWithString:self.thumbImageUrl]];
        [message setThumbImage:[UIImage imageWithData:data]];
    }
    WXWebpageObject *webpageObject = [WXWebpageObject object];
    webpageObject.webpageUrl = self.url;
    message.mediaObject = webpageObject;

    SendMessageToWXReq *req = [[SendMessageToWXReq alloc] init];
    req.bText = NO;
    req.message = message;

    return req;
}

- (void)shareToWechatFriend {
    SendMessageToWXReq *req = [self getConfigureWechatReq];
    req.scene = WXSceneSession;

    [WXApi sendReq:req];
}

- (void)shareToWechatTimeline {
    SendMessageToWXReq *req = [self getConfigureWechatReq];
    req.scene = WXSceneTimeline;

    [WXApi sendReq:req];
}

#pragma mark UIViewControllerTransitioningDelegate

- (id <UIViewControllerAnimatedTransitioning>)animationControllerForPresentedController:(UIViewController *)presented presentingController:(UIViewController *)presenting sourceController:(UIViewController *)source {
    return self.persentAnimation;
}

- (id <UIViewControllerAnimatedTransitioning>)animationControllerForDismissedController:(UIViewController *)dismissed {
    return self.dismissAnimation;
}


- (id <UIViewControllerInteractiveTransitioning>)interactionControllerForDismissal:(id<UIViewControllerAnimatedTransitioning>)animator {
    return self.swapTransition.interacting ? self.swapTransition : nil;
}

@end


@interface CDVNativeWebViewController()
@property (nonatomic, strong) NSURL *url;
@property (nonatomic, strong) WKWebView *webView;
@property (nonatomic, strong) CALayer *progressLayer;
@property (nonatomic, strong) UIBarButtonItem *backButton;
@property (nonatomic, strong) UIBarButtonItem *closeButton;
@property (nonatomic, strong) UIView *shadeView;
@property (nonatomic, strong) UIView *shareView;
@property (nonatomic, strong) NSTimer *countTimer;
@end

@implementation CDVNativeWebViewController

- (id)initWithUrl:(NSString *)url {
    self = [super init];

    if (self != nil) {
        NSCharacterSet *characterSet = [NSCharacterSet characterSetWithCharactersInString:@"`%^{}\"[]|\\<>"].invertedSet;
        NSString *stringURL = [url stringByAddingPercentEncodingWithAllowedCharacters:characterSet];
        _url = [NSURL URLWithString:stringURL];
    }

    return self;
}

- (UIColor *)navBarColor {
    return _navBarColor? _navBarColor : [UIColor whiteColor];
}

- (UIColor *)progressBarColor {
    return _progressBarColor? _progressBarColor : [UIColor blueColor];
}

- (UIColor *)iconButtonColor {
    return _iconButtonColor? _iconButtonColor : [UIColor blueColor];
}

- (void)start {
    [self createViews];
}

- (void)createViews {
    // 1. Create WKWebView and add progress observer
    self.webView = [[WKWebView alloc] initWithFrame:self.view.bounds];
    self.webView.frame = CGRectMake(0, 0, CGRectGetWidth(self.view.frame),CGRectGetHeight(self.view.frame) - 64.0);
    self.webView.navigationDelegate = self;
    self.hidesBottomBarWhenPushed = YES;
    self.edgesForExtendedLayout = UIRectEdgeNone;
    [self.webView addObserver:self forKeyPath:@"estimatedProgress" options:(NSKeyValueObservingOptionNew) context:nil];
    [self.view addSubview:self.webView];

    // 2. Create progressBar and progressLayer
    UIView *progress = [[UIView alloc] initWithFrame:CGRectMake(0, 0, CGRectGetWidth(self.view.frame), 3)];
    [self.view addSubview:progress];
    self.progressLayer = [CALayer layer];
    self.progressLayer.frame = CGRectMake(0, 0, 0, 3);
    self.progressLayer.backgroundColor = self.progressBarColor.CGColor;
    [progress.layer addSublayer:self.progressLayer];

    // 3. Create close and back button
    UIImage *backIcon = [IonIcons imageWithIcon:ion_ios_arrow_back size:30.0f color:self.iconButtonColor];
    self.backButton = [[UIBarButtonItem alloc] initWithImage:backIcon style:UIBarButtonItemStyleDone target:self action:@selector(goBack)];
    [self.backButton setImageInsets:UIEdgeInsetsMake(0, 0, 0, 15)];
    UIImage *closeIcon = [IonIcons imageWithIcon:ion_android_close size:30.0f color:self.iconButtonColor];
    self.closeButton = [[UIBarButtonItem alloc] initWithImage:closeIcon style:UIBarButtonItemStylePlain target:self action:@selector(close)];
    // Reduce items space
    [self.closeButton setImageInsets:UIEdgeInsetsMake(0, 0, 0, 45)];
    self.navigationItem.leftBarButtonItem = self.backButton;

    // 4. Add share buttom
    if (self.enableShare) {
        UIImage *shareIcon = [IonIcons imageWithIcon:ion_ios_more size:30.0f color:self.iconButtonColor];
        UIBarButtonItem *shareButton = [[UIBarButtonItem alloc] initWithImage:shareIcon style:UIBarButtonItemStylePlain target:self action:@selector(createShareModal)];
        UIBarButtonItem *fixedSpaceButton = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemFixedSpace target:nil action:nil];
        fixedSpaceButton.width = 20;
        self.navigationItem.rightBarButtonItems = @[shareButton, fixedSpaceButton];
    }

    [[UINavigationBar appearance] setBarTintColor:self.navBarColor];
    [[UINavigationBar appearance] setTranslucent:NO];

    // 5. Load url
    [self.webView loadRequest:[NSURLRequest requestWithURL:self.url]];
}

- (void)createShareModal {
    // 1. Create shade view
    UIView *rootView = self.navigationController.view;
    UIView *shadeView = [[UIView alloc] initWithFrame:rootView.bounds];
    shadeView.backgroundColor = [UIColor colorWithRed:0 green:0 blue:0 alpha:0.6];
    [rootView addSubview:shadeView];

    // 2. Create share view
    CGFloat height = 120.0;
    CGFloat y = CGRectGetHeight(rootView.bounds) - height;
    UIView *shareView = [[UIView alloc] initWithFrame:CGRectMake(0, y, SCREEN_WIDTH, height)];
    shareView.backgroundColor = [CDVNativeWebViewUtils getColorByHexString:@"#f6f6f6"];
    // 2.1 Add share title
    UILabel *title = [[UILabel alloc] initWithFrame:CGRectMake(0, 10, SCREEN_WIDTH, 20)];
    title.font = [UIFont systemFontOfSize:14.0];
    title.textColor = [UIColor grayColor];
    title.text = @"分享至";
    title.textAlignment = NSTextAlignmentCenter;
    [shareView addSubview:title];
    // 2.2 Add share ways
    UIImage *shareWayLogoImage;
    // 2.2.1 Share to wechat frient
    shareWayLogoImage = [UIImage imageNamed:@"shareToWechatFriend"];
    UIButton *shareToWechatFriendButton = [self createShareButtonWithImage:shareWayLogoImage title:@"微信" index:0];
    [shareToWechatFriendButton addTarget:self action:@selector(shareToWechatFriend) forControlEvents:UIControlEventTouchUpInside];
    [shareView addSubview:shareToWechatFriendButton];
    // 2.2.2 Share to wechat timeline
    shareWayLogoImage = [UIImage imageNamed:@"shareToWechatTimeline"];
    UIButton *shareToWechatTimelineButton = [self createShareButtonWithImage:shareWayLogoImage title:@"朋友圈" index:1];
    [shareToWechatTimelineButton addTarget:self action:@selector(shareToWechatTimeline) forControlEvents:UIControlEventTouchUpInside];
    [shareView addSubview:shareToWechatTimelineButton];


    // 3. Add tap gesture for views
    UITapGestureRecognizer *tap = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(closeShareView)];
    [shadeView addGestureRecognizer:tap];
    UITapGestureRecognizer *emptyTap = [[UITapGestureRecognizer alloc] initWithTarget:self action:nil];
    [shareView addGestureRecognizer:emptyTap];

    // 4. Set transition animation
    CATransition *transition = [CATransition animation];
    transition.duration = 0.3;
    transition.type = kCATransitionPush;
    transition.subtype = kCATransitionFromTop;
    [transition setTimingFunction:[CAMediaTimingFunction functionWithName:kCAMediaTimingFunctionEaseIn]];
    [shareView.layer addAnimation:transition forKey:nil];
    [shadeView addSubview:shareView];

    self.shadeView = shadeView;
    self.shareView = shareView;
}

- (UIButton *)createShareButtonWithImage:(UIImage *)image title:(NSString *)title index:(int)index {
    CGFloat x = 20.0;
    CGFloat y = 30.0;
    CGFloat width = 50.0;
    UIButton *shareButton = [UIButton buttonWithType:UIButtonTypeCustom];
    shareButton.frame = CGRectMake(index*(x + width) + x, y, width, width);
    [shareButton setImage:image forState:UIControlStateNormal];
    [shareButton setTitle:title forState:UIControlStateNormal];
    [shareButton setTitleColor:[UIColor grayColor] forState:UIControlStateNormal];
    shareButton.titleLabel.font = [UIFont systemFontOfSize:10.0];
    [shareButton setTitleEdgeInsets:UIEdgeInsetsMake(80.0, -100.0, 0, 0)];

    return shareButton;
}

- (void)shareToWechatFriend {
    [self.delegate shareToWechatFriend];
}

- (void)shareToWechatTimeline {
    [self.delegate shareToWechatTimeline];
}

- (void)closeShareView {

    if (self.shadeView != nil) {
        CATransition *transition = [CATransition animation];
        transition.duration = 0.2;
        transition.type = kCATransitionPush;
        transition.subtype = kCATransitionFromBottom;
        [transition setTimingFunction:[CAMediaTimingFunction functionWithName:kCAMediaTimingFunctionEaseOut]];
        [self.shareView.layer addAnimation:transition forKey:nil];
        self.shadeView.backgroundColor = [UIColor clearColor];
        self.shareView.hidden = YES;
        self.countTimer = [NSTimer scheduledTimerWithTimeInterval:0.3 target:self selector:@selector(removeShadeView) userInfo:nil repeats:NO];
        [[NSRunLoop mainRunLoop] addTimer:self.countTimer forMode:NSRunLoopCommonModes];
    }
}

- (void)removeShadeView {
    [self.shadeView removeFromSuperview];
    [self.countTimer invalidate];
    self.countTimer = nil;
}

- (void)observeValueForKeyPath:(NSString *)keyPath ofObject:(id)object change:(NSDictionary<NSKeyValueChangeKey,id> *)change context:(void *)context {
    if ([keyPath isEqualToString:@"estimatedProgress"]) {
        self.progressLayer.opacity = 1;
        self.progressLayer.frame = CGRectMake(0, 0, self.view.bounds.size.width * [change[NSKeyValueChangeNewKey] floatValue], 3);
        if ([change[NSKeyValueChangeNewKey] floatValue] == 1) {
            dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(.2 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
                self.progressLayer.opacity = 0;
            });
            dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(.8 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
                self.progressLayer.frame = CGRectMake(0, 0, 0, 3);
            });
        }
    } else {
        [super observeValueForKeyPath:keyPath ofObject:object change:change context:context];
    }
}

- (void)goBack {
    if (self.webView.canGoBack) {
        [self.webView goBack];
    } else {
        [self close];
    }
}

- (void)close {
    dispatch_async(dispatch_get_main_queue(), ^{
        [self dismissViewControllerAnimated:YES completion:nil];
    });
}

#pragma mark WKNavigationDelegate

- (void)webView:(WKWebView *)webView didCommitNavigation:(WKNavigation *)navigation {
    if (!self.navigationItem.title) {
        self.navigationItem.title = @"加载中...";
    }

    if (self.webView.canGoBack && [self.navigationItem.leftBarButtonItems count] == 1) {
        UIBarButtonItem *fixedSpaceButton = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemFixedSpace target:nil action:nil];
        fixedSpaceButton.width = 20;
        [self.navigationItem setLeftBarButtonItems:@[self.backButton, self.closeButton]];
        [self.navigationItem setRightBarButtonItems:@[fixedSpaceButton, fixedSpaceButton, fixedSpaceButton]];
    }
}

- (void)webView:(WKWebView *)webView didFinishNavigation:(WKNavigation *)navigation {
    if (self.delegate.webviewTitle == nil) {
        self.navigationItem.title = self.webView.title;
    } else {
        self.navigationItem.title = self.delegate.webviewTitle;
    }
}

- (void)webView:(WKWebView *)webView decidePolicyForNavigationAction:(WKNavigationAction *)navigationAction decisionHandler:(void (^)(WKNavigationActionPolicy))decisionHandler {

    NSString *urlStr = [navigationAction.request.URL.absoluteString stringByRemovingPercentEncoding];
    if ([urlStr containsString:@"alipay://"] || [urlStr containsString:@"weixin://"]) {
        [[UIApplication sharedApplication] openURL:navigationAction.request.URL options:@{} completionHandler:^(BOOL success) {
        }];
    }

    decisionHandler(WKNavigationActionPolicyAllow);
}

@end

@implementation  CDVNativeWebViewUtils

+ (UIColor *)getColorByHexString:(NSString *)hexString {
    unsigned int rgbValue = 0;
    NSScanner *scanner = [[NSScanner alloc] initWithString:hexString];
    [scanner setScanLocation:1];
    [scanner scanHexInt:&rgbValue];

    return [UIColor colorWithRed:((rgbValue & 0xFF0000) >> 16)/255.0 green:((rgbValue & 0xFF00) >> 8)/255.0 blue:(rgbValue & 0xFF)/255.0 alpha:1.0];
}

@end

@implementation PersentAnimation

- (NSTimeInterval)transitionDuration:(id<UIViewControllerContextTransitioning>)transitionContext {
    return 0.25f;
}

- (void)animateTransition:(id<UIViewControllerContextTransitioning>)transitionContext {
    // 1. Get controller from transition context
    UIViewController *toVC = [transitionContext viewControllerForKey:UITransitionContextToViewControllerKey];

    //2. Set init frame for toVC
    CGRect finalFrame = [transitionContext finalFrameForViewController:toVC];
    toVC.view.frame = CGRectOffset(finalFrame, SCREEN_WIDTH, 0);

    // 3. Add toVC's view to containerView
    UIView *containerView = [transitionContext containerView];
    [containerView addSubview:toVC.view];

    // 4. Do animate now
    NSTimeInterval duration = [self transitionDuration:transitionContext];
    [UIView animateWithDuration:duration animations:^{
        toVC.view.frame = finalFrame;
    } completion:^(BOOL finished) {
        [transitionContext completeTransition:YES];
    }];
}

@end

@implementation DismissAnimation

- (NSTimeInterval)transitionDuration:(id<UIViewControllerContextTransitioning>)transitionContext {
    return 0.25f;
}

- (void)animateTransition:(id<UIViewControllerContextTransitioning>)transitionContext {
    // 1. Get controllers from transition context
    UIViewController *fromVC = [transitionContext viewControllerForKey:UITransitionContextFromViewControllerKey];
    UIViewController *toVC = [transitionContext viewControllerForKey:UITransitionContextFromViewControllerKey];

    // 2. Set init frame for fromVC
    CGRect initFrame = [transitionContext initialFrameForViewController:fromVC];
    CGRect finalFrame = CGRectOffset(initFrame, SCREEN_WIDTH, 0);

    // 3. Add target view to the container, and move it to back
    UIView *containerView = [transitionContext containerView];
    [containerView addSubview:toVC.view];
    [containerView sendSubviewToBack:toVC.view];

    // 4. Do animate now
    NSTimeInterval duration = [self transitionDuration:transitionContext];
    [UIView animateWithDuration:duration animations:^{
        fromVC.view.frame = finalFrame;
    } completion:^(BOOL finished) {
        [transitionContext completeTransition:![transitionContext transitionWasCancelled]];
    }];
}

@end

@interface SwapeRightInteractiveTransition()
@property (nonatomic, assign) BOOL shouldComplete;
@property (nonatomic, strong) UIViewController *presentVC;
@end

@implementation SwapeRightInteractiveTransition

- (void)wireToViewController:(UIViewController *)viewController {
    self.presentVC = viewController;
    [self prepareGestureRecognizerInView:viewController.view];
}

- (void)prepareGestureRecognizerInView:(UIView *)view {
    UIPanGestureRecognizer *gesture = [[UIPanGestureRecognizer alloc] initWithTarget:self action:@selector(handleGesture:)];
    [view addGestureRecognizer:gesture];
}

- (CGFloat)completionSpeed {
    return 1 - self.percentComplete;
}

- (void)handleGesture:(UIPanGestureRecognizer *)gestureRecognizer {
    CGPoint translation = [gestureRecognizer translationInView:gestureRecognizer.view.superview];
    switch (gestureRecognizer.state) {
        case UIGestureRecognizerStateBegan: {
            // 1. Mark the interacting falg. Used when supplying it in delegate
            self.interacting = YES;
            [self.presentVC dismissViewControllerAnimated:YES completion:nil];
            break;
        }
        case UIGestureRecognizerStateChanged:{
            // 2. Calculate the percentage of gesture
            CGRect screenRect = [[UIScreen mainScreen] bounds];
            CGFloat fraction = translation.x / screenRect.size.width;
            fraction = fmin(fmaxf(fraction, 0.0), 1.0);
            self.shouldComplete = (fraction > 0.5);
            [self updateInteractiveTransition:fraction];
            break;
        }
        case UIGestureRecognizerStateEnded:
        case UIGestureRecognizerStateCancelled: {
            // 3. Gesture over. Check if transition should happen or not
            self.interacting = NO;
            if (!self.shouldComplete || gestureRecognizer.state == UIGestureRecognizerStateCancelled) {
                [self cancelInteractiveTransition];
            } else {
                [self finishInteractiveTransition];
            }
            break;
        }
        default:
            break;
    }
}

@end










