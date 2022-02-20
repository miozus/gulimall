from enum import Enum

class Api(Enum):
    HOME = 'http://gulimall.com/'
    REGIST = 'http://auth.gulimall.com/register.html'
    LOGIN = 'http://auth.gulimall.com/login.html'
    ITEM = 'http://item.gulimall.com/15.html'
    SEARCH = "http://search.gulimall.com/search.html"
    CART = "http://cart.gulimall.com"
    ORDER_TRADE = "http://order.gulimall.com/toTrade"
    ORDER_LIST = "http://member.gulimall.com/memberOrder.html"