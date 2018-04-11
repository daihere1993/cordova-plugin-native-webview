//
//  CDVNativeWebView.m
//
//  Created by Luke Wuu on 09/04/2018.
//

#import "CDVNativeWebView.h"
#import <ionicons.h>

static UIColor *navBarColor;
static UIColor *progressBarColor;
static UIColor *iconButtonColor;

@interface CDVNativeWebView()
@property (nonatomic, strong) PersentAnimation *persentAnimation;
@property (nonatomic, strong) DismissAnimation *dismissAnimation;
@property (nonatomic, strong) SwapeRightInteractiveTransition *swapTransition;
@end

@implementation CDVNativeWebView

- (id)settingForKey:(NSString*)key
{
    return [self.commandDelegate.settings objectForKey:[key lowercaseString]];
}

- (UIColor *)getColorByHexString:(NSString *)hexString {
    unsigned int rgbValue = 0;
    NSScanner *scanner = [[NSScanner alloc] initWithString:hexString];
    [scanner setScanLocation:1];
    [scanner scanHexInt:&rgbValue];
    
    return [UIColor colorWithRed:((rgbValue & 0xFF0000) >> 16)/255.0 green:((rgbValue & 0xFF00) >> 8)/255.0 blue:(rgbValue & 0xFF)/255.0 alpha:1.0];
}

- (void)pluginInitialize {
    _persentAnimation = [PersentAnimation new];
    _dismissAnimation = [DismissAnimation new];
    _swapTransition = [SwapeRightInteractiveTransition new];
    
    NSString *navBarColor_string = [self settingForKey:@"NativeWebViewNavBarColor"];
    if (navBarColor_string) {
        navBarColor = [self getColorByHexString:navBarColor_string];
    } else {
        navBarColor = [UIColor whiteColor];
    }
    NSString *progressBarColor_string = [self settingForKey:@"NativeWebViewProgressBarColor"];
    if (progressBarColor_string) {
        progressBarColor = [self getColorByHexString:progressBarColor_string];
    } else {
        progressBarColor = [UIColor blueColor];
    }
    NSString *iconButtonColor_string = [self settingForKey:@"NativeWebViewIconButtonColor"];
    if (iconButtonColor_string) {
        iconButtonColor = [self getColorByHexString:iconButtonColor_string];
    } else {
        iconButtonColor = [UIColor blueColor];
    }
}

- (void)open:(CDVInvokedUrlCommand *)command {
    NSString *url = [command argumentAtIndex:0];
    NSDictionary *options = [command argumentAtIndex:1];
    
    CDVNativeWebViewController *nwVC = [[CDVNativeWebViewController alloc] initWithArgs:url options:options];
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
@end

@implementation CDVNativeWebViewController

- (id)initWithArgs:(NSString *)url options:(NSDictionary *)options {
    self = [super init];
    
    if (self != nil) {
        _url = [NSURL URLWithString:url];
        [self createViews];
    }
    
    return self;
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
    self.progressLayer.backgroundColor = progressBarColor.CGColor;
    [progress.layer addSublayer:self.progressLayer];
    
    // 3. Create close and back button
    UIImage *backIcon = [IonIcons imageWithIcon:ion_ios_arrow_back size:30.0f color:iconButtonColor];
    self.backButton = [[UIBarButtonItem alloc] initWithImage:backIcon style:UIBarButtonItemStyleDone target:self action:@selector(goBack)];
    [self.backButton setImageInsets:UIEdgeInsetsMake(0, 0, 0, 15)];
    UIImage *closeIcon = [IonIcons imageWithIcon:ion_android_close size:30.0f color:iconButtonColor];
    self.closeButton = [[UIBarButtonItem alloc] initWithImage:closeIcon style:UIBarButtonItemStylePlain target:self action:@selector(close)];
    // Reduce items space
    [self.closeButton setImageInsets:UIEdgeInsetsMake(0, 0, 0, 45)];
    self.navigationItem.leftBarButtonItem = self.backButton;
    UIBarButtonItem *fixedSpaceButton = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemFixedSpace target:nil action:nil];
    fixedSpaceButton.width = 20;
    
    self.navigationItem.rightBarButtonItems = @[fixedSpaceButton, fixedSpaceButton];
    [[UINavigationBar appearance] setBarTintColor:navBarColor];
    
    // 4. Load url
    [self.webView loadRequest:[NSURLRequest requestWithURL:self.url]];
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
    self.navigationItem.title = self.webView.title;
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

@implementation PersentAnimation

- (NSTimeInterval)transitionDuration:(id<UIViewControllerContextTransitioning>)transitionContext {
    return 0.25f;
}

- (void)animateTransition:(id<UIViewControllerContextTransitioning>)transitionContext {
    // 1. Get controller from transition context
    UIViewController *toVC = [transitionContext viewControllerForKey:UITransitionContextToViewControllerKey];
    
    //2. Set init frame for toVC
    CGRect screenBounds = [[UIScreen mainScreen] bounds];
    CGRect finalFrame = [transitionContext finalFrameForViewController:toVC];
    toVC.view.frame = CGRectOffset(finalFrame, screenBounds.size.width, 0);
    
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
    CGRect screenBounds = [[UIScreen mainScreen] bounds];
    CGRect initFrame = [transitionContext initialFrameForViewController:fromVC];
    CGRect finalFrame = CGRectOffset(initFrame, screenBounds.size.width, 0);
    
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










