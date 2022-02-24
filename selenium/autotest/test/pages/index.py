from common.model import *


class IndexPage(Page):
    @mapping.get(Api.HOME)
    def open(self):
        """
        打开首页
        """