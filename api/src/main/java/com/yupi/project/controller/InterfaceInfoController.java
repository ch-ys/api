package com.yupi.project.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.client.apiClient;
import com.yupi.model.entity.InterfaceInfo;
import com.yupi.model.entity.User;
import com.yupi.project.annotation.AuthCheck;
import com.yupi.project.common.BaseResponse;
import com.yupi.project.common.DeleteRequest;
import com.yupi.project.common.ErrorCode;
import com.yupi.project.common.ResultUtils;
import com.yupi.project.constant.CommonConstant;
import com.yupi.project.exception.BusinessException;
import com.yupi.project.model.dto.interfaceinfo.InterfaceInfoAddRequest;
import com.yupi.project.model.dto.interfaceinfo.InterfaceInfoInvokeRequest;
import com.yupi.project.model.dto.interfaceinfo.InterfaceInfoQueryRequest;
import com.yupi.project.model.dto.interfaceinfo.InterfaceInfoUpdateRequest;
import com.yupi.project.model.enums.InterfaceStatusEnum;
import com.yupi.project.service.InterfaceInfoService;
import com.yupi.project.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;


@RestController
@RequestMapping("/interfaceInfo")
@Slf4j
public class InterfaceInfoController {

    @Resource
    private InterfaceInfoService interfaceinfoService;

    @Resource
    private UserService userService;

    @Resource
    private apiClient apiClient;

    // region 增删改查

    /**
     * 创建
     *
     * @param interfaceinfoAddRequest
     * @param request
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @PostMapping("/add")
    public BaseResponse<Long> addInterfaceInfo(@RequestBody InterfaceInfoAddRequest interfaceinfoAddRequest, HttpServletRequest request) {
        if (interfaceinfoAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        InterfaceInfo interfaceinfo = new InterfaceInfo();
        BeanUtils.copyProperties(interfaceinfoAddRequest, interfaceinfo);
        // 校验
        interfaceinfoService.validInterfaceInfo(interfaceinfo, true);
        User loginUser = userService.getLoginUser(request);
        interfaceinfo.setUserId(loginUser.getId());
        boolean result = interfaceinfoService.save(interfaceinfo);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        long newInterfaceInfoId = interfaceinfo.getId();
        return ResultUtils.success(newInterfaceInfoId);
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
    public BaseResponse<Boolean> deleteInterfaceInfo(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        InterfaceInfo oldInterfaceInfo = interfaceinfoService.getById(id);
        if (oldInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 仅本人或管理员可删除
        if (!oldInterfaceInfo.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = interfaceinfoService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新
     *
     * @param interfaceinfoUpdateRequest
     * @param request
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @PostMapping("/update")
    public BaseResponse<Boolean> updateInterfaceInfo(@RequestBody InterfaceInfoUpdateRequest interfaceinfoUpdateRequest,
                                            HttpServletRequest request) {
        if (interfaceinfoUpdateRequest == null || interfaceinfoUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        InterfaceInfo interfaceinfo = new InterfaceInfo();
        BeanUtils.copyProperties(interfaceinfoUpdateRequest, interfaceinfo);
        // 参数校验
        interfaceinfoService.validInterfaceInfo(interfaceinfo, false);
        User user = userService.getLoginUser(request);
        long id = interfaceinfoUpdateRequest.getId();
        // 判断是否存在
        InterfaceInfo oldInterfaceInfo = interfaceinfoService.getById(id);
        if (oldInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 仅本人或管理员可修改
        if (!oldInterfaceInfo.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = interfaceinfoService.updateById(interfaceinfo);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<InterfaceInfo> getInterfaceInfoById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        InterfaceInfo interfaceinfo = interfaceinfoService.getById(id);
        return ResultUtils.success(interfaceinfo);
    }

    /**
     * 获取列表（仅管理员可使用）
     *
     * @param interfaceinfoQueryRequest
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @GetMapping("/list")
    public BaseResponse<List<InterfaceInfo>> listInterfaceInfo(InterfaceInfoQueryRequest interfaceinfoQueryRequest) {
        InterfaceInfo interfaceinfoQuery = new InterfaceInfo();
        if (interfaceinfoQueryRequest != null) {
            BeanUtils.copyProperties(interfaceinfoQueryRequest, interfaceinfoQuery);
        }
        QueryWrapper<InterfaceInfo> queryWrapper = new QueryWrapper<>(interfaceinfoQuery);
        List<InterfaceInfo> interfaceinfoList = interfaceinfoService.list(queryWrapper);
        return ResultUtils.success(interfaceinfoList);
    }

    /**
     * 分页获取列表
     *
     * @param interfaceinfoQueryRequest
     * @param request
     * @return
     */
    @GetMapping("/list/page")
    public BaseResponse<Page<InterfaceInfo>> listInterfaceInfoByPage(InterfaceInfoQueryRequest interfaceinfoQueryRequest, HttpServletRequest request) {
        if (interfaceinfoQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        InterfaceInfo interfaceinfoQuery = new InterfaceInfo();
        BeanUtils.copyProperties(interfaceinfoQueryRequest, interfaceinfoQuery);
        long current = interfaceinfoQueryRequest.getCurrent();
        long size = interfaceinfoQueryRequest.getPageSize();
        String sortField = interfaceinfoQueryRequest.getSortField();
        String sortOrder = interfaceinfoQueryRequest.getSortOrder();
        String description = interfaceinfoQuery.getDescription();
        // description 需支持模糊搜索
        interfaceinfoQuery.setDescription(null);
        // 限制爬虫
        if (size > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<InterfaceInfo> queryWrapper = new QueryWrapper<>(interfaceinfoQuery);
        queryWrapper.like(StringUtils.isNotBlank(description), "description", description);
        queryWrapper.orderBy(StringUtils.isNotBlank(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);
        Page<InterfaceInfo> interfaceinfoPage = interfaceinfoService.page(new Page<>(current, size), queryWrapper);
        return ResultUtils.success(interfaceinfoPage);
    }

    /**
     * 接口上线
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/online")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Boolean> onlineInterfaceInfo(@RequestBody DeleteRequest deleteRequest,
                                                     HttpServletRequest request) {
        //判断参数状况
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 判断是否存在
        Long id = deleteRequest.getId();
        InterfaceInfo oldInterfaceInfo = interfaceinfoService.getById(id);
        if (oldInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        //获取url
//        String url = oldInterfaceInfo.getUrl();
//        if (StringUtils.isBlank(url)){
//            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"url为空");
//        }
        //判断是否可用
        com.yupi.pojo.ClientUser clientUser = new com.yupi.pojo.ClientUser();
        clientUser.setName("java");
        String res = apiClient.getNameByJson(clientUser,"http://localhost:8090/api/name/user");
        if (StringUtils.isBlank(res)){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"接口验证失败");
        }
        //接口上线
        InterfaceInfo interfaceInfo = new InterfaceInfo();
        interfaceInfo.setId(id);
        interfaceInfo.setStatus(InterfaceStatusEnum.ONLINE.getValue());

        boolean result = interfaceinfoService.updateById(interfaceInfo);
        return ResultUtils.success(result);
    }

    /**
     * 接口下线
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/offline")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Boolean> offlineInterfaceInfo(@RequestBody DeleteRequest deleteRequest,
                                                     HttpServletRequest request) {
        //判断参数状况
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 判断是否存在
        Long id = deleteRequest.getId();
        InterfaceInfo oldInterfaceInfo = interfaceinfoService.getById(id);
        if (oldInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        //接口下线
        InterfaceInfo interfaceInfo = new InterfaceInfo();
        interfaceInfo.setId(id);
        interfaceInfo.setStatus(InterfaceStatusEnum.OFFLINE.getValue());

        boolean result = interfaceinfoService.updateById(interfaceInfo);
        return ResultUtils.success(result);
    }

    /**
     * 接口在线调用
     * @param interfaceInfoInvokeRequest
     * @param request
     * @return
     */
    @PostMapping("/invoke")
    public Object invokeInterfaceInfo(@RequestBody InterfaceInfoInvokeRequest interfaceInfoInvokeRequest,
                                      HttpServletRequest request) {
        //判断参数状况
        if (interfaceInfoInvokeRequest == null || interfaceInfoInvokeRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //获取封装对象
        Long id = interfaceInfoInvokeRequest.getId();
        String userRequestParams = interfaceInfoInvokeRequest.getUserRequestParams();

        //获取url
//        String url = InterfaceInfo.getUrl();
        User loginUser = userService.getLoginUser(request);
        String accessKey = loginUser.getAccessKey();
        String secretKey = loginUser.getSecretKey();
        com.yupi.client.apiClient apiClient = new apiClient(accessKey,secretKey);
        String nameByJson = apiClient.getNameByJson(userRequestParams,"http://localhost:8090/api/name/user");
        return ResultUtils.success(nameByJson);
    }

//    @PostMapping("/add/independence")
//    public BaseResponse<Long> addIndependenceInterfaceInfo(@RequestBody InterfaceInfoAddRequest interfaceinfoAddRequest,
//                                                           HttpServletRequest request,
//                                                           Object requestParams) {
//        if (interfaceinfoAddRequest == null) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        InterfaceInfo interfaceinfo = new InterfaceInfo();
//        BeanUtils.copyProperties(interfaceinfoAddRequest, interfaceinfo);
//        // 校验
//        interfaceinfoService.validInterfaceInfo(interfaceinfo, true);
//        // 校验第三方接口有效性
//        String url = interfaceinfo.getUrl();
//        String nameByJson = apiClient.getNameByJson(requestParams,url);
//        if (nameByJson == "调用失败"){
//            throw new BusinessException(ErrorCode.OPERATION_ERROR,nameByJson);
//        }
//        User loginUser = userService.getLoginUser(request);
//        interfaceinfo.setUserId(loginUser.getId());
//        boolean result = interfaceinfoService.save(interfaceinfo);
//        if (!result) {
//            throw new BusinessException(ErrorCode.OPERATION_ERROR);
//        }
//        long newInterfaceInfoId = interfaceinfo.getId();
//        return ResultUtils.success(newInterfaceInfoId);
//    }
    
}





