import threading
import sys

sys.path.append('e:\\projects\\IdeaProjects\\gulimall\\selenium')
from common.api.urls import Api
from common.aspect.chrome import Chrome

m1 = Chrome(Api.HOME)
m2 = Chrome(Api.HOME)

print("m1m2", m1, m2)


def task(arg):
    m = Chrome(arg)
    print(m)


def test_multiThread():
    for i in range(10):
        t = threading.Thread(target=task, args=[
            i,
        ])
        t.start()


if __name__ == "__main__":
    test_multiThread()