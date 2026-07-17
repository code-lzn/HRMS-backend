package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.limou.hrms.model.entity.HolidayConfig;
import com.limou.hrms.model.vo.HolidayConfigVO;

import java.util.Date;
import java.util.List;

public interface HolidayConfigService extends IService<HolidayConfig> {

    HolidayConfigVO createHoliday(HolidayConfig config);

    HolidayConfigVO updateHoliday(HolidayConfig config);

    void deleteHoliday(Long id);

    HolidayConfigVO getHolidayDetail(Long id);

    List<HolidayConfigVO> getAllHolidays();

    List<HolidayConfigVO> getHolidaysByYear(Integer year);

    boolean isHoliday(Date date);

    boolean isWorkDay(Date date);
}
