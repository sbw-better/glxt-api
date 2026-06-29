package com.citics.glxtapi.web.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.citics.glxtapi.web.entity.Tenant;

public interface TenantService extends IService<Tenant> {

    /**
     * 清除数据源标识
     */
   default void clearDs(){}

   default void setDs(String ds){}

   default String getDs(){
       return "";
   }

    /**
     * 获取租户标识
     * @return
     */
   default String getTenant(){
       return "";
   }
}
