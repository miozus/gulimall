from common.model import *
from .login import LoginPage


class CartPage(Page):

    add_cart_button = By.ID, 'addToCart'
    cart_button = By.ID, 'GotoShoppingCart'
    login_now_button = By.CSS_SELECTOR, '.one_load_btn'
    my_cart_at_item_link = By.XPATH, '//*[@id="max"]/nav/div/div[1]/div[3]/a'
    my_cart_at_home_link = By.XPATH, '/html/body/header/div[3]/div[2]/div[1]/span[1]/a'
    go_pay_button = By.CSS_SELECTOR, '.One_ShopFootBuy>div:last-child button'
    count_plus_button = By.XPATH, '/html/body/div[2]/div[1]/div[3]/div[1]/ul/li/div[2]/ol/li[4]/p/span[3]' 
    count_minus_button = By.XPATH, '/html/body/div[2]/div[1]/div[3]/div[1]/ul/li/div[2]/ol/li[4]/p/span[1]'
    count_num = By.XPATH, '/html/body/div[2]/div[1]/div[3]/div[1]/ul/li/div[2]/ol/li[4]/p/span[2]'

    @mapping.get(Api.ITEM)
    def open_from_item_page(self):
        """ 打开购物车 """
        self.wait(self.my_cart_at_item_link).click()

    @mapping.get(Api.HOME)
    def open_from_home_page(self):
        """ 打开购物车 """
        self.wait(self.my_cart_at_home_link).click()

    @mapping.get(Api.ITEM)
    def add_item(self):
        """ 加入购物车 """
        self.wait(self.add_cart_button).click()
        self.wait(self.cart_button).click()

    @mapping.redirect_finnally(Api.CART_LIST)
    def login_if_absent(self):
        """ 立即登录 """
        try:
            self.find(self.login_now_button).click()
        except:
            pass
        if not self.driver.get_cookie(Api.SESSION_NAME.value):
            p = LoginPage(self.driver)
            p.sign_login("administrator", "administrator")

    def online(self):
        return self.contains(self.login_now_button)

    @mapping.get(Api.CART_LIST)
    def confirm(self):
        self.wait(self.go_pay_button).click()
        
    
    def minus_until_one(self):
        tried_time = 0
        while self.wait(self.count_num).text != "1":
            self.find(self.count_minus_button).click()
            tried_time +=1
            self.driver.implicitly_wait(2)
            log.info(f'{self.__repr__.__name__} tried to abtract for {tried_time} tiems')
            