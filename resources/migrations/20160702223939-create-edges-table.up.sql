CREATE TABLE edges (
  parent_id integer references posts,
  child_id integer references posts,
  type text not null,
  PRIMARY KEY (parent_id, child_id)
);
