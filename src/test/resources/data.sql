INSERT INTO sys_module (id, module_code, module_name, primary_table, parent_id) VALUES
    (100, 'MOD-STUDENT', '学生基本信息', 'student', 0),
    (110, 'MOD-STUDENT-PROFILE', '学生扩展档案', 'student_profile', 100),
    (120, 'MOD-CLAZZ', '所属班级', 'clazz', 100),
    (200, 'MOD-TALENT-VIRTUAL', '兴趣与特长中心', NULL, 100),
    (210, 'MOD-STUDENT-HOBBY', '兴趣爱好', 'student_hobby', 200),
    (220, 'MOD-STUDENT-SPECIALTY', '技能特长', 'student_specialty', 200),
    (300, 'MOD-STUDENT-COURSE', '选课记录', 'student_course', 100),
    (310, 'MOD-SCORE-ITEM', '考核分项明细', 'student_course_score_item', 300),
    (320, 'MOD-COURSE-TEACHER', '任课教师', 'teacher', 300);

INSERT INTO sys_table_relation (id, main_table, main_field, join_table, join_field, relation_type) VALUES
    (1, 'student', 'id', 'student_profile', 'student_id', 'ONE_TO_ONE'),
    (2, 'clazz', 'id', 'student', 'clazz_id', 'ONE_TO_MANY'),
    (3, 'student', 'id', 'student_hobby', 'student_id', 'ONE_TO_MANY'),
    (4, 'student', 'id', 'student_specialty', 'student_id', 'ONE_TO_MANY'),
    (5, 'student', 'id', 'student_course', 'student_id', 'ONE_TO_MANY'),
    (6, 'student_course', 'id', 'student_course_score_item', 'student_course_id', 'ONE_TO_MANY'),
    (7, 'teacher', 'id', 'student_course', 'teacher_id', 'ONE_TO_MANY');

INSERT INTO clazz (id, clazz_name, grade) VALUES
    (1, '计算机科学与技术2024级1班', '2024级'),
    (2, '软件工程2024级2班', '2024级'),
    (3, '人工智能2023级1班', '2023级');

INSERT INTO teacher (id, teacher_name, title) VALUES
    (1, '张道成', '教授'),
    (2, '李志强', '副教授'),
    (3, '王素华', '讲师');

INSERT INTO student (id, student_no, name, clazz_id, status) VALUES
    (1001, 'S2024001', '陈子轩', 1, 'NORMAL'),
    (1002, 'S2024002', '林雨欣', 1, 'NORMAL'),
    (1003, 'S2024003', '赵博宇', 2, 'NORMAL'),
    (1004, 'S2023088', '钱浩然', 3, 'SUSPENDED');

-- 1. 根模块字段（学生基本信息）
INSERT INTO sys_module_field (id, module_id, table_name, column_name, display_name, sort_order) VALUES
    (1001, 100, 'student', 'id', '学生ID', 1),
    (1002, 100, 'student', 'student_no', '学号', 2),
    (1003, 100, 'student', 'name', '姓名', 3),
    (1004, 100, 'student', 'status', '学籍状态', 4);

-- 2. 1:1 档案字段
INSERT INTO sys_module_field (id, module_id, table_name, column_name, display_name, sort_order) VALUES
    (1101, 110, 'student_profile', 'id', '档案ID', 1),
    (1102, 110, 'student_profile', 'student_id', '学生ID', 2),
    (1103, 110, 'student_profile', 'id_card', '身份证号', 3),
    (1104, 110, 'student_profile', 'emergency_phone', '紧急联系电话', 4),
    (1105, 110, 'student_profile', 'native_place', '籍贯', 5);

-- 3. N:1 班级维度字段
INSERT INTO sys_module_field (id, module_id, table_name, column_name, display_name, sort_order) VALUES
    (1201, 120, 'clazz', 'id', '班级ID', 1),
    (1202, 120, 'clazz', 'clazz_name', '班级名称', 2),
    (1203, 120, 'clazz', 'grade', '所属年级', 3);

-- 4. 虚拟模块下属：爱好字段 (1:N)
INSERT INTO sys_module_field (id, module_id, table_name, column_name, display_name, sort_order) VALUES
    (2101, 210, 'student_hobby', 'id', '记录ID', 1),
    (2102, 210, 'student_hobby', 'student_id', '学生ID', 2),
    (2103, 210, 'student_hobby', 'hobby_name', '爱好名称', 3),
    (2104, 210, 'student_hobby', 'category', '兴趣类别', 4),
    (2105, 210, 'student_hobby', 'frequency', '参与频次', 5);

-- 5. 虚拟模块下属：特长字段 (1:N)
INSERT INTO sys_module_field (id, module_id, table_name, column_name, display_name, sort_order) VALUES
    (2201, 220, 'student_specialty', 'id', '特长ID', 1),
    (2202, 220, 'student_specialty', 'student_id', '学生ID', 2),
    (2203, 220, 'student_specialty', 'specialty_name', '特长项目', 3),
    (2204, 220, 'student_specialty', 'skill_level', '专业技能等级', 4),
    (2205, 220, 'student_specialty', 'cert_no', '证书编号', 5);

-- 6. 选课子模块字段 (1:N)
INSERT INTO sys_module_field (id, module_id, table_name, column_name, display_name, sort_order) VALUES
    (3001, 300, 'student_course', 'id', '选课ID', 1),
    (3002, 300, 'student_course', 'student_id', '学生ID', 2),
    (3003, 300, 'student_course', 'course_name', '课程名称', 3),
    (3004, 300, 'student_course', 'semester', '学期', 4),
    (3005, 300, 'student_course', 'score', '综合成绩', 5);

-- 7. 孙模块：考核成绩分项 (1:N 级联)
INSERT INTO sys_module_field (id, module_id, table_name, column_name, display_name, sort_order) VALUES
    (3101, 310, 'student_course_score_item', 'id', '分项ID', 1),
    (3102, 310, 'student_course_score_item', 'student_course_id', '选课ID', 2),
    (3103, 310, 'student_course_score_item', 'item_name', '考核分项名称', 3),
    (3104, 310, 'student_course_score_item', 'weight', '比重权重', 4),
    (3105, 310, 'student_course_score_item', 'score', '分项得分', 5);

-- 8. 孙模块：任课教师维度 (N:1)
INSERT INTO sys_module_field (id, module_id, table_name, column_name, display_name, sort_order) VALUES
    (3201, 320, 'teacher', 'id', '教师ID', 1),
    (3202, 320, 'teacher', 'teacher_name', '教师姓名', 2),
    (3203, 320, 'teacher', 'title', '职称', 3);
