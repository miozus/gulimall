import sys
import logging
import threading


class Logger4Python(object):

    _instance_lock = threading.Lock()
    _logger = None

    def __init__(self):
        pass

    def __new__(cls, *args, **kwargs):
        if not hasattr(Logger4Python, "_instance"):
            with Logger4Python._instance_lock:
                if not hasattr(Logger4Python, "_instance"):
                    Logger4Python._logger = Logger4Python.initLogger()
                    Logger4Python._instance = object.__new__(cls)
        return Logger4Python._instance

    @classmethod
    def initLogger(cls):
        logger = logging.getLogger('log')
        logger.setLevel(logging.DEBUG)

        # 调用模块时,如果错误引用，比如多次调用，每次会添加Handler，造成重复日志
        # 这边每次都移除掉所有的handler，后面在重新添加，可以解决这类问题

        while logger.hasHandlers():
            for handler in logger.handlers:
                logger.removeHandler(handler)

        # file log 写入文件配置
        formatter = logging.Formatter(
            '%(asctime)s %(levelname)s --- [%(funcName)s] %(module)s-L%(lineno)d : %(message)s'
        )  # 日志的格式
        log_dir = r'E:\projects\IdeaProjects\gulimall\selenium\test_report.log'
        fileHandler = logging.FileHandler(log_dir, encoding='utf-8')  # 日志文件路径文件名称，编码格式
        fileHandler.setLevel(logging.DEBUG)  # 日志打印级别
        fileHandler.setFormatter(formatter)
        logger.addHandler(fileHandler)

        # console log 控制台输出控制
        consoleHandler = logging.StreamHandler(sys.stdout)
        consoleHandler.setLevel(logging.DEBUG)
        consoleHandler.setFormatter(formatter)
        logger.addHandler(consoleHandler)

        return logger


log = Logger4Python()._logger