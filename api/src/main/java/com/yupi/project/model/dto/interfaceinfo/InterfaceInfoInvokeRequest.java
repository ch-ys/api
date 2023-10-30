package com.yupi.project.model.dto.interfaceinfo;


import com.yupi.pojo.ClientUser;
import lombok.Data;

@Data
public class InterfaceInfoInvokeRequest {

    /**
     * 主键
     */
    private Long id;

    /**
     * 请求参数
     */
    private String userRequestParams;

}
