-- Created by Vertabelo (http://vertabelo.com)
-- Last modification date: 2015-11-15 16:30:38.927



-- tables
-- Table: book
CREATE TABLE book (
    _id integer  NOT NULL   PRIMARY KEY,
    name varchar(20)  NOT NULL
);

-- Table: exercise
CREATE TABLE exercise (
    _id integer  NOT NULL   PRIMARY KEY,
    name varchar(50)  NOT NULL,
    transition_image varchar(20)  NOT NULL,
    lesson_id integer  NOT NULL,
    FOREIGN KEY (lesson_id) REFERENCES lesson (_id)
);

-- Table: function
CREATE TABLE function (
    _id integer  NOT NULL   PRIMARY KEY,
    name varchar(50)  NOT NULL
);

-- Table: lesson
CREATE TABLE lesson (
    _id integer  NOT NULL   PRIMARY KEY,
    name varchar(20)  NOT NULL,
    book_id integer  NOT NULL,
    avg_completion_time_mili integer  NOT NULL,
    FOREIGN KEY (book_id) REFERENCES book (_id)
);

-- Table: practice_history
CREATE TABLE practice_history (
    _id integer  NOT NULL   PRIMARY KEY  AUTOINCREMENT,
    user_id integer  NOT NULL,
    lesson_id integer  NOT NULL,
    start_time varchar(20)  NOT NULL,
    finish_time varchar(20)  NOT NULL,
    total_hits integer  NOT NULL,
    percentage_wrong decimal(5,2)  NOT NULL,
    total_points integer  NOT NULL,
    FOREIGN KEY (user_id) REFERENCES user (_id),
    FOREIGN KEY (lesson_id) REFERENCES lesson (_id)
);

-- Table: script_entry
CREATE TABLE script_entry (
    _id integer  NOT NULL   PRIMARY KEY,
    text_to_read varchar(500)  NOT NULL,
    text_to_check varchar(500)  NOT NULL,
    text_to_show varchar(500)  NOT NULL,
    script_index integer  NOT NULL,
    exercise_id integer  NOT NULL,
    function_id integer  NOT NULL,
    CONSTRAINT script_entry_fk_1 UNIQUE (function_id),
    FOREIGN KEY (exercise_id) REFERENCES exercise (_id),
    FOREIGN KEY (function_id) REFERENCES function (_id)
);

-- Table: user
CREATE TABLE user (
    _id integer  NOT NULL   PRIMARY KEY,
    name varchar(20)  NOT NULL,
    code integer  NOT NULL  AUTOINCREMENT,
    last_completed_lesson_id integer  NOT NULL,
    CONSTRAINT user_ak_1 UNIQUE (_id),
    CONSTRAINT user_ak_2 UNIQUE (code)
);

-- Table: user_script
CREATE TABLE user_script (
    _id integer  NOT NULL   PRIMARY KEY,
    start_time datetime  NOT NULL,
    finish_time datetime  NOT NULL,
    user_id integer  NOT NULL,
    script_id integer  NOT NULL,
    number_attempts integer  NOT NULL,
    FOREIGN KEY (user_id) REFERENCES user (_id),
    FOREIGN KEY (script_id) REFERENCES script_entry (_id)
);





-- End of file.

