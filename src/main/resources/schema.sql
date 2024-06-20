DROP
    DATABASE IF EXISTS air_conditioner_system;
CREATE
    DATABASE air_conditioner_system;
-- USE DATABASE air_conditioner_system;

-- 顾客
DROP TABLE IF EXISTS users;
CREATE TABLE users
(
    id       bigserial PRIMARY KEY NOT NULL,
    name     VARCHAR(32) UNIQUE    NOT NULL,
    password VARCHAR(6)            NOT NULL DEFAULT '123456' -- 顾客的初始密码为 123456, 长度为 6 位数的数字, 和银行密码类似
);

-- 记录 room_id 和 user_id 之间的映射关系的表
DROP TABLE IF EXISTS room;
CREATE TABLE room
(
    id      bigserial PRIMARY KEY        NOT NULL,
    user_id bigint REFERENCES users (id) NOT NULL,
    inuse   boolean                      NOT NULL DEFAULT FALSE
);


-- 中央空调记录的每次从机启动
DROP TABLE IF EXISTS request;
CREATE TABLE request
(
    id          bigserial PRIMARY KEY        NOT NULL,
    room_id     bigint REFERENCES room (id)  NOT NULL,
    user_id     bigint REFERENCES users (id) NOT NULL,
    start_time  timestamp                    NOT NULL,
    stop_time   timestamp DEFAULT NULL,
    normal_exit boolean   DEFAULT NULL
);

-- 中央空调记录的每次温控请求详情
DROP TABLE IF EXISTS request_detail;
CREATE TABLE request_detail
(
    id         bigserial PRIMARY KEY          NOT NULL,
    request_id bigint REFERENCES request (id) NOT NULL,
    start_time timestamp                      NOT NULL,
    stop_time  timestamp                      NOT NULL,
    start_temp int                            NOT NULL,
    stop_temp  int                            NOT NULL,
    fan_speed  VARCHAR(6)                     NOT NULL DEFAULT 'MEDIUM', -- 风速等级
    total_fee  decimal(10, 2)                 NOT NULL
);