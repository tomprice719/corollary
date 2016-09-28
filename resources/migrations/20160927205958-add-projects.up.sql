CREATE TABLE PROJECTS(
  ID SERIAL PRIMARY KEY       NOT NULL,
  NAME                TEXT    NOT NULL,
  PASSWORD            TEXT    NOT NULL,
  ROOT_POST_ID INTEGER REFERENCES POSTS,
  DATE                BIGINT  NOT NULL
);

alter table posts add column project_id INTEGER REFERENCES projects;