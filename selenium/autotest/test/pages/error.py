from common.model import *

class ErrorPage(Page):

    whitelabel_text = By.XPATH, '/html/body/h1'
    
    def is_error_page(self):
        return self.find(self.whitelabel_text).text == "Whitelabel Error Page"
        