# from common.api.urls import Api
# from common.aspect.chrome import mapping
# from common.model.pageobject import Page
# from selenium.webdriver.common.by import By
from common.model import *


class RegistPage(Page):

    notify_close_button = By.CSS_SELECTOR, '.ty'
    username_input = By.NAME, 'username'
    password_input = By.NAME, 'password'
    password_ensure_input = By.CSS_SELECTOR, '.register-box:nth-child(4) input'
    phone_input = By.NAME, 'phone'
    agreement_checkbox = By.ID, 'xieyi'
    code_input = By.NAME, 'code'
    code_send_link = By.ID, 'sendCode'
    submit_button = By.ID, 'submit_btn'
    title = None

    @mapping(Api.REGIST)
    def reigst(self, username, password, phone):
        self.wait(self.notify_close_button).click()
        # self.find(self.notify_close_button).click()
        self.find(self.username_input).send_keys(username)
        self.find(self.password_input).send_keys(password)
        self.find(self.password_ensure_input).send_keys(password)
        self.find(self.phone_input).send_keys(phone)
        self.find(self.agreement_checkbox).click()

    def checkcode(self, code):
        self.find(self.code_send_link).click()
        self.wait(self.code_send_link)
        self.find(self.code_input).send_keys(code)

    def submit(self):
        self.find(self.submit_button).click()
        self.title = self.getTitle()
