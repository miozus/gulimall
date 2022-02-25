import os
import sys
import unittest

sys.path.append(os.path.dirname(os.path.dirname(__file__)))

from common.data.joker import user
from common.model.chrome import Chrome
from common.model.log4p import log

from pages import *


class AuthorizeTest(unittest.TestCase):
    def setUp(self):
        self.driver = Chrome().get_driver_singleton()

    # @unittest.skip("首页")
    def test_00_index(self):
        p = IndexPage(self.driver)
        p.open()
        self.assertEqual(self.driver.title, '谷粒商城')

    @unittest.skip("注册账号")
    def test_01_regist(self):
        p = RegistPage(self.driver)
        p.reigst(user['username'], user['password'], user['phone'])
        p.checkcode(user['code'])
        p.submit()
        self.assertEqual(self.driver.title, '用户登录')

    # @unittest.skip("账号登录")
    def test_02_sign_login(self):
        p = LoginPage(self.driver)
        p.sign_login(user['username'], user['password'])
        log.info(f'Authorized Cookies: {self.driver.get_cookies()}')

    @unittest.skip("账号登录_账号错误")
    def test_02_1_sign_login_error_account(self):
        p = LoginPage(self.driver)
        p.sign_login(user['username'] + 'error', user['password'])
        log.info(f'Authorized Cookies: {self.driver.get_cookies()}')
        self.assertEqual(self.driver.title, '谷粒商城')

    @unittest.skip("授权登录")
    def test_03_oauth_login(self):
        p = LoginPage(self.driver)
        p.oauth_login()
        log.info(f'Authorized Cookies: {self.driver.get_cookies()}')
        self.assertEqual(self.driver.title, '谷粒商城')

    # @unittest.skip("离线购物车")
    def test_04_add_cart(self):
        self.driver.delete_all_cookies()
        p = CartPage(self.driver)
        p.add_item()
        self.assertEqual(self.driver.title, '购物车')

    def tearDown(self) -> None:
        # self.driver.quit()
        pass


def main(out=sys.stderr, verbosity=2):
    loader = unittest.TestLoader()
    suite = loader.loadTestsFromModule(sys.modules[__name__])
    unittest.TextTestRunner(out, verbosity=verbosity).run(suite)


if __name__ == '__main__':
    with open('test_report.log', 'a+') as f:
        main(f)