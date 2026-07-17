package com.limou.hrms.model.vo;

import com.limou.hrms.model.entity.ApprovalDelegate;
import java.util.List;
import lombok.Data;

/**
 * 我的委托关系 VO
 * <p>
 * 不能用 {@code Map<String, List<ApprovalDelegate>>} 作为接口返回值：
 * SpringFox 无法渲染值为参数化 List 的 Map，会生成悬空的
 * {@code #/definitions/List} 引用，导致前端 openapi 生成报错。
 */
@Data
public class MyDelegatesVO {
    /** 我委托别人 */
    private List<ApprovalDelegate> asDelegator;
    /** 别人委托我 */
    private List<ApprovalDelegate> asDelegate;
}
