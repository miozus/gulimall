from common.model import *


class ConfirmPage(Page):

    order_confirm_page_locator = By.XPATH, "//span[contains(.,'结算页')]"
    submit_button = By.CSS_SELECTOR, '.tijiao'
    pay_page_locator = Locator.PAY.value

    # @mapping.get_authed(Api.ORDER_TRADE)
    def submit(self):
        self.driver.execute_script('window.scrollBy(0,1000)')
        self.wait(self.submit_button).click()
        self.wait(self.pay_page_locator, 10)
