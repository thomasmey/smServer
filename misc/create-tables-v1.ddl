create table sms_app_config (
  t_key   varchar(255) not null,
  t_value varchar(255)
);

create table sms_message_queue (
	message_id integer not null primary key,
	receiver_no varchar(255) not null,
	message_text varchar(255) not null,
	send_at varchar(255)
);

drop table sms_message_periodic;

create table sms_message_periodic (
	message_id integer not null primary key,
	receiver_no varchar(255) not null,
	message_text varchar(255) not null,
	period varchar(255) not null,
	send_at varchar(255) not null
);


