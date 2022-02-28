package cn.miozus.gulimall.common.valid;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.HashSet;
import java.util.Set;

public class ListValueConstraintValidator implements ConstraintValidator<ListValue,Integer> {

    private Set<Integer> set = new HashSet<>();

    // 初始化，将详细信息给出
    @Override
    public void initialize(ListValue constraintAnnotation) {
        int[] vals = constraintAnnotation.vals();
        // TODO: 最好做非空判断，避免无数据
        for (int val : vals) {
            set.add(val);
        }
    }

    /**
     * 是有效的
     *
     * @param integer                    整数, 需要校验的值
     * @param constraintValidatorContext 约束验证器上下文
     * @return boolean
     */// 是否校验成功
    @Override
    public boolean isValid(Integer integer, ConstraintValidatorContext constraintValidatorContext) {
        return set.contains(integer);
    }
}
