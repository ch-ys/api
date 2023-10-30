package com.yupi.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.model.entity.UserInterfaceInfo;
import com.yupi.project.common.ErrorCode;
import com.yupi.project.exception.BusinessException;
import com.yupi.project.service.UserInterfaceInfoService;
import com.yupi.project.mapper.UserInterfaceInfoMapper;
import com.yupi.service.InnerInterfaceInfoService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
* @author chenmoys
* @description 针对表【user_interface_info(用户调用接口关系)】的数据库操作Service实现
* @createDate 2023-10-08 18:44:14
*/
@Service
public class UserInterfaceInfoServiceImpl extends ServiceImpl<UserInterfaceInfoMapper, UserInterfaceInfo>
    implements UserInterfaceInfoService{

    @Resource UserInterfaceInfoMapper userInterfaceInfoMapper;

    @Override
    public void validUserInterfaceInfo(UserInterfaceInfo userInterfaceinfo, boolean add) {
        if (userInterfaceinfo == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        if (add) {
            if (userInterfaceinfo.getInterfaceInfoId() <= 0 || userInterfaceinfo.getUserId() <= 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"接口或用户不存在");
            }
        }
    }


}




