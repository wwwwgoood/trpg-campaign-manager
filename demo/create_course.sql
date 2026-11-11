USE university;

CREATE TABLE course (
    course_id VARCHAR(10) PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    dept_name VARCHAR(50),
    credits DECIMAL(2,0)
);

