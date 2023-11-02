package com.yupi.project.service.impl;

import cn.hutool.Hutool;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.model.entity.InterfaceInfo;
import com.yupi.project.common.ErrorCode;
import com.yupi.project.constant.CacheLogicExpireConstant;
import com.yupi.project.exception.BusinessException;
import com.yupi.project.mapper.InterfaceInfoMapper;
import com.yupi.project.service.InterfaceInfoService;
import com.yupi.untils.CacheConstant;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.auditing.CurrentDateTimeProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
* @author chenmoys
* @description 针对表【interface_info(接口信息)】的数据库操作Service实现
* @createDate 2023-10-07 12:33:38
*/
@Service
public class InterfaceInfoServiceImpl extends ServiceImpl<InterfaceInfoMapper, InterfaceInfo>
    implements InterfaceInfoService {

    @Resource
    private StringRedisTemplate StringRedisTemplate;

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
    @Override
    public void validInterfaceInfo(InterfaceInfo interfaceinfo, boolean add) {
        if (interfaceinfo == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String name = interfaceinfo.getName();
        // 创建时，所有参数必须非空
        if (add) {
            if (StringUtils.isAnyBlank(name)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR);
            }
        }
        if (StringUtils.isNotBlank(name) && name.length() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "名称过长");
        }
    }
    //缓存
    @Override
    public InterfaceInfo cacheGetById(long id) {
        //查询缓存
        String cacheKey = CacheConstant.CacheName + id;
        String json = StringRedisTemplate.opsForValue().get(cacheKey);
        //命中返回
        if (StringUtils.isNotBlank(json)){
            return JSONUtil.toBean(json, InterfaceInfo.class);
        }
        //未命中查询数据库
        //查询到返回并添加数据库
        InterfaceInfo interfaceInfo = getById(id);
        String jsonStr = JSONUtil.toJsonStr(interfaceInfo);
        StringRedisTemplate.opsForValue().set(cacheKey,jsonStr,CacheConstant.CacheTime, TimeUnit.MINUTES);
        //未查询到返回null
        return interfaceInfo;
    }

    //缓存一致性
    @Override
    @Transactional
    public boolean updateByIdDeleteCache(InterfaceInfo interfaceinfo) {
        boolean tag = updateById(interfaceinfo);
        //更新数据失败
        if (!tag) {
           return false;
        }
        //更新数据成功删除缓存
        String cacheKey = CacheConstant.CacheName + interfaceinfo.getId();
        Boolean delete = StringRedisTemplate.delete(cacheKey);

        return delete;
    }

    //缓存击穿互斥锁方案
    @Override
    public InterfaceInfo lockCacheGetById(long id) throws InterruptedException {
        //查询缓存
        String cacheKey = CacheConstant.CacheName + id;
        String json = StringRedisTemplate.opsForValue().get(cacheKey);
        //命中返回
        if (StringUtils.isNotBlank(json)){
            return JSONUtil.toBean(json, InterfaceInfo.class);
        }
        //未命中
        //获取锁
        Boolean tag = StringRedisTemplate.opsForValue().setIfAbsent("Lock:interfaceInfo:" + id,"1",10L,TimeUnit.SECONDS);
        //获取锁失败
        if (!tag){
            //等待后重新进行程序
            Thread.sleep(50L);
            lockCacheGetById(id);
        }
        //获取锁成功
        //二次检测
        String json2 = StringRedisTemplate.opsForValue().get(cacheKey);
        //命中返回
        if (StringUtils.isNotBlank(json2)){
            return JSONUtil.toBean(json2, InterfaceInfo.class);
        }
        // 查询数据库
        //未查询到返回null 从列表获取id无需考虑穿透
        //查询到返回并添加数据库
        InterfaceInfo interfaceInfo = getById(id);
        String jsonStr = JSONUtil.toJsonStr(interfaceInfo);
        StringRedisTemplate.opsForValue().set(cacheKey,jsonStr,CacheConstant.CacheTime, TimeUnit.MINUTES);
        //释放锁
        StringRedisTemplate.delete("Lock:interfaceInfo" + id);
        return interfaceInfo;
    }

    //列表页缓存
    @Override
    public Page<InterfaceInfo> cachePage(Page<InterfaceInfo> objectPage, QueryWrapper<InterfaceInfo> queryWrapper) {
        //查询缓存
        String cacheKey = CacheConstant.CacheName +"c" + objectPage.getCurrent() + "s" +objectPage.getSize();
        String json = StringRedisTemplate.opsForValue().get(cacheKey);
        //命中返回
        if (StringUtils.isNotBlank(json)){
            return JSONUtil.toBean(json, Page.class);
        }
        //未命中查询数据库
        //查询到返回并添加数据库
        Page<InterfaceInfo> interfaceinfoPage = this.page(objectPage,queryWrapper);
        String jsonStr = JSONUtil.toJsonStr(interfaceinfoPage);
        StringRedisTemplate.opsForValue().set(cacheKey,jsonStr,CacheConstant.ListCacheTime, TimeUnit.MINUTES);
        //未查询到返回null
        return interfaceinfoPage;

    }
    //缓存击穿逻辑过期方案
    @Override
    public InterfaceInfo logicCacheGetById(long id) {
        //查询缓存
        String cacheKey = CacheConstant.CacheName + id;
        String json = StringRedisTemplate.opsForValue().get(cacheKey);
        //未命中 默认存入热key 未命中则么无记录
        if (StringUtils.isBlank(json)){
            return null;
        }
        //命中查看是否过期
        CacheLogicExpireConstant bean = JSONUtil.toBean(json,CacheLogicExpireConstant.class);
        LocalDateTime expireTime = bean.getExpireTime();
        InterfaceInfo interfaceInfo = bean.getData();
        //逻辑时间未过期
        if (expireTime.isAfter(LocalDateTime.now())){
            return interfaceInfo;
        }
        //逻辑时间过期
        //获取锁
        Boolean tag = StringRedisTemplate.opsForValue().setIfAbsent("Lock:interfaceInfo:" + id,"1",10L,TimeUnit.SECONDS);
        //获取锁失败
        if (!tag){
            //返回逻辑过期数据
            return interfaceInfo;
        }
        //获取锁成功
        //二次检测
        String json2 = StringRedisTemplate.opsForValue().get(cacheKey);
        CacheLogicExpireConstant bean2 = JSONUtil.toBean(json, CacheLogicExpireConstant.class);
        LocalDateTime expireTime2 = bean2.getExpireTime();
        //二次检测逻辑时间未过期直接返回
        if (expireTime2.isAfter(LocalDateTime.now())){
            //释放锁并返回
            StringRedisTemplate.delete("Lock:interfaceInfo" + id);
            InterfaceInfo interfaceInfo2 =(InterfaceInfo) bean2.getData();
            return interfaceInfo2;
        }
        //新开线程重建缓存
        //  查询数据库
        //  未查询到返回null 从列表获取id无需考虑穿透
        //  查询到返回并添加缓存
        CACHE_REBUILD_EXECUTOR.submit(()->{
            InterfaceInfo data = getById(id);
            CacheLogicExpireConstant interfaceInfoCacheLogicExpireConstant = new CacheLogicExpireConstant();
            interfaceInfoCacheLogicExpireConstant.setData(data);
            interfaceInfoCacheLogicExpireConstant.setExpireTime(LocalDateTime.now().plusMinutes(CacheConstant.CacheTime));
            String jsonStr = JSONUtil.toJsonStr(interfaceInfoCacheLogicExpireConstant);
            StringRedisTemplate.opsForValue().set(cacheKey,jsonStr);
            //  释放锁
            StringRedisTemplate.delete("Lock:interfaceInfo" + id);
        });
        //  直接返回旧数据
        return interfaceInfo;
    }
}




