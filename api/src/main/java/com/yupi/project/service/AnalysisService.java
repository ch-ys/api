package com.yupi.project.service;

import com.yupi.project.model.vo.InterfaceInfoVO;

import java.util.List;

public interface AnalysisService {
    List<InterfaceInfoVO> listTopInvokeInterfaceInfo();
}
