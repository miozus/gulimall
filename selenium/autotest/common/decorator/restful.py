from enum import Enum
from functools import wraps

from common.data.joker import user
from common.model.chrome import Chrome
from common.model.log4p import log

driver = Chrome().get_driver_singleton()


class mapping(object):
    def __init__(self):
        """ 不允许实例化 """
        return False

    @staticmethod
    def get(url):
        """ 
        打开网页，然后执行你的方法
        """
        if isinstance(url, Enum):
            url = url.value
        def decorator(func):
            @wraps(func)
            def wrapper(*args, **kwargs):
                if driver.current_url != url:
                    log.info(f'[GET] {url} [{func.__name__}]')
                    driver.get(url)
                return func(*args, **kwargs)

            return wrapper

        return decorator

    @staticmethod
    def get_authed(url):
        """ 
        打开网页（已登录），然后执行你的方法
        """
        if isinstance(url, Enum):
            url = url.value
        def decorator(func):
            @wraps(func)
            def wrapper(*args, **kwargs):
                if driver.current_url != url:
                    log.info(f'[GET] {url} [{func.__name__}]')
                    # driver.get(user['auth'])
                    driver.get(url)
                    # log.info(f'[GET] {user["auth"]} [{func.__name__}]')
                    # log.info(f'Authorized?: {driver.get_cookies()}')
                    driver.add_cookie(user['cookies'][0])
                    driver.add_cookie(user['cookies'][1])
                    # log.info(f'Authorized.: {driver.get_cookies()}')
                    driver.get(url)
                    log.info(f'Authorized: {driver.get_cookies()}')
                return func(*args, **kwargs)

            return wrapper

        return decorator

    @staticmethod
    def redirect_finnally(url):
        """ 
        最后跳转到网页
        """
        if isinstance(url, Enum):
            url = url.value
        def decorator(func):
            @wraps(func)
            def wrapper(*args, **kwargs):
                retVal = func(*args, **kwargs)
                if driver.current_url != url:
                    log.info(f'[GET] {url} [{func.__name__}]')
                    driver.get(url)
                return retVal

            return wrapper

        return decorator
