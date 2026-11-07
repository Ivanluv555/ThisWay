-- Seed data: 5 rows for each table in cidi-db
-- Run after schema initialization

START TRANSACTION;

INSERT INTO `admin` (`adminid`, `job`, `username`, `create_time`, `password`, `role`) VALUES
(1, 'platform_ops', 'admin01', '2026-03-01 09:00:00.000000', 'hash_admin01', 'super_admin'),
(2, 'content_audit', 'admin02', '2026-03-01 09:10:00.000000', 'hash_admin02', 'content_admin'),
(3, 'user_support', 'admin03', '2026-03-01 09:20:00.000000', 'hash_admin03', 'ops_admin'),
(4, 'data_analyst', 'admin04', '2026-03-01 09:30:00.000000', 'hash_admin04', 'analyst_admin'),
(5, 'security', 'admin05', '2026-03-01 09:40:00.000000', 'hash_admin05', 'security_admin');

INSERT INTO `categories` (`id`, `name`, `sort_order`, `is_active`, `icon_url`) VALUES
(1, 'food', 1, 1, 'https://img.example.com/icons/food.png'),
(2, 'scenery', 2, 1, 'https://img.example.com/icons/scenery.png'),
(3, 'museum', 3, 1, 'https://img.example.com/icons/museum.png'),
(4, 'nightlife', 4, 1, 'https://img.example.com/icons/nightlife.png'),
(5, 'shopping', 5, 1, 'https://img.example.com/icons/shopping.png');

INSERT INTO `users` (`id`, `openid`, `username`, `avatar_url`, `bio`, `create_time`, `last_login_time`, `status`, `phone`) VALUES
(1, 'wx_openid_0001', 'alice', 'https://img.example.com/avatar/u1.png', 'city walker', '2026-03-01 10:00:00', '2026-03-22 10:10:00.000000', 1, '13800000001'),
(2, 'wx_openid_0002', 'bob', 'https://img.example.com/avatar/u2.png', 'food hunter', '2026-03-01 10:05:00', '2026-03-22 10:20:00.000000', 1, '13800000002'),
(3, 'wx_openid_0003', 'cindy', 'https://img.example.com/avatar/u3.png', 'photo lover', '2026-03-01 10:10:00', '2026-03-22 10:30:00.000000', 1, '13800000003'),
(4, 'wx_openid_0004', 'david', 'https://img.example.com/avatar/u4.png', 'museum fan', '2026-03-01 10:15:00', '2026-03-22 10:40:00.000000', 1, '13800000004'),
(5, 'wx_openid_0005', 'emma', 'https://img.example.com/avatar/u5.png', 'travel planner', '2026-03-01 10:20:00', '2026-03-22 10:50:00.000000', 1, '13800000005');

INSERT INTO `locations` (`id`, `title`, `latitude`, `longitude`, `address`, `description`, `heat_score`, `category`, `rating`) VALUES
(1, 'West Lake', 30.243060, 120.150164, 'Hangzhou, Zhejiang', 'Classic city lake and park area', 92, 'scenery', 4.8),
(2, 'Lingyin Temple', 30.242118, 120.104120, 'Xihu District, Hangzhou', 'Historic Buddhist temple with hill views', 88, 'culture', 4.7),
(3, 'Song City', 30.183005, 120.102214, 'Zhijiang Road, Hangzhou', 'Theme park with live performance', 85, 'nightlife', 4.5),
(4, 'Intime Mall', 30.257800, 120.170900, 'Hubin Road, Hangzhou', 'Urban shopping destination', 79, 'shopping', 4.4),
(5, 'Zhejiang Museum', 30.258751, 120.142883, 'Gushan Road, Hangzhou', 'Provincial museum with local history', 74, 'museum', 4.6);

INSERT INTO `posts` (`id`, `user_id`, `category_id`, `location_id`, `title`, `content`, `create_time`, `like_count`, `view_count`) VALUES
(1, 1, 2, 1, 'Morning at West Lake', 'Great weather and calm water this morning.', '2026-03-10 08:10:00', 23, 300),
(2, 2, 1, 2, 'Temple snacks guide', 'Top 3 snacks around Lingyin Temple.', '2026-03-10 12:30:00', 17, 240),
(3, 3, 4, 3, 'Night show review', 'Performance was better than expected.', '2026-03-11 20:45:00', 34, 420),
(4, 4, 3, 5, 'Museum route', 'A 2-hour route for first-time visitors.', '2026-03-12 14:15:00', 12, 180),
(5, 5, 5, 4, 'Shopping tips', 'How to avoid peak-hour queues.', '2026-03-13 16:00:00', 19, 260);

INSERT INTO `post_images` (`id`, `post_id`, `image_url`, `sort_order`, `height`, `width`) VALUES
(1, 1, 'https://img.example.com/posts/p1_1.jpg', 1, 1080, 1440),
(2, 2, 'https://img.example.com/posts/p2_1.jpg', 1, 1080, 1440),
(3, 3, 'https://img.example.com/posts/p3_1.jpg', 1, 1080, 1440),
(4, 4, 'https://img.example.com/posts/p4_1.jpg', 1, 1080, 1440),
(5, 5, 'https://img.example.com/posts/p5_1.jpg', 1, 1080, 1440);

INSERT INTO `comments` (`id`, `post_id`, `user_id`, `content`, `create_time`, `parent_id`) VALUES
(1, 1, 2, 'Nice shot, looks peaceful!', '2026-03-10 09:00:00', NULL),
(2, 1, 3, 'I want to go there this weekend.', '2026-03-10 09:20:00', 1),
(3, 2, 4, 'Can confirm snack #2 is great.', '2026-03-10 13:10:00', NULL),
(4, 3, 1, 'Thanks for the review.', '2026-03-11 21:00:00', NULL),
(5, 5, 2, 'Very practical tips!', '2026-03-13 16:40:00', NULL);

INSERT INTO `actions` (`id`, `user_id`, `post_id`, `type`, `create_time`) VALUES
(1, 1, 3, 'like', '2026-03-11 21:05:00'),
(2, 2, 1, 'collect', '2026-03-10 09:25:00'),
(3, 3, 2, 'like', '2026-03-10 13:20:00'),
(4, 4, 5, 'collect', '2026-03-13 17:00:00'),
(5, 5, 4, 'like', '2026-03-12 15:00:00');

INSERT INTO `feedbacks` (`id`, `user_id`, `content`, `contact_info`, `status`, `create_time`) VALUES
(1, 1, 'Map marker overlaps at high zoom levels.', 'alice@example.com', 0, '2026-03-15 10:00:00'),
(2, 2, 'Search results should support typo correction.', 'bob@example.com', 1, '2026-03-15 11:00:00'),
(3, 3, 'Dark mode text contrast is low on one page.', 'cindy@example.com', 0, '2026-03-15 12:00:00'),
(4, 4, 'Please add transport suggestions in location detail.', 'david@example.com', 2, '2026-03-15 13:00:00'),
(5, NULL, 'Guest feedback: page loading is slow in weak network.', 'guest@example.com', 0, '2026-03-15 14:00:00');

INSERT INTO `feedback_images` (`id`, `feedback_id`, `image_url`) VALUES
(1, 1, 'https://img.example.com/feedback/f1.png'),
(2, 2, 'https://img.example.com/feedback/f2.png'),
(3, 3, 'https://img.example.com/feedback/f3.png'),
(4, 4, 'https://img.example.com/feedback/f4.png'),
(5, 5, 'https://img.example.com/feedback/f5.png');

INSERT INTO `hot_searches` (`id`, `keyword`, `hot_value`, `is_active`) VALUES
(1, 'west lake', 980, 1),
(2, 'lingyin', 870, 1),
(3, 'song city show', 760, 1),
(4, 'hangzhou museum', 640, 1),
(5, 'weekend trip', 590, 1);

INSERT INTO `search_history` (`id`, `user_id`, `keyword`, `is_deleted`, `create_time`) VALUES
(1, 1, 'west lake route', 0, '2026-03-20 09:00:00'),
(2, 2, 'temple snacks', 0, '2026-03-20 09:05:00'),
(3, 3, 'song city ticket', 0, '2026-03-20 09:10:00'),
(4, 4, 'museum opening hours', 0, '2026-03-20 09:15:00'),
(5, 5, 'mall parking', 0, '2026-03-20 09:20:00');

INSERT INTO `spot_traffic_predictions` (`id`, `spot_id`, `predict_time`, `visitor_count`, `congestion_level`, `trend`, `create_time`, `crowd_density`, `traffic_level`) VALUES
(1, 1, '2026-03-23 10:00:00', 3600, 2, 'up', '2026-03-22 08:00:00', 72.50, 3),
(2, 2, '2026-03-23 11:00:00', 2400, 1, 'down', '2026-03-22 08:05:00', 54.20, 2),
(3, 3, '2026-03-23 19:00:00', 4100, 3, 'up', '2026-03-22 08:10:00', 81.70, 4),
(4, 4, '2026-03-23 15:00:00', 3000, 2, 'flat', '2026-03-22 08:15:00', 67.90, 3),
(5, 5, '2026-03-23 14:00:00', 1800, 1, 'down', '2026-03-22 08:20:00', 43.60, 2);

INSERT INTO `user_follows` (`id`, `follower_id`, `followed_id`, `create_time`) VALUES
(1, 1, 2, '2026-03-18 10:00:00'),
(2, 2, 3, '2026-03-18 10:10:00'),
(3, 3, 4, '2026-03-18 10:20:00'),
(4, 4, 5, '2026-03-18 10:30:00'),
(5, 5, 1, '2026-03-18 10:40:00');

INSERT INTO `user_settings` (`user_id`, `is_private_account`, `push_notifications`, `theme_color`, `update_time`) VALUES
(1, 0, 1, 'system', '2026-03-22 09:00:00'),
(2, 1, 1, 'dark', '2026-03-22 09:05:00'),
(3, 0, 0, 'light', '2026-03-22 09:10:00'),
(4, 0, 1, 'system', '2026-03-22 09:15:00'),
(5, 1, 0, 'dark', '2026-03-22 09:20:00');

INSERT INTO `location_images` (`id`, `location_id`, `image_url`, `sort_order`, `is_cover`) VALUES
(1, 1, 'https://img.example.com/locations/l1.jpg', 1, 1),
(2, 2, 'https://img.example.com/locations/l2.jpg', 1, 1),
(3, 3, 'https://img.example.com/locations/l3.jpg', 1, 1),
(4, 4, 'https://img.example.com/locations/l4.jpg', 1, 1),
(5, 5, 'https://img.example.com/locations/l5.jpg', 1, 1);

COMMIT;
