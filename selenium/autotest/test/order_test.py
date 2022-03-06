import os
import sys
import unittest
import json
import time

sys.path.append(os.path.dirname(os.path.dirname(__file__)))

from common.data.joker import user
from common.model.chrome import Chrome
from common.model.log4p import log

from pages import *


class OrderTest(unittest.TestCase):
    authorized_cookie = None

    def setUp(self):
        self.driver = Chrome().get_driver_singleton()

    # @unittest.skip("首页")
    def test_00_index(self):
        p = IndexPage(self.driver)
        p.open()
        self.assertEqual(self.driver.title, '谷粒商城')

    @unittest.skip("已加购[绿]_在线购物车")
    def test_01_add_cart_online(self):
        p = CartPage(self.driver)
        p.open_from_item_page()
        p.add_item()
        p.login_if_absent()
        self.assertFalse(p.online())

    # @unittest.skip("每次加购[灰]_订单确认")
    def test_02_order_confirm(self):
        p = CartPage(self.driver)
        p.login_if_absent()
        # p.open_from_home_page()
        p.add_item()
        p.minus_until_one()
        self.authorized_cookie = self.driver.get_cookies()
        p.confirm()
        time.sleep(2)
        self.assertEqual(self.driver.title, '订单确认')

    # @unittest.skip("结算页>收银台")
    def test_03_go_pay(self):
        p= ConfirmPage(self.driver)
        # 延迟为了避免 Redis 未写先读：NPE购物车商品未勾选
        time.sleep(2)
        p.submit()
        self.assertEqual(self.driver.title, '收银台')

    # @unittest.skip("跳转支付宝支付")
    def test_04_alipay(self):
        p = PayPage(self.driver)
        p.go_alipay()
        p.sign_login(user['account'], user['secret'])
        p.pay_by_input_secret(user['secret'])
        self.assertEqual(self.driver.title, '支付宝 - 网上支付 安全快速！')

    def tearDown(self) -> None:
        # self.driver.quit()
        # self.driver.delete_all_cookies()
        log.info(f"{self._testMethodName} : {json.dumps(self.driver.get_cookies())}")
        pass


def main(out=sys.stderr, verbosity=2):
    loader = unittest.TestLoader()
    suite = loader.loadTestsFromModule(sys.modules[__name__])
    unittest.TextTestRunner(out, verbosity=verbosity).run(suite)


if __name__ == '__main__':
    log_dir = r'E:\projects\IdeaProjects\gulimall\selenium\test_report.log'
    with open(log_dir, 'a+') as f:
        main(f)
