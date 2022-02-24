import os
import sys
import unittest
from autotest.test.pages.pay import PayPage

sys.path.append(os.path.dirname(os.path.dirname(__file__)))

from common.data.joker import user
from common.model.chrome import Chrome
from common.model.log4p import log

from pages import *


class OrderTest(unittest.TestCase):
    def setUp(self):
        self.driver = Chrome().get_driver_singleton()

    @unittest.skip("首页")
    def test_index(self):
        p = IndexPage(self.driver)
        p.open()
        self.assertEqual(self.driver.title, '谷粒商城')

    @unittest.skip("在线购物车")
    def test_add_cart_online(self):
        p = CartPage(self.driver)
        p.add_item()
        p.login_if_absent()
        self.assertFalse(p.isloggin())

    @unittest.skip("订单确认")
    def test_order_confirm(self):
        p = CartPage(self.driver)
        p.open_from_item_page()
        p.login_if_absent()
        p.pay()
        self.assertEqual(self.driver.title, '订单确认')

    # @unittest.skip("跳转支付")
    def test_go_pay(self):
        p = CartPage(self.driver)
        p.open_from_item_page()
        p.login_if_absent()
        p.pay()
        # 来到结算页
        order_comfirm = ConfirmPage(self.driver)
        order_comfirm.submit()
        self.assertEqual(self.driver.title, '收银台')

    @unittest.skip("todo")
    def test_ali_pay(self):
        p = CartPage(self.driver)
        p.open_from_item_page()
        p.login_if_absent()
        p.pay()
        # 来到结算页
        order_comfirm = ConfirmPage(self.driver)
        order_comfirm.submit()
        pay = PayPage(self.driver)
        pay.alipay()
        self.assertEqual(self.driver.title, '支付宝')

    def tearDown(self) -> None:
        # self.driver.quit()
        # self.driver.delete_all_cookies()
        pass


def main(out=sys.stderr, verbosity=2):
    loader = unittest.TestLoader()
    suite = loader.loadTestsFromModule(sys.modules[__name__])
    unittest.TextTestRunner(out, verbosity=verbosity).run(suite)


if __name__ == '__main__':
    with open('test_report.log', 'a+') as f:
        main(f)