import threading
from functools import wraps

from common.api.urls import Api
from common.aspect.log4p import log
from selenium import webdriver
from selenium.webdriver.chrome.service import Service as ChromeService


class Chrome(object):
    """
    生成浏览器引擎的单例模式

    🔒 线程安全.使用方法:

        driver = ChromeSingleton().driver

    exe: 初始化引擎，直接写死可执行文件的目录（避免代理433）
    """

    _instance_lock = threading.Lock()
    driver = None

    def __init__(self) -> None:
        pass

    def __new__(cls, *args, **kwargs):
        if not hasattr(Chrome, "_instance"):
            with Chrome._instance_lock:
                if not hasattr(Chrome, "_instance"):
                    Chrome.driver = Chrome.invokeDriver()
                    Chrome._instance = object.__new__(cls)
        return Chrome._instance

    @classmethod
    def invokeDriver(cls):
        driver_executable_path = r"C:\Users\miozus\.wdm\drivers\chromedriver\win32\98.0.4758.102\chromedriver.exe"
        service = ChromeService(executable_path=driver_executable_path)
        # 启用带插件的浏览器 + 双开
        option = webdriver.ChromeOptions()
        # option.add_argument("--user-data-dir="+r"C:/Users/miozus/AppData/Local/Google/Chrome/User Data/")
        option.add_argument("--user-data-dir="+r"C:/chrome_new/")
        # 启用手动退出
        # option.add_experimental_option("detach", True)
        # driver = webdriver.Chrome(service=service)
        driver = webdriver.Chrome(service=service, chrome_options=option)
        driver.set_window_size(1265, 1420)
        return driver


class mapping(object):
    """
    Restful 风格接口跳转的装饰器

    核心语句：driver.get(url)

    参数： 
        api 接口枚举类，中期改成字典，后期支持远程 mock 
    """

    _driver = Chrome().driver

    def __init__(self, url):
        self.url = url if not isinstance(url, Api) else url.value

    def __call__(self, func):
        @wraps(func)
        def wrapper(*args, **kwargs):
            log.info(f'[GET] {self.url} [{func.__name__}]')
            mapping._driver.get(self.url)
            return func(*args, **kwargs)

        return wrapper


driver = Chrome().driver
