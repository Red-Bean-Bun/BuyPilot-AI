# Repository Layer

Repo 层负责数据持久化（SQLModel + pgvector），包含 products、conversations、feedbacks 等表的读写操作。
当前为空 —— mock pipeline 不连接数据库。

当实现 8 张数据库表后，每个表的 DAO/Repo 放在此目录下。
