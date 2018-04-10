//
//  CDVNativeWebView.h
//
//  Created by Luke Wuu on 09/04/2018.
//
#import <Cordova/CDVPlugin.h>
#import <WebKit/WebKit.h>

@interface CDVNativeWebView : CDVPlugin <UIViewControllerTransitioningDelegate>
- (void)open:(CDVInvokedUrlCommand *)command;
@end

@interface CDVNativeWebViewController : UIViewController <WKNavigationDelegate>
- (id)initWithArgs:(NSString *)url options:(NSDictionary *)options;
@end

@interface PersentAnimation : NSObject <UIViewControllerAnimatedTransitioning>
@end


@interface DismissAnimation : NSObject <UIViewControllerAnimatedTransitioning>
@end

@interface SwapeRightInteractiveTransition : UIPercentDrivenInteractiveTransition
@property (nonatomic, assign) BOOL interacting;
- (void)wireToViewController:(UIViewController *)viewController;
@end

