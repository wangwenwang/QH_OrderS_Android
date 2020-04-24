
package com.kaidongyuan.qh_orders_android.track;

/**
 * Created by changwei on 2015/8/30.
 */
public interface AsyncHttpCallback {

  /**
   * 网络请求的回调方法
   * @param msg 返回的信息 出现错误msg 为"error"
   * @param request_tag 网络请求的TAG
   */
  void postSuccessMsg(String msg, String request_tag);
}
