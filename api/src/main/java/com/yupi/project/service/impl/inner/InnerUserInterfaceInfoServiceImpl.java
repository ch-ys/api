package com.yupi.project.service.impl.inner;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.model.entity.UserInterfaceInfo;
import com.yupi.project.common.ErrorCode;
import com.yupi.project.exception.BusinessException;
import com.yupi.project.mapper.UserInterfaceInfoMapper;
import com.yupi.service.InnerUserInterfaceInfoService;
import org.apache.dubbo.config.annotation.DubboService;



import javax.annotation.Resource;


@DubboService
public class InnerUserInterfaceInfoServiceImpl extends ServiceImpl<UserInterfaceInfoMapper, UserInterfaceInfo>
        implements InnerUserInterfaceInfoService {
    @Resource
    private UserInterfaceInfoMapper userInterfaceInfoMapper;
    @Override
    public Boolean invokeCount(long interfaceInfoId, long userId) {
        if (interfaceInfoId <= 0 || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        UpdateWrapper<UserInterfaceInfo> UpdateWrapper = new UpdateWrapper<>();
        UpdateWrapper.eq("interfaceInfoId",interfaceInfoId);
        UpdateWrapper.eq("userId",userId);
        UpdateWrapper.gt("leftNum",1L);
        UpdateWrapper.setSql("leftNum = leftNum - 1 , totalNum = totalNum + 1");
        return  update(UpdateWrapper);
    }

    @Override
    public UserInterfaceInfo getUserInterfaceInfoByIds(long interfaceInfoId, long userId) {
        QueryWrapper<UserInterfaceInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("interfaceInfoId",interfaceInfoId);
        queryWrapper.eq("userId",userId);
        return userInterfaceInfoMapper.selectOne(queryWrapper);
    }
}
