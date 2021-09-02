package cn.miozus.common.valid;


import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
// 指定校验器
@Constraint(
        // 可以加载多个，如换Interger，再写一个类型的校验器
        validatedBy = {ListValueConstraintValidator.class}
)
// 元注解
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
// 发现范围
@Retention(RetentionPolicy.RUNTIME)
public @interface ListValue {
    // 规范：三大金刚，报错反馈、组、加载
    String message() default "{cn.miozus.common.valid.ListValue.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    // 自定义参数
    int[] vals() default {};

}