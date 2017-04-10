package com.ledboot.core.interceptor;

/**
 * Created by ouyangxingyu198 on 17/4/10.
 */

public interface INetInterceptor {

    /**
     * 拦截请求头
     * @param obj
     */
    void interceptorRequestHead(Object obj);
}
