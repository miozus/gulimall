
from common.model import *

class PayPage(Page):
    
    payment_span = By.CSS_SELECTOR, '.Jdbox_BuySuc dl dd span:last-child'
    alipay_link = By.XPATH, '/html/body/div/div[4]/ul/li[2]/a'
    
    def alipay(self):
        self.wait(self.alipay).click()
