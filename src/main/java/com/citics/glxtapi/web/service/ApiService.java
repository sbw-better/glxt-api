package com.citics.glxtapi.web.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.citics.glxtapi.common.page.PageResult;
import com.citics.glxtapi.web.entity.ApiInterface;
import com.citics.glxtapi.web.entity.dto.ApiInterfaceDTO;
import com.citics.glxtapi.web.entity.vo.ApiInterfaceVO;
import com.citics.glxtapi.web.entity.vo.InterfaceDemoVo;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;
import java.util.List;

public interface ApiService extends IService<ApiInterface> {

    /**
     * 查询接口数量
     * @param tenant
     * @return
     */
    long count(String tenant);

    /**
     * 保存和修改接口配置
     * @param dto
     * @return
     */
    ApiInterfaceVO save(ApiInterfaceDTO dto);

    /**
     * 接口是否是分页的
     * @param dto
     * @return
     */
    Boolean isPage(ApiInterfaceDTO dto);

    /**
     * 删除
     * @param id
     * @return
     */
    boolean delete(Serializable id);

    /**
     * 接口查询
     * @param code
     * @return
     */
    ApiInterfaceVO getByApi(String tenant, String code);

    /**
     * 接口查询
     * @param code
     * @param name
     * @return
     */
    ApiInterface getByCodeName(String code, String name);

    /**
     * 单查询详细
     * @param id
     * @param code
     * @return
     */
    ApiInterfaceVO get(Long id, String code);

    /**
     * 列表
     * @param dto
     * @return
     */
    List<ApiInterface> list(ApiInterfaceDTO dto);

    /**
     * 分页
     * @param dto
     * @return
     */
    PageResult page(ApiInterfaceDTO dto);

    /**
     * 判断是否存在该tenant数据
     * @param tenant
     * @return
     */
    Boolean isHaveTenant(String tenant);

    /**
     * 判断是否存在该connection数据
     * @param connectionId
     * @return
     */
    Boolean isHaveConnection(Long connectionId);

    String preview(ApiInterfaceDTO dto);

    InterfaceDemoVo demo(ApiInterfaceDTO dto);

    String export(ApiInterfaceDTO dto);

    Boolean upload(MultipartFile file) throws Exception;
}
