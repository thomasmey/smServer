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

create table sms_event_periodic (
	event_id integer not null primary key,
	receiver_no varchar(255) not null,
	message_text_template varchar(255) not null,
	period varchar(255) not null,
	event_at varchar(255) not null
);

create table sms_event_data (
  event_id   integer not null primary key,
  change_ts  timestamp NULL DEFAULT NULL,
  entry_ts   timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  data_version integer NOT NULL,
  event_data varbinary(512) NOT NULL
);

