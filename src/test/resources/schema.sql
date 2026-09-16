-- 1. 班级表 (维度表，N:1)
CREATE TABLE clazz (
    id BIGINT PRIMARY KEY,
    clazz_name VARCHAR(64) NOT NULL,
    grade VARCHAR(32) NOT NULL
);

-- 2. 学生根表 (只保留最基本的字段)
CREATE TABLE student (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_no VARCHAR(32) NOT NULL,
    name VARCHAR(32) NOT NULL,
    clazz_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL
);

-- 3. 学生核心档案表 (1:1 依附学生)
CREATE TABLE student_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    id_card VARCHAR(32) NOT NULL,
    emergency_phone VARCHAR(32),
    native_place VARCHAR(64)
);

-- 4. 兴趣爱好表 (1:N 依附学生，挂在虚拟模块下)
CREATE TABLE student_hobby (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    hobby_name VARCHAR(64) NOT NULL,
    category VARCHAR(32),
    frequency VARCHAR(32)
);

-- 5. 技能特长表 (1:N 依附学生，挂在虚拟模块下)
CREATE TABLE student_specialty (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    specialty_name VARCHAR(64) NOT NULL,
    skill_level VARCHAR(32),
    cert_no VARCHAR(64)
);

-- 6. 选课记录表 (1:N 依附学生)
CREATE TABLE student_course (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    course_name VARCHAR(64) NOT NULL,
    semester VARCHAR(32) NOT NULL,
    teacher_id BIGINT,
    score DECIMAL(5,2)
);

-- 7. 成绩分项明细表 (1:N 依附选课，构成父子孙链条)
CREATE TABLE student_course_score_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_course_id BIGINT NOT NULL,
    item_name VARCHAR(100) NOT NULL,
    weight DECIMAL(5,2) NOT NULL,
    score DECIMAL(5,2) NOT NULL,
    remark VARCHAR(255)
);

-- 8. 教师表 (N:1 维度表，依附于选课/课程)
CREATE TABLE teacher (
    id BIGINT PRIMARY KEY,
    teacher_name VARCHAR(32) NOT NULL,
    title VARCHAR(32) NOT NULL
);

CREATE TABLE sys_module (
    id BIGINT PRIMARY KEY,
    module_code VARCHAR(128) NOT NULL,
    module_name VARCHAR(128) NOT NULL,
    primary_table VARCHAR(128),
    parent_id BIGINT NOT NULL
);

CREATE TABLE sys_module_field (
    id BIGINT PRIMARY KEY,
    module_id BIGINT NOT NULL,
    table_name VARCHAR(128),
    column_name VARCHAR(128) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    sort_order INT NOT NULL
);

CREATE TABLE sys_table_relation (
    id BIGINT PRIMARY KEY,
    main_table VARCHAR(128) NOT NULL,
    main_field VARCHAR(128) NOT NULL,
    join_table VARCHAR(128) NOT NULL,
    join_field VARCHAR(128) NOT NULL,
    relation_type VARCHAR(32) NOT NULL
);
