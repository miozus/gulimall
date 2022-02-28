package cn.miozus.gulimall.product.entity;

import cn.miozus.gulimall.common.valid.AddGroup;
import cn.miozus.gulimall.common.valid.ListValue;
import cn.miozus.gulimall.common.valid.UpdateGroup;
import cn.miozus.gulimall.common.valid.UpdateStatus;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;

import lombok.Data;
import org.hibernate.validator.constraints.URL;

import javax.validation.constraints.*;

/**
 * 品牌
 *
 * @author SuDongpo
 * @email miozus@outlook.com
 * @date 2021-08-06 23:57:18
 */
@Data
@TableName("pms_brand")
public class BrandEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 品牌id
     */
    @NotNull(message = "修改必须指定id", groups = {UpdateGroup.class})
    @Null(message = "新增不能指定id", groups = {AddGroup.class})
    @TableId
    private Long brandId;
    /**
     * 品牌名
     * 至少包含一个非空字符
     * 修改也提交，是为了方便人阅读，排查问题带出更好
     */
    //@NotBlank
    @NotBlank(message = "品牌名必须提交", groups = {AddGroup.class, UpdateGroup.class})
    private String name;
    /**
     * 品牌logo地址
     */
    @NotEmpty(groups = {AddGroup.class})
    @URL(message = "logo 必须是合法的 URL 地址", groups = {AddGroup.class, UpdateGroup.class})
    private String logo;
    /**
     * 介绍
     *
     * @NotEmpty 必须提交（不写：可以缺省）
     */
    @NotEmpty
    private String descript;
    /**
     * 显示状态[0-不显示；1-显示]
     */
    //@Pattern(regexp="^[01]$", message="显示状态只能是0或1")
    @NotNull(groups = {AddGroup.class, UpdateStatus.class})
    @ListValue(vals = {0, 1}, groups = {AddGroup.class, UpdateStatus.class})
    private Integer showStatus;
    /**
     * 检索首字母
     */
    @NotEmpty
    @Pattern(regexp = "^[a-zA-Z]$", message = "检索首字母必须是一个字母")
    private String firstLetter;
    /**
     * 排序
     */
    @NotNull
    @Min(value = 0, message = "排序至少大于零")
    private Integer sort;

}
