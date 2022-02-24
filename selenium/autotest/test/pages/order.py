from common.model import *

class ConfirmPage(Page):
    
    order_confirm_page_locator = By.XPATH, "//span[contains(.,'结算页')]"    
    submit_button = By.CSS_SELECTOR, '.tijiao'
    
    @mapping.get_authed(Api.ORDER_TRADE)
    def submit(self):
        self.wait(self.submit_button).click()
