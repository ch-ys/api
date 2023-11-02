package com.yupi.project.constant;


import com.yupi.model.entity.InterfaceInfo;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CacheLogicExpireConstant {
    private LocalDateTime expireTime;
    private InterfaceInfo data;
}
