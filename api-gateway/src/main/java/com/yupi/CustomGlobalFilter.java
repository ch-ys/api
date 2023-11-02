package com.yupi;


import com.yupi.model.entity.InterfaceInfo;
import com.yupi.model.entity.User;
import com.yupi.model.enums.InterfaceStatusEnum;
import com.yupi.service.InnerInterfaceInfoService;
import com.yupi.service.InnerUserInterfaceInfoService;
import com.yupi.service.InnerUserService;
import com.yupi.untils.SignUntils;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class CustomGlobalFilter implements GlobalFilter, Ordered {
    private static final List<String> IP_WHITE_LIST = Arrays.asList("127.0.0.1");
    private static final String HOST = "http://localhost:8123";
    private static final String AK= "AK-SK";
    @DubboReference
    private InnerUserInterfaceInfoService innerUserInterfaceInfoService;
    @DubboReference
    private InnerUserService innerUserService;
    @DubboReference
    private InnerInterfaceInfoService interfaceInfoService;

//    @Resource
//    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        //2请求日志
        String path = request.getPath().value();
        String method = request.getMethod().toString();
        String source = request.getLocalAddress().getHostString();
        log.info("用户标识:" + request.getId());
        log.info("用户的方法:" + method);
        log.info("用户的请求参数:" + request.getQueryParams());
        log.info("用户请求路径:" + path);
        log.info("用户来源地址:" + source);

//        //3名单
//        if (!IP_WHITE_LIST.contains(source)){
//            log.info("黑名单IP");
//            return handleNoAUTH(response);
//        }

        //5鉴权
        HttpHeaders headers = request.getHeaders();
        String accessKey = headers.getFirst("accessKey");
        String body = headers.getFirst("body");
        String nonce = headers.getFirst("nonce");
        String timestamp = headers.getFirst("timestamp");
        String sign = headers.getFirst("sign");

        User user = null;
        try {
            user = innerUserService.getUser(accessKey);
        } catch (Exception e) {
            log.info("getUser error", e);
        }
        if (user == null){
            log.info("用户名或密码错误");
            return handleNoAUTH(response);
        }
        //验证随机数防止重放
        if (nonce.length() > 5L){
            log.info("信息错误");
            return handleNoAUTH(response);
        }
        final long FIVE_MINS = 60L;
        Long time = System.currentTimeMillis() / 1000 - Long.parseLong(timestamp);
        if (time > FIVE_MINS ){
            log.info("时间过期");
            return handleNoAUTH(response);
        }

        String secretKey = user.getSecretKey();
        String valid = SignUntils.Sign(body,secretKey);
        if (!valid.equals(sign)){
            log.info("签名错误");
            return handleNoAUTH(response);
        }

        //4判断接口状态
        InterfaceInfo interfaceInfo = null;
        String url = HOST + path;
        try {
            interfaceInfo = interfaceInfoService.getInterfaceInfo(method,url);
        } catch (Exception e) {
            log.info("getInterfaceInfo error", e);
        }
        if (interfaceInfo == null){
            return handleInvokeError(response);
        }
        //判断是否上线
        if (interfaceInfo.getStatus() == InterfaceStatusEnum.OFFLINE.getValue()){
            return handleInvokeError(response);
        }

        // 5 判断调用次数
        long interfaceInfoId = interfaceInfo.getId();
        long userId = user.getId();
        Integer leftNum = null;
        synchronized(this) {
            try {
                leftNum = innerUserInterfaceInfoService.getUserInterfaceInfoByIds(interfaceInfoId,userId).getLeftNum();
            } catch (Exception e) {
                log.info("getUserInterfaceInfoByIds error", e);
            }
            if (leftNum == null) {
                log.info("未查询到次数记录");
                return handleNoAUTH(response);
            }
            if (leftNum < 1 ){
                log.info("剩余次数不够");
                return handleNoAUTH(response);
            }

            //6路由转发 //7响应日志 //8成功返回 调用次数+1 //9失败标准返回
            return handleResponse(exchange,chain,interfaceInfoId,userId);
        }
//        try {
//            leftNum = innerUserInterfaceInfoService.getUserInterfaceInfoByIds(interfaceInfoId,userId).getLeftNum();
//        } catch (Exception e) {
//            log.info("getUserInterfaceInfoByIds error", e);
//        }
//        if (leftNum == null) {
//            log.info("未查询到次数记录");
//            return handleNoAUTH(response);
//        }
//        if (leftNum < 1 ){
//            log.info("剩余次数不够");
//            return handleNoAUTH(response);
//        }
//
//        //6路由转发 //7响应日志 //8成功返回 调用次数+1 //9失败标准返回
//        return handleResponse(exchange,chain,interfaceInfoId,userId);
    }

    public Mono<Void> handleResponse(ServerWebExchange exchange, GatewayFilterChain chain, long interfaceInfoId, long userId) {
        try {
            ServerHttpResponse originalResponse = exchange.getResponse();
            // 缓存数据的工厂
            DataBufferFactory bufferFactory = originalResponse.bufferFactory();
            // 拿到响应码
            HttpStatus statusCode = originalResponse.getStatusCode();
            if (statusCode == HttpStatus.OK) {
                // 装饰，增强能力
                ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
                    // 等调用完转发的接口后才会执行
                    @Override
                    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                        log.info("body instanceof Flux: {}", (body instanceof Flux));
                        if (body instanceof Flux) {
                            Flux<? extends DataBuffer> fluxBody = Flux.from(body);
                            // 往返回值里写数据
                            // 拼接字符串
                            return super.writeWith(
                                    fluxBody.map(dataBuffer -> {
                                        // 7. 调用成功，接口调用次数 + 1 invokeCount
                                        try {
                                            innerUserInterfaceInfoService.invokeCount(interfaceInfoId, userId);
                                        } catch (Exception e) {
                                            log.error("invokeCount error", e);
                                        }
                                        byte[] content = new byte[dataBuffer.readableByteCount()];
                                        dataBuffer.read(content);
                                        DataBufferUtils.release(dataBuffer);//释放掉内存
                                        // 构建日志
                                        StringBuilder sb2 = new StringBuilder(200);
                                        List<Object> rspArgs = new ArrayList<>();
                                        rspArgs.add(originalResponse.getStatusCode());
                                        String data = new String(content, StandardCharsets.UTF_8); //data
                                        sb2.append(data);
                                        // 打印日志
                                        log.info("响应结果：" + data);
                                        return bufferFactory.wrap(content);
                                    }));
                        } else {
                            // 8. 调用失败，返回一个规范的错误码
                            log.error("<--- {} 响应code异常", getStatusCode());
                        }
                        return super.writeWith(body);
                    }
                };
                // 设置 response 对象为装饰过的
                return chain.filter(exchange.mutate().response(decoratedResponse).build());
            }
            return chain.filter(exchange); // 降级处理返回数据
        } catch (Exception e) {
            log.error("网关处理响应异常" + e);
            return chain.filter(exchange);
        }
    }


    @Override
    public int getOrder() {
        return -1;
    }

    public Mono<Void> handleNoAUTH(ServerHttpResponse response){
        response.setStatusCode(HttpStatus.FORBIDDEN);
        return response.setComplete();
    }

    public Mono<Void> handleInvokeError(ServerHttpResponse response){
        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        return response.setComplete();
    }

}
