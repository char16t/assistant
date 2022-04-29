create table webhooks(
  id bigserial primary key,
  data jsonb not null
);
