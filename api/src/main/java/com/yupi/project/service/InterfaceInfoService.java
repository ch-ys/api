package com.yupi.project.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.model.entity.InterfaceInfo;

/**
* @author chenmoys
* @description 针对表【interface_info(接口信息)】的数据库操作Service
* @createDate 2023-10-07 12:33:38
*/
public interface InterfaceInfoService extends IService<InterfaceInfo> {

    void validInterfaceInfo(InterfaceInfo interfaceinfo, boolean b);

    InterfaceInfo cacheGetById(long id);

    boolean updateByIdDeleteCache(InterfaceInfo interfaceinfo);

    InterfaceInfo lockCacheGetById(long id) throws InterruptedException;

    Page<InterfaceInfo> cachePage(Page<InterfaceInfo> objectPage, QueryWrapper<InterfaceInfo> queryWrapper);

    InterfaceInfo logicCacheGetById(long id);
}
