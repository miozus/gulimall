from common.model import *


class PayPage(Page):
    """支付宝 - 网上支付 安全快速！"""

    payment_span = By.CSS_SELECTOR, '.Jdbox_BuySuc dl dd span:last-child'
    alipay_link = By.XPATH, '/html/body/div/div[4]/ul/li[2]/a'
    alipay_logo = By.CSS_SELECTOR, '.alipay-logo'
    # account_login_link = By.CSS_SELECTOR, '.btn1:nth-child(3)'
    account_input = By.ID, 'J_tLoginId'
    password_input = By.ID, 'payPasswd_rsainput'
    submit_button = By.CSS_SELECTOR, '#J_newBtn > span'
    bank_title_span = By.CSS_SELECTOR, '#header .logo-title'

    square_click_button = By.CSS_SELECTOR, 'span:nth-child(7)'
    secret_input = By.ID, 'payPassword_rsainput'
    auth_submit_button = By.ID, 'J_authSubmit'
    order_list_locator = None

    def go_alipay(self):
        self.wait(self.alipay_link).click()
        self.wait(self.payment_span, 3)

    def sign_login(self, username, password):
        """ 账号登录 """
        # self.wait(self.account_login_link).click()
        self.find(self.account_input).send_keys(username)
        self.wait(self.password_input).click()
        self.find(self.password_input).click()
        self.driver.implicitly_wait(1)
        self.find(self.password_input).send_keys(password)
        self.find(self.submit_button).click()
        self.wait(self.bank_title_span, 5)

    def pay_by_input_secret(self, password):
        # self.wait(self.square_click_button).click()
        self.wait(self.secret_input).send_keys(password)
        self.find(self.auth_submit_button).click()
        self.wait(self.order_list_locator, 5)