from common.model import *


class LoginPage(Page):

    account_login_link = By.CSS_SELECTOR, '.btn1:nth-child(3)'
    account_input = By.NAME, "account"
    password_input = By.NAME, "password"
    gitee_link = By.LINK_TEXT, 'Gitee'
    oauth_button = By.NAME, 'commit'
    submit_button = By.CSS_SELECTOR, '.btn2'
    home_page_locator = Locator.HOME.value

    @mapping.get(Api.LOGIN)
    def sign_login(self, username, password):
        """ 账号登录 """
        self.wait(self.account_login_link).click()
        self.find(self.account_input).send_keys(username)
        self.find(self.password_input).send_keys(password)
        self.find(self.submit_button).click()
        self.wait(self.home_page_locator, 10)
    
    @mapping.get(Api.LOGIN)
    def oauth_login(self):
        """ 社交登录 """
        self.wait(self.gitee_link).click()
        self.wait(self.oauth_button).click()
        self.wait(self.home_page_locator, 15)

