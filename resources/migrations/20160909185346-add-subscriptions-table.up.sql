CREATE TABLE SUBSCRIPTIONS(
  post_id integer references posts,
  EMAIL             TEXT    NOT NULL,
  DATE              BIGINT PRIMARY KEY NOT NULL,
  DESCENDANTS       BOOLEAN NOT NULL
);