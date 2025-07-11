-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Jul 11, 2025 at 12:25 PM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.0.30

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `focusbuddy`
--

-- --------------------------------------------------------

--
-- Stand-in structure for view `daily_productivity`
-- (See below for the actual view)
--
CREATE TABLE `daily_productivity` (
`session_date` date
,`user_id` int(11)
,`username` varchar(50)
,`total_sessions` bigint(21)
,`focus_minutes` decimal(32,0)
,`break_minutes` decimal(32,0)
,`tasks_completed` bigint(21)
,`daily_mood` decimal(14,4)
);

-- --------------------------------------------------------

--
-- Table structure for table `focus_sessions`
--

CREATE TABLE `focus_sessions` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `task_id` int(11) DEFAULT NULL,
  `duration_minutes` int(11) NOT NULL,
  `session_date` date NOT NULL,
  `session_type` enum('FOCUS','BREAK') DEFAULT 'FOCUS',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `focus_sessions`
--

INSERT INTO `focus_sessions` (`id`, `user_id`, `task_id`, `duration_minutes`, `session_date`, `session_type`, `created_at`) VALUES
(1, 8, NULL, 25, '2025-07-01', 'FOCUS', '2025-07-01 06:44:13'),
(2, 8, NULL, 25, '2025-07-01', 'FOCUS', '2025-07-01 07:20:54'),
(3, 8, NULL, 25, '2025-07-10', 'FOCUS', '2025-07-10 07:09:48');

--
-- Triggers `focus_sessions`
--
DELIMITER $$
CREATE TRIGGER `log_focus_session` AFTER INSERT ON `focus_sessions` FOR EACH ROW BEGIN
    IF NEW.session_type = 'FOCUS' THEN
        UPDATE goals 
        SET current_value = current_value + 1
        WHERE user_id = NEW.user_id 
        AND goal_type = 'FOCUS_SESSIONS' 
        AND status = 'ACTIVE';
    END IF;
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Table structure for table `goals`
--

CREATE TABLE `goals` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `title` varchar(200) NOT NULL,
  `description` text DEFAULT NULL,
  `target_value` int(11) NOT NULL,
  `current_value` int(11) DEFAULT 0,
  `goal_type` enum('STUDY_HOURS','TASKS_COMPLETED','FOCUS_SESSIONS') DEFAULT 'STUDY_HOURS',
  `target_date` date DEFAULT NULL,
  `status` enum('ACTIVE','COMPLETED','PAUSED') DEFAULT 'ACTIVE',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `goals`
--

INSERT INTO `goals` (`id`, `user_id`, `title`, `description`, `target_value`, `current_value`, `goal_type`, `target_date`, `status`, `created_at`, `updated_at`) VALUES
(4, 8, 'PBO', 'Project PBO Mau selesai', 1, 14, 'TASKS_COMPLETED', '2025-07-01', 'COMPLETED', '2025-07-01 08:47:27', '2025-07-11 10:15:27'),
(5, 8, 'Fokus 25 menit', 'Testing pomodoro', 1, 3, 'FOCUS_SESSIONS', '2025-07-10', 'COMPLETED', '2025-07-10 06:13:45', '2025-07-11 10:15:28'),
(6, 8, 'PBW', 'PBW', 4, 14, 'TASKS_COMPLETED', '2025-07-10', 'COMPLETED', '2025-07-10 07:13:28', '2025-07-11 10:15:27'),
(7, 8, 'Selesaikan Tugas PBO', 'PBO', 3, 14, 'TASKS_COMPLETED', '2025-07-10', 'COMPLETED', '2025-07-10 08:00:19', '2025-07-11 10:15:27'),
(8, 9, 'Tugas PBO', 'PBO', 4, 3, 'TASKS_COMPLETED', '2025-07-10', 'ACTIVE', '2025-07-10 08:33:24', '2025-07-11 10:15:27'),
(9, 9, 'Tugas', 'Tugas', 4, 3, 'TASKS_COMPLETED', '2025-07-10', 'ACTIVE', '2025-07-10 08:53:01', '2025-07-11 10:15:27'),
(10, 10, 'Praktikum PBO', 'P1-4', 4, 4, 'TASKS_COMPLETED', '2025-07-10', 'COMPLETED', '2025-07-10 08:57:43', '2025-07-10 09:00:16'),
(11, 10, 'Project PBW', 'kumpulkan 4 tugas', 4, 2, 'TASKS_COMPLETED', '2025-07-11', 'ACTIVE', '2025-07-11 10:17:23', '2025-07-11 10:18:29');

-- --------------------------------------------------------

--
-- Table structure for table `goal_trigger_log`
--

CREATE TABLE `goal_trigger_log` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `task_id` int(11) DEFAULT NULL,
  `goal_type` enum('TASKS_COMPLETED','FOCUS_SESSIONS','STUDY_HOURS') DEFAULT NULL,
  `action` varchar(50) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `goal_trigger_log`
--

INSERT INTO `goal_trigger_log` (`id`, `user_id`, `task_id`, `goal_type`, `action`, `created_at`) VALUES
(1, 10, 23, 'TASKS_COMPLETED', 'INCREMENT', '2025-07-11 10:17:48'),
(2, 10, 24, 'TASKS_COMPLETED', 'INCREMENT', '2025-07-11 10:18:29');

-- --------------------------------------------------------

--
-- Table structure for table `migrations`
--

CREATE TABLE `migrations` (
  `id` int(11) NOT NULL,
  `migration_name` varchar(255) NOT NULL,
  `executed_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `migrations`
--

INSERT INTO `migrations` (`id`, `migration_name`, `executed_at`) VALUES
(1, 'initial_schema', '2025-06-25 08:08:45'),
(2, 'add_salt_column', '2025-06-25 08:08:45'),
(3, 'add_updated_at_tasks', '2025-06-25 08:08:45'),
(4, 'create_views', '2025-06-25 08:08:45'),
(5, 'create_procedures', '2025-06-25 08:08:45'),
(6, 'create_triggers', '2025-06-25 08:08:45'),
(7, 'create_indexes', '2025-06-25 08:08:45'),
(8, 'add_category_notes', '2025-07-01 02:27:00');

-- --------------------------------------------------------

--
-- Table structure for table `mood_entries`
--

CREATE TABLE `mood_entries` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `mood_level` int(11) DEFAULT NULL CHECK (`mood_level` between 1 and 5),
  `mood_description` varchar(500) DEFAULT NULL,
  `entry_date` date NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `mood_entries`
--

INSERT INTO `mood_entries` (`id`, `user_id`, `mood_level`, `mood_description`, `entry_date`, `created_at`) VALUES
(1, 8, 5, 'saya bahagia mepet deadline', '2025-07-01', '2025-07-01 03:59:07'),
(2, 8, 4, 'Happy sekali', '2025-07-03', '2025-07-03 01:41:08'),
(3, 10, 3, '', '2025-07-10', '2025-07-10 09:01:54');

-- --------------------------------------------------------

--
-- Table structure for table `notes`
--

CREATE TABLE `notes` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `title` varchar(200) NOT NULL,
  `content` longtext DEFAULT NULL,
  `tags` varchar(500) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `category` varchar(50) DEFAULT 'General'
) ;

--
-- Dumping data for table `notes`
--

INSERT INTO `notes` (`id`, `user_id`, `title`, `content`, `tags`, `created_at`, `updated_at`, `category`) VALUES
(1, 8, 'Catatan PBO', '<html dir=\"ltr\"><head></head><body contenteditable=\"true\"><p><span style=\"font-family: &quot;&quot;; font-weight: bold; color: rgb(0, 26, 128);\">Projectnya susah bangettt</span></p></body></html>', 'project', '2025-06-30 19:29:54', '2025-07-02 23:01:14', 'General'),
(2, 8, 'Catatan PBW', '<html dir=\"ltr\"><head></head><body contenteditable=\"true\"><p><span style=\"font-family: &quot;&quot;;\">Website yang saya bikin berjudul <span style=\"font-weight: bold; color: rgb(255, 0, 255);\">dapoer SS</span> yang menjual berbagai macam kue dan produk makanan lainnya.</span></p></body></html>', 'website', '2025-07-02 18:20:26', '2025-07-09 18:36:13', 'General'),
(3, 8, 'Project PBO', '<html dir=\"ltr\"><head></head><body contenteditable=\"true\"><p style=\"text-align: left;\"><span style=\"font-family: &quot;Bell MT&quot;; background-color: rgb(255, 255, 255);\">Hari ini sudah perkembangan <span style=\"font-weight: bold;\">database</span></span></p></body></html>', 'progress', '2025-07-03 00:41:16', '2025-07-03 02:02:10', 'General'),
(4, 10, 'PBO', '<html dir=\"ltr\"><head></head><body contenteditable=\"true\"><h1><span style=\"font-family: &quot;&quot;;\">Hari ini projek PBO selesai</span></h1></body></html>', 'tugas', '2025-07-10 02:03:14', '2025-07-10 02:03:57', 'Archive');

-- --------------------------------------------------------

--
-- Table structure for table `tasks`
--

CREATE TABLE `tasks` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `title` varchar(200) NOT NULL,
  `description` text DEFAULT NULL,
  `priority` enum('LOW','MEDIUM','HIGH') DEFAULT 'MEDIUM',
  `status` enum('PENDING','IN_PROGRESS','COMPLETED') DEFAULT 'PENDING',
  `due_date` date DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `tasks`
--

INSERT INTO `tasks` (`id`, `user_id`, `title`, `description`, `priority`, `status`, `due_date`, `created_at`, `updated_at`) VALUES
(1, 7, 'Project PBO', 'Semoga selesai', 'HIGH', 'COMPLETED', '2025-07-01', '2025-06-30 10:27:02', '2025-06-30 10:27:09'),
(2, 8, 'Project PBO', 'Membuat aplikasi secara berkelompok', 'HIGH', 'COMPLETED', '2025-07-01', '2025-06-30 19:31:37', '2025-06-30 19:32:07'),
(3, 8, 'Project PBW', 'Susah Juga', 'HIGH', 'COMPLETED', '2025-07-02', '2025-06-30 20:16:41', '2025-06-30 20:17:04'),
(4, 8, 'Project Komstat', 'dikit lagi siap', 'HIGH', 'COMPLETED', '2025-07-02', '2025-06-30 20:20:28', '2025-06-30 20:21:17'),
(5, 8, 'Tugas Metnum', 'Newton', 'HIGH', 'COMPLETED', '2025-07-01', '2025-06-30 20:48:29', '2025-06-30 20:48:44'),
(6, 8, 'Anareg', 'praktikum', 'HIGH', 'COMPLETED', '2025-07-02', '2025-06-30 23:52:02', '2025-07-01 19:23:35'),
(7, 8, 'Anareg', 'Praktikum 2', 'HIGH', 'COMPLETED', '2025-07-02', '2025-06-30 23:52:30', '2025-06-30 23:52:30'),
(8, 8, 'PBO', 'Praktikum PBO', 'HIGH', 'COMPLETED', '2025-07-01', '2025-06-30 23:55:18', '2025-06-30 23:55:18'),
(9, 8, 'PBO', 'Project PBO mau selesai', 'HIGH', 'COMPLETED', '2025-07-01', '2025-07-01 01:48:06', '2025-07-01 19:38:10'),
(10, 8, 'Praktikum Anareg', 'Pertemuan 13', 'HIGH', 'COMPLETED', '2025-07-03', '2025-07-01 19:30:11', '2025-07-01 19:38:13'),
(11, 8, 'Tugas Metnum', 'newton 5 titik', 'HIGH', 'COMPLETED', '2025-07-03', '2025-07-03 02:00:08', '2025-07-03 02:00:26'),
(12, 8, 'PBW 1', 'PBW 1', 'HIGH', 'COMPLETED', '2025-07-10', '2025-07-10 00:14:11', '2025-07-10 00:14:21'),
(13, 8, 'PBW 2', 'PBW 2', 'HIGH', 'COMPLETED', '2025-07-10', '2025-07-10 00:15:10', '2025-07-10 00:15:16'),
(14, 8, 'PBO 6', 'PBO', 'HIGH', 'COMPLETED', '2025-07-10', '2025-07-10 01:00:46', '2025-07-10 01:24:36'),
(15, 8, 'PBO 7', 'PBO', 'HIGH', 'COMPLETED', '2025-07-10', '2025-07-10 01:25:15', '2025-07-10 01:25:19'),
(16, 9, 'PBO 1', 'PBO 1', 'HIGH', 'COMPLETED', '2025-07-10', '2025-07-10 01:33:50', '2025-07-10 01:33:55'),
(17, 9, 'PBO 2', 'PBO 2', 'HIGH', 'COMPLETED', '2025-07-10', '2025-07-10 01:34:22', '2025-07-10 01:34:27'),
(18, 9, 'PBO 3', 'PBO 3', 'HIGH', 'COMPLETED', '2025-07-10', '2025-07-10 01:34:53', '2025-07-10 01:34:54'),
(19, 10, 'PBO 1', 'PBO 1', 'HIGH', 'COMPLETED', '2025-07-10', '2025-07-10 01:58:29', '2025-07-10 02:00:18'),
(20, 10, 'PBO 3', 'PBO', 'HIGH', 'COMPLETED', '2025-07-10', '2025-07-10 01:59:07', '2025-07-10 02:00:17'),
(21, 10, 'PBO 3', 'PBO 3', 'HIGH', 'COMPLETED', '2025-07-10', '2025-07-10 01:59:35', '2025-07-10 02:00:24'),
(22, 10, 'PBO 4', 'PBO 4', 'HIGH', 'COMPLETED', '2025-07-10', '2025-07-10 01:59:59', '2025-07-10 02:00:15'),
(23, 10, 'PBW 1', 'PBW 1', 'HIGH', 'COMPLETED', '2025-07-11', '2025-07-11 03:17:46', '2025-07-11 03:17:48'),
(24, 10, 'PBW 2', 'PBW 2', 'MEDIUM', 'COMPLETED', '2025-07-11', '2025-07-11 03:18:24', '2025-07-11 03:18:29');

--
-- Triggers `tasks`
--
DELIMITER $$
CREATE TRIGGER `update_goal_on_task_completion` AFTER UPDATE ON `tasks` FOR EACH ROW BEGIN
    -- Only trigger when task status changes from non-COMPLETED to COMPLETED
    IF NEW.status = 'COMPLETED' AND OLD.status != 'COMPLETED' THEN
        
        -- Update task completion goals
        UPDATE goals 
        SET current_value = current_value + 1,
            status = CASE 
                WHEN current_value + 1 >= target_value THEN 'COMPLETED'
                ELSE status
            END,
            updated_at = NOW()
        WHERE user_id = NEW.user_id 
        AND goal_type = 'TASKS_COMPLETED' 
        AND status = 'ACTIVE';
        
        -- Log the trigger action (for debugging)
        INSERT INTO goal_trigger_log (user_id, task_id, goal_type, action, created_at)
        VALUES (NEW.user_id, NEW.id, 'TASKS_COMPLETED', 'INCREMENT', NOW())
        ON DUPLICATE KEY UPDATE created_at = NOW();
        
    END IF;
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Stand-in structure for view `task_overview`
-- (See below for the actual view)
--
CREATE TABLE `task_overview` (
`id` int(11)
,`title` varchar(200)
,`description` text
,`priority` enum('LOW','MEDIUM','HIGH')
,`status` enum('PENDING','IN_PROGRESS','COMPLETED')
,`due_date` date
,`created_at` timestamp
,`username` varchar(50)
,`full_name` varchar(100)
,`urgency_status` varchar(9)
);

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `id` int(11) NOT NULL,
  `username` varchar(50) NOT NULL,
  `password` varchar(255) NOT NULL,
  `salt` varchar(255) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `full_name` varchar(100) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`id`, `username`, `password`, `salt`, `email`, `full_name`, `created_at`) VALUES
(1, 'demo', 'hashed_password_placeholder', 'salt_placeholder', 'demo@focusbuddy.com', 'Demo User', '2025-06-25 08:08:43'),
(2, 'student1', 'password123', NULL, 'student1@university.edu', 'John Doe', '2025-06-25 08:08:43'),
(3, 'student2', 'password123', NULL, 'student2@university.edu', 'Jane Smith', '2025-06-25 08:08:43'),
(7, 'imka', 'Lh12mF1GJqR/wkr8it416Y4furQHm4uyzHkJoGlcNcw=', 'i0kY8lP0eDocfX8btL9zog==', 'imka@mail.com', 'imka123', '2025-06-25 08:22:51'),
(8, 'satria', '8v3pq6Rwl0g1gdIthYssokCIRSR2OEPvFFOxVj9KfBw=', 'IP4NK76DpvQGwctTxHjchA==', '222313372@stis.ac.id', 'satria imka', '2025-06-30 14:06:08'),
(9, 'putra', 'g0y69XqQjErMTz1v6nIno6uPaqIxy6fN6tF1KQDjxFE=', 'etSBBJYMXaWIqB7g8yLlFA==', 'putra@gmail.com', 'Imka Putra', '2025-07-10 01:23:49'),
(10, 'winni', '1P+mq4ZNZDhAsSsVyTHofwV6LXS1MQ8pPAu3R6Ivrpc=', 'TFZy99dW698G4yZtnKLHlA==', '222313331@stis.ac.id', 'winni elfira', '2025-07-10 08:55:51');

-- --------------------------------------------------------

--
-- Stand-in structure for view `user_statistics`
-- (See below for the actual view)
--
CREATE TABLE `user_statistics` (
`id` int(11)
,`username` varchar(50)
,`full_name` varchar(100)
,`total_tasks` bigint(21)
,`completed_tasks` bigint(21)
,`pending_tasks` bigint(21)
,`mood_entries_count` bigint(21)
,`average_mood` decimal(14,4)
,`total_focus_sessions` bigint(21)
,`total_focus_minutes` decimal(32,0)
,`total_notes` bigint(21)
,`total_goals` bigint(21)
,`completed_goals` bigint(21)
);

-- --------------------------------------------------------

--
-- Structure for view `daily_productivity`
--
DROP TABLE IF EXISTS `daily_productivity`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `daily_productivity`  AS SELECT cast(`fs`.`session_date` as date) AS `session_date`, `fs`.`user_id` AS `user_id`, `u`.`username` AS `username`, count(`fs`.`id`) AS `total_sessions`, sum(case when `fs`.`session_type` = 'FOCUS' then `fs`.`duration_minutes` else 0 end) AS `focus_minutes`, sum(case when `fs`.`session_type` = 'BREAK' then `fs`.`duration_minutes` else 0 end) AS `break_minutes`, count(distinct case when `t`.`status` = 'COMPLETED' and cast(`t`.`updated_at` as date) = cast(`fs`.`session_date` as date) then `t`.`id` end) AS `tasks_completed`, avg(`me`.`mood_level`) AS `daily_mood` FROM (((`focus_sessions` `fs` join `users` `u` on(`fs`.`user_id` = `u`.`id`)) left join `tasks` `t` on(`fs`.`user_id` = `t`.`user_id`)) left join `mood_entries` `me` on(`fs`.`user_id` = `me`.`user_id` and `me`.`entry_date` = cast(`fs`.`session_date` as date))) GROUP BY cast(`fs`.`session_date` as date), `fs`.`user_id`, `u`.`username` ORDER BY cast(`fs`.`session_date` as date) DESC ;

-- --------------------------------------------------------

--
-- Structure for view `task_overview`
--
DROP TABLE IF EXISTS `task_overview`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `task_overview`  AS SELECT `t`.`id` AS `id`, `t`.`title` AS `title`, `t`.`description` AS `description`, `t`.`priority` AS `priority`, `t`.`status` AS `status`, `t`.`due_date` AS `due_date`, `t`.`created_at` AS `created_at`, `u`.`username` AS `username`, `u`.`full_name` AS `full_name`, CASE WHEN `t`.`due_date` < curdate() AND `t`.`status` <> 'COMPLETED' THEN 'OVERDUE' WHEN `t`.`due_date` = curdate() AND `t`.`status` <> 'COMPLETED' THEN 'DUE_TODAY' WHEN `t`.`due_date` between curdate() and curdate() + interval 3 day AND `t`.`status` <> 'COMPLETED' THEN 'DUE_SOON' ELSE 'NORMAL' END AS `urgency_status` FROM (`tasks` `t` join `users` `u` on(`t`.`user_id` = `u`.`id`)) ORDER BY CASE `t`.`priority` WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 WHEN 'LOW' THEN 3 END ASC, `t`.`due_date` ASC ;

-- --------------------------------------------------------

--
-- Structure for view `user_statistics`
--
DROP TABLE IF EXISTS `user_statistics`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `user_statistics`  AS SELECT `u`.`id` AS `id`, `u`.`username` AS `username`, `u`.`full_name` AS `full_name`, count(distinct `t`.`id`) AS `total_tasks`, count(distinct case when `t`.`status` = 'COMPLETED' then `t`.`id` end) AS `completed_tasks`, count(distinct case when `t`.`status` = 'PENDING' then `t`.`id` end) AS `pending_tasks`, count(distinct `me`.`id`) AS `mood_entries_count`, avg(`me`.`mood_level`) AS `average_mood`, count(distinct `fs`.`id`) AS `total_focus_sessions`, sum(`fs`.`duration_minutes`) AS `total_focus_minutes`, count(distinct `n`.`id`) AS `total_notes`, count(distinct `g`.`id`) AS `total_goals`, count(distinct case when `g`.`status` = 'COMPLETED' then `g`.`id` end) AS `completed_goals` FROM (((((`users` `u` left join `tasks` `t` on(`u`.`id` = `t`.`user_id`)) left join `mood_entries` `me` on(`u`.`id` = `me`.`user_id`)) left join `focus_sessions` `fs` on(`u`.`id` = `fs`.`user_id`)) left join `notes` `n` on(`u`.`id` = `n`.`user_id`)) left join `goals` `g` on(`u`.`id` = `g`.`user_id`)) GROUP BY `u`.`id`, `u`.`username`, `u`.`full_name` ;

--
-- Indexes for dumped tables
--

--
-- Indexes for table `focus_sessions`
--
ALTER TABLE `focus_sessions`
  ADD PRIMARY KEY (`id`),
  ADD KEY `task_id` (`task_id`),
  ADD KEY `idx_user_date` (`user_id`,`session_date`),
  ADD KEY `idx_focus_sessions_date_type` (`session_date`,`session_type`);

--
-- Indexes for table `goals`
--
ALTER TABLE `goals`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_user_status` (`user_id`,`status`),
  ADD KEY `idx_goals_user_type_status` (`user_id`,`goal_type`,`status`);

--
-- Indexes for table `goal_trigger_log`
--
ALTER TABLE `goal_trigger_log`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_user_type` (`user_id`,`goal_type`),
  ADD KEY `idx_created` (`created_at`);

--
-- Indexes for table `migrations`
--
ALTER TABLE `migrations`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `unique_migration` (`migration_name`);

--
-- Indexes for table `mood_entries`
--
ALTER TABLE `mood_entries`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `unique_user_date` (`user_id`,`entry_date`),
  ADD KEY `idx_user_date` (`user_id`,`entry_date`),
  ADD KEY `idx_mood_entries_date` (`entry_date`);

--
-- Indexes for table `notes`
--
ALTER TABLE `notes`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_user_id` (`user_id`),
  ADD KEY `idx_notes_updated_at` (`updated_at`),
  ADD KEY `idx_notes_category` (`category`);
ALTER TABLE `notes` ADD FULLTEXT KEY `ft_title_content` (`title`,`content`);

--
-- Indexes for table `tasks`
--
ALTER TABLE `tasks`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_user_id` (`user_id`),
  ADD KEY `idx_status` (`status`),
  ADD KEY `idx_due_date` (`due_date`),
  ADD KEY `idx_tasks_user_status` (`user_id`,`status`),
  ADD KEY `idx_tasks_due_date_status` (`due_date`,`status`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `username` (`username`),
  ADD KEY `idx_username` (`username`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `focus_sessions`
--
ALTER TABLE `focus_sessions`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT for table `goals`
--
ALTER TABLE `goals`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=12;

--
-- AUTO_INCREMENT for table `goal_trigger_log`
--
ALTER TABLE `goal_trigger_log`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `migrations`
--
ALTER TABLE `migrations`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=9;

--
-- AUTO_INCREMENT for table `mood_entries`
--
ALTER TABLE `mood_entries`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT for table `notes`
--
ALTER TABLE `notes`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `tasks`
--
ALTER TABLE `tasks`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=25;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `focus_sessions`
--
ALTER TABLE `focus_sessions`
  ADD CONSTRAINT `focus_sessions_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `focus_sessions_ibfk_2` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`) ON DELETE SET NULL;

--
-- Constraints for table `goals`
--
ALTER TABLE `goals`
  ADD CONSTRAINT `goals_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `mood_entries`
--
ALTER TABLE `mood_entries`
  ADD CONSTRAINT `mood_entries_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `notes`
--
ALTER TABLE `notes`
  ADD CONSTRAINT `notes_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `tasks`
--
ALTER TABLE `tasks`
  ADD CONSTRAINT `tasks_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
