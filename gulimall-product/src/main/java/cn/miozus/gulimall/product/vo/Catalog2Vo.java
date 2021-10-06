package cn.miozus.gulimall.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * catalog2分类
 *    - parentId
 *    - Nodes[...]
 *
 * @author miao
 * @date 2021/10/05
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Catalog2Vo {
    /**
     * catalog1 级父分类
     */
    private String catalog1Id;
    /**
     * catalog3 级父分类
     */
    private List<Catalog3Vo> catalog3List;
    /**
     * id 2级
     */
    private String id;
    /**
     * 2级 的名字
     */
    private String name;

    /**
     * catalog3级分类
     *
     * @author miao
     * @date 2021/10/05
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Catalog3Vo {
        /**
         * catalog2 级父分类
         */
        private String catalog2Id;
        /**
         * id 3级
         */
        private String id;
        /**
         * 3级 的名字
         */
        private String name;

    }
}
