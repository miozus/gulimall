from enum import Enum
from selenium.webdriver.common.by import By

class Api(Enum):
    HOME = 'http://gulimall.com/'
    REGIST = 'http://auth.gulimall.com/register.html'
    LOGIN = 'http://auth.gulimall.com/login.html'
    ITEM = 'http://item.gulimall.com/15.html'
    SEARCH = "http://search.gulimall.com/search.html"
    CART = "http://cart.gulimall.com"
    CART_LIST = "http://cart.gulimall.com/cartList.html"
    ORDER_TRADE = "http://order.gulimall.com/toTrade"
    ORDER_LIST = "http://member.gulimall.com/memberOrder.html"
    
class Locator(Enum):
    HOME = By.XPATH, '/html/body/header/div[1]/a/img'    
    REGIST = By.CSS_SELECTOR, '.ty'
    LOGIN = By.CSS_SELECTOR, '.btn1:nth-child(3)'
    ITEM = By.ID, 'addToCart'
    SEARCH = ""
    CART = ""
    CART_LIST = ""
    ORDER_TRADE = By.XPATH, "//span[contains(.,'结算页')]"
    ORDER_LIST = ""