import shutil
import os

def clean(filepath):
    """ 清空目录中的缓存文件
        如果语句没有更改，编译遇到就返回结果了，不会再去执行，可能报错
    """
    files = os.listdir(filepath)
    for fd in files:
        cur_path = os.path.join(filepath, fd)            
        if os.path.isdir(cur_path):
            if fd == "__pycache__":
                print(f"rm -rf {cur_path}")
                shutil.rmtree(cur_path)
            else:
                clean(cur_path)

if __name__ == "__main__":
    clean(".\\")