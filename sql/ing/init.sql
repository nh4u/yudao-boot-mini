/* 问卷管理系统数据库设计
  适用场景：老师评价学生，多维度加权，线性量表(1-10)
*/

-- 1. 维度字典表 (例如：专注力、逻辑思维、社交能力)
CREATE TABLE `survey_dimension` (
                                `id` INT AUTO_INCREMENT PRIMARY KEY,
                                `name` VARCHAR(50) NOT NULL COMMENT '维度名称',
                                `description` VARCHAR(255) COMMENT '维度描述',
                                `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='维度字典表';

-- 2. 题目库表 (独立于问卷存在)
CREATE TABLE `survey_question` (
                                   `id` INT AUTO_INCREMENT PRIMARY KEY,
                                   `content` TEXT NOT NULL COMMENT '题目内容（正向描述）',
                                   `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目库';

-- 3. 题目-维度系数表 (核心逻辑：定义每道题对不同维度的贡献比重)
CREATE TABLE `survey_question_dimension` (
                                             `id` INT AUTO_INCREMENT PRIMARY KEY,
                                             `question_id` INT NOT NULL COMMENT '关联题目ID',
                                             `dimension_id` INT NOT NULL COMMENT '关联维度ID',
                                             `weight` DECIMAL(3, 2) NOT NULL DEFAULT 1.0 COMMENT '维度系数（如0.5, 1.0）',
                                             FOREIGN KEY (`question_id`) REFERENCES `survey_question`(`id`),
                                             FOREIGN KEY (`dimension_id`) REFERENCES `survey_dimension`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目维度系数关联表';

-- 4. 问卷主表
CREATE TABLE `survey_main` (
                               `id` INT AUTO_INCREMENT PRIMARY KEY,
                               `title` VARCHAR(100) NOT NULL COMMENT '问卷标题',
                               `is_active` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
                               `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问卷主表';

-- 5. 问卷-题目关联表 (定义问卷由哪些题组成)
CREATE TABLE `survey_item` (
                               `id` INT AUTO_INCREMENT PRIMARY KEY,
                               `survey_id` INT NOT NULL,
                               `question_id` INT NOT NULL,
                               `sort_order` INT DEFAULT 0 COMMENT '排序',
                               FOREIGN KEY (`survey_id`) REFERENCES `survey_main`(`id`),
                               FOREIGN KEY (`question_id`) REFERENCES `survey_question`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问卷题目关联表';

-- 6. 评价记录主表 (记录老师在某天评价了某个学生)
CREATE TABLE `survey_record` (
                                 `id` INT AUTO_INCREMENT PRIMARY KEY,
                                 `survey_id` INT NOT NULL COMMENT '使用的问卷ID',
                                 `teacher_id` INT NOT NULL COMMENT '评价老师ID',
                                 `student_id` INT NOT NULL COMMENT '被评学生ID',
                                 `fill_date` DATE NOT NULL COMMENT '评价日期（用于统计趋势）',
                                 `remark` TEXT COMMENT '老师评语',
                                 `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
                                 INDEX `idx_student_date` (`student_id`, `fill_date`) -- 优化趋势查询性能
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价记录主表';

-- 7. 答题明细表 (记录每道题的具体得分)
CREATE TABLE `survey_answer` (
                                 `id` INT AUTO_INCREMENT PRIMARY KEY,
                                 `record_id` INT NOT NULL COMMENT '关联记录ID',
                                 `question_id` INT NOT NULL COMMENT '关联题目ID',
                                 `score` INT NOT NULL COMMENT '原始得分(1-10)',
                                 FOREIGN KEY (`record_id`) REFERENCES `survey_record`(`id`),
                                 FOREIGN KEY (`question_id`) REFERENCES `survey_question`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='答卷明细表';