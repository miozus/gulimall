import threading

from selenium import webdriver
from selenium.webdriver.chrome.service import Service as ChromeService


class Chrome(object):
    """
    生成浏览器引擎的单例模式

    🔒 线程安全.使用方法:

    driver = Chrome().get_driver_singleton()

    exe: 初始化引擎，直接写死可执行文件的目录（否则安装驱的写法，需要关闭代理）
    """

    _instance_lock = threading.Lock()
    _driver = None

    def __init__(self) -> None:
        pass

    def __new__(cls, *args, **kwargs):
        if not hasattr(Chrome, "_instance"):
            with Chrome._instance_lock:
                if not hasattr(Chrome, "_instance"):
                    Chrome._driver = Chrome.invoke_driver()
                    Chrome._instance = object.__new__(cls)
        return Chrome._instance

    @classmethod
    def invoke_driver(cls):
        driver_executable_path = r"C:\Users\miozus\.wdm\drivers\chromedriver\win32\98.0.4758.102\chromedriver.exe"
        service = ChromeService(executable_path=driver_executable_path)
        option = webdriver.ChromeOptions()
        # 启用带插件的浏览器 + 双开
        option.add_argument("--user-data-dir=" + r"C:/chrome_new/")
        # 启用手动退出
        # option.add_experimental_option("detach", True)
        # 后台默默运行
        # option.add_argument("--headless")
        driver = webdriver.Chrome(service=service, chrome_options=option)
        # 调整窗口位置
        # driver.set_window_size(854, 1440)
        driver.set_window_size(1254, 1440)
        # driver.set_window_position(1706, 0)
        driver.set_window_position(1206, 0)
        return driver

    def get_driver_singleton(self):
        return Chrome._driver
