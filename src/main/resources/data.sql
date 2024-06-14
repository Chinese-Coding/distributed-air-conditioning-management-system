-- 分配用户
INSERT INTO users(id, name, password)
VALUES (1, 'czl', '123456'),
       (2, 'hbw', '123456'),
       (3, 'zfq', '123456'),
       (4, 'zl', '123456');
-- 为用户分配房间
INSERT INTO room(id, user_id, inuse)
VALUES (1, 1, FALSE),
       (2, 2, FALSE),
       (3, 3, FALSE),
       (4, 4, FALSE);

