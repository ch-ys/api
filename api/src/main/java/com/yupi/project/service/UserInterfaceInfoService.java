package com.yupi.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.model.entity.UserInterfaceInfo;

/**
* @author chenmoys
* @description 针对表【user_interface_info(用户调用接口关系)】的数据库操作Service
* @createDate 2023-10-08 18:44:14
*/
public interface UserInterfaceInfoService extends IService<UserInterfaceInfo> {

    void validUserInterfaceInfo(UserInterfaceInfo userinterfaceinfo, boolean b);


}
