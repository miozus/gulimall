import os
import sys
import unittest

sys.path.append(os.path.dirname(os.path.dirname(__file__)))

from common.api.joker import user
from common.aspect.chrome import driver

from pages import *


class TestMall(unittest.TestCase):
    def setUp(self):
        self.driver = driver

    @unittest.skip("一次性")
    def test_regist(self):
        """ 验证码临时生成在 Redis 
        """
        p = RegistPage(self.driver)
        p.reigst(user['username'], user['password'], user['phone'])
        p.checkcode(user['code'])
        p.submit()
        self.assertEqual(p.title, '用户登录')

    @unittest.skip("ok")
    def test_sign_login(self):
        p = LoginPage(self.driver)
        p.sign_login(user['username'], user['password'])
        self.assertEqual(p.title, '谷粒商城')

    @unittest.skip("ok")
    def test_oauth_login(self):
        p = LoginPage(self.driver)
        p.oauth_login()
        self.assertEqual(p.title, '谷粒商城')

    @unittest.skip("todo")
    def test_add_cart(self):
        ...

    @unittest.skip("todo")
    def test_open_cart(self):
        ...

    @unittest.skip("todo")
    def test_order_confim(self):
        ...

    @unittest.skip("todo")
    def test_order_pay(self):
        ...


if __name__ == "__main__":
    unittest.main()
