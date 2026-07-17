package com.limou.hrms.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.mapper.HolidayConfigMapper;
import com.limou.hrms.model.entity.HolidayConfig;
import com.limou.hrms.model.vo.HolidayConfigVO;
import com.limou.hrms.service.HolidayConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class HolidayConfigServiceImpl extends ServiceImpl<HolidayConfigMapper, HolidayConfig>
        implements HolidayConfigService {

    @Resource
    private HolidayConfigMapper holidayConfigMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HolidayConfigVO createHoliday(HolidayConfig config) {
        ThrowUtils.throwIf(config.getHolidayDate() == null, ErrorCode.PARAMS_ERROR, "日期不能为空");
        ThrowUtils.throwIf(config.getHolidayName() == null || config.getHolidayName().isEmpty(),
                ErrorCode.PARAMS_ERROR, "节假日名称不能为空");
        ThrowUtils.throwIf(config.getHolidayType() == null, ErrorCode.PARAMS_ERROR, "类型不能为空");

        HolidayConfig existing = this.lambdaQuery()
                .eq(HolidayConfig::getHolidayDate, config.getHolidayDate()).one();
        ThrowUtils.throwIf(existing != null, ErrorCode.OPERATION_ERROR, "该日期已存在配置");

        config.setCreateTime(new Date());
        config.setUpdateTime(new Date());

        boolean saved = this.save(config);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "创建失败");

        return convertToVO(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HolidayConfigVO updateHoliday(HolidayConfig config) {
        ThrowUtils.throwIf(config.getId() == null, ErrorCode.PARAMS_ERROR, "ID不能为空");

        HolidayConfig existing = this.getById(config.getId());
        ThrowUtils.throwIf(existing == null, ErrorCode.NOT_FOUND_ERROR, "记录不存在");

        BeanUtils.copyProperties(config, existing, "id", "createTime");
        existing.setUpdateTime(new Date());

        boolean updated = this.updateById(existing);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "更新失败");

        return convertToVO(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHoliday(Long id) {
        HolidayConfig config = this.getById(id);
        ThrowUtils.throwIf(config == null, ErrorCode.NOT_FOUND_ERROR, "记录不存在");
        this.removeById(id);
    }

    @Override
    public HolidayConfigVO getHolidayDetail(Long id) {
        HolidayConfig config = this.getById(id);
        ThrowUtils.throwIf(config == null, ErrorCode.NOT_FOUND_ERROR, "记录不存在");
        return convertToVO(config);
    }

    @Override
    public List<HolidayConfigVO> getAllHolidays() {
        List<HolidayConfig> list = this.lambdaQuery().orderByAsc(HolidayConfig::getHolidayDate).list();
        return list.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public List<HolidayConfigVO> getHolidaysByYear(Integer year) {
        ThrowUtils.throwIf(year == null, ErrorCode.PARAMS_ERROR, "年份不能为空");

        Date startDate = DateUtil.parseDate(year + "-01-01");
        Date endDate = DateUtil.parseDate(year + "-12-31");

        List<HolidayConfig> list = this.lambdaQuery()
                .ge(HolidayConfig::getHolidayDate, startDate)
                .le(HolidayConfig::getHolidayDate, endDate)
                .orderByAsc(HolidayConfig::getHolidayDate)
                .list();
        return list.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public boolean isHoliday(Date date) {
        if (date == null) return false;
        HolidayConfig holiday = this.lambdaQuery()
                .eq(HolidayConfig::getHolidayDate, DateUtil.formatDate(date))
                .eq(HolidayConfig::getHolidayType, 0).one();
        return holiday != null;
    }

    @Override
    public boolean isWorkDay(Date date) {
        if (date == null) return false;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            HolidayConfig workDay = this.lambdaQuery()
                    .eq(HolidayConfig::getHolidayDate, DateUtil.formatDate(date))
                    .eq(HolidayConfig::getHolidayType, 1).one();
            return workDay != null;
        }

        HolidayConfig holiday = this.lambdaQuery()
                .eq(HolidayConfig::getHolidayDate, DateUtil.formatDate(date))
                .eq(HolidayConfig::getHolidayType, 0).one();
        return holiday == null;
    }

    private HolidayConfigVO convertToVO(HolidayConfig config) {
        HolidayConfigVO vo = new HolidayConfigVO();
        BeanUtils.copyProperties(config, vo);
        vo.setHolidayTypeText(getHolidayTypeText(config.getHolidayType()));
        return vo;
    }

    private String getHolidayTypeText(Integer holidayType) {
        if (holidayType == null) return "未知";
        switch (holidayType) {
            case 0: return "法定节假日";
            case 1: return "调休上班日";
            case 2: return "公司自定义假期";
            default: return "未知";
        }
    }
}
