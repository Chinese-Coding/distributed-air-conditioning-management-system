# distributed-air-conditioning-management-system (分布式空调管理系统)

# Attention !!!
本项目是基于 bupt 软件工程大作业---分布式空调管理系统的需求说明开发的. 本项目的代码是由我根据我们团队开发的代码修改而来. 完成了项目的全部需求, 可以给大家一个十分完善的参考.

# 1. 项目技术栈
- 开发框架: SpringBoot (kotlin)
- 构建工具: gradle (版本: 8.7, 语言: groovy)
- jdk运行版本: graalvm-jdk-21.0.3
- 数据库: PostgreSQL
- 持久化层框架: SpringBoot data JPA
- 前端框架: Vue3

# 2. 项目整体结构
本项目共分为四个部分: backend-master, backend-slave, front-master, front-slave. 分别对应该项目的四个分支 (目前 front-master 和 front-slave 还没有重构, 尽情期待).

# 3. 如何运行该项目的后端部分 
因为该项目基于 SpringBoot 框架开发, 所以熟悉 SpringBoot 框架的同学, 正确运行这个项目并不是难题, 因此可以忽略这部分.

## 3.1 使用 IDEA 克隆本项目并将分支切换为 backend-master
这一部分比较简单, 略

## 3.1 初始化数据库
首先从电脑上安装 PostgreSQL, 构建一个名为 `air_conditioner_system` 数据库, 并执行 backend-master 分支下 `/src/main/resources` 下的 `schema.sql` 和 `data.sql`.

## 3.2 修改 SpringBoot 配置文件以连接数据库
修改同一分支下, 同一文件路径下的`application.yml`, 将连接数据库的 `username` 和 `password` 改为自己的, 若您创建的数据库库名并非 `air_conditioner_system`, 则也需要修改 `url` 字段.

尝试从 IDEA 中启动项目.[^1]

## 3.3 运行从机代码
从另一台电脑或者现有电脑上重新 git 此仓库并使用 IDEA 打开, 将分支切换为 backedn-slave, 修改 resources 目录下 `config.json` 中的 `BASE_URL` 字段, 将其改为主机后端运行电脑上的 IP 和对应的监听端口. 并尝试运行从机后端程序.[^1]

## 3.4 测试接口
分别在浏览器输入 `http://localhost:8080/swagger-ui.html` 与 `http://localhost:8081/swagger-ui.html` 可分别打开主机后端和从机后端对应的接口界面[^2], 从这个界面就可以进行接口测试.

[^1]: 运行代码, 可能首先需要从网络上下载依赖 (这部分可能由 IDEA 自主完成), 请确保您的网络环境正常.
[^2]: 这里假定的还是主机和从机分别监听: 8080 和 8081 端口, 如果您在配置文件中修改了有关代码, 则改为对应程序监听的对应端口访问即可.


