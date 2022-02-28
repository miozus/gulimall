from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.select import Select
from selenium.webdriver.support.ui import WebDriverWait
from selenium.common.exceptions import TimeoutException
from common.model.log4p import log
import sys


class Page:
    """ 页面对象设计模式基类，用于所有页面的继承 
    """
    def __init__(self, driver):
        self.driver = driver

    def wait(self, locator, timeout=10):
        try:
            return WebDriverWait(self.driver, timeout).until(
            EC.presence_of_element_located(locator))
        except TimeoutException:
            log.info(f'{sys._getframe().f_code.co_name} timeout')


    def find(self, locator):
        return self.driver.find_element(*locator)

    def select(self, locator, option):
        element = self.find(locator)
        return Select(element).select_by_visible_text(option)

    def contains(self, locator):
        try:
            self.wait(locator, 1)
            return True
        except:
            return False
