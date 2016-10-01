alter table posts add column parent_id INTEGER references posts;

update posts set parent_id = (select min(parent_id) from edges where child_id = posts.id);

drop table edges;