package com.yupi.service;


import com.yupi.model.entity.InterfaceInfo;

/**
* @author chenmoys
* @description 针对表【interface_info(接口信息)】的数据库操作Service
* @createDate 2023-10-07 12:33:38
*/
public interface InnerInterfaceInfoService {

    InterfaceInfo getInterfaceInfo(String method, String url);
}
