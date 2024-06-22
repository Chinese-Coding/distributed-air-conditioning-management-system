# distributed-air-conditioning-management-system (分布式空调管理系统)

# Attention !!!
本项目是基于 bupt 软件工程大作业---分布式空调管理系统的需求说明开发的. 本项目的代码是由我根据我们团队开发的代码修改而来. 完成了项目的全部需求, 可以给大家一个十分完善的参考.

# 1. 项目技术栈
- 开发框架: SpringBoot (kotlin)
- 数据库: PostgreSQL
- 持久化层框架: SpringBoot data JPA
- 前端框架: Vue3

# 2. 项目整体结构
本项目共分为四个部分: backend-master, backend-slave, front-master, front-slave. 分别对应该项目的四个分支 (目前 front-master 和 front-slave 还没有重构, 尽情期待).

# 3. 如何运行该项目

## 3.1 使用 IDEA 克隆本项目并将分支切换为 backend-master
这一部分比较简单, 略

## 3.1 初始化数据库
首先从电脑上安装 PostgreSQL, 构建一个名为 `air_conditioner_system` 数据库, 并执行 backend-master 分支下 `/src/main/resources` 下的 `schema.sql` 和 `data.sql`.

## 3.2 修改 SpringBoot 配置文件以连接数据库
修改同一分支下, 同一文件路径下的`application.yml`, 将连接数据库的 `username` 和 `password` 改为自己的. 尝试从 IDEA 中启动项目
