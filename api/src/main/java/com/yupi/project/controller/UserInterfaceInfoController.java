package com.yupi.project.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.yupi.model.entity.User;
import com.yupi.model.entity.UserInterfaceInfo;
import com.yupi.project.annotation.AuthCheck;
import com.yupi.project.common.BaseResponse;
import com.yupi.project.common.DeleteRequest;
import com.yupi.project.common.ErrorCode;
import com.yupi.project.common.ResultUtils;
import com.yupi.project.constant.CommonConstant;
import com.yupi.project.exception.BusinessException;
import com.yupi.project.model.dto.userinterfaceinfo.UserInterfaceInfoAddRequest;
import com.yupi.project.model.dto.userinterfaceinfo.UserInterfaceInfoQueryRequest;
import com.yupi.project.model.dto.userinterfaceinfo.UserInterfaceInfoUpdateRequest;
import com.yupi.project.service.UserService;
import com.yupi.project.service.UserInterfaceInfoService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

public class UserInterfaceInfoController {
    @Resource
    private UserInterfaceInfoService UserinterfaceinfoService;

    @Resource
    private UserService userService;


    /**
     * 创建
     *
     * @param UserinterfaceinfoAddRequest
     * @param request
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @PostMapping("/add")
    public BaseResponse<Long> addUserInterfaceInfo(@RequestBody UserInterfaceInfoAddRequest UserinterfaceinfoAddRequest, HttpServletRequest request) {
        if (UserinterfaceinfoAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        UserInterfaceInfo Userinterfaceinfo = new UserInterfaceInfo();
        BeanUtils.copyProperties(UserinterfaceinfoAddRequest, Userinterfaceinfo);
        // 校验
        UserinterfaceinfoService.validUserInterfaceInfo(Userinterfaceinfo, true);
        User loginUser = userService.getLoginUser(request);
        Userinterfaceinfo.setUserId(loginUser.getId());
        boolean result = UserinterfaceinfoService.save(Userinterfaceinfo);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        long newUserInterfaceInfoId = Userinterfaceinfo.getId();
        return ResultUtils.success(newUserInterfaceInfoId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUserInterfaceInfo(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        UserInterfaceInfo oldUserInterfaceInfo = UserinterfaceinfoService.getById(id);
        if (oldUserInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 仅本人或管理员可删除
        if (!oldUserInterfaceInfo.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = UserinterfaceinfoService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新
     *
     * @param UserinterfaceinfoUpdateRequest
     * @param request
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @PostMapping("/update")
    public BaseResponse<Boolean> updateUserInterfaceInfo(@RequestBody UserInterfaceInfoUpdateRequest UserinterfaceinfoUpdateRequest,
                                                     HttpServletRequest request) {
        if (UserinterfaceinfoUpdateRequest == null || UserinterfaceinfoUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        UserInterfaceInfo Userinterfaceinfo = new UserInterfaceInfo();
        BeanUtils.copyProperties(UserinterfaceinfoUpdateRequest, Userinterfaceinfo);
        // 参数校验
        UserinterfaceinfoService.validUserInterfaceInfo(Userinterfaceinfo, false);
        User user = userService.getLoginUser(request);
        long id = UserinterfaceinfoUpdateRequest.getId();
        // 判断是否存在
        UserInterfaceInfo oldUserInterfaceInfo = UserinterfaceinfoService.getById(id);
        if (oldUserInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 仅本人或管理员可修改
        if (!oldUserInterfaceInfo.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = UserinterfaceinfoService.updateById(Userinterfaceinfo);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @GetMapping("/get")
    public BaseResponse<UserInterfaceInfo> getUserInterfaceInfoById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        UserInterfaceInfo Userinterfaceinfo = UserinterfaceinfoService.getById(id);
        return ResultUtils.success(Userinterfaceinfo);
    }

    /**
     * 获取列表（仅管理员可使用）
     *
     * @param UserinterfaceinfoQueryRequest
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @GetMapping("/list")
    public BaseResponse<List<UserInterfaceInfo>> listUserInterfaceInfo(UserInterfaceInfoQueryRequest UserinterfaceinfoQueryRequest) {
        UserInterfaceInfo UserinterfaceinfoQuery = new UserInterfaceInfo();
        if (UserinterfaceinfoQueryRequest != null) {
            BeanUtils.copyProperties(UserinterfaceinfoQueryRequest, UserinterfaceinfoQuery);
        }
        QueryWrapper<UserInterfaceInfo> queryWrapper = new QueryWrapper<>(UserinterfaceinfoQuery);
        List<UserInterfaceInfo> UserinterfaceinfoList = UserinterfaceinfoService.list(queryWrapper);
        return ResultUtils.success(UserinterfaceinfoList);
    }

    /**
     * 分页获取列表
     *
     * @param UserinterfaceinfoQueryRequest
     * @param request
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @GetMapping("/list/page")
    public BaseResponse<Page<UserInterfaceInfo>> listUserInterfaceInfoByPage(UserInterfaceInfoQueryRequest UserinterfaceinfoQueryRequest, HttpServletRequest request) {
        if (UserinterfaceinfoQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        UserInterfaceInfo userInterfaceInfo = new UserInterfaceInfo();
        BeanUtils.copyProperties(UserinterfaceinfoQueryRequest, userInterfaceInfo);
        long current = UserinterfaceinfoQueryRequest.getCurrent();
        long size = UserinterfaceinfoQueryRequest.getPageSize();
        String sortField = UserinterfaceinfoQueryRequest.getSortField();
        String sortOrder = UserinterfaceinfoQueryRequest.getSortOrder();

        // 限制爬虫
        if (size > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<UserInterfaceInfo> queryWrapper = new QueryWrapper<>(userInterfaceInfo);
        queryWrapper.orderBy(StringUtils.isNotBlank(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);
        Page<UserInterfaceInfo> UserinterfaceinfoPage = UserinterfaceinfoService.page(new Page<>(current, size), queryWrapper);
        return ResultUtils.success(UserinterfaceinfoPage);
    }



}
