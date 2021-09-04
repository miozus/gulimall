<!--  -->
<template>
  <div>
    <el-switch v-model="draggable" active-text="开启拖拽" inactive-text="">
    </el-switch>
    <el-button
      v-if="draggable"
      @click="batchSave"
      size="mini"
      type="primary"
      plain
      >批量保存</el-button
    >
    <el-button type="danger" @click="batchDelete" size="mini" plain
      >批量删除</el-button
    >

    <el-tree
      :data="menus"
      :props="defaultProps"
      :expand-on-click-node="false"
      show-checkbox
      node-key="catId"
      :default-expanded-keys="expandedKey"
      @node-drop="handleDrop"
      :draggable="draggable"
      :allow-drop="allowDrop"
      :allow-drag="allowDrag"
      ref="tree"
    >
      <span class="custom-tree-node" slot-scope="{ node, data }">
        <span>{{ node.label }}</span>
        <span>
          <el-button
            v-if="node.level < 3"
            type="text"
            size="mini"
            @click="() => save(data)"
          >
            增
          </el-button>

          <el-button type="text" size="mini" @click="() => update(data)">
            改
          </el-button>
          <el-button
            v-if="node.childNodes.length == 0"
            type="text"
            size="mini"
            @click="() => delete (node, data)"
          >
            删
          </el-button>
        </span>
      </span>
    </el-tree>
    <el-dialog
      :title="dialogTitle"
      :visible.sync="dialogVisible"
      width="30%"
      :close-on-click-modal="false"
    >
      <el-form :model="category">
        <el-form-item label="分类名称">
          <el-input v-model="category.name" autocomplete="on"></el-input>
        </el-form-item>
        <el-form-item label="分类图标">
          <el-input v-model="category.icon" autocomplete="on"></el-input>
        </el-form-item>
        <el-form-item label="计量单位">
          <el-input v-model="category.productUnit" autocomplete="on"></el-input>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitData()">确 定</el-button>
      </span>
    </el-dialog>
  </div>
</template>
<script>
export default {
  data() {
    return {
      pCid: [],
      draggable: false,
      updateNodes: [],
      maxNodeLevel: 0,
      dialogTitle: "",
      dialogType: "",
      category: {
        name: "",
        parentCid: 0,
        catLevel: 0,
        showStatus: 1,
        sort: 0,
        productUnit: "",
        icon: "",
        catId: null
      },
      dialogVisible: false,
      menus: [],
      expandedKey: [],
      defaultProps: {
        children: "children",
        label: "name"
      }
    };
  },
  methods: {
    handleNodeClick(data) {
      console.log(data);
    },
    getMenus() {
      this.$http({
        url: this.$http.adornUrl("/product/category/list/tree"),
        method: "get"
      }).then(({ data }) => {
        console.log("success get data... ", data.data);
        this.menus = data.data;
      });
    },
    submitData() {
      switch (this.dialogType) {
        case "save":
          this.saveCategory();
          break;
        case "update":
          this.updateCategory();
          break;
        default:
          break;
      }
    },
    save(data) {
      console.log(data);
      this.dialogType = "save";
      this.dialogTitle = "新增";
      this.dialogVisible = true;
      this.category.parentCid = data.catId;
      this.category.catLevel = data.catLevel * 1 + 1;
      this.category.name = "";
      this.category.icon = "";
      this.category.productUnit = "";
    },
    update(data) {
      console.log("update:", data);
      this.dialogType = "update";
      this.dialogTitle = "修改";
      this.dialogVisible = true;
      this.$http({
        url: this.$http.adornUrl(`/product/category/info/${data.catId}`),
        method: "get"
      }).then(({ data }) => {
        this.category = { ...data.data };
      });
    },
    // 修改三级分类
    updateCategory() {
      console.log("修改三级绑定数据：", this.category);
      var { catId, name, icon, productUnit } = this.category;
      this.$http({
        url: this.$http.adornUrl("/product/category/update"),
        method: "post",
        data: this.$http.adornData({ catId, name, icon, productUnit }, false)
      }).then(({ data }) => {
        this.$message({
          message: "修改成功",
          type: "success"
        });
        this.expandedKey = [this.category.parentCid];
        this.getMenus();
        this.dialogVisible = false;
      });
    },
    // 添加三级分类
    saveCategory() {
      console.log("添加三级绑定数据：", this.category);

      this.$http({
        url: this.$http.adornUrl("/product/category/save"),
        method: "post",
        data: this.$http.adornData(this.category, false)
      }).then(({ data }) => {
        this.$message({
          message: "添加成功",
          type: "success"
        });
        this.expandedKey = [this.category.parentCid];
        this.getMenus();
        this.dialogVisible = false;
      });
    },

    delete(node, data) {
      var ids = [data.catId];
      this.$confirm(`是否删除【${data.name}】菜单？`, "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      })
        .then(() => {
          this.$http({
            url: this.$http.adornUrl("/product/category/delete"),
            method: "post",
            data: this.$http.adornData(ids, false)
          }).then(({ data }) => {
            this.$message({
              type: "success",
              message: "删除成功!"
            });
            this.expandedKey = [node.parent.data.catId];
            this.getMenus();
          });
        })
        .catch(() => {
          this.$message({
            type: "info",
            message: "已取消删除"
          });
        });
      console.log(node, data);
    },
    /* 批量删除节点 */
    batchDelete() {
      let catIds = this.$refs.tree.getCheckedKeys();
      let checkedNodes = this.$refs.tree.getCheckedNodes();
      let names = checkedNodes.map(node => node.name);
      // dialog
      this.$confirm(`是否删除【${names}】菜单？`, "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      })
        .then(() => {
          this.$http({
            url: this.$http.adornUrl("/product/category/delete"),
            method: "post",
            data: this.$http.adornData(catIds, false)
          }).then(({ data }) => {
            this.$message({
              type: "success",
              message: "删除成功!"
            });
            this.getMenus();
          });
        })
        .catch(() => {
          this.$message({
            type: "info",
            message: "已取消删除"
          });
        });
    },
    /**
     * 可拖拽节点
     *
     * 节点思维（A拖拽节点，B目标节点，位置类型：前中后）
     *   A  ->    B
     *            ├─parent
     *            ├─level
     *            ├─data─ (pCid)          inner
     *            └─sblings(b + !b)
     *
     *   A  ->    B
     *            ├─parent
     *            |   ├─sblings(B + !B)
     *            |   |                    pre
     *            |   └─data-(pCid)
     *            |                        next
     *            ├─level
     *            └─data─ (null)
     *
     * @var {integer} pCid B的身份证，即A移动后，继承父节点的身份证
     * @var {Node} siblings C，即B的父节点的子节点数组，
     *      因为移动后受影响的范围扩大到它和兄弟之间， 所以从它父节点向下辐射
     *
     */
    handleDrop(draggingNode, dropNode, dropType, ev) {
      console.log("tree drag: ", draggingNode, dropNode, dropType);
      let pCid = 0;
      let siblings = null;

      if (dropType == "inner") {
        pCid = dropNode.data.catId;
        siblings = dropNode.childNodes;
      } else {
        // 修复：一级菜单到顶部后，它的pCid为null，因为前后B.data变成了数组，未定义
        pCid = dropNode.parent.data.catId || 0;
        siblings = dropNode.parent.childNodes;
      }
      this.pCid.push(pCid);

      // A 的顺序，由拷贝B获得，（B内部移动：子节点集合 = 它自己 + 兄弟节点）
      // （B前后移动：子节点集合 = 兄弟节点）
      for (let i = 0; i < siblings.length; i++) {
        //  B-b 它自己
        if (siblings[i].data.catId == draggingNode.data.catId) {
          let catLevel = draggingNode.level;
          // B 层级变动：两点比较，并非同级，跳出包含关系
          if (siblings[i].level != draggingNode.level) {
            catLevel = siblings[i].level;
            // B-b-（b1~bn） a的所有子节点层级变动
            this.updateChildNodeLevel(siblings[i]);
          }
          this.updateNodes.push({
            catId: siblings[i].data.catId,
            sort: i,
            parentCid: pCid,
            catLevel: catLevel
          });
        } else {
          // B-（!b） 兄弟节点
          this.updateNodes.push({
            catId: siblings[i].data.catId,
            sort: i
          });
        }
      }
      console.log("updateNodes: ", this.updateNodes);
    },
    batchSave() {
      // 发送请求到后端接口
      this.$http({
        url: this.$http.adornUrl("/product/category/update/sort"),
        method: "post",
        data: this.$http.adornData(this.updateNodes, false)
      }).then(({ data }) => {
        // 刷新页面放这里，页面所见即所得；否则放外围，被异步抢先，顺序是老的，还要手动刷新网页
        this.expandedKey = this.pCid;
        this.getMenus();
        this.$message({
          message: "菜单顺序修改成功",
          type: "success"
        });
        // 修复：每次拖动，数组的中的历史越来越大，单页面应用不会自动刷新，记得清空回默认值
        this.updateNodes = [];
        this.maxNodeLevel = 0;
        // this.pCid = [];
      });
    },
    updateChildNodeLevel(node) {
      if (node.childNodes.length > 0) {
        for (let i = 0; i < node.childNodes.length; i++) {
          var cNode = node.childNodes[i];
          this.updateNodes.push({
            catId: cNode.catId,
            catLevel: node.childNodes[i].level
          });
          this.updateChildNodeLevel(node.childNodes[i]);
        }
      }
    },
    allowDrop(draggingNode, dropNode, type) {
      console.log("drag: ", draggingNode, "drop: ", dropNode, type);
      // 获取拖拽节点的最大深度
      this.countNodeLevel(draggingNode);
      // 被拖动的节点 + 所在父节点层数 < 3
      // 从根节点拉出两条直线，它们间距 + 1 索引（从零开始）, 用页面实际的层级（因为多次变动，最后才提交到数据库）
      // 间距是非负数，所以用绝对值方法
      let deep = Math.abs(this.maxNodeLevel - dropNode.level) + 1;
      console.log("deep: ", deep);
      // debug
      console.log(`
    this.maxNodeLevel: ${this.maxNodeLevel}
    draggingNode.level: ${draggingNode.level}
    dropNode.level: ${dropNode.level}
    `);
      if (type == "inner") {
        return deep + dropNode.level <= 3;
      } else {
        return deep + dropNode.parent.level <= 3;
      }
    },
    // 找到节点自己的最大层级
    // 递归遍历：它的子节点也要找到，最终找最大的level
    // 一级1，二级2，三级3
    countNodeLevel(node) {
      if (node.childNodes != null && node.childNodes.length > 0) {
        for (let i = 0; i < node.childNodes.length; i++) {
          if (node.childNodes[i].level > this.maxNodeLevel) {
            this.maxNodeLevel = node.childNodes[i].level;
          }
          this.countNodeLevel(node.childNodes[i]);
        }
      } else {
        // 修复：第三级节点无子节点（0），默认为本层级（3）
        this.maxNodeLevel = node.level;
      }
    },
    allowDrag(draggingNode) {
      return true;
      // return draggingNode.data.catLevel !== 1;
    }
  },
  //这里可以导入其他文件（比如：组件，工具js，第三方插件js，json文件，图片文件等等）
  //例如：import 《组件名称》 from '《组件路径》';

  //import引入的组件需要注入到对象中才能使用
  components: {},
  //监听属性 类似于data概念
  computed: {},
  //监控data中的数据变化
  watch: {},
  //方法集合
  //生命周期 - 创建完成（可以访问当前this实例）
  created() {
    this.getMenus();
  },
  //生命周期 - 挂载完成（可以访问DOM元素）
  mounted() {},
  beforeCreate() {}, //生命周期 - 创建之前
  beforeMount() {}, //生命周期 - 挂载之前
  beforeUpdate() {}, //生命周期 - 更新之前
  updated() {}, //生命周期 - 更新之后
  beforeDestroy() {}, //生命周期 - 销毁之前
  destroyed() {}, //生命周期 - 销毁完成
  activated() {} //如果页面有keep-alive缓存功能，这个函数会触发
};
</script>
<style scoped></style>
